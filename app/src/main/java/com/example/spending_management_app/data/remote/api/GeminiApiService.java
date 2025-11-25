package com.example.spending_management_app.data.remote.api;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.example.spending_management_app.BuildConfig;
import com.example.spending_management_app.R;
import com.example.spending_management_app.domain.usecase.ai.AiSystemInstructions;
import com.example.spending_management_app.utils.LocaleHelper;
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
     * Send a simple one-shot prompt to Gemini and get textual response.
     */
    public static void sendSimplePrompt(Context context, String prompt, AIResponseCallback callback) {
        OkHttpClient client = new OkHttpClient();
        Handler mainHandler = new Handler(Looper.getMainLooper());

        try {
            JSONObject json = new JSONObject();

            // Put prompt as user content
            JSONArray contents = new JSONArray();
            JSONObject userContent = new JSONObject();
            JSONArray userParts = new JSONArray();
            JSONObject userPart = new JSONObject();
            userPart.put("text", prompt);
            userParts.put(userPart);
            userContent.put("parts", userParts);
            userContent.put("role", "user");
            contents.put(userContent);
            json.put("contents", contents);

            RequestBody body = RequestBody.create(json.toString(), MediaType.parse("application/json"));
            Request request = new Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + BuildConfig.GEMINI_API_KEY)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    mainHandler.post(() -> callback.onFailure(context.getString(R.string.ai_connection_error)));
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

                            mainHandler.post(() -> callback.onSuccess(formattedText));
                        } catch (Exception e) {
                            mainHandler.post(() -> callback.onFailure(context.getString(R.string.ai_processing_error)));
                        }
                    } else {
                        mainHandler.post(() -> callback.onFailure(context.getString(R.string.ai_send_error) + " " + response.code()));
                    }
                }
            });
        } catch (Exception e) {
            mainHandler.post(() -> callback.onFailure(context.getString(R.string.ai_send_error)));
        }
    }
    
    /**
     * Send prompt to AI with budget context
     * 
     * @param context Application context for getting app language
     * @param userQuery The user's query
     * @param budgetContext The budget context data
     * @param messages The list of chat messages for conversation history
     * @param analyzingIndex The index of the analyzing message
     * @param callback Callback for handling response
     */
    public static void sendPromptWithBudgetContext(
            Context context,
            String userQuery, 
            String budgetContext,
            java.util.List<com.example.spending_management_app.presentation.dialog.AiChatBottomSheet.ChatMessage> messages,
            int analyzingIndex,
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

            // Get app language and currency
            String appLanguage = LocaleHelper.getLanguage(context);
            String appCurrency = "VND"; // Currently hardcoded, can be made configurable later

            String instruction = AiSystemInstructions.getBudgetAnalysisInstruction(currentDateInfo, budgetContext, appLanguage, appCurrency);

            systemPart.put("text", instruction);
            systemParts.put(systemPart);
            systemInstruction.put("parts", systemParts);
            json.put("system_instruction", systemInstruction);

            // Build conversation history
            JSONArray contents = new JSONArray();
            
            // Add previous messages (excluding the current analyzing message and welcome messages)
            for (int i = 0; i < analyzingIndex; i++) {
                com.example.spending_management_app.presentation.dialog.AiChatBottomSheet.ChatMessage msg = messages.get(i);
                // Skip welcome messages or system messages that are not part of conversation
                if (msg.message.startsWith("📊") || msg.message.startsWith("💰") || 
                    msg.message.startsWith("📅") || msg.message.contains("Đang phân tích") ||
                    msg.message.contains("Lỗi") || msg.message.contains("Offline")) {
                    continue;
                }
                
                JSONObject contentObj = new JSONObject();
                JSONArray parts = new JSONArray();
                JSONObject part = new JSONObject();
                part.put("text", msg.message);
                parts.put(part);
                contentObj.put("parts", parts);
                contentObj.put("role", msg.isUser ? "user" : "model");
                contents.put(contentObj);
            }
            
            // Add current user query
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
                    .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + BuildConfig.GEMINI_API_KEY)
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
