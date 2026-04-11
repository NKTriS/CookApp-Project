const express = require('express');
const router = express.Router();
const crypto = require('crypto');
const { Order } = require('../models');

// Configuration
const vnp_TmnCode = process.env.VNP_TMNCODE || 'VNPAY_TMN';
const vnp_HashSecret = process.env.VNP_HASHSECRET || 'VNPAY_HASH';
const vnp_Url = process.env.VNP_URL || 'https://sandbox.vnpayment.vn/paymentv2/vpcpay.html';
const vnp_ReturnUrl = process.env.VNP_RETURNURL || 'http://10.0.2.2:3000/api/payment/vnpay/return';

// Helper to sort objects by property names alphabetically
function sortObject(obj) {
    let sorted = {};
    let str = [];
    let key;
    for (key in obj) {
        if (obj.hasOwnProperty(key)) {
            str.push(encodeURIComponent(key));
        }
    }
    str.sort();
    for (key = 0; key < str.length; key++) {
        sorted[str[key]] = encodeURIComponent(obj[str[key]]).replace(/%20/g, "+");
    }
    return sorted;
}

// ─────────────────────────────────────────────
// POST /api/payment/vnpay/create_url
// Creates the VNPay checkout URL
// ─────────────────────────────────────────────
router.post('/create_url', async (req, res) => {
    try {
        const { orderId, amount, bankCode } = req.body;
        
        let ipAddr = req.headers['x-forwarded-for'] ||
            req.connection.remoteAddress ||
            req.socket.remoteAddress ||
            req.connection.socket.remoteAddress || '127.0.0.1';

        const order = await Order.findByPk(orderId);
        if (!order) {
             return res.status(404).json({ error: 'Order not found' });
        }

        // ✅ Build return URL dynamically from request host
        // Lấy IP từ request để return URL luôn đúng dù IP thay đổi
        const requestHost = req.headers['x-forwarded-host'] || req.headers['host'] || `localhost:3000`;
        const protocol = req.headers['x-forwarded-proto'] || 'http';
        const dynamicReturnUrl = `${protocol}://${requestHost}/api/payment/vnpay/return`;
        console.log(`[VNPay] Dynamic return URL: ${dynamicReturnUrl}`);

        const date = new Date();
        const createDate = date.getFullYear() +
            ('0' + (date.getMonth() + 1)).slice(-2) +
            ('0' + date.getDate()).slice(-2) +
            ('0' + date.getHours()).slice(-2) +
            ('0' + date.getMinutes()).slice(-2) +
            ('0' + date.getSeconds()).slice(-2);
        
        const expireDateObj = new Date(date.getTime() + 15 * 60000); // 15 mins expiry
        const expireDate = expireDateObj.getFullYear() +
            ('0' + (expireDateObj.getMonth() + 1)).slice(-2) +
            ('0' + expireDateObj.getDate()).slice(-2) +
            ('0' + expireDateObj.getHours()).slice(-2) +
            ('0' + expireDateObj.getMinutes()).slice(-2) +
            ('0' + expireDateObj.getSeconds()).slice(-2);

        // Required VNPay parameters
        let vnp_Params = {};
        vnp_Params['vnp_Version'] = '2.1.0';
        vnp_Params['vnp_Command'] = 'pay';
        vnp_Params['vnp_TmnCode'] = vnp_TmnCode;
        vnp_Params['vnp_Locale'] = 'vn';
        vnp_Params['vnp_CurrCode'] = 'VND';
        vnp_Params['vnp_TxnRef'] = orderId + '_' + date.getTime();
        vnp_Params['vnp_OrderInfo'] = 'Thanh toan don hang COOKAPP' + orderId;
        vnp_Params['vnp_OrderType'] = 'other';
        vnp_Params['vnp_Amount'] = amount * 100; // VNPay requires multiplying by 100
        vnp_Params['vnp_ReturnUrl'] = dynamicReturnUrl;
        vnp_Params['vnp_IpAddr'] = ipAddr;
        vnp_Params['vnp_CreateDate'] = createDate;
        vnp_Params['vnp_ExpireDate'] = expireDate;

        if (bankCode !== null && bankCode !== '' && bankCode !== undefined) {
            vnp_Params['vnp_BankCode'] = bankCode;
        }

        vnp_Params = sortObject(vnp_Params);

        const querystring = require('qs');
        let signData = querystring.stringify(vnp_Params, { encode: false });

        let hmac = crypto.createHmac("sha512", vnp_HashSecret);
        let signed = hmac.update(Buffer.from(signData, 'utf-8')).digest("hex"); 
        vnp_Params['vnp_SecureHash'] = signed;

        let vnpUrl = vnp_Url + '?' + querystring.stringify(vnp_Params, { encode: false });

        console.log("=== VNPAY DEBUG ===");
        console.log("signData:", signData);
        console.log("HashSecret length:", vnp_HashSecret.length);
        console.log("vnpUrl:", vnpUrl);
        console.log("===================");

        res.json({ paymentUrl: vnpUrl });
    } catch (e) {
        console.error('[VNPay Create URL] Error:', e);
        res.status(500).json({ error: e.message });
    }
});

