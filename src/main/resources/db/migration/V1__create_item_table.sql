CREATE TABLE item (
                      id UUID PRIMARY KEY,
                      name VARCHAR(255) NOT NULL,
                      description TEXT,
                      price NUMERIC(19, 2),
                      image_url VARCHAR(500),
                      category VARCHAR(100)
);

-- Optional: Add some starting data
INSERT INTO item (id, name, description, price, category)
VALUES (gen_random_uuid(), 'Milk', 'Fresh whole milk', 3.50, 'Dairy');