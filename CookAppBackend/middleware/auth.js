const jwt = require('jsonwebtoken');

const JWT_SECRET = process.env.JWT_SECRET || 'cookapp_super_secret_key';
if (!process.env.JWT_SECRET) {
    console.warn('⚠ WARNING: JWT_SECRET not set in .env — using default (unsafe for production!)');
}

function authenticateToken(req, res, next) {
    const authHeader = req.headers['authorization'];
    // Format: "Bearer <token>"
    const token = authHeader && authHeader.split(' ')[1];

    if (!token) {
        return res.status(401).json({ error: 'Access Denied. No token provided.' });
    }

    jwt.verify(token, JWT_SECRET, (err, user) => {
        if (err) {
            return res.status(403).json({ error: 'Invalid or expired token.' });
        }
        // Save decoded user payload (id, email) to request object
        req.user = user;
        next();
    });
}

function optionalAuthenticateToken(req, res, next) {
    const authHeader = req.headers['authorization'];
    const token = authHeader && authHeader.split(' ')[1];

    if (!token) {
        return next();
    }

    jwt.verify(token, JWT_SECRET, (err, user) => {
        if (!err) {
            req.user = user;
        }
        next();
    });
}

module.exports = {
    authenticateToken,
    optionalAuthenticateToken,
    JWT_SECRET
};
