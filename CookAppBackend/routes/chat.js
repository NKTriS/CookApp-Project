const express = require('express');
const router = express.Router();
const { GoogleGenerativeAI } = require('@google/generative-ai');
const Groq = require('groq-sdk');
const {
    Recipe, Category, Ingredient, NutritionFact, Favorite
} = require('../models');
const { authenticateToken } = require('../middleware/auth');

// ─────────────────────────────────────────────
// CHATBOT AI (Ưu tiên Groq + Dự phòng Gemini nếu lỗi)
// ─────────────────────────────────────────────

const GEMINI_API_KEY = process.env.GEMINI_API_KEY || '';
const genAI = new GoogleGenerativeAI(GEMINI_API_KEY);

const GROQ_API_KEY = process.env.GROQ_API_KEY || '';
const groq = new Groq({ apiKey: GROQ_API_KEY });

// ── Bộ nhớ đệm System Prompt (TTL 5 phút) để tránh tải toàn bộ công thức mỗi tin nhắn ──
let cachedPrompt = null;
let cachedPromptTime = 0;
let cachedPromptUserId = null;
const PROMPT_CACHE_TTL = 5 * 60 * 1000; // 5 phút

async function buildSystemPrompt(userId) {
    const now = Date.now();
    // Trả về prompt trong bộ nhớ đệm nếu cùng user và trong thời hạn TTL
    if (cachedPrompt && cachedPromptUserId === userId && (now - cachedPromptTime) < PROMPT_CACHE_TTL) {
        return cachedPrompt;
    }

    const allRecipes = await Recipe.findAll({
        attributes: ['id', 'title', 'difficulty', 'cook_time'],
        include: [
            { model: Category, attributes: ['name'] },
            { model: Ingredient, as: 'ingredients', through: { attributes: ['quantity', 'unit'] }, attributes: ['name'] },
            { model: NutritionFact, as: 'nutrition', attributes: ['calories', 'protein', 'fat', 'carbs'] }
        ],
        limit: 100 // Giới hạn 100 công thức gần nhất để tránh quá tải dung lượng prompt
    });
    const recipeContext = allRecipes.map(r => {
        const plain = r.get({ plain: true });
        const ings = (plain.ingredients || []).map(i => `${i.name} (${i.RecipeIngredient?.quantity || ''} ${i.RecipeIngredient?.unit || ''})`).join(', ');
        const nutri = plain.nutrition ? `Calo: ${plain.nutrition.calories || '?'}, Protein: ${plain.nutrition.protein || '?'}g, Fat: ${plain.nutrition.fat || '?'}g, Carb: ${plain.nutrition.carbs || '?'}g` : '';
        return `- ${plain.title} (${plain.difficulty || '?'}, ${plain.cook_time || '?'} phút): [${ings}]. ${nutri}`;
    }).join('\n');
    let favContext = '';
    try {
        const favs = await Favorite.findAll({ where: { user_id: userId }, include: [{ model: Recipe, attributes: ['title'] }] });
        if (favs.length > 0) favContext = '\nMón yêu thích: ' + favs.map(f => f.Recipe?.title).filter(Boolean).join(', ');
    } catch(e) {}
    
    cachedPrompt = `Bạn là "Chef AI" 🍳 — trợ lý đầu bếp thông minh CHUYÊN VỀ NẤU ĂN trong ứng dụng CookApp.
Bạn nói tiếng Việt, thân thiện, vui vẻ, dùng emoji.

CHỨC NĂNG CHÍNH:
1) Gợi ý món ăn theo sở thích, mùa, dịp lễ
2) Tìm món từ nguyên liệu có sẵn trong tủ lạnh
3) Phân tích dinh dưỡng (calo, protein, fat, carb)
4) Hướng dẫn mẹo nấu ăn, kỹ thuật chế biến
5) Gợi ý thực đơn hàng ngày/hàng tuần

Danh sách công thức trong CookApp:\n${recipeContext}${favContext}

QUY TẮC BẮT BUỘC (PHẢI TUÂN THỦ):
- Chỉ trả lời các câu hỏi LIÊN QUAN ĐẾN NẤU ĂN, THỰC PHẨM, DINH DƯỠNG.
- Nếu người dùng hỏi chủ đề KHÔNG LIÊN QUAN, hãy từ chối khéo léo.
- ƯU TIÊN GỢI Ý CÁC MÓN CÓ TRONG DANH SÁCH CỦA COOKAPP ĐÃ CUNG CẤP Ở TRÊN.
- **ĐỘ CHÍNH XÁC ẨM THỰC**: Tuyệt đối không được gợi ý các sự thay thế nguyên liệu làm mất bản sắc món ăn hoặc phi lý (VD: Không bao giờ dùng thịt gà thay cho thịt bò trong món "Cơm rang dưa bò").
- **PHÂN BIỆT NGUYÊN LIỆU THÔ & THÀNH PHẨM**: Nếu người dùng có "Bột mì" (nguyên liệu thô), hãy gợi ý các món làm từ bột (pancake, bánh rán, mì sợi), KHÔNG ĐƯỢC gợi ý món dùng "Bánh mì" (thành phẩm) như "Bánh mì trứng" trừ khi bạn hướng dẫn họ làm bánh mi từ bột mì trước đó.
- Nếu người dung hỏi món không hợp với nguyên liệu họ có, hãy giải thích tại sao và gợi ý món khác phù hợp hơn.
- Sử dụng Markdown: In đậm **Tên Món**, bullet points cho nguyên liệu, đánh số cho các bước.
- Luôn kết thúc bằng một câu hỏi mở để tương tác.`;
    cachedPromptTime = now;
    cachedPromptUserId = userId;
    return cachedPrompt;
}

