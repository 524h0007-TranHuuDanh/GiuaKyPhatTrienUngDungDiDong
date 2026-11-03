const fs = require("fs");
const path = require("path");
const Database = require("better-sqlite3");

const config = require("../config/env");

const databasePath = path.resolve(config.databasePath);
const databaseDirectory = path.dirname(databasePath);

if (!fs.existsSync(databaseDirectory)) {
    fs.mkdirSync(databaseDirectory, { recursive: true });
}

const db = new Database(databasePath);

db.pragma("journal_mode = WAL");
db.pragma("foreign_keys = ON");
db.pragma("busy_timeout = 5000");

const migrationPath = path.join(
    __dirname,
    "migrations",
    "001_initial_schema.sql"
);

const migrationSql = fs.readFileSync(migrationPath, "utf8");

db.exec(migrationSql);

module.exports = db;