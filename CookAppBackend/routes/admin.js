const express = require('express');
const router = express.Router();
const { Op, Sequelize } = require('sequelize');
const {
    User, Recipe, Category, Order, Post, PostComment,
    Review, Favorite, StoreProduct, Notification,
    Ingredient, RecipeStep, NutritionFact, PostLike,
    sequelize
} = require('../models');
const { adminAuth } = require('../middleware/adminAuth');
const multer = require('multer');
const path = require('path');
const fs = require('fs');
const { GoogleGenerativeAI } = require('@google/generative-ai');
const { GoogleAIFileManager } = require('@google/generative-ai/server');

// Đảm bảo các thư mục lưu trữ tĩnh tồn tại
const imageDest = path.join(__dirname, '..', 'public', 'videos', 'thumbnails');
const videoDest = path.join(__dirname, '..', 'public', 'videos');
fs.mkdirSync(imageDest, { recursive: true });
fs.mkdirSync(videoDest, { recursive: true });

const storage = multer.diskStorage({
    destination: (req, file, cb) => {
        if (file.fieldname === 'image') cb(null, imageDest);
        else if (file.fieldname === 'video') cb(null, videoDest);
        else cb(null, path.join(__dirname, '..', 'public'));
    },
    filename: (req, file, cb) => {
        let ext = path.extname(file.originalname);
        if (!ext || ext === '.tmp' || ext === '.blob') {
            if (file.mimetype && file.mimetype.startsWith('image/')) ext = '.jpg';
            else if (file.mimetype && file.mimetype.startsWith('video/')) ext = '.mp4';
            else ext = '.bin';
        }
        const name = Date.now() + '-' + Math.round(Math.random() * 1E9);
        cb(null, file.fieldname + '-' + name + ext);
    }
});
const upload = multer({ storage });

// Tất cả route trong file này đều yêu cầu admin auth
router.use(adminAuth);

// ─────────────────────────────────────────────
// THỐNG KÊ HỆ THỐNG (DASHBOARD)
// ─────────────────────────────────────────────
router.get('/stats', async (req, res) => {
    try {
        const [users, recipes, orders, posts, reviews, ingredients] = await Promise.all([
            User.count(),
            Recipe.count(),
            Order.count(),
            Post.count(),
            Review.count(),
            Ingredient.count()
        ]);

        // Tính tổng doanh thu
        const revenueResult = await Order.findOne({
            attributes: [[Sequelize.fn('SUM', Sequelize.col('totalPrice')), 'total']],
            where: { status: { [Op.ne]: 'Đã hủy' } }
        });
        const revenue = revenueResult?.dataValues?.total || 0;

        // Thống kê đơn hàng theo trạng thái
        const ordersByStatus = await Order.findAll({
            attributes: ['status', [Sequelize.fn('COUNT', Sequelize.col('id')), 'count']],
            group: ['status']
        });

        // Lấy 5 đơn hàng gần đây nhất
        const recentOrders = await Order.findAll({
            order: [['created_at', 'DESC']],
            limit: 5,
            include: [{ model: User, attributes: ['fullName', 'email'] }]
        });

        // Thống kê số lượng người dùng đăng ký mới trong ngày hôm nay
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        const newUsersToday = await User.count({
            where: { created_at: { [Op.gte]: today } }
        });

        res.json({
            users, recipes, orders, posts, reviews, ingredients,
            revenue,
            ordersByStatus: ordersByStatus.map(o => o.dataValues),
            recentOrders,
            newUsersToday
        });
    } catch (e) { res.status(500).json({ error: e.message }); }
});

// ─────────────────────────────────────────────
// QUẢN LÝ TÀI KHOẢN NGƯỜI DÙNG (USERS)
// ─────────────────────────────────────────────
router.get('/users', async (req, res) => {
    try {
        const page = parseInt(req.query.page) || 1;
        const limit = parseInt(req.query.limit) || 20;
        const search = req.query.search || '';

        const where = {};
        if (search) {
            where[Op.or] = [
                { email: { [Op.like]: `%${search}%` } },
                { fullName: { [Op.like]: `%${search}%` } }
            ];
        }

        const { count, rows } = await User.findAndCountAll({
            where,
            attributes: { exclude: ['password'] },
            order: [['created_at', 'DESC']],
            limit,
            offset: (page - 1) * limit
        });

        res.json({ total: count, page, totalPages: Math.ceil(count / limit), users: rows });
    } catch (e) { res.status(500).json({ error: e.message }); }
});

