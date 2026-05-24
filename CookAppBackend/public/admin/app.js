/*  ═══════════════════════════════════════════════
    CookApp Admin Panel — app.js
    Premium SPA dashboard for managing CookApp
    ═══════════════════════════════════════════════ */

const API = '';  // Same origin

// ── STATE ─────────────────────────────────────────
let token = localStorage.getItem('admin_token') || '';
let currentPage = 'dashboard';
let pageData = {};
let sidebarOpen = false;

// ── INIT ──────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
    // Check for token in URL query (auto-login from Android WebView)
    const urlParams = new URLSearchParams(window.location.search);
    const urlToken = urlParams.get('token');
    if (urlToken) {
        token = urlToken;
        localStorage.setItem('admin_token', token);
        // Clean URL
        window.history.replaceState({}, '', window.location.pathname);
    }

    if (token) {
        verifyAndEnter();
    } else {
        showLogin();
    }
    setupClock();
    setupMobileMenu();
});

// ── AUTH ──────────────────────────────────────────
function showLogin() {
    document.getElementById('login-screen').style.display = 'flex';
    document.getElementById('app').style.display = 'none';
}

function showApp() {
    document.getElementById('login-screen').style.display = 'none';
    document.getElementById('app').style.display = 'flex';
    setupNavigation();
    navigateTo('dashboard');
}

async function verifyAndEnter() {
    try {
        const res = await apiFetch('/api/auth/me');
        if (res.role !== 'admin') {
            toast('Tài khoản không có quyền Admin', 'error');
            logout();
            return;
        }
        document.getElementById('admin-name').textContent = res.fullName || res.email;
        showApp();
    } catch (e) {
        logout();
    }
}
// Expose globally for Android WebView injection fallback
window.verifyAndEnter = verifyAndEnter;

document.getElementById('login-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const email = document.getElementById('login-email').value;
    const password = document.getElementById('login-password').value;
    const errEl = document.getElementById('login-error');

    try {
        const res = await fetch(API + '/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password })
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.error || 'Login failed');

        token = data.token;
        localStorage.setItem('admin_token', token);

        // Verify admin role
        const me = await apiFetch('/api/auth/me');
        if (me.role !== 'admin') {
            localStorage.removeItem('admin_token');
            token = '';
            errEl.textContent = 'Tài khoản không có quyền Admin';
            errEl.style.display = 'block';
            return;
        }

        document.getElementById('admin-name').textContent = me.fullName || me.email;
        showApp();
    } catch (err) {
        errEl.textContent = err.message;
        errEl.style.display = 'block';
    }
});

function logout() {
    token = '';
    localStorage.removeItem('admin_token');
    showLogin();
}
document.getElementById('btn-logout').addEventListener('click', logout);

// ── API HELPER ────────────────────────────────────
async function apiFetch(url, options = {}) {
    const res = await fetch(API + url, {
        ...options,
        headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + token,
            ...(options.headers || {})
        }
    });
    const data = await res.json();
    if (!res.ok) throw new Error(data.error || 'Request failed');
    return data;
}

// ── MOBILE MENU ───────────────────────────────────
function setupMobileMenu() {
    const toggle = document.getElementById('sidebar-toggle');
    const sidebar = document.querySelector('.sidebar');
    const overlay = document.getElementById('sidebar-overlay');

    if (toggle) {
        toggle.addEventListener('click', () => {
            sidebarOpen = !sidebarOpen;
            sidebar.classList.toggle('open', sidebarOpen);
            overlay.classList.toggle('active', sidebarOpen);
        });
    }
    if (overlay) {
        overlay.addEventListener('click', () => {
            sidebarOpen = false;
            sidebar.classList.remove('open');
            overlay.classList.remove('active');
        });
    }
}

function closeMobileSidebar() {
    sidebarOpen = false;
    const sidebar = document.querySelector('.sidebar');
    const overlay = document.getElementById('sidebar-overlay');
    if (sidebar) sidebar.classList.remove('open');
    if (overlay) overlay.classList.remove('active');
}

