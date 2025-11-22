package com.example.spending_management_app.domain.usecase.budget;

public final class BudgetMessageHelper {
    private BudgetMessageHelper() {
        throw new UnsupportedOperationException("Utility class");
    }


    // Check if user is querying about budget
    public static boolean isBudgetQuery(String text) {
        String lowerText = text.toLowerCase();

        // Nếu có từ "ngân sách"
        if (!lowerText.contains("ngân sách")) {
            return false;
        }

        // Các trường hợp luôn là câu hỏi về ngân sách:
        // 1. Có động từ hành động hoặc câu hỏi
        boolean hasActionOrQuestion =
                lowerText.contains("xem") || lowerText.contains("hiển thị") ||
                        lowerText.contains("cho tôi biết") || lowerText.contains("thế nào") ||
                        lowerText.contains("bao nhiêu") || lowerText.contains("phân tích") ||
                        lowerText.contains("tư vấn") || lowerText.contains("đánh giá") ||
                        lowerText.contains("so sánh") || lowerText.contains("xu hướng") ||
                        lowerText.contains("xóa") || lowerText.contains("xoá") ||
                        lowerText.contains("thêm") || lowerText.contains("đặt") ||
                        lowerText.contains("sửa") || lowerText.contains("thay đổi") ||
                        lowerText.contains("thiết lập");

        // 2. Có từ khóa thời gian (năm, tháng, ngày) - ngầm hiểu là xem ngân sách
        boolean hasTimeKeyword =
                lowerText.contains("năm") || lowerText.contains("tháng") ||
                        lowerText.contains("này") || lowerText.contains("trước") ||
                        lowerText.contains("sau") || lowerText.contains("tất cả") ||
                        lowerText.contains("toàn bộ") || lowerText.contains("hiện tại");

        // 3. Chỉ có "ngân sách" một mình (câu ngắn <= 15 ký tự) - có thể là xem tổng quan
        boolean isShortBudgetQuery = lowerText.trim().length() <= 15;

        return hasActionOrQuestion || hasTimeKeyword || isShortBudgetQuery;
    }

}
