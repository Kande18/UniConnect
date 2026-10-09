const path = require('path');
const dotenv = require('dotenv');
dotenv.config({ path: path.resolve(__dirname, '.env') });
dotenv.config();

const express = require('express');
const cors = require('cors');
const morgan = require('morgan');
const { query, testConnection, getDbType } = require('./config/db');

// Import Route Handlers
const authRoutes = require('./routes/authRoutes');
const studentRoutes = require('./routes/studentRoutes');
const groupRoutes = require('./routes/groupRoutes');
const requestRoutes = require('./routes/requestRoutes');
const lecturerRoutes = require('./routes/lecturerRoutes');
const syncRoutes = require('./routes/syncRoutes');

const app = express();
const PORT = process.env.PORT || 5000;

// Global Middleware
app.use(cors());
app.use(express.json());
if (process.env.NODE_ENV !== 'test') {
  app.use(morgan('dev'));
}

// API Health Check
app.get('/api/health', async (req, res) => {
  try {
    const [result] = await query('SELECT 1 AS status, NOW() AS serverTime');
    res.status(200).json({
      success: true,
      message: 'UniConnect REST API service is healthy.',
      data: {
        status: 'UP',
        database: 'CONNECTED',
        databaseEngine: getDbType().toUpperCase(),
        dbTime: result[0].serverTime || result[0].servertime,
        port: PORT,
        timestamp: new Date().toISOString()
      }
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      error: {
        code: 'DATABASE_ERROR',
        message: 'Database connectivity failure: ' + error.message
      }
    });
  }
});

// Mount Routes
app.use('/api/auth', authRoutes);
app.use('/api/students', studentRoutes);
app.use('/api/groups', groupRoutes);
app.use('/api/requests', requestRoutes);
app.use('/api/lecturer', lecturerRoutes);
app.use('/api/sync', syncRoutes);

// 404 Handler for undefined routes
app.use((req, res) => {
  res.status(404).json({
    success: false,
    error: {
      code: 'NOT_FOUND',
      message: `Cannot ${req.method} ${req.originalUrl}`
    }
  });
});

// Global Error Handler
app.use((err, req, res, next) => {
  console.error('Unhandled server error:', err);
  res.status(err.status || 500).json({
    success: false,
    error: {
      code: err.code || 'SERVER_ERROR',
      message: err.message || 'An unexpected internal error occurred.'
    }
  });
});

// Start Server
if (process.env.NODE_ENV !== 'test') {
  app.listen(PORT, async () => {
    console.log(`🚀 UniConnect Server listening on port ${PORT}`);
    await testConnection();
  });
}

module.exports = app;
