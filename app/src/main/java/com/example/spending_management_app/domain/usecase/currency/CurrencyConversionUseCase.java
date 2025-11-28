package com.example.spending_management_app.domain.usecase.currency;

import android.content.Context;
import android.util.Log;

import com.example.spending_management_app.R;

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
     * Supports all currencies by calculating cross-rates when needed.
     */
    public static void fetchAndStoreRate(Context context, String targetCurrency, Callback callback) {
        if (context == null || targetCurrency == null) {
            if (callback != null) callback.onResult(false, 0.0, context.getString(R.string.invalid_args));
            return;
        }

        // Run HTTP request on background thread
        new Thread(() -> {
            try {
                // Use USD as base currency for reliable API response
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

                        double rateVndPerUnit = 0.0;

                        if ("USD".equalsIgnoreCase(targetCurrency)) {
                            // Direct rate for USD
                            if (rates.has("VND")) {
                                rateVndPerUnit = rates.getDouble("VND");
                                Log.d(TAG, "Fetched direct rate for USD: " + rateVndPerUnit + " VND per USD");
                            }
                        } else {
                            // Calculate cross-rate for other currencies
                            // Rate = (USD per targetCurrency) * (VND per USD)
                            if (rates.has(targetCurrency.toUpperCase()) && rates.has("VND")) {
                                double usdPerTarget = rates.getDouble(targetCurrency.toUpperCase());
                                double vndPerUsd = rates.getDouble("VND");
                                rateVndPerUnit = vndPerUsd / usdPerTarget;
                                Log.d(TAG, "Calculated cross-rate for " + targetCurrency + ": " + rateVndPerUnit + " VND per " + targetCurrency +
                                      " (USD/" + targetCurrency + ": " + usdPerTarget + ", VND/USD: " + vndPerUsd + ")");
                            } else {
                                // Currency not supported by API
                                if (callback != null) callback.onResult(false, 0.0, context.getString(R.string.currency_not_supported, targetCurrency.toUpperCase()));
                                return;
                            }
                        }

                        if (rateVndPerUnit > 0.0) {
                            // Store the rate
                            com.example.spending_management_app.utils.SettingsHelper.setExchangeRateVndPerUnit(context, targetCurrency, rateVndPerUnit);
                            com.example.spending_management_app.utils.SettingsHelper.setSelectedCurrency(context, targetCurrency);

                            if (callback != null) callback.onResult(true, rateVndPerUnit, "OK");
                            return;
                        }
                    }

                    if (callback != null) callback.onResult(false, 0.0, context.getString(R.string.invalid_api_response, response.toString()));
                } else {
                    if (callback != null) callback.onResult(false, 0.0, context.getString(R.string.http_error, responseCode));
                }

                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Error fetching rate from API", e);
                if (callback != null) callback.onResult(false, 0.0, context.getString(R.string.network_error, e.getMessage()));
            }
        }).start();
    }
}
