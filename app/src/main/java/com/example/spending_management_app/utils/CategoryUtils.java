package com.example.spending_management_app.utils;

import android.content.Context;
import com.example.spending_management_app.R;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for handling category-related operations including
 * localized category names and icons
 */
public class CategoryUtils {

    private static final Map<String, String> CATEGORY_ICON_MAP = new HashMap<>();
    private static final Map<String, Integer> CATEGORY_COLOR_MAP = new HashMap<>();

    static {
        // Initialize icon mappings
        CATEGORY_ICON_MAP.put("Ăn uống", "🍽️");
        CATEGORY_ICON_MAP.put("Food & Dining", "🍽️");
        CATEGORY_ICON_MAP.put("Di chuyển", "🚗");
        CATEGORY_ICON_MAP.put("Transportation", "🚗");
        CATEGORY_ICON_MAP.put("Tiện ích", "⚡");
        CATEGORY_ICON_MAP.put("Utilities", "⚡");
        CATEGORY_ICON_MAP.put("Y tế", "🏥");
        CATEGORY_ICON_MAP.put("Healthcare", "🏥");
        CATEGORY_ICON_MAP.put("Nhà ở", "🏠");
        CATEGORY_ICON_MAP.put("Housing", "🏠");
        CATEGORY_ICON_MAP.put("Mua sắm", "🛍️");
        CATEGORY_ICON_MAP.put("Shopping", "🛍️");
        CATEGORY_ICON_MAP.put("Giáo dục", "📚");
        CATEGORY_ICON_MAP.put("Education", "📚");
        CATEGORY_ICON_MAP.put("Sách & Học tập", "📖");
        CATEGORY_ICON_MAP.put("Books & Learning", "📖");
        CATEGORY_ICON_MAP.put("Thể thao", "⚽");
        CATEGORY_ICON_MAP.put("Sports", "⚽");
        CATEGORY_ICON_MAP.put("Sức khỏe & Làm đẹp", "💆");
        CATEGORY_ICON_MAP.put("Beauty & Health", "💆");
        CATEGORY_ICON_MAP.put("Giải trí", "🎬");
        CATEGORY_ICON_MAP.put("Entertainment", "🎬");
        CATEGORY_ICON_MAP.put("Du lịch", "✈️");
        CATEGORY_ICON_MAP.put("Travel", "✈️");
        CATEGORY_ICON_MAP.put("Ăn ngoài & Cafe", "☕");
        CATEGORY_ICON_MAP.put("Cafe & Dining Out", "☕");
        CATEGORY_ICON_MAP.put("Quà tặng & Từ thiện", "🎁");
        CATEGORY_ICON_MAP.put("Gifts & Charity", "🎁");
        CATEGORY_ICON_MAP.put("Hội họp & Tiệc tụng", "🎉");
        CATEGORY_ICON_MAP.put("Events & Parties", "🎉");
        CATEGORY_ICON_MAP.put("Điện thoại & Internet", "📱");
        CATEGORY_ICON_MAP.put("Phone & Internet", "📱");
        CATEGORY_ICON_MAP.put("Đăng ký & Dịch vụ", "💳");
        CATEGORY_ICON_MAP.put("Services & Subscriptions", "💳");
        CATEGORY_ICON_MAP.put("Phần mềm & Apps", "💻");
        CATEGORY_ICON_MAP.put("Software & Apps", "💻");
        CATEGORY_ICON_MAP.put("Ngân hàng & Phí", "🏦");
        CATEGORY_ICON_MAP.put("Banking & Fees", "🏦");
        CATEGORY_ICON_MAP.put("Con cái", "👶");
        CATEGORY_ICON_MAP.put("Children", "👶");
        CATEGORY_ICON_MAP.put("Thú cưng", "🐕");
        CATEGORY_ICON_MAP.put("Pets", "🐕");
        CATEGORY_ICON_MAP.put("Gia đình", "👨‍👩‍👧‍👦");
        CATEGORY_ICON_MAP.put("Family", "👨‍👩‍👧‍👦");
        CATEGORY_ICON_MAP.put("Lương", "💰");
        CATEGORY_ICON_MAP.put("Salary", "💰");
        CATEGORY_ICON_MAP.put("Đầu tư", "📈");
        CATEGORY_ICON_MAP.put("Investment", "📈");
        CATEGORY_ICON_MAP.put("Thu nhập phụ", "💵");
        CATEGORY_ICON_MAP.put("Side Income", "💵");
        CATEGORY_ICON_MAP.put("Tiết kiệm", "🏦");
        CATEGORY_ICON_MAP.put("Savings", "🏦");
        CATEGORY_ICON_MAP.put("Khác", "📱");
        CATEGORY_ICON_MAP.put("Other", "📱");
        CATEGORY_ICON_MAP.put("Ngân sách", "💰");
        CATEGORY_ICON_MAP.put("Budget", "💰");

        // Initialize color mappings (using resource IDs)
        CATEGORY_COLOR_MAP.put("Ăn uống", R.color.category_food);
        CATEGORY_COLOR_MAP.put("Food & Dining", R.color.category_food);
        CATEGORY_COLOR_MAP.put("Di chuyển", R.color.category_transport);
        CATEGORY_COLOR_MAP.put("Transportation", R.color.category_transport);
        CATEGORY_COLOR_MAP.put("Tiện ích", R.color.category_utility);
        CATEGORY_COLOR_MAP.put("Utilities", R.color.category_utility);
        CATEGORY_COLOR_MAP.put("Y tế", R.color.category_health);
        CATEGORY_COLOR_MAP.put("Healthcare", R.color.category_health);
        CATEGORY_COLOR_MAP.put("Nhà ở", R.color.category_housing);
        CATEGORY_COLOR_MAP.put("Housing", R.color.category_housing);
        CATEGORY_COLOR_MAP.put("Mua sắm", R.color.category_shopping);
        CATEGORY_COLOR_MAP.put("Shopping", R.color.category_shopping);
        CATEGORY_COLOR_MAP.put("Giáo dục", R.color.category_education);
        CATEGORY_COLOR_MAP.put("Education", R.color.category_education);
        CATEGORY_COLOR_MAP.put("Sách & Học tập", R.color.category_education);
        CATEGORY_COLOR_MAP.put("Books & Learning", R.color.category_education);
        CATEGORY_COLOR_MAP.put("Thể thao", R.color.category_fitness);
        CATEGORY_COLOR_MAP.put("Sports", R.color.category_fitness);
        CATEGORY_COLOR_MAP.put("Sức khỏe & Làm đẹp", R.color.category_fitness);
        CATEGORY_COLOR_MAP.put("Beauty & Health", R.color.category_fitness);
        CATEGORY_COLOR_MAP.put("Giải trí", R.color.category_entertainment);
        CATEGORY_COLOR_MAP.put("Entertainment", R.color.category_entertainment);
        CATEGORY_COLOR_MAP.put("Du lịch", R.color.category_entertainment);
        CATEGORY_COLOR_MAP.put("Travel", R.color.category_entertainment);
        CATEGORY_COLOR_MAP.put("Ăn ngoài & Cafe", R.color.category_cafe);
        CATEGORY_COLOR_MAP.put("Cafe & Dining Out", R.color.category_cafe);
        CATEGORY_COLOR_MAP.put("Quà tặng & Từ thiện", R.color.category_gift);
        CATEGORY_COLOR_MAP.put("Gifts & Charity", R.color.category_gift);
        CATEGORY_COLOR_MAP.put("Hội họp & Tiệc tụng", R.color.category_gift);
        CATEGORY_COLOR_MAP.put("Events & Parties", R.color.category_gift);
        CATEGORY_COLOR_MAP.put("Điện thoại & Internet", R.color.category_tech);
        CATEGORY_COLOR_MAP.put("Phone & Internet", R.color.category_tech);
        CATEGORY_COLOR_MAP.put("Phần mềm & Apps", R.color.category_tech);
        CATEGORY_COLOR_MAP.put("Software & Apps", R.color.category_tech);
        CATEGORY_COLOR_MAP.put("Đăng ký & Dịch vụ", R.color.category_service);
        CATEGORY_COLOR_MAP.put("Services & Subscriptions", R.color.category_service);
        CATEGORY_COLOR_MAP.put("Ngân hàng & Phí", R.color.category_service);
        CATEGORY_COLOR_MAP.put("Banking & Fees", R.color.category_service);
        CATEGORY_COLOR_MAP.put("Con cái", R.color.category_family);
        CATEGORY_COLOR_MAP.put("Children", R.color.category_family);
        CATEGORY_COLOR_MAP.put("Thú cưng", R.color.category_family);
        CATEGORY_COLOR_MAP.put("Pets", R.color.category_family);
        CATEGORY_COLOR_MAP.put("Gia đình", R.color.category_family);
        CATEGORY_COLOR_MAP.put("Family", R.color.category_family);
        CATEGORY_COLOR_MAP.put("Lương", R.color.category_income);
        CATEGORY_COLOR_MAP.put("Salary", R.color.category_income);
        CATEGORY_COLOR_MAP.put("Đầu tư", R.color.category_income);
        CATEGORY_COLOR_MAP.put("Investment", R.color.category_income);
        CATEGORY_COLOR_MAP.put("Thu nhập phụ", R.color.category_income);
        CATEGORY_COLOR_MAP.put("Side Income", R.color.category_income);
        CATEGORY_COLOR_MAP.put("Tiết kiệm", R.color.category_income);
        CATEGORY_COLOR_MAP.put("Savings", R.color.category_income);
        CATEGORY_COLOR_MAP.put("Khác", R.color.category_default);
        CATEGORY_COLOR_MAP.put("Other", R.color.category_default);
        CATEGORY_COLOR_MAP.put("Ngân sách", R.color.category_income);
        CATEGORY_COLOR_MAP.put("Budget", R.color.category_income);
    }

