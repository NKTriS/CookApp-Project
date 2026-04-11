const express = require('express');
const router = express.Router();
const {
    Recipe, Favorite, ShoppingList, ShoppingListItem,
    Notification, Post, Review, Order
} = require('../models');
const { authenticateToken } = require('../middleware/auth');
const { recipeSummaryAttributes } = require('./recipes');

// ─────────────────────────────────────────────
// FAVORITES
// ─────────────────────────────────────────────

router.get('/favorites', authenticateToken, async (req, res) => {
    try {
        const favs = await Favorite.findAll({
            where: { user_id: req.user.id },
            include: [{
                model: Recipe,
                attributes: recipeSummaryAttributes
            }]
        });
        res.json(favs);
    } catch (e) { res.status(500).json({ error: e.message }); }
});

router.post('/favorites/toggle', authenticateToken, async (req, res) => {
    try {
        const { recipe_id } = req.body;
        if (!recipe_id) return res.status(400).json({ error: 'recipe_id is required' });
        const existing = await Favorite.findOne({ where: { user_id: req.user.id, recipe_id } });
        if (existing) {
            await existing.destroy();
            return res.json({ isFavorite: false, message: 'Removed from favorites' });
        } else {
            await Favorite.create({ user_id: req.user.id, recipe_id });
            return res.json({ isFavorite: true, message: 'Added to favorites' });
        }
    } catch (e) { res.status(500).json({ error: e.message }); }
});

router.get('/favorites/check/:recipeId', authenticateToken, async (req, res) => {
    try {
        const fav = await Favorite.findOne({
            where: { user_id: req.user.id, recipe_id: req.params.recipeId }
        });
        res.json({ isFavorite: !!fav });
    } catch (e) { res.status(500).json({ error: e.message }); }
});

// ─────────────────────────────────────────────
// SHOPPING LIST
// ─────────────────────────────────────────────

router.get('/shopping-list', authenticateToken, async (req, res) => {
    try {
        const list = await ShoppingList.findOne({
            where: { user_id: req.user.id },
            include: [{ model: ShoppingListItem, as: 'items' }]
        });
        res.json(list || { items: [] });
    } catch (e) { res.status(500).json({ error: e.message }); }
});

router.post('/shopping-list/sync', authenticateToken, async (req, res) => {
    try {
        const { items } = req.body;
        if (!Array.isArray(items)) return res.status(400).json({ error: 'items must be an array' });

        let list = await ShoppingList.findOne({ where: { user_id: req.user.id } });
        if (!list) list = await ShoppingList.create({ user_id: req.user.id });

        await ShoppingListItem.destroy({ where: { shopping_list_id: list.id } });

        if (items.length > 0) {
            const mappedItems = items.map(it => ({
                shopping_list_id: list.id,
                ingredient_name: it.ingredient_name || 'Unknown',
                quantity: it.quantity || 1,
                unit: it.unit || '',
                checked: it.checked || false,
                price: it.price || 0
            }));
            await ShoppingListItem.bulkCreate(mappedItems);
        }

        res.json({ message: 'Shopping list synced successfully', count: items.length });
    } catch (e) { res.status(500).json({ error: e.message }); }
});

router.post('/shopping-list/items', authenticateToken, async (req, res) => {
    try {
        const { ingredient_name, quantity, unit } = req.body;
        if (!ingredient_name) return res.status(400).json({ error: 'ingredient_name is required' });

        let list = await ShoppingList.findOne({ where: { user_id: req.user.id } });
        if (!list) list = await ShoppingList.create({ user_id: req.user.id });

        const existing = await ShoppingListItem.findOne({
            where: { shopping_list_id: list.id, ingredient_name }
        });

        if (existing) {
            existing.quantity += (quantity || 1);
            await existing.save();
            return res.json(existing);
        }

        const item = await ShoppingListItem.create({
            shopping_list_id: list.id,
            ingredient_name,
            quantity: quantity || 1,
            unit: unit || '',
            checked: false
        });
        res.status(201).json(item);
    } catch (e) { res.status(500).json({ error: e.message }); }
});

router.patch('/shopping-list/items/:id', authenticateToken, async (req, res) => {
    try {
        const { quantity, checked } = req.body;
        const list = await ShoppingList.findOne({ where: { user_id: req.user.id } });
        if (!list) return res.status(404).json({ error: 'Shopping list not found' });

        const item = await ShoppingListItem.findOne({
            where: { id: req.params.id, shopping_list_id: list.id }
        });
        if (!item) return res.status(404).json({ error: 'Item not found or not authorized' });

        if (quantity !== undefined) item.quantity = quantity;
        if (checked  !== undefined) item.checked  = checked;
        await item.save();
        res.json(item);
    } catch (e) { res.status(500).json({ error: e.message }); }
});