router.patch('/users/:id/role', async (req, res) => {
    try {
        const { role } = req.body;
        if (!['user', 'admin'].includes(role)) {
            return res.status(400).json({ error: 'role must be "user" or "admin"' });
        }
        const user = await User.findByPk(req.params.id);
        if (!user) return res.status(404).json({ error: 'User not found' });
        user.role = role;
        await user.save();
        res.json({ message: 'Role updated', user: { id: user.id, email: user.email, role: user.role } });
    } catch (e) { res.status(500).json({ error: e.message }); }
});

router.delete('/users/:id', async (req, res) => {
    try {
        const user = await User.findByPk(req.params.id);
        if (!user) return res.status(404).json({ error: 'User not found' });
        if (user.id === req.user.id) return res.status(400).json({ error: 'Cannot delete yourself' });
        await user.destroy();
        res.json({ message: 'User deleted' });
    } catch (e) { res.status(500).json({ error: e.message }); }
});

// ─────────────────────────────────────────────
// QUẢN LÝ ĐƠN HÀNG SIÊU THỊ (ORDERS)
// ─────────────────────────────────────────────
router.get('/orders', async (req, res) => {
    try {
        const page = parseInt(req.query.page) || 1;
        const limit = parseInt(req.query.limit) || 20;
        const status = req.query.status || '';

        const where = {};
        if (status) where.status = status;

        const { count, rows } = await Order.findAndCountAll({
            where,
            include: [{ model: User, attributes: ['fullName', 'email'] }],
            order: [['created_at', 'DESC']],
            limit,
            offset: (page - 1) * limit
        });

        res.json({ total: count, page, totalPages: Math.ceil(count / limit), orders: rows });
    } catch (e) { res.status(500).json({ error: e.message }); }
});

router.patch('/orders/:id/status', async (req, res) => {
    try {
        const { status } = req.body;
        const validStatuses = ['Chờ xác nhận', 'Đang giao', 'Hoàn thành', 'Đã hủy'];
        if (!validStatuses.includes(status)) {
            return res.status(400).json({ error: `status must be one of: ${validStatuses.join(', ')}` });
        }

        const order = await Order.findByPk(req.params.id);
        if (!order) return res.status(404).json({ error: 'Order not found' });

        order.status = status;
        if (status === 'Đã hủy') {
            order.cancelReason = req.body.reason || 'Admin cancelled';
            order.cancelledAt = new Date();
        }
        await order.save();
        res.json(order);
    } catch (e) { res.status(500).json({ error: e.message }); }
});

// ─────────────────────────────────────────────
// QUẢN LÝ CÔNG THỨC NẤU ĂN (RECIPES)
// ─────────────────────────────────────────────
router.get('/recipes', async (req, res) => {
    try {
        const page = parseInt(req.query.page) || 1;
        const limit = parseInt(req.query.limit) || 20;
        const search = req.query.search || '';

        const where = {};
        if (search) where.title = { [Op.like]: `%${search}%` };

        const { count, rows } = await Recipe.findAndCountAll({
            where,
            include: [{ model: Category }],
            order: [['id', 'ASC']],
            limit,
            offset: (page - 1) * limit
        });

        res.json({ total: count, page, totalPages: Math.ceil(count / limit), recipes: rows });
    } catch (e) { res.status(500).json({ error: e.message }); }
});

router.delete('/recipes/:id', async (req, res) => {
    try {
        const recipe = await Recipe.findByPk(req.params.id);
        if (!recipe) return res.status(404).json({ error: 'Recipe not found' });
        // Tự động xóa liên đới (cascade) các bảng bước nấu, dinh dưỡng, đánh giá, yêu thích liên quan
        await RecipeStep.destroy({ where: { recipe_id: recipe.id } });
        await NutritionFact.destroy({ where: { recipe_id: recipe.id } });
        await Review.destroy({ where: { recipe_id: recipe.id } });
        await Favorite.destroy({ where: { recipe_id: recipe.id } });
        await recipe.destroy();
        res.json({ message: 'Recipe deleted' });
    } catch (e) { res.status(500).json({ error: e.message }); }
});

