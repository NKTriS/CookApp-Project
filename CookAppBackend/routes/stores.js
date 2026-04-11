const express = require('express');
const router = express.Router();
const { Op } = require('sequelize');
const { StoreProduct } = require('../models');

// GET /api/store-products?q=tên&store=storeName
router.get('/store-products', async (req, res) => {
    try {
        const where = { in_stock: true };
        if (req.query.q) {
            where.ingredient_name = { [Op.like]: `%${req.query.q}%` };
        }
        if (req.query.store) {
            where.store_name = req.query.store;
        }
        const products = await StoreProduct.findAll({
            where,
            order: [['price_dong', 'ASC']]
        });
        res.json(products);
    } catch (e) { res.status(500).json({ error: e.message }); }
});

// POST /api/store-products/check-ingredients
router.post('/store-products/check-ingredients', async (req, res) => {
    try {
        const names = req.body.names;
        if (!names || !Array.isArray(names)) {
            return res.status(400).json({ error: '"names" phải là mảng string' });
        }

        const result = {};
        for (const name of names) {
            const products = await StoreProduct.findAll({
                where: {
                    ingredient_name: { [Op.like]: `%${name}%` },
                    store_name: 'WinMart',
                    in_stock: true
                },
                order: [['price_dong', 'ASC']],
                limit: 3
            });
            result[name] = products.map(p => ({
                id:           p.id,
                product_name: p.product_name,
                unit:         p.unit,
                price_dong:   p.price_dong,
                store_name:   p.store_name,
                store_logo_url: p.store_logo_url,
                image_url:    p.image_url,
                rating:       p.rating
            }));
        }

        res.json(result);
    } catch (e) { res.status(500).json({ error: e.message }); }
});

// GET /api/stores — danh sách cửa hàng
router.get('/stores', async (req, res) => {
    try {
        const stores = await StoreProduct.findAll({
            attributes: [[require('sequelize').fn('DISTINCT', require('sequelize').col('store_name')), 'store_name'], 'store_logo_url'],
            where: { in_stock: true }
        });
        res.json([...new Map(stores.map(s => [s.store_name, s])).values()]);
    } catch (e) { res.status(500).json({ error: e.message }); }
});

module.exports = router;
