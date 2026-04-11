const express = require('express');
const router = express.Router();
const { Order } = require('../models');
const { authenticateToken } = require('../middleware/auth');
const { adminAuth } = require('../middleware/adminAuth');

const SEPAY_API_KEY = process.env.SEPAY_API_KEY || '';

// ─────────────────────────────────────────────
// POST /api/payment/sepay-webhook
// SePay gọi endpoint này khi có giao dịch mới
// ─────────────────────────────────────────────
router.post('/sepay-webhook', async (req, res) => {
    try {
        // 1. Xác thực API Key từ SePay
        const authHeader = req.headers['authorization'] || '';
        const incomingKey = authHeader.replace('Apikey ', '').trim();

        if (!SEPAY_API_KEY) {
            console.log('[SePay Webhook] ⚠️ SEPAY_API_KEY not configured — rejecting request');
            return res.status(503).json({ success: false, error: 'Payment webhook not configured' });
        }

        if (incomingKey !== SEPAY_API_KEY) {
            console.log('[SePay Webhook] ❌ Invalid API Key');
            return res.status(401).json({ success: false, error: 'Invalid API Key' });
        }

        // 2. Parse SePay payload
        const {
            id: sepayTxId,       // SePay transaction ID
            transferType,         // "in" = tiền vào
            transferAmount,       // Số tiền (VND)
            content,              // Nội dung chuyển khoản
            code,                 // Mã thanh toán SePay detect
            description,          // Full SMS content
            transactionDate
        } = req.body;

        console.log('[SePay Webhook] 📨 Received:', {
            sepayTxId, transferType, transferAmount,
            content, code, description
        });

        // 3. Chỉ xử lý giao dịch "tiền vào"
        if (transferType !== 'in') {
            console.log('[SePay Webhook] ⏭️ Skipping non-deposit transaction');
            return res.json({ success: true });
        }

        // 4. Tìm paymentCode trong nội dung CK
        // Ưu tiên dùng `code` (SePay tự detect), fallback parse từ `content`
        let paymentCode = null;

        if (code) {
            paymentCode = code.toUpperCase();
        }

        if (!paymentCode && content) {
            const match = content.toUpperCase().match(/COOKAPP(\d+)/);
            if (match) {
                paymentCode = 'COOKAPP' + match[1];
            }
        }

        if (!paymentCode && description) {
            const match = description.toUpperCase().match(/COOKAPP(\d+)/);
            if (match) {
                paymentCode = 'COOKAPP' + match[1];
            }
        }

        if (!paymentCode) {
            console.log('[SePay Webhook] ⚠️ No payment code found in transfer content');
            return res.json({ success: true }); // Vẫn trả success để SePay không retry
        }

        // 5. Tìm đơn hàng theo paymentCode
        const order = await Order.findOne({
            where: { paymentCode: paymentCode, paymentStatus: 'pending' }
        });

        if (!order) {
            console.log(`[SePay Webhook] ⚠️ No pending order found for code: ${paymentCode}`);
            return res.json({ success: true });
        }

        // 6. Chống duplicate: kiểm tra sepayTransactionId
        if (sepayTxId) {
            const existingTx = await Order.findOne({
                where: { sepayTransactionId: String(sepayTxId) }
            });
            if (existingTx) {
                console.log(`[SePay Webhook] ⚠️ Duplicate transaction: ${sepayTxId}`);
                return res.json({ success: true });
            }
        }

        // 7. Kiểm tra số tiền (cho phép sai lệch ±1000đ do bank fees)
        const expectedAmount = (order.totalPrice || 0) + (order.shippingFee || 0);
        const amountDiff = Math.abs(transferAmount - expectedAmount);
        if (amountDiff > 1000) {
            console.log(`[SePay Webhook] ⚠️ Amount mismatch: expected ${expectedAmount}, got ${transferAmount}`);
            // Vẫn xác nhận nếu số tiền >= expected (user chuyển dư)
            if (transferAmount < expectedAmount - 1000) {
                return res.json({ success: true }); // Không confirm nếu thiếu tiền
            }
        }

        // 8. ✅ Xác nhận thanh toán thành công!
        order.paymentStatus = 'paid';
        order.paidAt = new Date();
        order.sepayTransactionId = sepayTxId ? String(sepayTxId) : null;
        order.status = 'Chờ xác nhận'; // Đơn hàng chuyển sang xử lý
        await order.save();

        console.log(`[SePay Webhook] ✅ Order #${order.id} (${paymentCode}) marked as PAID! Amount: ${transferAmount}`);

        res.json({ success: true });
    } catch (e) {
        console.error('[SePay Webhook] ❌ Error:', e.message);
        res.status(500).json({ success: false, error: e.message });
    }
});

// ─────────────────────────────────────────────
// GET /api/payment/status/:orderId
// App polling để kiểm tra trạng thái thanh toán
// ─────────────────────────────────────────────
router.get('/status/:orderId', authenticateToken, async (req, res) => {
    try {
        const order = await Order.findOne({
            where: { id: req.params.orderId, user_id: req.user.id }
        });

        if (!order) {
            return res.status(404).json({ error: 'Order not found' });
        }

        res.json({
            paymentStatus: order.paymentStatus || 'none',
            paymentCode: order.paymentCode,
            paidAt: order.paidAt
        });
    } catch (e) {
        res.status(500).json({ error: e.message });
    }
});

// ─────────────────────────────────────────────
// POST /api/payment/simulate-webhook
// 🧪 CHỈ DÙNG KHI DEV/TEST — giả lập SePay webhook
// ─────────────────────────────────────────────
router.post('/simulate-webhook', adminAuth, async (req, res) => {
    // Chỉ cho phép trong development mode
    if (process.env.NODE_ENV === 'production') {
        return res.status(403).json({ error: 'Not available in production' });
    }

    try {
        const { orderId } = req.body;
        if (!orderId) {
            return res.status(400).json({ error: 'orderId is required' });
        }

        const order = await Order.findByPk(orderId);
        if (!order) {
            return res.status(404).json({ error: 'Order not found' });
        }

        if (order.paymentStatus === 'paid') {
            return res.json({ message: 'Order already paid', paymentStatus: 'paid' });
        }

        // Giả lập thanh toán thành công
        order.paymentStatus = 'paid';
        order.paidAt = new Date();
        order.sepayTransactionId = 'SIM_' + Date.now();
        await order.save();

        console.log(`[Simulate] ✅ Order #${order.id} marked as PAID (simulated)`);

        res.json({
            success: true,
            message: `Order #${orderId} payment confirmed (simulated)`,
            paymentStatus: 'paid',
            paidAt: order.paidAt
        });
    } catch (e) {
        res.status(500).json({ error: e.message });
    }
});

module.exports = router;
