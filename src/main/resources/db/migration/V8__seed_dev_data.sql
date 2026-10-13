-- Company
INSERT INTO companies (id, name, cnpj, global_score_goal)
VALUES (1, 'EcoAl Corp', '12345678000100', 100);

-- Users (password is bcrypt hash of 'password')
INSERT INTO users (id, name, email, password, role, company_id, department)
VALUES
    (1, 'Alice', 'alice@example.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'admin', 1, 'Engineering');

INSERT INTO users (id, name, email, password, role, company_id, department)
VALUES
    (2, 'Bob', 'bob@example.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'user', 1, 'Operations');

-- Goals (one per ESG dimension)
INSERT INTO goals (id, title, description, dimension, company_id)
VALUES (1, 'Reduce Carbon Emissions', 'Lower CO2 output by 20% by end of 2026', 'environmental', 1);

INSERT INTO goals (id, title, description, dimension, company_id)
VALUES (2, 'Improve Employee Well-being', 'Launch mental health and wellness programs', 'social', 1);

INSERT INTO goals (id, title, description, dimension, company_id)
VALUES (3, 'Strengthen Board Diversity', 'Achieve at least 40% diverse board composition', 'governance', 1);

-- Tasks for goal 1 (environmental)
INSERT INTO tasks (id, title, description, score, completed, goal_id)
VALUES (1, 'Audit current energy consumption', 'Collect baseline data from all facilities', 25, FALSE, 1);

INSERT INTO tasks (id, title, description, score, completed, goal_id)
VALUES (2, 'Install solar panels on HQ roof', 'Procure and install 100 kW solar installation', 25, FALSE, 1);

INSERT INTO tasks (id, title, description, score, completed, goal_id)
VALUES (3, 'Switch to LED lighting company-wide', 'Replace all fluorescent fixtures with LEDs', 25, FALSE, 1);

-- Energy metrics for the company
INSERT INTO energy_metrics (company_id, month, year, realized_value, goal_value)
VALUES (1, 'January', 2026, 4200.0, 4000.0);

INSERT INTO energy_metrics (company_id, month, year, realized_value, goal_value)
VALUES (1, 'February', 2026, 3850.0, 4000.0);

-- Training metric for the company
INSERT INTO training_metrics (company_id, quarter, year, total_hours)
VALUES (1, 'Q1', 2026, 120.0);

-- User contributions for Alice (user_id = 1)
INSERT INTO user_contributions (user_id, category, points)
VALUES (1, 'volunteering', 50.0);

INSERT INTO user_contributions (user_id, category, points)
VALUES (1, 'training', 30.0);

-- Advance identity sequences past the explicitly seeded IDs
SELECT setval(pg_get_serial_sequence('companies', 'id'), (SELECT MAX(id) FROM companies));
SELECT setval(pg_get_serial_sequence('users', 'id'), (SELECT MAX(id) FROM users));
SELECT setval(pg_get_serial_sequence('goals', 'id'), (SELECT MAX(id) FROM goals));
SELECT setval(pg_get_serial_sequence('tasks', 'id'), (SELECT MAX(id) FROM tasks));
