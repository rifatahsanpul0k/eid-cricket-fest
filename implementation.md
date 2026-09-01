The current dashboard needs a **content hierarchy fix**, not just styling.

The biggest problems are all in a few frontend functions.

### 1. Admin incorrectly says “Organizer”

**File**

```text
frontend/src/app/(dashboard)/dashboard/page.tsx
```

**Function**

```text
DashboardHeader()
```

It currently hardcodes:

```tsx
<p>Organizer</p>
```

That is why ADMIN still sees Organizer.

Change it so the label comes from the session role:

```text
ADMIN      → Administrator
ORGANIZER  → Organizer
```

Best place for the role-selection logic:

**File**

```text
frontend/src/lib/dashboard/roles.ts
```

Add a helper such as:

```ts
getDashboardRoleLabel(session)
```

Priority:

```text
ADMIN > ORGANIZER
```

The existing `hasOrganizerAccess()` deliberately treats both `ORGANIZER` and `ADMIN` as authorized, but it doesn't distinguish their display names.

---

### 2. “Tournament data is temporarily unavailable” is wrong after DB reset

**File**

```text
frontend/src/lib/tournament/current-edition.ts
```

**Functions**

```text
getCurrentEditionData()
unavailable()
```

Currently all of these become the same error:

```text
backend unreachable
no tournament exists
no edition exists
```

and all display:

> Tournament data is temporarily unavailable.

That is incorrect.

Change the state model to distinguish:

```text
ready
not_configured
unavailable
```

Then:

```text
No tournaments in DB
→ "No tournament has been created yet."

Tournament exists, no edition
→ "No tournament edition has been created yet."

Backend/API failure
→ "Tournament data is temporarily unavailable."
```

This is important for an Admin because an empty database is a **setup state**, not an error.

---

### 3. The word “Review” above Teams/Draft/Fixtures/Matches is wrong

**File**

```text
frontend/src/app/(dashboard)/dashboard/page.tsx
```

**Function**

```text
DashboardCard()
```

This line is causing it:

```tsx
{count === undefined ? "Review" : `${count} total`}
```

So everything without a count says:

```text
Review

Teams
Review

Draft
Review

Fixtures
```

That makes no semantic sense.

Change `DashboardCard` to accept something like:

```ts
eyebrow?: string
```

or:

```ts
step?: string
```

Then explicitly give each card context.

For example:

```text
TEAM MANAGEMENT
Teams

PLAYER SELECTION
Draft

MATCH SETUP
Fixtures

MATCH OPERATIONS
Matches

PLAYER REVIEW
Registrations

FINANCE
Payments
```

Do not automatically print `"Review"`.

---

### 4. Reorder the dashboard according to the actual workflow

**File**

```text
frontend/src/app/(dashboard)/dashboard/page.tsx
```

**Function**

```text
DashboardPage()
```

Current order:

```text
Teams
Draft
Fixtures
Matches
Registrations
Payments
```

This is one reason it feels confusing.

That is not the order an organizer/admin operates the tournament.

Change it to:

```text
1. Tournament Setup

2. Player Registration
   Registrations
   Payments

3. Team Setup
   Teams

4. Player Draft
   Draft

5. Match Preparation
   Fixtures
   Matches

6. Match Day
   Scorer Console
```

At minimum, change the cards to:

```text
Registrations
Payments
Teams
Draft
Fixtures
Matches
Scorer Console
```

for the current implementation.

---

### 5. Add section headings instead of one flat grid

Again:

**File**

```text
frontend/src/app/(dashboard)/dashboard/page.tsx
```

**Function**

```text
DashboardPage()
```

Instead of:

```text
[Teams] [Draft]
[Fixtures] [Matches]
[Registrations] [Payments]
```

make it visually explain the process:

```text
PLAYER MANAGEMENT
[ Registrations ] [ Payments ]

TEAM MANAGEMENT
[ Teams ] [ Draft ]

MATCH MANAGEMENT
[ Fixtures ] [ Matches ]

MATCH DAY
[ Scorer Console ]
```

This single change will make the page much easier to understand.

---

### 6. Rewrite the descriptions

Still in:

```text
frontend/src/app/(dashboard)/dashboard/page.tsx
```

inside `DashboardPage()`.

The current descriptions are developer-oriented:

> Manage draft lifecycle, order, eligible pool, picks, and backend-provided rosters.

That's accurate technically, but bad dashboard UX.

Use action-oriented text.

Change them approximately to:

**Registrations**

```text
Review players who applied for the current tournament and approve or reject their registration.
```

**Payments**

```text
Check player registration payments and verify or reject submitted payments.
```

