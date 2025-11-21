package com.example.spending_management_app.utils;

import java.util.ArrayList;
import java.util.List;

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

}