// ─────────────────────────────────────────────
// QUẢN LÝ BÀI ĐĂNG CỘNG ĐỒNG (POSTS)
// ─────────────────────────────────────────────
router.get('/posts', async (req, res) => {
    try {
        const page = parseInt(req.query.page) || 1;
        const limit = parseInt(req.query.limit) || 20;

        const { count, rows } = await Post.findAndCountAll({
            include: [{ model: PostComment, as: 'comments' }],
            order: [['created_at', 'DESC']],
            limit,
            offset: (page - 1) * limit
        });

        res.json({ total: count, page, totalPages: Math.ceil(count / limit), posts: rows });
    } catch (e) { res.status(500).json({ error: e.message }); }
});

router.delete('/posts/:id', async (req, res) => {
    try {
        const post = await Post.findByPk(req.params.id);
        if (!post) return res.status(404).json({ error: 'Post not found' });
        await PostComment.destroy({ where: { post_id: post.id } });
        await PostLike.destroy({ where: { post_id: post.id } });
        await post.destroy();
        res.json({ message: 'Post deleted' });
    } catch (e) { res.status(500).json({ error: e.message }); }
});

// ─────────────────────────────────────────────
// QUẢN LÝ ĐÁNH GIÁ CÔNG THỨC (REVIEWS)
// ─────────────────────────────────────────────
router.get('/reviews', async (req, res) => {
    try {
        const page = parseInt(req.query.page) || 1;
        const limit = parseInt(req.query.limit) || 20;

        const { count, rows } = await Review.findAndCountAll({
            include: [{ model: Recipe, attributes: ['id', 'title'] }],
            order: [['id', 'DESC']],
            limit,
            offset: (page - 1) * limit
        });

        res.json({ total: count, page, totalPages: Math.ceil(count / limit), reviews: rows });
    } catch (e) { res.status(500).json({ error: e.message }); }
});

router.delete('/reviews/:id', async (req, res) => {
    try {
        const review = await Review.findByPk(req.params.id);
        if (!review) return res.status(404).json({ error: 'Review not found' });
        await review.destroy();
        res.json({ message: 'Review deleted' });
    } catch (e) { res.status(500).json({ error: e.message }); }
});

// ─────────────────────────────────────────────
// METADATA & KHỞI TẠO CÔNG THỨC
// ─────────────────────────────────────────────
const { DietType, RecipeCategory, RecipeDietType, RecipeIngredient } = require('../models');

router.get('/recipe-metadata', async (req, res) => {
    try {
        const [categories, dietTypes, ingredients] = await Promise.all([
            Category.findAll({ attributes: ['id', 'name'] }),
            DietType.findAll({ attributes: ['id', 'name'] }),
            Ingredient.findAll({ attributes: ['id', 'name'], order: [['name', 'ASC']] })
        ]);
        res.json({ categories, dietTypes, ingredients });
    } catch (e) { res.status(500).json({ error: e.message }); }
});

router.get('/recipes/:id/steps', async (req, res) => {
    try {
        const steps = await RecipeStep.findAll({
            where: { recipe_id: req.params.id },
            order: [['step_number', 'ASC']]
        });
        res.json(steps);
    } catch (e) { res.status(500).json({ error: e.message }); }
});

router.put('/recipes/:id/steps', async (req, res) => {
    try {
        const { steps } = req.body;
        if (!steps || !Array.isArray(steps)) return res.status(400).json({error: "Invalid data"});

        for (const step of steps) {
            if (step.id) {
                await RecipeStep.update(
                    { video_start_time: step.video_start_time || 0 },
                    { where: { id: step.id, recipe_id: req.params.id }}
                );
            }
        }
        res.json({ success: true });
    } catch (e) { res.status(500).json({ error: e.message }); }
});

