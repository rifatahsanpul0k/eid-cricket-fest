# Backend V1 Freeze

Backend v1 feature development is frozen after Production Hardening III-D passes local verification, Docker verification, OpenAPI contract verification, and GitHub Actions CI.

## Implemented Areas

- Authentication and security
- Tournaments and editions
- Players
- Registration and payment
- Teams, rosters, and captains
- Draft
- Fixtures and matches
- Playing XI, toss, and scorers
- Live scoring
- Correction, undo, and idempotency
- Score revision ordering for REST and WebSocket live state
- Standings and NRR
- Statistics
- Knockout
- Awards
- History and career
- Pagination and filtering
- Rate limiting
- Query and N+1 hardening
- Production profiles
- Docker
- Health and readiness
- CI
- OpenAPI

## Live State Ordering

Every committed live-scoring mutation increments `innings.scoreRevision` exactly once. Idempotent retries return the current innings state without another revision increment or score-change broadcast.

WebSocket score updates are published after commit and include the latest live response. Frontends should compare `innings.inningsId` and `innings.scoreRevision`; for the same innings, an incoming revision older than the currently displayed revision is stale and should be ignored. When the innings ID changes, revision ordering resets for that innings.

## Non-V1 Features

These are intentionally outside backend v1:

- Actual Super Over scoring
- Bowling quota enforcement
- Distributed rate limiter for multiple backend instances
- Advanced observability/APM
- Kubernetes/Terraform

## Freeze Rule

BACKEND V1 FEATURE FREEZE applies once all freeze checks pass. New backend features should be added only when frontend integration exposes a genuinely missing requirement or production reveals a correctness/security problem.
