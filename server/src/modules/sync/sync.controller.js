const syncService = require("./sync.service");

function push(req, res, next) {
    try {
        const results = syncService.pushChanges(req.body);

        const hasConflict = results.some(
            result => !result.ok &&
                result.error.code === "VERSION_CONFLICT"
        );

        if (hasConflict) {
            return res.status(409).json({
                results
            });
        }

        res.status(200).json({
            results
        });
    } catch (error) {
        next(error);
    }
}

function pull(req, res, next) {
    try {
        const afterSeq = Number(req.query.after_seq || 0);
        const limit = Number(req.query.limit || 100);

        if (
            !Number.isSafeInteger(afterSeq) ||
            afterSeq < 0 ||
            !Number.isInteger(limit) ||
            limit < 1 ||
            limit > 500
        ) {
            return res.status(400).json({
                error: {
                    code: "INVALID_PULL_PARAMETERS",
                    message: "after_seq hoặc limit không hợp lệ"
                }
            });
        }

        const result = syncService.pullChanges(afterSeq, limit);

        res.status(200).json(result);
    } catch (error) {
        next(error);
    }
}

function getMovements(req, res, next) {
    try {
        res.status(200).json({
            items: syncService.getMovements()
        });
    } catch (error) {
        next(error);
    }
}

module.exports = {
    push,
    pull,
    getMovements
};