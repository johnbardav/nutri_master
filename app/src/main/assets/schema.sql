CREATE TABLE recipes (
 id INTEGER PRIMARY KEY AUTOINCREMENT,
 title TEXT NOT NULL CHECK(length(trim(title))>0),
 category TEXT NOT NULL,
 description TEXT NOT NULL,
 ingredients TEXT NOT NULL,
 steps TEXT NOT NULL,
 image TEXT NOT NULL DEFAULT 'food_bowl',
 video TEXT NOT NULL DEFAULT '',
 video_uri TEXT NOT NULL DEFAULT '',
 minutes INTEGER NOT NULL CHECK(minutes BETWEEN 1 AND 1440),
 base_servings REAL NOT NULL CHECK(base_servings BETWEEN 0.25 AND 24),
 kcal REAL NOT NULL CHECK(kcal BETWEEN 0 AND 5000),
 protein REAL NOT NULL CHECK(protein BETWEEN 0 AND 1000),
 carbs REAL NOT NULL CHECK(carbs BETWEEN 0 AND 1000),
 fat REAL NOT NULL CHECK(fat BETWEEN 0 AND 1000),
 favorite INTEGER NOT NULL DEFAULT 0 CHECK(favorite IN (0,1)),
 saved INTEGER NOT NULL DEFAULT 0 CHECK(saved IN (0,1)),
 built_in INTEGER NOT NULL DEFAULT 0 CHECK(built_in IN (0,1)),
 notes TEXT NOT NULL DEFAULT '',
 created_at INTEGER NOT NULL
);
CREATE TABLE profile (
 id INTEGER PRIMARY KEY CHECK(id=1), name TEXT NOT NULL, role TEXT NOT NULL,
 education TEXT NOT NULL, experience TEXT NOT NULL, bio TEXT NOT NULL,
 photo TEXT NOT NULL DEFAULT ''
);
CREATE TABLE shopping (
 id INTEGER PRIMARY KEY AUTOINCREMENT,
 recipe_id INTEGER REFERENCES recipes(id) ON DELETE SET NULL,
 text TEXT NOT NULL CHECK(length(trim(text))>0),
 checked INTEGER NOT NULL DEFAULT 0 CHECK(checked IN (0,1)),
 UNIQUE(recipe_id,text)
);
CREATE TABLE calculations (
 id INTEGER PRIMARY KEY AUTOINCREMENT,
 recipe_id INTEGER REFERENCES recipes(id) ON DELETE SET NULL,
 title TEXT NOT NULL,
 portions REAL NOT NULL CHECK(portions BETWEEN 0.25 AND 24),
 kcal REAL NOT NULL, protein REAL NOT NULL, carbs REAL NOT NULL, fat REAL NOT NULL,
 created_at INTEGER NOT NULL
);
CREATE INDEX idx_recipes_flags ON recipes(favorite,saved);
CREATE INDEX idx_shopping_recipe ON shopping(recipe_id);
CREATE INDEX idx_calculations_date ON calculations(created_at DESC);
