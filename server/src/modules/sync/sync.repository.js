const db = require("../../database/connection");

const statements = {
    getMovement: db.prepare(`
        SELECT *
        FROM stock_movement
        WHERE id = ?
    `),

    insertMovement: db.prepare(`
        INSERT INTO stock_movement (
            id,
            product_id,
            product_name,
            type,
            quantity,
            batch_code,
            partner,
            note,
            status,
            version,
            server_seq,
            deleted_at,
            created_at,
            updated_at
        )
        VALUES (
            @id,
            @product_id,
            @product_name,
            @type,
            @quantity,
            @batch_code,
            @partner,
            @note,
            @status,
            @version,
            @server_seq,
            @deleted_at,
            @created_at,
            @updated_at
        )
    `),

    updateMovement: db.prepare(`
        UPDATE stock_movement
        SET
            product_id = @product_id,
            product_name = @product_name,
            type = @type,
            quantity = @quantity,
            batch_code = @batch_code,
            partner = @partner,
            note = @note,
            status = @status,
            version = @version,
            server_seq = @server_seq,
            deleted_at = @deleted_at,
            updated_at = @updated_at
        WHERE id = @id
    `),

    getLastSequence: db.prepare(`
        SELECT last_server_seq
        FROM sync_meta
        WHERE id = 1
    `),

    updateSequence: db.prepare(`
        UPDATE sync_meta
        SET last_server_seq = ?
        WHERE id = 1
    `),

    insertHistory: db.prepare(`
        INSERT INTO movement_history (
            movement_id,
            version,
            snapshot_json,
            changed_at
        )
        VALUES (?, ?, ?, ?)
    `),

    getIdempotency: db.prepare(`
        SELECT response_json
        FROM idempotency_records
        WHERE mutation_id = ?
    `),

    saveIdempotency: db.prepare(`
        INSERT INTO idempotency_records (
            mutation_id,
            response_json,
            created_at
        )
        VALUES (?, ?, ?)
    `),

    getChanges: db.prepare(`
        SELECT *
        FROM stock_movement
        WHERE server_seq > ?
        ORDER BY server_seq ASC
        LIMIT ?
    `),

    getAllMovements: db.prepare(`
        SELECT *
        FROM stock_movement
        WHERE deleted_at IS NULL
        ORDER BY updated_at DESC
    `)
};

function nextServerSequence() {
    const current = statements.getLastSequence.get();

    const next = current.last_server_seq + 1;

    statements.updateSequence.run(next);

    return next;
}

function getMovement(id) {
    return statements.getMovement.get(id);
}

function insertMovement(movement) {
    return statements.insertMovement.run(movement);
}

function updateMovement(movement) {
    return statements.updateMovement.run(movement);
}

function saveHistory(movement, changedAt) {
    statements.insertHistory.run(
        movement.id,
        movement.version,
        JSON.stringify(movement),
        changedAt
    );
}

function getIdempotency(mutationId) {
    return statements.getIdempotency.get(mutationId);
}

function saveIdempotency(mutationId, response, createdAt) {
    statements.saveIdempotency.run(
        mutationId,
        JSON.stringify(response),
        createdAt
    );
}

function getChanges(afterSeq, limit) {
    return statements.getChanges.all(afterSeq, limit);
}

function getAllMovements() {
    return statements.getAllMovements.all();
}

module.exports = {
    db,
    nextServerSequence,
    getMovement,
    insertMovement,
    updateMovement,
    saveHistory,
    getIdempotency,
    saveIdempotency,
    getChanges,
    getAllMovements
};