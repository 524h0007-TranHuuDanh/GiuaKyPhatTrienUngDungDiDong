const path = require("path");
const Database = require("better-sqlite3");

const serverPath =
    process.argv[2] ||
    process.env.DATABASE_PATH ||
    "./data/coffeestock.db";

const roomPath = process.argv[3];

if (!roomPath) {
    console.error(
        "Usage: node src/scripts/check-consistency.js <server.db> <room.db>"
    );
    process.exit(2);
}

const serverDb = new Database(
    path.resolve(serverPath),
    { readonly: true }
);

const roomDb = new Database(
    path.resolve(roomPath),
    { readonly: true }
);

function fail(message) {
    console.error(`FAIL: ${message}`);
    process.exitCode = 1;
}

try {
    const duplicateServerIds = serverDb.prepare(`
        SELECT id, COUNT(*) AS total
        FROM stock_movement
        GROUP BY id
        HAVING COUNT(*) > 1
    `).all();

    const duplicateRoomIds = roomDb.prepare(`
        SELECT id, COUNT(*) AS total
        FROM stock_movement
        GROUP BY id
        HAVING COUNT(*) > 1
    `).all();

    if (duplicateServerIds.length > 0) {
        fail(
            `Server has duplicate IDs: ${JSON.stringify(duplicateServerIds)}`
        );
    }

    if (duplicateRoomIds.length > 0) {
        fail(
            `Room has duplicate IDs: ${JSON.stringify(duplicateRoomIds)}`
        );
    }

    const serverRows = serverDb.prepare(`
        SELECT
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
            deleted_at,
            created_at,
            updated_at
        FROM stock_movement
        WHERE deleted_at IS NULL
        ORDER BY id
    `).all();

    const roomRows = roomDb.prepare(`
        SELECT
            id,
            productId,
            productName,
            type,
            quantity,
            batchCode,
            partner,
            note,
            status,
            serverVersion,
            isDeleted,
            createdAt,
            updatedAt
        FROM stock_movement
        WHERE isDeleted = 0
        ORDER BY id
    `).all();

    const serverById = new Map(
        serverRows.map(row => [row.id, row])
    );

    const roomById = new Map(
        roomRows.map(row => [row.id, row])
    );

    for (const [id] of serverById) {
        if (!roomById.has(id)) {
            fail(`Missing on Room: ${id}`);
        }
    }

    for (const [id] of roomById) {
        if (!serverById.has(id)) {
            fail(`Missing on server: ${id}`);
        }
    }

    const fields = [
        ["product_id", "productId"],
        ["product_name", "productName"],
        ["type", "type"],
        ["quantity", "quantity"],
        ["batch_code", "batchCode"],
        ["partner", "partner"],
        ["note", "note"],
        ["status", "status"],
        ["version", "serverVersion"],
        ["created_at", "createdAt"],
        ["updated_at", "updatedAt"]
    ];

    for (const [id, server] of serverById) {
        const local = roomById.get(id);

        if (!local) {
            continue;
        }

        for (const [serverField, roomField] of fields) {
            const left = server[serverField];
            const right = local[roomField];

            const equal =
                typeof left === "number" &&
                typeof right === "number"
                    ? Math.abs(left - right) < 0.000001
                    : left === right;

            if (!equal) {
                fail(
                    `Mismatch ${id}.${serverField}: ` +
                    `server=${left} Room=${right}`
                );
            }
        }
    }

    if (process.exitCode !== 1) {
        console.log(
            `PASS: ${serverRows.length} server rows match ` +
            `${roomRows.length} Room rows; no duplicate IDs.`
        );
    }

} finally {
    serverDb.close();
    roomDb.close();
}