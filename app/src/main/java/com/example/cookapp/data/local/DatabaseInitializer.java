package com.example.cookapp.data.local;

import android.os.AsyncTask;

import com.example.cookapp.data.local.entity.*;

import java.util.Arrays;
import java.util.List;

/**
 * Nạp toàn bộ dữ liệu mẫu vào Room Database.
 * populateSync() được gọi từ RecipeListActivity background thread.
 */
public class DatabaseInitializer {

        public static void populateAsync(final AppDatabase db) {
                new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... voids) {
                                populateDB(db);
                                return null;
                        }
                }.execute();
        }

        /** Gọi trực tiếp từ background thread — KHÔNG gọi trên Main Thread */
        public static void populateSync(final AppDatabase db) {
                populateDB(db);
        }

        private static void populateDB(final AppDatabase db) {
                synchronized (AppDatabase.class) {
                        // Seed lại khi DB trống hoặc thiếu dữ liệu (version bump sẽ xóa toàn bộ)
                        if (db.recipeDao().countRecipes() >= 30)
                                return;

                        // Xóa sạch toàn bộ dữ liệu cũ ở TẤT CẢ bảng trước khi seed lại
                        db.recipeDao().deleteAllRecipes();
                        db.recipeDao().deleteAllSteps();
                        db.recipeDao().deleteAllRecipeIngredients();
                        db.nutritionDao().deleteAll();
                        db.favoriteReviewDao().deleteAllFavorites();
                        db.favoriteReviewDao().deleteAllReviews();
                        db.communityDao().deleteAllPosts();
                        db.communityDao().deleteAllComments();
                        db.communityDao().deleteAllLikes();
                        db.shoppingDao().deleteAll();
                        // Since we bumped version destructively, we don't need to deleteAllUsers/Orders
                        // explicitly.

                        db.runInTransaction(() -> {

                                // ================================================================
                                // 0. USERS (Seed Data)
                                // ================================================================
                                UserEntity u1 = new UserEntity();
                                u1.email = "demo@cookapp.com";
                                u1.password = "123456";
                                u1.fullName = "Phạm Minh Tâm";
                                u1.address = "02 Phan Châu Trinh, Hưng Yên";
                                u1.phoneNumber = "0391234567";
                                u1.createdAt = System.currentTimeMillis();
                                db.userDao().insertUser(u1);

                                UserEntity u2 = new UserEntity();
                                u2.email = "test@cookapp.com";
                                u2.password = "123456";
                                u2.fullName = "Khắc Trí";
                                u2.address = "Đà Nẵng";
                                u2.phoneNumber = "0987654321";
                                u2.createdAt = System.currentTimeMillis();
                                db.userDao().insertUser(u2);
                                // 1. CATEGORIES
                                // ================================================================
                                insertCat(db, 1, "Gia đình", SERVER_BASE + "/images/categories/gia_dinh.jpg");
                                insertCat(db, 2, "Lành mạnh", SERVER_BASE + "/images/categories/lanh_manh.jpg");
                                insertCat(db, 3, "Tráng miệng", SERVER_BASE + "/images/categories/trangmieng.jpg");
                                insertCat(db, 4, "Bánh", SERVER_BASE + "/images/categories/banh.jpg");
                                insertCat(db, 5, "Nhanh & dễ", SERVER_BASE + "/images/categories/nhanh_va_de.jpg");
                                insertCat(db, 6, "Đặc sản", SERVER_BASE + "/images/categories/dac_san_vung_mien.jpg");
                                insertCat(db, 7, "Chay", SERVER_BASE + "/images/categories/chay.jpg");
                                insertCat(db, 8, "Món nướng", SERVER_BASE + "/images/categories/nuong.jpg");

                                // ================================================================
                                // 2. DIET TYPES
                                // ================================================================
                                insertDiet(db, 1, "Thông thường");
                                insertDiet(db, 2, "Ăn chay");
                                insertDiet(db, 3, "Keto");
                                insertDiet(db, 4, "Low-carb");
                                insertDiet(db, 5, "Gluten-free");

                                // ================================================================
                                // 3. INGREDIENTS (60 nguyên liệu – ảnh + đơn vị + giá)
                                // ================================================================
                                insertIng(db, 1, "Thịt gà",
                                                "https://dochienxienque.com/wp-content/uploads/2021/02/Dui-Ga-Chien-Xu-1536x1081.jpg",
                                                "500g", 45000);
                                insertIng(db, 2, "Sả",
                                                "https://posapp.vn/wp-content/uploads/2022/06/menu-nha-hang-HCN.jpg",
                                                "100g", 8000);
                                insertIng(db, 3, "Ớt",
                                                "https://posapp.vn/wp-content/uploads/2022/06/menu-nha-hang-HCN.jpg",
                                                "100g", 5000);
                                insertIng(db, 4, "Đậu phụ",
                                                "https://posapp.vn/wp-content/uploads/2022/06/menu-nha-hang-HCN.jpg",
                                                "300g", 12000);
                                insertIng(db, 5, "Cà chua",
                                                "https://tunaucom123.com.vn/wp-content/uploads/2022/12/do-chua-an-com-tam-1.jpg",
                                                "500g", 15000);
                                insertIng(db, 6, "Hành tây",
                                                "https://posapp.vn/wp-content/uploads/2022/06/menu-nha-hang-HCN.jpg",
                                                "1 củ", 5000);
                                insertIng(db, 7, "Trứng gà",
                                                "https://posapp.vn/wp-content/uploads/2022/06/menu-nha-hang-HCN.jpg",
                                                "10 quả", 30000);
                                insertIng(db, 8, "Thịt bò",
                                                "https://thuthuatnhanh.com/wp-content/uploads/2022/09/hinh-nen-powerpoint-chu-de-mon-an-dep-don-gian.jpeg",
                                                "500g", 120000);
                                insertIng(db, 9, "Bánh phở",
                                                "https://posapp.vn/wp-content/uploads/2022/06/menu-nha-hang-HCN.jpg",
                                                "500g", 18000);
                                insertIng(db, 10, "Nước mắm",
                                                "https://posapp.vn/wp-content/uploads/2022/06/menu-nha-hang-HCN.jpg",
                                                "1 chai", 25000);
                                insertIng(db, 11, "Đường",
                                                "https://posapp.vn/wp-content/uploads/2022/06/menu-nha-hang-HCN.jpg",
                                                "1kg", 22000);
                                insertIng(db, 12, "Tiêu",
                                                "https://posapp.vn/wp-content/uploads/2022/06/menu-nha-hang-HCN.jpg",
                                                "50g", 15000);
                                insertIng(db, 13, "Tỏi",
                                                "https://posapp.vn/wp-content/uploads/2022/06/menu-nha-hang-HCN.jpg",
                                                "100g", 8000);
                                insertIng(db, 14, "Gừng",
                                                "https://posapp.vn/wp-content/uploads/2022/06/menu-nha-hang-HCN.jpg",
                                                "100g", 7000);
                                insertIng(db, 15, "Bột chiên",
                                                "https://didongviet.vn/dchannel/wp-content/uploads/2022/12/mon-an-ngay-tet-didongviet.jpg",
                                                "500g", 18000);
                                insertIng(db, 16, "Sữa tươi",
                                                "https://posapp.vn/wp-content/uploads/2022/06/menu-nha-hang-HCN.jpg",
                                                "1L", 32000);
                                insertIng(db, 17, "Thịt heo",
                                                "http://pvhttnt.vn/wp-content/uploads/2022/11/banh-trang-cuon-thit-heo-da-nang-ivivu-5.jpg",
                                                "500g", 75000);
                                insertIng(db, 18, "Rau cải",
                                                "https://noipho.vn/wp-content/uploads/2022/09/nhung-loai-rau-an-pho-nhat-dinh-phai-co-7.jpg",
                                                "300g", 12000);
                                insertIng(db, 19, "Nước dừa",
                                                "https://posapp.vn/wp-content/uploads/2022/06/menu-nha-hang-HCN.jpg",
                                                "1 trái", 15000);
                                insertIng(db, 20, "Bánh mì",
                                                "https://png.pngtree.com/background/20250205/original/pngtree-cheese-burger-background-hd-images-food-photography-picture-image_15312195.jpg",
                                                "1 ổ", 6000);
                                insertIng(db, 21, "Khoai tây",
                                                "https://product.hstatic.net/200000582249/product/khoai_tay_chien_9572ca98e77744ae9d608e7350096be2_master.jpg",
                                                "500g", 18000);
                                insertIng(db, 22, "Cà rốt",
                                                "https://posapp.vn/wp-content/uploads/2022/06/menu-nha-hang-HCN.jpg",
                                                "500g", 12000);
                                insertIng(db, 23, "Nấm hương",
                                                "https://thuthuatnhanh.com/wp-content/uploads/2022/09/hinh-nen-powerpoint-chu-de-mon-an-dep-don-gian.jpeg",
                                                "200g", 25000);
                                insertIng(db, 24, "Tôm sú",
                                                "https://mekongreststop.com/images/stories/virtuemart/product/37---tom-su-ram-man.jpg",
                                                "500g", 150000);
                                insertIng(db, 25, "Cua biển",
                                                "https://cdn2.fptshop.com.vn/unsafe/Uploads/images/tin-tuc/164296/Originals/hap-cua-bao-nhieu-phut-2.JPG",
                                                "1 con", 90000);
                                insertIng(db, 26, "Bún tươi",
                                                "https://posapp.vn/wp-content/uploads/2022/06/menu-nha-hang-HCN.jpg",
                                                "500g", 15000);
                                insertIng(db, 27, "Gạo tẻ",
                                                "https://posapp.vn/wp-content/uploads/2022/06/menu-nha-hang-HCN.jpg",
                                                "1kg", 25000);
                                insertIng(db, 28, "Dầu hào",
                                                "https://posapp.vn/wp-content/uploads/2022/06/menu-nha-hang-HCN.jpg",
                                                "200ml", 20000);
                                insertIng(db, 29, "Hành lá",
                                                "https://posapp.vn/wp-content/uploads/2022/06/menu-nha-hang-HCN.jpg",
                                                "100g", 5000);
                                insertIng(db, 30, "Rau muống",
                                                "https://rauxanh.net/wp-content/uploads/2019/05/rau-muong.png", "500g",
                                                10000);
                                insertIng(db, 31, "Cá hồi",
                                                "https://posapp.vn/wp-content/uploads/2022/06/menu-nha-hang-HCN.jpg",
                                                "500g", 200000);
                                insertIng(db, 32, "Cá lóc",
                                                "https://posapp.vn/wp-content/uploads/2022/06/menu-nha-hang-HCN.jpg",
                                                "1 con", 55000);
                                insertIng(db, 33, "Mực ống",
                                                "https://posapp.vn/wp-content/uploads/2022/06/menu-nha-hang-HCN.jpg",
                                                "500g", 90000);
                                insertIng(db, 34, "Bơ lạt",
                                                "https://posapp.vn/wp-content/uploads/2022/06/menu-nha-hang-HCN.jpg",
                                                "250g", 45000);
                                insertIng(db, 35, "Kem tươi",
                                                "https://mixuediemdien.com/wp-content/uploads/2023/07/menu-mixue.jpg",
                                                "200ml", 35000);
                                insertIng(db, 36, "Phô mai",
                                                "https://hinhnen4k.com/wp-content/uploads/2023/03/pho-mai-que-cute-1.jpg",
                                                "200g", 55000);
                                insertIng(db, 37, "Thịt bò băm",
                                                "https://cdn.pixabay.com/photo/2020/05/28/08/49/soup-5230475_1280.jpg",
                                                "500g", 110000);
                                insertIng(db, 38, "Gân bò",
                                                "http://learningvietnamese.edu.vn/wp-content/uploads/2022/10/vietnamese-food-vocabulary-bun-bo-1.jpg",
                                                "500g", 70000);
                                insertIng(db, 39, "Sườn non", "https://i.ytimg.com/vi/cJu6tFJe_Gc/maxresdefault.jpg",
                                                "500g", 95000);
                                insertIng(db, 40, "Dưa hấu",
                                                "https://png.pngtree.com/background/20220723/original/pngtree-watermelon-fruit-hd-photography-material-picture-image_1736617.jpg",
                                                "1 trái", 45000);
                                insertIng(db, 41, "Hạt sen",
                                                "https://posapp.vn/wp-content/uploads/2022/06/menu-nha-hang-HCN.jpg",
                                                "200g", 40000);
                                insertIng(db, 42, "Đậu xanh",
                                                "https://tiki.vn/blog/wp-content/uploads/2023/04/cach-nau-xoi-dau-xanh.jpg",
                                                "500g", 25000);
                                insertIng(db, 43, "Đậu đỏ",
                                                "https://posapp.vn/wp-content/uploads/2022/06/menu-nha-hang-HCN.jpg",
                                                "500g", 28000);
                                insertIng(db, 44, "Gạo nếp",
                                                "https://posapp.vn/wp-content/uploads/2022/06/menu-nha-hang-HCN.jpg",
                                                "1kg", 32000);
                                insertIng(db, 45, "Lá dứa",
                                                "https://posapp.vn/wp-content/uploads/2022/06/menu-nha-hang-HCN.jpg",
                                                "100g", 5000);
                                insertIng(db, 46, "Nước cốt dừa",
                                                "https://posapp.vn/wp-content/uploads/2022/06/menu-nha-hang-HCN.jpg",
                                                "400ml", 20000);
                                insertIng(db, 47, "Lá chanh",
                                                "https://chanhthai.vn/wp-content/uploads/2023/08/mon-an-tu-la-chanh-say-kho.png",
                                                "50g", 5000);
                                insertIng(db, 48, "Bơ sáp",
                                                "https://posapp.vn/wp-content/uploads/2022/06/menu-nha-hang-HCN.jpg",
                                                "2 trái", 30000);
                                insertIng(db, 49, "Chuối",
                                                "https://posapp.vn/wp-content/uploads/2022/06/menu-nha-hang-HCN.jpg",
                                                "1 nải", 20000);
                                insertIng(db, 50, "Cà phê đen",
                                                "https://posapp.vn/wp-content/uploads/2022/06/menu-nha-hang-HCN.jpg",
                                                "100g", 50000);
                                insertIng(db, 51, "Rượu vang đỏ",
                                                "https://thekeywine.vn/wp-content/uploads/2022/08/do-nguoi-uong-ruou-vang.jpg",
                                                "1 chai", 120000);
                                insertIng(db, 52, "Mật ong",
                                                "https://www.huongnghiepaau.com/wp-content/uploads/2018/06/ga-nuong-mat-ong.jpg",
                                                "500ml", 95000);
                                insertIng(db, 53, "Mayonnaise",
                                                "https://png.pngtree.com/png-vector/20240818/ourlarge/pngtree-delicious-mayonnaise-perfecting-your-sandwiches-and-salads-png-image_13533638.png",
                                                "250ml", 35000);
                                insertIng(db, 54, "Mù tạt",
                                                "https://didongviet.vn/dchannel/wp-content/uploads/2022/12/mon-an-ngay-tet-didongviet.jpg",
                                                "100ml", 22000);
                                insertIng(db, 55, "Bột mì",
                                                "https://thuthuatnhanh.com/wp-content/uploads/2022/09/hinh-nen-powerpoint-chu-de-mon-an-dep-don-gian.jpeg",
                                                "1kg", 18000);
                                insertIng(db, 56, "Bột năng",
                                                "https://posapp.vn/wp-content/uploads/2022/06/menu-nha-hang-HCN.jpg",
                                                "500g", 15000);
                                insertIng(db, 57, "Thịt vịt",
                                                "https://thuthuatnhanh.com/wp-content/uploads/2022/09/hinh-nen-powerpoint-chu-de-mon-an-dep-don-gian.jpeg",
                                                "1 con", 120000);
                                insertIng(db, 58, "Cá chép",
                                                "https://posapp.vn/wp-content/uploads/2022/06/menu-nha-hang-HCN.jpg",
                                                "1 con", 60000);
                                insertIng(db, 59, "Mộc nhĩ",
                                                "https://posapp.vn/wp-content/uploads/2022/06/menu-nha-hang-HCN.jpg",
                                                "100g", 12000);
                                insertIng(db, 60, "Bột nghệ",
                                                "https://posapp.vn/wp-content/uploads/2022/06/menu-nha-hang-HCN.jpg",
                                                "100g", 15000);

                                // ================================================================
                        });
                }
        }

        // ================================================================
        // Helper methods
        private static void insertCat(AppDatabase db, int id, String name, String imageUrl) {
                CategoryEntity e = new CategoryEntity();
                e.id = id;
                e.name = name;
                e.imageUrl = imageUrl;
                db.recipeDao().insertCategory(e);
        }

        private static void insertDiet(AppDatabase db, int id, String name) {
                /* unused directly */ }

        private static void insertIng(AppDatabase db, int id, String name,
                        String imageUrl, String unit, int priceDong) {
                IngredientEntity e = new IngredientEntity();
                e.id = id;
                e.name = name;
                e.unit = unit;
                e.priceDong = priceDong;

                // --- Semantic Resolving ---
                String resolvedUrl = com.example.cookapp.utils.image.ImageResolver.resolveIngredientImage(name);
                e.normalizedName = com.example.cookapp.utils.image.StringUtils.normalizeName(name);

                if (resolvedUrl != null) {
                        e.imageUrl = resolvedUrl;
                        e.imageVerified = true;
                        e.imageSource = "curated_static";
                } else {
                        e.imageUrl = null; // Forces Glide to use placeholder xml
                        e.imageVerified = false;
                        e.imageSource = "unverified_fallback";
                }

                db.recipeDao().insertIngredient(e);
        }

        // Map video cho từng recipe id
        // Recipes 1-12: Video MP4 local (served by backend Express static)
        // Recipes 13-30: YouTube (đã xác nhận tồn tại)
        private static final String SERVER_BASE = com.example.cookapp.utils.NetworkConfig.getBaseUrl();
        private static final String[][] VIDEO_DATA = {
                        // {id, videoUrl}
                        // ── 12 món có video MP4 local ──
                        { "1", SERVER_BASE + "/videos/ga_xao_xa_ot.mp4" }, // Gà xào sả ớt
                        { "2", SERVER_BASE + "/videos/dau_phu_sot_ca_chua.mp4" }, // Đậu phụ sốt cà chua
                        { "3", SERVER_BASE + "/videos/bun_bo_hue.mp4" }, // Bún bò Huế
                        { "4", SERVER_BASE + "/videos/pho_bo_truyen_thong.mp4" }, // Phở Bò truyền thống
                        { "5", SERVER_BASE + "/videos/banh_mi_trung_op_la.mp4" }, // Bánh mì trứng ốp la
                        { "6", SERVER_BASE + "/videos/canh_rau_nau_thit_heo.mp4" }, // Canh rau cải nấu thịt heo
                        { "7", SERVER_BASE + "/videos/com_rang_dua_bo.mp4" }, // Cơm rang dưa bò
                        { "8", SERVER_BASE + "/videos/tom_xao_bong_cai.mp4" }, // Tôm xào bông cải
                        { "9", SERVER_BASE + "/videos/sup_bi_do_kem.mp4" }, // Súp bí đỏ kem
                        { "10", SERVER_BASE + "/videos/goi_cuon_tom_thit.mp4" }, // Gỏi cuốn tôm thịt
                        { "11", SERVER_BASE + "/videos/cha_gio.mp4" }, // Chả giò (Nem rán)
                        { "12", SERVER_BASE + "/videos/banh_flan.mp4" }, // Bánh flan caramel

        };

        private static String getVideoUrl(int recipeId) {
                for (String[] row : VIDEO_DATA) {
                        if (Integer.parseInt(row[0]) == recipeId)
                                return row[1];
                }
                // fallback: video nấu ăn Việt Nam chung trên YouTube
                return "https://www.youtube.com/watch?v=3T9-8dlDkQc";
        }

        /** Kiểm tra URL có phải YouTube không */
        private static boolean isYouTubeUrlStatic(String url) {
                return url != null && (url.contains("youtube.com") || url.contains("youtu.be"));
        }

        /** Trích xuất YouTube Video ID */
        private static String extractYtId(String url) {
                if (url == null)
                        return "";
                if (url.contains("v=")) {
                        String id = url.substring(url.indexOf("v=") + 2);
                        if (id.contains("&"))
                                id = id.substring(0, id.indexOf("&"));
                        return id;
                }
                if (url.contains("youtu.be/")) {
                        String id = url.substring(url.indexOf("youtu.be/") + 9);
                        if (id.contains("?"))
                                id = id.substring(0, id.indexOf("?"));
                        return id;
                }
                return url;
        }

        private static RecipeEntity makeR(int id, String title, String desc, String img,
                        int time, String diff, int serv, int cal, int catId, int dietId,
                        String videoUrl) {
                RecipeEntity e = new RecipeEntity();
                e.id = id;
                e.title = title;
                e.description = desc;
                e.cook_time = time;
                e.difficulty = diff;
                e.servings = serv;
                e.calories = cal;
                e.category_id = catId;
                e.diet_type_id = dietId;

                // Gán video URL — hỗ trợ cả MP4 local và YouTube
                String resolvedVideoUrl = getVideoUrl(id);
                e.video_url = resolvedVideoUrl;
                e.video_title = "Hướng dẫn nấu: " + title;

                if (isYouTubeUrlStatic(resolvedVideoUrl)) {
                        // YouTube → lấy thumbnail từ YouTube
                        String ytId = extractYtId(resolvedVideoUrl);
                        e.video_thumbnail_url = "https://img.youtube.com/vi/" + ytId + "/hqdefault.jpg";
                } else {
                        // MP4 local → dùng ảnh thumbnail trích xuất từ video
                        // URL dạng: .../videos/xxx.mp4 → .../videos/thumbnails/xxx.jpg
                        String mp4Name = resolvedVideoUrl.substring(resolvedVideoUrl.lastIndexOf('/') + 1);
                        String jpgName = mp4Name.replace(".mp4", ".jpg");
                        e.video_thumbnail_url = SERVER_BASE + "/videos/thumbnails/" + jpgName;
                }

                // --- Phase 1: Diet & Allergy Filters ---
                // dietId 1: Mặn, 2: Chay, 3: Keto, 4: Eat Clean...
                e.isVegetarian = (dietId == 2) || title.toLowerCase().contains("chay")
                                || title.toLowerCase().contains("đậu phụ");
                e.isKeto = (dietId == 3) || title.toLowerCase().contains("bò") || title.toLowerCase().contains("heo"); // Just
                                                                                                                       // mockup
                e.isLowCarb = e.isKeto;
                e.isEatClean = (dietId == 4) || title.toLowerCase().contains("rau")
                                || title.toLowerCase().contains("salad");

                // Allergy Mockup
                e.isDairyFree = !title.toLowerCase().contains("sữa") && !title.toLowerCase().contains("kem")
                                && !title.toLowerCase().contains("phô mai");
                e.isGlutenFree = !title.toLowerCase().contains("mì") && !title.toLowerCase().contains("bánh");
                e.isSeafoodFree = !title.toLowerCase().contains("tôm") && !title.toLowerCase().contains("cua")
                                && !title.toLowerCase().contains("cá");
                e.isPeanutFree = true; // Assume all are peanut free for now unless modified

                // --- Semantic Resolving ---
                e.normalizedName = com.example.cookapp.utils.image.StringUtils.normalizeName(title);

                // Nếu có video MP4 local → dùng ảnh thumbnail từ video làm ảnh đại diện
                if (!isYouTubeUrlStatic(resolvedVideoUrl) && e.video_thumbnail_url != null) {
                        e.image_url = e.video_thumbnail_url;
                        e.imageVerified = true;
                        e.imageSource = "video_thumbnail";
                } else {
                        String resolvedUrl = com.example.cookapp.utils.image.ImageResolver.resolveRecipeImage(title);
                        if (resolvedUrl != null) {
                                e.image_url = resolvedUrl;
                                e.imageVerified = true;
                                e.imageSource = "curated_static";
                        } else {
                                e.image_url = null;
                                e.imageVerified = false;
                                e.imageSource = "unverified_fallback";
                        }
                }

                return e;
        }

        private static void insertRecipe(AppDatabase db, int id, String title, String desc, String img,
                        int time, String diff, int serv, int cal, int catId, int dietId,
                        String videoUrl) {
                db.recipeDao().insertRecipe(
                                makeR(id, title, desc, img, time, diff, serv, cal, catId, dietId, videoUrl));
        }

        private static void insertStep(AppDatabase db, int rid, int num, String inst, int timer) {
                insertStepFull(db, rid, num, null, inst, timer, 0);
        }

        private static void insertStepFull(AppDatabase db, int rid, int num, String title, String inst, int timer,
                        int videoStartTime) {
                RecipeStepEntity e = new RecipeStepEntity();
                e.recipe_id = rid;
                e.step_number = num;
                e.title = title;
                e.instruction = inst;
                e.timer_seconds = timer;
                e.video_start_time = videoStartTime;
                db.recipeDao().insertStep(e);
        }

        private static void insertRI(AppDatabase db, int rid, int iid, float qty, String unit) {
                RecipeIngredientEntity e = new RecipeIngredientEntity();
                e.recipe_id = rid;
                e.ingredient_id = iid;
                e.quantity = qty;
                e.unit = unit;
                db.recipeDao().insertRecipeIngredient(e);
        }

        private static void insertNut(AppDatabase db, int rid, int cal, float prot, float fat,
                        float carbs, float fiber, float sugar, float sodium) {
                NutritionFactEntity e = new NutritionFactEntity();
                e.recipe_id = rid;
                e.calories = cal;
                e.protein = prot;
                e.fat = fat;
                e.carbs = carbs;
                e.fiber = fiber;
                e.sugar = sugar;
                e.sodium = sodium;
                db.nutritionDao().insertNutritionFact(e);
        }

        private static void insertRev(AppDatabase db, int rid, int rating, String comment) {
                ReviewEntity e = new ReviewEntity();
                e.recipe_id = rid;
                e.rating = rating;
                e.comment = comment;
                e.user_id = 1;
                db.favoriteReviewDao().insertReview(e);
        }

        private static void insertFav(AppDatabase db, int rid, int uid) {
                FavoriteEntity e = new FavoriteEntity();
                e.recipe_id = rid;
                e.user_id = uid;
                db.favoriteReviewDao().addFavorite(e);
        }

        private static ShoppingListItemEntity makeShopItem(int listId, String name, float qty, String unit,
                        boolean checked) {
                ShoppingListItemEntity e = new ShoppingListItemEntity();
                e.shopping_list_id = listId;
                e.ingredient_name = name;
                e.quantity = qty;
                e.unit = unit;
                e.checked = checked;
                return e;
        }

        private static long insertPost(AppDatabase db, String title, String content, String author, int uid, int likes,
                        String imageUri, long timeDiffMs) {
                PostEntity e = new PostEntity();
                e.title = title;
                e.content = content;
                e.author = author;
                e.user_id = uid;
                e.likes = likes;
                e.created_at = System.currentTimeMillis() - timeDiffMs;
                e.image_uri = imageUri;
                return db.communityDao().insertPost(e);
        }

        private static void insertCmt(AppDatabase db, int pid, String author, String content) {
                PostCommentEntity e = new PostCommentEntity();
                e.post_id = pid;
                e.author = author;
                e.content = content;
                e.user_id = 1;
                e.created_at = System.currentTimeMillis();
                db.communityDao().insertComment(e);
        }

        private static void insertNotif(AppDatabase db, int userId, String message, String type, long timeDiffMs) {
                NotificationEntity e = new NotificationEntity();
                e.user_id = userId;
                e.message = message;
                e.type = type;
                e.isRead = false;
                e.created_at = System.currentTimeMillis() - timeDiffMs;
                db.notificationDao().insertNotification(e);
        }
}
