-- All quantities are in the ingredient's base unit (G, ML or PCS).

CREATE TABLE ingredient (
    id       BIGSERIAL PRIMARY KEY,
    name     VARCHAR(100)   NOT NULL UNIQUE,
    unit     VARCHAR(10)    NOT NULL CHECK (unit IN ('G', 'ML', 'PCS')),
    on_hand  NUMERIC(14, 3) NOT NULL DEFAULT 0 CHECK (on_hand >= 0),
    reserved NUMERIC(14, 3) NOT NULL DEFAULT 0 CHECK (reserved >= 0),
    CONSTRAINT ingredient_reserved_not_above_on_hand CHECK (reserved <= on_hand)
);

CREATE TABLE recipe (
    id             BIGSERIAL PRIMARY KEY,
    name           VARCHAR(100)   NOT NULL UNIQUE,
    yield_quantity NUMERIC(14, 3) NOT NULL CHECK (yield_quantity > 0),
    yield_unit     VARCHAR(20)    NOT NULL
);

CREATE TABLE recipe_ingredient (
    id                 BIGSERIAL PRIMARY KEY,
    recipe_id          BIGINT         NOT NULL REFERENCES recipe (id),
    ingredient_id      BIGINT         NOT NULL REFERENCES ingredient (id),
    quantity_per_batch NUMERIC(14, 3) NOT NULL CHECK (quantity_per_batch > 0),
    UNIQUE (recipe_id, ingredient_id)
);

CREATE TABLE production_order (
    id           BIGSERIAL PRIMARY KEY,
    recipe_id    BIGINT      NOT NULL REFERENCES recipe (id),
    batches      INTEGER     NOT NULL CHECK (batches > 0),
    status       VARCHAR(20) NOT NULL CHECK (status IN ('DRAFT', 'RELEASED', 'COMPLETED', 'CANCELLED')),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    released_at  TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ
);

CREATE INDEX production_order_status_idx ON production_order (status);

-- Snapshot of quantity_per_batch * batches, taken when the order is created.
CREATE TABLE order_requirement (
    id                BIGSERIAL PRIMARY KEY,
    order_id          BIGINT         NOT NULL REFERENCES production_order (id),
    ingredient_id     BIGINT         NOT NULL REFERENCES ingredient (id),
    required_quantity NUMERIC(14, 3) NOT NULL CHECK (required_quantity > 0),
    UNIQUE (order_id, ingredient_id)
);

-- Append-only history. quantity is always positive; type decides its effect:
--   OPENING   on_hand +q
--   RESERVE   reserved +q
--   UNRESERVE reserved -q
--   CONSUME   on_hand -q, reserved -q
CREATE TABLE stock_movement (
    id            BIGSERIAL PRIMARY KEY,
    ingredient_id BIGINT         NOT NULL REFERENCES ingredient (id),
    order_id      BIGINT REFERENCES production_order (id),
    type          VARCHAR(20)    NOT NULL CHECK (type IN ('OPENING', 'RESERVE', 'UNRESERVE', 'CONSUME')),
    quantity      NUMERIC(14, 3) NOT NULL CHECK (quantity > 0),
    created_at    TIMESTAMPTZ    NOT NULL DEFAULT now(),
    -- Only OPENING movements have no order; every other movement must have one.
    CONSTRAINT stock_movement_order_link CHECK ((type = 'OPENING') = (order_id IS NULL))
);

CREATE INDEX stock_movement_ingredient_idx ON stock_movement (ingredient_id, created_at);
CREATE INDEX stock_movement_order_idx ON stock_movement (order_id);
