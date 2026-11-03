function errorMiddleware(err, req, res, next) {
    console.error(err);

    if (res.headersSent) {
        return next(err);
    }

    let statusCode = err.statusCode || err.status || 500;

    let code = err.code || "INTERNAL_SERVER_ERROR";

    let message = err.message || "Lỗi máy chủ";

    // JSON gửi lên không đúng định dạng.
    if (err.type === "entity.parse.failed") {
        statusCode = 400;
        code = "INVALID_JSON";
        message = "Dữ liệu JSON không hợp lệ";
    }

    // Request vượt quá giới hạn kích thước.
    if (err.type === "entity.too.large") {
        statusCode = 413;
        code = "REQUEST_TOO_LARGE";
        message = "Dữ liệu gửi lên quá lớn";
    }

    // Không để lộ thông tin lỗi nội bộ.
    if (statusCode >= 500) {
        code = "INTERNAL_SERVER_ERROR";
        message = "Đã xảy ra lỗi máy chủ";
    }

    return res.status(statusCode).json({
        error: {
            code,
            message
        }
    });
}

module.exports = errorMiddleware;