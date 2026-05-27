const express = require('express');
const router = express.Router();
const { Op, Sequelize } = require('sequelize');
const {
    Category, DietType, Recipe, RecipeStep, RecipeIngredient,
    Ingredient, NutritionFact, Review, Favorite, RecipeCategory
} = require('../models');
const { authenticateToken } = require('../middleware/auth');

// ─────────────────────────────────────────────
// HELPERS (shared recipe includes)
// ─────────────────────────────────────────────
const recipeIncludes = [
    { model: Category },
    { model: Category, as: 'categories', through: { attributes: [] } },
    { model: DietType, as: 'dietTypes', through: { attributes: [] } },
    { model: DietType, as: 'primaryDietType' },
    { model: RecipeStep, as: 'steps', order: [['step_number', 'ASC']] },
    { model: NutritionFact, as: 'nutrition' },
    {
        model: Ingredient,
        as: 'ingredients',
        through: { attributes: ['quantity', 'unit'] }
    }
];

const recipeSummaryAttributes = ['id', 'title', 'image_url', 'cook_time', 'difficulty', 'servings', 'calories', 'category_id', 'diet_type_id'];

// ─────────────────────────────────────────────
// CATEGORIES
// ─────────────────────────────────────────────
router.get('/categories', async (req, res) => {
    try {
        const categories = await Category.findAll();
        res.json(categories);
    } catch (e) { res.status(500).json({ error: e.message }); }
});

// ─────────────────────────────────────────────
// INGREDIENTS
// ─────────────────────────────────────────────
router.get('/ingredients', async (req, res) => {
    try {
        const ingredients = await Ingredient.findAll({ order: [['name', 'ASC']] });
        res.json(ingredients);
    } catch (e) { res.status(500).json({ error: e.message }); }
});

// ─────────────────────────────────────────────
// RECIPES (with pagination)
// ─────────────────────────────────────────────

// GET /api/recipes  (+ ?search, ?category, ?diet, ?limit, ?page, ?filters)
router.get('/recipes', async (req, res) => {
    try {
        const where = {};
        if (req.query.diet)     where.diet_type_id = req.query.diet;
        if (req.query.search) {
            where.title = { [Op.like]: `%${req.query.search}%` };
        }

        // Multi-diet filter
        if (req.query.filters) {
            const filters = req.query.filters.split(',').map(f => f.trim().toLowerCase());
            const filterMap = { vegetarian: 2, keto: 3, lowcarb: 4, glutenfree: 5 };
            const dietIds = [];
            filters.forEach(f => { if (filterMap[f]) dietIds.push(filterMap[f]); });
            if (dietIds.length > 0) {
                if (where.diet_type_id) dietIds.push(where.diet_type_id);
                where.diet_type_id = { [Op.in]: dietIds };
            }
        }

        // Category M-N filter
        let includeOverride = [...recipeIncludes];
        if (req.query.category) {
            includeOverride = recipeIncludes.map(inc => {
                if (inc.as === 'categories') {
                    return { ...inc, where: { id: req.query.category }, required: true };
                }
                return inc;
            });
        }

        const options = {
            where,
            include: includeOverride,
            order: [['id', 'ASC']]
        };

        // Pagination support
        const page = parseInt(req.query.page) || 0;
        const limit = parseInt(req.query.limit) || 0;
        if (limit > 0) {
            options.limit = limit;
            if (page > 0) options.offset = (page - 1) * limit;
        }

        const recipes = await Recipe.findAll(options);
        res.json(recipes);
    } catch (e) { res.status(500).json({ error: e.message }); }
});

// GET /api/recipes/search?q=keyword
router.get('/recipes/search', async (req, res) => {
    try {
        const q = req.query.q || '';
        const limit = parseInt(req.query.limit) || 50;
        const page = parseInt(req.query.page) || 1;

        const recipes = await Recipe.findAll({
            where: { title: { [Op.like]: `%${q}%` } },
            include: [{ model: Category }],
            order: [['id', 'ASC']],
            limit,
            offset: (page - 1) * limit
        });
        res.json(recipes);
    } catch (e) { res.status(500).json({ error: e.message }); }
});

