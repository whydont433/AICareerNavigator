CREATE TABLE professions (
                             id SERIAL PRIMARY KEY,
                             name TEXT NOT NULL UNIQUE
);

CREATE TABLE skills (
                        id SERIAL PRIMARY KEY,
                        name TEXT NOT NULL UNIQUE,
                        category TEXT,
                        canonical_name TEXT
);

CREATE TABLE app_users (
                           id SERIAL PRIMARY KEY,
                           name TEXT NOT NULL UNIQUE,
                           current_profession_id INTEGER REFERENCES professions(id) ON DELETE SET NULL,
                           current_grade TEXT CHECK (current_grade IN ('JUNIOR', 'MIDDLE', 'SENIOR')),
                           available_time_weekly INTEGER NOT NULL DEFAULT 10 CHECK (available_time_weekly >= 0)
);

CREATE TABLE analysis_runs (
                               id SERIAL PRIMARY KEY,
                               profession_name TEXT,
                               selected_grade TEXT,
                               search_area TEXT,
                               publication_period TEXT,
                               vacancy_limit INTEGER,
                               started_at TIMESTAMP NOT NULL DEFAULT NOW(),
                               found_vacancies INTEGER NOT NULL DEFAULT 0,
                               collected_unique INTEGER NOT NULL DEFAULT 0,
                               analyzed_success INTEGER NOT NULL DEFAULT 0,
                               status TEXT NOT NULL DEFAULT 'running' CHECK (status IN ('running', 'completed', 'partial', 'failed')),
                               error_info TEXT
);

CREATE TABLE vacancies (
                           id SERIAL PRIMARY KEY,
                           hh_id TEXT NOT NULL UNIQUE,
                           profession_id INTEGER NOT NULL REFERENCES professions(id),
                           analysis_run_id INTEGER REFERENCES analysis_runs(id) ON DELETE SET NULL,
                           name TEXT NOT NULL,
                           description TEXT,
                           published_at TIMESTAMP,
                           url TEXT,
                           area TEXT,
                           employer TEXT,
                           hh_experience TEXT,
                           grade TEXT NOT NULL CHECK (grade IN ('JUNIOR', 'MIDDLE', 'SENIOR')),
                           fetched_at TIMESTAMP NOT NULL DEFAULT NOW(),
                           ai_status BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE profession_profiles (
                                     id SERIAL PRIMARY KEY,
                                     profession_id INTEGER NOT NULL REFERENCES professions(id) ON DELETE CASCADE,
                                     grade TEXT NOT NULL CHECK (grade IN ('JUNIOR', 'MIDDLE', 'SENIOR')),
                                     sample_size INTEGER NOT NULL DEFAULT 0 CHECK (sample_size >= 0),
                                     computed_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                     low_data BOOLEAN NOT NULL DEFAULT FALSE,
                                     UNIQUE (profession_id, grade)
);

CREATE TABLE profession_profile_skills (
                                           profile_id INTEGER NOT NULL REFERENCES profession_profiles(id) ON DELETE CASCADE,
                                           skill_id INTEGER NOT NULL REFERENCES skills(id) ON DELETE CASCADE,
                                           weight NUMERIC(3,2) NOT NULL CHECK (weight >= 0 AND weight <= 1),
                                           required_level INTEGER NOT NULL CHECK (required_level >= 1 AND required_level <= 6),
                                           PRIMARY KEY (profile_id, skill_id)
);

CREATE TABLE user_skills (
                             user_id INTEGER NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
                             skill_id INTEGER NOT NULL REFERENCES skills(id) ON DELETE CASCADE,
                             level INTEGER NOT NULL CHECK (level >= 0 AND level <= 6),
                             source TEXT NOT NULL CHECK (source IN ('manual', 'ai', 'test')),
                             test_level INTEGER CHECK (test_level >= 0 AND test_level <= 6),
                             test_at TIMESTAMP,
                             updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
                             PRIMARY KEY (user_id, skill_id)
);

CREATE TABLE vacancy_skills (
                                vacancy_id INTEGER NOT NULL REFERENCES vacancies(id) ON DELETE CASCADE,
                                skill_id INTEGER NOT NULL REFERENCES skills(id) ON DELETE CASCADE,
                                need TEXT NOT NULL CHECK (need IN ('required', 'nice')),
                                level INTEGER CHECK (level >= 1 AND level <= 6),
                                confidence NUMERIC(3,2) CHECK (confidence >= 0 AND confidence <= 1),
                                PRIMARY KEY (vacancy_id, skill_id)
);