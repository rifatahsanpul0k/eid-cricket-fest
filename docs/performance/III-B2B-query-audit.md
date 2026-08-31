# Production Hardening III-B2B Query Audit

## Query-count results

| Resource | Mandatory filter | Optional filters | Sort | Existing relevant index | Before | After | Action |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Players | - | `q`, `category` | `fullName`, `createdAt` | `idx_players_full_name`, `players_pkey`, `uq_players_user` | size 5: 3, size 20: 3 | size 5: 3, size 20: 3 | No N+1 fix needed |
| Registrations | `editionId` | `status`, `category`, `q` | `registeredAt`, `status`, `createdAt` | `idx_player_registrations_edition`, `idx_player_registrations_status`, `uq_player_registration`, `uq_player_registrations_id_edition` | size 5: 4, size 20: 4 | size 5: 4, size 20: 4 | No N+1 fix needed |
| Payments | `editionId` through registration join | `status`, `method`, `q` | `createdAt`, `paidAt`, `status` | `idx_registration_payments_registration`, `idx_registration_payments_status`, `uq_payment_transaction_reference`, registration edition indexes | size 5: 2, size 20: 2 | size 5: 2, size 20: 2 | No N+1 fix needed |
| Matches | `editionId` | `status`, `stage`, `teamId` | `matchNumber`, `scheduledAt`, `stage`, `createdAt` | `idx_matches_edition`, `idx_matches_status`, `idx_matches_scheduled`, `uq_match_number`, `uq_match_id_edition` | size 5: 22, size 20: 82 | size 5: 2, size 20: 2 | Added `@EntityGraph(teamA, teamA.team, teamB, teamB.team, venue)` |

## Generated query shapes

Players:

```sql
SELECT ...
FROM players p
WHERE lower(p.full_name) LIKE ?
ORDER BY p.full_name, p.id
LIMIT ? OFFSET ?;

SELECT count(p.id)
FROM players p
WHERE lower(p.full_name) LIKE ?;
```

Registrations:

```sql
SELECT ...
FROM player_registrations pr
WHERE pr.tournament_edition_id = ?
  AND pr.status = ?
ORDER BY pr.registered_at DESC, pr.id
LIMIT ? OFFSET ?;

SELECT count(pr.id)
FROM player_registrations pr
WHERE pr.tournament_edition_id = ?
  AND pr.status = ?;
```

Payments:

```sql
SELECT ...
FROM registration_payments rp
JOIN player_registrations pr
  ON pr.id = rp.registration_id
WHERE pr.tournament_edition_id = ?
  AND rp.status = ?
  AND rp.payment_method = ?
ORDER BY rp.created_at DESC, rp.id
LIMIT ? OFFSET ?;

SELECT count(rp.id)
FROM registration_payments rp
JOIN player_registrations pr
  ON pr.id = rp.registration_id
WHERE pr.tournament_edition_id = ?
  AND rp.status = ?
  AND rp.payment_method = ?;
```

Matches after `@EntityGraph`:

```sql
SELECT ...
FROM matches m
JOIN tournament_teams ta ON ta.id = m.team_a_id
JOIN teams t1 ON t1.id = ta.team_id
JOIN tournament_teams tb ON tb.id = m.team_b_id
JOIN teams t2 ON t2.id = tb.team_id
LEFT JOIN venues v ON v.id = m.venue_id
WHERE m.tournament_edition_id = ?
  AND m.status = ?
  AND m.stage = ?
ORDER BY m.match_number, m.id
LIMIT ? OFFSET ?;

SELECT count(m.id)
FROM matches m
WHERE m.tournament_edition_id = ?
  AND m.status = ?
  AND m.stage = ?;
```

## EXPLAIN summary

The representative Testcontainers dataset was intentionally small. After `ANALYZE`, PostgreSQL used seq scans for the sample content/count queries and touched only one or two shared buffers per plan. That is expected for 25-row fixtures and is not evidence that extra indexes are required.

Important observations:

| Resource | EXPLAIN observation | Index action |
| --- | --- | --- |
| Players | `lower(full_name) LIKE '%...%'` cannot be meaningfully helped by the existing plain btree `full_name` index. `pg_trgm` was not justified for the expected tournament scale. | None |
| Registrations | Existing edition/status indexes and unique edition/player indexes cover the main equality paths. A composite `(tournament_edition_id, status, registered_at DESC, id)` may become useful at larger scale, but current expected volume and plans do not justify it yet. | None |
| Payments | Query filters payment status/method and joins by registration to edition. Existing registration and payment status indexes cover the current path; table size expectations do not justify another write-cost index now. | None |
| Matches | `uq_match_number (tournament_edition_id, match_number)` already fits the common edition + match-number pagination path. Match tables are expected to stay small per edition. | None |

## Decision

No `V18__add_query_performance_indexes.sql` migration was created. Existing indexes are sufficient for the current query patterns and expected scale, and the only genuine N+1 issue was in paginated match response mapping.
