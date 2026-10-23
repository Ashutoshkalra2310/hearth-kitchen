# Build Log

**Name:**
**Repository URL:**
**Start (date and time):**
**End (date and time):**
**Total working time:**

## 1. Timeline

One row per working block (roughly every 20–40 minutes).

| Start | End | What I worked on | Outcome | AI tool used? |
|---|---|---|---|---|
| | | Read brief, chose stack, designed entity model | Data model in `docs/data-model.md` | Yes (Claude Code) |

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

## 3. Data model

See `docs/data-model.md` for the diagram.

| Table | Purpose | Key columns | Relationships |
|---|---|---|---|
| `ingredient` | Current stock balance | `name`, `unit`, `on_hand`, `reserved` | — |
| `recipe` | Product and yield per batch | `name`, `yield_quantity`, `yield_unit` | has many `recipe_ingredient` |
| `recipe_ingredient` | Quantity of an ingredient per batch | `quantity_per_batch` | → `recipe`, → `ingredient` |
| `production_order` | Order for N batches of a recipe | `batches`, `status`, timestamps | → `recipe`; has many `order_requirement` |
| `order_requirement` | Snapshot of required quantity per ingredient | `required_quantity` | → `production_order`, → `ingredient` |
| `stock_movement` | Append-only history of stock changes | `type`, `quantity`, `created_at` | → `ingredient`, → `production_order` (null for `OPENING`) |

## 4. AI tool usage

**Tools used:** Claude Code
**Raw logs:** in the `ai-logs/` folder

Record the important interactions, especially where you rejected or changed what the AI produced.

| # | What I asked the AI | What it produced | What I kept, changed or rejected, and why |
|---|---|---|---|
| 1 | Make a UML of the backend entities | Class diagram where `StockMovement` also stored signed deltas and running balances (`onHandAfter`, `reservedAfter`) | Rejected the extra columns to keep it simple; type + quantity is enough to explain the stock |

**Parts I wrote or designed without AI:**

## 5. Problems and fixes

| Problem | How I found it | Fix | Time lost |
|---|---|---|---|
| | | | |

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

## 7. Unfinished work and known issues

What doesn't work yet, shortcuts you took, and anything you would not ship as is.

-

## 8. Next steps

What you would do next with more time, in priority order.

1.

## 9. Time summary

| Phase | Minutes |
|---|---|
| Understanding the brief and design | |
| Database and backend | |
| Frontend | |
| Testing | |
| README and build log | |
| **Total** | |
