require('dotenv').config();

const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const rateLimit = require('express-rate-limit');
const sequelize = require('./config/database');

const app = express();
const PORT = process.env.PORT || 3000;

app.use(helmet());
app.use(cors());
app.use(express.json({ limit: '10mb' }));

app.use((req, res, next) => {
    console.log(`[REQUEST] ${req.method} ${req.url} (IP: ${req.ip})`);
    next();
});

// ── URL Rewriter — convert relative paths → absolute URLs based on request host ──
const urlRewriter = require('./middleware/urlRewriter');
app.use(urlRewriter);

// Serve static files (videos, images, etc.) from public/
app.use(express.static('public'));

// ── Rate Limiting ────────────────────────────────────────────────────────────
// Auth endpoints: 10 attempts per 15 minutes (chống brute force)
const authLimiter = rateLimit({
    windowMs: 15 * 60 * 1000,
    max: 10,
    message: { error: 'Quá nhiều lần thử — vui lòng đợi 15 phút.' },
    standardHeaders: true,
    legacyHeaders: false,
});

// AI Chat endpoint: 30 requests per minute (chống spam, tiết kiệm API)
const chatLimiter = rateLimit({
    windowMs: 60 * 1000,
    max: 30,
    message: { error: 'Bạn chat quá nhiều — vui lòng chờ 1 phút.' },
    standardHeaders: true,
    legacyHeaders: false,
});

// General API: 200 requests per minute
const apiLimiter = rateLimit({
    windowMs: 60 * 1000,
    max: 200,
    standardHeaders: true,
    legacyHeaders: false,
});

app.use('/api/auth/login', authLimiter);
app.use('/api/auth/register', authLimiter);
app.use('/api/chat', chatLimiter);
app.use('/api', apiLimiter);

// ── Modular API Routes ───────────────────────────────────────────────────────
app.use('/api/auth',       require('./routes/auth'));
app.use('/api',            require('./routes/recipes'));    // recipes, categories, ingredients, reviews
app.use('/api/community',  require('./routes/community')); // posts, comments, likes, saves
app.use('/api',            require('./routes/user'));       // favorites, shopping, notifications, orders, profile
app.use('/api',            require('./routes/stores'));     // store-products
app.use('/api',            require('./routes/chat'));       // AI chatbot + health check
app.use('/api/admin',      require('./routes/admin'));      // Admin panel API
app.use('/api/payment/vnpay', require('./routes/vnpay'));    // VNPay sandbox webhook & return
app.use('/api/payment',    require('./routes/payment'));    // SePay webhook + payment status

// Basic test route
app.get('/', (req, res) => {
    res.json({ message: 'Welcome to CookApp Backend API!' });
});

// ── Global Error Handler ─────────────────────────────────────────────────────
app.use((err, req, res, next) => {
    console.error(`[ERROR] ${req.method} ${req.url}:`, err.message);
    res.status(err.status || 500).json({
        error: process.env.NODE_ENV === 'production'
            ? 'Internal Server Error'
            : err.message
    });
});

// Sync database and start server
sequelize.sync().then(() => {
    console.log('Database connected and synced.');
    app.listen(PORT, '0.0.0.0', () => {
        console.log(`Server is running on http://0.0.0.0:${PORT}`);
        
        // Dynamic IP Detection for easier testing on physical devices
        const os = require('os');
        const networkInterfaces = os.networkInterfaces();
        let lanIp = 'localhost';
        for (const interfaceName in networkInterfaces) {
            const interfaces = networkInterfaces[interfaceName];
            for (const iface of interfaces) {
                if (iface.family === 'IPv4' && !iface.internal) {
                    lanIp = iface.address;
                    break;
                }
            }
        }
        console.log(`LAN access: http://${lanIp}:${PORT}`);
    });
}).catch(err => {
    console.error('Failed to sync database:', err);
});

// Cập nhật