// GET /api/recipes/recommendations — AI-1: Gợi ý công thức thông minh
router.get('/recipes/recommendations', authenticateToken, async (req, res) => {
    try {
        const favorites = await Favorite.findAll({
            where: { user_id: req.user.id },
            include: [{ model: Recipe }]
        });

        const favoritedRecipeIds = favorites.map(f => f.recipe_id);
        let recommendedRecipes = [];

        if (favoritedRecipeIds.length > 0) {
            const categoryCounts = {};
            favorites.forEach(f => {
                const r = f.Recipe;
                if (r && r.category_id) categoryCounts[r.category_id] = (categoryCounts[r.category_id] || 0) + 1;
            });

            const topCategories = Object.keys(categoryCounts).sort((a, b) => categoryCounts[b] - categoryCounts[a]).slice(0, 2);

            if (topCategories.length > 0) {
                recommendedRecipes = await Recipe.findAll({
                    where: {
                        category_id: { [Op.in]: topCategories },
                        id: { [Op.notIn]: favoritedRecipeIds }
                    },
                    include: recipeIncludes,
                    limit: 8,
                    order: Sequelize.literal('RANDOM()')
                });
            }
        }

        if (recommendedRecipes.length < 5) {
            const excludeIds = [...favoritedRecipeIds, ...recommendedRecipes.map(r => r.id)];
            const fallbackRecipes = await Recipe.findAll({
                where: { id: { [Op.notIn]: excludeIds } },
                include: recipeIncludes,
                limit: 10 - recommendedRecipes.length,
                order: Sequelize.literal('RANDOM()')
            });
            recommendedRecipes = [...recommendedRecipes, ...fallbackRecipes];
        }

        res.json(recommendedRecipes);
    } catch (e) { res.status(500).json({ error: e.message }); }
});

// POST /api/recipes/smart-fridge — AI-2: Gợi ý theo nguyên liệu (Optimized)
router.post('/recipes/smart-fridge', async (req, res) => {
    try {
        const { ingredients } = req.body;
        if (!ingredients || !Array.isArray(ingredients) || ingredients.length === 0) {
            return res.status(400).json({ error: "Missing ingredients array" });
        }

        const userIngs = ingredients.map(i => i.toLowerCase().trim()).filter(i => i);

        // OPTIMIZED: Use RecipeIngredient to pre-filter recipes with matching ingredients
        const matchConditions = userIngs.map(ing => ({ name: { [Op.like]: `%${ing}%` } }));

        const matchingIngredientIds = await Ingredient.findAll({
            attributes: ['id'],
            where: { [Op.or]: matchConditions }
        });

        const ingIds = matchingIngredientIds.map(i => i.id);

        // If no matching ingredients found at all, return empty
        if (ingIds.length === 0) {
            return res.json([]);
        }

        // Only load recipes that have at least one matching ingredient via join
        const { RecipeIngredient } = require('../models');
        const matchingRecipeIds = await RecipeIngredient.findAll({
            attributes: [[Sequelize.fn('DISTINCT', Sequelize.col('recipe_id')), 'recipe_id']],
            where: { ingredient_id: { [Op.in]: ingIds } }
        });

        const recipeIds = matchingRecipeIds.map(r => r.recipe_id);
        if (recipeIds.length === 0) {
            return res.json([]);
        }

        const candidateRecipes = await Recipe.findAll({
            where: { id: { [Op.in]: recipeIds } },
            include: recipeIncludes
        });

        const scoredRecipes = [];
        for (let recipe of candidateRecipes) {
            let matchCount = 0;
            const recipeIngs = recipe.ingredients || [];

            recipeIngs.forEach(ri => {
                if (!ri.name) return;
                const dbName = ri.name.toLowerCase();
                for (let input of userIngs) {
                    if (dbName.includes(input)) { matchCount++; break; }
                }
            });

            if (matchCount > 0) {
                const totalRecipeIngs = recipeIngs.length > 0 ? recipeIngs.length : 1;
                const matchPercentage = Math.round((matchCount / totalRecipeIngs) * 100);
                const plainRecipe = recipe.get({ plain: true });
                plainRecipe.matchPercentage = matchPercentage;
                plainRecipe.matchCount = matchCount;
                scoredRecipes.push(plainRecipe);
            }
        }

        scoredRecipes.sort((a, b) => {
            if (b.matchPercentage !== a.matchPercentage) return b.matchPercentage - a.matchPercentage;
            return b.matchCount - a.matchCount;
        });

        res.json(scoredRecipes.slice(0, 50));
    } catch (e) { res.status(500).json({ error: e.message }); }
});

// GET /api/recipes/category/:categoryId
router.get('/recipes/category/:categoryId', async (req, res) => {
    try {
        let includeOverride = recipeIncludes.map(inc => {
            if (inc.as === 'categories') {
                return { ...inc, where: { id: req.params.categoryId }, required: true };
            }
            return inc;
        });

        const recipes = await Recipe.findAll({
            include: includeOverride,
            order: [['id', 'ASC']]
        });
        res.json(recipes);
    } catch (e) { res.status(500).json({ error: e.message }); }
});

// GET /api/recipes/:id — chi tiết 1 công thức
/**
 * GET /api/recipes/:id
 * Lấy chi tiết công thức, bao gồm video_url để app mở Video nấu ăn/Cooking Mode.
 */
router.get('/recipes/:id', async (req, res) => {
    try {
        const recipe = await Recipe.findByPk(req.params.id, { include: recipeIncludes });
        if (!recipe) return res.status(404).json({ error: 'Recipe not found' });
        res.json(recipe);
    } catch (e) { res.status(500).json({ error: e.message }); }
});

