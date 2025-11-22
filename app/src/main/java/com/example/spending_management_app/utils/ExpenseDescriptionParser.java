package com.example.spending_management_app.utils;

import java.util.regex.Pattern;

public final class ExpenseDescriptionParser {
    private ExpenseDescriptionParser() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static String extractDescriptionOffline(String text, String category, long amount) {
        String result = text;
        String lowerResult = result.toLowerCase();
        
        // Remove date keywords first
        result = result.replaceAll("(?i)(hôm qua|hôm kia|hôm nay)", "").trim();
        result = result.replaceAll("(?i)(ngày|tháng|năm)\\s*\\d+", "").trim();
        result = result.replaceAll("\\d{1,2}/\\d{1,2}(?:/\\d{4})?", "").trim();
        
        // Remove amount patterns (all formats: 100k, 2 triệu, 500 nghìn, 8 tỷ 6, etc.)
        result = result.replaceAll("(?i)\\d+[\\s,.]*(tỷ|tỉ)\\s*\\d*[\\s,.]*(triệu|tr|nghìn|ngàn|k)?", "").trim();
        result = result.replaceAll("(?i)\\d+[\\s,.]*(triệu|tr|nghìn|ngàn|nghin|ngan|k|đ|vnd|dong)", "").trim();
        result = result.replaceAll("\\d+[\\s,.]*\\d*", "").trim();
        
        // Remove category name if it appears
        if (category != null && !category.equals("Khác")) {
            result = result.replaceAll("(?i)" + Pattern.quote(category.toLowerCase()), "").trim();
        }
        
        // Remove common action verbs but keep the main object
        // Only remove these if they appear at the beginning
        result = result.replaceAll("^(?i)(tôi|mình|em|anh|chị)\\s+", "").trim();
        result = result.replaceAll("^(?i)(chi tiêu|thêm|đã)\\s+", "").trim();
        
        // Keep "mua" + object (e.g., "mua cá" -> "Mua cá")
        if (result.toLowerCase().startsWith("mua ") || 
            result.toLowerCase().startsWith("đổ ") ||
            result.toLowerCase().startsWith("ăn ") ||
            result.toLowerCase().startsWith("uống ")) {
            // Capitalize first letter
            result = result.substring(0, 1).toUpperCase() + result.substring(1);
        } else {
            // If no action verb, capitalize first letter
            if (!result.isEmpty()) {
                result = result.substring(0, 1).toUpperCase() + result.substring(1);
            }
        }
        
        // Clean up extra spaces and special characters
        result = result.replaceAll("\\s+", " ").trim();
        result = result.replaceAll("^[,.:;\\s]+", "").trim();
        result = result.replaceAll("[,.:;\\s]+$", "").trim();
        
        return result;
    }
    
}