async function chatWithGroq(systemPrompt, message, history) {
    const msgs = [{ role: 'system', content: systemPrompt }];
    (history || []).forEach(h => msgs.push({ role: h.role === 'user' ? 'user' : 'assistant', content: h.content }));
    msgs.push({ role: 'user', content: message });
    const c = await groq.chat.completions.create({ messages: msgs, model: 'llama-3.3-70b-versatile', temperature: 0.7, max_tokens: 1024 });
    return c.choices[0]?.message?.content || 'Xin lỗi, tôi không hiểu.';
}

async function chatWithGemini(systemPrompt, message, history) {
    const model = genAI.getGenerativeModel({ model: 'gemini-2.0-flash' });
    const h = (history || []).map(x => ({ role: x.role === 'user' ? 'user' : 'model', parts: [{ text: x.content }] }));
    const chat = model.startChat({ history: [
        { role: 'user', parts: [{ text: systemPrompt }] },
        { role: 'model', parts: [{ text: 'Vâng, tôi là Chef AI! 🍳' }] }, ...h
    ]});
    const result = await chat.sendMessage(message);
    return result.response.text();
}

router.post('/chat', authenticateToken, async (req, res) => {
    try {
        const { message, history } = req.body;
        if (!message) return res.status(400).json({ error: 'Thiếu tin nhắn người dùng' });
        const systemPrompt = await buildSystemPrompt(req.user.id);
        let reply = '';
        try {
            if (GROQ_API_KEY) {
                reply = await chatWithGroq(systemPrompt, message, history);
            } else { throw new Error('Chưa cấu hình khóa API Groq'); }
        } catch (groqErr) {
            console.log('Groq lỗi:', groqErr.message, '-> chuyển sang gọi Gemini');
            try { reply = await chatWithGemini(systemPrompt, message, history); }
            catch (geminiErr) { return res.status(500).json({ error: 'Trợ lý AI đang bận, vui lòng thử lại sau!' }); }
        }
        res.json({ reply });
    } catch (e) { res.status(500).json({ error: 'Lỗi hệ thống AI: ' + e.message }); }
});

// ─────────────────────────────────────────────
// HEALTH CHECK
// ─────────────────────────────────────────────
router.get('/health', (req, res) => {
    res.json({ status: 'ok', timestamp: new Date().toISOString() });
});

module.exports = router;