// ─────────────────────────────────────────────
// GET /api/payment/vnpay/return
// The return URL after VNPay transaction finishes
// ─────────────────────────────────────────────
router.get('/return', async (req, res) => {
    try {
        let vnp_Params = req.query;
        let secureHash = vnp_Params['vnp_SecureHash'];

        delete vnp_Params['vnp_SecureHash'];
        delete vnp_Params['vnp_SecureHashType'];

        vnp_Params = sortObject(vnp_Params);
        
        const querystring = require('qs');
        let signData = querystring.stringify(vnp_Params, { encode: false });
        let hmac = crypto.createHmac("sha512", vnp_HashSecret);
        let signed = hmac.update(Buffer.from(signData, 'utf-8')).digest("hex");     

        // Prepare simple HTML response for the WebView
        if(secureHash === signed){
            // Extract orderId from txnRef (format: orderId_timestamp)
            let orderId = vnp_Params['vnp_TxnRef'] ? vnp_Params['vnp_TxnRef'].split('_')[0] : null;

            if (vnp_Params['vnp_ResponseCode'] === '00') { // success
                // ✅ Cập nhật đơn hàng ngay tại /return (hỗ trợ dev local khi IPN không tới được)
                if (orderId) {
                    try {
                        const order = await Order.findByPk(orderId);
                        if (order && order.paymentStatus !== 'paid') {
                            order.paymentStatus = 'paid';
                            order.status = 'Chờ xác nhận';
                            order.paidAt = new Date();
                            order.sepayTransactionId = vnp_Params['vnp_TransactionNo'] || null;
                            await order.save();
                            console.log(`[VNPay Return] ✅ Order #${orderId} marked as PAID`);
                        }
                    } catch (err) {
                        console.error('[VNPay Return] Error updating order:', err.message);
                    }
                }

                res.send(`
                    <html>
                    <head><meta name="viewport" content="width=device-width, initial-scale=1"></head>
                    <body style="display:flex; flex-direction:column; align-items:center; justify-content:center; height:100vh; font-family:sans-serif; text-align:center;">
                        <h2 style="color:#00B14F;">Thanh Toán Thành Công</h2>
                        <p>Đơn hàng của bạn đã được xử lý thành công.</p>
                        <p style="color:gray; font-size:12px;">Đang chuyển hướng...</p>
                    </body>
                    </html>
                `);
            } else {
                // ❌ Thanh toán thất bại / bị hủy
                if (orderId) {
                    try {
                        const order = await Order.findByPk(orderId);
                        if (order && order.paymentStatus === 'pending') {
                            order.paymentStatus = 'expired';
                            order.status = 'Đã hủy';
                            order.cancelReason = 'Người dùng hủy thanh toán VNPay';
                            order.cancelledAt = new Date();
                            await order.save();
                            console.log(`[VNPay Return] ❌ Order #${orderId} cancelled`);
                        }
                    } catch (err) {
                        console.error('[VNPay Return] Error cancelling order:', err.message);
                    }
                }

                res.send(`
                    <html>
                    <head><meta name="viewport" content="width=device-width, initial-scale=1"></head>
                    <body style="display:flex; flex-direction:column; align-items:center; justify-content:center; height:100vh; font-family:sans-serif; text-align:center;">
                        <h2 style="color:#FF383C;">Thanh Toán Lỗi / Bị Hủy</h2>
                        <p>Mã lỗi: ${vnp_Params['vnp_ResponseCode']}</p>
                    </body>
                    </html>
                `);
            }
        } else {
            res.send('<h1>Lỗi xác thực chữ ký VNPay</h1>');
        }
    } catch (e) {
        res.status(500).send('Error');
    }
});

// ─────────────────────────────────────────────
// GET /api/payment/vnpay/ipn
// The Webhook IPN that VNPay server queries
// ─────────────────────────────────────────────
router.get('/ipn', async (req, res) => {
    try {
        let vnp_Params = req.query;
        let secureHash = vnp_Params['vnp_SecureHash'];
        
        delete vnp_Params['vnp_SecureHash'];
        delete vnp_Params['vnp_SecureHashType'];

        vnp_Params = sortObject(vnp_Params);
        
        const querystring = require('qs');
        let signData = querystring.stringify(vnp_Params, { encode: false });
        let hmac = crypto.createHmac("sha512", vnp_HashSecret);
        let signed = hmac.update(Buffer.from(signData, 'utf-8')).digest("hex");     

        let orderId = vnp_Params['vnp_TxnRef'].split('_')[0];

        if(secureHash === signed){
            const order = await Order.findByPk(orderId);
            if (!order) {
                return res.json({ RspCode: '01', Message: 'Order not found' });
            }

            // Check if order already updated
            if (order.paymentStatus === 'paid') {
                return res.json({ RspCode: '02', Message: 'Order already confirmed' });
            }

            if (vnp_Params['vnp_ResponseCode'] === '00' && vnp_Params['vnp_TransactionStatus'] === '00') {
                // Success
                order.paymentStatus = 'paid';
                order.status = 'Chờ xác nhận';
                order.paidAt = new Date();
                order.sepayTransactionId = vnp_Params['vnp_TransactionNo'];
                await order.save();
                
                return res.json({ RspCode: '00', Message: 'Confirm Success' });
            } else {
                // Fail
                order.paymentStatus = 'expired';
                order.status = 'Đã hủy';
                order.cancelReason = 'Thanh toán VNPay lỗi/hủy';
                await order.save();
                
                return res.json({ RspCode: '00', Message: 'Confirm Success' });
            }
        } else {
             return res.json({ RspCode: '97', Message: 'Checksum failed' });
        }
    } catch (e) {
        return res.json({ RspCode: '99', Message: 'Unknown error' });
    }
});

module.exports = router;