router.post('/recipes/:id/auto-sync-video', async (req, res) => {
    try {
        const recipeId = req.params.id;
        const recipe = await Recipe.findByPk(recipeId);
        if (!recipe) return res.status(404).json({ error: "Recipe not found" });

        const steps = await RecipeStep.findAll({
            where: { recipe_id: recipeId },
            order: [['step_number', 'ASC']]
        });
        if (steps.length === 0) return res.status(400).json({ error: "Recipe has no steps" });

        let videoUrl = recipe.video_url;
        if (!videoUrl) return res.status(400).json({ error: "Recipe has no video" });

        // Phân tích xem video có phải là mp4 cục bộ không
        // VD: http://10.0.2.2:3000/videos/ga_xao_xa_ot.mp4
        let localPath = "";
        if (videoUrl.includes('/videos/') && videoUrl.endsWith('.mp4')) {
            const fileName = videoUrl.split('/videos/').pop();
            localPath = path.join(__dirname, '../public/videos/', fileName);
        } else {
             return res.status(400).json({ error: "Chỉ hỗ trợ video MP4 cục bộ được upload." });
        }

        if (!fs.existsSync(localPath)) return res.status(404).json({ error: "Video file not found on disk: " + localPath });

        const fileManager = new GoogleAIFileManager(process.env.GEMINI_API_KEY);
        const genAI = new GoogleGenerativeAI(process.env.GEMINI_API_KEY);

        console.log("Uploading to Gemini File API...");
        const uploadResult = await fileManager.uploadFile(localPath, {
            mimeType: 'video/mp4',
            displayName: recipe.title,
        });

        // Chờ Gemini xử lý video xong
        let fileInfo = await fileManager.getFile(uploadResult.file.name);
        let maxWait = 0;
        while (fileInfo.state === 'PROCESSING' && maxWait < 120) {
            console.log('Waiting for video processing...');
            await new Promise(r => setTimeout(r, 2000));
            fileInfo = await fileManager.getFile(uploadResult.file.name);
            maxWait += 2;
        }

        if (fileInfo.state === 'FAILED') throw new Error('Video processing on Gemini failed');

        const model = genAI.getGenerativeModel({ model: 'gemini-2.0-flash' });

        const stepDataForPrompt = steps.map(s => ({ id: s.id, number: s.step_number, text: s.instruction }));

        const prompt = `You are a culinary temporal-sync agent. Analyze this video for the recipe "${recipe.title}". 
Map the following instructional steps to the EXACT second they begin in the video.
Steps:
${JSON.stringify(stepDataForPrompt, null, 2)}

Return purely a JSON array of objects without any markdown blocks. Format: [ { "id": number, "video_start_time": number } ]`;

        console.log("Requesting generation...");
        let result = await model.generateContent([
            { fileData: { mimeType: uploadResult.file.mimeType, fileUri: uploadResult.file.uri } },
            { text: prompt }
        ]);

        let responseText = result.response.text();
        // Xóa block markdown (nếu có)
        responseText = responseText.replace(/```json/g, '').replace(/```/g, '').trim();
        const jsonResult = JSON.parse(responseText);

        // Lưu vào database
        for (const item of jsonResult) {
            if (item.id && typeof item.video_start_time === 'number') {
                await RecipeStep.update(
                    { video_start_time: item.video_start_time },
                    { where: { id: item.id, recipe_id: recipeId } }
                );
            }
        }

        // Dọn dẹp
        await fileManager.deleteFile(uploadResult.file.name);

        res.json({ success: true, message: "Đồng bộ thành công", data: jsonResult });
    } catch (e) {
        console.error("Auto Sync Error:", e);
        res.status(500).json({ error: e.message });
    }
});

