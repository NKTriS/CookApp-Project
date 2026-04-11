package com.example.cookapp;

import android.os.Bundle;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.cookapp.admin.AdminContentFragment;
import com.example.cookapp.admin.AdminDashboardFragment;
import com.example.cookapp.admin.AdminOrdersFragment;
import com.example.cookapp.admin.AdminUsersFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Admin Panel — native Android activity với 4 tabs:
 * Dashboard, Đơn hàng, Người dùng, Nội dung
 */
public class AdminActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private Fragment dashboardFragment, ordersFragment, usersFragment, contentFragment;
    private Fragment activeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            androidx.core.graphics.Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sb.left, sb.top, sb.right, 0);
            return insets;
        });

        // Back button
        ImageView ivBack = findViewById(R.id.iv_back);
        if (ivBack != null) ivBack.setOnClickListener(v -> finish());

        // Init fragments
        dashboardFragment = new AdminDashboardFragment();
        ordersFragment = new AdminOrdersFragment();
        usersFragment = new AdminUsersFragment();
        contentFragment = new AdminContentFragment();
        activeFragment = dashboardFragment;

        // Add all fragments, show only dashboard
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, contentFragment, "content").hide(contentFragment)
                .add(R.id.fragment_container, usersFragment, "users").hide(usersFragment)
                .add(R.id.fragment_container, ordersFragment, "orders").hide(ordersFragment)
                .add(R.id.fragment_container, dashboardFragment, "dashboard")
                .commit();

        // Bottom nav
        bottomNav = findViewById(R.id.admin_bottom_nav);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            Fragment target;
            if (id == R.id.nav_admin_dashboard) target = dashboardFragment;
            else if (id == R.id.nav_admin_orders) target = ordersFragment;
            else if (id == R.id.nav_admin_users) target = usersFragment;
            else if (id == R.id.nav_admin_content) target = contentFragment;
            else return false;

            if (target != activeFragment) {
                getSupportFragmentManager().beginTransaction()
                        .hide(activeFragment)
                        .show(target)
                        .commit();
                activeFragment = target;
            }
            return true;
        });
    }

    @Override
    public void onBackPressed() {
        // If not on dashboard, go back to dashboard first
        if (activeFragment != dashboardFragment) {
            bottomNav.setSelectedItemId(R.id.nav_admin_dashboard);
        } else {
            super.onBackPressed();
        }
    }
}