router.delete('/shopping-list/items/:id', authenticateToken, async (req, res) => {
    try {
        const list = await ShoppingList.findOne({ where: { user_id: req.user.id } });
        if (!list) return res.status(404).json({ error: 'Shopping list not found' });

        const deleted = await ShoppingListItem.destroy({
            where: { id: req.params.id, shopping_list_id: list.id }
        });
        if (!deleted) return res.status(404).json({ error: 'Item not found or not authorized' });
        res.json({ message: 'Item deleted' });
    } catch (e) { res.status(500).json({ error: e.message }); }
});

// ─────────────────────────────────────────────
// NOTIFICATIONS
// ─────────────────────────────────────────────

router.get('/notifications', authenticateToken, async (req, res) => {
    try {
        const limit = parseInt(req.query.limit) || 50;
        const page = parseInt(req.query.page) || 1;

        const notifs = await Notification.findAll({
            where: { user_id: req.user.id },
            order: [['created_at', 'DESC']],
            limit,
            offset: (page - 1) * limit
        });
        res.json(notifs);
    } catch (e) { res.status(500).json({ error: e.message }); }
});

router.post('/notifications/:id/read', authenticateToken, async (req, res) => {
    try {
        const notif = await Notification.findOne({ where: { id: req.params.id, user_id: req.user.id } });
        if (!notif) return res.status(404).json({ error: 'Notification not found' });
        notif.isRead = true;
        await notif.save();
        res.json(notif);
    } catch (e) { res.status(500).json({ error: e.message }); }
});

// ─────────────────────────────────────────────
// PROFILE STATS
// ─────────────────────────────────────────────
router.get('/profile/stats', authenticateToken, async (req, res) => {
    try {
        const postsCount     = await Post.count({ where: { user_id: req.user.id } });
        const reviewsCount   = await Review.count({ where: { user_id: req.user.id } });
        const favoritesCount = await Favorite.count({ where: { user_id: req.user.id } });
        res.json({ posts: postsCount, reviews: reviewsCount, favorites: favoritesCount });
    } catch (e) { res.status(500).json({ error: e.message }); }
});

// ─────────────────────────────────────────────
// ORDERS (Server-side persistence)
// ─────────────────────────────────────────────

router.get('/orders', authenticateToken, async (req, res) => {
    try {
        const orders = await Order.findAll({
            where: { user_id: req.user.id },
            order: [['created_at', 'DESC']]
        });
        res.json(orders);
    } catch (e) { res.status(500).json({ error: e.message }); }
});

router.get('/orders/:id', authenticateToken, async (req, res) => {
    try {
        const order = await Order.findOne({
            where: { id: req.params.id, user_id: req.user.id }
        });
        if (!order) return res.status(404).json({ error: 'Order not found' });
        res.json(order);
    } catch (e) { res.status(500).json({ error: e.message }); }
});

router.post('/orders', authenticateToken, async (req, res) => {
    try {
        const { customerName, phone, address, totalPrice, shippingFee, itemsSummary, paymentMethod, note } = req.body;

        if (!customerName || !phone || !address) {
            return res.status(400).json({ error: 'customerName, phone, address are required' });
        }

        const isBankTransfer = paymentMethod === 'Chuyển khoản';

        const order = await Order.create({
            user_id: req.user.id,
            customerName,
            phone,
            address,
            totalPrice: totalPrice || 0,
            shippingFee: shippingFee || 0,
            itemsSummary: itemsSummary || '',
            status: isBankTransfer ? 'Chờ thanh toán' : 'Chờ xác nhận',
            paymentMethod: paymentMethod || 'COD',
            paymentStatus: isBankTransfer ? 'pending' : null,
            note: note || null
        });

        // Generate paymentCode after creation (uses order.id)
        if (isBankTransfer) {
            order.paymentCode = 'COOKAPP' + order.id;
            await order.save();
        }

        res.status(201).json(order);
    } catch (e) { res.status(500).json({ error: e.message }); }
});

router.patch('/orders/:id/cancel', authenticateToken, async (req, res) => {
    try {
        const order = await Order.findOne({
            where: { id: req.params.id, user_id: req.user.id }
        });
        if (!order) return res.status(404).json({ error: 'Order not found' });

        // Allow cancel for both "Chờ xác nhận" (COD) and "Chờ thanh toán" (bank transfer)
        const cancellableStatuses = ['Chờ xác nhận', 'Chờ thanh toán'];
        if (!cancellableStatuses.includes(order.status)) {
            return res.status(400).json({ error: 'Chỉ có thể hủy đơn ở trạng thái "Chờ xác nhận" hoặc "Chờ thanh toán"' });
        }

        order.status = 'Đã hủy';
        order.cancelReason = req.body.reason || null;
        order.cancelledAt = new Date();
        // Mark payment as expired if it was pending
        if (order.paymentStatus === 'pending') {
            order.paymentStatus = 'expired';
        }
        await order.save();

        res.json(order);
    } catch (e) { res.status(500).json({ error: e.message }); }
});

module.exports = router;
