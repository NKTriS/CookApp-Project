package com.example.cookapp.utils.image;

import java.util.HashMap;
import java.util.Map;

/**
 * Map ảnh nguyên liệu đã kiểm duyệt.
 *
 * Ưu tiên: "local:ing_xxx" → file drawable trong APK (100% đúng, offline)
 * Fallback: URL remote      → load qua mạng (Unsplash / Wikimedia)
 * Không có entry            → ImageResolver trả null → Glide hiện placeholder
 */
public class CuratedIngredientImages {

    private static final Map<String, String> IMAGE_MAP = new HashMap<>();

    static {
        // ── Meats ────────────────────────────────────────────────────────────
        // "local:ing_thit_ga" = đã tải về drawable-nodpi/ing_thit_ga.jpg ✅
        IMAGE_MAP.put("thit ga", "local:ing_thit_ga");
        IMAGE_MAP.put("thit heo", "local:ing_thit_heo");
        IMAGE_MAP.put("thit bo", "local:ing_thit_bo");
        IMAGE_MAP.put("thit bo bam", "local:ing_thit_bo_bam");
        IMAGE_MAP.put("gan bo", "local:ing_gan_bo");
        IMAGE_MAP.put("suon non", "local:ing_suon_non");
        IMAGE_MAP.put("thit vit", "local:ing_thit_vit");

        // ── Seafood ──────────────────────────────────────────────────────────
        IMAGE_MAP.put("tom su", "local:ing_tom_su");
        IMAGE_MAP.put("cua bien", "local:ing_cua_bien");
        IMAGE_MAP.put("ca hoi", "local:ing_ca_hoi");
        IMAGE_MAP.put("ca loc", "local:ing_ca_loc");
        IMAGE_MAP.put("ca chep", "local:ing_ca_chep");
        IMAGE_MAP.put("muc ong", "local:ing_muc_ong");

        // ── Veggies & Herbs ──────────────────────────────────────────────────
        IMAGE_MAP.put("sa", "local:ing_sa");
        IMAGE_MAP.put("ot", "local:ing_ot");
        IMAGE_MAP.put("ca chua", "local:ing_ca_chua");
        IMAGE_MAP.put("hanh tay", "local:ing_hanh_tay");
        IMAGE_MAP.put("toi", "local:ing_toi");
        IMAGE_MAP.put("gung", "local:ing_gung");
        IMAGE_MAP.put("rau cai", "local:ing_rau_cai");
        IMAGE_MAP.put("khoai tay", "local:ing_khoai_tay");
        IMAGE_MAP.put("ca rot", "local:ing_ca_rot");
        IMAGE_MAP.put("hanh la", "local:ing_hanh_la");
        IMAGE_MAP.put("rau muong", "local:ing_rau_muong");
        IMAGE_MAP.put("la dua", "local:ing_la_dua");
        IMAGE_MAP.put("la chanh", "local:ing_la_chanh");
        IMAGE_MAP.put("nam huong", "local:ing_nam_huong");
        IMAGE_MAP.put("moc nhi", "local:ing_moc_nhi");

        // ── Staples ──────────────────────────────────────────────────────────
        IMAGE_MAP.put("dau phu", "local:ing_dau_phu");
        IMAGE_MAP.put("trung ga", "local:ing_trung_ga");
        IMAGE_MAP.put("banh pho", "local:ing_banh_pho");
        IMAGE_MAP.put("bun tuoi", "local:ing_bun_tuoi");
        IMAGE_MAP.put("gao te", "local:ing_gao_te");
        IMAGE_MAP.put("gao nep", "local:ing_gao_nep");
        IMAGE_MAP.put("banh mi", "local:ing_banh_mi");

        // ── Condiments ───────────────────────────────────────────────────────
        IMAGE_MAP.put("nuoc mam", "local:ing_nuoc_mam");
        IMAGE_MAP.put("duong", "local:ing_duong");
        IMAGE_MAP.put("tieu", "local:ing_tieu");
        IMAGE_MAP.put("dau hao", "local:ing_dau_hao");
        IMAGE_MAP.put("mu tat", "local:ing_mu_tat");
        IMAGE_MAP.put("mayonnaise", "local:ing_mayonnaise");
        IMAGE_MAP.put("bot nghe", "local:ing_bot_nghe");

        // ── Dairy & Liquids ──────────────────────────────────────────────────
        IMAGE_MAP.put("sua tuoi", "local:ing_sua_tuoi");
        IMAGE_MAP.put("nuoc dua", "local:ing_nuoc_dua");
        IMAGE_MAP.put("nuoc cot dua", "local:ing_nuoc_cot_dua");
        IMAGE_MAP.put("bo lat", "local:ing_bo_lat");
        IMAGE_MAP.put("kem tuoi", "local:ing_kem_tuoi");
        IMAGE_MAP.put("pho mai", "local:ing_pho_mai");
        IMAGE_MAP.put("ca phe den", "local:ing_ca_phe_den");
        IMAGE_MAP.put("ruou vang do", "local:ing_ruou_vang_do");
        IMAGE_MAP.put("mat ong", "local:ing_mat_ong");

        // ── Powders ──────────────────────────────────────────────────────────
        IMAGE_MAP.put("bot chien", "local:ing_bot_chien");
        IMAGE_MAP.put("bot mi", "local:ing_bot_mi");
        IMAGE_MAP.put("bot nang", "local:ing_bot_nang");

        // ── Fruits & Beans ───────────────────────────────────────────────────
        IMAGE_MAP.put("dua hau", "local:ing_dua_hau");
        IMAGE_MAP.put("hat sen", "local:ing_hat_sen");
        IMAGE_MAP.put("dau xanh", "local:ing_dau_xanh");
        IMAGE_MAP.put("dau do", "local:ing_dau_do");
        IMAGE_MAP.put("bo sap", "local:ing_bo_sap");
        IMAGE_MAP.put("chuoi", "local:ing_chuoi");
    }

    /** Returns "local:res_name", URL, or null if not curated. */
    public static String getUrl(String normalized) {
        return IMAGE_MAP.get(normalized);
    }
}