    /**
     * Get the icon emoji for a category
     * @param category The category name (can be localized)
     * @return The emoji icon for the category, or default icon if not found
     */
    public static String getIconForCategory(String category) {
        return CATEGORY_ICON_MAP.getOrDefault(category, "💳");
    }

    /**
     * Get the color resource ID for a category
     * @param category The category name (can be localized)
     * @return The color resource ID for the category, or default color if not found
     */
    public static int getColorForCategory(String category) {
        return CATEGORY_COLOR_MAP.getOrDefault(category, R.color.category_default);
    }

    /**
     * Get the localized category name from string resources
     * @param context The context to access string resources
     * @param categoryKey The category key to look up
     * @return The localized category name with icon
     */
    public static String getLocalizedCategoryName(Context context, String categoryKey) {
        if (context == null || categoryKey == null) {
            return categoryKey;
        }

        try {
            // Map category keys to string resource names
            switch (categoryKey) {
                case "Ăn uống":
                    return context.getString(R.string.food_category);
                case "Di chuyển":
                    return context.getString(R.string.transport_category);
                case "Mua sắm":
                    return context.getString(R.string.shopping_category);
                case "Giải trí":
                    return context.getString(R.string.entertainment_category);
                case "Tiện ích":
                    return context.getString(R.string.utilities_category);
                case "Y tế":
                    return context.getString(R.string.healthcare_category);
                case "Nhà ở":
                    return context.getString(R.string.housing_category);
                case "Giáo dục":
                    return context.getString(R.string.education_category);
                case "Sách & Học tập":
                    return context.getString(R.string.books_category);
                case "Thể thao":
                    return context.getString(R.string.sports_category);
                case "Sức khỏe & Làm đẹp":
                    return context.getString(R.string.beauty_category);
                case "Du lịch":
                    return context.getString(R.string.travel_category);
                case "Ăn ngoài & Cafe":
                    return context.getString(R.string.cafe_category);
                case "Quà tặng & Từ thiện":
                    return context.getString(R.string.gifts_category);
                case "Hội họp & Tiệc tụng":
                    return context.getString(R.string.events_category);
                case "Điện thoại & Internet":
                    return context.getString(R.string.phone_category);
                case "Đăng ký & Dịch vụ":
                    return context.getString(R.string.services_category);
                case "Phần mềm & Apps":
                    return context.getString(R.string.software_category);
                case "Ngân hàng & Phí":
                    return context.getString(R.string.banking_category);
                case "Con cái":
                    return context.getString(R.string.children_category);
                case "Thú cưng":
                    return context.getString(R.string.pets_category);
                case "Gia đình":
                    return context.getString(R.string.family_category);
                case "Lương":
                    return context.getString(R.string.salary_category);
                case "Đầu tư":
                    return context.getString(R.string.investment_category);
                case "Thu nhập phụ":
                    return context.getString(R.string.side_income_category);
                case "Tiết kiệm":
                    return context.getString(R.string.savings_category);
                case "Khác":
                    return context.getString(R.string.other_category);
                case "Ngân sách":
                    return context.getString(R.string.budget_category);
                default:
                    return categoryKey; // Return original if not found
            }
        } catch (Exception e) {
            // If string resource is not found, return original category
            return categoryKey;
        }
    }
}