package com.example.cookapp.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.cookapp.data.local.dao.CommunityDao;
import com.example.cookapp.data.local.dao.FavoriteReviewDao;
import com.example.cookapp.data.local.dao.NotificationDao;
import com.example.cookapp.data.local.dao.NutritionDao;
import com.example.cookapp.data.local.dao.RecipeDao;
import com.example.cookapp.data.local.dao.ShoppingDao;
import com.example.cookapp.data.local.dao.ShoppingListDao;
import com.example.cookapp.data.local.entity.CategoryEntity;
import com.example.cookapp.data.local.entity.DietTypeEntity;
import com.example.cookapp.data.local.entity.FavoriteEntity;
import com.example.cookapp.data.local.entity.IngredientEntity;
import com.example.cookapp.data.local.entity.NotificationEntity;
import com.example.cookapp.data.local.entity.NutritionFactEntity;
import com.example.cookapp.data.local.entity.PostCommentEntity;
import com.example.cookapp.data.local.entity.PostEntity;
import com.example.cookapp.data.local.entity.PostLikeEntity;
import com.example.cookapp.data.local.entity.RecipeEntity;
import com.example.cookapp.data.local.entity.RecipeIngredientEntity;
import com.example.cookapp.data.local.entity.RecipeStepEntity;
import com.example.cookapp.data.local.entity.ReviewEntity;
import com.example.cookapp.data.local.entity.UserEntity;
import com.example.cookapp.data.local.entity.ShoppingListEntity;
import com.example.cookapp.data.local.entity.ShoppingListItemEntity;

@Database(
        entities = {
                UserEntity.class,
                CategoryEntity.class,
                DietTypeEntity.class,
                IngredientEntity.class,
                RecipeEntity.class,
                RecipeStepEntity.class,
                RecipeIngredientEntity.class,
                ReviewEntity.class,
                PostEntity.class,
                PostLikeEntity.class,
                PostCommentEntity.class,
                FavoriteEntity.class,
                ShoppingListEntity.class,
                ShoppingListItemEntity.class,
                NutritionFactEntity.class,
                NotificationEntity.class      // Phase 2: per-user notifications
        },
        version = 35,   // Trigger destructive wipe for database seed update
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract RecipeDao recipeDao();
    public abstract ShoppingDao shoppingDao();
    public abstract CommunityDao communityDao();
    public abstract NutritionDao nutritionDao();
    public abstract FavoriteReviewDao favoriteReviewDao();
    public abstract ShoppingListDao shoppingListDao();
    public abstract NotificationDao notificationDao();
    public abstract com.example.cookapp.data.local.dao.UserDao userDao();

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "cookapp_database")
                            .fallbackToDestructiveMigration()
                            .addCallback(new RoomDatabase.Callback() {
                                @Override
                                public void onCreate(@androidx.annotation.NonNull androidx.sqlite.db.SupportSQLiteDatabase db) {
                                    super.onCreate(db);
                                    // Seed runs on background thread via executor
                                    new Thread(() ->
                                        DatabaseInitializer.populateSync(getDatabase(context))
                                    ).start();
                                }
                            })
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
