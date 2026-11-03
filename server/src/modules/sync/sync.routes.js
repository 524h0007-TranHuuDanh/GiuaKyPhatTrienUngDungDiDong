const express = require("express");

const controller = require("./sync.controller");
const verifyApiKey = require("../../middleware/apiKey.middleware");

const router = express.Router();

router.use(verifyApiKey);

router.post("/push", controller.push);

router.get("/pull", controller.pull);

router.get("/movements", controller.getMovements);

module.exports = router;