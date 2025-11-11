package com.example.spending_management_app.ui;

import static android.app.Activity.RESULT_OK;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import android.speech.RecognizerIntent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.spending_management_app.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.snackbar.Snackbar;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import com.example.spending_management_app.database.AppDatabase;
import com.example.spending_management_app.database.BudgetEntity;
import com.example.spending_management_app.database.TransactionEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.util.concurrent.Executors;
import java.text.NumberFormat;

public class AiChatBottomSheet extends DialogFragment {

    private static final int VOICE_REQUEST_CODE = 1001;

    @Override
    public int getTheme() {
        return R.style.RoundedDialog;
    }

    private RecyclerView messagesRecycler;
    private EditText messageInput;
    private ImageButton sendButton;
    private ImageButton closeButton;
    private ImageButton microBtn;

    private List<ChatMessage> messages;
    private ChatAdapter chatAdapter;
    private TextToSpeech textToSpeech;
    private OkHttpClient client;
    private String spokenText = "";

    public void setSpokenText(String text) {
        this.spokenText = text;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        View view = inflater.inflate(R.layout.bottom_sheet_ai_chat, container, false);

        messagesRecycler = view.findViewById(R.id.messages_recycler);
        messageInput = view.findViewById(R.id.message_input);
        sendButton = view.findViewById(R.id.send_button);
        closeButton = view.findViewById(R.id.close_button);
        microBtn = view.findViewById(R.id.microBtn);


        // Initialize TTS and HTTP client
        textToSpeech = new TextToSpeech(getContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(Locale.getDefault());
            }
        });
        client = new OkHttpClient();

        setupMessages();
        setupListeners();

        // If spoken text, send to AI
        if (!spokenText.isEmpty()) {
            android.util.Log.d("AiChatBottomSheet", "Sending spoken text to AI: " + spokenText);
            messages.add(new ChatMessage(spokenText, true, "Bây giờ"));
            chatAdapter.notifyItemInserted(messages.size() - 1);
            messagesRecycler.smoothScrollToPosition(messages.size() - 1);
            sendToAI(spokenText);
            spokenText = ""; // Reset
        }

        // Check for voice input from arguments
        Bundle args = getArguments();
        if (args != null && args.containsKey("voice_input")) {
            String voiceText = args.getString("voice_input");
            if (voiceText != null && !voiceText.isEmpty()) {
                android.util.Log.d("AiChatBottomSheet", "Voice input from args: " + voiceText);
                // Add voice message to chat
                messages.add(new ChatMessage(voiceText, true, "Bây giờ"));
                chatAdapter.notifyItemInserted(messages.size() - 1);
                messagesRecycler.smoothScrollToPosition(messages.size() - 1);
                // Process AI response
                sendToAI(voiceText);
            }
        } else if (args != null && args.containsKey("initial_prompt")) {
            String prompt = args.getString("initial_prompt");
            if (prompt != null && !prompt.isEmpty()) {
                android.util.Log.d("AiChatBottomSheet", "Initial prompt from args: " + prompt);
                // Decide what user-visible message to show in the chat based on the prompt
                // If the user requested adding an expense, show "Thiết lập chi tiêu tháng này"
                // If the user requested budget management, keep "Thiết lập ngân sách tháng này"
                String lower = prompt.toLowerCase();
                String userVisibleMessage;
                if (lower.contains("chi tiêu") || lower.contains("thêm chi tiêu") || lower.contains("chi tieu")) {
                    userVisibleMessage = "Thêm chi tiêu mới";
                } else if (lower.contains("ngân sách") || lower.contains("thiet lap ngan sach") || lower.contains("thiết lập ngân sách")) {
                    userVisibleMessage = "Thiết lập ngân sách tháng này";
                } else {
                    // Fallback: show the prompt itself (trimmed) so it's clear what the user requested
                    userVisibleMessage = prompt.trim();
                }

                // Add the determined message to the chat and process the original prompt
                messages.add(new ChatMessage(userVisibleMessage, true, "Bây giờ"));
                chatAdapter.notifyItemInserted(messages.size() - 1);
                messagesRecycler.smoothScrollToPosition(messages.size() - 1);
                // Process AI response with the actual prompt
                sendToAI(prompt);
            }
        }

        return view;
    }
    


    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            getDialog().getWindow().setGravity(android.view.Gravity.BOTTOM);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VOICE_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                String spokenText = results.get(0);
                // Add to messages and send to AI, tái sử dụng logic từ MainActivity
                messages.add(new ChatMessage(spokenText, true, "Bây giờ"));
                chatAdapter.notifyItemInserted(messages.size() - 1);
                messagesRecycler.smoothScrollToPosition(messages.size() - 1);
                sendToAI(spokenText);
            }
        }
    }

    private void setupMessages() {
        messages = new ArrayList<>();
        messages.add(new ChatMessage("Chào bạn! Tôi có thể giúp bạn ghi lại chi tiêu. Hãy nói cho tôi biết hôm nay bạn đã chi tiêu gì nhé!", false, "9:00"));

        chatAdapter = new ChatAdapter(messages);
        messagesRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        messagesRecycler.setAdapter(chatAdapter);
    }

    private void setupListeners() {
        sendButton.setOnClickListener(v -> {
            String message = messageInput.getText().toString().trim();
            if (!message.isEmpty()) {
                
                messages.add(new ChatMessage(message, true, "Bây giờ"));
                chatAdapter.notifyItemInserted(messages.size() - 1);
                messagesRecycler.smoothScrollToPosition(messages.size() - 1);
                sendToAI(message);
                messageInput.setText("");
            }
        });

        microBtn.setOnClickListener(v -> startVoiceRecognition());

        closeButton.setOnClickListener(v -> dismiss());
    }

    private void startVoiceRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN");
        intent.putExtra(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES, new String[]{"en-US", "vi-VN"});
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Nói gì đó...");
        try {
            startActivityForResult(intent, VOICE_REQUEST_CODE);
        } catch (Exception e) {
            showTopToast("Thiết bị không hỗ trợ nhận diện giọng nói", Toast.LENGTH_SHORT);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (textToSpeech != null) {
            textToSpeech.shutdown();
        }
    }

    private void sendToAI(String text) {
        // Check if user is requesting budget management like the button
        if (text.toLowerCase().contains("ngân sách") && (text.toLowerCase().contains("tháng") || text.toLowerCase().contains("thiết lập") || text.toLowerCase().contains("hiện tại") || text.toLowerCase().contains("bao nhiêu"))) {
            // Handle like the button: query DB and create prompt
            Executors.newSingleThreadExecutor().execute(() -> {
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.DAY_OF_MONTH, 1);
                Date startOfMonth = cal.getTime();
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
                Date endOfMonth = cal.getTime();

                List<BudgetEntity> monthlyBudgets = AppDatabase.getInstance(getContext()).budgetDao().getBudgetsByDateRange(startOfMonth, endOfMonth);

                getActivity().runOnUiThread(() -> {
                    String prompt;
                    if (monthlyBudgets == null || monthlyBudgets.isEmpty()) {
                        // No budget set: ask AI to prompt user naturally and request an amount
                        prompt = "Người dùng chưa thiết lập ngân sách cho tháng này. Hãy hỏi họ bằng một câu tự nhiên, đa dạng (không quá cứng nhắc) để yêu cầu họ nhập số ngân sách cho tháng này. Sau câu hỏi, khi người dùng trả lời, hãy trả về JSON dạng {\"action\":\"set_budget\", \"amount\": số, \"currency\": \"VND\"} để app có thể lưu." 
                                + " Hãy đưa ra một câu hỏi kèm theo gợi ý ngắn nếu cần.";
                    } else {
                        BudgetEntity budget = monthlyBudgets.get(0);
                        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", new Locale("vi", "VN"));
                        String dateStr = budget.getDate() != null ? dateFormat.format(budget.getDate()) : "(không xác định)";
                        prompt = "Người dùng đã thiết lập ngân sách tháng này vào ngày " + dateStr + ". Số tiền hiện tại là "
                                + String.format(Locale.getDefault(), "%,d", budget.getMonthlyLimit()) + " VND. Hãy trả lời người dùng bằng ngôn ngữ tự nhiên (có thể hài hước), thông báo số ngân sách hiện tại và hỏi xem họ có muốn thay đổi không. Nếu user muốn thay đổi và cung cấp số mới, trả về JSON {\"action\":\"update_budget\", \"amount\": số, \"currency\": \"VND\"}.";
                    }

                    // Send the crafted prompt to AI
                    sendPromptToAI(prompt);
                });
            });
            return;
        }

        // Normal send to AI
        sendPromptToAI(text);
    }

    private void sendPromptToAI(String text) {
        // Add temporary "Đang phân tích..." message
        int analyzingIndex = messages.size();
        messages.add(new ChatMessage("Đang phân tích...", false, "Bây giờ"));
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
            systemPart.put("text", "Bạn là trợ lý ghi chi tiêu. " + currentDateInfo + ". Khi user nói 'Tôi muốn thêm chi tiêu', hãy trả lời một cách thân thiện và đưa ra VÍ DỤ cụ thể như: 'Chào bạn! Tôi sẽ giúp bạn ghi lại chi tiêu. Hãy cho tôi biết chi tiêu cụ thể nhé, ví dụ: \"Hôm qua ăn bún đậu hết 50k\" hoặc \"Ngày 5/11 mua cafe 25k\" hoặc \"Hôm nay ăn sáng 30k\". Sau đó tôi sẽ tự động lưu luôn cho bạn!' Khi user cung cấp thông tin chi tiêu cụ thể, hãy trích xuất ngày tháng CHÍNH XÁC và trả về JSON với cấu trúc: {\"type\": \"expense\", \"name\": \"tên giao dịch\", \"amount\": số tiền, \"currency\": \"VND\", \"category\": \"Ăn uống\", \"day\": ngày (1-31), \"month\": tháng (1-12), \"year\": năm} kèm câu trả lời tự nhiên hài hước theo FORMAT: 'Okela! Đã ghi nhận và LƯU LUÔN chi tiêu [TÊN] với số tiền [SỐ TIỀN] VND thuộc danh mục [DANH MỤC] vào ngày [NGÀY/THÁNG/NĂM]. [CÂU HÀI HƯỚC VỀ CHI TIÊU ĐÓ]. Bạn có muốn thêm chi tiêu nào khác không?' QUAN TRỌNG: Bạn phải trả về CÙNG LÚC cả JSON và text trong cùng một response. Ví dụ: '{\"type\":\"expense\",\"name\":\"Ăn phở\",\"amount\":45000,\"currency\":\"VND\",\"category\":\"Ăn uống\",\"day\":10,\"month\":11,\"year\":2025} Okela! Đã ghi nhận và LƯU LUÔN chi tiêu Ăn phở với số tiền 45,000 VND thuộc danh mục Ăn uống vào ngày 10/11/2025. Phở ngon thế này thì tiền bay cũng đáng rồi! 🍜 Bạn có muốn thêm chi tiêu nào khác không?'. QUY TẮC NGÀY: 'hôm nay'=" + currentDay + "/" + currentMonth + "/" + currentYear + ", 'hôm qua'=" + yesterdayDay + "/" + yesterdayMonth + "/" + yesterdayYear + ", 'ngày X/Y'=ngày X tháng Y năm " + currentYear + ". Nếu user không nói rõ ngày, hãy dùng ngày hiện tại (" + currentDay + "/" + currentMonth + "/" + currentYear + "). Khi user muốn thay đổi ngân sách tháng, trả về JSON {\"action\":\"update_budget\", \"amount\": số tiền mới, \"currency\": \"VND\"} và câu trả lời tự nhiên để xác nhận. Nếu không phải thêm giao dịch hoặc thay đổi ngân sách, trả lời bình thường.");
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

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    getActivity().runOnUiThread(() -> {
                        // Replace analyzing message with error
                        messages.set(analyzingIndex, new ChatMessage("Lỗi kết nối AI.", false, "Bây giờ"));
                        chatAdapter.notifyItemChanged(analyzingIndex);
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

                            // Check if response contains JSON
                            String jsonPart = extractJsonFromText(aiText);
                            String displayText = extractDisplayText(aiText);

                            android.util.Log.d("AiChatBottomSheet", "AI full response: " + aiText);
                            android.util.Log.d("AiChatBottomSheet", "Extracted JSON: " + jsonPart);
                            android.util.Log.d("AiChatBottomSheet", "Display text: " + displayText);

                            getActivity().runOnUiThread(() -> {
                                // Replace analyzing message with display text
                                android.util.Log.d("AiChatBottomSheet", "Updating message at index: " + analyzingIndex + " with: " + displayText);
                                messages.set(analyzingIndex, new ChatMessage(displayText, false, "Bây giờ"));
                                chatAdapter.notifyItemChanged(analyzingIndex);
                                Log.d("AiChatBottomSheet", "AI response: " + displayText);

                                messagesRecycler.smoothScrollToPosition(messages.size() - 1);
                                textToSpeech.speak(displayText, TextToSpeech.QUEUE_FLUSH, null, null);

                                // If JSON found, route to appropriate confirmation dialog
                                if (jsonPart != null) {
                                    android.util.Log.d("AiChatBottomSheet", "JSON found, processing...");
                                    try {
                                        JSONObject parsed = new JSONObject(jsonPart);
                                        String action = parsed.optString("action", "");
                                        android.util.Log.d("AiChatBottomSheet", "Action: " + action);
                                        if ("set_budget".equals(action) || "update_budget".equals(action)) {
                                            android.util.Log.d("AiChatBottomSheet", "Routing to budget dialog");
                                            showBudgetConfirmationDialog(jsonPart);
                                        } else {
                                            android.util.Log.d("AiChatBottomSheet", "Routing to saveExpenseDirectly");
                                            saveExpenseDirectly(jsonPart);
                                        }
                                    } catch (Exception e) {
                                        android.util.Log.d("AiChatBottomSheet", "JSON parsing failed, routing to saveExpenseDirectly anyway");
                                        saveExpenseDirectly(jsonPart);
                                    }
                                } else {
                                    android.util.Log.d("AiChatBottomSheet", "No JSON found in AI response");
                                }
                            });
                        } catch (Exception e) {
                            getActivity().runOnUiThread(() -> {
                                // Replace analyzing message with error
                                messages.set(analyzingIndex, new ChatMessage("Lỗi xử lý phản hồi AI.", false, "Bây giờ"));
                                chatAdapter.notifyItemChanged(analyzingIndex);
                            });
                        }
                    } else {
                        getActivity().runOnUiThread(() -> {
                            // Replace analyzing message with error
                            messages.set(analyzingIndex, new ChatMessage("Lỗi từ AI: " + response.code(), false, "Bây giờ"));
                            chatAdapter.notifyItemChanged(analyzingIndex);
                        });
                    }
                }
            });
        } catch (Exception e) {
            // Replace analyzing message with error
            messages.set(analyzingIndex, new ChatMessage("Lỗi gửi tin nhắn.", false, "Bây giờ"));
            chatAdapter.notifyItemChanged(analyzingIndex);
        }
    }

    private void showConfirmationDialog(String aiText) {
        // Create custom dialog
        Dialog dialog = new Dialog(getContext());
        dialog.setContentView(R.layout.dialog_confirm_expense);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        TextView message = dialog.findViewById(R.id.dialog_message);
        TextInputEditText input = dialog.findViewById(R.id.input_description);
        Button btnCancel = dialog.findViewById(R.id.btn_cancel);
        Button btnConfirm = dialog.findViewById(R.id.btn_confirm);

        input.setText(aiText);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            String finalText = input.getText().toString().trim();
            if (!finalText.isEmpty()) {
                // TODO: Parse and add to database
                Toast.makeText(getContext(), "Đã thêm: " + finalText, Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    private String extractJsonFromText(String text) {
        android.util.Log.d("AiChatBottomSheet", "Extracting JSON from text: " + text);
        // Find JSON object in text
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        android.util.Log.d("AiChatBottomSheet", "JSON positions - start: " + start + ", end: " + end);
        if (start != -1 && end != -1 && end > start) {
            String jsonResult = text.substring(start, end + 1);
            android.util.Log.d("AiChatBottomSheet", "Extracted JSON result: " + jsonResult);
            return jsonResult;
        }
        android.util.Log.d("AiChatBottomSheet", "No JSON found in text");
        return null;
    }

    private String extractDisplayText(String text) {
        // Remove JSON part and return the rest
        String jsonPart = extractJsonFromText(text);
        if (jsonPart != null) {
            return text.replace(jsonPart, "").trim();
        }
        return text;
    }

    private void saveExpenseDirectly(String jsonString) {
        android.util.Log.d("AiChatBottomSheet", "saveExpenseDirectly called with: " + jsonString);
        
        try {
            // Parse JSON từ AI response
            JSONObject json = new JSONObject(jsonString);
            android.util.Log.d("AiChatBottomSheet", "JSON parsed successfully");

            if (json != null) {
                // Lấy giá trị từ JSON
                String name = json.optString("name", "");
                double amount = json.optDouble("amount", 0.0);
                String category = json.optString("category", "");
                String currency = json.optString("currency", "VND");
                String type = json.optString("type", "expense");
                int day = json.optInt("day", Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
                int month = json.optInt("month", Calendar.getInstance().get(Calendar.MONTH) + 1);
                int year = json.optInt("year", Calendar.getInstance().get(Calendar.YEAR));

                android.util.Log.d("AiChatBottomSheet", "Extracted data: name=" + name + ", amount=" + amount + ", category=" + category);

                // Tạo Calendar object
                Calendar calendar = Calendar.getInstance();
                calendar.set(year, month - 1, day); // Month is 0-based

                // Tạo TransactionEntity với constructor đúng
                long transactionAmount = type.equals("expense") ? -Math.abs((long)amount) : (long)amount;
                TransactionEntity transaction = new TransactionEntity(
                    name,                    // description
                    category,                // category
                    transactionAmount,       // amount (negative for expense)
                    calendar.getTime(),      // date
                    type                     // type
                );

                android.util.Log.d("AiChatBottomSheet", "Transaction entity created, starting save process");

                // Lưu vào database trong background thread
                new Thread(() -> {
                    android.util.Log.d("AiChatBottomSheet", "Background thread started for database save");
                    try {
                        AppDatabase.getInstance(getContext()).transactionDao().insert(transaction);
                        android.util.Log.d("AiChatBottomSheet", "Database save successful");
                        
                        // Hiển thị toast trên main thread với layer cao nhất
                        requireActivity().runOnUiThread(() -> {
                            android.util.Log.d("AiChatBottomSheet", "Back on UI thread, preparing toast");
                            String toastMessage = String.format("✅ Đã thêm %s %,.0f %s - %s", 
                                type.equals("expense") ? "chi tiêu" : "thu nhập",
                                amount, currency, category);
                            
                            android.util.Log.d("AiChatBottomSheet", "Toast message: " + toastMessage);
                            
                            // Hiển thị 1 toast duy nhất ở TOP với UI đẹp
                            showToastOnTop(toastMessage);
                        });

                        // Hiển thị message trong chat trên main thread
                        requireActivity().runOnUiThread(() -> {
                            // Chỉ hiển thị toast, không thêm message nữa vì AI đã trả về display text rồi
                            android.util.Log.d("AiChatBottomSheet", "Skipping additional chat message - AI already provided response");
                        });

                    } catch (Exception e) {
                        e.printStackTrace();
                        requireActivity().runOnUiThread(() -> {
                            String errorMessage = "❌ Có lỗi xảy ra khi lưu dữ liệu: " + e.getMessage();
                            showErrorToast(errorMessage);
                            android.util.Log.e("AiChatBottomSheet", "Error saving expense", e);
                        });
                    }
                }).start();
            }

        } catch (Exception e) {
            e.printStackTrace();
            String errorMessage = "❌ Có lỗi xảy ra khi xử lý dữ liệu: " + e.getMessage();
            showErrorToast(errorMessage);
            android.util.Log.e("AiChatBottomSheet", "Error processing data", e);
        }
    }

    // Helper method để thêm message vào chat
    private void addMessageToChat(String message, boolean isUser) {
        android.util.Log.d("AiChatBottomSheet", "Adding message to chat (isUser=" + isUser + "): " + message);
        android.util.Log.d("AiChatBottomSheet", "Current message count before add: " + messages.size());
        
        messages.add(new ChatMessage(message, isUser, "Bây giờ"));
        chatAdapter.notifyItemInserted(messages.size() - 1);
        messagesRecycler.smoothScrollToPosition(messages.size() - 1);
        
        android.util.Log.d("AiChatBottomSheet", "Current message count after add: " + messages.size());
    }

    // Helper method để hiển thị toast ở top
    private void showTopToast(String message, int duration) {
        try {
            if (getActivity() != null) {
                Toast toast = Toast.makeText(getActivity().getApplicationContext(), message, duration);
                // Đặt toast ở TOP của màn hình với margin lớn
                toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 250);
                toast.show();
                
                // Log để debug
                android.util.Log.d("AiChatBottomSheet", "Top toast shown: " + message);
            }
        } catch (Exception e) {
            android.util.Log.e("AiChatBottomSheet", "Error showing top toast", e);
        }
    }

    // Method tạo custom view toast ở TOP với UI đẹp và animation
    private void showCustomTopToast(String message) {
        showCustomToastWithType(message, "success");
    }

    // Method tổng quát cho các loại toast
    private void showCustomToastWithType(String message, String type) {
        try {
            // Tạo custom toast view với layout đẹp
            android.view.LayoutInflater inflater = android.view.LayoutInflater.from(requireActivity());
            android.view.View layout = inflater.inflate(R.layout.custom_toast_layout, null);
            
            // Set background dựa vào type
            switch (type) {
                case "success":
                    layout.setBackgroundResource(R.drawable.toast_success_background);
                    break;
                case "error":
                    layout.setBackgroundResource(R.drawable.toast_error_background);
                    break;
                default:
                    layout.setBackgroundResource(R.drawable.toast_background);
                    break;
            }
            
            android.widget.TextView text = layout.findViewById(R.id.toast_text);
            text.setText(message);
            
            Toast toast = new Toast(requireActivity());
            toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 150);
            toast.setDuration(Toast.LENGTH_LONG);
            toast.setView(layout);
            
            // Animation dựa vào type
            layout.setAlpha(0f);
            if ("error".equals(type)) {
                // Animation cho error với shake effect
                layout.animate()
                    .alpha(1f)
                    .setDuration(400)
                    .withEndAction(() -> {
                        // Hiệu ứng rung nhẹ
                        layout.animate().translationX(-8).setDuration(80)
                            .withEndAction(() -> layout.animate().translationX(8).setDuration(80)
                                .withEndAction(() -> layout.animate().translationX(0).setDuration(80).start()).start()).start();
                    }).start();
            } else {
                // Animation bình thường cho success/info
                layout.animate()
                    .alpha(1f)
                    .setDuration(400)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .start();
            }
            
            toast.show();
            
            // Animation slide out với timing khác nhau
            int delay = "error".equals(type) ? 4500 : 4000;
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (layout.getParent() != null) {
                    layout.animate()
                        .translationX(layout.getWidth() + 100)
                        .alpha(0.2f)
                        .setDuration(600)
                        .setInterpolator(new android.view.animation.AccelerateInterpolator())
                        .withEndAction(() -> android.util.Log.d("AiChatBottomSheet", "Toast slide out completed"))
                        .start();
                }
            }, delay);
            
            android.util.Log.d("AiChatBottomSheet", "Beautiful " + type + " toast shown: " + message);
            
        } catch (Exception e) {
            android.util.Log.e("AiChatBottomSheet", "Custom toast failed", e);
            // Fallback đơn giản
            Toast simpleToast = Toast.makeText(requireActivity(), message, Toast.LENGTH_LONG);
            simpleToast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 150);
            simpleToast.show();
        }
    }

    // Method để hiển thị toast ở layer cao nhất (trên cùng màn hình)
    private void showToastOnTop(String message) {
        try {
            // Chỉ hiển thị 1 custom toast duy nhất ở TOP với UI đẹp
            showCustomTopToast(message);
            android.util.Log.d("AiChatBottomSheet", "Single top toast shown: " + message);
            
        } catch (Exception e) {
            android.util.Log.e("AiChatBottomSheet", "Error showing top toast", e);
            // Simple fallback nếu custom toast fail
            try {
                Toast simpleToast = Toast.makeText(requireActivity(), message, Toast.LENGTH_LONG);
                simpleToast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 200);
                simpleToast.show();
            } catch (Exception ex) {
                android.util.Log.e("AiChatBottomSheet", "Fallback toast failed", ex);
            }
        }
    }

    // Method riêng cho error toast
    private void showErrorToast(String message) {
        showCustomToastWithType(message, "error");
    }

    private void showBudgetConfirmationDialog(String jsonString) {
        try {
            JSONObject json = new JSONObject(jsonString);
            double amount = json.optDouble("amount", 0);
            String currency = json.optString("currency", "VND");

            // Calculate date range
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.DAY_OF_MONTH, 1);
            Date startOfMonth = cal.getTime();
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
            Date endOfMonth = cal.getTime();

            // Confirm with user using a simple dialog
            Dialog dialog = new Dialog(getContext());
            dialog.setContentView(R.layout.dialog_confirm_budget);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

            TextView message = dialog.findViewById(R.id.dialog_message);
            TextView currentBudgetText = dialog.findViewById(R.id.current_budget_text);
            EditText inputAmount = dialog.findViewById(R.id.input_amount);
            TextView newBudgetDate = dialog.findViewById(R.id.new_budget_date);
            Button btnCancel = dialog.findViewById(R.id.btn_cancel);
            Button btnConfirm = dialog.findViewById(R.id.btn_confirm);

            message.setText("Xác nhận thay đổi ngân sách tháng");
            inputAmount.setText(NumberFormat.getInstance(Locale.getDefault()).format((long) amount));

            // Set new budget date
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", new Locale("vi", "VN"));
            newBudgetDate.setText("Ngày thêm: " + dateFormat.format(new Date()));

            // Load current budget
            Executors.newSingleThreadExecutor().execute(() -> {
                List<BudgetEntity> monthlyBudgets = AppDatabase.getInstance(getContext()).budgetDao().getBudgetsByDateRange(startOfMonth, endOfMonth);
                getActivity().runOnUiThread(() -> {
                    if (monthlyBudgets != null && !monthlyBudgets.isEmpty()) {
                        BudgetEntity budget = monthlyBudgets.get(0);
                        String dateStr = budget.getDate() != null ? dateFormat.format(budget.getDate()) : "(không xác định)";
                        currentBudgetText.setText("Ngân sách cũ: " + String.format(Locale.getDefault(), "%,d", budget.getMonthlyLimit()) + " VND (thêm ngày " + dateStr + ")");
                    } else {
                        currentBudgetText.setText("Ngân sách cũ: Chưa có (thêm ngày -)");
                    }
                });
            });

            btnCancel.setOnClickListener(v -> dialog.dismiss());
            btnConfirm.setOnClickListener(v -> {
                String amtStr = inputAmount.getText().toString().trim();
                if (!amtStr.isEmpty()) {
                    try {
                        long amt = Long.parseLong(amtStr.replaceAll("[^0-9]", ""));
                        // Save budget to DB (update if exists, else insert)
                        Executors.newSingleThreadExecutor().execute(() -> {
                            List<BudgetEntity> existingBudgets = AppDatabase.getInstance(getContext()).budgetDao().getBudgetsByDateRange(startOfMonth, endOfMonth);
                            if (existingBudgets != null && !existingBudgets.isEmpty()) {
                                // Update existing
                                BudgetEntity existing = existingBudgets.get(0);
                                existing.setMonthlyLimit(amt);
                                existing.setDate(new Date());
                                AppDatabase.getInstance(getContext()).budgetDao().update(existing);
                            } else {
                                // Insert new
                                BudgetEntity budget = new BudgetEntity("Ngân sách tháng", amt, 0L, new Date());
                                AppDatabase.getInstance(getContext()).budgetDao().insert(budget);
                            }
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(), "Ngân sách đã được cập nhật: " + String.format(Locale.getDefault(), "%,d", amt) + " " + currency, Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            });
                        });
                    } catch (NumberFormatException ex) {
                        Toast.makeText(getContext(), "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            dialog.show();

        } catch (Exception e) {
            Log.e("AiChatBottomSheet", "Error parsing budget JSON: " + jsonString, e);
            Toast.makeText(getContext(), "Lỗi xử lý dữ liệu ngân sách", Toast.LENGTH_SHORT).show();
        }
    }

    private String createExpenseConfirmationMessage(String name, long amount, String category, int day, int month, int year, String type) {
        // Format amount with thousand separator
        String formattedAmount = String.format(Locale.getDefault(), "%,d", amount);
        String dateStr = String.format("%d/%d/%d", day, month, year);
        
        // Create humorous comments based on category and amount
        String humorousComment = getHumorousComment(category, amount, name);
        
        // Create confirmation message with full format
        if ("expense".equals(type)) {
            return String.format("Okela! Đã ghi nhận chi tiêu %s với số tiền %s VND thuộc danh mục %s vào ngày %s. %s Bạn có muốn thêm chi tiêu nào khác không?", 
                name, formattedAmount, category, dateStr, humorousComment);
        } else {
            return String.format("Tuyệt vời! Đã ghi nhận thu nhập %s với số tiền %s VND thuộc danh mục %s vào ngày %s. %s Túi tiền đang mỉm cười đấy! 😊", 
                name, formattedAmount, category, dateStr, humorousComment);
        }
    }

    private String getHumorousComment(String category, long amount, String name) {
        // Generate humorous comments based on category and amount
        switch (category.toLowerCase()) {
            case "ăn uống":
                if (amount > 100000) {
                    return "Ăn ngon thế này thì tiền bay cũng đáng rồi! 🍽️";
                } else if (amount > 50000) {
                    return "Đói bụng thì phải ăn thôi mà! 😋";
                } else {
                    return "Tiết kiệm mà vẫn ngon, giỏi lắm! 👍";
                }
            case "di chuyển":
                if (amount > 200000) {
                    return "Đi xa thế này chắc về quê nhỉ? 🚗";
                } else {
                    return "Đi lại cũng cần tiền xăng chứ! ⛽";
                }
            case "mua sắm":
                if (amount > 500000) {
                    return "Shopping thế này ví run cầm cập! 💸";
                } else {
                    return "Mua sắm hợp lý, đúng rồi! 🛍️";
                }
            case "giải trí":
                return "Vui chơi để sống khỏe mạnh! 🎉";
            case "y tế":
                return "Sức khỏe là vàng, chi tiêu đúng rồi! 🏥";
            default:
                if (amount > 100000) {
                    return "Chi tiêu khủng thế này! 💰";
                } else {
                    return "Chi tiêu hợp lý, tốt lắm! ✨";
                }
        }
    }

    public static class ChatMessage {
        public String message;
        public boolean isUser;
        public String time;

        public ChatMessage(String message, boolean isUser, String time) {
            this.message = message;
            this.isUser = isUser;
            this.time = time;
        }
    }

    public static class ChatAdapter extends RecyclerView.Adapter<ChatViewHolder> {

        private List<ChatMessage> messages;

        public ChatAdapter(List<ChatMessage> messages) {
            this.messages = messages;
        }

        @NonNull
        @Override
        public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message, parent, false);
            return new ChatViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
            ChatMessage message = messages.get(position);
            holder.bind(message);
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {

        private View messageBubble;
        private android.widget.TextView messageText;
        private android.widget.TextView timeText;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            messageBubble = itemView.findViewById(R.id.message_bubble);
            messageText = itemView.findViewById(R.id.message_text);
            timeText = itemView.findViewById(R.id.time_text);
        }

        public void bind(ChatMessage message) {
            messageText.setText(message.message);
            timeText.setText(message.time);

            if (message.isUser) {
                // User message - align right
                messageBubble.setBackgroundResource(R.drawable.user_message_background);
                ((android.widget.LinearLayout.LayoutParams) messageBubble.getLayoutParams()).gravity = android.view.Gravity.END;
                ((android.widget.LinearLayout.LayoutParams) timeText.getLayoutParams()).gravity = android.view.Gravity.END;
            } else {
                // AI message - align left
                messageBubble.setBackgroundResource(R.drawable.ai_message_background);
                ((android.widget.LinearLayout.LayoutParams) messageBubble.getLayoutParams()).gravity = android.view.Gravity.START;
                ((android.widget.LinearLayout.LayoutParams) timeText.getLayoutParams()).gravity = android.view.Gravity.START;
            }
        }
    }
}
