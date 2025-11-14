package com.example.spending_management_app.utils;

/**
 * Helper class for managing expense categories and their emojis
 */
public class CategoryHelper {
    
    /**
     * Get emoji icon for a specific category
     * @param category The category name
     * @return The corresponding emoji icon
     */
    public static String getEmojiForCategory(String category) {
        if (category == null) {
            return "💳";
        }
        
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
                return "📝";
            default:
                return "💳";
        }
    }
    
    /**
     * Get all available categories grouped by type
     * @return String representation of all categories
     */
    public static String getCategoriesDescription() {
        return "DANH MỤC CHI TIÊU CÓ SẴN (chỉ chọn 1 trong các danh mục sau):\n" +
                "• NHU CẦU THIẾT YẾU: Ăn uống, Di chuyển, Tiện ích, Y tế, Nhà ở\n" +
                "• MUA SẮM & PHÁT TRIỂN: Mua sắm, Giáo dục, Sách & Học tập, Thể thao, Sức khỏe & Làm đẹp\n" +
                "• GIẢI TRÍ & XÃ HỘI: Giải trí, Du lịch, Ăn ngoài & Cafe, Quà tặng & Từ thiện, Hội họp & Tiệc tụng\n" +
                "• CÔNG NGHỆ & DỊCH VỤ: Điện thoại & Internet, Đăng ký & Dịch vụ, Phần mềm & Apps, Ngân hàng & Phí\n" +
                "• GIA ĐÌNH: Con cái, Thú cưng, Gia đình\n" +
                "• THU NHẬP: Lương, Đầu tư, Thu nhập phụ, Tiết kiệm\n" +
                "• KHÁC: Khác (chỉ dùng khi không thuộc danh mục nào)";
    }
    
    /**
     * Get category classification rules
     * @return String representation of classification rules
     */
    public static String getCategoryRules() {
        return "QUY TẮC PHÂN LOẠI:\n" +
                "- Cà phê/trà sữa/đồ uống → Ăn ngoài & Cafe\n" +
                "- Mua đồ ăn nấu → Ăn uống\n" +
                "- Ăn nhà hàng/quán → Ăn ngoài & Cafe\n" +
                "- Xe/xăng/grab/taxi → Di chuyển\n" +
                "- Điện/nước/rác → Tiện ích\n" +
                "- Thuốc/khám bệnh → Y tế\n" +
                "- Thuê nhà/vật liệu xây → Nhà ở\n" +
                "- Quần áo/mỹ phẩm → Mua sắm\n" +
                "- Học phí/khóa học → Giáo dục\n" +
                "- Sách/tài liệu → Sách & Học tập\n" +
                "- Gym/thể dục/sport → Thể thao\n" +
                "- Spa/massage/làm tóc → Sức khỏe & Làm đẹp\n" +
                "- Phim/game/concert → Giải trí\n" +
                "- Vé máy bay/khách sạn → Du lịch\n" +
                "- Điện thoại/internet/data → Điện thoại & Internet\n" +
                "- Netflix/Spotify/dịch vụ online → Đăng ký & Dịch vụ\n" +
                "- App/phần mềm → Phần mềm & Apps\n" +
                "- Phí chuyển khoản/ATM → Ngân hàng & Phí\n" +
                "- Đồ cho con → Con cái\n" +
                "- Thức ăn/phụ kiện thú cưng → Thú cưng";
    }
}
