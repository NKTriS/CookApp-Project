const express = require('express');
const router = express.Router();
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const { User, ShoppingList } = require('../models');
const { authenticateToken, JWT_SECRET } = require('../middleware/auth');

// POST /api/auth/register
router.post('/register', async (req, res) => {
    try {
        const { email, password, fullName, phoneNumber, address } = req.body;

        if (!email || !password || !fullName) {
            return res.status(400).json({ error: 'Email, password, and fullName are required.' });
        }

        // Password strength validation
        if (password.length < 6) {
            return res.status(400).json({ error: 'Mật khẩu phải có ít nhất 6 ký tự.' });
        }
        if (!/[a-zA-Z]/.test(password) || !/[0-9]/.test(password)) {
            return res.status(400).json({ error: 'Mật khẩu phải có ít nhất 1 chữ cái và 1 chữ số.' });
        }

        const existingUser = await User.findOne({ where: { email } });
        if (existingUser) {
            return res.status(400).json({ error: 'Email is already registered.' });
        }

        const hashedPassword = await bcrypt.hash(password, 10);

        const newUser = await User.create({
            email,
            password: hashedPassword,
            fullName,
            phoneNumber,
            address,
            avatarUrl: null
        });

        // Tự động tạo một ShoppingList trống cho User mới
        await ShoppingList.create({ user_id: newUser.id });

        const token = jwt.sign({ id: newUser.id, email: newUser.email, role: newUser.role || 'user' }, JWT_SECRET, { expiresIn: '30d' });

        res.status(201).json({
            message: 'User registered successfully',
            token: token,
            user: {
                id: newUser.id,
                email: newUser.email,
                fullName: newUser.fullName
            }
        });
    } catch (e) {
        res.status(500).json({ error: e.message });
    }
});

// POST /api/auth/login
router.post('/login', async (req, res) => {
    try {
        const { email, password } = req.body;

        const user = await User.findOne({ where: { email } });
        if (!user) {
            return res.status(401).json({ error: 'Email hoặc mật khẩu không đúng.' });
        }

        const isValidPassword = await bcrypt.compare(password, user.password);
        if (!isValidPassword) {
            return res.status(401).json({ error: 'Email hoặc mật khẩu không đúng.' });
        }

        const token = jwt.sign({ id: user.id, email: user.email, role: user.role || 'user' }, JWT_SECRET, { expiresIn: '30d' });

        res.json({
            message: 'Login successful',
            token: token,
            user: {
                id: user.id,
                email: user.email,
                fullName: user.fullName,
                address: user.address,
                phoneNumber: user.phoneNumber,
                avatarUrl: user.avatarUrl
            }
        });

    } catch (e) {
        res.status(500).json({ error: e.message });
    }
});

// GET /api/auth/me
router.get('/me', authenticateToken, async (req, res) => {
    try {
        const user = await User.findByPk(req.user.id, {
            attributes: { exclude: ['password'] }
        });
        if (!user) return res.status(404).json({ error: 'User not found' });

        res.json(user);
    } catch (e) {
        res.status(500).json({ error: e.message });
    }
});

// PATCH /api/auth/profile — cập nhật thông tin cá nhân
router.patch('/profile', authenticateToken, async (req, res) => {
    try {
        const { fullName, phoneNumber, address } = req.body;
        const user = await User.findByPk(req.user.id);
        if (!user) return res.status(404).json({ error: 'User not found' });

        if (fullName)    user.fullName    = fullName;
        if (phoneNumber !== undefined) user.phoneNumber = phoneNumber;
        if (address !== undefined)    user.address     = address;
        await user.save();

        res.json({
            message: 'Profile updated',
            user: {
                id: user.id,
                email: user.email,
                fullName: user.fullName,
                phoneNumber: user.phoneNumber,
                address: user.address
            }
        });
    } catch (e) { res.status(500).json({ error: e.message }); }
});

module.exports = router;