// ── NAVIGATION ────────────────────────────────────
function setupNavigation() {
    document.querySelectorAll('.nav-item').forEach(el => {
        el.addEventListener('click', (e) => {
            e.preventDefault();
            navigateTo(el.dataset.page);
            closeMobileSidebar();
        });
    });
}

function navigateTo(page) {
    currentPage = page;
    document.querySelectorAll('.nav-item').forEach(el => {
        el.classList.toggle('active', el.dataset.page === page);
    });

    const titles = {
        dashboard: 'Dashboard',
        orders: 'Đơn hàng',
        recipes: 'Công thức',
        users: 'Người dùng',
        posts: 'Bài viết',
        reviews: 'Đánh giá'
    };
    document.getElementById('page-title').textContent = titles[page] || 'Dashboard';

    const content = document.getElementById('page-content');
    content.innerHTML = '<div class="loading">Đang tải dữ liệu...</div>';

    switch (page) {
        case 'dashboard': loadDashboard(); break;
        case 'orders':    loadOrders(); break;
        case 'recipes':   loadRecipes(); break;
        case 'users':     loadUsers(); break;
        case 'posts':     loadPosts(); break;
        case 'reviews':   loadReviews(); break;
    }
}

// ── CLOCK ──────────────────────────────────────────
function setupClock() {
    const el = document.getElementById('current-time');
    if (!el) return;
    function tick() {
        const now = new Date();
        el.textContent = now.toLocaleString('vi-VN', {
            day: '2-digit', month: '2-digit',
            year: 'numeric', hour: '2-digit', minute: '2-digit'
        });
    }
    tick();
    setInterval(tick, 30000);
}

// ── TOAST ──────────────────────────────────────────
function toast(msg, type = 'info') {
    let container = document.querySelector('.toast-container');
    if (!container) {
        container = document.createElement('div');
        container.className = 'toast-container';
        document.body.appendChild(container);
    }
    const t = document.createElement('div');
    t.className = `toast toast-${type}`;
    t.textContent = msg;
    container.appendChild(t);
    setTimeout(() => t.remove(), 3000);
}

// ── FORMAT HELPERS ────────────────────────────────
function formatMoney(n) {
    return new Intl.NumberFormat('vi-VN').format(n || 0) + ' đ';
}

function formatDate(d) {
    if (!d) return '—';
    return new Date(d).toLocaleString('vi-VN', {
        day: '2-digit', month: '2-digit', year: 'numeric',
        hour: '2-digit', minute: '2-digit'
    });
}

function statusBadge(status) {
    const map = {
        'Chờ xác nhận': 'badge-pending',
        'Đang giao': 'badge-shipping',
        'Hoàn thành': 'badge-done',
        'Đã hủy': 'badge-cancelled'
    };
    return `<span class="badge ${map[status] || 'badge-pending'}">${status}</span>`;
}

function roleBadge(role) {
    return role === 'admin'
        ? '<span class="badge badge-admin">Admin</span>'
        : '<span class="badge badge-user">User</span>';
}

// ═══════════════════════════════════════════════════
// PAGE RENDERERS
// ═══════════════════════════════════════════════════

