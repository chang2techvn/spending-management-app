package com.example.spending_management_app.utils;

public final class TextFormatHelper {
    private TextFormatHelper() {
        throw new UnsupportedOperationException("Utility class");
    }

    // Helper method để format markdown text thành plain text dễ đọc
    public static String formatMarkdownText(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        try {
            // Xóa bold markdown (**text**)
            text = text.replaceAll("\\*\\*(.*?)\\*\\*", "$1");
            
            // Xóa italic markdown (*text*)
            text = text.replaceAll("(?<!\\*)\\*(?!\\*)([^*]+)\\*(?!\\*)", "$1");
            
            // Xóa heading markdown (###, ##, #)
            text = text.replaceAll("^#{1,6}\\s+", "");
            text = text.replaceAll("\\n#{1,6}\\s+", "\n");
            
            // Giữ nguyên xuống dòng - KHÔNG xóa
            // Chỉ chuẩn hóa: tối đa 2 xuống dòng liên tiếp
            text = text.replaceAll("\\n{3,}", "\n\n");
            
            // Xóa các asterisk đơn lẻ còn sót lại
            text = text.replaceAll("(?<!\\S)\\*(?!\\S)", "");
            
            // Trim whitespace đầu cuối
            text = text.trim();
            
            android.util.Log.d("AiChatBottomSheet", "Formatted text: " + text);
            
            return text;
            
        } catch (Exception e) {
            android.util.Log.e("AiChatBottomSheet", "Error formatting markdown", e);
            return text; // Return original if error
        }
    }
}
