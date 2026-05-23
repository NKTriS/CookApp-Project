const express = require('express');
const router = express.Router();
const crypto = require('crypto');
const { Order } = require('../models');

// Cấu hình VNPay
const vnp_TmnCode = process.env.VNP_TMNCODE || 'VNPAY_TMN';
const vnp_HashSecret = process.env.VNP_HASHSECRET || 'VNPAY_HASH';
const vnp_Url = process.env.VNP_URL || 'https://sandbox.vnpayment.vn/paymentv2/vpcpay.html';
const vnp_ReturnUrl = process.env.VNP_RETURNURL || 'http://10.0.2.2:3000/api/payment/vnpay/return';

// Hàm hỗ trợ sắp xếp các tham số của object theo thứ tự bảng chữ cái để tạo chuỗi ký số
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
// Khởi tạo liên kết thanh toán VNPay (Checkout URL)
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
             return res.status(404).json({ error: 'Không tìm thấy đơn hàng' });
        }

        // ✅ Tự động lấy cấu hình Host để sinh Return URL động (giúp không bị lỗi IP khi chạy LAN)
        const requestHost = req.headers['x-forwarded-host'] || req.headers['host'] || `localhost:3000`;
        const protocol = req.headers['x-forwarded-proto'] || 'http';
        const dynamicReturnUrl = `${protocol}://${requestHost}/api/payment/vnpay/return`;
        console.log(`[VNPay] Đường dẫn return động: ${dynamicReturnUrl}`);

        const date = new Date();
        const createDate = date.getFullYear() +
            ('0' + (date.getMonth() + 1)).slice(-2) +
            ('0' + date.getDate()).slice(-2) +
            ('0' + date.getHours()).slice(-2) +
            ('0' + date.getMinutes()).slice(-2) +
            ('0' + date.getSeconds()).slice(-2);
        
        const expireDateObj = new Date(date.getTime() + 15 * 60000); // Hết hạn trong 15 phút
        const expireDate = expireDateObj.getFullYear() +
            ('0' + (expireDateObj.getMonth() + 1)).slice(-2) +
            ('0' + expireDateObj.getDate()).slice(-2) +
            ('0' + expireDateObj.getHours()).slice(-2) +
            ('0' + expireDateObj.getMinutes()).slice(-2) +
            ('0' + expireDateObj.getSeconds()).slice(-2);

        // Các tham số bắt buộc gửi sang cổng thanh toán VNPay
        let vnp_Params = {};
        vnp_Params['vnp_Version'] = '2.1.0';
        vnp_Params['vnp_Command'] = 'pay';
        vnp_Params['vnp_TmnCode'] = vnp_TmnCode;
        vnp_Params['vnp_Locale'] = 'vn';
        vnp_Params['vnp_CurrCode'] = 'VND';
        vnp_Params['vnp_TxnRef'] = orderId + '_' + date.getTime();
        vnp_Params['vnp_OrderInfo'] = 'Thanh toan don hang COOKAPP ' + orderId;
        vnp_Params['vnp_OrderType'] = 'other';
        vnp_Params['vnp_Amount'] = amount * 100; // VNPay yêu cầu nhân 100 để quy đổi xu lẻ
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

        console.log("=== VNPAY GỠ LỖI ===");
        console.log("Dữ liệu ký:", signData);
        console.log("Độ dài khóa bí mật:", vnp_HashSecret.length);
        console.log("URL Thanh toán tạo ra:", vnpUrl);
        console.log("====================");

        res.json({ paymentUrl: vnpUrl });
    } catch (e) {
        console.error('[Tạo URL VNPay] Lỗi:', e);
        res.status(500).json({ error: e.message });
    }
});

