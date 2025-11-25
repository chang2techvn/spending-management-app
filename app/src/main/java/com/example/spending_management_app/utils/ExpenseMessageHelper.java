package com.example.spending_management_app.utils;

public final class ExpenseMessageHelper {
    // Private constructor prevents instantiation
    private ExpenseMessageHelper() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    public static String getHumorousComment(String category, long amount, String name) {
        // Generate humorous comments based on category and amount
        switch (category.toLowerCase()) {
            case "ăn uống":
                if (amount > 100000) {
                    return "Ăn ngon thế này thì tiền bay cũng đáng rồi! 🍽️";
                } else if (amount > 50000) {
                    return "Đói bụng thì phải ăn thôi mà! 😋";
                } else {
                    return "Tiết kiệm mà vẫn ngon, giỏi lắm! 👍";
                }
            case "di chuyển":
                if (amount > 200000) {
                    return "Đi xa thế này chắc về quê nhỉ? 🚗";
                } else {
                    return "Đi lại cũng cần tiền xăng chứ! ⛽";
                }
            case "mua sắm":
                if (amount > 500000) {
                    return "Shopping thế này ví run cầm cập! 💸";
                } else {
                    return "Mua sắm hợp lý, đúng rồi! 🛍️";
                }
            case "giải trí":
                return "Vui chơi để sống khỏe mạnh! 🎉";
            case "y tế":
                return "Sức khỏe là vàng, chi tiêu đúng rồi! 🏥";
            default:
                if (amount > 100000) {
                    return "Chi tiêu khủng thế này! 💰";
                } else {
                    return "Chi tiêu hợp lý, tốt lắm! ✨";
                }
        }
    }

        // Check if user is asking for financial analysis
    public static boolean isFinancialQuery(String text) {
        String lowerText = text.toLowerCase();
        return lowerText.contains("chi tiêu") && (
                lowerText.contains("hôm nay") || lowerText.contains("hôm qua") || 
                lowerText.contains("tuần") || lowerText.contains("tháng") ||
                lowerText.contains("tổng") || lowerText.contains("bao nhiêu") ||
                lowerText.contains("phân tích") || lowerText.contains("báo cáo") ||
                lowerText.contains("danh mục") || lowerText.contains("thống kê") ||
                lowerText.contains("ngày") && (lowerText.contains("/") || lowerText.matches(".*\\d+.*")) ||
                lowerText.contains("so với") || lowerText.contains("tư vấn")
        ) || lowerText.contains("spending") && (
                lowerText.contains("today") || lowerText.contains("yesterday") ||
                lowerText.contains("week") || lowerText.contains("month") ||
                lowerText.contains("total") || lowerText.contains("how much") ||
                lowerText.contains("analyze") || lowerText.contains("report") ||
                lowerText.contains("category") || lowerText.contains("statistics") ||
                lowerText.contains("day") && lowerText.matches(".*\\d+.*") ||
                lowerText.contains("compared to") || lowerText.contains("consult") ||
                lowerText.contains("expense") || lowerText.contains("expenses") ||
                lowerText.contains("cost") || lowerText.contains("costs")
        );
    }
}