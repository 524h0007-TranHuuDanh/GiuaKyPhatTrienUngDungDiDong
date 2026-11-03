const config = require("../config/env");

function verifyApiKey(req, res, next) {
    const apiKey = req.header("X-API-Key");

    if (!apiKey || apiKey !== config.apiKey) {
        return res.status(401).json({
            error: {
                code: "INVALID_API_KEY",
                message: "API key không hợp lệ"
            }
        });
    }

    next();
}

module.exports = verifyApiKey;