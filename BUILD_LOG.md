# Build Log

**Name:** Ashutosh Kalra
**Repository URL:** https://github.com/Ashutoshkalra2310/hearth-kitchen
**Start (date and time):** 23 Oct 2026, 13:10 IST
**End (date and time):** 23 Oct 2026, 17:00 IST
**Total working time:** about 3 h 50 min (over the 3-hour timebox; about 45 min of it went on the butter → margarine extension, which isn't in the brief)

## 1. Timeline

One row per working block (roughly every 20–40 minutes).

| Start | End | What I worked on | Outcome | AI tool used? |
|---|---|---|---|---|
| 13:10 | 13:51 | Read brief, chose stack and database, designed the entity model, simplified stock movements to type + quantity | Data model in `docs/data-model.md`; repository started | Yes (Claude Code) |
| 13:51 | 14:07 | Spring Boot skeleton, Flyway schema + seed, JPA entities, order/stock services with row locks, REST API | App runs on PostgreSQL; service and API checks pass | Yes (Claude Code) |
| 14:07 | 14:40 | React frontend (orders, create order, order detail, stock, ingredient history); ran it locally; set up TablePlus | All five screens working against the backend | Yes (Claude Code) |
| 14:40 | 15:10 | Reviewed entity relationships; removed `@OneToMany` / `@ManyToOne` in favour of plain id columns; added GitHub remote | Simpler entities, identical API behaviour | Yes (Claude Code) |
| 15:10 | 15:55 | Manual UI testing; Complete / Cancel did nothing; debugged and fixed; reviewed ER and UML diagrams | On-page confirmation works; diagrams up to date | Yes (Claude Code) |
| 15:55 | 16:40 | Extension: use margarine when butter is short (plan, migration, service, UI, tests); reset dev database | Substitution works end to end; 6 extra scenarios pass | Yes (Claude Code) |
| 16:40 | 17:00 | Build log | This document | Yes (Claude Code) |

## 2. Decisions and assumptions

Every point where the brief was unclear or you had to choose between approaches.

| # | Question or decision point | Options I considered | What I chose | Why |
|---|---|---|---|---|
| 1 | Stack | TypeScript / Python / Java / Kotlin backends; React frontend | Kotlin + Spring Boot, React | The stack I know best |
| 2 | Database | SQLite / PostgreSQL | PostgreSQL | Preferred by the brief; real transactions and row locks |
| 3 | Units: stock is in kg/L, recipes in g/ml | Store in display units / store in base units | Store everything in g, ml, pcs as `NUMERIC(14,3)`; show kg/L in the UI | No conversion at calculation time, no float rounding |
| 4 | How is opening stock explained? | Just set `on_hand` / record it as a movement | Seed writes an `OPENING` movement per ingredient | Movement history fully explains every number |
| 5 | Where do balances live? | Derive from movements every time / store on ingredient + log movements | Store `on_hand` and `reserved` on ingredient, write a movement in the same transaction | Fast reads; DB constraints (`reserved <= on_hand`) block bad states |
| 6 | What does a movement row hold? | Type + quantity / also signed deltas and running balances | Type + quantity only | Simpler; the type defines the effect on on-hand and reserved |
| 7 | Brief uses "release" for both releasing an order and freeing a reservation | Keep "release" / rename | Movement types `OPENING`, `RESERVE`, `UNRESERVE`, `CONSUME` | Avoids two meanings of "release" |
| 8 | What if a recipe changes after an order is created? | Read recipe live / snapshot requirements | Snapshot requirements on the order at creation | Existing orders don't change underneath you |
| 9 | Order rules | — | Batches are whole numbers > 0; drafts aren't editable (cancel and recreate); cancel allowed from Draft or Released; Completed and Cancelled are final | Keeps the state machine small |
| 10 | Does completing record actual usage? | Actual vs planned / consume exactly what was reserved | Consume exactly what was reserved | Not asked for in the brief |
| 11 | Users and auth | Login / none | None, single user | Out of scope |
| 12 | Order reference shown in UI | Separate order number / id | `PO-{id}` | No extra column needed |
| 13 | Two requests touching the same stock (or a double-clicked Release) | No locking / optimistic `@Version` / row locks | Lock the order row, then its ingredient rows in id order (`SELECT ... FOR UPDATE`) at the start of release, complete and cancel | JPA writes balances computed in memory, so without locks two releases can overwrite each other's reservation (lost update) and the DB constraint won't notice. Fixed lock order prevents deadlocks |
| 14 | Upper limit on batches | None / a cap | 1 to 1000 | Very large numbers would overflow `NUMERIC(14,3)` and fail with a database error |
| 15 | Return entities or read models from services | Entities / read models | Read models (`OrderDetail`, `IngredientStock`, ...) built inside the transaction | `open-in-view` is off, so lazy relations must be loaded inside the service |
| 16 | Where stock changes happen | In the order service / one stock service | Only `StockService` changes balances, and it always records the movement in the same call (`MANDATORY` transaction) | One place guarantees every balance change has a movement |
| 17 | API error format | Ad-hoc JSON / RFC 9457 problem details | Problem details: 404 not found, 409 wrong status, 409 with a `shortages` list for a short release, 400 for invalid input | Built into Spring; the frontend reads one error shape |
| 18 | Where the batches limit is checked | Request annotation and service / service only | Service checks the range; the request only checks the fields are present | One place defines the limit |
| 19 | Frontend libraries | UI kit + data-fetching library / plain React | React + React Router + `fetch`, plain CSS | Small app; less to explain; function over polish |
| 20 | Calling the API from the dev server | CORS config in Spring / Vite proxy | Vite forwards `/api` to `localhost:8080` | Browser sees one origin, no CORS code |
| 21 | Where the order status filter lives | Component state / URL query (`?status=`) | URL query | Survives reload and back/forward, linkable |
| 22 | Showing why an ingredient's numbers are what they are | Store running balances / compute in UI | Ingredient page replays movements to show change and balance after each one | Keeps the table simple (decision 6) while still reading like a statement |
| 23 | How entities refer to each other | JPA relationships (`@ManyToOne` / `@OneToMany`) / plain id columns | Plain id columns (`recipeId`, `ingredientId`, `orderId`); related rows loaded by id in the services | Simpler entities with no lazy loading or proxies, and every query is explicit. Foreign keys in the schema still enforce integrity |
| 24 | Use margarine when butter is short: where is the rule? | Hard-code ids 3 → 11 / `substitute_id` column | `ingredient.substitute_id`, set by migration `V3` (by name, not id) | Works on a fresh database and for other ingredients later |
| 25 | Butter only partly available | Split butter + margarine / swap the whole requirement | Swap the whole requirement to the substitute (same unit, 1:1) | One source per requirement; simple to show and explain |
| 26 | What do complete / cancel act on once substitutes exist? | The requirements / what was actually reserved | The order's `RESERVE` movements | A requirement for butter may have been reserved as margarine; matches "consumes its reserved stock" |
| 27 | Two requirements counting on the same stock (e.g. a shared substitute) | Check each against current stock / track remaining stock while planning | Plan all reservations first with a running `remaining` per ingredient, then reserve | Prevents over-promising the same stock twice |

## 3. Data model

See `docs/data-model.md` for the diagrams. Every relationship is a plain foreign key column; the entities hold ids, not JPA relationships.

| Table | Purpose | Key columns | Relationships |
|---|---|---|---|
| `ingredient` | Current stock balance | `name`, `unit`, `on_hand`, `reserved`, `substitute_id` | `substitute_id` → `ingredient` (optional) |
| `recipe` | Product and yield per batch | `name`, `yield_quantity`, `yield_unit` | — |
| `recipe_ingredient` | Quantity of an ingredient per batch | `recipe_id`, `ingredient_id`, `quantity_per_batch` | → `recipe`, → `ingredient` |
| `production_order` | Order for N batches of a recipe | `recipe_id`, `batches`, `status`, timestamps | → `recipe` |
| `order_requirement` | Snapshot of required quantity per ingredient | `order_id`, `ingredient_id`, `required_quantity` | → `production_order`, → `ingredient` |
| `stock_movement` | Append-only history of stock changes | `ingredient_id`, `order_id`, `type`, `quantity`, `created_at` | → `ingredient`, → `production_order` (null only for `OPENING`) |

## 4. AI tool usage

**Tools used:** Claude Code (Claude Opus 5.5), in the Claude desktop app
**Raw logs:** in the `ai-logs/` folder

Record the important interactions, especially where you rejected or changed what the AI produced.

| # | What I asked the AI | What it produced | What I kept, changed or rejected, and why |
|---|---|---|---|
| 1 | Make a UML of the backend entities | Class diagram where `StockMovement` also stored signed deltas and running balances (`onHandAfter`, `reservedAfter`) | Rejected the extra columns to keep it simple; type + quantity is enough to explain the stock |
| 2 | Generate JPA entities for the data model | Entities linked with `@ManyToOne` and `@OneToMany` collections (recipe lines, order requirements) | Rejected the relationships; changed every link to a plain id column and load related rows with repository queries |
| 3 | Plan using margarine when butter isn't available (my draft hard-coded ids 3 and 11 in `release()`) | Plan: `substitute_id` column, whole-requirement swap, complete/cancel from `RESERVE` movements; pointed out my draft would crash (margarine not loaded/locked) and still reserved butter | Chose the column and whole swap; replaced my draft with the planned version |
| 4 | Build the service layer, with no optional extras | Also added row locks on the order and its ingredients | Kept: without them, two releases can overwrite each other's reservation (lost update). Not the optional concurrency extra: no concurrency test was written |
| 5 | Build the order detail screen | Complete / Cancel asked for confirmation with `window.confirm` | Changed: the embedded browser suppresses `window.confirm`, so the buttons did nothing. Replaced with an on-page confirmation |
| 6 | Start the frontend with the latest packages | Proposed Vite 8 and TypeScript 7 | Changed to Vite 7 and TypeScript 5.9: stable, widely known versions that are easier to explain |

**Parts I wrote or designed without AI:**

- Choice of stack (Kotlin + Spring Boot, React), PostgreSQL, and building step by step with a review after each step.
- Keeping stock movements to type + quantity (no stored deltas or running balances).
- Entities without JPA relationships (plain id columns).
- The butter → margarine requirement, the first draft of it in `release()`, and the rules: a substitute column, swap the whole requirement.
- Manual testing of the UI, which found the Complete / Cancel bug and the hand-edited egg and margarine stock.
- The code itself was generated by Claude Code; I reviewed each step before moving on.

## 5. Problems and fixes

| Problem | How I found it | Fix | Time lost |
|---|---|---|---|
| Complete and Cancel buttons did nothing; Release worked | Manual testing in the IDE's embedded browser. DB showed no status change; the endpoints answered correctly via curl; `window.confirm` returned `false` in 1 ms without showing a dialog | Replaced `window.confirm` with an on-page confirmation (Yes / Go back) | ~10 min |
| Margarine added and eggs edited directly in the database | Margarine had no `OPENING` movement; eggs showed 200 but their history added up to 62 | Added margarine through migration `V3` (with its opening movement) and recreated the dev database | ~5 min |

## 6. Testing

At least five scenarios you tested, manually or with automated tests.

Service layer checked with a temporary integration test against a separate database (not committed):

| # | Scenario | Steps | Expected result | Actual result | Pass / Fail |
|---|---|---|---|---|---|
| 1 | Create a draft order | Brioche × 2 | Draft with 7 requirements, Butter 2400 g | As expected | Pass |
| 2 | Release with enough stock | Release the order above | Released; 7 `RESERVE` movements; Butter reserved 2400, available 5600 | As expected | Pass |
| 3 | Release with shortages | Brioche × 5, release | Rejected; Butter (6000 / 5600 / 400) and Eggs (90 / 84 / 6) listed; order stays Draft; nothing reserved | As expected | Pass |
| 4 | Complete | Complete order from #2 | Completed; 7 `CONSUME` movements; Butter on hand 5600, reserved 0 | As expected | Pass |
| 5 | Cancel a released order | Sponge × 1, release, cancel | Cancelled; 5 `UNRESERVE` movements; reservation freed | As expected | Pass |
| 6 | Cancel a draft | Cancel order from #3 | Cancelled; no movements | As expected | Pass |
| 7 | Invalid transitions | Complete a draft; release / cancel a completed order | Rejected | As expected | Pass |
| 8 | Invalid batches | Create with 0 batches | Rejected | As expected | Pass |
| 9 | History explains balances | Replay every ingredient's movements | Equals current on hand and reserved | As expected | Pass |

REST API checked end to end with curl against a throwaway database (script not committed):

| # | Scenario | Steps | Expected result | Actual result | Pass / Fail |
|---|---|---|---|---|---|
| 10 | Full order flow over HTTP | Create (201 + Location), release, short release (409 + shortages), list by status, complete, release+cancel, cancel draft, ingredient history | Correct status codes and stock numbers; Butter history shows OPENING, RESERVE, CONSUME, RESERVE, UNRESERVE | As expected | Pass |
| 11 | Error responses | Unknown order / recipe / ingredient, complete a cancelled order, 0 batches, missing recipeId, malformed JSON, unknown status filter | 404 / 409 / 400 with a readable `detail` | As expected | Pass |

Butter → margarine substitution checked over HTTP against a throwaway database with butter set to 1 kg (script not committed):

| # | Scenario | Steps | Expected result | Actual result | Pass / Fail |
|---|---|---|---|---|---|
| 12 | Butter short, margarine enough | Brioche × 1 (1.2 kg butter), release | Margarine reserved; requirement marked as substituted; butter untouched | As expected | Pass |
| 13 | Complete a substituted order | Complete #12 | Margarine consumed (5 → 3.8 kg); butter unchanged | As expected | Pass |
| 14 | Butter enough | Sponge × 1 (0.8 kg), release, cancel | Butter reserved, then freed | As expected | Pass |
| 15 | Cancel a substituted order | Brioche × 1, release, cancel | Margarine reserved, then freed | As expected | Pass |
| 16 | Both short | Brioche × 4 (4.8 kg): butter 1 kg, margarine 3.8 kg | 409; Butter listed with margarine's availability; nothing reserved | As expected | Pass |
| 17 | History still explains numbers | Replay all movements | All 11 ingredients match | As expected | Pass |

Manual testing in the browser:

| # | Scenario | Steps | Expected result | Actual result | Pass / Fail |
|---|---|---|---|---|---|
| 18 | Order flow through the UI | Create order, release, complete, cancel | Status and stock update on screen | Release worked; Complete / Cancel did nothing (`window.confirm` suppressed). Passed after the fix | Fail, then Pass |
| 19 | On-page confirmation | Released order → Complete → panel appears → Go back | Panel shows; Go back leaves the order Released | As expected | Pass |

## 7. Unfinished work and known issues

What doesn't work yet, shortcuts you took, and anything you would not ship as is.

- **No automated tests in the repository.** Every scenario in section 6 was run, but with temporary scripts and a throwaway database, not committed tests.
- **No way to add stock.** There is no receive-stock or adjustment movement or screen. Changing stock directly in the database breaks the "history explains the numbers" rule (this happened once with eggs).
- **Substitution is hard to demo with the seed data.** 120 eggs cover exactly 8 kg of butter's worth of brioche, so any order big enough to need margarine is also short on eggs.
- **Substitutes are simple.** One substitute per ingredient, 1:1, same unit, used for every recipe. No partial split (some butter plus some margarine).
- **The concurrency handling is untested.** Row locks are in place, but no test runs two releases at the same moment.
- **No Docker setup.** It runs locally only, with the frontend on the Vite dev server (no production build served by Spring).
- **No authentication.** It is a single-user app.
- **Numbers in the API.** Quantities are sent as JSON numbers, which is fine for these sizes; very large or very precise values would need strings.

## 8. Next steps

What you would do next with more time, in priority order.

1. Turn the temporary checks into committed integration tests (JUnit + a test PostgreSQL), including a test that runs two releases at once.
2. Add a `RECEIVE` / `ADJUST` movement type and a screen to add stock, so stock changes never bypass the history.
3. Raise a restock request automatically when a release is short (optional item in the brief).
4. Docker Compose for PostgreSQL, backend and frontend.
5. Per-recipe substitutes with a conversion ratio, and partial substitution if the kitchen wants it.

## 9. Time summary

| Phase | Minutes |
|---|---|
| Understanding the brief and design | 40 |
| Database and backend (including the id-column refactor) | 45 |
| Frontend (including the confirmation fix) | 30 |
| Testing (automated checks and manual UI testing) | 35 |
| Local setup, TablePlus and GitHub | 15 |
| Butter → margarine extension (beyond the brief) | 45 |
| README and build log | 20 |
| **Total** | **230** |
