class HttpError extends Error {
    constructor(statusCode, code, message, currentMovement = null) {
        super(message);

        this.statusCode = statusCode;
        this.code = code;
        this.currentMovement = currentMovement;
    }
}

module.exports = HttpError;