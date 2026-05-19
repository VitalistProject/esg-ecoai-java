-- Company
INSERT INTO companies (id, name, cnpj, global_score_goal)
VALUES (1, 'EcoAl Corp', '12345678000100', 100);

-- Users (password is bcrypt hash of 'password')
INSERT INTO users (id, name, email, password, role, company_id, department)
VALUES
    (1, 'Alice', 'alice@example.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'admin', 1, 'Engineering'),
    (2, 'Bob',   'bob@example.com',   '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'user',  1, 'Operations');

-- Goals (one per ESG dimension)
INSERT INTO goals (id, title, description, dimension, company_id)
VALUES
    (1, 'Reduce Carbon Emissions',      'Lower CO2 output by 20% by end of 2026',         'environmental', 1),
    (2, 'Improve Employee Well-being',  'Launch mental health and wellness programs',      'social',        1),
    (3, 'Strengthen Board Diversity',   'Achieve at least 40% diverse board composition', 'governance',    1);

-- Tasks for goal 1 (environmental)
INSERT INTO tasks (id, title, description, score, completed, goal_id)
VALUES
    (1, 'Audit current energy consumption',  'Collect baseline data from all facilities',    25, FALSE, 1),
    (2, 'Install solar panels on HQ roof',   'Procure and install 100 kW solar installation', 25, FALSE, 1),
    (3, 'Switch to LED lighting company-wide', 'Replace all fluorescent fixtures with LEDs', 25, FALSE, 1);

-- Energy metrics for the company
INSERT INTO energy_metrics (company_id, month, year, realized_value, goal_value)
VALUES
    (1, 'January',  2026, 4200.0, 4000.0),
    (1, 'February', 2026, 3850.0, 4000.0);

-- Training metric for the company
INSERT INTO training_metrics (company_id, quarter, year, total_hours)
VALUES (1, 'Q1', 2026, 120.0);

-- User contributions for Alice (user_id = 1)
INSERT INTO user_contributions (user_id, category, points)
VALUES
    (1, 'volunteering', 50.0),
    (1, 'training',     30.0);

-- Reset sequences to avoid PK conflicts on future inserts
SELECT setval('companies_id_seq',         (SELECT MAX(id) FROM companies));
SELECT setval('users_id_seq',             (SELECT MAX(id) FROM users));
SELECT setval('goals_id_seq',             (SELECT MAX(id) FROM goals));
SELECT setval('tasks_id_seq',             (SELECT MAX(id) FROM tasks));
SELECT setval('energy_metrics_id_seq',    (SELECT MAX(id) FROM energy_metrics));
SELECT setval('training_metrics_id_seq',  (SELECT MAX(id) FROM training_metrics));
SELECT setval('user_contributions_id_seq',(SELECT MAX(id) FROM user_contributions));
