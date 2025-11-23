package com.example.spending_management_app.utils;

public final class CategoryIconHelper {
    // Private constructor prevents instantiation
    private CategoryIconHelper() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    public static String getIconEmoji(String category) {
        switch (category) {
            // Nhu cầu thiết yếu
            case "Ăn uống":
                return "🍽️";
            case "Di chuyển":
                return "🚗";
            case "Tiện ích":
                return "⚡";
            case "Y tế":
                return "🏥";
            case "Nhà ở":
                return "🏠";
            
            // Mua sắm & Phát triển bản thân
            case "Mua sắm":
                return "🛍️";
            case "Giáo dục":
                return "📚";
            case "Sách & Học tập":
                return "📖";
            case "Thể thao":
                return "⚽";
            case "Sức khỏe & Làm đẹp":
                return "💆";
            
            // Giải trí & Xã hội
            case "Giải trí":
                return "🎬";
            case "Du lịch":
                return "✈️";
            case "Ăn ngoài & Cafe":
                return "☕";
            case "Quà tặng & Từ thiện":
                return "🎁";
            case "Hội họp & Tiệc tụng":
                return "🎉";
            
            // Công nghệ & Dịch vụ
            case "Điện thoại & Internet":
                return "📱";
            case "Đăng ký & Dịch vụ":
                return "💳";
            case "Phần mềm & Apps":
                return "💻";
            case "Ngân hàng & Phí":
                return "🏦";
            
            // Gia đình & Con cái
            case "Con cái":
                return "👶";
            case "Thú cưng":
                return "🐕";
            case "Gia đình":
                return "👨‍👩‍👧‍👦";
            
            // Thu nhập & Tài chính
            case "Lương":
                return "💰";
            case "Đầu tư":
                return "📈";
            case "Thu nhập phụ":
                return "💵";
            case "Tiết kiệm":
                return "🏦";
            
            // Khác
            case "Khác":
                return "📌";
            default:
                return "💳";
        }
    }
}