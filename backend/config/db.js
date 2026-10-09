const path = require('path');
const dotenv = require('dotenv');
dotenv.config({ path: path.resolve(__dirname, '../.env') });
dotenv.config();

let dbType = 'mysql';
let pool = null;

const databaseUrl = process.env.DATABASE_URL || '';

if (databaseUrl.startsWith('postgres://') || databaseUrl.startsWith('postgresql://')) {
  // Cloud PostgreSQL / Supabase mode
  dbType = 'postgres';
  const { Pool } = require('pg');
  pool = new Pool({
    connectionString: databaseUrl,
    ssl: { rejectUnauthorized: false }
  });
} else {
  // Local MySQL 8.0 mode
  dbType = 'mysql';
  const mysql = require('mysql2/promise');
  pool = mysql.createPool({
    host: process.env.DB_HOST || 'localhost',
    port: parseInt(process.env.DB_PORT || '3306', 10),
    user: process.env.DB_USER || 'root',
    password: process.env.DB_PASSWORD || '',
    database: process.env.DB_NAME || 'uniconnect_db',
    waitForConnections: true,
    connectionLimit: 10,
    queueLimit: 0,
    enableKeepAlive: true,
    keepAliveInitialDelay: 0,
    timezone: '+00:00',
    dateStrings: true
  });
}

/**
 * Universal Query Helper
 * Translates query syntax and parameter markers (? -> $1, $2) if running in PostgreSQL.
 */
async function query(sql, params = []) {
  if (dbType === 'postgres') {
    // Convert '?' parameter markers to '$1, $2, ...'
    let paramIndex = 1;
    const pgSql = sql.replace(/\?/g, () => `$${paramIndex++}`);
    const res = await pool.query(pgSql, params);
    return [res.rows, res.fields];
  } else {
    // Native MySQL
    return await pool.query(sql, params);
  }
}

/**
 * Universal Connection Helper for Transactions
 */
async function getConnection() {
  if (dbType === 'postgres') {
    const client = await pool.connect();
    return {
      query: async (sql, params = []) => {
        let paramIndex = 1;
        const pgSql = sql.replace(/\?/g, () => `$${paramIndex++}`);
        const res = await client.query(pgSql, params);
        return [res.rows, res.fields];
      },
      beginTransaction: async () => {
        await client.query('BEGIN');
      },
      commit: async () => {
        await client.query('COMMIT');
      },
      rollback: async () => {
        await client.query('ROLLBACK');
      },
      release: () => {
        client.release();
      }
    };
  } else {
    return await pool.getConnection();
  }
}

/**
 * Connection Health Check
 */
async function testConnection() {
  try {
    const [rows] = await query('SELECT 1 AS status, NOW() AS server_time');
    console.log(`✅ Connected to [${dbType.toUpperCase()}] database successfully.`);
    return true;
  } catch (err) {
    console.error(`❌ Failed to connect to [${dbType.toUpperCase()}] database:`, err.message);
    return false;
  }
}

module.exports = {
  pool,
  query,
  getConnection,
  testConnection,
  getDbType: () => dbType
};
