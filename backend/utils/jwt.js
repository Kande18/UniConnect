const jwt = require('jsonwebtoken');

const JWT_SECRET = process.env.JWT_SECRET || 'uniconnect_jwt_secret_key_2026_super_secure!';
const JWT_EXPIRES_IN = parseInt(process.env.JWT_EXPIRES_IN || '86400', 10);

function signToken(payload) {
  return jwt.sign(payload, JWT_SECRET, { expiresIn: JWT_EXPIRES_IN });
}

function verifyToken(token) {
  return jwt.verify(token, JWT_SECRET);
}

module.exports = {
  signToken,
  verifyToken,
  JWT_EXPIRES_IN
};
