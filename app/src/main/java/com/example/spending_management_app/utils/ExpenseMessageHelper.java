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
}