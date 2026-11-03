CREATE TABLE IF NOT EXISTS stock_movement (
    id TEXT PRIMARY KEY,

    product_id TEXT NOT NULL,
    product_name TEXT NOT NULL,

    type TEXT NOT NULL CHECK (type IN ('IN', 'OUT')),
    quantity REAL NOT NULL CHECK (quantity > 0),

    batch_code TEXT NOT NULL DEFAULT '',
    partner TEXT NOT NULL DEFAULT '',
    note TEXT NOT NULL DEFAULT '',

    status TEXT NOT NULL CHECK (status IN ('DRAFT', 'CONFIRMED')),

    version INTEGER NOT NULL DEFAULT 1,
    server_seq INTEGER NOT NULL UNIQUE,

    deleted_at TEXT,

    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_movement_server_seq
ON stock_movement(server_seq);

CREATE INDEX IF NOT EXISTS idx_movement_updated_at
ON stock_movement(updated_at);


CREATE TABLE IF NOT EXISTS movement_history (
    history_id INTEGER PRIMARY KEY AUTOINCREMENT,

    movement_id TEXT NOT NULL,
    version INTEGER NOT NULL,

    snapshot_json TEXT NOT NULL,
    changed_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_history_movement
ON movement_history(movement_id);


CREATE TABLE IF NOT EXISTS idempotency_records (
    mutation_id TEXT PRIMARY KEY,

    response_json TEXT NOT NULL,
    created_at TEXT NOT NULL
);


CREATE TABLE IF NOT EXISTS sync_meta (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    last_server_seq INTEGER NOT NULL DEFAULT 0
);

INSERT OR IGNORE INTO sync_meta (id, last_server_seq)
VALUES (1, 0);