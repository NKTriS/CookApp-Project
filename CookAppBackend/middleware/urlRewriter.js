/**
 * URL Rewriter Middleware
 * 
 * Tự động chuyển relative path (e.g. "/videos/thumbnails/xxx.jpg")
 * thành absolute URL dựa trên request host thật sự.
 * 
 * Khi đổi WiFi (IP thay đổi), URL sẽ tự cập nhật theo IP mới
 * mà không cần sửa database.
 */

function getBaseUrl(req) {
    const protocol = req.protocol || 'http';
    const host = req.get('host');
    return `${protocol}://${host}`;
}

/** Kiểm tra tên field có phải URL field không */
const URL_FIELD_REGEX = /url|image|thumbnail|video|logo/i;
function isUrlField(key) {
    return URL_FIELD_REGEX.test(key);
}

/** Kiểm tra string có phải relative path không */
function isRelativePath(str) {
    return typeof str === 'string' && str.startsWith('/') && !str.startsWith('//');
}

/**
 * Duyệt qua JSON response và convert relative path → absolute URL.
 * Giới hạn depth để tránh stack overflow.
 */
function rewriteUrls(data, baseUrl, depth) {
    if (depth > 10) return data; // safety limit
    if (data === null || data === undefined) return data;

    if (Array.isArray(data)) {
        for (let i = 0; i < data.length; i++) {
            data[i] = rewriteUrls(data[i], baseUrl, depth + 1);
        }
        return data;
    }

    if (typeof data === 'object' && data !== null) {
        const keys = Object.keys(data);
        for (let i = 0; i < keys.length; i++) {
            const key = keys[i];
            const value = data[key];
            
            if (typeof value === 'string' && isUrlField(key) && isRelativePath(value)) {
                data[key] = baseUrl + value;
            } else if (typeof value === 'object' && value !== null) {
                data[key] = rewriteUrls(value, baseUrl, depth + 1);
            }
        }
        return data;
    }

    return data;
}

/**
 * Express middleware: Intercept res.json() để rewrite URLs trước khi gửi response.
 */
function urlRewriterMiddleware(req, res, next) {
    const baseUrl = getBaseUrl(req);
    
    const originalJson = res.json;
    res.json = function (data) {
        // Convert Sequelize instances to plain objects first
        let plainData = data;
        if (data && typeof data.toJSON === 'function') {
            plainData = data.toJSON();
        } else if (Array.isArray(data)) {
            plainData = data.map(item => 
                (item && typeof item.toJSON === 'function') ? item.toJSON() : item
            );
        }
        
        // Rewrite URLs
        const rewritten = rewriteUrls(plainData, baseUrl, 0);
        
        // Call original res.json with rewritten data
        return originalJson.call(this, rewritten);
    };
    
    next();
}

module.exports = urlRewriterMiddleware;