// ─────────────────────────────────────────────
// GET /api/payment/vnpay/return
// Đường dẫn nhận phản hồi sau khi người dùng thực hiện thanh toán trên cổng VNPay
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

        // Tạo trang HTML phản hồi ngắn để WebView bắt kết quả và đóng màn hình
        if(secureHash === signed){
            // Tách mã ID đơn hàng từ txnRef (Định dạng: orderId_timestamp)
            let orderId = vnp_Params['vnp_TxnRef'] ? vnp_Params['vnp_TxnRef'].split('_')[0] : null;

            if (vnp_Params['vnp_ResponseCode'] === '00') { // Thành công
                // ✅ Cập nhật đơn hàng trực tiếp tại callback để hỗ trợ môi trường dev local khi không bắt được IPN
                if (orderId) {
                    try {
                        const order = await Order.findByPk(orderId);
                        if (order && order.paymentStatus !== 'paid') {
                            order.paymentStatus = 'paid';
                            order.status = 'Chờ xác nhận';
                            order.paidAt = new Date();
                            order.sepayTransactionId = vnp_Params['vnp_TransactionNo'] || null;
                            await order.save();
                            console.log(`[VNPay Callback] ✅ Đơn hàng #${orderId} được cập nhật: ĐÃ THANH TOÁN`);
                        }
                    } catch (err) {
                        console.error('[VNPay Callback] Lỗi cập nhật đơn hàng:', err.message);
                    }
                }

                res.send(`
                    <html>
                    <head><meta name="viewport" content="width=device-width, initial-scale=1"></head>
                    <body style="display:flex; flex-direction:column; align-items:center; justify-content:center; height:100vh; font-family:sans-serif; text-align:center;">
                        <h2 style="color:#00B14F;">Thanh Toán Thành Công</h2>
                        <p>Đơn hàng của bạn đã được xử lý thành công.</p>
                        <p style="color:gray; font-size:12px;">Đang chuyển hướng về ứng dụng...</p>
                    </body>
                    </html>
                `);
            } else {
                // ❌ Thanh toán thất bại hoặc người dùng tự ý hủy bỏ giao dịch
                if (orderId) {
                    try {
                        const order = await Order.findByPk(orderId);
                        if (order && order.paymentStatus === 'pending') {
                            order.paymentStatus = 'expired';
                            order.status = 'Đã hủy';
                            order.cancelReason = 'Người dùng hủy thanh toán VNPay';
                            order.cancelledAt = new Date();
                            await order.save();
                            console.log(`[VNPay Callback] ❌ Đơn hàng #${orderId} đã hủy do thanh toán không thành công`);
                        }
                    } catch (err) {
                        console.error('[VNPay Callback] Lỗi khi hủy đơn hàng:', err.message);
                    }
                }

                res.send(`
                    <html>
                    <head><meta name="viewport" content="width=device-width, initial-scale=1"></head>
                    <body style="display:flex; flex-direction:column; align-items:center; justify-content:center; height:100vh; font-family:sans-serif; text-align:center;">
                        <h2 style="color:#FF383C;">Thanh Toán Lỗi / Bị Hủy</h2>
                        <p>Mã phản hồi từ ngân hàng: ${vnp_Params['vnp_ResponseCode']}</p>
                    </body>
                    </html>
                `);
            }
        } else {
            res.send('<h1>Lỗi xác thực chữ ký VNPay (Secure Hash không trùng khớp)</h1>');
        }
    } catch (e) {
        res.status(500).send('Lỗi máy chủ');
    }
});

// ─────────────────────────────────────────────
// GET /api/payment/vnpay/ipn
// Webhook IPN nhận cuộc gọi ngầm (Server-to-Server) trực tiếp từ VNPay để cập nhật CSDL tin cậy
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
                return res.json({ RspCode: '01', Message: 'Không tìm thấy đơn hàng' });
            }

            // Kiểm tra xem đơn hàng đã được cập nhật thanh toán trước đó chưa
            if (order.paymentStatus === 'paid') {
                return res.json({ RspCode: '02', Message: 'Đơn hàng đã được xác nhận thanh toán trước đó' });
            }

            if (vnp_Params['vnp_ResponseCode'] === '00' && vnp_Params['vnp_TransactionStatus'] === '00') {
                // Thanh toán thành công từ phía VNPay
                order.paymentStatus = 'paid';
                order.status = 'Chờ xác nhận';
                order.paidAt = new Date();
                order.sepayTransactionId = vnp_Params['vnp_TransactionNo'];
                await order.save();
                
                return res.json({ RspCode: '00', Message: 'Xác nhận thanh toán thành công' });
            } else {
                // Thanh toán thất bại hoặc lỗi giao dịch
                order.paymentStatus = 'expired';
                order.status = 'Đã hủy';
                order.cancelReason = 'Thanh toán VNPay lỗi hoặc bị hủy bỏ';
                await order.save();
                
                return res.json({ RspCode: '00', Message: 'Xác nhận hủy thanh toán thành công' });
            }
        } else {
             return res.json({ RspCode: '97', Message: 'Sai chữ ký bảo mật (Checksum failed)' });
        }
    } catch (e) {
        return res.json({ RspCode: '99', Message: 'Lỗi không xác định' });
    }
});

module.exports = router;
