package com.example.spending_management_app.domain.usecase.ai;

import android.app.Activity;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import androidx.recyclerview.widget.RecyclerView;

import com.example.spending_management_app.presentation.dialog.AiChatBottomSheet;
import com.example.spending_management_app.utils.ExtractorHelper;
import com.example.spending_management_app.utils.TextFormatHelper;
import com.example.spending_management_app.domain.usecase.expense.ExpenseUseCase;

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

            // Use helper class for system instruction
            String instruction = AiSystemInstructions.getExpenseTrackingInstruction(
                currentDateInfo, currentDay, currentMonth, currentYear,
                yesterdayDay, yesterdayMonth, yesterdayYear
            );
            systemPart.put("text", instruction);
            systemParts.put(systemPart);
            systemInstruction.put("parts", systemParts);
            json.put("system_instruction", systemInstruction);

            // User message
            JSONArray contents = new JSONArray();
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
                    .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=AIzaSyAsDEIa1N6Dn_rCXYiRCXuUAY-E1DQ0Yv8")
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
}