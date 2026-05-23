/**
 * Script tạo/set admin user.
 * Chạy: node scripts/make-admin.js <email>
 * 
 * Nếu email chưa tồn tại → tạo mới với password "admin123"
 * Nếu email đã tồn tại → set role = 'admin'
 */
require('dotenv').config();
const bcrypt = require('bcryptjs');
const { User, sequelize } = require('../models');

async function main() {
    const email = process.argv[2] || 'admin@cookapp.vn';
    
    await sequelize.sync({ alter: true });

    let user = await User.findOne({ where: { email } });
    
    if (user) {
        user.role = 'admin';
        await user.save();
        console.log(`✅ User "${email}" đã được set thành Admin!`);
    } else {
        const hashedPassword = await bcrypt.hash('admin123', 10);
        user = await User.create({
            email,
            password: hashedPassword,
            fullName: 'CookApp Admin',
            role: 'admin'
        });
        console.log(`✅ Tạo admin mới: ${email} / admin123`);
    }

    console.log(`   ID: ${user.id}, Role: ${user.role}`);
    process.exit(0);
}

main().catch(e => { console.error('❌', e.message); process.exit(1); });

// Cập nhật