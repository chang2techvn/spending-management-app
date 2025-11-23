package com.example.spending_management_app.utils;

import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DateParser {
    private DateParser() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static Date parseDate(String text) {
        try {
            String lowerText = text.toLowerCase();
            Calendar cal = Calendar.getInstance();
            
            if (lowerText.contains("hôm qua")) {
                cal.add(Calendar.DAY_OF_MONTH, -1);
                return cal.getTime();
            } else if (lowerText.contains("hôm kia")) {
                cal.add(Calendar.DAY_OF_MONTH, -2);
                return cal.getTime();
            }
            
            // Pattern: dd/MM or dd/MM/yyyy
            Pattern datePattern = Pattern.compile("(\\d{1,2})/(\\d{1,2})(?:/(\\d{4}))?");
            Matcher dateMatcher = datePattern.matcher(text);
            
            if (dateMatcher.find()) {
                int day = Integer.parseInt(dateMatcher.group(1));
                int month = Integer.parseInt(dateMatcher.group(2)) - 1; // Calendar month is 0-based
                int year = cal.get(Calendar.YEAR);
                
                if (dateMatcher.group(3) != null) {
                    year = Integer.parseInt(dateMatcher.group(3));
                }
                
                cal.set(year, month, day);
                return cal.getTime();
            }
            
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public static int[] extractMonthYear(String text) {
        try {
            text = text.toLowerCase().trim();
            
            Calendar currentCal = Calendar.getInstance();
            int currentMonth = currentCal.get(Calendar.MONTH) + 1; // 0-based to 1-based
            int currentYear = currentCal.get(Calendar.YEAR);
            
            // Pattern 1: "tháng X" or "tháng X/YYYY"
            Pattern monthPattern = Pattern.compile("tháng\\s+(\\d{1,2})(?:/(\\d{4}))?");
            Matcher monthMatcher = monthPattern.matcher(text);
            if (monthMatcher.find()) {
                int month = Integer.parseInt(monthMatcher.group(1));
                int year = monthMatcher.group(2) != null ? 
                          Integer.parseInt(monthMatcher.group(2)) : currentYear;
                
                // If month is valid (1-12)
                if (month >= 1 && month <= 12) {
                    return new int[]{month, year};
                }
            }
            
            // Pattern 2: "X/YYYY" or "XX/YYYY"
            Pattern datePattern = Pattern.compile("(\\d{1,2})/(\\d{4})");
            Matcher dateMatcher = datePattern.matcher(text);
            if (dateMatcher.find()) {
                int month = Integer.parseInt(dateMatcher.group(1));
                int year = Integer.parseInt(dateMatcher.group(2));
                
                if (month >= 1 && month <= 12) {
                    return new int[]{month, year};
                }
            }
            
            // Pattern 3: "tháng này" - current month
            if (text.contains("tháng này") || text.contains("thang nay")) {
                return new int[]{currentMonth, currentYear};
            }
            
            // Pattern 4: "tháng sau" or "tháng tới" - next month
            if (text.contains("tháng sau") || text.contains("tháng tới") || 
                text.contains("thang sau") || text.contains("thang toi")) {
                currentCal.add(Calendar.MONTH, 1);
                return new int[]{currentCal.get(Calendar.MONTH) + 1, currentCal.get(Calendar.YEAR)};
            }
            
            // Default: current month
            return new int[]{currentMonth, currentYear};
            
        } catch (Exception e) {
            android.util.Log.e("AiChatBottomSheet", "Error extracting month/year", e);
            Calendar currentCal = Calendar.getInstance();
            return new int[]{currentCal.get(Calendar.MONTH) + 1, currentCal.get(Calendar.YEAR)};
        }
    }

    public static Date extractDateFromText(String text) {
        String lowerText = text.toLowerCase();
        Calendar cal = Calendar.getInstance();

        // Check for specific date patterns
        if (lowerText.contains("hôm qua") || lowerText.contains("yesterday")) {
            cal.add(Calendar.DAY_OF_MONTH, -1);
        } else if (lowerText.contains("hôm kia") || lowerText.contains("2 ngày trước")) {
            cal.add(Calendar.DAY_OF_MONTH, -2);
        } else if (lowerText.contains("tuần trước") || lowerText.contains("last week")) {
            cal.add(Calendar.DAY_OF_MONTH, -7);
        } else {
            // Try to find date pattern: "ngày 10/11" or "10/11" or "10-11"
            Pattern datePattern = Pattern.compile("(?:ngày\\s+)?(\\d{1,2})[/-](\\d{1,2})(?:[/-](\\d{2,4}))?");
            Matcher matcher = datePattern.matcher(lowerText);

            if (matcher.find()) {
                int day = Integer.parseInt(matcher.group(1));
                int month = Integer.parseInt(matcher.group(2));
                int year = cal.get(Calendar.YEAR);

                if (matcher.group(3) != null) {
                    year = Integer.parseInt(matcher.group(3));
                    if (year < 100) {
                        year += 2000;
                    }
                }

                cal.set(Calendar.YEAR, year);
                cal.set(Calendar.MONTH, month - 1);
                cal.set(Calendar.DAY_OF_MONTH, day);
            }
            // Default: today (no changes to cal)
        }

        // Set time to current time
        return cal.getTime();
    }
}
