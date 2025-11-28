package com.example.spending_management_app.utils;

public final class BudgetMessageHelper {
    private BudgetMessageHelper() {
        throw new UnsupportedOperationException("Utility class");
    }


    // Check if user is querying about budget
    public static boolean isBudgetQuery(String text) {
        String lowerText = text.toLowerCase();

        // Check for Vietnamese budget keywords
        boolean hasVietnameseBudget = lowerText.contains("ngân sách");

        // Check for English budget keywords
        boolean hasEnglishBudget = lowerText.contains("budget") || lowerText.contains("budgets");

        if (!hasVietnameseBudget && !hasEnglishBudget) {
            return false;
        }

        // Vietnamese action keywords
        boolean hasVietnameseActions = lowerText.contains("xem") || lowerText.contains("hiển thị") ||
                lowerText.contains("cho tôi biết") || lowerText.contains("thế nào") ||
                lowerText.contains("bao nhiêu") || lowerText.contains("phân tích") ||
                lowerText.contains("tư vấn") || lowerText.contains("đánh giá") ||
                lowerText.contains("so sánh") || lowerText.contains("xu hướng") ||
                lowerText.contains("xóa") || lowerText.contains("xoá") ||
                lowerText.contains("thêm") || lowerText.contains("đặt") ||
                lowerText.contains("sửa") || lowerText.contains("thay đổi") ||
                lowerText.contains("thiết lập");

        // English action keywords
        boolean hasEnglishActions = lowerText.contains("view") || lowerText.contains("show") ||
                lowerText.contains("tell me") || lowerText.contains("how much") ||
                lowerText.contains("analyze") || lowerText.contains("consult") ||
                lowerText.contains("evaluate") || lowerText.contains("compare") ||
                lowerText.contains("trend") || lowerText.contains("delete") ||
                lowerText.contains("remove") || lowerText.contains("add") ||
                lowerText.contains("set") || lowerText.contains("edit") ||
                lowerText.contains("change") || lowerText.contains("establish") ||
                lowerText.contains("update") || lowerText.contains("modify");

        // Vietnamese time keywords
        boolean hasVietnameseTime = lowerText.contains("năm") || lowerText.contains("tháng") ||
                lowerText.contains("này") || lowerText.contains("trước") ||
                lowerText.contains("sau") || lowerText.contains("tất cả") ||
                lowerText.contains("toàn bộ") || lowerText.contains("hiện tại");

        // English time keywords
        boolean hasEnglishTime = lowerText.contains("year") || lowerText.contains("month") ||
                lowerText.contains("this") || lowerText.contains("previous") ||
                lowerText.contains("next") || lowerText.contains("all") ||
                lowerText.contains("current") || lowerText.contains("now");

        // Short query check (Vietnamese)
        boolean isShortVietnameseQuery = lowerText.trim().length() <= 15 && hasVietnameseBudget;

        // Short query check (English)
        boolean isShortEnglishQuery = lowerText.trim().length() <= 15 && hasEnglishBudget;

        return hasVietnameseActions || hasEnglishActions || hasVietnameseTime || hasEnglishTime ||
               isShortVietnameseQuery || isShortEnglishQuery;
    }

}