// POST /api/recipes — tạo công thức mới (yêu cầu đăng nhập)
router.post('/recipes', authenticateToken, async (req, res) => {
    try {
        const { title, description, image_url, cook_time, difficulty, servings, calories, category_id, diet_type_id } = req.body;
        if (!title) return res.status(400).json({ error: 'title is required' });
        if (cook_time && (isNaN(cook_time) || cook_time < 0)) return res.status(400).json({ error: 'cook_time must be a positive number' });
        if (servings && (isNaN(servings) || servings < 1)) return res.status(400).json({ error: 'servings must be at least 1' });
        const recipe = await Recipe.create({ title, description, image_url, cook_time, difficulty, servings, calories, category_id, diet_type_id });
        res.status(201).json(recipe);
    } catch (e) { res.status(500).json({ error: e.message }); }
});

// ─────────────────────────────────────────────
// RECIPE STEPS
// ─────────────────────────────────────────────
/**
 * GET /api/recipes/:id/steps
 * Trả về danh sách bước nấu cho Cooking Mode. Các cột timer_seconds và
 * video_start_time được Android dùng để hiển thị hẹn giờ và tua video đúng đoạn.
 */
router.get('/recipes/:id/steps', async (req, res) => {
    try {
        const steps = await RecipeStep.findAll({
            where: { recipe_id: req.params.id },
            order: [['step_number', 'ASC']]
        });
        res.json(steps);
    } catch (e) { res.status(500).json({ error: e.message }); }
});

router.post('/recipes/:id/steps', authenticateToken, async (req, res) => {
    try {
        const recipe = await Recipe.findByPk(req.params.id);
        if (!recipe) return res.status(404).json({ error: 'Recipe not found' });

        const { step_number, instruction, timer_seconds } = req.body;
        if (!instruction) return res.status(400).json({ error: 'instruction is required' });
        const step = await RecipeStep.create({
            recipe_id: req.params.id, step_number, instruction, timer_seconds: timer_seconds || 0
        });
        res.status(201).json(step);
    } catch (e) { res.status(500).json({ error: e.message }); }
});

// ─────────────────────────────────────────────
// RECIPE INGREDIENTS
// ─────────────────────────────────────────────
router.get('/recipes/:id/ingredients', async (req, res) => {
    try {
        const recipe = await Recipe.findByPk(req.params.id, {
            include: [{
                model: Ingredient, as: 'ingredients',
                through: { attributes: ['quantity', 'unit'] }
            }]
        });
        if (!recipe) return res.status(404).json({ error: 'Recipe not found' });
        res.json(recipe.ingredients);
    } catch (e) { res.status(500).json({ error: e.message }); }
});

// ─────────────────────────────────────────────
// NUTRITION FACTS
// ─────────────────────────────────────────────
router.get('/recipes/:id/nutrition', async (req, res) => {
    try {
        const nutrition = await NutritionFact.findOne({ where: { recipe_id: req.params.id } });
        if (!nutrition) return res.status(404).json({ error: 'No nutrition data' });
        res.json(nutrition);
    } catch (e) { res.status(500).json({ error: e.message }); }
});

// ─────────────────────────────────────────────
// REVIEWS
// ─────────────────────────────────────────────
router.get('/recipes/:id/reviews', async (req, res) => {
    try {
        const reviews = await Review.findAll({
            where: { recipe_id: req.params.id },
            order: [['id', 'DESC']]
        });
        const avg = reviews.length
            ? (reviews.reduce((s, r) => s + r.rating, 0) / reviews.length).toFixed(1)
            : null;
        res.json({ average_rating: avg, count: reviews.length, reviews });
    } catch (e) { res.status(500).json({ error: e.message }); }
});

router.post('/recipes/:id/reviews', authenticateToken, async (req, res) => {
    try {
        const { rating, comment } = req.body;
        if (!rating || rating < 1 || rating > 5)
            return res.status(400).json({ error: 'rating must be between 1 and 5' });

        const existing = await Review.findOne({
            where: { recipe_id: req.params.id, user_id: req.user.id }
        });
        if (existing) {
            existing.rating  = rating;
            existing.comment = comment || existing.comment;
            await existing.save();
            return res.json({ ...existing.toJSON(), updated: true });
        }

        const review = await Review.create({
            recipe_id: req.params.id,
            rating,
            comment,
            author: req.user.email,
            user_id: req.user.id
        });
        res.status(201).json(review);
    } catch (e) { res.status(500).json({ error: e.message }); }
});

router.delete('/recipes/:id/reviews/:reviewId', authenticateToken, async (req, res) => {
    try {
        const review = await Review.findOne({
            where: { id: req.params.reviewId, recipe_id: req.params.id, user_id: req.user.id }
        });
        if (!review) return res.status(404).json({ error: 'Review not found or not authorized' });
        await review.destroy();
        res.json({ message: 'Review deleted' });
    } catch (e) { res.status(500).json({ error: e.message }); }
});

// Export both router and shared helpers
module.exports = router;
module.exports.recipeIncludes = recipeIncludes;
module.exports.recipeSummaryAttributes = recipeSummaryAttributes;
