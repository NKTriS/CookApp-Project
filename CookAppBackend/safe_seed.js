/**
 * safe_seed.js — Seeds only the missing recipe data without wiping or dropping database tables.
 * Run: node safe_seed.js
 */
const {
    sequelize, Category, DietType, Ingredient, Recipe,
    RecipeStep, RecipeIngredient, NutritionFact, Review, Post, PostComment, User, StoreProduct,
    RecipeCategory, RecipeDietType
} = require('./models');

async function runSafeSeed() {
    try {
        await sequelize.authenticate();
        console.log('✅ Connection to database established.');

        // 1. Fetch existing lookup data
        const categories = await Category.findAll();
        const dietTypes = await DietType.findAll();
        const ingredients = await Ingredient.findAll();
        const posts = await Post.findAll();
        const users = await User.findAll();

        console.log(`Fetched lookup data: ${categories.length} categories, ${dietTypes.length} dietTypes, ${ingredients.length} ingredients, ${posts.length} posts, ${users.length} users.`);

        // 2. Map Categories
        const catNames = ['Gia đình', 'Lành mạnh', 'Tráng miệng', 'Bánh', 'Nhanh & dễ', 'Đặc sản vùng miền', 'Chay', 'Món nướng'];
        const catMap = {};
        catNames.forEach(name => {
            const found = categories.find(c => c.name === name);
            if (!found) throw new Error(`Category not found in DB: ${name}`);
            catMap[name] = found;
        });

        // 3. Map DietTypes
        const dietNames = ['Ăn chay', 'Keto', 'Low-carb', 'Eat-clean', 'Không Gluten', 'Không Bơ Sữa', 'Không Hải Sản', 'Không Đậu Phộng'];
        const dietMap = {};
        dietNames.forEach(name => {
            const found = dietTypes.find(d => d.name === name);
            if (!found) throw new Error(`DietType not found in DB: ${name}`);
            dietMap[name] = found;
        });

        // 4. Map Ingredients
        const ingNames = [
            'Thịt gà', 'Sả', 'Ớt', 'Đậu phụ', 'Cà chua', 'Hành tây', 'Trứng gà', 'Thịt bò', 'Bánh phở',
            'Nước mắm', 'Đường', 'Tiêu', 'Tỏi', 'Gừng', 'Bột mì', 'Sữa tươi', 'Thịt heo', 'Rau cải',
            'Nước dừa', 'Bánh mì', 'Khoai tây', 'Cà rốt', 'Nấm', 'Tôm', 'Cua', 'Bún', 'Cơm',
            'Dầu hào', 'Hành lá', 'Rau muống'
        ];
        const ingMap = {};
        ingNames.forEach(name => {
            const found = ingredients.find(i => i.name === name);
            if (!found) throw new Error(`Ingredient not found in DB: ${name}`);
            ingMap[name] = found;
        });

        // 5. Map Posts
        const postTitles = [
            'Chia sẻ mẹo xào rau không bị thâm',
            'Review nồi chiên không dầu Philips',
            'Hỏi: Sốt xào thịt bò cần loại nào?',
            'Công thức bánh cupcake vị matcha',
            'Mẹo luộc rau muống xanh mướt',
            'Nên mua dao Thái hay dao Nhật?',
            'Thử thách 30 ngày nấu ăn tại nhà'
        ];
        const postMap = {};
        postTitles.forEach(title => {
            const found = posts.find(p => p.title === title);
            if (!found) throw new Error(`Post not found in DB: ${title}`);
            postMap[title] = found;
        });

        // 6. Map Users
        let demoUser1 = users.find(u => u.email === 'demo@cookapp.vn');
        let demoUser2 = users.find(u => u.email === 'user2@cookapp.vn');
        
        if (!demoUser1 || !demoUser2) {
            console.log('⚠️ Demo users not found in DB. Creating them safely...');
            const bcrypt = require('bcryptjs');
            const hashedPw = await bcrypt.hash('123456', 10);
            if (!demoUser1) {
                const [u1] = await User.findOrCreate({ where: { email: 'demo@cookapp.vn' }, defaults: { password: hashedPw, fullName: 'Nguyễn Văn Demo', role: 'user' } });
                demoUser1 = u1;
            }
            if (!demoUser2) {
                const [u2] = await User.findOrCreate({ where: { email: 'user2@cookapp.vn' }, defaults: { password: hashedPw, fullName: 'Trần Thị User', role: 'user' } });
                demoUser2 = u2;
            }
        }

        // 7. Verify Recipes Count
        const recipeCount = await Recipe.count();
        if (recipeCount > 0) {
            console.log(`⚠️ Recipes already exist in the database (${recipeCount}). Skipping recipe seeding to preserve existing data.`);
            return;
        }

        console.log('🌱 Seeding Recipes and related records...');

        // Insert Recipes
        const recipes = [
            { id: 1, title:'Gà xào sả ớt', description:'Món ăn đưa cơm thơm ngon đậm vị gia đình.',
              image_url:'/videos/thumbnails/ga_xao_xa_ot.jpg',
              video_url:'/videos/ga_xao_xa_ot.mp4', video_thumbnail_url:'/videos/thumbnails/ga_xao_xa_ot.jpg',
              cook_time:30, difficulty:'Dễ', servings:4, calories:350, category_id:catMap['Gia đình'].id, diet_type_id:1 },

            { id: 2, title:'Đậu phụ sốt cà chua', description:'Món chay thanh đạm, dễ nấu, phù hợp ăn kiêng nhẹ.',
              image_url:'/videos/thumbnails/dau_phu_sot_ca_chua.jpg',
              video_url:'/videos/dau_phu_sot_ca_chua.mp4', video_thumbnail_url:'/videos/thumbnails/dau_phu_sot_ca_chua.jpg',
              cook_time:20, difficulty:'Rất dễ', servings:2, calories:180, category_id:catMap['Chay'].id, diet_type_id:1 },

            { id: 3, title:'Bún bò Huế', description:'Đặc sản miền Trung với nước dùng cay nồng thơm mùi ruốc sả.',
              image_url:'/videos/thumbnails/bun_bo_hue.jpg',
              video_url:'/videos/bun_bo_hue.mp4', video_thumbnail_url:'/videos/thumbnails/bun_bo_hue.jpg',
              cook_time:90, difficulty:'Trung bình', servings:4, calories:480, category_id:catMap['Đặc sản vùng miền'].id, diet_type_id:1 },

            { id: 4, title:'Phở Bò truyền thống', description:'Phở bò Hà Nội với nước dùng trong, ngọt tự nhiên từ xương bò hầm kỹ.',
              image_url:'/videos/thumbnails/pho_bo_truyen_thong.jpg',
              video_url:'/videos/pho_bo_truyen_thong.mp4', video_thumbnail_url:'/videos/thumbnails/pho_bo_truyen_thong.jpg',
              cook_time:120, difficulty:'Khó', servings:4, calories:420, category_id:catMap['Đặc sản vùng miền'].id, diet_type_id:1 },

            { id: 5, title:'Bánh mì trứng ốp la', description:'Bữa sáng nhanh gọn 10 phút, đủ năng lượng cho cả ngày làm việc.',
              image_url:'/videos/thumbnails/banh_mi_trung_op_la.jpg',
              video_url:'/videos/banh_mi_trung_op_la.mp4', video_thumbnail_url:'/videos/thumbnails/banh_mi_trung_op_la.jpg',
              cook_time:10, difficulty:'Rất dễ', servings:1, calories:320, category_id:catMap['Nhanh & dễ'].id, diet_type_id:1 },

            { id: 6, title:'Canh rau cải nấu thịt heo', description:'Canh thanh mát giải nhiệt, bổ dưỡng và dễ nấu cho bữa cơm gia đình.',
              image_url:'/videos/thumbnails/canh_rau_nau_thit_heo.jpg',
              video_url:'/videos/canh_rau_nau_thit_heo.mp4', video_thumbnail_url:'/videos/thumbnails/canh_rau_nau_thit_heo.jpg',
              cook_time:20, difficulty:'Dễ', servings:3, calories:150, category_id:catMap['Gia đình'].id, diet_type_id:1 },

            { id: 7, title:'Cơm rang dưa bò', description:'Cơm rang thơm ngon từ thịt bò và dưa cải muối, nhanh gọn cho bữa trưa.',
              image_url:'/videos/thumbnails/com_rang_dua_bo.jpg',
              video_url:'/videos/com_rang_dua_bo.mp4', video_thumbnail_url:'/videos/thumbnails/com_rang_dua_bo.jpg',
              cook_time:15, difficulty:'Dễ', servings:2, calories:400, category_id:catMap['Nhanh & dễ'].id, diet_type_id:1 },

            { id: 8, title:'Tôm xào bông cải', description:'Tôm tươi xào với bông cải xanh giòn ngọt, ít dầu mỡ và giàu dinh dưỡng.',
              image_url:'/videos/thumbnails/tom_xao_bong_cai.jpg',
              video_url:'/videos/tom_xao_bong_cai.mp4', video_thumbnail_url:'/videos/thumbnails/tom_xao_bong_cai.jpg',
              cook_time:20, difficulty:'Dễ', servings:3, calories:220, category_id:catMap['Lành mạnh'].id, diet_type_id:1 },

            { id: 9, title:'Súp bí đỏ kem', description:'Súp bí đỏ mịn mượt, béo nhẹ từ kem tươi, thích hợp mùa đông.',
              image_url:'/videos/thumbnails/sup_bi_do_kem.jpg',
              video_url:'/videos/sup_bi_do_kem.mp4', video_thumbnail_url:'/videos/thumbnails/sup_bi_do_kem.jpg',
              cook_time:30, difficulty:'Dễ', servings:4, calories:160, category_id:catMap['Lành mạnh'].id, diet_type_id:1 },

            { id: 10, title:'Gỏi cuốn tôm thịt', description:'Gỏi cuốn Việt Nam truyền thống với tôm, thịt, rau sống và tương hoisin.',
              image_url:'/videos/thumbnails/goi_cuon_tom_thit.jpg',
              video_url:'/videos/goi_cuon_tom_thit.mp4', video_thumbnail_url:'/videos/thumbnails/goi_cuon_tom_thit.jpg',
              cook_time:40, difficulty:'Trung bình', servings:4, calories:180, category_id:catMap['Đặc sản vùng miền'].id, diet_type_id:1 },

            { id: 11, title:'Chả giò (Nem rán)', description:'Chả giò giòn rụm với nhân thịt và rau củ đậm đà, kèm nước chấm chua ngọt.',
              image_url:'/videos/thumbnails/cha_gio.jpg',
              video_url:'/videos/cha_gio.mp4', video_thumbnail_url:'/videos/thumbnails/cha_gio.jpg',
              cook_time:60, difficulty:'Trung bình', servings:6, calories:280, category_id:catMap['Đặc sản vùng miền'].id, diet_type_id:1 },

            { id: 12, title:'Bánh flan caramel', description:'Bánh flan mềm mịn như lụa, thơm béo với lớp caramel đắng nhẹ đẹp mắt.',
              image_url:'/videos/thumbnails/banh_flan.jpg',
              video_url:'/videos/banh_flan.mp4', video_thumbnail_url:'/videos/thumbnails/banh_flan.jpg',
              cook_time:50, difficulty:'Trung bình', servings:6, calories:190, category_id:catMap['Tráng miệng'].id, diet_type_id:1 },
        ];

        await Recipe.bulkCreate(recipes);
        console.log('✅ Recipes table seeded.');

        // Insert RecipeCategory Mappings
        const recipeCategories = [
            { recipe_id: 1, category_id: catMap['Gia đình'].id },
            { recipe_id: 1, category_id: catMap['Nhanh & dễ'].id },
            { recipe_id: 2, category_id: catMap['Chay'].id },
            { recipe_id: 2, category_id: catMap['Lành mạnh'].id },
            { recipe_id: 2, category_id: catMap['Gia đình'].id },
            { recipe_id: 3, category_id: catMap['Đặc sản vùng miền'].id },
            { recipe_id: 3, category_id: catMap['Gia đình'].id },
            { recipe_id: 4, category_id: catMap['Đặc sản vùng miền'].id },
            { recipe_id: 4, category_id: catMap['Gia đình'].id },
            { recipe_id: 5, category_id: catMap['Nhanh & dễ'].id },
            { recipe_id: 5, category_id: catMap['Gia đình'].id },
            { recipe_id: 6, category_id: catMap['Gia đình'].id },
            { recipe_id: 6, category_id: catMap['Lành mạnh'].id },
            { recipe_id: 7, category_id: catMap['Nhanh & dễ'].id },
            { recipe_id: 7, category_id: catMap['Gia đình'].id },
            { recipe_id: 8, category_id: catMap['Lành mạnh'].id },
            { recipe_id: 8, category_id: catMap['Nhanh & dễ'].id },
            { recipe_id: 9, category_id: catMap['Lành mạnh'].id },
            { recipe_id: 9, category_id: catMap['Chay'].id },
            { recipe_id: 10, category_id: catMap['Đặc sản vùng miền'].id },
            { recipe_id: 10, category_id: catMap['Lành mạnh'].id },
            { recipe_id: 11, category_id: catMap['Đặc sản vùng miền'].id },
            { recipe_id: 11, category_id: catMap['Món nướng'].id },
            { recipe_id: 12, category_id: catMap['Tráng miệng'].id },
            { recipe_id: 12, category_id: catMap['Bánh'].id },
        ];
        await RecipeCategory.bulkCreate(recipeCategories);
        console.log('✅ RecipeCategory mapping seeded.');

        // Insert RecipeDietType Mappings
        const recipeDietTypes = [
            // R1
            { recipe_id: 1, diet_type_id: dietMap['Keto'].id },
            { recipe_id: 1, diet_type_id: dietMap['Low-carb'].id },
            { recipe_id: 1, diet_type_id: dietMap['Không Gluten'].id },
            { recipe_id: 1, diet_type_id: dietMap['Không Bơ Sữa'].id },
            { recipe_id: 1, diet_type_id: dietMap['Không Hải Sản'].id },
            { recipe_id: 1, diet_type_id: dietMap['Không Đậu Phộng'].id },
            // R2
            { recipe_id: 2, diet_type_id: dietMap['Ăn chay'].id },
            { recipe_id: 2, diet_type_id: dietMap['Eat-clean'].id },
            { recipe_id: 2, diet_type_id: dietMap['Không Gluten'].id },
            { recipe_id: 2, diet_type_id: dietMap['Không Bơ Sữa'].id },
            { recipe_id: 2, diet_type_id: dietMap['Không Hải Sản'].id },
            { recipe_id: 2, diet_type_id: dietMap['Không Đậu Phộng'].id },
            // R3
            { recipe_id: 3, diet_type_id: dietMap['Không Gluten'].id },
            { recipe_id: 3, diet_type_id: dietMap['Không Bơ Sữa'].id },
            { recipe_id: 3, diet_type_id: dietMap['Không Đậu Phộng'].id },
            // R4
            { recipe_id: 4, diet_type_id: dietMap['Không Gluten'].id },
            { recipe_id: 4, diet_type_id: dietMap['Không Bơ Sữa'].id },
            { recipe_id: 4, diet_type_id: dietMap['Không Hải Sản'].id },
            { recipe_id: 4, diet_type_id: dietMap['Không Đậu Phộng'].id },
            // R5
            { recipe_id: 5, diet_type_id: dietMap['Không Hải Sản'].id },
            { recipe_id: 5, diet_type_id: dietMap['Không Đậu Phộng'].id },
            // R6
            { recipe_id: 6, diet_type_id: dietMap['Eat-clean'].id },
            { recipe_id: 6, diet_type_id: dietMap['Keto'].id },
            { recipe_id: 6, diet_type_id: dietMap['Low-carb'].id },
            { recipe_id: 6, diet_type_id: dietMap['Không Gluten'].id },
            { recipe_id: 6, diet_type_id: dietMap['Không Bơ Sữa'].id },
            { recipe_id: 6, diet_type_id: dietMap['Không Hải Sản'].id },
            { recipe_id: 6, diet_type_id: dietMap['Không Đậu Phộng'].id },
            // R7
            { recipe_id: 7, diet_type_id: dietMap['Không Gluten'].id },
            { recipe_id: 7, diet_type_id: dietMap['Không Bơ Sữa'].id },
            { recipe_id: 7, diet_type_id: dietMap['Không Hải Sản'].id },
            { recipe_id: 7, diet_type_id: dietMap['Không Đậu Phộng'].id },
            // R8
            { recipe_id: 8, diet_type_id: dietMap['Eat-clean'].id },
            { recipe_id: 8, diet_type_id: dietMap['Keto'].id },
            { recipe_id: 8, diet_type_id: dietMap['Low-carb'].id },
            { recipe_id: 8, diet_type_id: dietMap['Không Gluten'].id },
            { recipe_id: 8, diet_type_id: dietMap['Không Bơ Sữa'].id },
            { recipe_id: 8, diet_type_id: dietMap['Không Đậu Phộng'].id },
            // R9
            { recipe_id: 9, diet_type_id: dietMap['Ăn chay'].id },
            { recipe_id: 9, diet_type_id: dietMap['Không Gluten'].id },
            { recipe_id: 9, diet_type_id: dietMap['Không Hải Sản'].id },
            { recipe_id: 9, diet_type_id: dietMap['Không Đậu Phộng'].id },
            // R10
            { recipe_id: 10, diet_type_id: dietMap['Eat-clean'].id },
            { recipe_id: 10, diet_type_id: dietMap['Không Gluten'].id },
            { recipe_id: 10, diet_type_id: dietMap['Không Bơ Sữa'].id },
            // R11
            { recipe_id: 11, diet_type_id: dietMap['Không Bơ Sữa'].id },
            { recipe_id: 11, diet_type_id: dietMap['Không Đậu Phộng'].id },
            // R12
            { recipe_id: 12, diet_type_id: dietMap['Ăn chay'].id },
            { recipe_id: 12, diet_type_id: dietMap['Không Gluten'].id },
            { recipe_id: 12, diet_type_id: dietMap['Không Hải Sản'].id },
            { recipe_id: 12, diet_type_id: dietMap['Không Đậu Phộng'].id },
        ];
        await RecipeDietType.bulkCreate(recipeDietTypes);
        console.log('✅ RecipeDietType mapping seeded.');

        // Insert RecipeSteps
        const steps = [
            // R1 – Gà xào sả ớt
            { recipe_id:1, step_number:1, instruction:'Rửa sạch thịt gà, chặt miếng vừa ăn và để ráo nước.', timer_seconds:0, video_start_time: 0 },
            { recipe_id:1, step_number:2, instruction:'Ướp gà cùng sả băm, ớt, muối, nước mắm và 1 muỗng đường. Để ngấm 15 phút.', timer_seconds:900, video_start_time: 5 },
            { recipe_id:1, step_number:3, instruction:'Bắc chảo lên bếp, đổ dầu ăn, phi tỏi vàng thơm.', timer_seconds:60, video_start_time: 12 },
            { recipe_id:1, step_number:4, instruction:'Xào gà trên lửa lớn khoảng 5 phút đến khi chín vàng đều.', timer_seconds:300, video_start_time: 18 },
            { recipe_id:1, step_number:5, instruction:'Thêm sả cây đập dập vào xào cùng.', timer_seconds:60, video_start_time: 25 },
            { recipe_id:1, step_number:6, instruction:'Nêm gia vị vừa miệng, tắt bếp, trang trí lá chanh và ớt.', timer_seconds:0, video_start_time: 32 },

            // R2 – Đậu phụ sốt cà chua
            { recipe_id:2, step_number:1, instruction:'Cắt đậu phụ thành miếng vuông khoảng 2cm.', timer_seconds:0 },
            { recipe_id:2, step_number:2, instruction:'Chiên đậu phụ trong dầu nóng đến vàng đều hai mặt.', timer_seconds:240 },
            { recipe_id:2, step_number:3, instruction:'Xào cà chua thái múi cua với hành tím đến khi nhuyễn, ra màu đỏ.', timer_seconds:120 },
            { recipe_id:2, step_number:4, instruction:'Cho đậu phụ chiên vào sốt, đảo nhẹ tay.', timer_seconds:60 },
            { recipe_id:2, step_number:5, instruction:'Nêm nước mắm, đường vừa miệng và rắc hành lá.', timer_seconds:30 },

            // R3 – Bún bò Huế
            { recipe_id:3, step_number:1, instruction:'Hầm xương heo và xương bò với gừng, hành khô trong 2 tiếng để lấy nước trong.', timer_seconds:7200 },
            { recipe_id:3, step_number:2, instruction:'Phi sả băm cùng dầu màu điều cho thơm, ra màu đỏ đẹp.', timer_seconds:300 },
            { recipe_id:3, step_number:3, instruction:'Cho hỗn hợp sả vào nồi nước dùng, thêm mắm ruốc, nêm nước mắm và muối.', timer_seconds:0 },
            { recipe_id:3, step_number:4, instruction:'Luộc thịt bò, chả Huế vừa chín tới rồi thái lát.', timer_seconds:600 },
            { recipe_id:3, step_number:5, instruction:'Trần bún qua nước sôi, xếp vào tô, để thịt lên trên.', timer_seconds:0 },
            { recipe_id:3, step_number:6, instruction:'Chan nước dùng đang sôi vào tô, thêm hành lá, ớt và chanh.', timer_seconds:0 },

            // R4 – Phở Bò
            { recipe_id:4, step_number:1, instruction:'Nướng gừng và hành tím cho đến khi thơm và có màu.', timer_seconds:0 },
            { recipe_id:4, step_number:2, instruction:'Chần xương bò qua nước sôi 10 phút, đổ nước đi, rửa sạch xương.', timer_seconds:600 },
            { recipe_id:4, step_number:3, instruction:'Hầm xương bò 3 tiếng với gừng nướng, hành, quế, hoa hồi.', timer_seconds:10800 },
            { recipe_id:4, step_number:4, instruction:'Nêm nước mắm, đường, muối vừa miệng và lọc nước dùng.', timer_seconds:0 },
            { recipe_id:4, step_number:5, instruction:'Thái thịt bò sống thành lát mỏng.', timer_seconds:0 },
            { recipe_id:4, step_number:6, instruction:'Trần bánh phở, xếp thịt bò tái lên, chan nước dùng sôi già và thêm hành lá.', timer_seconds:0 },

            // R5 – Bánh mì trứng ốp la
            { recipe_id:5, step_number:1, instruction:'Đun nóng chảo chống dính với 1 thìa cà phê dầu ăn.', timer_seconds:30 },
            { recipe_id:5, step_number:2, instruction:'Đập 2 quả trứng vào chảo, rán lửa vừa đến khi lòng trắng đông lại.', timer_seconds:120 },
            { recipe_id:5, step_number:3, instruction:'Lật nhẹ và rán thêm 30 giây, hoặc giữ nguyên kiểu lòng đào.', timer_seconds:30 },
            { recipe_id:5, step_number:4, instruction:'Rắc muối tiêu lên trứng.', timer_seconds:0 },
            { recipe_id:5, step_number:5, instruction:'Kẹp trứng vào bánh mì cùng rau sống, dưa leo và tương ớt.', timer_seconds:0 },

            // R6 – Canh rau cải
            { recipe_id:6, step_number:1, instruction:'Thái thịt heo lát mỏng, ướp muối tiêu 5 phút.', timer_seconds:300 },
            { recipe_id:6, step_number:2, instruction:'Rửa và cắt rau cải thành khúc 4–5cm.', timer_seconds:0 },
            { recipe_id:6, step_number:3, instruction:'Phi hành tím cho thơm, cho thịt heo vào xào chín.', timer_seconds:120 },
            { recipe_id:6, step_number:4, instruction:'Đổ 500ml nước vào, đun sôi, nêm nước mắm và đường.', timer_seconds:300 },
            { recipe_id:6, step_number:5, instruction:'Cho rau cải vào nấu thêm 3 phút là chín tới.', timer_seconds:180 },

            // R7 – Cơm rang dưa bò
            { recipe_id:7, step_number:1, instruction:'Thái thịt bò lát mỏng, ướp nước mắm, tiêu, dầu hào.', timer_seconds:300 },
            { recipe_id:7, step_number:2, instruction:'Xào dưa cải muối đã vắt nước với tỏi cho vàng thơm.', timer_seconds:120 },
            { recipe_id:7, step_number:3, instruction:'Thêm thịt bò vào xào đến khi chín, trải đều.', timer_seconds:120 },
            { recipe_id:7, step_number:4, instruction:'Cho cơm nguội vào đảo đều trên lửa lớn.', timer_seconds:180 },
            { recipe_id:7, step_number:5, instruction:'Nêm nước mắm vừa miệng, rắc hành lá và tiêu.', timer_seconds:0 },

            // R8 – Tôm xào bông cải
            { recipe_id:8, step_number:1, instruction:'Bóc vỏ tôm, rút chỉ đen và ướp muối tiêu.', timer_seconds:300 },
            { recipe_id:8, step_number:2, instruction:'Cắt bông cải xanh thành florets, chần qua nước sôi 1 phút.', timer_seconds:60 },
            { recipe_id:8, step_number:3, instruction:'Phi tỏi vàng, cho tôm vào xào chín hồng rồi gắp ra.', timer_seconds:120 },
            { recipe_id:8, step_number:4, instruction:'Xào bông cải với dầu hào và nước mắm đến khi vừa mềm.', timer_seconds:120 },
            { recipe_id:8, step_number:5, instruction:'Trộn tôm vào bông cải, đảo đều, nêm lại vừa miệng và dọn ra đĩa.', timer_seconds:30 },

            // R9 – Súp bí đỏ kem
            { recipe_id:9, step_number:1, instruction:'Gọt vỏ bí đỏ, cắt khối vuông 2cm.', timer_seconds:0 },
            { recipe_id:9, step_number:2, instruction:'Xào hành tây thái hạt lựu với bơ đến khi trong suốt.', timer_seconds:300 },
            { recipe_id:9, step_number:3, instruction:'Cho bí đỏ vào xào cùng, thêm 600ml nước dùng gà hoặc nước lọc.', timer_seconds:0 },
            { recipe_id:9, step_number:4, instruction:'Đun sôi rồi hạ lửa, ninh 20 phút đến khi bí mềm.', timer_seconds:1200 },
            { recipe_id:9, step_number:5, instruction:'Xay nhuyễn bằng máy xay cắm tay, thêm kem tươi, nêm muối tiêu.', timer_seconds:0 },
            { recipe_id:9, step_number:6, instruction:'Trang trí với kem tươi, hạt bí rang và lá húng quế.', timer_seconds:0 },

            // R10 – Gỏi cuốn tôm thịt
            { recipe_id:10, step_number:1, instruction:'Luộc tôm chín, bóc vỏ và chẻ đôi dọc lưng.', timer_seconds:300 },
            { recipe_id:10, step_number:2, instruction:'Luộc thịt heo, thái lát mỏng.', timer_seconds:600 },
            { recipe_id:10, step_number:3, instruction:'Chuẩn bị bún, rau sống, húng quế, cà rốt và dưa leo bào sợi.', timer_seconds:0 },
            { recipe_id:10, step_number:4, instruction:'Nhúng bánh tráng qua nước ấm vài giây đến khi mềm.', timer_seconds:0 },
            { recipe_id:10, step_number:5, instruction:'Xếp tôm, thịt, bún và rau vào giữa bánh tráng, cuốn chặt tay.', timer_seconds:0 },
            { recipe_id:10, step_number:6, instruction:'Pha nước chấm hoisin với tương đen, lạc rang và tương ớt.', timer_seconds:0 },

            // R11 – Chả giò
            { recipe_id:11, step_number:1, instruction:'Trộn nhân chả giò: thịt heo băm, miến, cà rốt bào, nấm mèo, trứng, gia vị.', timer_seconds:0 },
            { recipe_id:11, step_number:2, instruction:'Nhúng bánh tráng chả giò qua nước, xếp nhân vào và cuốn chặt.', timer_seconds:0 },
            { recipe_id:11, step_number:3, instruction:'Chiên trong dầu sâu ở 160°C khoảng 8 phút đến vàng nhạt.', timer_seconds:480 },
            { recipe_id:11, step_number:4, instruction:'Vớt ra để ráo, rồi chiên lần 2 ở 180°C khoảng 3 phút để giòn.', timer_seconds:180 },
            { recipe_id:11, step_number:5, instruction:'Pha nước chấm chua ngọt và dọn ra cùng rau sống.', timer_seconds:0 },

            // R12 – Bánh flan
            { recipe_id:12, step_number:1, instruction:'Đun đường với 3 muỗng nước đến khi thành caramel vàng nâu. Đổ vào khuôn.', timer_seconds:300 },
            { recipe_id:12, step_number:2, instruction:'Đun sữa tươi và kem tươi đến gần sôi, để nguội bớt.', timer_seconds:0 },
            { recipe_id:12, step_number:3, instruction:'Đánh trứng gà với đường vani đến khi hòa tan.', timer_seconds:0 },
            { recipe_id:12, step_number:4, instruction:'Đổ sữa ấm vào hỗn hợp trứng, khuấy đều và lọc qua rây.', timer_seconds:0 },
            { recipe_id:12, step_number:5, instruction:'Đổ hỗn hợp vào khuôn caramel, hấp cách thủy 25 phút ở lửa nhỏ.', timer_seconds:1500 },
            { recipe_id:12, step_number:6, instruction:'Để nguội rồi cho vào tủ lạnh tối thiểu 2 tiếng trước khi lật ra đĩa.', timer_seconds:7200 },
        ];
        await RecipeStep.bulkCreate(steps);
        console.log('✅ Recipe steps seeded.');

        // Insert RecipeIngredients
        const recipeIngredients = [
            // R1 – Gà xào sả ớt
            { recipe_id:1, ingredient_id:ingMap['Thịt gà'].id, quantity:500, unit:'gram' },
            { recipe_id:1, ingredient_id:ingMap['Sả'].id, quantity:3, unit:'cây' },
            { recipe_id:1, ingredient_id:ingMap['Ớt'].id, quantity:2, unit:'quả' },
            { recipe_id:1, ingredient_id:ingMap['Tỏi'].id, quantity:4, unit:'tép' },
            { recipe_id:1, ingredient_id:ingMap['Nước mắm'].id, quantity:2, unit:'muỗng' },
            { recipe_id:1, ingredient_id:ingMap['Đường'].id, quantity:1, unit:'muỗng' },
            // R2 – Đậu phụ sốt cà chua
            { recipe_id:2, ingredient_id:ingMap['Đậu phụ'].id, quantity:300, unit:'gram' },
            { recipe_id:2, ingredient_id:ingMap['Cà chua'].id, quantity:3, unit:'quả' },
            { recipe_id:2, ingredient_id:ingMap['Hành tây'].id, quantity:1, unit:'củ' },
            { recipe_id:2, ingredient_id:ingMap['Nước mắm'].id, quantity:1, unit:'muỗng' },
            { recipe_id:2, ingredient_id:ingMap['Hành lá'].id, quantity:2, unit:'cây' },
            // R3 – Bún bò Huế
            { recipe_id:3, ingredient_id:ingMap['Thịt bò'].id, quantity:300, unit:'gram' },
            { recipe_id:3, ingredient_id:ingMap['Bún'].id, quantity:400, unit:'gram' },
            { recipe_id:3, ingredient_id:ingMap['Sả'].id, quantity:5, unit:'cây' },
            { recipe_id:3, ingredient_id:ingMap['Gừng'].id, quantity:1, unit:'củ nhỏ' },
            { recipe_id:3, ingredient_id:ingMap['Ớt'].id, quantity:3, unit:'quả' },
            // R4 – Phở Bò
            { recipe_id:4, ingredient_id:ingMap['Thịt bò'].id, quantity:400, unit:'gram' },
            { recipe_id:4, ingredient_id:ingMap['Bánh phở'].id, quantity:400, unit:'gram' },
            { recipe_id:4, ingredient_id:ingMap['Gừng'].id, quantity:1, unit:'củ' },
            { recipe_id:4, ingredient_id:ingMap['Nước mắm'].id, quantity:3, unit:'muỗng' },
            // R5 – Bánh mì trứng ốp la
            { recipe_id:5, ingredient_id:ingMap['Bánh mì'].id, quantity:1, unit:'ổ' },
            { recipe_id:5, ingredient_id:ingMap['Trứng gà'].id, quantity:2, unit:'quả' },
            { recipe_id:5, ingredient_id:ingMap['Tiêu'].id, quantity:0.5, unit:'muỗng cà phê' },
            // R6 – Canh rau cải
            { recipe_id:6, ingredient_id:ingMap['Thịt heo'].id, quantity:200, unit:'gram' },
            { recipe_id:6, ingredient_id:ingMap['Rau cải'].id, quantity:300, unit:'gram' },
            { recipe_id:6, ingredient_id:ingMap['Nước mắm'].id, quantity:2, unit:'muỗng' },
            // R7 – Cơm rang dưa bò
            { recipe_id:7, ingredient_id:ingMap['Thịt bò'].id, quantity:200, unit:'gram' },
            { recipe_id:7, ingredient_id:ingMap['Cơm'].id, quantity:400, unit:'gram' },
            { recipe_id:7, ingredient_id:ingMap['Dầu hào'].id, quantity:1, unit:'muỗng' },
            { recipe_id:7, ingredient_id:ingMap['Hành lá'].id, quantity:3, unit:'cây' },
            // R8 – Tôm xào bông cải
            { recipe_id:8, ingredient_id:ingMap['Tôm'].id, quantity:300, unit:'gram' },
            { recipe_id:8, ingredient_id:ingMap['Nước mắm'].id, quantity:1, unit:'muỗng' },
            { recipe_id:8, ingredient_id:ingMap['Tỏi'].id, quantity:3, unit:'tép' },
            { recipe_id:8, ingredient_id:ingMap['Dầu hào'].id, quantity:1, unit:'muỗng' },
            // R9 – Súp bí đỏ
            { recipe_id:9, ingredient_id:ingMap['Khoai tây'].id, quantity:400, unit:'gram' },
            { recipe_id:9, ingredient_id:ingMap['Hành tây'].id, quantity:1, unit:'củ' },
            { recipe_id:9, ingredient_id:ingMap['Sữa tươi'].id, quantity:200, unit:'ml' },
            // R10 – Gỏi cuốn
            { recipe_id:10, ingredient_id:ingMap['Tôm'].id, quantity:200, unit:'gram' },
            { recipe_id:10, ingredient_id:ingMap['Thịt heo'].id, quantity:150, unit:'gram' },
            { recipe_id:10, ingredient_id:ingMap['Bún'].id, quantity:200, unit:'gram' },
            { recipe_id:10, ingredient_id:ingMap['Cà rốt'].id, quantity:1, unit:'củ' },
            // R11 – Chả giò
            { recipe_id:11, ingredient_id:ingMap['Thịt heo'].id, quantity:300, unit:'gram' },
            { recipe_id:11, ingredient_id:ingMap['Nấm'].id, quantity:100, unit:'gram' },
            { recipe_id:11, ingredient_id:ingMap['Cà rốt'].id, quantity:1, unit:'củ' },
            { recipe_id:11, ingredient_id:ingMap['Trứng gà'].id, quantity:1, unit:'quả' },
            // R12 – Bánh flan
            { recipe_id:12, ingredient_id:ingMap['Trứng gà'].id, quantity:3, unit:'quả' },
            { recipe_id:12, ingredient_id:ingMap['Sữa tươi'].id, quantity:300, unit:'ml' },
            { recipe_id:12, ingredient_id:ingMap['Đường'].id, quantity:80, unit:'gram' },
        ];
        await RecipeIngredient.bulkCreate(recipeIngredients);
        console.log('✅ RecipeIngredient mapping seeded.');

        // Insert NutritionFacts
        const nutritionFacts = [
            { recipe_id:1,  calories:350, protein:28, fat:14, carbs:6,  fiber:1.2, sugar:2.5, sodium:820 },
            { recipe_id:2,  calories:180, protein:12, fat:8,  carbs:14, fiber:2.0, sugar:4.0, sodium:560 },
            { recipe_id:3,  calories:480, protein:35, fat:18, carbs:42, fiber:2.5, sugar:3.0, sodium:1100 },
            { recipe_id:4,  calories:420, protein:30, fat:10, carbs:52, fiber:1.0, sugar:2.0, sodium:950 },
            { recipe_id:5,  calories:320, protein:14, fat:12, carbs:38, fiber:2.0, sugar:3.0, sodium:620 },
            { recipe_id:6,  calories:150, protein:15, fat:6,  carbs:8,  fiber:2.5, sugar:1.5, sodium:480 },
            { recipe_id:7,  calories:400, protein:22, fat:12, carbs:52, fiber:1.5, sugar:2.0, sodium:780 },
            { recipe_id:8,  calories:220, protein:24, fat:8,  carbs:12, fiber:3.0, sugar:2.0, sodium:680 },
            { recipe_id:9,  calories:160, protein:4,  fat:8,  carbs:20, fiber:3.5, sugar:6.0, sodium:350 },
            { recipe_id:10, calories:180, protein:16, fat:4,  carbs:22, fiber:2.0, sugar:2.5, sodium:520 },
            { recipe_id:11, calories:280, protein:18, fat:14, carbs:24, fiber:1.5, sugar:2.0, sodium:750 },
            { recipe_id:12, calories:190, protein:6,  fat:8,  carbs:26, fiber:0.2, sugar:22,  sodium:120 },
        ];
        await NutritionFact.bulkCreate(nutritionFacts);
        console.log('✅ NutritionFact records seeded.');

        // Insert Reviews
        const reviews = [
            { recipe_id:1,  user_id:demoUser1 ? demoUser1.id : null, rating:5, comment:'Ngon tuyệt! Cả nhà mình đều thích.', author:'Nguyễn Văn Demo' },
            { recipe_id:1,  user_id:demoUser2 ? demoUser2.id : null, rating:4, comment:'Thơm ngon nhưng tôi giảm ớt cho trẻ con.', author:'Trần Thị User' },
            { recipe_id:1,  user_id:null,         rating:5, comment:'Đã thử nhiều công thức, cái này ngon nhất!', author:'Mai Linh' },
            { recipe_id:2,  user_id:null,         rating:5, comment:'Đơn giản mà ngon, ăn với cơm rất hợp.', author:'Hoa Trang' },
            { recipe_id:2,  user_id:null,         rating:4, comment:'Món chay ngon, con mình cũng thích.', author:'Nam Phong' },
            { recipe_id:3,  user_id:null,         rating:5, comment:'Chuẩn vị Huế, nước dùng rất đậm đà.', author:'Thanh Hương' },
            { recipe_id:3,  user_id:null,         rating:5, comment:'Tuyệt vời! Từng học ở Huế và đây chính xác là vị đó.', author:'Minh Khoa' },
            { recipe_id:4,  user_id:demoUser1 ? demoUser1.id : null, rating:4, comment:'Phở ngon, nhưng hầm xương mất nhiều thời gian.', author:'Nguyễn Văn Demo' },
            { recipe_id:4,  user_id:null,         rating:5, comment:'Nước dùng trong vắt và ngọt thanh, rất chuẩn!', author:'Thu Hà' },
            { recipe_id:5,  user_id:null,         rating:5, comment:'10 phút có ngay bữa sáng ngon!', author:'Bình Minh' },
            { recipe_id:6,  user_id:null,         rating:4, comment:'Canh thanh mát, giải nhiệt mùa hè rất tốt.', author:'Diễm Hương' },
            { recipe_id:7,  user_id:null,         rating:4, comment:'Cơm rang đậm đà, thơm mùi dưa.', author:'Quang Minh' },
            { recipe_id:8,  user_id:null,         rating:5, comment:'Tôm tươi xào bông cải giòn, bổ dưỡng!', author:'Thanh Thảo' },
            { recipe_id:9,  user_id:null,         rating:5, comment:'Súp mịn như nhung, con bé nhà mình mê lắm.', author:'Vũ Hoa' },
            { recipe_id:10, user_id:demoUser2 ? demoUser2.id : null, rating:5, comment:'Gỏi cuốn nhà làm ngon hơn ngoài hàng.', author:'Trần Thị User' },
            { recipe_id:10, user_id:null,         rating:4, comment:'Cuốn đẹp và ngon, nước chấm chuẩn vị.', author:'Trung Hiếu' },
            { recipe_id:11, user_id:null,         rating:5, comment:'Chả giò giòn rụm, nhân đầy đặn.', author:'Phương Linh' },
            { recipe_id:11, user_id:null,         rating:5, comment:'Hai lần chiên là bí quyết để giòn lâu!', author:'Hoàng Nam' },
            { recipe_id:12, user_id:null,         rating:5, comment:'Flan mịn mượt, caramel đắng vừa phải.', author:'Ngọc Ánh' },
            { recipe_id:12, user_id:null,         rating:4, comment:'Cần hấp đúng lửa nhỏ để bánh không bị rỗ.', author:'Thành Long' },
        ];
        await Review.bulkCreate(reviews);
        console.log('✅ Review records seeded.');

        // Insert PostComments
        const comments = [
            { post_id:postMap['Chia sẻ mẹo xào rau không bị thâm'].id, author:'Lan Hương', content:'Mình làm thử rồi, rau xanh hơn hẳn luôn!', created_at:new Date('2025-03-11') },
            { post_id:postMap['Chia sẻ mẹo xào rau không bị thâm'].id, author:'Bếp Trưởng', content:'Lửa lớn là chìa khóa quan trọng nhất!', created_at:new Date('2025-03-11') },
            { post_id:postMap['Review nồi chiên không dầu Philips'].id, author:'Thanh Hà', content:'Mình cũng dùng Philips, đồng ý 100%!', created_at:new Date('2025-03-13') },
            { post_id:postMap['Hỏi: Sốt xào thịt bò cần loại nào?'].id, author:'Chef A', content:'Sốt teriyaki ngon nhưng hơi ngọt, thử pha loãng nhé.', created_at:new Date('2025-03-15') },
            { post_id:postMap['Công thức bánh cupcake vị matcha'].id, author:'Cupcake Queen', content:'Cảm ơn công thức nha! Mình sẽ thử cuối tuần này.', created_at:new Date('2025-03-17') },
            { post_id:postMap['Công thức bánh cupcake vị matcha'].id, author:'Matcha Lover', content:'Cho thêm kem cheese frosting matcha vào là hoàn hảo!', created_at:new Date('2025-03-17') },
            { post_id:postMap['Mẹo luộc rau muống xanh mướt'].id, author:'Rau Sạch', content:'Mẹo hay! Sẽ áp dụng ngay tối nay.', created_at:new Date('2025-03-19') },
            { post_id:postMap['Nên mua dao Thái hay dao Nhật?'].id, author:'Knife Collector', content:'Nên thêm dao Đức vào danh sách, cân bằng tốt lắm.', created_at:new Date('2025-03-21') },
            { post_id:postMap['Thử thách 30 ngày nấu ăn tại nhà'].id, author:'Join 30days', content:'Mình tham gia! Bắt đầu từ ngày mai luôn.', created_at:new Date('2025-03-23') },
            { post_id:postMap['Thử thách 30 ngày nấu ăn tại nhà'].id, author:'Healthy Fan', content:'Đã tiết kiệm được 1.5 triệu tháng trước nhờ nấu nhà!', created_at:new Date('2025-03-23') },
        ];
        await PostComment.bulkCreate(comments);
        console.log('✅ PostComment records seeded.');

        console.log('🎉 Safe seeding of recipes completed successfully!');
    } catch (error) {
        console.error('❌ Safe seeding failed:', error);
    } finally {
        await sequelize.close();
    }
}

runSafeSeed();
