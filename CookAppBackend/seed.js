/**
 * seed.js — Nạp dữ liệu mẫu đầy đủ cho CookApp Backend
 * Chạy: node seed.js
 */
const bcrypt = require('bcryptjs');
const {
    sequelize, Category, DietType, Ingredient, Recipe,
    RecipeStep, RecipeIngredient, NutritionFact, Review, Post, PostComment, User, StoreProduct
} = require('./models');

async function seedDatabase() {
    try {
        await sequelize.sync({ force: true });
        console.log('✅ Database synced (all tables recreated).');

        // ────────────────────────────────────────────
        // 0. DEMO USERS
        // ────────────────────────────────────────────
        const hashedPw = await bcrypt.hash('123456', 10);
        const [demoUser1, demoUser2] = await User.bulkCreate([
            { email: 'demo@cookapp.vn', password: hashedPw, fullName: 'Nguyễn Văn Demo' },
            { email: 'user2@cookapp.vn', password: hashedPw, fullName: 'Trần Thị User' },
        ]);
        console.log('✅ Demo users inserted.');

        // ────────────────────────────────────────────
        // 1. CATEGORIES (8 danh mục)
        // ────────────────────────────────────────────
        const [catGiaDinh, catLanhManh, catTrangMieng, catBanh, catNhanh, catDacSan, catChay, catNuong] =
            await Category.bulkCreate([
                { name: 'Gia đình' },
                { name: 'Lành mạnh' },
                { name: 'Tráng miệng' },
                { name: 'Bánh' },
                { name: 'Nhanh & dễ' },
                { name: 'Đặc sản vùng miền' },
                { name: 'Chay' },
                { name: 'Món nướng' },
            ]);
        console.log('✅ Categories inserted.');

        // ────────────────────────────────────────────
        // 2. DIET TYPES (TAGS)
        // ────────────────────────────────────────────
        const [
            dietChay, dietKeto, dietLowCarb, dietEatClean,
            dietKhongGluten, dietKhongBoSua, dietKhongHaiSan, dietKhongDauPhong
        ] = await DietType.bulkCreate([
            { name: 'Ăn chay' },
            { name: 'Keto' },
            { name: 'Low-carb' },
            { name: 'Eat-clean' },
            { name: 'Không Gluten' },
            { name: 'Không Bơ Sữa' },
            { name: 'Không Hải Sản' },
            { name: 'Không Đậu Phộng' }
        ]);
        console.log('✅ Diet types (Tags) inserted.');

        // ────────────────────────────────────────────
        // 3. INGREDIENTS (30 nguyên liệu — đồng bộ với Android Room)
        // ────────────────────────────────────────────
        const ings = await Ingredient.bulkCreate([
            { name: 'Thịt gà' },    // id 1
            { name: 'Sả' },          // id 2
            { name: 'Ớt' },          // id 3
            { name: 'Đậu phụ' },     // id 4
            { name: 'Cà chua' },     // id 5
            { name: 'Hành tây' },    // id 6
            { name: 'Trứng gà' },    // id 7
            { name: 'Thịt bò' },     // id 8
            { name: 'Bánh phở' },    // id 9
            { name: 'Nước mắm' },    // id 10
            { name: 'Đường' },       // id 11
            { name: 'Tiêu' },        // id 12
            { name: 'Tỏi' },         // id 13
            { name: 'Gừng' },        // id 14
            { name: 'Bột mì' },      // id 15
            { name: 'Sữa tươi' },    // id 16
            { name: 'Thịt heo' },    // id 17
            { name: 'Rau cải' },     // id 18
            { name: 'Nước dừa' },    // id 19
            { name: 'Bánh mì' },     // id 20
            { name: 'Khoai tây' },   // id 21
            { name: 'Cà rốt' },      // id 22
            { name: 'Nấm' },         // id 23
            { name: 'Tôm' },         // id 24
            { name: 'Cua' },         // id 25
            { name: 'Bún' },         // id 26
            { name: 'Cơm' },         // id 27
            { name: 'Dầu hào' },     // id 28
            { name: 'Hành lá' },     // id 29
            { name: 'Rau muống' },   // id 30
        ]);
        // Tham chiếu ngắn gọn
        const [tGa, sa, ot, dauPhu, caChua, hanhTay, trungGa, tBo, banhPho,
               nuocMam, duong, tieu, toi, gung, botMi, suaTuoi, tHeo, rauCai,
               nuocDua, banhMi, khoaiTay, caRot, nam, tom, cua, bun, com,
               dauHao, hanhLa, rauMuong] = ings;
        console.log('✅ Ingredients inserted.');

        // ────────────────────────────────────────────
        // 4. RECIPES (12 công thức — đồng bộ với Android)
        // ────────────────────────────────────────────
        const [r1, r2, r3, r4, r5, r6, r7, r8, r9, r10, r11, r12] = await Recipe.bulkCreate([
            { id: 1, title:'Gà xào sả ớt', description:'Món ăn đưa cơm thơm ngon đậm vị gia đình.',
              image_url:'/videos/thumbnails/ga_xao_xa_ot.jpg',
              video_url:'/videos/ga_xao_xa_ot.mp4', video_thumbnail_url:'/videos/thumbnails/ga_xao_xa_ot.jpg',
              cook_time:30, difficulty:'Dễ', servings:4, calories:350, category_id:catGiaDinh.id, diet_type_id:1 },

            { id: 2, title:'Đậu phụ sốt cà chua', description:'Món chay thanh đạm, dễ nấu, phù hợp ăn kiêng nhẹ.',
              image_url:'/videos/thumbnails/dau_phu_sot_ca_chua.jpg',
              video_url:'/videos/dau_phu_sot_ca_chua.mp4', video_thumbnail_url:'/videos/thumbnails/dau_phu_sot_ca_chua.jpg',
              cook_time:20, difficulty:'Rất dễ', servings:2, calories:180, category_id:catChay.id, diet_type_id:1 },

            { id: 3, title:'Bún bò Huế', description:'Đặc sản miền Trung với nước dùng cay nồng thơm mùi ruốc sả.',
              image_url:'/videos/thumbnails/bun_bo_hue.jpg',
              video_url:'/videos/bun_bo_hue.mp4', video_thumbnail_url:'/videos/thumbnails/bun_bo_hue.jpg',
              cook_time:90, difficulty:'Trung bình', servings:4, calories:480, category_id:catDacSan.id, diet_type_id:1 },

            { id: 4, title:'Phở Bò truyền thống', description:'Phở bò Hà Nội với nước dùng trong, ngọt tự nhiên từ xương bò hầm kỹ.',
              image_url:'/videos/thumbnails/pho_bo_truyen_thong.jpg',
              video_url:'/videos/pho_bo_truyen_thong.mp4', video_thumbnail_url:'/videos/thumbnails/pho_bo_truyen_thong.jpg',
              cook_time:120, difficulty:'Khó', servings:4, calories:420, category_id:catDacSan.id, diet_type_id:1 },

            { id: 5, title:'Bánh mì trứng ốp la', description:'Bữa sáng nhanh gọn 10 phút, đủ năng lượng cho cả ngày làm việc.',
              image_url:'/videos/thumbnails/banh_mi_trung_op_la.jpg',
              video_url:'/videos/banh_mi_trung_op_la.mp4', video_thumbnail_url:'/videos/thumbnails/banh_mi_trung_op_la.jpg',
              cook_time:10, difficulty:'Rất dễ', servings:1, calories:320, category_id:catNhanh.id, diet_type_id:1 },

            { id: 6, title:'Canh rau cải nấu thịt heo', description:'Canh thanh mát giải nhiệt, bổ dưỡng và dễ nấu cho bữa cơm gia đình.',
              image_url:'/videos/thumbnails/canh_rau_nau_thit_heo.jpg',
              video_url:'/videos/canh_rau_nau_thit_heo.mp4', video_thumbnail_url:'/videos/thumbnails/canh_rau_nau_thit_heo.jpg',
              cook_time:20, difficulty:'Dễ', servings:3, calories:150, category_id:catGiaDinh.id, diet_type_id:1 },

            { id: 7, title:'Cơm rang dưa bò', description:'Cơm rang thơm ngon từ thịt bò và dưa cải muối, nhanh gọn cho bữa trưa.',
              image_url:'/videos/thumbnails/com_rang_dua_bo.jpg',
              video_url:'/videos/com_rang_dua_bo.mp4', video_thumbnail_url:'/videos/thumbnails/com_rang_dua_bo.jpg',
              cook_time:15, difficulty:'Dễ', servings:2, calories:400, category_id:catNhanh.id, diet_type_id:1 },

            { id: 8, title:'Tôm xào bông cải', description:'Tôm tươi xào với bông cải xanh giòn ngọt, ít dầu mỡ và giàu dinh dưỡng.',
              image_url:'/videos/thumbnails/tom_xao_bong_cai.jpg',
              video_url:'/videos/tom_xao_bong_cai.mp4', video_thumbnail_url:'/videos/thumbnails/tom_xao_bong_cai.jpg',
              cook_time:20, difficulty:'Dễ', servings:3, calories:220, category_id:catLanhManh.id, diet_type_id:1 },

            { id: 9, title:'Súp bí đỏ kem', description:'Súp bí đỏ mịn mượt, béo nhẹ từ kem tươi, thích hợp mùa đông.',
              image_url:'/videos/thumbnails/sup_bi_do_kem.jpg',
              video_url:'/videos/sup_bi_do_kem.mp4', video_thumbnail_url:'/videos/thumbnails/sup_bi_do_kem.jpg',
              cook_time:30, difficulty:'Dễ', servings:4, calories:160, category_id:catLanhManh.id, diet_type_id:1 },

            { id: 10, title:'Gỏi cuốn tôm thịt', description:'Gỏi cuốn Việt Nam truyền thống với tôm, thịt, rau sống và tương hoisin.',
              image_url:'/videos/thumbnails/goi_cuon_tom_thit.jpg',
              video_url:'/videos/goi_cuon_tom_thit.mp4', video_thumbnail_url:'/videos/thumbnails/goi_cuon_tom_thit.jpg',
              cook_time:40, difficulty:'Trung bình', servings:4, calories:180, category_id:catDacSan.id, diet_type_id:1 },

            { id: 11, title:'Chả giò (Nem rán)', description:'Chả giò giòn rụm với nhân thịt và rau củ đậm đà, kèm nước chấm chua ngọt.',
              image_url:'/videos/thumbnails/cha_gio.jpg',
              video_url:'/videos/cha_gio.mp4', video_thumbnail_url:'/videos/thumbnails/cha_gio.jpg',
              cook_time:60, difficulty:'Trung bình', servings:6, calories:280, category_id:catDacSan.id, diet_type_id:1 },

            { id: 12, title:'Bánh flan caramel', description:'Bánh flan mềm mịn như lụa, thơm béo với lớp caramel đắng nhẹ đẹp mắt.',
              image_url:'/videos/thumbnails/banh_flan.jpg',
              video_url:'/videos/banh_flan.mp4', video_thumbnail_url:'/videos/thumbnails/banh_flan.jpg',
              cook_time:50, difficulty:'Trung bình', servings:6, calories:190, category_id:catTrangMieng.id, diet_type_id:1 },
        ]);
        console.log('✅ Recipes inserted (12).');

        // ────────────────────────────────────────────
        // 4b. RECIPE-CATEGORY MAPPING (M-N: 1 món có thể thuộc nhiều danh mục)
        // ────────────────────────────────────────────
        const { RecipeCategory, RecipeDietType } = require('./models');
        await RecipeCategory.bulkCreate([
            // R1 – Gà xào sả ớt → Gia đình + Nhanh & dễ
            { recipe_id: r1.id, category_id: catGiaDinh.id },
            { recipe_id: r1.id, category_id: catNhanh.id },

            // R2 – Đậu phụ sốt cà chua → Chay + Lành mạnh + Gia đình
            { recipe_id: r2.id, category_id: catChay.id },
            { recipe_id: r2.id, category_id: catLanhManh.id },
            { recipe_id: r2.id, category_id: catGiaDinh.id },

            // R3 – Bún bò Huế → Đặc sản + Gia đình
            { recipe_id: r3.id, category_id: catDacSan.id },
            { recipe_id: r3.id, category_id: catGiaDinh.id },

            // R4 – Phở Bò → Đặc sản + Gia đình
            { recipe_id: r4.id, category_id: catDacSan.id },
            { recipe_id: r4.id, category_id: catGiaDinh.id },

            // R5 – Bánh mì trứng ốp la → Nhanh & dễ + Gia đình
            { recipe_id: r5.id, category_id: catNhanh.id },
            { recipe_id: r5.id, category_id: catGiaDinh.id },

            // R6 – Canh rau cải nấu thịt heo → Gia đình + Lành mạnh
            { recipe_id: r6.id, category_id: catGiaDinh.id },
            { recipe_id: r6.id, category_id: catLanhManh.id },

            // R7 – Cơm rang dưa bò → Nhanh & dễ + Gia đình
            { recipe_id: r7.id, category_id: catNhanh.id },
            { recipe_id: r7.id, category_id: catGiaDinh.id },

            // R8 – Tôm xào bông cải → Lành mạnh + Nhanh & dễ
            { recipe_id: r8.id, category_id: catLanhManh.id },
            { recipe_id: r8.id, category_id: catNhanh.id },

            // R9 – Súp bí đỏ kem → Lành mạnh + Chay (không thịt)
            { recipe_id: r9.id, category_id: catLanhManh.id },
            { recipe_id: r9.id, category_id: catChay.id },

            // R10 – Gỏi cuốn → Đặc sản + Lành mạnh
            { recipe_id: r10.id, category_id: catDacSan.id },
            { recipe_id: r10.id, category_id: catLanhManh.id },

            // R11 – Chả giò → Đặc sản + Món nướng (chiên giòn)
            { recipe_id: r11.id, category_id: catDacSan.id },
            { recipe_id: r11.id, category_id: catNuong.id },

            // R12 – Bánh flan caramel → Tráng miệng + Bánh
            { recipe_id: r12.id, category_id: catTrangMieng.id },
            { recipe_id: r12.id, category_id: catBanh.id },
        ]);
        console.log('✅ Recipe-category mappings inserted.');

        // ────────────────────────────────────────────
        // 4c. RECIPE-DIETTYPE MAPPING (M-N Tags)
        // ────────────────────────────────────────────
        await RecipeDietType.bulkCreate([
            // r1: Gà xào sả ớt -> Keto, Low-carb, Không Gluten, Không Bơ Sữa, Không Hải Sản, Không Đậu Phộng
            { recipe_id: r1.id, diet_type_id: dietKeto.id },
            { recipe_id: r1.id, diet_type_id: dietLowCarb.id },
            { recipe_id: r1.id, diet_type_id: dietKhongGluten.id },
            { recipe_id: r1.id, diet_type_id: dietKhongBoSua.id },
            { recipe_id: r1.id, diet_type_id: dietKhongHaiSan.id },
            { recipe_id: r1.id, diet_type_id: dietKhongDauPhong.id },

            // r2: Đậu phụ sốt cà chua -> Ăn chay, Eat-clean, Không Gluten, Không Bơ Sữa, Không Hải Sản, Không Đậu Phộng
            { recipe_id: r2.id, diet_type_id: dietChay.id },
            { recipe_id: r2.id, diet_type_id: dietEatClean.id },
            { recipe_id: r2.id, diet_type_id: dietKhongGluten.id },
            { recipe_id: r2.id, diet_type_id: dietKhongBoSua.id },
            { recipe_id: r2.id, diet_type_id: dietKhongHaiSan.id },
            { recipe_id: r2.id, diet_type_id: dietKhongDauPhong.id },

            // r3: Bún bò Huế -> Không Gluten, Không Bơ Sữa, Không Đậu Phộng (Có mắm ruốc tôm -> ko tag Không hải sản)
            { recipe_id: r3.id, diet_type_id: dietKhongGluten.id },
            { recipe_id: r3.id, diet_type_id: dietKhongBoSua.id },
            { recipe_id: r3.id, diet_type_id: dietKhongDauPhong.id },

            // r4: Phở Bò truyền thống -> Không Gluten, Không Bơ Sữa, Không Hải Sản, Không Đậu Phộng
            { recipe_id: r4.id, diet_type_id: dietKhongGluten.id },
            { recipe_id: r4.id, diet_type_id: dietKhongBoSua.id },
            { recipe_id: r4.id, diet_type_id: dietKhongHaiSan.id },
            { recipe_id: r4.id, diet_type_id: dietKhongDauPhong.id },

            // r5: Bánh mì trứng ốp la -> Không Hải sản, Không Đậu phộng (Có phết bơ/pate và bột mì -> Có bơ sữa, gluten)
            { recipe_id: r5.id, diet_type_id: dietKhongHaiSan.id },
            { recipe_id: r5.id, diet_type_id: dietKhongDauPhong.id },

            // r6: Canh rau cải nấu thịt heo -> Eat-clean, Keto, Low-carb, Không Gluten, Không Bơ sữa, Không Hải Sản, Không Đậu phộng
            { recipe_id: r6.id, diet_type_id: dietEatClean.id },
            { recipe_id: r6.id, diet_type_id: dietKeto.id },
            { recipe_id: r6.id, diet_type_id: dietLowCarb.id },
            { recipe_id: r6.id, diet_type_id: dietKhongGluten.id },
            { recipe_id: r6.id, diet_type_id: dietKhongBoSua.id },
            { recipe_id: r6.id, diet_type_id: dietKhongHaiSan.id },
            { recipe_id: r6.id, diet_type_id: dietKhongDauPhong.id },

            // r7: Cơm rang dưa bò -> Không Gluten, Không Bơ Sữa, Không Hải Sản, Không Đậu Phộng
            { recipe_id: r7.id, diet_type_id: dietKhongGluten.id },
            { recipe_id: r7.id, diet_type_id: dietKhongBoSua.id },
            { recipe_id: r7.id, diet_type_id: dietKhongHaiSan.id },
            { recipe_id: r7.id, diet_type_id: dietKhongDauPhong.id },

            // r8: Tôm xào bông cải -> Eat-clean, Keto, Low-carb, Không Gluten, Không Bơ sữa, Không Đậu phộng (Có hải sản tôm)
            { recipe_id: r8.id, diet_type_id: dietEatClean.id },
            { recipe_id: r8.id, diet_type_id: dietKeto.id },
            { recipe_id: r8.id, diet_type_id: dietLowCarb.id },
            { recipe_id: r8.id, diet_type_id: dietKhongGluten.id },
            { recipe_id: r8.id, diet_type_id: dietKhongBoSua.id },
            { recipe_id: r8.id, diet_type_id: dietKhongDauPhong.id },

            // r9: Súp bí đỏ kem -> Ăn chay, Không Gluten, Không Hải Sản, Không Đậu Phộng (Sử dụng kem tươi topping ngậy -> Có bơ sữa)
            { recipe_id: r9.id, diet_type_id: dietChay.id },
            { recipe_id: r9.id, diet_type_id: dietKhongGluten.id },
            { recipe_id: r9.id, diet_type_id: dietKhongHaiSan.id },
            { recipe_id: r9.id, diet_type_id: dietKhongDauPhong.id },

            // r10: Gỏi cuốn tôm thịt -> Eat-clean, Không Gluten, Không Bơ Sữa (Cuốn tôm, chấm sốt bơ đậu phộng -> có hải sản, có đậu phộng)
            { recipe_id: r10.id, diet_type_id: dietEatClean.id },
            { recipe_id: r10.id, diet_type_id: dietKhongGluten.id },
            { recipe_id: r10.id, diet_type_id: dietKhongBoSua.id },

            // r11: Chả giò (Nem rán) -> Không Bơ Sữa, Không Đậu Phộng (Có tôm cua, cuốn bánh tráng ko lúa mì -> Không Gluten? Để an toàn ko tag)
            { recipe_id: r11.id, diet_type_id: dietKhongBoSua.id },
            { recipe_id: r11.id, diet_type_id: dietKhongDauPhong.id },

            // r12: Bánh flan caramel -> Ăn chay (lacto-ovo), Không Gluten, Không Hải Sản, Không Đậu Phộng (Dùng sữa tươi -> Có bơ sữa)
            { recipe_id: r12.id, diet_type_id: dietChay.id },
            { recipe_id: r12.id, diet_type_id: dietKhongGluten.id },
            { recipe_id: r12.id, diet_type_id: dietKhongHaiSan.id },
            { recipe_id: r12.id, diet_type_id: dietKhongDauPhong.id },
        ]);
        console.log('✅ Recipe-DietType (Tags) mapping inserted.');

        // ────────────────────────────────────────────
        // 5. RECIPE STEPS (65+ bước)
        // ────────────────────────────────────────────
        await RecipeStep.bulkCreate([
            // R1 – Gà xào sả ớt (Thêm video_start_time mẫu để demo độ chính xác mili-giây)
            { recipe_id:r1.id, step_number:1, instruction:'Rửa sạch thịt gà, chặt miếng vừa ăn và để ráo nước.', timer_seconds:0, video_start_time: 0 },
            { recipe_id:r1.id, step_number:2, instruction:'Ướp gà cùng sả băm, ớt, muối, nước mắm và 1 muỗng đường. Để ngấm 15 phút.', timer_seconds:900, video_start_time: 5 },
            { recipe_id:r1.id, step_number:3, instruction:'Bắc chảo lên bếp, đổ dầu ăn, phi tỏi vàng thơm.', timer_seconds:60, video_start_time: 12 },
            { recipe_id:r1.id, step_number:4, instruction:'Xào gà trên lửa lớn khoảng 5 phút đến khi chín vàng đều.', timer_seconds:300, video_start_time: 18 },
            { recipe_id:r1.id, step_number:5, instruction:'Thêm sả cây đập dập vào xào cùng.', timer_seconds:60, video_start_time: 25 },
            { recipe_id:r1.id, step_number:6, instruction:'Nêm gia vị vừa miệng, tắt bếp, trang trí lá chanh và ớt.', timer_seconds:0, video_start_time: 32 },

            // R2 – Đậu phụ sốt cà chua
            { recipe_id:r2.id, step_number:1, instruction:'Cắt đậu phụ thành miếng vuông khoảng 2cm.', timer_seconds:0 },
            { recipe_id:r2.id, step_number:2, instruction:'Chiên đậu phụ trong dầu nóng đến vàng đều hai mặt.', timer_seconds:240 },
            { recipe_id:r2.id, step_number:3, instruction:'Xào cà chua thái múi cua với hành tím đến khi nhuyễn, ra màu đỏ.', timer_seconds:120 },
            { recipe_id:r2.id, step_number:4, instruction:'Cho đậu phụ chiên vào sốt, đảo nhẹ tay.', timer_seconds:60 },
            { recipe_id:r2.id, step_number:5, instruction:'Nêm nước mắm, đường vừa miệng và rắc hành lá.', timer_seconds:30 },

            // R3 – Bún bò Huế
            { recipe_id:r3.id, step_number:1, instruction:'Hầm xương heo và xương bò với gừng, hành khô trong 2 tiếng để lấy nước trong.', timer_seconds:7200 },
            { recipe_id:r3.id, step_number:2, instruction:'Phi sả băm cùng dầu màu điều cho thơm, ra màu đỏ đẹp.', timer_seconds:300 },
            { recipe_id:r3.id, step_number:3, instruction:'Cho hỗn hợp sả vào nồi nước dùng, thêm mắm ruốc, nêm nước mắm và muối.', timer_seconds:0 },
            { recipe_id:r3.id, step_number:4, instruction:'Luộc thịt bò, chả Huế vừa chín tới rồi thái lát.', timer_seconds:600 },
            { recipe_id:r3.id, step_number:5, instruction:'Trần bún qua nước sôi, xếp vào tô, để thịt lên trên.', timer_seconds:0 },
            { recipe_id:r3.id, step_number:6, instruction:'Chan nước dùng đang sôi vào tô, thêm hành lá, ớt và chanh.', timer_seconds:0 },

            // R4 – Phở Bò
            { recipe_id:r4.id, step_number:1, instruction:'Nướng gừng và hành tím cho đến khi thơm và có màu.', timer_seconds:0 },
            { recipe_id:r4.id, step_number:2, instruction:'Chần xương bò qua nước sôi 10 phút, đổ nước đi, rửa sạch xương.', timer_seconds:600 },
            { recipe_id:r4.id, step_number:3, instruction:'Hầm xương bò 3 tiếng với gừng nướng, hành, quế, hoa hồi.', timer_seconds:10800 },
            { recipe_id:r4.id, step_number:4, instruction:'Nêm nước mắm, đường, muối vừa miệng và lọc nước dùng.', timer_seconds:0 },
            { recipe_id:r4.id, step_number:5, instruction:'Thái thịt bò sống thành lát mỏng.', timer_seconds:0 },
            { recipe_id:r4.id, step_number:6, instruction:'Trần bánh phở, xếp thịt bò tái lên, chan nước dùng sôi già và thêm hành lá.', timer_seconds:0 },

            // R5 – Bánh mì trứng ốp la
            { recipe_id:r5.id, step_number:1, instruction:'Đun nóng chảo chống dính với 1 thìa cà phê dầu ăn.', timer_seconds:30 },
            { recipe_id:r5.id, step_number:2, instruction:'Đập 2 quả trứng vào chảo, rán lửa vừa đến khi lòng trắng đông lại.', timer_seconds:120 },
            { recipe_id:r5.id, step_number:3, instruction:'Lật nhẹ và rán thêm 30 giây, hoặc giữ nguyên kiểu lòng đào.', timer_seconds:30 },
            { recipe_id:r5.id, step_number:4, instruction:'Rắc muối tiêu lên trứng.', timer_seconds:0 },
            { recipe_id:r5.id, step_number:5, instruction:'Kẹp trứng vào bánh mì cùng rau sống, dưa leo và tương ớt.', timer_seconds:0 },

            // R6 – Canh rau cải
            { recipe_id:r6.id, step_number:1, instruction:'Thái thịt heo lát mỏng, ướp muối tiêu 5 phút.', timer_seconds:300 },
            { recipe_id:r6.id, step_number:2, instruction:'Rửa và cắt rau cải thành khúc 4–5cm.', timer_seconds:0 },
            { recipe_id:r6.id, step_number:3, instruction:'Phi hành tím cho thơm, cho thịt heo vào xào chín.', timer_seconds:120 },
            { recipe_id:r6.id, step_number:4, instruction:'Đổ 500ml nước vào, đun sôi, nêm nước mắm và đường.', timer_seconds:300 },
            { recipe_id:r6.id, step_number:5, instruction:'Cho rau cải vào nấu thêm 3 phút là chín tới.', timer_seconds:180 },

            // R7 – Cơm rang dưa bò
            { recipe_id:r7.id, step_number:1, instruction:'Thái thịt bò lát mỏng, ướp nước mắm, tiêu, dầu hào.', timer_seconds:300 },
            { recipe_id:r7.id, step_number:2, instruction:'Xào dưa cải muối đã vắt nước với tỏi cho vàng thơm.', timer_seconds:120 },
            { recipe_id:r7.id, step_number:3, instruction:'Thêm thịt bò vào xào đến khi chín, trải đều.', timer_seconds:120 },
            { recipe_id:r7.id, step_number:4, instruction:'Cho cơm nguội vào đảo đều trên lửa lớn.', timer_seconds:180 },
            { recipe_id:r7.id, step_number:5, instruction:'Nêm nước mắm vừa miệng, rắc hành lá và tiêu.', timer_seconds:0 },

            // R8 – Tôm xào bông cải
            { recipe_id:r8.id, step_number:1, instruction:'Bóc vỏ tôm, rút chỉ đen và ướp muối tiêu.', timer_seconds:300 },
            { recipe_id:r8.id, step_number:2, instruction:'Cắt bông cải xanh thành florets, chần qua nước sôi 1 phút.', timer_seconds:60 },
            { recipe_id:r8.id, step_number:3, instruction:'Phi tỏi vàng, cho tôm vào xào chín hồng rồi gắp ra.', timer_seconds:120 },
            { recipe_id:r8.id, step_number:4, instruction:'Xào bông cải với dầu hào và nước mắm đến khi vừa mềm.', timer_seconds:120 },
            { recipe_id:r8.id, step_number:5, instruction:'Trộn tôm vào bông cải, đảo đều, nêm lại vừa miệng và dọn ra đĩa.', timer_seconds:30 },

            // R9 – Súp bí đỏ kem
            { recipe_id:r9.id, step_number:1, instruction:'Gọt vỏ bí đỏ, cắt khối vuông 2cm.', timer_seconds:0 },
            { recipe_id:r9.id, step_number:2, instruction:'Xào hành tây thái hạt lựu với bơ đến khi trong suốt.', timer_seconds:300 },
            { recipe_id:r9.id, step_number:3, instruction:'Cho bí đỏ vào xào cùng, thêm 600ml nước dùng gà hoặc nước lọc.', timer_seconds:0 },
            { recipe_id:r9.id, step_number:4, instruction:'Đun sôi rồi hạ lửa, ninh 20 phút đến khi bí mềm.', timer_seconds:1200 },
            { recipe_id:r9.id, step_number:5, instruction:'Xay nhuyễn bằng máy xay cắm tay, thêm kem tươi, nêm muối tiêu.', timer_seconds:0 },
            { recipe_id:r9.id, step_number:6, instruction:'Trang trí với kem tươi, hạt bí rang và lá húng quế.', timer_seconds:0 },

            // R10 – Gỏi cuốn tôm thịt
            { recipe_id:r10.id, step_number:1, instruction:'Luộc tôm chín, bóc vỏ và chẻ đôi dọc lưng.', timer_seconds:300 },
            { recipe_id:r10.id, step_number:2, instruction:'Luộc thịt heo, thái lát mỏng.', timer_seconds:600 },
            { recipe_id:r10.id, step_number:3, instruction:'Chuẩn bị bún, rau sống, húng quế, cà rốt và dưa leo bào sợi.', timer_seconds:0 },
            { recipe_id:r10.id, step_number:4, instruction:'Nhúng bánh tráng qua nước ấm vài giây đến khi mềm.', timer_seconds:0 },
            { recipe_id:r10.id, step_number:5, instruction:'Xếp tôm, thịt, bún và rau vào giữa bánh tráng, cuốn chặt tay.', timer_seconds:0 },
            { recipe_id:r10.id, step_number:6, instruction:'Pha nước chấm hoisin với tương đen, lạc rang và tương ớt.', timer_seconds:0 },

            // R11 – Chả giò
            { recipe_id:r11.id, step_number:1, instruction:'Trộn nhân chả giò: thịt heo băm, miến, cà rốt bào, nấm mèo, trứng, gia vị.', timer_seconds:0 },
            { recipe_id:r11.id, step_number:2, instruction:'Nhúng bánh tráng chả giò qua nước, xếp nhân vào và cuốn chặt.', timer_seconds:0 },
            { recipe_id:r11.id, step_number:3, instruction:'Chiên trong dầu sâu ở 160°C khoảng 8 phút đến vàng nhạt.', timer_seconds:480 },
            { recipe_id:r11.id, step_number:4, instruction:'Vớt ra để ráo, rồi chiên lần 2 ở 180°C khoảng 3 phút để giòn.', timer_seconds:180 },
            { recipe_id:r11.id, step_number:5, instruction:'Pha nước chấm chua ngọt và dọn ra cùng rau sống.', timer_seconds:0 },

            // R12 – Bánh flan
            { recipe_id:r12.id, step_number:1, instruction:'Đun đường với 3 muỗng nước đến khi thành caramel vàng nâu. Đổ vào khuôn.', timer_seconds:300 },
            { recipe_id:r12.id, step_number:2, instruction:'Đun sữa tươi và kem tươi đến gần sôi, để nguội bớt.', timer_seconds:0 },
            { recipe_id:r12.id, step_number:3, instruction:'Đánh trứng gà với đường vani đến khi hòa tan.', timer_seconds:0 },
            { recipe_id:r12.id, step_number:4, instruction:'Đổ sữa ấm vào hỗn hợp trứng, khuấy đều và lọc qua rây.', timer_seconds:0 },
            { recipe_id:r12.id, step_number:5, instruction:'Đổ hỗn hợp vào khuôn caramel, hấp cách thủy 25 phút ở lửa nhỏ.', timer_seconds:1500 },
            { recipe_id:r12.id, step_number:6, instruction:'Để nguội rồi cho vào tủ lạnh tối thiểu 2 tiếng trước khi lật ra đĩa.', timer_seconds:7200 },
        ]);
        console.log('✅ Recipe steps inserted (65+).');

        // ────────────────────────────────────────────
        // 6. RECIPE INGREDIENTS
        // ────────────────────────────────────────────
        await RecipeIngredient.bulkCreate([
            // R1 – Gà xào sả ớt
            { recipe_id:r1.id, ingredient_id:tGa.id, quantity:500, unit:'gram' },
            { recipe_id:r1.id, ingredient_id:sa.id, quantity:3, unit:'cây' },
            { recipe_id:r1.id, ingredient_id:ot.id, quantity:2, unit:'quả' },
            { recipe_id:r1.id, ingredient_id:toi.id, quantity:4, unit:'tép' },
            { recipe_id:r1.id, ingredient_id:nuocMam.id, quantity:2, unit:'muỗng' },
            { recipe_id:r1.id, ingredient_id:duong.id, quantity:1, unit:'muỗng' },
            // R2 – Đậu phụ sốt cà chua
            { recipe_id:r2.id, ingredient_id:dauPhu.id, quantity:300, unit:'gram' },
            { recipe_id:r2.id, ingredient_id:caChua.id, quantity:3, unit:'quả' },
            { recipe_id:r2.id, ingredient_id:hanhTay.id, quantity:1, unit:'củ' },
            { recipe_id:r2.id, ingredient_id:nuocMam.id, quantity:1, unit:'muỗng' },
            { recipe_id:r2.id, ingredient_id:hanhLa.id, quantity:2, unit:'cây' },
            // R3 – Bún bò Huế
            { recipe_id:r3.id, ingredient_id:tBo.id, quantity:300, unit:'gram' },
            { recipe_id:r3.id, ingredient_id:bun.id, quantity:400, unit:'gram' },
            { recipe_id:r3.id, ingredient_id:sa.id, quantity:5, unit:'cây' },
            { recipe_id:r3.id, ingredient_id:gung.id, quantity:1, unit:'củ nhỏ' },
            { recipe_id:r3.id, ingredient_id:ot.id, quantity:3, unit:'quả' },
            // R4 – Phở Bò
            { recipe_id:r4.id, ingredient_id:tBo.id, quantity:400, unit:'gram' },
            { recipe_id:r4.id, ingredient_id:banhPho.id, quantity:400, unit:'gram' },
            { recipe_id:r4.id, ingredient_id:gung.id, quantity:1, unit:'củ' },
            { recipe_id:r4.id, ingredient_id:nuocMam.id, quantity:3, unit:'muỗng' },
            // R5 – Bánh mì trứng ốp la
            { recipe_id:r5.id, ingredient_id:banhMi.id, quantity:1, unit:'ổ' },
            { recipe_id:r5.id, ingredient_id:trungGa.id, quantity:2, unit:'quả' },
            { recipe_id:r5.id, ingredient_id:tieu.id, quantity:0.5, unit:'muỗng cà phê' },
            // R6 – Canh rau cải
            { recipe_id:r6.id, ingredient_id:tHeo.id, quantity:200, unit:'gram' },
            { recipe_id:r6.id, ingredient_id:rauCai.id, quantity:300, unit:'gram' },
            { recipe_id:r6.id, ingredient_id:nuocMam.id, quantity:2, unit:'muỗng' },
            // R7 – Cơm rang dưa bò
            { recipe_id:r7.id, ingredient_id:tBo.id, quantity:200, unit:'gram' },
            { recipe_id:r7.id, ingredient_id:com.id, quantity:400, unit:'gram' },
            { recipe_id:r7.id, ingredient_id:dauHao.id, quantity:1, unit:'muỗng' },
            { recipe_id:r7.id, ingredient_id:hanhLa.id, quantity:3, unit:'cây' },
            // R8 – Tôm xào bông cải
            { recipe_id:r8.id, ingredient_id:tom.id, quantity:300, unit:'gram' },
            { recipe_id:r8.id, ingredient_id:nuocMam.id, quantity:1, unit:'muỗng' },
            { recipe_id:r8.id, ingredient_id:toi.id, quantity:3, unit:'tép' },
            { recipe_id:r8.id, ingredient_id:dauHao.id, quantity:1, unit:'muỗng' },
            // R9 – Súp bí đỏ
            { recipe_id:r9.id, ingredient_id:khoaiTay.id, quantity:400, unit:'gram' },
            { recipe_id:r9.id, ingredient_id:hanhTay.id, quantity:1, unit:'củ' },
            { recipe_id:r9.id, ingredient_id:suaTuoi.id, quantity:200, unit:'ml' },
            // R10 – Gỏi cuốn
            { recipe_id:r10.id, ingredient_id:tom.id, quantity:200, unit:'gram' },
            { recipe_id:r10.id, ingredient_id:tHeo.id, quantity:150, unit:'gram' },
            { recipe_id:r10.id, ingredient_id:bun.id, quantity:200, unit:'gram' },
            { recipe_id:r10.id, ingredient_id:caRot.id, quantity:1, unit:'củ' },
            // R11 – Chả giò
            { recipe_id:r11.id, ingredient_id:tHeo.id, quantity:300, unit:'gram' },
            { recipe_id:r11.id, ingredient_id:nam.id, quantity:100, unit:'gram' },
            { recipe_id:r11.id, ingredient_id:caRot.id, quantity:1, unit:'củ' },
            { recipe_id:r11.id, ingredient_id:trungGa.id, quantity:1, unit:'quả' },
            // R12 – Bánh flan
            { recipe_id:r12.id, ingredient_id:trungGa.id, quantity:3, unit:'quả' },
            { recipe_id:r12.id, ingredient_id:suaTuoi.id, quantity:300, unit:'ml' },
            { recipe_id:r12.id, ingredient_id:duong.id, quantity:80, unit:'gram' },
        ]);
        console.log('✅ Recipe ingredients inserted.');

        // ────────────────────────────────────────────
        // 7. NUTRITION FACTS (12 công thức)
        // ────────────────────────────────────────────
        await NutritionFact.bulkCreate([
            { recipe_id:r1.id,  calories:350, protein:28, fat:14, carbs:6,  fiber:1.2, sugar:2.5, sodium:820 },
            { recipe_id:r2.id,  calories:180, protein:12, fat:8,  carbs:14, fiber:2.0, sugar:4.0, sodium:560 },
            { recipe_id:r3.id,  calories:480, protein:35, fat:18, carbs:42, fiber:2.5, sugar:3.0, sodium:1100 },
            { recipe_id:r4.id,  calories:420, protein:30, fat:10, carbs:52, fiber:1.0, sugar:2.0, sodium:950 },
            { recipe_id:r5.id,  calories:320, protein:14, fat:12, carbs:38, fiber:2.0, sugar:3.0, sodium:620 },
            { recipe_id:r6.id,  calories:150, protein:15, fat:6,  carbs:8,  fiber:2.5, sugar:1.5, sodium:480 },
            { recipe_id:r7.id,  calories:400, protein:22, fat:12, carbs:52, fiber:1.5, sugar:2.0, sodium:780 },
            { recipe_id:r8.id,  calories:220, protein:24, fat:8,  carbs:12, fiber:3.0, sugar:2.0, sodium:680 },
            { recipe_id:r9.id,  calories:160, protein:4,  fat:8,  carbs:20, fiber:3.5, sugar:6.0, sodium:350 },
            { recipe_id:r10.id, calories:180, protein:16, fat:4,  carbs:22, fiber:2.0, sugar:2.5, sodium:520 },
            { recipe_id:r11.id, calories:280, protein:18, fat:14, carbs:24, fiber:1.5, sugar:2.0, sodium:750 },
            { recipe_id:r12.id, calories:190, protein:6,  fat:8,  carbs:26, fiber:0.2, sugar:22,  sodium:120 },
        ]);
        console.log('✅ Nutrition facts inserted.');

        // ────────────────────────────────────────────
        // 8. REVIEWS (20 đánh giá)
        // ────────────────────────────────────────────
        await Review.bulkCreate([
            { recipe_id:r1.id,  user_id:demoUser1.id, rating:5, comment:'Ngon tuyệt! Cả nhà mình đều thích.', author:'Nguyễn Văn Demo' },
            { recipe_id:r1.id,  user_id:demoUser2.id, rating:4, comment:'Thơm ngon nhưng tôi giảm ớt cho trẻ con.', author:'Trần Thị User' },
            { recipe_id:r1.id,  user_id:null,         rating:5, comment:'Đã thử nhiều công thức, cái này ngon nhất!', author:'Mai Linh' },
            { recipe_id:r2.id,  user_id:null,         rating:5, comment:'Đơn giản mà ngon, ăn với cơm rất hợp.', author:'Hoa Trang' },
            { recipe_id:r2.id,  user_id:null,         rating:4, comment:'Món chay ngon, con mình cũng thích.', author:'Nam Phong' },
            { recipe_id:r3.id,  user_id:null,         rating:5, comment:'Chuẩn vị Huế, nước dùng rất đậm đà.', author:'Thanh Hương' },
            { recipe_id:r3.id,  user_id:null,         rating:5, comment:'Tuyệt vời! Từng học ở Huế và đây chính xác là vị đó.', author:'Minh Khoa' },
            { recipe_id:r4.id,  user_id:demoUser1.id, rating:4, comment:'Phở ngon, nhưng hầm xương mất nhiều thời gian.', author:'Nguyễn Văn Demo' },
            { recipe_id:r4.id,  user_id:null,         rating:5, comment:'Nước dùng trong vắt và ngọt thanh, rất chuẩn!', author:'Thu Hà' },
            { recipe_id:r5.id,  user_id:null,         rating:5, comment:'10 phút có ngay bữa sáng ngon!', author:'Bình Minh' },
            { recipe_id:r6.id,  user_id:null,         rating:4, comment:'Canh thanh mát, giải nhiệt mùa hè rất tốt.', author:'Diễm Hương' },
            { recipe_id:r7.id,  user_id:null,         rating:4, comment:'Cơm rang đậm đà, thơm mùi dưa.', author:'Quang Minh' },
            { recipe_id:r8.id,  user_id:null,         rating:5, comment:'Tôm tươi xào bông cải giòn, bổ dưỡng!', author:'Thanh Thảo' },
            { recipe_id:r9.id,  user_id:null,         rating:5, comment:'Súp mịn như nhung, con bé nhà mình mê lắm.', author:'Vũ Hoa' },
            { recipe_id:r10.id, user_id:demoUser2.id, rating:5, comment:'Gỏi cuốn nhà làm ngon hơn ngoài hàng.', author:'Trần Thị User' },
            { recipe_id:r10.id, user_id:null,         rating:4, comment:'Cuốn đẹp và ngon, nước chấm chuẩn vị.', author:'Trung Hiếu' },
            { recipe_id:r11.id, user_id:null,         rating:5, comment:'Chả giò giòn rụm, nhân đầy đặn.', author:'Phương Linh' },
            { recipe_id:r11.id, user_id:null,         rating:5, comment:'Hai lần chiên là bí quyết để giòn lâu!', author:'Hoàng Nam' },
            { recipe_id:r12.id, user_id:null,         rating:5, comment:'Flan mịn mượt, caramel đắng vừa phải.', author:'Ngọc Ánh' },
            { recipe_id:r12.id, user_id:null,         rating:4, comment:'Cần hấp đúng lửa nhỏ để bánh không bị rỗ.', author:'Thành Long' },
        ]);
        console.log('✅ Reviews inserted (20).');

        // ────────────────────────────────────────────
        // 9. COMMUNITY POSTS (7 bài viết)
        // ────────────────────────────────────────────
        const [p1,p2,p3,p4,p5,p6,p7] = await Post.bulkCreate([
            { title:'Chia sẻ mẹo xào rau không bị thâm', content:'Để rau xào luôn xanh và giòn, cần xào lửa thật lớn và không đậy nắp. Thêm chút muối vào dầu trước khi xào cũng giúp rau giữ màu tốt hơn. Các bạn thử nhé!', author:'Đầu bếp Minh', likes:47, image_url:null, created_at:new Date('2025-03-10') },
            { title:'Review nồi chiên không dầu Philips', content:'Sau 3 tháng dùng, mình thấy nồi chiên Philips rất tiện. Gà chiên giòn ngon hơn so với chiên thường, ít dầu mỡ hơn. Tuy nhiên cần làm nóng nồi trước 3 phút mới cho thức ăn vào.', author:'Bếp Nhà Tôi', likes:32, image_url:null, created_at:new Date('2025-03-12') },
            { title:'Hỏi: Sốt xào thịt bò cần loại nào?', content:'Mình hay xào thịt bò với dầu hào nhưng hơi mặn. Mọi người có ai dùng loại sốt khác không? Mình muốn thử sốt teriyaki.', author:'Mèo Bếp', likes:15, image_url:null, created_at:new Date('2025-03-14') },
            { title:'Công thức bánh cupcake vị matcha', content:'Nguyên liệu: 150g bột mì, 80g bơ, 100g đường, 2 trứng, 2 muỗng bột matcha, 80ml sữa. Đánh bơ mềm với đường, thêm trứng, rồi trộn bột matcha vào. Đổ vào khuôn và nướng 180°C 20 phút là xong!', author:'Bakery Linh', likes:89, image_url:null, created_at:new Date('2025-03-16') },
            { title:'Mẹo luộc rau muống xanh mướt', content:'Thêm vài giọt dầu thực vật và muối vào nước sôi trước khi cho rau vào. Chỉ luộc 1-2 phút rồi vớt ra ngay ngâm vào nước lạnh. Rau sẽ xanh mướt và giòn ngon.', author:'Chị Ba Bếp', likes:63, image_url:null, created_at:new Date('2025-03-18') },
            { title:'Nên mua dao Thái hay dao Nhật?', content:'Dao Nhật thường mỏng hơn, sắc bén và giữ lưỡi lâu hơn nhưng giòn hơn. Dao Thái dày hơn, chặt được xương. Với đa năng thì dao Nhật hơn, còn nấu kiểu VN thì dao Thái tiện hơn.', author:'Chef Hoàng', likes:28, image_url:null, created_at:new Date('2025-03-20') },
            { title:'Thử thách 30 ngày nấu ăn tại nhà', content:'Mình đang thực hiện thử thách nấu ăn tại nhà 30 ngày thay vì order. Tiết kiệm được khoảng 2 triệu/tháng và ăn sạch hơn hẳn. Ai muốn join cùng không?', author:'Healthy Cooking', likes:112, image_url:null, created_at:new Date('2025-03-22') },
        ]);
        console.log('✅ Community posts inserted (7).');

        // Comments
        await PostComment.bulkCreate([
            { post_id:p1.id, author:'Lan Hương', content:'Mình làm thử rồi, rau xanh hơn hẳn luôn!', created_at:new Date('2025-03-11') },
            { post_id:p1.id, author:'Bếp Trưởng', content:'Lửa lớn là chìa khóa quan trọng nhất!', created_at:new Date('2025-03-11') },
            { post_id:p2.id, author:'Thanh Hà', content:'Mình cũng dùng Philips, đồng ý 100%!', created_at:new Date('2025-03-13') },
            { post_id:p3.id, author:'Chef A', content:'Sốt teriyaki ngon nhưng hơi ngọt, thử pha loãng nhé.', created_at:new Date('2025-03-15') },
            { post_id:p4.id, author:'Cupcake Queen', content:'Cảm ơn công thức nha! Mình sẽ thử cuối tuần này.', created_at:new Date('2025-03-17') },
            { post_id:p4.id, author:'Matcha Lover', content:'Cho thêm kem cheese frosting matcha vào là hoàn hảo!', created_at:new Date('2025-03-17') },
            { post_id:p5.id, author:'Rau Sạch', content:'Mẹo hay! Sẽ áp dụng ngay tối nay.', created_at:new Date('2025-03-19') },
            { post_id:p6.id, author:'Knife Collector', content:'Nên thêm dao Đức vào danh sách, cân bằng tốt lắm.', created_at:new Date('2025-03-21') },
            { post_id:p7.id, author:'Join 30days', content:'Mình tham gia! Bắt đầu từ ngày mai luôn.', created_at:new Date('2025-03-23') },
            { post_id:p7.id, author:'Healthy Fan', content:'Đã tiết kiệm được 1.5 triệu tháng trước nhờ nấu nhà!', created_at:new Date('2025-03-23') },
        ]);
        console.log('✅ Comments inserted (10+).');

        // ────────────────────────────────────────────
        // STORE PRODUCTS (giả lập sản phẩm từ siêu thị VN)
        // Tạo mạng lưới bán hàng: WinMart, Bách Hóa Xanh, Co.op Mart
        // ────────────────────────────────────────────
        const stores = [
            { name: 'WinMart',        logo: 'https://upload.wikimedia.org/wikipedia/commons/thumb/7/77/Winmart_logo.svg/1200px-Winmart_logo.svg.png' },
            { name: 'Bách Hóa Xanh', logo: 'https://upload.wikimedia.org/wikipedia/vi/a/a8/BachHoaXanh-Logo.png' },
            { name: 'Co.op Mart',     logo: 'https://upload.wikimedia.org/wikipedia/vi/7/7c/CoopMart_logo.png' },
            { name: 'Shopee Food',    logo: 'https://cf.shopee.vn/file/shopee_th-11134207-7racy-lvcbymm1bplq24' },
        ];

        const storeProducts = [
            // Thịt gà
            { ingredient_name:'Thịt gà', product_name:'Gà ta nguyên con (khoảng 1.2kg)', unit:'1 con', price_dong:89000, store_name:'WinMart', store_logo_url:stores[0].logo, image_url:'https://images.unsplash.com/photo-1587593810167-a84920ea0781?w=300', in_stock:true, rating:4.6 },
            { ingredient_name:'Thịt gà', product_name:'Thịt gà ợc phi lê 500g', unit:'500g', price_dong:65000, store_name:'Bách Hóa Xanh', store_logo_url:stores[1].logo, image_url:'https://images.unsplash.com/photo-1604503468506-a8da13d11d36?w=300', in_stock:true, rating:4.4 },
            { ingredient_name:'Thịt gà', product_name:'Cánh gà giữ nhiệt 500g (công ty Cầu Tre)', unit:'500g', price_dong:58000, store_name:'Co.op Mart', store_logo_url:stores[2].logo, image_url:'https://images.unsplash.com/photo-1559058789-672da06263d8?w=300', in_stock:true, rating:4.3 },

            // Sả
            { ingredient_name:'Sả', product_name:'Sả cây tươi (bó 200g)', unit:'200g', price_dong:9000, store_name:'Bách Hóa Xanh', store_logo_url:stores[1].logo, image_url:'https://images.unsplash.com/photo-1615485290382-441e4d049cb5?w=300', in_stock:true, rating:4.7 },
            { ingredient_name:'Sả', product_name:'Sả tươi rang cắ (hộp 100g)', unit:'100g', price_dong:12000, store_name:'WinMart', store_logo_url:stores[0].logo, image_url:'https://images.unsplash.com/photo-1615485290382-441e4d049cb5?w=300', in_stock:true, rating:4.5 },

            // Ờ
            { ingredient_name:'Ờ', product_name:'Ờ sừng tươi (100g)', unit:'100g', price_dong:8000, store_name:'WinMart', store_logo_url:stores[0].logo, image_url:'https://images.unsplash.com/photo-1588252303782-cb80119abd6d?w=300', in_stock:true, rating:4.5 },
            { ingredient_name:'Ờ', product_name:'Ờ hiểm khô nghiền (gói 50g)', unit:'50g', price_dong:15000, store_name:'Co.op Mart', store_logo_url:stores[2].logo, image_url:'https://images.unsplash.com/photo-1588252303782-cb80119abd6d?w=300', in_stock:true, rating:4.2 },

            // Đậu phụ
            { ingredient_name:'Đậu phụ', product_name:'Đậu hũ kính bạc Hà Giang (300g)', unit:'300g', price_dong:12000, store_name:'Bách Hóa Xanh', store_logo_url:stores[1].logo, image_url:'https://images.unsplash.com/photo-1546069901-d5bfd2cbfb1f?w=300', in_stock:true, rating:4.8 },
            { ingredient_name:'Đậu phụ', product_name:'Đậu hũ thượng hạng 400g', unit:'400g', price_dong:15000, store_name:'Co.op Mart', store_logo_url:stores[2].logo, image_url:'https://images.unsplash.com/photo-1546069901-d5bfd2cbfb1f?w=300', in_stock:true, rating:4.6 },

            // Cà chua
            { ingredient_name:'Cà chua', product_name:'Cà chua đà Lạt cộng 500g', unit:'500g', price_dong:18000, store_name:'WinMart', store_logo_url:stores[0].logo, image_url:'https://images.unsplash.com/photo-1592924357228-91a4daadcfea?w=300', in_stock:true, rating:4.9 },
            { ingredient_name:'Cà chua', product_name:'Cà chua bi vàng (300g)', unit:'300g', price_dong:22000, store_name:'Bách Hóa Xanh', store_logo_url:stores[1].logo, image_url:'https://images.unsplash.com/photo-1592924357228-91a4daadcfea?w=300', in_stock:true, rating:4.7 },

            // Hành tây
            { ingredient_name:'Hành tây', product_name:'Hành tây vàng (1 củ)', unit:'1 củ', price_dong:8000, store_name:'Bách Hóa Xanh', store_logo_url:stores[1].logo, image_url:'https://images.unsplash.com/photo-1508747703725-719777637510?w=300', in_stock:true, rating:4.4 },
            { ingredient_name:'Hành tây', product_name:'Hành tây tím 500g Đà Lạt', unit:'500g', price_dong:25000, store_name:'WinMart', store_logo_url:stores[0].logo, image_url:'https://images.unsplash.com/photo-1508747703725-719777637510?w=300', in_stock:true, rating:4.6 },

            // Trứng gà
            { ingredient_name:'Trứng gà', product_name:'Trứng gà sạch Ba Huàn (vỷ 10)', unit:'vỷ 10', price_dong:38000, store_name:'WinMart', store_logo_url:stores[0].logo, image_url:'https://images.unsplash.com/photo-1582722872445-44dc5f7e3c8f?w=300', in_stock:true, rating:4.9 },
            { ingredient_name:'Trứng gà', product_name:'Trứng gà ta đồng quê (6 trứng)', unit:'6 trứng', price_dong:32000, store_name:'Bách Hóa Xanh', store_logo_url:stores[1].logo, image_url:'https://images.unsplash.com/photo-1582722872445-44dc5f7e3c8f?w=300', in_stock:true, rating:4.8 },

            // Thịt bò
            { ingredient_name:'Thịt bò', product_name:'Thịt bò tân bì (500g)', unit:'500g', price_dong:145000, store_name:'WinMart', store_logo_url:stores[0].logo, image_url:'https://images.unsplash.com/photo-1603048297172-c92544798d5a?w=300', in_stock:true, rating:4.7 },
            { ingredient_name:'Thịt bò', product_name:'Thịt bò thăm thượng hạng (300g)', unit:'300g', price_dong:98000, store_name:'Co.op Mart', store_logo_url:stores[2].logo, image_url:'https://images.unsplash.com/photo-1603048297172-c92544798d5a?w=300', in_stock:true, rating:4.5 },

            // Bánh phở
            { ingredient_name:'Bánh phở', product_name:'Bánh phở tươi (500g)', unit:'500g', price_dong:18000, store_name:'Bách Hóa Xanh', store_logo_url:stores[1].logo, image_url:'https://images.unsplash.com/photo-1555126634-323283e090fa?w=300', in_stock:true, rating:4.6 },
            { ingredient_name:'Bánh phở', product_name:'Bánh phở khô Acecook (400g)', unit:'400g', price_dong:22000, store_name:'WinMart', store_logo_url:stores[0].logo, image_url:'https://images.unsplash.com/photo-1555126634-323283e090fa?w=300', in_stock:true, rating:4.4 },

            // Nước mắm
            { ingredient_name:'Nước mắm', product_name:'Nước mắm Phú Quốc 40° (500ml)', unit:'500ml', price_dong:45000, store_name:'WinMart', store_logo_url:stores[0].logo, image_url:'https://images.unsplash.com/photo-1601463073941-c5419e1ea8c3?w=300', in_stock:true, rating:4.9 },
            { ingredient_name:'Nước mắm', product_name:'Nước mắm Nam Ngư 1L', unit:'1L', price_dong:38000, store_name:'Bách Hóa Xanh', store_logo_url:stores[1].logo, image_url:'https://images.unsplash.com/photo-1601463073941-c5419e1ea8c3?w=300', in_stock:true, rating:4.7 },

            // Đường
            { ingredient_name:'Đường', product_name:'Đường tinh luyện Biên Hòa (1kg)', unit:'1kg', price_dong:28000, store_name:'Co.op Mart', store_logo_url:stores[2].logo, image_url:'https://images.unsplash.com/photo-1581400000816-4e8c6e41ca8d?w=300', in_stock:true, rating:4.8 },
            { ingredient_name:'Đường', product_name:'Đường nho organic 500g', unit:'500g', price_dong:45000, store_name:'WinMart', store_logo_url:stores[0].logo, image_url:'https://images.unsplash.com/photo-1581400000816-4e8c6e41ca8d?w=300', in_stock:true, rating:4.5 },

            // Tiêu
            { ingredient_name:'Tiêu', product_name:'Tiêu xay Phú Quốc (100g)', unit:'100g', price_dong:35000, store_name:'WinMart', store_logo_url:stores[0].logo, image_url:'https://images.unsplash.com/photo-1506368249639-73a05d6f6488?w=300', in_stock:true, rating:4.7 },
            { ingredient_name:'Tiêu', product_name:'Hạt tiêu đen nguyên hạt (50g)', unit:'50g', price_dong:22000, store_name:'Bách Hóa Xanh', store_logo_url:stores[1].logo, image_url:'https://images.unsplash.com/photo-1506368249639-73a05d6f6488?w=300', in_stock:true, rating:4.6 },

            // Tỏi
            { ingredient_name:'Tỏi', product_name:'Tỏi khô bóc vỏ sẵn (200g)', unit:'200g', price_dong:22000, store_name:'Bách Hóa Xanh', store_logo_url:stores[1].logo, image_url:'https://images.unsplash.com/photo-1540148426945-6cf22a6b2383?w=300', in_stock:true, rating:4.8 },
            { ingredient_name:'Tỏi', product_name:'Tỏi Đà Lạt nguyên củ (500g)', unit:'500g', price_dong:35000, store_name:'WinMart', store_logo_url:stores[0].logo, image_url:'https://images.unsplash.com/photo-1540148426945-6cf22a6b2383?w=300', in_stock:true, rating:4.7 },

            // Thịt heo
            { ingredient_name:'Thịt heo', product_name:'Thịt heo ba chỉ Vissan (500g)', unit:'500g', price_dong:75000, store_name:'WinMart', store_logo_url:stores[0].logo, image_url:'https://images.unsplash.com/photo-1602470520998-f4a52199a3d6?w=300', in_stock:true, rating:4.6 },
            { ingredient_name:'Thịt heo', product_name:'Sướn heo thượng hạng (500g)', unit:'500g', price_dong:89000, store_name:'Co.op Mart', store_logo_url:stores[2].logo, image_url:'https://images.unsplash.com/photo-1602470520998-f4a52199a3d6?w=300', in_stock:true, rating:4.5 },

            // Rau cải
            { ingredient_name:'Rau cải', product_name:'Cải xanh đà Lạt (300g)', unit:'300g', price_dong:12000, store_name:'Bách Hóa Xanh', store_logo_url:stores[1].logo, image_url:'https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=300', in_stock:true, rating:4.8 },
            { ingredient_name:'Rau cải', product_name:'Cải thìa sala organic (200g)', unit:'200g', price_dong:18000, store_name:'WinMart', store_logo_url:stores[0].logo, image_url:'https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=300', in_stock:true, rating:4.7 },

            // Khoai tây
            { ingredient_name:'Khoai tây', product_name:'Khoai tây đà Lạt 500g', unit:'500g', price_dong:18000, store_name:'Bách Hóa Xanh', store_logo_url:stores[1].logo, image_url:'https://images.unsplash.com/photo-1518977676601-b53f82aba655?w=300', in_stock:true, rating:4.6 },
            { ingredient_name:'Khoai tây', product_name:'Khoai tây sạch Mỹ (1kg)', unit:'1kg', price_dong:35000, store_name:'WinMart', store_logo_url:stores[0].logo, image_url:'https://images.unsplash.com/photo-1518977676601-b53f82aba655?w=300', in_stock:true, rating:4.5 },

            // Tôm
            { ingredient_name:'Tôm', product_name:'Tôm sú tươi (500g)', unit:'500g', price_dong:125000, store_name:'WinMart', store_logo_url:stores[0].logo, image_url:'https://images.unsplash.com/photo-1565680018434-b513d5e5fd47?w=300', in_stock:true, rating:4.8 },
            { ingredient_name:'Tôm', product_name:'Tôm thẻ biển đông (300g)', unit:'300g', price_dong:85000, store_name:'Co.op Mart', store_logo_url:stores[2].logo, image_url:'https://images.unsplash.com/photo-1565680018434-b513d5e5fd47?w=300', in_stock:true, rating:4.6 },

            // Bột mì
            { ingredient_name:'Bột mì', product_name:'Bột mì đa dụng Kim Nghĩa (1kg)', unit:'1kg', price_dong:28000, store_name:'WinMart', store_logo_url:stores[0].logo, image_url:'https://images.unsplash.com/photo-1578985545062-69928b1d9587?w=300', in_stock:true, rating:4.7 },
            { ingredient_name:'Bột mì', product_name:'Bột mì số 8 Meizan (2kg)', unit:'2kg', price_dong:52000, store_name:'Co.op Mart', store_logo_url:stores[2].logo, image_url:'https://images.unsplash.com/photo-1578985545062-69928b1d9587?w=300', in_stock:true, rating:4.6 },

            // Sữa tươi
            { ingredient_name:'Sữa tươi', product_name:'Sữa tươi tiệt trùng Vinamilk (1L)', unit:'1L', price_dong:32000, store_name:'Bách Hóa Xanh', store_logo_url:stores[1].logo, image_url:'https://images.unsplash.com/photo-1563636619-e9143da7973b?w=300', in_stock:true, rating:4.9 },
            { ingredient_name:'Sữa tươi', product_name:'Sữa tươi hữu cơ TH True Milk (500ml)', unit:'500ml', price_dong:28000, store_name:'WinMart', store_logo_url:stores[0].logo, image_url:'https://images.unsplash.com/photo-1563636619-e9143da7973b?w=300', in_stock:true, rating:4.8 },

            // Bún
            { ingredient_name:'Bún', product_name:'Bún tươi (500g)', unit:'500g', price_dong:15000, store_name:'Bách Hóa Xanh', store_logo_url:stores[1].logo, image_url:'https://images.unsplash.com/photo-1555126634-323283e090fa?w=300', in_stock:true, rating:4.5 },

            // Nấm
            { ingredient_name:'Nấm', product_name:'Nấm rơm tươi (300g)', unit:'300g', price_dong:28000, store_name:'WinMart', store_logo_url:stores[0].logo, image_url:'https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=300', in_stock:true, rating:4.6 },
            { ingredient_name:'Nấm', product_name:'Nấm đông cô khô (100g)', unit:'100g', price_dong:45000, store_name:'Co.op Mart', store_logo_url:stores[2].logo, image_url:'https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=300', in_stock:true, rating:4.7 },

            // Hành lá
            { ingredient_name:'Hành lá', product_name:'Hành lá tươi (bó 100g)', unit:'100g', price_dong:5000, store_name:'Bách Hóa Xanh', store_logo_url:stores[1].logo, image_url:'https://images.unsplash.com/photo-1540420773420-3366772f4999?w=300', in_stock:true, rating:4.8 },

            // Rau muống
            { ingredient_name:'Rau muống', product_name:'Rau muống đồng VietGAP (500g)', unit:'500g', price_dong:12000, store_name:'Bách Hóa Xanh', store_logo_url:stores[1].logo, image_url:'https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=300', in_stock:true, rating:4.7 },

            // Gừng
            { ingredient_name:'Gừng', product_name:'Gừng tươi (200g)', unit:'200g', price_dong:12000, store_name:'WinMart', store_logo_url:stores[0].logo, image_url:'https://images.unsplash.com/photo-1615485290382-441e4d049cb5?w=300', in_stock:true, rating:4.6 },

            // Bánh mì
            { ingredient_name:'Bánh mì', product_name:'Bánh mì Thần Tài (1 ổ)', unit:'1 ổ', price_dong:5000, store_name:'Bách Hóa Xanh', store_logo_url:stores[1].logo, image_url:'https://images.unsplash.com/photo-1509722747041-616f39b57569?w=300', in_stock:true, rating:4.9 },

            // Cà rốt
            { ingredient_name:'Cà rốt', product_name:'Cà rốt đà Lạt (500g)', unit:'500g', price_dong:15000, store_name:'WinMart', store_logo_url:stores[0].logo, image_url:'https://images.unsplash.com/photo-1598170845058-32b9d6a5da37?w=300', in_stock:true, rating:4.8 },
        ];

        await StoreProduct.bulkCreate(storeProducts);
        console.log(`✅ Store products inserted (${storeProducts.length} sản phẩm từ WinMart, Bách Hóa Xanh, Co.op Mart).`);

        console.log('\n🎉 All seed data inserted successfully!');
        process.exit(0);
    } catch (err) {
        console.error('❌ Seed failed:', err);
        process.exit(1);
    }
}

seedDatabase();

// Cập nhật