-- Seed data from the brief, converted to base units (kg -> g, L -> ml).

INSERT INTO ingredient (name, unit, on_hand) VALUES
    ('Flour',     'G',   50000),
    ('Sugar',     'G',   20000),
    ('Butter',    'G',    8000),
    ('Eggs',      'PCS',   120),
    ('Milk',      'ML',  15000),
    ('Yeast',     'G',     500),
    ('Salt',      'G',    5000),
    ('Tomatoes',  'G',   30000),
    ('Olive oil', 'ML',   5000),
    ('Garlic',    'G',    2000);

-- Record the opening stock so each ingredient's history explains its on-hand number.
INSERT INTO stock_movement (ingredient_id, type, quantity)
SELECT id, 'OPENING', on_hand
FROM ingredient;

INSERT INTO recipe (name, yield_quantity, yield_unit) VALUES
    ('Brioche loaf',   12, 'loaves'),
    ('Sponge cake',     8, 'cakes'),
    ('Marinara sauce', 10, 'L');

INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity_per_batch)
SELECT r.id, i.id, v.quantity
FROM (VALUES
    ('Brioche loaf',   'Flour',     3000),
    ('Brioche loaf',   'Butter',    1200),
    ('Brioche loaf',   'Eggs',        18),
    ('Brioche loaf',   'Sugar',      300),
    ('Brioche loaf',   'Milk',       600),
    ('Brioche loaf',   'Yeast',       60),
    ('Brioche loaf',   'Salt',        50),
    ('Sponge cake',    'Flour',     1000),
    ('Sponge cake',    'Sugar',     1000),
    ('Sponge cake',    'Eggs',        20),
    ('Sponge cake',    'Butter',     800),
    ('Sponge cake',    'Milk',       300),
    ('Marinara sauce', 'Tomatoes',  8000),
    ('Marinara sauce', 'Olive oil',  400),
    ('Marinara sauce', 'Garlic',     250),
    ('Marinara sauce', 'Salt',        80),
    ('Marinara sauce', 'Sugar',       40)
) AS v (recipe_name, ingredient_name, quantity)
JOIN recipe r ON r.name = v.recipe_name
JOIN ingredient i ON i.name = v.ingredient_name;
