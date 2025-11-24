package com.example.spending_management_app.utils;

import android.content.Context;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Utility class for formatting currency amounts in the selected currency
 */
public final class CurrencyFormatter {

    private CurrencyFormatter() { throw new UnsupportedOperationException("Utility class"); }

    /**
     * Format amount in VND to selected currency with proper formatting
     * @param context Application context
     * @param vndAmount Amount in VND
     * @return Formatted string like "1,234 USD" or "300,000 VND"
     */
    public static String formatCurrency(Context context, double vndAmount) {
        String selectedCurrency = SettingsHelper.getSelectedCurrency(context);
        double convertedAmount = SettingsHelper.convertFromVND(context, vndAmount);
        // Always format absolute value; caller should add sign if needed
        convertedAmount = Math.abs(convertedAmount);

        NumberFormat formatter = NumberFormat.getNumberInstance(Locale.getDefault());
        formatter.setMaximumFractionDigits(0); // No decimals for VND-like formatting

        String symbol = getCurrencySymbol(selectedCurrency);
        return formatter.format(convertedAmount) + " " + symbol;
    }

    /**
     * Format amount in VND to selected currency with short formatting (K, M suffixes)
     * @param context Application context
     * @param vndAmount Amount in VND
     * @return Formatted string like "1.2K USD" or "300K VND"
     */
    public static String formatCurrencyShort(Context context, double vndAmount) {
        String selectedCurrency = SettingsHelper.getSelectedCurrency(context);
        double convertedAmount = SettingsHelper.convertFromVND(context, vndAmount);
        // Use absolute value for short representation
        convertedAmount = Math.abs(convertedAmount);

        String suffix = "";
        if (convertedAmount >= 1000000) {
            convertedAmount /= 1000000;
            suffix = "M";
        } else if (convertedAmount >= 1000) {
            convertedAmount /= 1000;
            suffix = "K";
        }

        NumberFormat formatter = NumberFormat.getNumberInstance(Locale.getDefault());
        if (suffix.isEmpty()) {
            formatter.setMaximumFractionDigits(0);
        } else {
            formatter.setMaximumFractionDigits(1);
        }

        String symbol = getCurrencySymbol(selectedCurrency);
        return formatter.format(convertedAmount) + suffix + " " + symbol;
    }

    /**
     * Get currency symbol for display
     * @param currency Currency code
     * @return Currency symbol
     */
    private static String getCurrencySymbol(String currency) {
        switch (currency) {
            case "USD":
                return "USD";
            case "EUR":
                return "EUR";
            case "JPY":
                return "JPY";
            case "VND":
            default:
                return "VND";
        }
    }

    /**
     * Get the selected currency symbol
     * @param context Application context
     * @return Currency symbol
     */
    public static String getSelectedCurrencySymbol(Context context) {
        String currency = SettingsHelper.getSelectedCurrency(context);
        return getCurrencySymbol(currency);
    }
}