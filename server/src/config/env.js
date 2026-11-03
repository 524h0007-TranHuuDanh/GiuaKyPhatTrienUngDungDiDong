const dotenv = require("dotenv");

dotenv.config();

const config = {
    port: Number(process.env.PORT) || 8080,
    host: process.env.HOST || "0.0.0.0",
    apiKey: process.env.API_KEY || "",
    databasePath: process.env.DATABASE_PATH || "./data/coffeestock.db"
};

if (!config.apiKey) {
    throw new Error("API_KEY chưa được cấu hình trong file .env");
}

module.exports = config;