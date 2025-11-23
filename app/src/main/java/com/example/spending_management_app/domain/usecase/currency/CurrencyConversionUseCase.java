package com.example.spending_management_app.domain.usecase.currency;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Use case to fetch exchange rate for a given currency using exchangerate.host API and store it.
 */
public class CurrencyConversionUseCase {

    private static final String TAG = "CurrencyConversionUC";

    public interface Callback {
        void onResult(boolean success, double rateVndPerUnit, String message);
    }

    /**
     * Fetch today's rate of targetCurrency in VND from exchangerate.host API and store it.
     * Currently supports USD only, as the API is USD-based.
     */
    public static void fetchAndStoreRate(Context context, String targetCurrency, Callback callback) {
        if (context == null || targetCurrency == null) {
            if (callback != null) callback.onResult(false, 0.0, "Invalid args");
            return;
        }

        // Only support USD for now
        if (!"USD".equalsIgnoreCase(targetCurrency)) {
            if (callback != null) callback.onResult(false, 0.0, "Only USD supported via API");
            return;
        }

        // Run HTTP request on background thread
        new Thread(() -> {
            try {
                URL url = new URL("https://api.exchangerate-api.com/v4/latest/USD");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "SpendingManagementApp/1.0");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    in.close();

                    // Parse JSON
                    JSONObject json = new JSONObject(response.toString());
                    Log.d(TAG, "API response: " + response.toString());
                    if (json.has("rates")) {
                        JSONObject rates = json.getJSONObject("rates");
                        if (rates.has("VND")) {
                            double rateVndPerUnit = rates.getDouble("VND");
                            Log.d(TAG, "Fetched rate for USD: " + rateVndPerUnit + " VND per unit from API");

                            // Store the rate
                            com.example.spending_management_app.utils.SettingsHelper.setExchangeRateVndPerUnit(context, targetCurrency, rateVndPerUnit);
                            com.example.spending_management_app.utils.SettingsHelper.setSelectedCurrency(context, targetCurrency);

                            if (callback != null) callback.onResult(true, rateVndPerUnit, "OK");
                            return;
                        }
                    }

                    if (callback != null) callback.onResult(false, 0.0, "Invalid API response: " + response.toString());
                } else {
                    if (callback != null) callback.onResult(false, 0.0, "HTTP error: " + responseCode);
                }

                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Error fetching rate from API", e);
                if (callback != null) callback.onResult(false, 0.0, "Network error: " + e.getMessage());
            }
        }).start();
    }
}
