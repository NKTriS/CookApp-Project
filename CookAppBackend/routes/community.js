const express = require('express');
const fs = require('fs');
const path = require('path');
const router = express.Router();
const {
    Post, PostComment, PostLike, User, SavedPost
} = require('../models');
const { authenticateToken, optionalAuthenticateToken } = require('../middleware/auth');

// ─────────────────────────────────────────────
// COMMUNITY POSTS (with pagination)
// ─────────────────────────────────────────────

// GET /api/community/posts
router.get('/posts', optionalAuthenticateToken, async (req, res) => {
    try {
        const currentUserId = req.user ? req.user.id : null;
        const limit = parseInt(req.query.limit) || 50;
        const page = parseInt(req.query.page) || 1;

        const posts = await Post.findAll({
            include: [{ model: PostComment, as: 'comments' }],
            order: [['created_at', 'DESC']],
            limit,
            offset: (page - 1) * limit
        });

        let likedSet = new Set();
        let savedSet = new Set();
        if (currentUserId) {
            const myLikes = await PostLike.findAll({ where: { user_id: currentUserId } });
            myLikes.forEach(l => likedSet.add(l.post_id));
            const mySaves = await SavedPost.findAll({ where: { user_id: currentUserId } });
            mySaves.forEach(s => savedSet.add(s.post_id));
        }

        res.json(posts.map(p => ({ ...p.toJSON(), is_liked_by_me: likedSet.has(p.id), is_saved_by_me: savedSet.has(p.id) })));
    } catch (e) { res.status(500).json({ error: e.message }); }
});

// GET /api/community/posts/saved
router.get('/posts/saved', authenticateToken, async (req, res) => {
    try {
        const savedPosts = await SavedPost.findAll({
            where: { user_id: req.user.id },
            include: [{
                model: Post,
                include: [{ model: PostComment, as: 'comments' }]
            }],
            order: [['created_at', 'DESC']]
        });
        const posts = savedPosts
            .filter(sp => sp.Post)
            .map(sp => ({ ...sp.Post.toJSON(), is_liked_by_me: false, is_saved_by_me: true }));
        res.json(posts);
    } catch (e) { res.status(500).json({ error: e.message }); }
});

// GET /api/community/posts/mine
router.get('/posts/mine', authenticateToken, async (req, res) => {
    try {
        const posts = await Post.findAll({
            where: { user_id: req.user.id },
            include: [{ model: PostComment, as: 'comments', order: [['created_at', 'ASC']] }],
            order: [['created_at', 'DESC']]
        });
        res.json(posts);
    } catch (e) { res.status(500).json({ error: e.message }); }
});

// GET /api/community/posts/:id
router.get('/posts/:id', optionalAuthenticateToken, async (req, res) => {
    try {
        const currentUserId = req.user ? req.user.id : null;
        const post = await Post.findByPk(req.params.id, {
            include: [{ model: PostComment, as: 'comments', order: [['created_at', 'ASC']] }]
        });
        if (!post) return res.status(404).json({ error: 'Post not found' });
        let is_liked_by_me = false;
        let is_saved_by_me = false;
        if (currentUserId) {
            const like = await PostLike.findOne({ where: { user_id: currentUserId, post_id: post.id } });
            is_liked_by_me = !!like;
            const save = await SavedPost.findOne({ where: { user_id: currentUserId, post_id: post.id } });
            is_saved_by_me = !!save;
        }
        res.json({ ...post.toJSON(), is_liked_by_me, is_saved_by_me });
    } catch (e) { res.status(500).json({ error: e.message }); }
});

