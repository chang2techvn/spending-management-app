package com.example.spending_management_app.utils;

public final class CategoryIconHelper {
    // Private constructor prevents instantiation
    private CategoryIconHelper() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    public static String getIconEmoji(String category) {
        switch (category) {
            // Vietnamese + English mappings so icons work regardless of app language
            // Essential needs
            case "Ăn uống":
            case "Food":
                return "🍽️";
            case "Di chuyển":
            case "Transportation":
                return "🚗";
            case "Tiện ích":
            case "Utilities":
            case "Bills":
                return "⚡";
            case "Y tế":
            case "Healthcare":
                return "🏥";
            case "Nhà ở":
            case "Housing":
                return "🏠";

            // Shopping & personal development
            case "Mua sắm":
            case "Shopping":
                return "🛍️";
            case "Giáo dục":
            case "Education":
                return "📚";
            case "Sách & Học tập":
            case "Books & Learning":
                return "📖";
            case "Thể thao":
            case "Sports":
                return "⚽";
            case "Sức khỏe & Làm đẹp":
            case "Beauty & Health":
                return "💆";

            // Entertainment & social
            case "Giải trí":
            case "Entertainment":
                return "🎬";
            case "Du lịch":
            case "Travel":
                return "✈️";
            case "Ăn ngoài & Cafe":
            case "Cafe & Dining Out":
                return "☕";
            case "Quà tặng & Từ thiện":
            case "Gifts & Charity":
                return "🎁";
            case "Hội họp & Tiệc tụng":
            case "Events & Parties":
                return "🎉";

            // Technology & services
            case "Điện thoại & Internet":
            case "Phone & Internet":
                return "📱";
            case "Đăng ký & Dịch vụ":
            case "Services & Subscriptions":
                return "💳";
            case "Phần mềm & Apps":
            case "Software & Apps":
                return "💻";
            case "Ngân hàng & Phí":
            case "Banking & Fees":
                return "🏦";

            // Family & children
            case "Con cái":
            case "Children":
                return "👶";
            case "Thú cưng":
            case "Pets":
                return "🐕";
            case "Gia đình":
            case "Family":
                return "👨‍👩‍👧‍👦";

            // Income & finance
            case "Lương":
            case "Salary":
                return "💰";
            case "Đầu tư":
            case "Investment":
                return "📈";
            case "Thu nhập phụ":
            case "Side Income":
                return "💵";
            case "Tiết kiệm":
            case "Savings":
                return "🏦";

            // Other
            case "Khác":
            case "Other":
                return "📌";
            default:
                return "💳";
        }
    }
}