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
    
    /**
     * Get all available categories
     * @return Array of all category names
     */
    public static String[] getAllCategories() {
        return new String[]{
                "Ăn uống", "Di chuyển", "Tiện ích", "Y tế", "Nhà ở",
                "Mua sắm", "Giáo dục", "Sách & Học tập", "Thể thao", "Sức khỏe & Làm đẹp",
                "Giải trí", "Du lịch", "Ăn ngoài & Cafe", "Quà tặng & Từ thiện", "Hội họp & Tiệc tụng",
                "Điện thoại & Internet", "Đăng ký & Dịch vụ", "Phần mềm & Apps", "Ngân hàng & Phí",
                "Con cái", "Thú cưng", "Gia đình",
                "Lương", "Đầu tư", "Thu nhập phụ", "Tiết kiệm",
                "Khác"
        };
    }
    
    /**
     * Detect category from text description
     * @param text The text to analyze
     * @return The detected category name
     */
    public static String detectCategory(String text) {
        String lowerText = text.toLowerCase();
        
        // Ăn ngoài & Cafe
        if (lowerText.contains("cafe") || lowerText.contains("cà phê") || lowerText.contains("ca phe") ||
            lowerText.contains("trà sữa") || lowerText.contains("đồ uống") || lowerText.contains("nhà hàng") ||
            lowerText.contains("quán ăn") || lowerText.contains("buffet")) {
            return "Ăn ngoài & Cafe";
        }
        
        // Ăn uống (mua đồ ăn về nấu)
        if (lowerText.contains("siêu thị") || lowerText.contains("chợ") || lowerText.contains("thực phẩm") ||
            lowerText.contains("đồ ăn") || lowerText.contains("rau") || lowerText.contains("thịt") ||
            lowerText.contains("cá") || lowerText.contains("trứng")) {
            return "Ăn uống";
        }
        
        // Di chuyển
        if (lowerText.contains("xăng") || lowerText.contains("xe") || lowerText.contains("grab") ||
            lowerText.contains("taxi") || lowerText.contains("bus") || lowerText.contains("xe buýt") ||
            lowerText.contains("gửi xe") || lowerText.contains("đỗ xe") || lowerText.contains("bãi xe") ||
            lowerText.contains("vé xe")) {
            return "Di chuyển";
        }
        
        // Tiện ích
        if (lowerText.contains("điện") || lowerText.contains("nước") || lowerText.contains("rác") ||
            lowerText.contains("gas") || lowerText.contains("ga")) {
            return "Tiện ích";
        }
        
        // Y tế
        if (lowerText.contains("thuốc") || lowerText.contains("khám") || lowerText.contains("bệnh viện") ||
            lowerText.contains("phòng khám") || lowerText.contains("bác sĩ") || lowerText.contains("nha khoa")) {
            return "Y tế";
        }
        
        // Nhà ở
        if (lowerText.contains("thuê nhà") || lowerText.contains("tiền nhà") || lowerText.contains("sửa nhà") ||
            lowerText.contains("xây dựng") || lowerText.contains("vật liệu")) {
            return "Nhà ở";
        }
        
        // Mua sắm
        if (lowerText.contains("quần áo") || lowerText.contains("giày") || lowerText.contains("dép") ||
            lowerText.contains("mỹ phẩm") || lowerText.contains("đồ dùng") || lowerText.contains("shopping")) {
            return "Mua sắm";
        }
        
        // Giáo dục
        if (lowerText.contains("học phí") || lowerText.contains("khóa học") || lowerText.contains("lớp học")) {
            return "Giáo dục";
        }
        
        // Sách & Học tập
        if (lowerText.contains("sách") || lowerText.contains("tài liệu") || lowerText.contains("vở")) {
            return "Sách & Học tập";
        }
        
        // Thể thao
        if (lowerText.contains("gym") || lowerText.contains("thể dục") || lowerText.contains("thể thao") ||
            lowerText.contains("bơi") || lowerText.contains("chạy bộ") || lowerText.contains("yoga")) {
            return "Thể thao";
        }
        
        // Sức khỏe & Làm đẹp
        if (lowerText.contains("spa") || lowerText.contains("massage") || lowerText.contains("làm tóc") ||
            lowerText.contains("cắt tóc") || lowerText.contains("nails") || lowerText.contains("làm đẹp")) {
            return "Sức khỏe & Làm đẹp";
        }
        
        // Giải trí
        if (lowerText.contains("phim") || lowerText.contains("rạp") || lowerText.contains("game") ||
            lowerText.contains("concert") || lowerText.contains("show")) {
            return "Giải trí";
        }
        
        // Du lịch
        if (lowerText.contains("du lịch") || lowerText.contains("máy bay") || lowerText.contains("khách sạn") ||
            lowerText.contains("resort") || lowerText.contains("vé tham quan")) {
            return "Du lịch";
        }
        
        // Điện thoại & Internet
        if (lowerText.contains("điện thoại") || lowerText.contains("internet") || lowerText.contains("data") ||
            lowerText.contains("sim") || lowerText.contains("cước")) {
            return "Điện thoại & Internet";
        }
        
        // Đăng ký & Dịch vụ
        if (lowerText.contains("netflix") || lowerText.contains("spotify") || lowerText.contains("dịch vụ") ||
            lowerText.contains("đăng ký")) {
            return "Đăng ký & Dịch vụ";
        }
        
        // Phần mềm & Apps
        if (lowerText.contains("app") || lowerText.contains("phần mềm") || lowerText.contains("software")) {
            return "Phần mềm & Apps";
        }
        
        // Ngân hàng & Phí
        if (lowerText.contains("phí") || lowerText.contains("chuyển khoản") || lowerText.contains("atm") ||
            lowerText.contains("ngân hàng")) {
            return "Ngân hàng & Phí";
        }
        
        // Con cái
        if (lowerText.contains("con") || lowerText.contains("bé") || lowerText.contains("em bé") ||
            lowerText.contains("trẻ em")) {
            return "Con cái";
        }
        
        // Thú cưng
        if (lowerText.contains("thú cưng") || lowerText.contains("chó") || lowerText.contains("mèo") ||
            lowerText.contains("pet")) {
            return "Thú cưng";
        }
        
        // Gia đình
        if (lowerText.contains("gia đình") || lowerText.contains("bố") || lowerText.contains("mẹ") ||
            lowerText.contains("ông") || lowerText.contains("bà")) {
            return "Gia đình";
        }
        
        // Quà tặng & Từ thiện
        if (lowerText.contains("quà") || lowerText.contains("tặng") || lowerText.contains("từ thiện")) {
            return "Quà tặng & Từ thiện";
        }
        
        // Default
        return "Khác";
    }
}