// POST /api/community/posts
router.post('/posts', authenticateToken, async (req, res) => {
    try {
        const { title, content, image_url } = req.body;
        if (!title || !content) return res.status(400).json({ error: 'title and content are required' });

        let final_image_url = image_url || null;
        if (image_url && image_url.startsWith('data:image')) {
            const matches = image_url.match(/^data:([A-Za-z-+\/]+);base64,(.+)$/);
            if (matches && matches.length === 3) {
                const ext = matches[1].split('/')[1] === 'png' ? 'png' : 'jpg';
                const buffer = Buffer.from(matches[2], 'base64');
                const filename = 'post_' + Date.now() + '.' + ext;
                const uploadPath = path.join(__dirname, '../public/videos/thumbnails', filename);
                fs.writeFileSync(uploadPath, buffer);
                final_image_url = '/videos/thumbnails/' + filename;
            }
        } else if (image_url && image_url.startsWith('content://')) {
            final_image_url = null; // Drop invalid Android specific local URI mappings
        }

        const user = await User.findByPk(req.user.id, { attributes: ['fullName', 'email'] });
        const authorName = (user && user.fullName) ? user.fullName : req.user.email;
        const post = await Post.create({
            title, content,
            author: authorName,
            image_url: final_image_url,
            created_at: Math.floor(Date.now() / 1000),
            likes: 0,
            user_id: req.user.id
        });
        res.status(201).json(post);
    } catch (e) { res.status(500).json({ error: e.message }); }
});

// POST /api/community/posts/:id/like — toggle like
router.post('/posts/:id/like', authenticateToken, async (req, res) => {
    try {
        const post = await Post.findByPk(req.params.id);
        if (!post) return res.status(404).json({ error: 'Post not found' });

        const existingLike = await PostLike.findOne({
            where: { user_id: req.user.id, post_id: post.id }
        });

        if (existingLike) {
            await existingLike.destroy();
            await post.decrement('likes');
            await post.reload();
            res.json({ isLiked: false, likesCount: post.likes });
        } else {
            await PostLike.create({ user_id: req.user.id, post_id: post.id });
            await post.increment('likes');
            await post.reload();
            res.json({ isLiked: true, likesCount: post.likes });
        }
    } catch (e) { res.status(500).json({ error: e.message }); }
});

// POST /api/community/posts/:id/comments
router.post('/posts/:id/comments', authenticateToken, async (req, res) => {
    try {
        const { content } = req.body;
        if (!content) return res.status(400).json({ error: 'content is required' });

        const post = await Post.findByPk(req.params.id);
        if (!post) return res.status(404).json({ error: 'Post not found' });

        const user = await User.findByPk(req.user.id, { attributes: ['fullName', 'email'] });
        const authorName = (user && user.fullName) ? user.fullName : req.user.email;
        const comment = await PostComment.create({
            post_id: req.params.id,
            author: authorName,
            content,
            created_at: new Date(),
            user_id: req.user.id
        });
        res.status(201).json(comment);
    } catch (e) { res.status(500).json({ error: e.message }); }
});

// POST /api/community/posts/:id/save — toggle save/unsave
router.post('/posts/:id/save', authenticateToken, async (req, res) => {
    try {
        const post = await Post.findByPk(req.params.id);
        if (!post) return res.status(404).json({ error: 'Post not found' });

        const existing = await SavedPost.findOne({
            where: { user_id: req.user.id, post_id: post.id }
        });

        if (existing) {
            await existing.destroy();
            res.json({ isSaved: false, message: 'Post unsaved' });
        } else {
            await SavedPost.create({ user_id: req.user.id, post_id: post.id });
            res.json({ isSaved: true, message: 'Post saved' });
        }
    } catch (e) { res.status(500).json({ error: e.message }); }
});

// PUT /api/community/comments/:id — edit a comment
router.put('/comments/:id', authenticateToken, async (req, res) => {
    try {
        const { content } = req.body;
        if (!content) return res.status(400).json({ error: 'content is required' });

        const comment = await PostComment.findByPk(req.params.id);
        if (!comment) return res.status(404).json({ error: 'Comment not found' });

        if (comment.user_id !== req.user.id) {
            return res.status(403).json({ error: 'You are not allowed to edit this comment' });
        }

        comment.content = content;
        await comment.save();

        res.json(comment);
    } catch (e) { res.status(500).json({ error: e.message }); }
});

// DELETE /api/community/comments/:id — delete a comment
router.delete('/comments/:id', authenticateToken, async (req, res) => {
    try {
        const comment = await PostComment.findByPk(req.params.id);
        if (!comment) return res.status(404).json({ error: 'Comment not found' });

        if (comment.user_id !== req.user.id) {
            return res.status(403).json({ error: 'You are not allowed to delete this comment' });
        }

        await comment.destroy();
        res.json({ success: true, message: 'Comment deleted successfully' });
    } catch (e) { res.status(500).json({ error: e.message }); }
});

module.exports = router;