// ── DASHBOARD ─────────────────────────────────────
async function loadDashboard() {
    try {
        const data = await apiFetch('/api/admin/stats');
        const el = document.getElementById('page-content');

        el.innerHTML = `
            <div class="stats-grid">
                <div class="stat-card accent">
                    <div class="stat-icon">💰</div>
                    <div class="stat-value">${formatMoney(data.revenue)}</div>
                    <div class="stat-label">Tổng doanh thu</div>
                </div>
                <div class="stat-card info">
                    <div class="stat-icon">📦</div>
                    <div class="stat-value">${data.orders}</div>
                    <div class="stat-label">Đơn hàng</div>
                </div>
                <div class="stat-card success">
                    <div class="stat-icon">👥</div>
                    <div class="stat-value">${data.users}</div>
                    <div class="stat-label">Người dùng</div>
                </div>
                <div class="stat-card warning">
                    <div class="stat-icon">🍲</div>
                    <div class="stat-value">${data.recipes}</div>
                    <div class="stat-label">Công thức</div>
                </div>
                <div class="stat-card info">
                    <div class="stat-icon">📝</div>
                    <div class="stat-value">${data.posts}</div>
                    <div class="stat-label">Bài viết</div>
                </div>
                <div class="stat-card success">
                    <div class="stat-icon">⭐</div>
                    <div class="stat-value">${data.reviews}</div>
                    <div class="stat-label">Đánh giá</div>
                </div>
            </div>

            <!-- Order Status Breakdown -->
            <div class="stats-grid" style="margin-bottom:28px">
                ${(data.ordersByStatus || []).map(s => `
                    <div class="stat-card">
                        <div class="stat-value" style="font-size:22px">${s.count}</div>
                        <div class="stat-label">${statusBadge(s.status)}</div>
                    </div>
                `).join('')}
            </div>

            <!-- Recent Orders -->
            <div class="section-title">📦 Đơn hàng gần đây</div>
            <div class="data-table-wrapper">
                <div class="table-scroll">
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>Khách hàng</th>
                            <th>Tổng tiền</th>
                            <th>Trạng thái</th>
                            <th>Ngày tạo</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${(data.recentOrders || []).map(o => `
                            <tr>
                                <td>#${o.id}</td>
                                <td>${o.customerName || o.User?.fullName || '—'}</td>
                                <td>${formatMoney(o.totalPrice + o.shippingFee)}</td>
                                <td>${statusBadge(o.status)}</td>
                                <td>${formatDate(o.created_at)}</td>
                            </tr>
                        `).join('')}
                        ${(data.recentOrders || []).length === 0 ? '<tr><td colspan="5" style="text-align:center;color:var(--text-muted)">Chưa có đơn hàng</td></tr>' : ''}
                    </tbody>
                </table>
                </div>
            </div>
        `;
    } catch (e) {
        document.getElementById('page-content').innerHTML = `<div class="empty-state">❌ ${e.message}</div>`;
    }
}

// ── ORDERS ────────────────────────────────────────
async function loadOrders(page = 1, status = '') {
    try {
        const qs = `?page=${page}&limit=15${status ? '&status=' + encodeURIComponent(status) : ''}`;
        const data = await apiFetch('/api/admin/orders' + qs);
        pageData.orders = data;

        const el = document.getElementById('page-content');
        el.innerHTML = `
            <div class="data-table-wrapper">
                <div class="table-header">
                    <div class="table-title">📦 Đơn hàng (${data.total})</div>
                    <select class="status-select" id="order-filter" style="min-width:140px">
                        <option value="">Tất cả</option>
                        <option value="Chờ xác nhận" ${status==='Chờ xác nhận'?'selected':''}>Chờ xác nhận</option>
                        <option value="Đang giao" ${status==='Đang giao'?'selected':''}>Đang giao</option>
                        <option value="Hoàn thành" ${status==='Hoàn thành'?'selected':''}>Hoàn thành</option>
                        <option value="Đã hủy" ${status==='Đã hủy'?'selected':''}>Đã hủy</option>
                    </select>
                </div>
                <div class="table-scroll">
                <table class="data-table">
                    <thead>
                        <tr><th>ID</th><th>Khách hàng</th><th>SĐT</th><th>Tổng</th><th>Trạng thái</th><th>Ngày</th><th>Thao tác</th></tr>
                    </thead>
                    <tbody>
                        ${data.orders.map(o => `
                            <tr>
                                <td>#${o.id}</td>
                                <td>${o.customerName}</td>
                                <td>${o.phone || '—'}</td>
                                <td class="nowrap">${formatMoney(o.totalPrice + o.shippingFee)}</td>
                                <td>${statusBadge(o.status)}</td>
                                <td class="nowrap">${formatDate(o.created_at)}</td>
                                <td>
                                    <select class="status-select" onchange="updateOrderStatus(${o.id}, this.value)" ${o.status==='Đã hủy'||o.status==='Hoàn thành'?'disabled':''}>
                                        <option value="Chờ xác nhận" ${o.status==='Chờ xác nhận'?'selected':''}>Chờ xác nhận</option>
                                        <option value="Đang giao" ${o.status==='Đang giao'?'selected':''}>Đang giao</option>
                                        <option value="Hoàn thành" ${o.status==='Hoàn thành'?'selected':''}>Hoàn thành</option>
                                        <option value="Đã hủy" ${o.status==='Đã hủy'?'selected':''}>Đã hủy</option>
                                    </select>
                                </td>
                            </tr>
                        `).join('')}
                        ${data.orders.length === 0 ? '<tr><td colspan="7" style="text-align:center;color:var(--text-muted)">Không có đơn hàng</td></tr>' : ''}
                    </tbody>
                </table>
                </div>
                ${renderPagination(data, (p) => `loadOrders(${p}, '${status}')`)}
            </div>
        `;

        document.getElementById('order-filter').addEventListener('change', function() {
            loadOrders(1, this.value);
        });
    } catch (e) {
        document.getElementById('page-content').innerHTML = `<div class="empty-state">❌ ${e.message}</div>`;
    }
}

