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

            // Pattern 3: "tháng này" - current month (Vietnamese)
            if (text.contains("tháng này") || text.contains("thang nay")) {
                return new int[]{currentMonth, currentYear};
            }

            // Pattern 4: "this month" - current month (English)
            if (text.contains("this month")) {
                return new int[]{currentMonth, currentYear};
            }

            // Pattern 5: "tháng trước" - previous month (Vietnamese)
            if (text.contains("tháng trước") || text.contains("thang truoc")) {
                currentCal.add(Calendar.MONTH, -1);
                return new int[]{currentCal.get(Calendar.MONTH) + 1, currentCal.get(Calendar.YEAR)};
            }

            // Pattern 6: "last month" - previous month (English)
            if (text.contains("last month")) {
                currentCal.add(Calendar.MONTH, -1);
                return new int[]{currentCal.get(Calendar.MONTH) + 1, currentCal.get(Calendar.YEAR)};
            }

            // Pattern 7: "tháng sau" or "tháng tới" - next month (Vietnamese)
            if (text.contains("tháng sau") || text.contains("tháng tới") ||
                text.contains("thang sau") || text.contains("thang toi")) {
                currentCal.add(Calendar.MONTH, 1);
                return new int[]{currentCal.get(Calendar.MONTH) + 1, currentCal.get(Calendar.YEAR)};
            }

            // Pattern 8: "next month" - next month (English)
            if (text.contains("next month")) {
                currentCal.add(Calendar.MONTH, 1);
                return new int[]{currentCal.get(Calendar.MONTH) + 1, currentCal.get(Calendar.YEAR)};
            }

            // Pattern 9: "month X" or "month X/YYYY" (English)
            Pattern englishMonthPattern = Pattern.compile("month\\s+(\\d{1,2})(?:/(\\d{4}))?");
            Matcher englishMonthMatcher = englishMonthPattern.matcher(text);
            if (englishMonthMatcher.find()) {
                int month = Integer.parseInt(englishMonthMatcher.group(1));
                int year = englishMonthMatcher.group(2) != null ?
                          Integer.parseInt(englishMonthMatcher.group(2)) : currentYear;

                // If month is valid (1-12)
                if (month >= 1 && month <= 12) {
                    return new int[]{month, year};
                }
            }

            // Default: current month
            return new int[]{currentMonth, currentYear};

        } catch (Exception e) {
            android.util.Log.e("DateParser", "Error extracting month/year", e);
            Calendar currentCal = Calendar.getInstance();
            return new int[]{currentCal.get(Calendar.MONTH) + 1, currentCal.get(Calendar.YEAR)};
        }
    }

    public static int extractYear(String text) {
        try {
            text = text.toLowerCase().trim();

            Calendar currentCal = Calendar.getInstance();
            int currentYear = currentCal.get(Calendar.YEAR);

            // Pattern 1: "năm XXXX" or "năm XXXX"
            Pattern yearPattern = Pattern.compile("năm\\s+(\\d{4})");
            Matcher yearMatcher = yearPattern.matcher(text);
            if (yearMatcher.find()) {
                int year = Integer.parseInt(yearMatcher.group(1));
                // Reasonable year range (2000-2050)
                if (year >= 2000 && year <= 2050) {
                    return year;
                }
            }

            // Pattern 2: "XXXX" (4 digits, likely a year)
            Pattern fourDigitPattern = Pattern.compile("\\b(\\d{4})\\b");
            Matcher fourDigitMatcher = fourDigitPattern.matcher(text);
            if (fourDigitMatcher.find()) {
                int year = Integer.parseInt(fourDigitMatcher.group(1));
                if (year >= 2000 && year <= 2050) {
                    return year;
                }
            }

            // Pattern 3: "năm này" - current year
            if (text.contains("năm này") || text.contains("nam nay")) {
                return currentYear;
            }

            // Pattern 4: "năm trước" - previous year
            if (text.contains("năm trước") || text.contains("nam truoc")) {
                return currentYear - 1;
            }

            // Pattern 5: "năm sau" or "năm tới" - next year
            if (text.contains("năm sau") || text.contains("năm tới") ||
                text.contains("nam sau") || text.contains("nam toi")) {
                return currentYear + 1;
            }

            // Pattern 6: "năm kia" - year before previous (2 years ago)
            if (text.contains("năm kia") || text.contains("nam kia")) {
                return currentYear - 2;
            }

            // Pattern 7: "năm ngoái" - last year
            if (text.contains("năm ngoái") || text.contains("nam ngoai")) {
                return currentYear - 1;
            }

            // Pattern 8: "năm tới" - next year (duplicate of pattern 5)
            if (text.contains("năm tới") || text.contains("nam toi")) {
                return currentYear + 1;
            }

            // Default: current year
            return currentYear;

        } catch (Exception e) {
            android.util.Log.e("DateParser", "Error extracting year", e);
            return Calendar.getInstance().get(Calendar.YEAR);
        }
    }

    public static Date extractDateFromText(String text) {
        String lowerText = text.toLowerCase();
        Calendar cal = Calendar.getInstance();

        // Check for specific date patterns
        if (lowerText.contains("hôm nay") || lowerText.contains("today")) {
            // Today - no change needed
            android.util.Log.d("DateParser", "Parsed: today");
        } else if (lowerText.contains("hôm qua") || lowerText.contains("yesterday")) {
            cal.add(Calendar.DAY_OF_MONTH, -1);
            android.util.Log.d("DateParser", "Parsed: yesterday");
        } else if (lowerText.contains("hôm kia") || lowerText.contains("2 ngày trước")) {
            cal.add(Calendar.DAY_OF_MONTH, -2);
            android.util.Log.d("DateParser", "Parsed: 2 days ago");
        } else if (lowerText.contains("hôm trước") || lowerText.contains("3 ngày trước")) {
            cal.add(Calendar.DAY_OF_MONTH, -3);
            android.util.Log.d("DateParser", "Parsed: 3 days ago");
        } else if (lowerText.contains("ngày mai") || lowerText.contains("tomorrow")) {
            cal.add(Calendar.DAY_OF_MONTH, 1);
            android.util.Log.d("DateParser", "Parsed: tomorrow");
        } else if (lowerText.contains("ngày kia") || lowerText.contains("2 ngày sau")) {
            cal.add(Calendar.DAY_OF_MONTH, 2);
            android.util.Log.d("DateParser", "Parsed: 2 days later");
        } else if (lowerText.contains("tuần trước") || lowerText.contains("last week")) {
            cal.add(Calendar.DAY_OF_MONTH, -7);
            android.util.Log.d("DateParser", "Parsed: last week");
        } else if (lowerText.contains("tuần sau") || lowerText.contains("next week")) {
            cal.add(Calendar.DAY_OF_MONTH, 7);
            android.util.Log.d("DateParser", "Parsed: next week");
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
                android.util.Log.d("DateParser", "Parsed date pattern: " + day + "/" + month + "/" + year);
            } else {
                android.util.Log.d("DateParser", "No date pattern found, defaulting to today");
                // Default: today (no changes to cal)
            }
        }

        // Set time to current time
        Date result = cal.getTime();
        android.util.Log.d("DateParser", "Final extracted date: " + result);
        return result;
    }
}
