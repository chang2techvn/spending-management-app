package com.example.spending_management_app.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BudgetAmountParser {
    private BudgetAmountParser() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static Long parseAmount(String text) {
        try {
            String lowerText = text.toLowerCase();
            
            // Pattern 1: "X tỷ Y triệu" or "X tỷ Y" (e.g., "8 tỷ 6" = 8,500,000,000 or "8 tỷ 500 triệu" = 8,500,000,000)
            Pattern tyTrieuPattern = Pattern.compile("(\\d+(?:[,.]\\d+)?)\\s*(?:tỷ|tỉ)\\s*(\\d+(?:[,.]\\d+)?)?\\s*(?:triệu|tr)?", Pattern.CASE_INSENSITIVE);
            Matcher tyTrieuMatcher = tyTrieuPattern.matcher(lowerText);
            if (tyTrieuMatcher.find()) {
                String tyStr = tyTrieuMatcher.group(1).replace(",", ".");
                String trieuStr = tyTrieuMatcher.group(2);
                
                double ty = Double.parseDouble(tyStr);
                long amount = (long) (ty * 1000000000); // tỷ = 1,000,000,000
                
                if (trieuStr != null && !trieuStr.isEmpty()) {
                    double trieu = Double.parseDouble(trieuStr.replace(",", "."));
                    // If trieu >= 100, it's already in triệu (e.g., "8 tỷ 500 triệu")
                    // If trieu < 100, it's in trăm triệu (e.g., "8 tỷ 5" = "8 tỷ 500 triệu")
                    if (trieu >= 100) {
                        amount += (long) (trieu * 1000000);
                    } else {
                        amount += (long) (trieu * 100000000); // trăm triệu
                    }
                }
                
                android.util.Log.d("AiChatBottomSheet", "Parsed amount (tỷ): " + amount + " from text: " + text);
                return amount;
            }
            
            // Pattern 2: "X triệu Y" or "X triệu Y nghìn" (e.g., "2 triệu 5" = 2,500,000 or "2 triệu 500 nghìn" = 2,500,000)
            Pattern trieuNghinPattern = Pattern.compile("(\\d+(?:[,.]\\d+)?)\\s*(?:triệu|tr)\\s*(\\d+(?:[,.]\\d+)?)?\\s*(?:nghìn|ngàn|k)?", Pattern.CASE_INSENSITIVE);
            Matcher trieuNghinMatcher = trieuNghinPattern.matcher(lowerText);
            if (trieuNghinMatcher.find()) {
                String trieuStr = trieuNghinMatcher.group(1).replace(",", ".");
                String nghinStr = trieuNghinMatcher.group(2);
                
                double trieu = Double.parseDouble(trieuStr);
                long amount = (long) (trieu * 1000000); // triệu = 1,000,000
                
                if (nghinStr != null && !nghinStr.isEmpty()) {
                    double nghin = Double.parseDouble(nghinStr.replace(",", "."));
                    // If nghin >= 100, it's already in nghìn (e.g., "2 triệu 500 nghìn")
                    // If nghin < 100, it's in trăm nghìn (e.g., "2 triệu 5" = "2 triệu 500 nghìn")
                    if (nghin >= 100) {
                        amount += (long) (nghin * 1000);
                    } else {
                        amount += (long) (nghin * 100000); // trăm nghìn
                    }
                }
                
                android.util.Log.d("AiChatBottomSheet", "Parsed amount (triệu): " + amount + " from text: " + text);
                return amount;
            }
            
            // Pattern 3: Simple format "X triệu", "Y nghìn", "Z k"
            Pattern simplePattern = Pattern.compile("(\\d+(?:[,.]\\d+)?)\\s*(tỷ|tỉ|triệu|tr|nghìn|ngàn|k)", Pattern.CASE_INSENSITIVE);
            Matcher simpleMatcher = simplePattern.matcher(lowerText);
            if (simpleMatcher.find()) {
                String amountStr = simpleMatcher.group(1).replace(",", ".");
                String unit = simpleMatcher.group(2).toLowerCase();
                
                double baseAmount = Double.parseDouble(amountStr);
                long amount;
                
                if (unit.contains("tỷ") || unit.contains("tỉ")) {
                    amount = (long) (baseAmount * 1000000000);
                } else if (unit.contains("triệu") || unit.contains("tr")) {
                    amount = (long) (baseAmount * 1000000);
                } else if (unit.contains("k") || unit.contains("nghìn") || unit.contains("ngàn")) {
                    amount = (long) (baseAmount * 1000);
                } else {
                    amount = (long) baseAmount;
                }
                
                android.util.Log.d("AiChatBottomSheet", "Parsed amount (simple): " + amount + " from text: " + text);
                return amount;
            }
            
            // Pattern 4: Just number (no unit)
            Pattern numberPattern = Pattern.compile("(\\d+(?:[,.]\\d+)?)");
            Matcher numberMatcher = numberPattern.matcher(lowerText);
            if (numberMatcher.find()) {
                String amountStr = numberMatcher.group(1).replace(",", ".");
                long amount = (long) Double.parseDouble(amountStr);
                android.util.Log.d("AiChatBottomSheet", "Parsed amount (no unit): " + amount + " from text: " + text);
                return amount;
            }
            
            return null;
        } catch (Exception e) {
            android.util.Log.e("AiChatBottomSheet", "Error parsing amount from: " + text, e);
            return null;
        }
    }

    public static long extractBudgetAmount(String text) {
        try {
            text = text.toLowerCase().trim();
            
            // Pattern 1: "X triệu" or "X tr"
            Pattern trPattern = Pattern.compile("(\\d+(?:[,.]\\d+)?)\\s*(?:triệu|tr)");
            Matcher trMatcher = trPattern.matcher(text);
            if (trMatcher.find()) {
                String numberStr = trMatcher.group(1).replace(",", ".").replace(".", "");
                double millions = Double.parseDouble(numberStr);
                return (long)(millions * 1000000);
            }
            
            // Pattern 2: "X nghìn" or "X k"
            Pattern kPattern = Pattern.compile("(\\d+(?:[,.]\\d+)?)\\s*(?:nghìn|k|ng)");
            Matcher kMatcher = kPattern.matcher(text);
            if (kMatcher.find()) {
                String numberStr = kMatcher.group(1).replace(",", ".").replace(".", "");
                double thousands = Double.parseDouble(numberStr);
                return (long)(thousands * 1000);
            }
            
            // Pattern 3: Plain number (should be large enough to be a budget)
            Pattern numberPattern = Pattern.compile("(\\d{5,})"); // At least 5 digits
            Matcher numberMatcher = numberPattern.matcher(text);
            if (numberMatcher.find()) {
                return Long.parseLong(numberMatcher.group(1));
            }
            
            return 0;
            
        } catch (Exception e) {
            android.util.Log.e("AiChatBottomSheet", "Error extracting budget amount", e);
            return 0;
        }
    }
}
