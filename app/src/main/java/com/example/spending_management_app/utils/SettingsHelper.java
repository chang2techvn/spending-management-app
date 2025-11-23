package com.example.spending_management_app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Small helper to persist app settings like selected currency and exchange rates
 */
public final class SettingsHelper {

    private static final String KEY_SELECTED_CURRENCY = "selected_currency";
    private static final String KEY_EXCHANGE_RATE_PREFIX = "exchange_rate_"; // stored as VND per unit, e.g. 26000 for USD
    private static final String DEFAULT_CURRENCY = "VND";

    private SettingsHelper() { throw new UnsupportedOperationException("Utility class"); }

    public static void setSelectedCurrency(Context context, String currency) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putString(KEY_SELECTED_CURRENCY, currency).apply();
    }

    public static String getSelectedCurrency(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(KEY_SELECTED_CURRENCY, DEFAULT_CURRENCY);
    }

    public static void setExchangeRateVndPerUnit(Context context, String currency, double rateVndPerUnit) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putFloat(KEY_EXCHANGE_RATE_PREFIX + currency, (float) rateVndPerUnit).apply();
    }

    public static double getExchangeRateVndPerUnit(Context context, String currency) {
        if (currency == null) return 1.0;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (currency.equals(DEFAULT_CURRENCY)) return 1.0;
        float v = prefs.getFloat(KEY_EXCHANGE_RATE_PREFIX + currency, 0f);
        return v == 0f ? 0.0 : v;
    }

    /**
     * Convert amount from VND to selected currency
     * @param context Android context
     * @param vndAmount Amount in VND
     * @return Amount in selected currency
     */
    public static double convertFromVND(Context context, double vndAmount) {
        String selectedCurrency = getSelectedCurrency(context);
        double rateVndPerUnit = getExchangeRateVndPerUnit(context, selectedCurrency);

        if (selectedCurrency == null || DEFAULT_CURRENCY.equals(selectedCurrency) || rateVndPerUnit <= 0) {
            return vndAmount; // Return original VND amount
        }

        // Convert VND to target currency
        double result = vndAmount / rateVndPerUnit;
        android.util.Log.d("SettingsHelper", "Converting " + vndAmount + " VND to " + selectedCurrency + " using rate " + rateVndPerUnit + " -> " + result);
        return result;
    }
}
