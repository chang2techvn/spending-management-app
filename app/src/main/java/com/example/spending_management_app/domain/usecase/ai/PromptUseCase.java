package com.example.spending_management_app.domain.usecase.ai;

import android.app.Activity;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import androidx.recyclerview.widget.RecyclerView;

import com.example.spending_management_app.BuildConfig;
import com.example.spending_management_app.domain.usecase.expense.ExpenseBulkUseCase;
import com.example.spending_management_app.domain.repository.ExpenseRepository;
import com.example.spending_management_app.data.local.database.AppDatabase;
import com.example.spending_management_app.data.repository.ExpenseRepositoryImpl;
import com.example.spending_management_app.domain.usecase.expense.ExpenseUseCase;
import com.example.spending_management_app.presentation.dialog.AiChatBottomSheet;
import com.example.spending_management_app.utils.ExtractorHelper;
import com.example.spending_management_app.utils.LocaleHelper;
import com.example.spending_management_app.utils.TextFormatHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import java.io.IOException;

/**
 * Service class for handling AI prompt sending operations
 */
public class PromptUseCase {

    private final ExpenseUseCase expenseUseCase;

    public PromptUseCase(ExpenseUseCase expenseUseCase) {
        this.expenseUseCase = expenseUseCase;
    }

    /**
     * Send a prompt to AI for expense tracking
     */
    public void sendPromptToAI(String text, Activity activity, List<AiChatBottomSheet.ChatMessage> messages,
                               AiChatBottomSheet.ChatAdapter chatAdapter, RecyclerView messagesRecycler,
                               TextToSpeech textToSpeech, Runnable updateNetworkStatusCallback) {

        // Check if this is a delete request - handle locally instead of sending to AI
        if (isDeleteRequest(text)) {
            android.util.Log.d("PromptService", "Detected delete request, routing to ExpenseBulkUseCase");
            handleDeleteRequestLocally(text, activity, messages, chatAdapter, messagesRecycler, updateNetworkStatusCallback);
            return;
        }

        // Add temporary "Đang phân tích..." message
        int analyzingIndex = messages.size();
        messages.add(new AiChatBottomSheet.ChatMessage("Đang phân tích...", false, "Bây giờ"));
        chatAdapter.notifyItemInserted(messages.size() - 1);
        messagesRecycler.smoothScrollToPosition(messages.size() - 1);

        // Prepare JSON for Gemini API with system instruction
        try {
            JSONObject json = new JSONObject();

            // Get current date for AI context
            java.util.Calendar currentCalendar = java.util.Calendar.getInstance();
            int currentDay = currentCalendar.get(java.util.Calendar.DAY_OF_MONTH);
            int currentMonth = currentCalendar.get(java.util.Calendar.MONTH) + 1; // Calendar.MONTH is 0-based
            int currentYear = currentCalendar.get(java.util.Calendar.YEAR);

            // Calculate yesterday's date
            java.util.Calendar yesterdayCalendar = java.util.Calendar.getInstance();
            yesterdayCalendar.add(java.util.Calendar.DAY_OF_MONTH, -1);
            int yesterdayDay = yesterdayCalendar.get(java.util.Calendar.DAY_OF_MONTH);
            int yesterdayMonth = yesterdayCalendar.get(java.util.Calendar.MONTH) + 1;
            int yesterdayYear = yesterdayCalendar.get(java.util.Calendar.YEAR);

            String currentDateInfo = String.format("Hôm nay là ngày %d/%d/%d", currentDay, currentMonth, currentYear);

            // System instruction
            JSONObject systemInstruction = new JSONObject();
            JSONArray systemParts = new JSONArray();
            JSONObject systemPart = new JSONObject();

            // Get app language and currency
            String appLanguage = LocaleHelper.getLanguage(activity.getApplicationContext());
            String appCurrency = "VND"; // Currently hardcoded, can be made configurable later

            // Use helper class for system instruction
            String instruction = AiSystemInstructions.getExpenseTrackingInstruction(
                currentDateInfo, currentDay, currentMonth, currentYear,
                yesterdayDay, yesterdayMonth, yesterdayYear, appLanguage, appCurrency
            );
            systemPart.put("text", instruction);
            systemParts.put(systemPart);
            systemInstruction.put("parts", systemParts);
            json.put("system_instruction", systemInstruction);

            // Build conversation history
            JSONArray contents = new JSONArray();
            
            // Add previous messages (excluding the current analyzing message and welcome messages)
            for (int i = 0; i < analyzingIndex; i++) {
                AiChatBottomSheet.ChatMessage msg = messages.get(i);
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
            
            // Add current user message
            JSONObject userContent = new JSONObject();
            JSONArray userParts = new JSONArray();
            JSONObject userPart = new JSONObject();
            userPart.put("text", text);
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

            OkHttpClient client = new OkHttpClient();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    activity.runOnUiThread(() -> {
                        // Replace analyzing message with error
                        messages.set(analyzingIndex, new AiChatBottomSheet.ChatMessage("Lỗi kết nối AI.", false, "Bây giờ"));
                        chatAdapter.notifyItemChanged(analyzingIndex);
                        // Update network status
                        updateNetworkStatusCallback.run();
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

                            // Check if response contains JSON - extract ALL JSON objects
                            List<String> allJsonParts = ExtractorHelper.extractAllJsonFromText(aiText);
                            String displayText = ExtractorHelper.extractDisplayText(aiText);

                            android.util.Log.d("PromptService", "AI full response: " + aiText);
                            android.util.Log.d("PromptService", "Number of JSON objects found: " + allJsonParts.size());
                            android.util.Log.d("PromptService", "Display text: " + displayText);

                            activity.runOnUiThread(() -> {
                                // Replace analyzing message with display text
                                android.util.Log.d("PromptService", "Updating message at index: " + analyzingIndex + " with: " + displayText);

                                // Format markdown text để dễ đọc hơn
                                String formattedDisplayText = TextFormatHelper.formatMarkdownText(displayText);

                                messages.set(analyzingIndex, new AiChatBottomSheet.ChatMessage(formattedDisplayText, false, "Bây giờ"));
                                chatAdapter.notifyItemChanged(analyzingIndex);
                                Log.d("PromptService", "AI response: " + formattedDisplayText);

                                messagesRecycler.smoothScrollToPosition(messages.size() - 1);
                                textToSpeech.speak(formattedDisplayText, TextToSpeech.QUEUE_FLUSH, null, null);

                                // Process ALL JSON objects found
                                if (!allJsonParts.isEmpty()) {
                                    android.util.Log.d("PromptService", "Processing " + allJsonParts.size() + " JSON objects");

                                    for (String jsonPart : allJsonParts) {
                                        try {
                                            android.util.Log.d("PromptService", "Routing to saveExpenseDirectly");
                                            expenseUseCase.saveExpenseDirectly(jsonPart, activity, messages, chatAdapter, messagesRecycler);
                                        } catch (Exception e) {
                                            android.util.Log.e("PromptService", "Error processing JSON: " + jsonPart, e);
                                        }
                                    }
                                } else {
                                    android.util.Log.d("PromptService", "No JSON found in AI response");
                                }

                                // Update network status after successful response
                                updateNetworkStatusCallback.run();
                            });
                        } catch (Exception e) {
                            activity.runOnUiThread(() -> {
                                // Replace analyzing message with error
                                messages.set(analyzingIndex, new AiChatBottomSheet.ChatMessage("Lỗi xử lý phản hồi AI.", false, "Bây giờ"));
                                chatAdapter.notifyItemChanged(analyzingIndex);
                                updateNetworkStatusCallback.run();
                            });
                        }
                    } else {
                        activity.runOnUiThread(() -> {
                            // Replace analyzing message with error
                            messages.set(analyzingIndex, new AiChatBottomSheet.ChatMessage("Lỗi từ AI: " + response.code(), false, "Bây giờ"));
                            chatAdapter.notifyItemChanged(analyzingIndex);
                            updateNetworkStatusCallback.run();
                        });
                    }
                }
            });
        } catch (Exception e) {
            // Replace analyzing message with error
            messages.set(analyzingIndex, new AiChatBottomSheet.ChatMessage("Lỗi gửi tin nhắn.", false, "Bây giờ"));
            chatAdapter.notifyItemChanged(analyzingIndex);
        }
    }

    /**
     * Check if the user input is a delete request
     */
    private boolean isDeleteRequest(String text) {
        String lowerText = text.toLowerCase();
        return lowerText.contains("xóa") || lowerText.contains("xoá") || lowerText.contains("xoa") ||
               lowerText.contains("delete") || lowerText.contains("remove");
    }

    /**
     * Handle delete requests locally using ExpenseBulkUseCase
     */
    private void handleDeleteRequestLocally(String text, Activity activity, List<AiChatBottomSheet.ChatMessage> messages,
                                           AiChatBottomSheet.ChatAdapter chatAdapter, RecyclerView messagesRecycler,
                                           Runnable updateNetworkStatusCallback) {

        android.util.Log.d("PromptService", "Handling delete request locally: " + text);

        // Initialize ExpenseBulkUseCase with proper context
        AppDatabase appDatabase = AppDatabase.getInstance(activity.getApplicationContext());
        ExpenseRepository expenseRepository = new ExpenseRepositoryImpl(appDatabase);
        ExpenseBulkUseCase bulkUseCase = new ExpenseBulkUseCase(expenseRepository);

        // Create refresh callbacks
        Runnable refreshHomeFragment = () -> {
            // This will be called by ExpenseBulkUseCase
            android.util.Log.d("PromptService", "Refresh home fragment called");
        };

        Runnable refreshExpenseWelcomeMessage = () -> {
            // This will be called by ExpenseBulkUseCase
            android.util.Log.d("PromptService", "Refresh expense welcome message called");
        };

        // Call ExpenseBulkUseCase to handle the delete request
        bulkUseCase.handleExpenseBulkRequest(text, activity.getApplicationContext(), activity, messages,
                                           chatAdapter, messagesRecycler, refreshHomeFragment, refreshExpenseWelcomeMessage);

        // Update network status
        updateNetworkStatusCallback.run();
    }
}