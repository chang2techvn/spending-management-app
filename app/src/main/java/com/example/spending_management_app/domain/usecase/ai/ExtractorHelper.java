package com.example.spending_management_app.domain.usecase.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ExtractorHelper {
    private ExtractorHelper() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static List<String> extractAllJsonFromText(String text) {
        android.util.Log.d("AiChatBottomSheet", "Extracting ALL JSON objects from text");
        List<String> jsonList = new ArrayList<>();
        
        int pos = 0;
        while (pos < text.length()) {
            int start = text.indexOf('{', pos);
            if (start == -1) break;
            
            // Find matching closing brace
            int braceCount = 0;
            int end = start;
            boolean inString = false;
            boolean escapeNext = false;
            
            for (int i = start; i < text.length(); i++) {
                char c = text.charAt(i);
                
                if (escapeNext) {
                    escapeNext = false;
                    continue;
                }
                
                if (c == '\\') {
                    escapeNext = true;
                    continue;
                }
                
                if (c == '"') {
                    inString = !inString;
                    continue;
                }
                
                if (!inString) {
                    if (c == '{') {
                        braceCount++;
                    } else if (c == '}') {
                        braceCount--;
                        if (braceCount == 0) {
                            end = i;
                            break;
                        }
                    }
                }
            }
            
            if (braceCount == 0 && end > start) {
                String json = text.substring(start, end + 1);
                android.util.Log.d("AiChatBottomSheet", "Found JSON object: " + json);
                jsonList.add(json);
                pos = end + 1;
            } else {
                pos = start + 1;
            }
        }
        
        android.util.Log.d("AiChatBottomSheet", "Total JSON objects found: " + jsonList.size());
        return jsonList;
    }

    public static String extractDisplayText(String text) {
        // Remove ALL JSON parts and markdown code blocks
        String result = text;

        // Remove markdown code blocks (```json ... ```)
        result = result.replaceAll("```json[\\s\\S]*?```", "");
        result = result.replaceAll("```[\\s\\S]*?```", "");

        // Remove all JSON objects
        List<String> allJsons = ExtractorHelper.extractAllJsonFromText(result);
        for (String json : allJsons) {
            result = result.replace(json, "");
        }

        // Clean up extra whitespace and newlines
        result = result.replaceAll("\\n{3,}", "\n\n"); // Max 2 consecutive newlines
        result = result.trim();

        return result.isEmpty() ? "✅ Đã xử lý!" : result;
    }

    public static String extractDescription(String text, String category, long amount) {
        String result = text;

        // Remove category
        result = result.replaceAll("(?i)" + Pattern.quote(category), "").trim();

        // Remove amount patterns
        result = result.replaceAll("\\d+[\\s]*(triệu|tr|ngàn|k|nghìn|n|đ|vnd)", "").trim();
        result = result.replaceAll("\\d+", "").trim();

        // Remove common keywords
        result = result.replaceAll("(?i)(chi tiêu|thêm|mua|đi|về)", "").trim();

        // Clean up extra spaces
        result = result.replaceAll("\\s+", " ").trim();

        return result;
    }

}
