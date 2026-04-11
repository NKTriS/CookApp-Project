package com.example.cookapp.utils.image;

import java.util.HashMap;
import java.util.Map;

/**
 * Map ảnh công thức đã kiểm duyệt.
 *
 * "local:recipe_xxx" = file PNG/JPG bundled trong APK tại drawable-nodpi/
 * URL remote          = fallback tải qua mạng khi file local chưa có
 */
public class CuratedRecipeImages {

    private static final Map<String, String> IMAGE_MAP = new HashMap<>();

    static {
        // ID 1 – Gà xào sả ớt
        IMAGE_MAP.put("ga xao sa ot", "local:recipe_ga_xao_sa_ot");

        // ID 2 – Đậu phụ sốt cà chua
        IMAGE_MAP.put("dau phu sot ca chua", "local:recipe_dau_phu_sot_ca_chua");

        // ID 3 – Bún bò Huế
        IMAGE_MAP.put("bun bo hue", "local:recipe_bun_bo_hue");

        // ID 4 – Phở Bò truyền thống
        IMAGE_MAP.put("pho bo truyen thong", "local:recipe_pho_bo_truyen_thong");

        // ID 5 – Bánh mì trứng ốp la
        IMAGE_MAP.put("banh mi trung op la", "local:recipe_banh_mi_trung_op_la");

        // ID 6 – Canh rau cải nấu thịt heo
        IMAGE_MAP.put("canh rau cai nau thit heo", "local:recipe_canh_rau_cai_thit_heo");

        // ID 7 – Cơm rang dưa bò
        IMAGE_MAP.put("com rang dua bo", "local:recipe_com_rang_dua_bo");

        // ID 8 – Tôm xào bông cải
        IMAGE_MAP.put("tom xao bong cai", "local:recipe_tom_xao_bong_cai");

        // ID 9 – Súp bí đỏ kem
        IMAGE_MAP.put("sup bi do kem", "local:recipe_sup_bi_do_kem");

        // ID 10 – Gỏi cuốn tôm thịt
        IMAGE_MAP.put("goi cuon tom thit", "local:recipe_goi_cuon_tom_thit");

        // ID 11 – Chả giò (Nem rán)
        IMAGE_MAP.put("cha gio (nem ran)",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/a/ae/Fried_spring_rolls.jpg/640px-Fried_spring_rolls.jpg");

        // ID 12 – Bánh flan caramel
        IMAGE_MAP.put("banh flan caramel", "local:recipe_banh_flan_caramel");

        // ID 13 – Gà kho gừng
        IMAGE_MAP.put("ga kho gung", "local:recipe_ga_kho_gung");

        // ID 14 – Cà ri gà nước cốt dừa
        IMAGE_MAP.put("ca ri ga nuoc cot dua", "local:recipe_ca_ri_ga_nuoc_cot_dua");

        // ID 15 – Mì Ý Bolognese Dễ Làm
        IMAGE_MAP.put("mi y bolognese de lam",     "local:recipe_mi_y_bolognese");

        // ID 16 – Salad Cá Hồi Áp Chảo
        IMAGE_MAP.put("salad ca hoi ap chao", "local:recipe_salad_ca_hoi_ap_chao");

        // ID 17 – Bánh xèo miền Tây
        IMAGE_MAP.put("banh xeo mien tay", "local:recipe_banh_xeo_mien_tay");

        // ID 18 – Lẩu Thái chua cay
        IMAGE_MAP.put("lau thai chua cay", "local:recipe_lau_thai_chua_cay");

        // ID 19 – Vịt Nấu Chao
        IMAGE_MAP.put("vit nau chao", "local:recipe_vit_nau_chao");

        // ID 20 – Cháo gà đậu xanh
        IMAGE_MAP.put("chao ga dau xanh", "local:recipe_chao_ga_dau_xanh");

        // ID 21 – Xôi lá dứa cốt dừa
        IMAGE_MAP.put("xoi la dua cot dua", "local:recipe_xoi_la_dua_cot_dua");

        // ID 22 – Chè bưởi sầu riêng
        IMAGE_MAP.put("che buoi sau rieng", "local:recipe_che_buoi_sau_rieng");

        // ID 23 – Cơm chiên Dương Châu
        IMAGE_MAP.put("com chien duong chau", "local:recipe_com_chien_duong_chau");

        // ID 24 – Thịt heo kho trứng cuốc
        IMAGE_MAP.put("thit heo kho trung cuoc",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/9/94/Th%E1%BB%8Bt_kho_tàu.jpg/640px-Th%E1%BB%8Bt_kho_tàu.jpg");

        // ID 25 – Sườn heo nướng mật ong
        IMAGE_MAP.put("suon heo nuong mat ong", "local:recipe_suon_heo_nuong_mat_ong");

        // ID 26 – Rau câu cốt dừa
        IMAGE_MAP.put("rau cau cot dua", "local:recipe_rau_cau_cot_dua");

        // ID 27 – Sinh tố Bơ truyền thống
        IMAGE_MAP.put("sinh to bo truyen thong",   "local:recipe_sinh_to_bo");

        // ID 28 – Trà sữa trân châu đường đen
        IMAGE_MAP.put("tra sua tran chau duong den",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/1/1a/Bubble_Tea_%28Boba%29_in_clear_cup.jpg/640px-Bubble_Tea_%28Boba%29_in_clear_cup.jpg");

        // ID 29 – Bò lúc lắc chảo gang
        IMAGE_MAP.put("bo luc lac chao gang",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/7/75/Bo_luc_lac.jpg/640px-Bo_luc_lac.jpg");

        // ID 30 – Miến Măng Gà Rừng
        IMAGE_MAP.put("mien mang ga rung", "local:recipe_mien_mang_ga_rung");
    }

    /** Returns "local:res_name", URL, or null if not curated. */
    public static String getUrl(String normalizedTitle) {
        return IMAGE_MAP.get(normalizedTitle);
    }
}
