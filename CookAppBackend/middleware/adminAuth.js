const { User } = require('../models');
const { authenticateToken } = require('./auth');

/**
 * Middleware: xác thực admin.
 * Yêu cầu user đã authenticated VÀ có role === 'admin'.
 */
async function requireAdmin(req, res, next) {
    // authenticateToken đã chạy trước → req.user.id có sẵn
    try {
        const user = await User.findByPk(req.user.id, { attributes: ['id', 'role'] });
        if (!user || user.role !== 'admin') {
            return res.status(403).json({ error: 'Forbidden — Admin only' });
        }
        req.adminUser = user;
        next();
    } catch (e) {
        res.status(500).json({ error: e.message });
    }
}

/**
 * Combined middleware: authenticateToken + requireAdmin
 */
function adminAuth(req, res, next) {
    authenticateToken(req, res, () => {
        requireAdmin(req, res, next);
    });
}

module.exports = { requireAdmin, adminAuth };