async function updateOrderStatus(id, status) {
    try {
        await apiFetch(`/api/admin/orders/${id}/status`, {
            method: 'PATCH',
            body: JSON.stringify({ status })
        });
        toast('Cập nhật trạng thái thành công', 'success');
        loadOrders(pageData.orders?.page || 1);
    } catch (e) {
        toast(e.message, 'error');
    }
}

// ── RECIPES ───────────────────────────────────────
async function loadRecipes(page = 1, search = '') {
    try {
        const qs = `?page=${page}&limit=15${search ? '&search=' + encodeURIComponent(search) : ''}`;
        const data = await apiFetch('/api/admin/recipes' + qs);
        pageData.recipes = data;

        const el = document.getElementById('page-content');
        el.innerHTML = `
            <div class="data-table-wrapper">
                <div class="table-header">
                    <div class="table-title">🍲 Công thức (${data.total})</div>
                    <input type="text" class="table-search" id="recipe-search" placeholder="🔍 Tìm..." value="${search}">
                </div>
                <div class="table-scroll">
                <table class="data-table">
                    <thead>
                        <tr><th>ID</th><th>Hình</th><th>Tên</th><th>Danh mục</th><th>Phút</th><th>Độ khó</th><th></th></tr>
                    </thead>
                    <tbody>
                        ${data.recipes.map(r => `
                            <tr>
                                <td>${r.id}</td>
                                <td><img src="${r.image_url || ''}" class="table-thumb" onerror="this.style.display='none'"></td>
                                <td class="cell-title">${r.title}</td>
                                <td>${r.Category?.name || '—'}</td>
                                <td>${r.cook_time || '—'}</td>
                                <td>${r.difficulty || '—'}</td>
                                <td>
                                    <button class="btn btn-outline btn-sm" onclick="editSteps(${r.id}, '${r.title.replace(/'/g, "\\'")}')">⏱️ Giờ</button>
                                    <button class="btn btn-danger btn-sm" onclick="deleteRecipe(${r.id})">🗑</button>
                                </td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
                </div>
                ${renderPagination(data, (p) => `loadRecipes(${p}, '${search}')`)}
            </div>
        `;

        let searchTimeout;
        document.getElementById('recipe-search').addEventListener('input', function() {
            clearTimeout(searchTimeout);
            searchTimeout = setTimeout(() => loadRecipes(1, this.value), 400);
        });
    } catch (e) {
        document.getElementById('page-content').innerHTML = `<div class="empty-state">❌ ${e.message}</div>`;
    }
}

async function deleteRecipe(id) {
    if (!confirm('Bạn chắc chắn muốn xóa công thức này? Tất cả bước nấu, đánh giá liên quan sẽ bị xóa.')) return;
    try {
        await apiFetch(`/api/admin/recipes/${id}`, { method: 'DELETE' });
        toast('Đã xóa công thức', 'success');
        loadRecipes(pageData.recipes?.page || 1);
    } catch (e) { toast(e.message, 'error'); }
}

async function editSteps(id, title) {
    try {
        const steps = await apiFetch(`/api/admin/recipes/${id}/steps`);
        let html = `<h3>Chỉnh thời gian Video - ${title}</h3>
                    <p style="margin-bottom:15px; color:#666; font-size:14px;">Nhập số giây xuất hiện cảnh trong Video của từng bước nấu.</p>
                    <div style="max-height:60vh; overflow-y:auto; padding-right:10px;">`;
        if(steps.length === 0) {
            html += `<p>Món ăn này chưa có bước nào!</p>`;
        } else {
            steps.forEach(s => {
                html += `<div style="display:flex; gap:10px; align-items:center; margin-bottom:10px; background:#f5f6f8; padding:10px; border-radius:6px;">
                            <div style="width:30px; font-weight:bold;">\u00A0\u00A0Bước ${s.step_number}.</div>
                            <div style="flex:1; font-size:13px; line-height:1.4;">${s.instruction}</div>
                            <div style="width:80px;">
                                <input type="number" id="step_time_${s.id}" class="form-input" style="padding:5px;" value="${s.video_start_time || 0}" min="0">
                            </div>
                            <div style="font-size:12px; color:#888; width: 40px">giây</div>
                         </div>`;
            });
        }
        html += `</div>
                 <div style="margin-top:20px; display:flex; justify-content:flex-end; align-items:center;">
                    <div style="display:flex; gap:10px;">
                        <button class="btn btn-outline" onclick="closeAdminModal()">Hủy</button>
                        ${steps.length > 0 ? `<button class="btn btn-primary" onclick="saveSteps(${id}, [${steps.map(s => s.id).join(',')}])">Lưu mốc thời gian</button>` : ''}
                    </div>
                 </div>`;
        showAdminModal(html);
    } catch (e) { toast(e.message, 'error'); }
}


async function saveSteps(recipeId, stepIds) {
    const payloadSteps = stepIds.map(id => ({
        id: id,
        video_start_time: parseInt(document.getElementById(`step_time_${id}`).value) || 0
    }));
    try {
        await apiFetch(`/api/admin/recipes/${recipeId}/steps`, {
            method: 'PUT',
            body: JSON.stringify({ steps: payloadSteps })
        });
        toast('Đã lưu mốc thời gian!', 'success');
        closeAdminModal();
    } catch (e) { toast(e.message, 'error'); }
}

function showAdminModal(html) {
    let modal = document.getElementById('admin-modal');
    if(!modal) {
        modal = document.createElement('div');
        modal.id = 'admin-modal';
        modal.style.cssText = 'position:fixed; top:0; left:0; width:100%; height:100%; background:rgba(0,0,0,0.5); z-index:9999; display:flex; align-items:center; justify-content:center;';
        const modalContent = document.createElement('div');
        modalContent.id = 'admin-modal-content';
        modalContent.style.cssText = 'background:white; padding:20px; border-radius:12px; width:90%; max-width:600px; box-shadow:0 10px 25px rgba(0,0,0,0.2);';
        modal.appendChild(modalContent);
        document.body.appendChild(modal);
    }
    document.getElementById('admin-modal-content').innerHTML = html;
    modal.style.display = 'flex';
}

function closeAdminModal() {
    const modal = document.getElementById('admin-modal');
    if(modal) modal.style.display = 'none';
}

// ── USERS ─────────────────────────────────────────
async function loadUsers(page = 1, search = '') {
    try {
        const qs = `?page=${page}&limit=15${search ? '&search=' + encodeURIComponent(search) : ''}`;
        const data = await apiFetch('/api/admin/users' + qs);
        pageData.users = data;

        const el = document.getElementById('page-content');
        el.innerHTML = `
            <div class="data-table-wrapper">
                <div class="table-header">
                    <div class="table-title">👥 Người dùng (${data.total})</div>
                    <input type="text" class="table-search" id="user-search" placeholder="🔍 Tìm..." value="${search}">
                </div>
                <div class="table-scroll">
                <table class="data-table">
                    <thead>
                        <tr><th>ID</th><th>Tên</th><th>Email</th><th>SĐT</th><th>Vai trò</th><th>Ngày tạo</th><th></th></tr>
                    </thead>
                    <tbody>
                        ${data.users.map(u => `
                            <tr>
                                <td>${u.id}</td>
                                <td class="cell-title">${u.fullName || '—'}</td>
                                <td>${u.email}</td>
                                <td>${u.phoneNumber || '—'}</td>
                                <td>${roleBadge(u.role)}</td>
                                <td class="nowrap">${formatDate(u.created_at)}</td>
                                <td class="btn-group">
                                    <button class="btn btn-outline btn-sm" onclick="toggleRole(${u.id}, '${u.role}')">
                                        ${u.role === 'admin' ? '↓' : '↑'}
                                    </button>
                                    <button class="btn btn-danger btn-sm" onclick="deleteUser(${u.id})">🗑</button>
                                </td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
                </div>
                ${renderPagination(data, (p) => `loadUsers(${p}, '${search}')`)}
            </div>
        `;

        let searchTimeout;
        document.getElementById('user-search').addEventListener('input', function() {
            clearTimeout(searchTimeout);
            searchTimeout = setTimeout(() => loadUsers(1, this.value), 400);
        });
    } catch (e) {
        document.getElementById('page-content').innerHTML = `<div class="empty-state">❌ ${e.message}</div>`;
    }
}

async function toggleRole(id, currentRole) {
    const newRole = currentRole === 'admin' ? 'user' : 'admin';
    if (!confirm(`Đổi vai trò thành "${newRole}"?`)) return;
    try {
        await apiFetch(`/api/admin/users/${id}/role`, {
            method: 'PATCH',
            body: JSON.stringify({ role: newRole })
        });
        toast('Cập nhật vai trò thành công', 'success');
        loadUsers(pageData.users?.page || 1);
    } catch (e) { toast(e.message, 'error'); }
}

async function deleteUser(id) {
    if (!confirm('Xóa người dùng này? Lưu ý: Hành động này không thể hoàn tác!')) return;
    try {
        await apiFetch(`/api/admin/users/${id}`, { method: 'DELETE' });
        toast('Đã xóa người dùng', 'success');
        loadUsers(pageData.users?.page || 1);
    } catch (e) { toast(e.message, 'error'); }
}

// ── POSTS ─────────────────────────────────────────
async function loadPosts(page = 1) {
    try {
        const data = await apiFetch(`/api/admin/posts?page=${page}&limit=15`);
        pageData.posts = data;

        const el = document.getElementById('page-content');
        el.innerHTML = `
            <div class="data-table-wrapper">
                <div class="table-header">
                    <div class="table-title">📝 Bài viết (${data.total})</div>
                </div>
                <div class="table-scroll">
                <table class="data-table">
                    <thead>
                        <tr><th>ID</th><th>Tiêu đề</th><th>Tác giả</th><th>❤️</th><th>💬</th><th>Ngày</th><th></th></tr>
                    </thead>
                    <tbody>
                        ${data.posts.map(p => `
                            <tr>
                                <td>${p.id}</td>
                                <td class="cell-title">${p.title}</td>
                                <td>${p.author || '—'}</td>
                                <td>${p.likes || 0}</td>
                                <td>${p.comments?.length || 0}</td>
                                <td class="nowrap">${formatDate(p.created_at)}</td>
                                <td><button class="btn btn-danger btn-sm" onclick="deletePost(${p.id})">🗑</button></td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
                </div>
                ${renderPagination(data, (p) => `loadPosts(${p})`)}
            </div>
        `;
    } catch (e) {
        document.getElementById('page-content').innerHTML = `<div class="empty-state">❌ ${e.message}</div>`;
    }
}

async function deletePost(id) {
    if (!confirm('Xóa bài viết này? Comments liên quan cũng sẽ bị xóa.')) return;
    try {
        await apiFetch(`/api/admin/posts/${id}`, { method: 'DELETE' });
        toast('Đã xóa bài viết', 'success');
        loadPosts(pageData.posts?.page || 1);
    } catch (e) { toast(e.message, 'error'); }
}

// ── REVIEWS ───────────────────────────────────────
async function loadReviews(page = 1) {
    try {
        const data = await apiFetch(`/api/admin/reviews?page=${page}&limit=15`);
        pageData.reviews = data;

        const el = document.getElementById('page-content');
        el.innerHTML = `
            <div class="data-table-wrapper">
                <div class="table-header">
                    <div class="table-title">⭐ Đánh giá (${data.total})</div>
                </div>
                <div class="table-scroll">
                <table class="data-table">
                    <thead>
                        <tr><th>ID</th><th>Công thức</th><th>Rating</th><th>Bình luận</th><th>Tác giả</th><th></th></tr>
                    </thead>
                    <tbody>
                        ${data.reviews.map(r => `
                            <tr>
                                <td>${r.id}</td>
                                <td class="cell-title">${r.Recipe?.title || 'Recipe #' + r.recipe_id}</td>
                                <td class="nowrap">${'⭐'.repeat(r.rating)}${'☆'.repeat(5 - r.rating)}</td>
                                <td class="cell-comment">${r.comment || '—'}</td>
                                <td>${r.author || '—'}</td>
                                <td><button class="btn btn-danger btn-sm" onclick="deleteReview(${r.id})">🗑</button></td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
                </div>
                ${renderPagination(data, (p) => `loadReviews(${p})`)}
            </div>
        `;
    } catch (e) {
        document.getElementById('page-content').innerHTML = `<div class="empty-state">❌ ${e.message}</div>`;
    }
}

async function deleteReview(id) {
    if (!confirm('Xóa đánh giá này?')) return;
    try {
        await apiFetch(`/api/admin/reviews/${id}`, { method: 'DELETE' });
        toast('Đã xóa đánh giá', 'success');
        loadReviews(pageData.reviews?.page || 1);
    } catch (e) { toast(e.message, 'error'); }
}

// ── PAGINATION RENDERER ──────────────────────────
function renderPagination(data, onClickFn) {
    if (!data.totalPages || data.totalPages <= 1) return '';

    let btns = '';
    const maxVisible = 5;
    let start = Math.max(1, data.page - Math.floor(maxVisible / 2));
    let end = Math.min(data.totalPages, start + maxVisible - 1);
    if (end - start < maxVisible - 1) start = Math.max(1, end - maxVisible + 1);

    for (let i = start; i <= end; i++) {
        btns += `<button class="${i === data.page ? 'active' : ''}" onclick="${onClickFn(i)}">${i}</button>`;
    }

    return `
        <div class="pagination">
            <div class="pagination-info">Trang ${data.page}/${data.totalPages}</div>
            <div class="pagination-btns">
                <button onclick="${onClickFn(data.page - 1)}" ${data.page <= 1 ? 'disabled' : ''}>←</button>
                ${btns}
                <button onclick="${onClickFn(data.page + 1)}" ${data.page >= data.totalPages ? 'disabled' : ''}>→</button>
            </div>
        </div>
    `;
}

// Expose functions to global scope for inline onclick handlers
window.updateOrderStatus = updateOrderStatus;
window.deleteRecipe = deleteRecipe;
window.toggleRole = toggleRole;
window.deleteUser = deleteUser;
window.deletePost = deletePost;
window.deleteReview = deleteReview;
window.loadOrders = loadOrders;
window.loadRecipes = loadRecipes;
window.loadUsers = loadUsers;
window.loadPosts = loadPosts;
window.loadReviews = loadReviews;