router.post('/recipes', upload.fields([{name: 'image', maxCount: 1}, {name: 'video', maxCount: 1}]), async (req, res) => {
    // Transaction wrapper for atomic creation
    const t = await sequelize.transaction();
    try {
        let data = req.body;
        // Nếu tải lên qua multipart, dữ liệu dạng cấu trúc có thể được truyền dưới dạng chuỗi trong trường "data"
        if (req.body.data) {
            data = JSON.parse(req.body.data);
        }

        let imageUrl = data.imageUrl || '';
        let videoUrl = data.videoUrl || null;

        if (req.files && req.files['image'] && req.files['image'].length > 0) {
            const f = req.files['image'][0];
            imageUrl = `/videos/thumbnails/${f.filename}`;
        }
        
        if (req.files && req.files['video'] && req.files['video'].length > 0) {
            const v = req.files['video'][0];
            videoUrl = `/videos/${v.filename}`;
        }
        
        // 1. Tạo bản ghi thông tin công thức cơ bản
        const recipe = await Recipe.create({
            title: data.title,
            description: data.description,
            image_url: imageUrl,
            video_url: videoUrl,
            cook_time: data.cookTime,
            difficulty: data.difficulty,
            servings: data.servings,
            calories: data.calories,
            // Thiết lập danh mục và chế độ ăn uống mặc định (lấy phần tử đầu tiên)
            category_id: data.categoryIds && data.categoryIds.length > 0 ? data.categoryIds[0] : null,
            diet_type_id: data.dietTypeIds && data.dietTypeIds.length > 0 ? data.dietTypeIds[0] : null,
        }, { transaction: t });

        // 2. Thêm thông tin dinh dưỡng chi tiết
        if (data.nutritionFacts) {
            await NutritionFact.create({
                recipe_id: recipe.id,
                calories: data.nutritionFacts.calories || 0,
                protein: data.nutritionFacts.protein || 0,
                fat: data.nutritionFacts.fat || 0,
                carbs: data.nutritionFacts.carbs || 0,
                fiber: data.nutritionFacts.fiber || 0,
                sugar: data.nutritionFacts.sugar || 0,
                sodium: data.nutritionFacts.sodium || 0
            }, { transaction: t });
        }

        // 3. Category Mapping
        if (data.categoryIds && data.categoryIds.length > 0) {
            const catMappings = data.categoryIds.map(catId => ({
                recipe_id: recipe.id,
                category_id: catId
            }));
            await RecipeCategory.bulkCreate(catMappings, { transaction: t });
        }

        // 4. DietType Mapping
        if (data.dietTypeIds && data.dietTypeIds.length > 0) {
            const dietMappings = data.dietTypeIds.map(dtId => ({
                recipe_id: recipe.id,
                diet_type_id: dtId
            }));
            await RecipeDietType.bulkCreate(dietMappings, { transaction: t });
        }

        // 5. Khởi tạo danh sách các bước nấu ăn tương ứng
        if (data.steps && data.steps.length > 0) {
            const stepObjs = data.steps.map((step, idx) => ({
                recipe_id: recipe.id,
                step_number: idx + 1,
                title: step.title || '',
                instruction: step.instruction,
                timer_seconds: step.timerSeconds || 0,
                video_start_time: step.videoStartTime || 0
            }));
            await RecipeStep.bulkCreate(stepObjs, { transaction: t });
        }

        // 6. Liên kết nguyên liệu với công thức món ăn
        if (data.ingredients && data.ingredients.length > 0) {
            const ingObjs = [];
            for (const ing of data.ingredients) {
                let ingId = ing.ingredientId;
                
                // Nếu không có mã ID nguyên liệu nhưng có tên nguyên liệu, tự động tìm hoặc tạo mới trong danh mục Khác
                if (!ingId && ing.ingredientName) {
                    const [newIng] = await Ingredient.findOrCreate({
                        where: { name: ing.ingredientName },
                        defaults: { name: ing.ingredientName, category: 'Khác', image_url: 'placeholder_ingredient.jpg' },
                        transaction: t
                    });
                    ingId = newIng.id;
                }

                if (ingId) {
                    ingObjs.push({
                        recipe_id: recipe.id,
                        ingredient_id: ingId,
                        quantity: ing.quantity || 1,
                        unit: ing.unit || ''
                    });
                }
            }
            if (ingObjs.length > 0) {
                await RecipeIngredient.bulkCreate(ingObjs, { transaction: t });
            }
        }

        await t.commit();
        res.status(201).json({ message: 'Recipe created successfully', recipeId: recipe.id });
    } catch (e) {
        await t.rollback();
        res.status(500).json({ error: e.message });
    }
});

module.exports = router;
