-- An ingredient can name one substitute (same unit, used 1:1). When an order is released and the
-- ingredient can't cover a requirement on its own, the whole requirement is reserved from the substitute.
ALTER TABLE ingredient
    ADD COLUMN substitute_id BIGINT REFERENCES ingredient (id),
    ADD CONSTRAINT ingredient_substitute_not_self CHECK (substitute_id <> id);

INSERT INTO ingredient (name, unit, on_hand) VALUES ('Margarine', 'G', 5000)
ON CONFLICT (name) DO NOTHING;

-- Opening stock for margarine, unless it already has movements.
INSERT INTO stock_movement (ingredient_id, type, quantity)
SELECT i.id, 'OPENING', i.on_hand
FROM ingredient i
WHERE i.name = 'Margarine'
  AND i.on_hand > 0
  AND NOT EXISTS (SELECT 1 FROM stock_movement m WHERE m.ingredient_id = i.id);

UPDATE ingredient
SET substitute_id = (SELECT id FROM ingredient WHERE name = 'Margarine')
WHERE name = 'Butter';
