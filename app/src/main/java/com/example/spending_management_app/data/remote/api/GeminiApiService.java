package com.example.spending_management_app.data.remote.api;

import android.os.Handler;
import android.os.Looper;

import com.example.spending_management_app.domain.usecase.ai.AiSystemInstructions;
import com.example.spending_management_app.utils.TextFormatHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Service class for Gemini AI API interactions
 * Handles budget analysis requests with proper callback pattern
 */
public final class GeminiApiService {
    
    // Private constructor to prevent instantiation
    private GeminiApiService() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    /**
     * Callback interface for AI response
     */
    public interface AIResponseCallback {
        void onSuccess(String formattedResponse);
        void onFailure(String errorMessage);
    }
    
    /**
     * Send prompt to AI with budget context
     * 
     * @param userQuery The user's query
     * @param budgetContext The budget context data
     * @param callback Callback for handling response
     */
    public static void sendPromptWithBudgetContext(
            String userQuery, 
            String budgetContext,
            AIResponseCallback callback) {
        
        OkHttpClient client = new OkHttpClient();
        Handler mainHandler = new Handler(Looper.getMainLooper());
        
        try {
            JSONObject json = new JSONObject();

            // Get current date for AI context
            java.util.Calendar currentCalendar = java.util.Calendar.getInstance();
            int currentDay = currentCalendar.get(java.util.Calendar.DAY_OF_MONTH);
            int currentMonth = currentCalendar.get(java.util.Calendar.MONTH) + 1;
            int currentYear = currentCalendar.get(java.util.Calendar.YEAR);
            String currentDateInfo = String.format("Hôm nay là ngày %d/%d/%d", currentDay, currentMonth, currentYear);

            // System instruction for budget analysis
            JSONObject systemInstruction = new JSONObject();
            JSONArray systemParts = new JSONArray();
            JSONObject systemPart = new JSONObject();

            String instruction = AiSystemInstructions.getBudgetAnalysisInstruction(currentDateInfo, budgetContext);

            systemPart.put("text", instruction);
            systemParts.put(systemPart);
            systemInstruction.put("parts", systemParts);
            json.put("system_instruction", systemInstruction);

            // User message
            JSONArray contents = new JSONArray();
            JSONObject userContent = new JSONObject();
            JSONArray userParts = new JSONArray();
            JSONObject userPart = new JSONObject();
            userPart.put("text", userQuery);
            userParts.put(userPart);
            userContent.put("parts", userParts);
            userContent.put("role", "user");
            contents.put(userContent);
            json.put("contents", contents);

            RequestBody body = RequestBody.create(json.toString(), MediaType.parse("application/json"));
            Request request = new Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=AIzaSyAsDEIa1N6Dn_rCXYiRCXuUAY-E1DQ0Yv8")
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    mainHandler.post(() -> {
                        callback.onFailure("Lỗi kết nối AI.");
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        try {
                            String responseBody = response.body().string();
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            JSONArray candidates = jsonResponse.getJSONArray("candidates");
                            JSONObject candidate = candidates.getJSONObject(0);
                            JSONObject content = candidate.getJSONObject("content");
                            JSONArray parts = content.getJSONArray("parts");
                            String aiText = parts.getJSONObject(0).getString("text").trim();

                            String formattedText = TextFormatHelper.formatMarkdownText(aiText);

                            mainHandler.post(() -> {
                                callback.onSuccess(formattedText);
                            });
                        } catch (Exception e) {
                            mainHandler.post(() -> {
                                callback.onFailure("Lỗi xử lý phản hồi AI.");
                            });
                        }
                    } else {
                        mainHandler.post(() -> {
                            callback.onFailure("Lỗi từ AI: " + response.code());
                        });
                    }
                }
            });
        } catch (Exception e) {
            mainHandler.post(() -> {
                callback.onFailure("Lỗi gửi tin nhắn.");
            });
        }
    }
}
