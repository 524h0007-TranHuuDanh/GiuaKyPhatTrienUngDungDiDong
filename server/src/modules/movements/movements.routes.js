const express = require("express");

const controller = require("./movements.controller");
const verifyApiKey = require("../../middleware/apiKey.middleware");

const router = express.Router();

// Tất cả API CRUD đều yêu cầu API key.
router.use(verifyApiKey);

// GET /api/v1/movements
router.get("/", controller.getAll);

// GET /api/v1/movements/:id
router.get("/:id", controller.getById);

// POST /api/v1/movements
router.post("/", controller.create);

// PUT /api/v1/movements/:id
router.put("/:id", controller.update);

// DELETE /api/v1/movements/:id
router.delete("/:id", controller.remove);

module.exports = router;