**Teams**

```text
Create tournament teams, add them to the current edition, assign captains, and view team setup.
```

**Draft**

```text
Run the player draft and assign approved players to tournament teams.
```

**Fixtures**

```text
Create venues and generate the tournament match schedule.
```

**Matches**

```text
Prepare matches by scheduling them, assigning scorers, selecting playing XIs, and recording the toss.
```

**Scorer Console**

```text
Open the scoring area for live tournament matches.
```

These tell an admin **what to do**, not how the backend works.

---

### 7. Change the button text from generic “Open”

**File**

```text
frontend/src/app/(dashboard)/dashboard/page.tsx
```

**Function**

```text
DashboardCard()
```

Currently every card says:

```text
Open
```

Instead add:

```ts
actionLabel: string
```

Then use:

```text
Registrations → Review registrations
Payments      → Review payments
Teams         → Manage teams
Draft         → Open draft
Fixtures      → Manage fixtures
Matches       → Manage matches
Scorer        → Open scorer console
```

That will improve clarity substantially.

---

### 8. The header should explain the current admin state

**File**

```text
frontend/src/app/(dashboard)/dashboard/page.tsx
```

**Functions**

```text
DashboardPage()
DashboardHeader()
```

For ADMIN with a clean DB, I would display:

```text
ADMINISTRATION

Tournament Dashboard

No tournament is configured yet.
Create the tournament and its first edition before starting
registration, teams, draft, or fixtures.
```

Once an edition exists:

```text
ADMINISTRATION

Tournament Dashboard

Eid-ul-Fitr 2027
Registration Open
```

So `DashboardHeader` should probably accept:

```ts
roleLabel
title
description
```

rather than hardcoding Organizer.

---

## 9. The dashboard is also missing an obvious Tournament Setup entry

This is an actual information-architecture gap.

Your current `DashboardPage()` has cards for Teams, Draft, Fixtures, Matches, Registrations and Payments, but **nothing explaining where the tournament/edition itself is configured**.

That's especially bad on a fresh DB because the first question is:

> Where do I create my tournament?

Eventually the dashboard should begin with:

```text
TOURNAMENT SETUP

Tournament & Edition
Configure tournament identity, current edition, dates,
registration window and tournament status.
```

Do **not** make this a dead frontend button unless the corresponding page/function exists. But conceptually, this must become the first admin operation.

---

## 10. Dashboard layout itself

**File**

```text
frontend/src/app/(dashboard)/layout.tsx
```

**Function**

```text
DashboardLayout()
```

Currently it only renders:

```text
SiteHeader
page content
SiteFooter
```

There is no dashboard-specific navigation.

Later I strongly recommend adding a dashboard sub-navigation:

```text
Overview
Registrations
Payments
Teams
Draft
Fixtures
Matches
Scorer Console
```

Desktop:

```text
left sidebar
```

Mobile:

```text
horizontal/dropdown dashboard menu
```

This will stop users from constantly returning to `/dashboard` just to navigate.

---

## Make these changes first

I would modify only these three existing files now:

| File                                              | Function                                   | Change                                    |
| ------------------------------------------------- | ------------------------------------------ | ----------------------------------------- |
| `frontend/src/lib/dashboard/roles.ts`             | add `getDashboardRoleLabel()`              | ADMIN displays Administrator              |
| `frontend/src/lib/tournament/current-edition.ts`  | `getCurrentEditionData()`, `unavailable()` | distinguish empty DB from backend failure |
| `frontend/src/app/(dashboard)/dashboard/page.tsx` | `DashboardPage()`                          | reorder/group workflow                    |
| same                                              | `DashboardHeader()`                        | dynamic role + clearer heading            |
| same                                              | `DashboardCard()`                          | remove generic Review/Open                |

Then your dashboard should read roughly:

```text
ADMINISTRATOR

Tournament Dashboard

No tournament has been configured yet.
Set up the tournament before beginning tournament operations.


PLAYER MANAGEMENT

Registrations
Review player applications and approve or reject registrations.
[Review registrations]

Payments
Verify registration payments submitted by players.
[Review payments]


TEAM MANAGEMENT

Teams
Create teams, add them to the edition, and assign captains.
[Manage teams]

Draft
Draft approved players into tournament teams.
[Open draft]


MATCH MANAGEMENT

Fixtures
Create venues and generate the tournament schedule.
[Manage fixtures]

Matches
Schedule matches, assign scorers, select XIs, and record tosses.
[Manage matches]


MATCH DAY

Scorer Console
Open live match scoring.
[Open scorer console]
```

That is much more appropriate for an admin dashboard than the current flat six-card layout.
