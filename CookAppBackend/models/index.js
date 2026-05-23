const sequelize = require('../config/database');

const Category = require('./Category');
const DietType = require('./DietType');
const Ingredient = require('./Ingredient');
const Recipe = require('./Recipe');
const RecipeStep = require('./RecipeStep');
const RecipeIngredient = require('./RecipeIngredient');
const RecipeCategory = require('./RecipeCategory');
const RecipeDietType = require('./RecipeDietType');
const NutritionFact = require('./NutritionFact');
const Review = require('./Review');
const Post = require('./Post');
const PostComment = require('./PostComment');
const User = require('./User');
const Favorite = require('./Favorite');
const ShoppingList = require('./ShoppingList');
const ShoppingListItem = require('./ShoppingListItem');
const Notification = require('./Notification');
const PostLike = require('./PostLike');
const StoreProduct = require('./StoreProduct');
const SavedPost = require('./SavedPost');
const Order = require('./Order');

// Define Relationships

// Recipe <-> Category (M-N qua RecipeCategory — 1 món có thể có nhiều danh mục)
Recipe.belongsToMany(Category, { through: RecipeCategory, foreignKey: 'recipe_id', as: 'categories' });
Category.belongsToMany(Recipe, { through: RecipeCategory, foreignKey: 'category_id' });

// Giữ lại quan hệ 1-N cũ (category_id column) cho backwards-compat
Category.hasMany(Recipe, { foreignKey: 'category_id', as: 'primaryRecipes' });
Recipe.belongsTo(Category, { foreignKey: 'category_id' });

// Recipe <-> DietType (M-N qua RecipeDietType)
Recipe.belongsToMany(DietType, { through: RecipeDietType, foreignKey: 'recipe_id', as: 'dietTypes' });
DietType.belongsToMany(Recipe, { through: RecipeDietType, foreignKey: 'diet_type_id' });

// Backward compatibility cho cột diet_type_id cũ nếu có
DietType.hasMany(Recipe, { foreignKey: 'diet_type_id', as: 'primaryRecipes' });
Recipe.belongsTo(DietType, { foreignKey: 'diet_type_id', as: 'primaryDietType' });

// Recipe <-> RecipeStep (1-N)
Recipe.hasMany(RecipeStep, { foreignKey: 'recipe_id', as: 'steps' });
RecipeStep.belongsTo(Recipe, { foreignKey: 'recipe_id' });

// Recipe <-> NutritionFact (1-1)
Recipe.hasOne(NutritionFact, { foreignKey: 'recipe_id', as: 'nutrition' });
NutritionFact.belongsTo(Recipe, { foreignKey: 'recipe_id' });

// Recipe <-> Review (1-N)
Recipe.hasMany(Review, { foreignKey: 'recipe_id', as: 'reviews' });
Review.belongsTo(Recipe, { foreignKey: 'recipe_id' });

// Recipe <-> Ingredient (M-N through RecipeIngredient)
Recipe.belongsToMany(Ingredient, { through: RecipeIngredient, foreignKey: 'recipe_id', as: 'ingredients' });
Ingredient.belongsToMany(Recipe, { through: RecipeIngredient, foreignKey: 'ingredient_id' });

// Post <-> PostComment (1-N)
Post.hasMany(PostComment, { foreignKey: 'post_id', as: 'comments' });
PostComment.belongsTo(Post, { foreignKey: 'post_id' });

// ----------------------------------------------------
// New User Associations (Phase 3)
// ----------------------------------------------------

// User <-> Favorite (1-N)
User.hasMany(Favorite, { foreignKey: 'user_id' });
Favorite.belongsTo(User, { foreignKey: 'user_id' });

// Recipe <-> Favorite (1-N)
Recipe.hasMany(Favorite, { foreignKey: 'recipe_id' });
Favorite.belongsTo(Recipe, { foreignKey: 'recipe_id' });

// User <-> Notification (1-N)
User.hasMany(Notification, { foreignKey: 'user_id' });
Notification.belongsTo(User, { foreignKey: 'user_id' });

// User <-> ShoppingList (1-1)
User.hasOne(ShoppingList, { foreignKey: 'user_id' });
ShoppingList.belongsTo(User, { foreignKey: 'user_id' });

// ShoppingList <-> ShoppingListItem (1-N)
ShoppingList.hasMany(ShoppingListItem, { foreignKey: 'shopping_list_id', as: 'items' });
ShoppingListItem.belongsTo(ShoppingList, { foreignKey: 'shopping_list_id' });

// User <-> Review (1-N)
// Need to alter Review slightly if it only had Author string before, but we'll map user_id
User.hasMany(Review, { foreignKey: 'user_id' });
Review.belongsTo(User, { foreignKey: 'user_id' });

// User <-> Post (1-N)
User.hasMany(Post, { foreignKey: 'user_id' });
Post.belongsTo(User, { foreignKey: 'user_id' });

// User <-> PostComment (1-N)
User.hasMany(PostComment, { foreignKey: 'user_id' });
PostComment.belongsTo(User, { foreignKey: 'user_id' });

// User <-> PostLike (1-N)
User.hasMany(PostLike, { foreignKey: 'user_id' });
PostLike.belongsTo(User, { foreignKey: 'user_id' });

// Post <-> PostLike (1-N)
Post.hasMany(PostLike, { foreignKey: 'post_id' });
PostLike.belongsTo(Post, { foreignKey: 'post_id' });

// User <-> SavedPost (1-N)
User.hasMany(SavedPost, { foreignKey: 'user_id' });
SavedPost.belongsTo(User, { foreignKey: 'user_id' });

// Post <-> SavedPost (1-N)
Post.hasMany(SavedPost, { foreignKey: 'post_id' });
SavedPost.belongsTo(Post, { foreignKey: 'post_id' });

// User <-> Order (1-N)
User.hasMany(Order, { foreignKey: 'user_id' });
Order.belongsTo(User, { foreignKey: 'user_id' });

module.exports = {
    sequelize,
    Category, DietType, Ingredient, Recipe, RecipeStep, RecipeIngredient,
    RecipeCategory, RecipeDietType, NutritionFact, Review, Post, PostComment, User,
    Favorite, ShoppingList, ShoppingListItem, Notification, PostLike, StoreProduct,
    SavedPost, Order
};

// Cập nhật