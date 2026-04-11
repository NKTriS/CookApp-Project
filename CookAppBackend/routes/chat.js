const express = require('express');
const router = express.Router();
const { GoogleGenerativeAI } = require('@google/generative-ai');
const Groq = require('groq-sdk');
const {
    Recipe, Category, Ingredient, NutritionFact, Favorite
} = require('../models');
const { authenticateToken } = require('../middleware/auth');

// ─────────────────────────────────────────────
// AI CHATBOT (Groq primary + Gemini fallback)
// ─────────────────────────────────────────────

const GEMINI_API_KEY = process.env.GEMINI_API_KEY || '';
const genAI = new GoogleGenerativeAI(GEMINI_API_KEY);

const GROQ_API_KEY = process.env.GROQ_API_KEY || '';
const groq = new Groq({ apiKey: GROQ_API_KEY });

// ── System prompt cache (5-minute TTL) to avoid loading all recipes per message ──
let cachedPrompt = null;
let cachedPromptTime = 0;
let cachedPromptUserId = null;
const PROMPT_CACHE_TTL = 5 * 60 * 1000; // 5 minutes

async function buildSystemPrompt(userId) {
    const now = Date.now();
    // Return cached prompt if same user and within TTL
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
        limit: 100 // Cap at 100 most recent recipes for prompt size
    });
    const recipeContext = allRecipes.map(r => {
        const plain = r.get({ plain: true });
        const ings = (plain.ingredients || []).map(i => `${i.name} (${i.RecipeIngredient?.quantity || ''} ${i.RecipeIngredient?.unit || ''})`).join(', ');
        const nutri = plain.nutrition ? `Calo: ${plain.nutrition.calories || '?'}, Protein: ${plain.nutrition.protein || '?'}g, Fat: ${plain.nutrition.fat || '?'}g, Carb: ${plain.nutrition.carbs || '?'}g` : '';
        return `- ${plain.title} (${plain.difficulty || '?'}, ${plain.cook_time || '?'} phut): [${ings}]. ${nutri}`;
    }).join('\n');
    let favContext = '';
    try {
        const favs = await Favorite.findAll({ where: { user_id: userId }, include: [{ model: Recipe, attributes: ['title'] }] });
        if (favs.length > 0) favContext = '\nMon yeu thich: ' + favs.map(f => f.Recipe?.title).filter(Boolean).join(', ');
    } catch(e) {}
    
    cachedPrompt = `Ban la "Chef AI" 🍳 — tro ly dau bep thong minh CHUYEN VE NAU AN trong ung dung CookApp.
Ban noi tieng Viet, than thien, vui ve, dung emoji.

CHUC NANG CHINH:
1) Goi y mon an theo so thich, mua, dip le
2) Tim mon tu nguyen lieu co san trong tu lanh
3) Phan tich dinh duong (calo, protein, fat, carb)
4) Huong dan meo nau an, ky thuat che bien
5) Goi y thuc don hang ngay/hang tuan

Danh sach cong thuc trong CookApp:\n${recipeContext}${favContext}

QUY TAC BAT BUOC (PHAI TUAN THU):
- Chi tra loi cac cau hoi LIEN QUAN DEN NAU AN, THUC PHAM, DINH DUONG.
- Neu nguoi dung hoi chu de KHONG LIEN QUAN, hay tu choi kheo leo.
- UU TIEN GOI Y CAC MON CO TRONG DANH SACH CUA COOKAPP DA CUNG CAP O TREN.
- **DO CHINH XAC CULINARY**: Tuyet doi khong duoc goi y cac su thay the nguyen lieu lam mat ban sac mon an hoac phi ly (VD: Khong bao gio dung thit ga thay cho thịt bò trong mon "Com rang dua bo").
- **PHAN BIET NGUYEN LIEU THO & THANH PHAM**: Neu nguoi dung co "Bot mi" (nguyen lieu tho), hay goi y cac mon lam tu bot (pancake, banh ran, mi soi), KHONG DUOC goi y mon dung "Banh mi" (thanh pham) nhu "Banh mi trung" tru khi ban huong dan ho lam banh mi tu bot mi truoc do.
- Neu nguoi dung hoi mon khong hop voi nguyen lieu ho co, hay giai thich tai sao va goi y mon khac phu hop hon.
- Su dung Markdown: In dam **Ten Mon**, bullet points cho nguyen lieu, danh so cho cac buoc.
- Luon ket thuc bang mot cau hoi mo de tuong tac.`;
    cachedPromptTime = now;
    cachedPromptUserId = userId;
    return cachedPrompt;
}

async function chatWithGroq(systemPrompt, message, history) {
    const msgs = [{ role: 'system', content: systemPrompt }];
    (history || []).forEach(h => msgs.push({ role: h.role === 'user' ? 'user' : 'assistant', content: h.content }));
    msgs.push({ role: 'user', content: message });
    const c = await groq.chat.completions.create({ messages: msgs, model: 'llama-3.3-70b-versatile', temperature: 0.7, max_tokens: 1024 });
    return c.choices[0]?.message?.content || 'Xin loi, toi khong hieu.';
}

async function chatWithGemini(systemPrompt, message, history) {
    const model = genAI.getGenerativeModel({ model: 'gemini-2.0-flash' });
    const h = (history || []).map(x => ({ role: x.role === 'user' ? 'user' : 'model', parts: [{ text: x.content }] }));
    const chat = model.startChat({ history: [
        { role: 'user', parts: [{ text: systemPrompt }] },
        { role: 'model', parts: [{ text: 'Vang, toi la Chef AI! 🍳' }] }, ...h
    ]});
    const result = await chat.sendMessage(message);
    return result.response.text();
}

router.post('/chat', authenticateToken, async (req, res) => {
    try {
        const { message, history } = req.body;
        if (!message) return res.status(400).json({ error: 'Missing message' });
        const systemPrompt = await buildSystemPrompt(req.user.id);
        let reply = '';
        try {
            if (GROQ_API_KEY) {
                reply = await chatWithGroq(systemPrompt, message, history);
            } else { throw new Error('No Groq key configured'); }
        } catch (groqErr) {
            console.log('Groq failed:', groqErr.message, '-> trying Gemini');
            try { reply = await chatWithGemini(systemPrompt, message, history); }
            catch (geminiErr) { return res.status(500).json({ error: 'AI dang ban, vui long thu lai!' }); }
        }
        res.json({ reply });
    } catch (e) { res.status(500).json({ error: 'Loi AI: ' + e.message }); }
});

// ─────────────────────────────────────────────
// HEALTH CHECK
// ─────────────────────────────────────────────
router.get('/health', (req, res) => {
    res.json({ status: 'ok', timestamp: new Date().toISOString() });
});

module.exports = router;
