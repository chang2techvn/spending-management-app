package com.example.spending_management_app.ui;

import static android.app.Activity.RESULT_OK;

import android.content.Intent;
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

import com.example.spending_management_app.MainActivity;
import com.example.spending_management_app.R;
import com.example.spending_management_app.service.GeminiAI;
import com.example.spending_management_app.utils.TextFormatHelper;
import com.example.spending_management_app.utils.ExpenseDescriptionParser;
import com.example.spending_management_app.utils.DateParser;
import com.example.spending_management_app.utils.ExtractorHelper;
import com.example.spending_management_app.utils.BudgetAmountParser;
import com.example.spending_management_app.utils.BudgetMessageHelper;
import com.example.spending_management_app.utils.CategoryHelper;
import com.example.spending_management_app.utils.ExpenseMessageHelper;
import com.example.spending_management_app.utils.CategoryIconHelper;
import com.example.spending_management_app.utils.AiSystemInstructions;

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
import java.util.regex.Pattern;
import java.util.regex.Matcher;

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
    private TextView statusText;

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
        statusText = view.findViewById(R.id.status_text);

        // Initialize TTS and HTTP client
        textToSpeech = new TextToSpeech(getContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(Locale.getDefault());
            }
        });
        client = new OkHttpClient();
        
        // Check and update network status
        updateNetworkStatus();

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
                
                String lower = prompt.toLowerCase();
                
                // Check if this is "Add expense" request
                if (lower.contains("chi tiêu") || lower.contains("thêm chi tiêu") || lower.contains("chi tieu") ||
                    lower.contains("expense_bulk")) {
                    // For "Add expense" or "expense_bulk", don't send to AI, just show the welcome message
                    // The welcome message with recent transactions is already loaded in setupMessages()
                    android.util.Log.d("AiChatBottomSheet", "Add expense/expense bulk request - showing welcome message only");
                } else if (lower.contains("ngân sách") || lower.contains("thiet lap ngan sach") || lower.contains("thiết lập ngân sách")) {
                    // For budget management, send to AI
                    String userVisibleMessage = "Thiết lập ngân sách tháng này";
                    messages.add(new ChatMessage(userVisibleMessage, true, "Bây giờ"));
                    chatAdapter.notifyItemInserted(messages.size() - 1);
                    messagesRecycler.smoothScrollToPosition(messages.size() - 1);
                    sendToAI(prompt);
                } else {
                    // For other prompts, send to AI
                    String userVisibleMessage = prompt.trim();
                    messages.add(new ChatMessage(userVisibleMessage, true, "Bây giờ"));
                    chatAdapter.notifyItemInserted(messages.size() - 1);
                    messagesRecycler.smoothScrollToPosition(messages.size() - 1);
                    sendToAI(prompt);
                }
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
        
        // Check if this is budget management mode or category budget management mode
        Bundle args = getArguments();
        if (args != null) {
            String mode = args.getString("mode");
            
            // Check if there's a custom welcome message
            if (args.containsKey("welcome_message")) {
                String welcomeMessage = args.getString("welcome_message");
                android.util.Log.d("AiChatBottomSheet", "Using custom welcome message: " + welcomeMessage);
                messages.add(new ChatMessage(welcomeMessage, false, "Bây giờ"));
            } else if ("budget_management".equals(mode)) {
                // Load budget welcome message
                loadBudgetWelcomeMessage();
            } else if ("category_budget_management".equals(mode)) {
                // This should not happen since category budget always provides welcome_message
                // But add fallback just in case
                boolean isOnline = isNetworkAvailable();
                String fallbackMessage = "📊 Ngân sách theo danh mục\n\n";
                
                if (!isOnline) {
                    fallbackMessage += "⚠️ Offline: Chỉ có thể thêm/sửa/xóa ngân sách danh mục\n\n";
                }
                
                fallbackMessage += "💡 Hướng dẫn:\n" +
                        "• Thêm: 'Đặt ngân sách ăn uống 2 triệu'\n" +
                        "• Sửa: 'Sửa ngân sách di chuyển 1 triệu'\n" +
                        "• Xóa: 'Xóa ngân sách cafe'\n\n" +
                        "📂 Danh mục: Ăn uống, Di chuyển, Tiện ích, Y tế, Nhà ở, Mua sắm, v.v.";
                messages.add(new ChatMessage(fallbackMessage, false, "Bây giờ"));
            } else if ("expense_bulk_management".equals(mode)) {
                // Load expense bulk management welcome message
                loadExpenseBulkWelcomeMessage();
            } else {
                // Load expense tracking welcome message
                loadRecentTransactionsForWelcome();
            }
        } else {
            // Load expense tracking welcome message (default)
            loadRecentTransactionsForWelcome();
        }

        chatAdapter = new ChatAdapter(messages);
        messagesRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        messagesRecycler.setAdapter(chatAdapter);
    }
    
    private void loadBudgetWelcomeMessage() {
        // Add a temporary loading message
        messages.add(new ChatMessage("Đang tải thông tin ngân sách...", false, "Bây giờ"));
        
        // Load budget data from database in background
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Get current month's budget
                Calendar currentCal = Calendar.getInstance();
                currentCal.set(Calendar.DAY_OF_MONTH, 1);
                currentCal.set(Calendar.HOUR_OF_DAY, 0);
                currentCal.set(Calendar.MINUTE, 0);
                currentCal.set(Calendar.SECOND, 0);
                currentCal.set(Calendar.MILLISECOND, 0);
                Date currentMonthStart = currentCal.getTime();
                
                currentCal.set(Calendar.DAY_OF_MONTH, currentCal.getActualMaximum(Calendar.DAY_OF_MONTH));
                currentCal.set(Calendar.HOUR_OF_DAY, 23);
                currentCal.set(Calendar.MINUTE, 59);
                currentCal.set(Calendar.SECOND, 59);
                currentCal.set(Calendar.MILLISECOND, 999);
                Date currentMonthEnd = currentCal.getTime();
                
                android.util.Log.d("AiChatBottomSheet", "Loading budget for range: " + currentMonthStart + " to " + currentMonthEnd);
                
                List<BudgetEntity> currentMonthBudgets = AppDatabase.getInstance(getContext())
                        .budgetDao()
                        .getBudgetsByDateRangeOrdered(currentMonthStart, currentMonthEnd);
                
                android.util.Log.d("AiChatBottomSheet", "Found " + (currentMonthBudgets != null ? currentMonthBudgets.size() : 0) + " budgets for current month");
                
                // Get budgets from 6 months ago
                Calendar pastCal = Calendar.getInstance();
                pastCal.add(Calendar.MONTH, -6);
                pastCal.set(Calendar.DAY_OF_MONTH, 1);
                Date sixMonthsAgoStart = pastCal.getTime();
                
                List<BudgetEntity> pastBudgets = AppDatabase.getInstance(getContext())
                        .budgetDao()
                        .getBudgetsByDateRangeOrdered(sixMonthsAgoStart, currentMonthEnd);
                
                SimpleDateFormat monthFormat = new SimpleDateFormat("MM/yyyy", new Locale("vi", "VN"));
                
                // Build welcome message with budget information
                StringBuilder welcomeMessage = new StringBuilder();
                welcomeMessage.append("Chào bạn! 👋\n\n");
                
                // Check network status and add warning if offline
                if (!isNetworkAvailable()) {
                    welcomeMessage.append("⚠️ CHẾ ĐỘ OFFLINE\n");
                    welcomeMessage.append("Bạn có thể:\n");
                    welcomeMessage.append("✅ Thêm, sửa, xóa chi tiêu\n");
                    welcomeMessage.append("✅ Quản lý ngân sách\n");
                    welcomeMessage.append("❌ Không thể phân tích và tư vấn với AI\n\n");
                }
                
                // Part 1: Budget history from 6 months ago
                if (!pastBudgets.isEmpty()) {
                    welcomeMessage.append("📊 Ngân sách 6 tháng gần đây:\n\n");
                    
                    // Group budgets by month and show the most recent one for each month
                    java.util.Map<String, BudgetEntity> budgetsByMonth = new java.util.HashMap<>();
                    for (BudgetEntity budget : pastBudgets) {
                        String monthKey = monthFormat.format(budget.date);
                        if (!budgetsByMonth.containsKey(monthKey) || 
                            budget.date.after(budgetsByMonth.get(monthKey).date)) {
                            budgetsByMonth.put(monthKey, budget);
                        }
                    }
                    
                    // Sort and display (limit to last 6 entries)
                    java.util.List<String> sortedMonths = new java.util.ArrayList<>(budgetsByMonth.keySet());
                    java.util.Collections.sort(sortedMonths);
                    
                    // Only show last 6 entries
                    int startIndex = Math.max(0, sortedMonths.size() - 6);
                    for (int i = startIndex; i < sortedMonths.size(); i++) {
                        String month = sortedMonths.get(i);
                        BudgetEntity budget = budgetsByMonth.get(month);
                        String formattedAmount = String.format("%,d", budget.monthlyLimit);
                        welcomeMessage.append("💰 Tháng ").append(month).append(": ")
                                .append(formattedAmount).append(" VND\n");
                    }
                    welcomeMessage.append("\n");
                }
                
                // Current month budget
                android.util.Log.d("AiChatBottomSheet", "Current month budgets found: " + (currentMonthBudgets != null ? currentMonthBudgets.size() : 0));
                if (currentMonthBudgets != null) {
                    for (int i = 0; i < currentMonthBudgets.size(); i++) {
                        BudgetEntity b = currentMonthBudgets.get(i);
                        android.util.Log.d("AiChatBottomSheet", "Budget " + i + ": date=" + b.date + ", amount=" + b.monthlyLimit);
                    }
                }
                
                if (!currentMonthBudgets.isEmpty()) {
                    BudgetEntity currentBudget = currentMonthBudgets.get(0);
                    String formattedAmount = String.format("%,d", currentBudget.monthlyLimit);
                    String currentMonth = monthFormat.format(currentBudget.date);
                    welcomeMessage.append("📅 Ngân sách tháng này (").append(currentMonth).append("): ")
                            .append(formattedAmount).append(" VND\n\n");
                } else {
                    welcomeMessage.append("📅 Ngân sách tháng này: Chưa thiết lập\n\n");
                }
                
                // Part 2: Instructions for managing budget
                welcomeMessage.append("💡 Để quản lý ngân sách, hãy cho tôi biết:\n");
                welcomeMessage.append("Ví dụ: \"Thêm ngân sách 15 triệu\" hoặc \"Sửa ngân sách lên 20 triệu\"");
                
                String finalMessage = welcomeMessage.toString();
                
                // Update UI on main thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        // Replace loading message with actual welcome message
                        if (!messages.isEmpty()) {
                            messages.set(0, new ChatMessage(finalMessage, false, "Bây giờ"));
                            chatAdapter.notifyItemChanged(0);
                        }
                    });
                }
                
            } catch (Exception e) {
                android.util.Log.e("AiChatBottomSheet", "Error loading budget information", e);
                
                // Fallback to simple welcome message
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        StringBuilder fallbackMessage = new StringBuilder();
                        fallbackMessage.append("Chào bạn! 👋\n\n");
                        
                        // Check network status and add warning if offline
                        if (!isNetworkAvailable()) {
                            fallbackMessage.append("⚠️ CHẾ ĐỘ OFFLINE\n");
                            fallbackMessage.append("Bạn có thể:\n");
                            fallbackMessage.append("✅ Thêm, sửa, xóa chi tiêu\n");
                            fallbackMessage.append("✅ Quản lý ngân sách\n");
                            fallbackMessage.append("❌ Không thể phân tích và tư vấn với AI\n\n");
                        }
                        
                        fallbackMessage.append("💡 Để quản lý ngân sách tháng, hãy cho tôi biết:\n");
                        fallbackMessage.append("Ví dụ: \"Đặt ngân sách 15 triệu\" hoặc \"Sửa ngân sách lên 20 triệu\"");
                        
                        if (!messages.isEmpty()) {
                            messages.set(0, new ChatMessage(fallbackMessage.toString(), false, "Bây giờ"));
                            chatAdapter.notifyItemChanged(0);
                        }
                    });
                }
            }
        });
    }
    
    private void loadRecentTransactionsForWelcome() {
        // Add a temporary loading message
        messages.add(new ChatMessage("Đang tải...", false, "Bây giờ"));
        
        // Load recent transactions from database in background
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                List<TransactionEntity> recentTransactions = AppDatabase.getInstance(getContext())
                        .transactionDao()
                        .getRecentTransactions(3);
                
                // Build welcome message with recent transactions
                StringBuilder welcomeMessage = new StringBuilder();
                welcomeMessage.append("Chào bạn! 👋\n\n");
                
                // Check network status and add warning if offline
                if (!isNetworkAvailable()) {
                    welcomeMessage.append("⚠️ CHẾ ĐỘ OFFLINE\n");
                    welcomeMessage.append("Bạn có thể:\n");
                    welcomeMessage.append("✅ Thêm, sửa, xóa chi tiêu\n");
                    welcomeMessage.append("✅ Quản lý ngân sách\n");
                    welcomeMessage.append("❌ Không thể phân tích và tư vấn với AI\n\n");
                }
                
                if (!recentTransactions.isEmpty()) {
                    welcomeMessage.append("📋 Chi tiêu gần đây:\n\n");
                    
                    for (TransactionEntity transaction : recentTransactions) {
                        String emoji = CategoryHelper.getEmojiForCategory(transaction.category);
                        String formattedAmount = String.format("%,d", Math.abs(transaction.amount));
                        welcomeMessage.append(emoji).append(" ")
                                .append(transaction.description).append(": ")
                                .append(formattedAmount).append(" VND")
                                .append(" (").append(transaction.category).append(")")
                                .append("\n");
                    }
                    welcomeMessage.append("\n");
                }
                
                welcomeMessage.append("💡 Để thêm chi tiêu mới, hãy cho tôi biết:\n");
                welcomeMessage.append("Ví dụ: \"Hôm qua tôi đổ xăng 50k\" hoặc \"Ngày 10/11 mua cafe 25k\"");
                
                String finalMessage = welcomeMessage.toString();
                
                // Update UI on main thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        // Replace loading message with actual welcome message
                        if (!messages.isEmpty()) {
                            messages.set(0, new ChatMessage(finalMessage, false, "Bây giờ"));
                            chatAdapter.notifyItemChanged(0);
                        }
                    });
                }
                
            } catch (Exception e) {
                android.util.Log.e("AiChatBottomSheet", "Error loading recent transactions", e);
                
                // Fallback to simple welcome message
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        StringBuilder fallbackMessage = new StringBuilder();
                        fallbackMessage.append("Chào bạn! 👋\n\n");
                        
                        // Check network status and add warning if offline
                        if (!isNetworkAvailable()) {
                            fallbackMessage.append("⚠️ CHẾ ĐỘ OFFLINE\n");
                            fallbackMessage.append("Bạn có thể:\n");
                            fallbackMessage.append("✅ Thêm, sửa, xóa chi tiêu\n");
                            fallbackMessage.append("✅ Quản lý ngân sách\n");
                            fallbackMessage.append("❌ Không thể phân tích và tư vấn với AI\n\n");
                        }
                        
                        fallbackMessage.append("💡 Để thêm chi tiêu mới, hãy cho tôi biết:\n");
                        fallbackMessage.append("Ví dụ: \"Hôm qua tôi đổ xăng 50k\" hoặc \"Ngày 10/11 mua cafe 25k\"");
                        
                        if (!messages.isEmpty()) {
                            messages.set(0, new ChatMessage(fallbackMessage.toString(), false, "Bây giờ"));
                            chatAdapter.notifyItemChanged(0);
                        }
                    });
                }
            }
        });
    }
    
    private void loadExpenseBulkWelcomeMessage() {
        // Add a temporary loading message
        messages.add(new ChatMessage("Đang tải...", false, "Bây giờ"));
        
        // Load recent transactions from database in background
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                List<TransactionEntity> recentTransactions = AppDatabase.getInstance(getContext())
                        .transactionDao()
                        .getRecentTransactions(5); // Show 5 recent transactions
                
                // Build welcome message with recent transactions
                StringBuilder welcomeMessage = new StringBuilder();
                welcomeMessage.append("📋 Quản lý chi tiêu hàng loạt\n\n");
                
                // Check network status and add warning if offline
                if (!isNetworkAvailable()) {
                    welcomeMessage.append("⚠️ CHẾ ĐỘ OFFLINE\n");
                    welcomeMessage.append("Bạn có thể:\n");
                    welcomeMessage.append("✅ Thêm, sửa, xóa chi tiêu\n");
                    welcomeMessage.append("✅ Quản lý ngân sách\n");
                    welcomeMessage.append("❌ Không thể phân tích và tư vấn với AI\n\n");
                }
                
                if (!recentTransactions.isEmpty()) {
                    welcomeMessage.append("💳 Chi tiêu gần đây:\n\n");
                    
                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM", new Locale("vi", "VN"));
                    
                    for (TransactionEntity transaction : recentTransactions) {
                        String emoji = CategoryIconHelper.getIconEmoji(transaction.category);
                        String formattedAmount = String.format("%,d", Math.abs(transaction.amount));
                        String dateStr = dateFormat.format(transaction.date);
                        
                        welcomeMessage.append(emoji).append(" ")
                                .append(transaction.description)
                                .append(": ").append(formattedAmount).append(" VND")
                                .append(" - ").append(dateStr)
                                .append("\n");
                    }
                    welcomeMessage.append("\n");
                }
                
                welcomeMessage.append("💡 Hướng dẫn:\n");
                welcomeMessage.append("• Thêm: 'Hôm qua ăn sáng 25k và cafe 30k'\n");
                welcomeMessage.append("• Xóa: 'Xóa chi tiêu #123' (tìm ID ở trang Lịch sử)");
                
                String finalMessage = welcomeMessage.toString();
                
                // Update UI on main thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        // Replace loading message with actual welcome message
                        if (!messages.isEmpty()) {
                            messages.set(0, new ChatMessage(finalMessage, false, "Bây giờ"));
                            chatAdapter.notifyItemChanged(0);
                        }
                    });
                }
                
            } catch (Exception e) {
                android.util.Log.e("AiChatBottomSheet", "Error loading expense bulk welcome message", e);
                
                // Fallback to simple welcome message
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        StringBuilder fallbackMessage = new StringBuilder();
                        fallbackMessage.append("📋 Quản lý chi tiêu hàng loạt\n\n");
                        
                        // Check network status and add warning if offline
                        if (!isNetworkAvailable()) {
                            fallbackMessage.append("⚠️ CHẾ ĐỘ OFFLINE\n");
                            fallbackMessage.append("Bạn có thể:\n");
                            fallbackMessage.append("✅ Thêm, sửa, xóa chi tiêu\n");
                            fallbackMessage.append("✅ Quản lý ngân sách\n");
                            fallbackMessage.append("❌ Không thể phân tích và tư vấn với AI\n\n");
                        }
                        
                        fallbackMessage.append("💡 Hướng dẫn:\n");
                        fallbackMessage.append("• Thêm: 'Hôm qua ăn sáng 25k và cafe 30k'\n");
                        fallbackMessage.append("• Xóa: 'Xóa chi tiêu #123' (tìm ID ở trang Lịch sử)");
                        
                        if (!messages.isEmpty()) {
                            messages.set(0, new ChatMessage(fallbackMessage.toString(), false, "Bây giờ"));
                            chatAdapter.notifyItemChanged(0);
                        }
                    });
                }
            }
        });
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

    private void updateNetworkStatus() {
        if (getContext() == null || statusText == null) {
            return;
        }
        
        if (isNetworkAvailable()) {
            statusText.setText("Đang hoạt động");
            statusText.setTextColor(0xFF4CAF50); // Green color
        } else {
            statusText.setText("Offline");
            statusText.setTextColor(0xFFFF5252); // Red color
        }
    }

    private boolean isNetworkAvailable() {
        if (getContext() == null) {
            return false;
        }
        
        android.net.ConnectivityManager connectivityManager = 
            (android.net.ConnectivityManager) getContext().getSystemService(android.content.Context.CONNECTIVITY_SERVICE);
        
        if (connectivityManager != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                android.net.Network network = connectivityManager.getActiveNetwork();
                if (network == null) return false;
                
                android.net.NetworkCapabilities capabilities = 
                    connectivityManager.getNetworkCapabilities(network);
                
                return capabilities != null && (
                    capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET)
                );
            } else {
                android.net.NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
                return activeNetwork != null && activeNetwork.isConnected();
            }
        }
        
        return false;
    }

    private void sendToAI(String text) {
        // Check network connectivity first
        boolean isOnline = isNetworkAvailable();
        
        // Check if this is budget management mode or category budget management mode or expense bulk management mode
        Bundle args = getArguments();
        boolean isBudgetMode = args != null && "budget_management".equals(args.getString("mode"));
        boolean isCategoryBudgetMode = args != null && "category_budget_management".equals(args.getString("mode"));
        boolean isExpenseBulkMode = args != null && "expense_bulk_management".equals(args.getString("mode"));
        
        // If offline, try to handle with regex first
        if (!isOnline) {
            boolean handled = handleOfflineRequest(text, isBudgetMode, isCategoryBudgetMode, isExpenseBulkMode);
            if (handled) {
                return;
            }
            // If not handled by regex, show error
            messages.add(new ChatMessage("❌ Chức năng này cần kết nối internet. Vui lòng kiểm tra kết nối mạng của bạn.", false, "Bây giờ"));
            chatAdapter.notifyItemInserted(messages.size() - 1);
            messagesRecycler.smoothScrollToPosition(messages.size() - 1);
            return;
        }
        
        // Handle expense bulk management
        if (isExpenseBulkMode) {
            handleExpenseBulkRequest(text);
            return;
        }
        
        // Handle category budget management
        if (isCategoryBudgetMode) {
            handleCategoryBudgetRequest(text);
            return;
        }
        
        // Check if user is asking for budget analysis, view, or delete
        if (isBudgetMode || BudgetMessageHelper.isBudgetQuery(text)) {
            handleBudgetQuery(text);
            return;
        }
        
        // Check if user is asking for financial analysis or reports
        if (!isBudgetMode && ExpenseMessageHelper.isFinancialQuery(text)) {
            // Get comprehensive financial data from database
            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    String financialContext = getFinancialContext();
                    getActivity().runOnUiThread(() -> {
                        sendPromptToAIWithContext(text, financialContext);
                    });
                } catch (Exception e) {
                    getActivity().runOnUiThread(() -> {
                        sendPromptToAI(text);
                    });
                }
            });
            return;
        }

        // Normal send to AI for expense tracking
        sendPromptToAI(text);
    }
    
    // Handle offline requests using regex
    private boolean handleOfflineRequest(String text, boolean isBudgetMode, boolean isCategoryBudgetMode, boolean isExpenseBulkMode) {
        String lowerText = text.toLowerCase();
        
        // Check if text contains a category name (for category budget detection)
        boolean hasCategoryName = false;
        for (String cat : CategoryHelper.getAllCategories()) {
            if (lowerText.contains(cat.toLowerCase())) {
                hasCategoryName = true;
                break;
            }
        }
        
        // 3. Handle category budget operations (check first if category name is present)
        if (isCategoryBudgetMode || hasCategoryName || lowerText.contains("ngân sách danh mục")) {
            boolean isBudgetOperation = lowerText.contains("ngân sách") || 
                                       lowerText.contains("đặt") || 
                                       lowerText.contains("thêm") ||
                                       lowerText.contains("sửa") ||
                                       lowerText.contains("xóa") || 
                                       lowerText.contains("xoá");
            
            if (isBudgetOperation && hasCategoryName) {
                // Delete category budget
                if (lowerText.contains("xóa") || lowerText.contains("xoá")) {
                    return handleOfflineDeleteCategoryBudget(text);
                }
                // Add/Update category budget
                if (lowerText.contains("thêm") || lowerText.contains("đặt") || 
                    lowerText.contains("sửa") || lowerText.contains("ngân sách")) {
                    return handleOfflineUpdateCategoryBudget(text);
                }
            }
        }
        
        // 2. Handle monthly budget operations
        if (isBudgetMode || lowerText.contains("ngân sách tháng")) {
            // Delete budget
            if (lowerText.contains("xóa") || lowerText.contains("xoá")) {
                return handleOfflineDeleteBudget(text);
            }
            // Add/Update budget
            if (lowerText.contains("thêm") || lowerText.contains("đặt") || 
                lowerText.contains("sửa") || lowerText.contains("nâng") || 
                lowerText.contains("tăng") || lowerText.contains("giảm") ||
                lowerText.contains("hạ") || lowerText.contains("cắt") ||
                lowerText.contains("trừ") || lowerText.contains("bớt")) {
                return handleOfflineUpdateBudget(text);
            }
        }
        
        // 1. Handle expense operations
        if (isExpenseBulkMode || (!isBudgetMode && !isCategoryBudgetMode)) {
            // Delete expense
            if (lowerText.contains("xóa") || lowerText.contains("xoá")) {
                return handleOfflineDeleteExpense(text);
            }
            // Add expense
            if (containsExpenseKeywords(lowerText)) {
                return handleOfflineAddExpense(text);
            }
        }
        
        return false;
    }
    
    private boolean containsExpenseKeywords(String lowerText) {
        String[] keywords = {"chi tiêu", "mua", "đổ xăng", "ăn", "uống", "cafe", "cà phê", 
                            "nhà hàng", "siêu thị", "shopping", "mỹ phẩm", "quần áo",
                            "điện", "nước", "internet", "điện thoại", "taxi", "grab"};
        for (String keyword : keywords) {
            if (lowerText.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    // Handle budget queries (view, analyze, add, edit, delete)
    private void handleBudgetQuery(String text) {
        String lowerText = text.toLowerCase();
        
        // Check if user wants to delete budget
        if (lowerText.contains("xóa") || lowerText.contains("xoá")) {
            handleDeleteBudget(text);
            return;
        }
        
        // Check if user wants to add/edit/increase/decrease budget
        // Include: set, add, edit, increase, decrease keywords
        if (lowerText.contains("thêm") || lowerText.contains("đặt") || 
            lowerText.contains("sửa") || lowerText.contains("thay đổi") ||
            lowerText.contains("thiết lập") ||
            lowerText.contains("tăng") || lowerText.contains("nâng") ||
            lowerText.contains("giảm") || lowerText.contains("hạ") ||
            lowerText.contains("cộng") || lowerText.contains("trừ") ||
            lowerText.contains("bớt") || lowerText.contains("cắt")) {
            handleBudgetRequest(text);
            return;
        }
        
        // User wants to view or analyze budget - get budget data and send to AI
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                String budgetContext = getBudgetContext();
                
                // Detect if user wants detailed analysis/consultation or just viewing
                boolean needsDetailedAnalysis = lowerText.contains("phân tích") || 
                                               lowerText.contains("tư vấn") || 
                                               lowerText.contains("đánh giá") ||
                                               lowerText.contains("so sánh") ||
                                               lowerText.contains("xu hướng") ||
                                               lowerText.contains("dự báo") ||
                                               lowerText.contains("nhận xét") ||
                                               lowerText.contains("góp ý");
                
                // Add context prefix to help AI understand user's intent
                String queryWithContext = text;
                if (needsDetailedAnalysis) {
                    queryWithContext = "[YÊU CẦU PHÂN TÍCH CHI TIẾT] " + text;
                } else {
                    queryWithContext = "[CHỈ XEM THÔNG TIN] " + text;
                }
                
                String finalQuery = queryWithContext;
                getActivity().runOnUiThread(() -> {
                    sendPromptToAIWithBudgetContext(finalQuery, budgetContext);
                });
            } catch (Exception e) {
                android.util.Log.e("AiChatBottomSheet", "Error getting budget context", e);
                getActivity().runOnUiThread(() -> {
                    sendPromptToAI(text);
                });
            }
        });
    }
    
    private void handleBudgetRequest(String text) {
        // Add analyzing message
        int analyzingIndex = messages.size();
        messages.add(new ChatMessage("Đang xử lý yêu cầu...", false, "Bây giờ"));
        chatAdapter.notifyItemInserted(messages.size() - 1);
        messagesRecycler.smoothScrollToPosition(messages.size() - 1);
        
        // Check if this is an increase/decrease request or absolute set request
        String textLower = text.toLowerCase().trim();
        
        android.util.Log.d("AiChatBottomSheet", "=== BUDGET REQUEST DEBUG ===");
        android.util.Log.d("AiChatBottomSheet", "Original text: [" + text + "]");
        android.util.Log.d("AiChatBottomSheet", "Lowercase text: [" + textLower + "]");
        
        // Check for ABSOLUTE set commands with "lên" or "xuống" 
        // "Tăng lên 10 triệu", "Nâng lên 10 triệu", "Hạ xuống 10 triệu", "Giảm xuống 10 triệu"
        boolean hasLenKeyword = textLower.contains("lên");
        boolean hasXuongKeyword = textLower.contains("xuống");
        boolean isAbsoluteSet = ((textLower.contains("tăng") || textLower.contains("nâng")) && hasLenKeyword) ||
                                ((textLower.contains("giảm") || textLower.contains("hạ")) && hasXuongKeyword);
        
        android.util.Log.d("AiChatBottomSheet", "Has 'lên': " + hasLenKeyword + ", Has 'xuống': " + hasXuongKeyword);
        android.util.Log.d("AiChatBottomSheet", "isAbsoluteSet: " + isAbsoluteSet);
        
        // Check for RELATIVE increase (add more) - only if NOT absolute set
        // "Nâng ngân sách 10 triệu", "Tăng ngân sách 10 triệu", "Tăng thêm 10 triệu"
        boolean hasIncreaseKeyword = textLower.contains("nâng") || 
                                     textLower.contains("tăng") || 
                                     textLower.contains("cộng") || 
                                     textLower.contains("thêm");
        boolean isIncrease = !isAbsoluteSet && hasIncreaseKeyword;
        
        android.util.Log.d("AiChatBottomSheet", "Has increase keyword: " + hasIncreaseKeyword + ", isIncrease: " + isIncrease);
        
        // Check for RELATIVE decrease (subtract) - only if NOT absolute set
        // "Giảm ngân sách 2 triệu", "Hạ ngân sách 1 triệu", "Trừ 2 triệu"
        boolean hasDecreaseKeyword = textLower.contains("giảm") || 
                                     textLower.contains("hạ") || 
                                     textLower.contains("trừ") || 
                                     textLower.contains("bớt") ||
                                     textLower.contains("cắt");
        boolean isDecrease = !isAbsoluteSet && hasDecreaseKeyword;
        
        android.util.Log.d("AiChatBottomSheet", "Has decrease keyword: " + hasDecreaseKeyword + ", isDecrease: " + isDecrease);
        android.util.Log.d("AiChatBottomSheet", "=== FINAL RESULT: isAbsoluteSet=" + isAbsoluteSet + ", isIncrease=" + isIncrease + ", isDecrease=" + isDecrease + " ===");
        
        // Extract amount from text (support various formats like "15 triệu", "20000000", "25tr")
        long amount = BudgetAmountParser.extractBudgetAmount(text);
        
        // Extract month and year from text (default to current month if not specified)
        int[] monthYear = DateParser.extractMonthYear(text);
        int targetMonth = monthYear[0];
        int targetYear = monthYear[1];
        
        // Get current month and year for validation
        Calendar currentCal = Calendar.getInstance();
        int currentMonth = currentCal.get(Calendar.MONTH) + 1; // 0-based to 1-based
        int currentYear = currentCal.get(Calendar.YEAR);
        
        // Validate: only allow current month and future months
        if (targetYear < currentYear || (targetYear == currentYear && targetMonth < currentMonth)) {
            getActivity().runOnUiThread(() -> {
                messages.set(analyzingIndex, new ChatMessage(
                        "⚠️ Không thể thêm hoặc sửa ngân sách cho tháng trong quá khứ!\n\n" +
                        "Bạn chỉ có thể quản lý ngân sách từ tháng " + currentMonth + "/" + currentYear + " trở đi.",
                        false, "Bây giờ"));
                chatAdapter.notifyItemChanged(analyzingIndex);
            });
            return;
        }
        
        if (amount > 0) {
            // Save budget to database
            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    // Create calendar for target month
                    Calendar targetCal = Calendar.getInstance();
                    targetCal.set(Calendar.YEAR, targetYear);
                    targetCal.set(Calendar.MONTH, targetMonth - 1); // 1-based to 0-based
                    targetCal.set(Calendar.DAY_OF_MONTH, 1);
                    targetCal.set(Calendar.HOUR_OF_DAY, 0);
                    targetCal.set(Calendar.MINUTE, 0);
                    targetCal.set(Calendar.SECOND, 0);
                    targetCal.set(Calendar.MILLISECOND, 0);
                    Date startOfMonth = targetCal.getTime();
                    
                    targetCal.set(Calendar.DAY_OF_MONTH, targetCal.getActualMaximum(Calendar.DAY_OF_MONTH));
                    targetCal.set(Calendar.HOUR_OF_DAY, 23);
                    targetCal.set(Calendar.MINUTE, 59);
                    targetCal.set(Calendar.SECOND, 59);
                    Date endOfMonth = targetCal.getTime();
                    
                    android.util.Log.d("AiChatBottomSheet", "Saving budget for range: " + startOfMonth + " to " + endOfMonth);
                    
                    List<BudgetEntity> existingBudgets = AppDatabase.getInstance(getContext())
                            .budgetDao()
                            .getBudgetsByDateRangeOrdered(startOfMonth, endOfMonth);
                    
                    android.util.Log.d("AiChatBottomSheet", "Found " + (existingBudgets != null ? existingBudgets.size() : 0) + " existing budgets");
                    
                    boolean isUpdate = !existingBudgets.isEmpty();
                    
                    // Use the first day of target month as the budget date
                    Date budgetDate = startOfMonth;
                    
                    android.util.Log.d("AiChatBottomSheet", "Budget date to save: " + budgetDate + ", Amount: " + amount + ", isAbsoluteSet: " + isAbsoluteSet + ", isIncrease: " + isIncrease + ", isDecrease: " + isDecrease);
                    
                    // Calculate final amount and determine action type
                    long calculatedFinalAmount;
                    String determinedActionType;
                    
                    if (isUpdate) {
                        // Update existing budget
                        BudgetEntity existing = existingBudgets.get(0);
                        android.util.Log.d("AiChatBottomSheet", "Updating existing budget, old date: " + existing.date + ", new date: " + budgetDate);
                        long oldAmount = existing.monthlyLimit;
                        
                        // Calculate final amount based on operation type
                        if (isAbsoluteSet) {
                            // Absolute set: "Tăng lên 10 triệu", "Giảm xuống 10 triệu" -> Set to exact amount
                            calculatedFinalAmount = amount;
                            determinedActionType = "set";
                            android.util.Log.d("AiChatBottomSheet", "Setting budget to absolute value: " + calculatedFinalAmount);
                        } else if (isIncrease) {
                            // Relative increase: "Nâng 10 triệu", "Tăng thêm 10 triệu" -> Add amount
                            calculatedFinalAmount = oldAmount + amount;
                            determinedActionType = "increase";
                            android.util.Log.d("AiChatBottomSheet", "Increasing budget: " + oldAmount + " + " + amount + " = " + calculatedFinalAmount);
                        } else if (isDecrease) {
                            // Relative decrease: "Giảm 2 triệu", "Trừ 2 triệu" -> Subtract amount
                            long tempAmount = oldAmount - amount;
                            // Don't allow negative budget
                            if (tempAmount < 0) {
                                android.util.Log.w("AiChatBottomSheet", "Final amount would be negative, setting to 0");
                                calculatedFinalAmount = 0;
                            } else {
                                calculatedFinalAmount = tempAmount;
                            }
                            determinedActionType = "decrease";
                            android.util.Log.d("AiChatBottomSheet", "Decreasing budget: " + oldAmount + " - " + amount + " = " + calculatedFinalAmount);
                        } else {
                            // Default: Set to amount (backward compatibility)
                            calculatedFinalAmount = amount;
                            determinedActionType = "set";
                        }
                        
                        existing.monthlyLimit = calculatedFinalAmount;
                        existing.date = budgetDate;
                        AppDatabase.getInstance(getContext()).budgetDao().update(existing);
                        
                        // Log budget history
                        com.example.spending_management_app.utils.BudgetHistoryLogger.logMonthlyBudgetUpdated(
                                getContext(), oldAmount, calculatedFinalAmount, budgetDate);
                    } else {
                        // Insert new budget - ignore increase/decrease for new budget
                        if (isIncrease || isDecrease) {
                            // No existing budget to increase/decrease
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    SimpleDateFormat monthYearFormat = new SimpleDateFormat("MM/yyyy", new Locale("vi", "VN"));
                                    String monthYearStr = monthYearFormat.format(budgetDate);
                                    messages.set(analyzingIndex, new ChatMessage(
                                            "⚠️ Chưa có ngân sách cho tháng " + monthYearStr + " để " + 
                                            (isIncrease ? "nâng" : "giảm") + "!\n\n" +
                                            "Vui lòng đặt ngân sách trước. Ví dụ:\n" +
                                            "   • \"Đặt ngân sách tháng " + monthYearStr + " là 15 triệu\"",
                                            false, "Bây giờ"));
                                    chatAdapter.notifyItemChanged(analyzingIndex);
                                });
                            }
                            return; // Exit without creating new budget
                        }
                        
                        calculatedFinalAmount = amount;
                        determinedActionType = "set";
                        
                        BudgetEntity budget = new BudgetEntity("Ngân sách tháng", calculatedFinalAmount, 0L, budgetDate);
                        android.util.Log.d("AiChatBottomSheet", "Inserting new budget: " + budget.date);
                        AppDatabase.getInstance(getContext()).budgetDao().insert(budget);
                        
                        // Log budget history
                        com.example.spending_management_app.utils.BudgetHistoryLogger.logMonthlyBudgetCreated(
                                getContext(), calculatedFinalAmount, budgetDate);
                    }
                    
                    // Make final variables for lambda
                    final long finalAmount = calculatedFinalAmount;
                    final String actionType = determinedActionType;
                    
                    String formattedFinalAmount = String.format("%,d", finalAmount);
                    String formattedChangeAmount = String.format("%,d", amount);
                    SimpleDateFormat monthYearFormat = new SimpleDateFormat("MM/yyyy", new Locale("vi", "VN"));
                    String monthYearStr = monthYearFormat.format(budgetDate);
                    
                    // Update UI
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            String responseMessage;
                            String toastMessage;
                            
                            if (isUpdate) {
                                if (actionType.equals("increase")) {
                                    responseMessage = "✅ Đã nâng ngân sách tháng " + monthYearStr + " thêm " + formattedChangeAmount + " VND!\n\n" +
                                            "💰 Ngân sách mới: " + formattedFinalAmount + " VND\n\n" +
                                            "Chúc bạn quản lý tài chính tốt! 💪";
                                    toastMessage = "✅ Đã nâng ngân sách tháng " + monthYearStr + ": +" + formattedChangeAmount + " VND";
                                } else if (actionType.equals("decrease")) {
                                    responseMessage = "✅ Đã giảm ngân sách tháng " + monthYearStr + " xuống " + formattedChangeAmount + " VND!\n\n" +
                                            "💰 Ngân sách mới: " + formattedFinalAmount + " VND\n\n" +
                                            "Chúc bạn chi tiêu hợp lý! 💰";
                                    toastMessage = "✅ Đã giảm ngân sách tháng " + monthYearStr + ": -" + formattedChangeAmount + " VND";
                                } else {
                                    responseMessage = "✅ Đã cập nhật ngân sách tháng " + monthYearStr + " thành " + formattedFinalAmount + " VND!\n\n" +
                                            "Chúc bạn quản lý tài chính tốt! 💪";
                                    toastMessage = "✅ Đã cập nhật ngân sách tháng " + monthYearStr + ": " + formattedFinalAmount + " VND";
                                }
                            } else {
                                responseMessage = "✅ Đã thiết lập ngân sách tháng " + monthYearStr + " là " + formattedFinalAmount + " VND!\n\n" +
                                        "Chúc bạn chi tiêu hợp lý! 💰";
                                toastMessage = "✅ Đã thiết lập ngân sách tháng " + monthYearStr + ": " + formattedFinalAmount + " VND";
                            }
                            
                            messages.set(analyzingIndex, new ChatMessage(responseMessage, false, "Bây giờ"));
                            chatAdapter.notifyItemChanged(analyzingIndex);
                            messagesRecycler.smoothScrollToPosition(messages.size() - 1);
                            
                            showToastOnTop(toastMessage);
                            
                            // Refresh HomeFragment
                            refreshHomeFragment();
                        });
                    }
                    
                } catch (Exception e) {
                    android.util.Log.e("AiChatBottomSheet", "Error saving budget", e);
                    
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            messages.set(analyzingIndex, new ChatMessage(
                                    "❌ Có lỗi xảy ra khi lưu ngân sách. Vui lòng thử lại!", 
                                    false, "Bây giờ"));
                            chatAdapter.notifyItemChanged(analyzingIndex);
                            showErrorToast("Lỗi lưu ngân sách");
                        });
                    }
                }
            });
        } else {
            // Could not extract amount, ask AI to help
            getActivity().runOnUiThread(() -> {
                messages.set(analyzingIndex, new ChatMessage(
                        "🤔 Tôi không thể xác định số tiền ngân sách từ yêu cầu của bạn.\n\n" +
                        "Vui lòng nhập rõ số tiền và tháng (nếu cần), ví dụ:\n\n" +
                        "📝 Đặt ngân sách:\n" +
                        "   • \"Đặt ngân sách tháng này 15 triệu\"\n" +
                        "   • \"Đặt ngân sách tháng 12 là 20 triệu\"\n\n" +
                        "➕ Tăng thêm (cộng vào ngân sách hiện tại):\n" +
                        "   • \"Nâng ngân sách 2 triệu\"\n" +
                        "   • \"Tăng thêm 1.5 triệu\"\n\n" +
                        "➖ Giảm bớt (trừ khỏi ngân sách hiện tại):\n" +
                        "   • \"Giảm ngân sách 500k\"\n" +
                        "   • \"Trừ 1 triệu\"\n\n" +
                        "🎯 Đặt lại thành số cụ thể:\n" +
                        "   • \"Tăng ngân sách lên 10 triệu\"\n" +
                        "   • \"Giảm ngân sách xuống 8 triệu\"",
                        false, "Bây giờ"));
                chatAdapter.notifyItemChanged(analyzingIndex);
            });
        }
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

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    getActivity().runOnUiThread(() -> {
                        // Replace analyzing message with error
                        messages.set(analyzingIndex, new ChatMessage("Lỗi kết nối AI.", false, "Bây giờ"));
                        chatAdapter.notifyItemChanged(analyzingIndex);
                        // Update network status
                        updateNetworkStatus();
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

                            android.util.Log.d("AiChatBottomSheet", "AI full response: " + aiText);
                            android.util.Log.d("AiChatBottomSheet", "Number of JSON objects found: " + allJsonParts.size());
                            android.util.Log.d("AiChatBottomSheet", "Display text: " + displayText);

                            getActivity().runOnUiThread(() -> {
                                // Replace analyzing message with display text
                                android.util.Log.d("AiChatBottomSheet", "Updating message at index: " + analyzingIndex + " with: " + displayText);
                                
                                // Format markdown text để dễ đọc hơn
                                String formattedDisplayText = TextFormatHelper.formatMarkdownText(displayText);
                                
                                messages.set(analyzingIndex, new ChatMessage(formattedDisplayText, false, "Bây giờ"));
                                chatAdapter.notifyItemChanged(analyzingIndex);
                                Log.d("AiChatBottomSheet", "AI response: " + formattedDisplayText);

                                messagesRecycler.smoothScrollToPosition(messages.size() - 1);
                                textToSpeech.speak(formattedDisplayText, TextToSpeech.QUEUE_FLUSH, null, null);

                                // Process ALL JSON objects found
                                if (!allJsonParts.isEmpty()) {
                                    android.util.Log.d("AiChatBottomSheet", "Processing " + allJsonParts.size() + " JSON objects");
                                    
                                    for (String jsonPart : allJsonParts) {
                                        try {
                                            android.util.Log.d("AiChatBottomSheet", "Routing to saveExpenseDirectly");
                                            saveExpenseDirectly(jsonPart);
                                        } catch (Exception e) {
                                            android.util.Log.e("AiChatBottomSheet", "Error processing JSON: " + jsonPart, e);
                                        }
                                    }
                                } else {
                                    android.util.Log.d("AiChatBottomSheet", "No JSON found in AI response");
                                }
                                
                                // Update network status after successful response
                                updateNetworkStatus();
                            });
                        } catch (Exception e) {
                            getActivity().runOnUiThread(() -> {
                                // Replace analyzing message with error
                                messages.set(analyzingIndex, new ChatMessage("Lỗi xử lý phản hồi AI.", false, "Bây giờ"));
                                chatAdapter.notifyItemChanged(analyzingIndex);
                                updateNetworkStatus();
                            });
                        }
                    } else {
                        getActivity().runOnUiThread(() -> {
                            // Replace analyzing message with error
                            messages.set(analyzingIndex, new ChatMessage("Lỗi từ AI: " + response.code(), false, "Bây giờ"));
                            chatAdapter.notifyItemChanged(analyzingIndex);
                            updateNetworkStatus();
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
                            
                            // Refresh HomeFragment if available
                            refreshHomeFragment();
                            
                            // Also refresh HistoryFragment if it exists
                            refreshHistoryFragment();
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
    
    // Method to refresh HomeFragment after successful transaction save
    private void refreshHomeFragment() {
        try {
            if (getActivity() != null && getActivity() instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) getActivity();
                // Find HomeFragment and refresh it
                androidx.fragment.app.FragmentManager fragmentManager = mainActivity.getSupportFragmentManager();
                androidx.navigation.fragment.NavHostFragment navHostFragment = 
                    (androidx.navigation.fragment.NavHostFragment) fragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main);
                
                if (navHostFragment != null) {
                    // Try to find HomeFragment in all child fragments, not just current one
                    androidx.fragment.app.FragmentManager childFragmentManager = navHostFragment.getChildFragmentManager();
                    
                    // First try current fragment
                    androidx.fragment.app.Fragment currentFragment = childFragmentManager.getPrimaryNavigationFragment();
                    if (currentFragment instanceof com.example.spending_management_app.ui.home.HomeFragment) {
                        com.example.spending_management_app.ui.home.HomeFragment homeFragment = 
                            (com.example.spending_management_app.ui.home.HomeFragment) currentFragment;
                        homeFragment.refreshRecentTransactions();
                        android.util.Log.d("AiChatBottomSheet", "HomeFragment refreshed (current fragment)");
                        return;
                    }
                    
                    // If not current, search in all fragments
                    for (androidx.fragment.app.Fragment fragment : childFragmentManager.getFragments()) {
                        if (fragment instanceof com.example.spending_management_app.ui.home.HomeFragment) {
                            com.example.spending_management_app.ui.home.HomeFragment homeFragment = 
                                (com.example.spending_management_app.ui.home.HomeFragment) fragment;
                            homeFragment.refreshRecentTransactions();
                            android.util.Log.d("AiChatBottomSheet", "HomeFragment refreshed (found in fragments list)");
                            return;
                        }
                    }
                    
                    android.util.Log.d("AiChatBottomSheet", "HomeFragment not found in any fragments");
                }
            }
        } catch (Exception e) {
            android.util.Log.e("AiChatBottomSheet", "Error refreshing HomeFragment", e);
        }
    }
    
    // Method to refresh HistoryFragment after successful transaction save
    private void refreshHistoryFragment() {
        try {
            if (getActivity() != null && getActivity() instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) getActivity();
                // Find HistoryFragment and refresh it
                androidx.fragment.app.FragmentManager fragmentManager = mainActivity.getSupportFragmentManager();
                androidx.navigation.fragment.NavHostFragment navHostFragment = 
                    (androidx.navigation.fragment.NavHostFragment) fragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main);
                
                if (navHostFragment != null) {
                    androidx.fragment.app.Fragment currentFragment = navHostFragment.getChildFragmentManager().getPrimaryNavigationFragment();
                    if (currentFragment instanceof com.example.spending_management_app.ui.history.HistoryFragment) {
                        com.example.spending_management_app.ui.history.HistoryFragment historyFragment = 
                            (com.example.spending_management_app.ui.history.HistoryFragment) currentFragment;
                        historyFragment.refreshTransactions();
                        android.util.Log.d("AiChatBottomSheet", "HistoryFragment refreshed after transaction save");
                    }
                }
            }
        } catch (Exception e) {
            android.util.Log.e("AiChatBottomSheet", "Error refreshing HistoryFragment", e);
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

    // Get comprehensive financial context from database
    private String getFinancialContext() {
        StringBuilder context = new StringBuilder();
        
        try {
            // Get current month date range
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date startOfMonth = cal.getTime();
            
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            Date endOfMonth = cal.getTime();

            // Get all transactions this month
            List<TransactionEntity> monthlyTransactions = AppDatabase.getInstance(getContext())
                    .transactionDao()
                    .getTransactionsByDateRange(startOfMonth, endOfMonth);

            // Calculate totals
            long totalExpense = 0;
            long totalIncome = 0;
            java.util.Map<String, Long> expensesByCategory = new java.util.HashMap<>();
            java.util.Map<String, Integer> transactionCountByDay = new java.util.HashMap<>();
            
            SimpleDateFormat dayFormat = new SimpleDateFormat("d", Locale.getDefault());
            
            for (TransactionEntity transaction : monthlyTransactions) {
                if ("expense".equals(transaction.type)) {
                    totalExpense += Math.abs(transaction.amount);
                    expensesByCategory.put(transaction.category, 
                        expensesByCategory.getOrDefault(transaction.category, 0L) + Math.abs(transaction.amount));
                } else if ("income".equals(transaction.type)) {
                    totalIncome += transaction.amount;
                }
                
                // Count transactions by day
                String day = dayFormat.format(transaction.date);
                transactionCountByDay.put(day, transactionCountByDay.getOrDefault(day, 0) + 1);
            }

            // Get budget info
            List<BudgetEntity> monthlyBudgets = AppDatabase.getInstance(getContext())
                    .budgetDao()
                    .getBudgetsByDateRange(startOfMonth, endOfMonth);

            // Build context string
            context.append("THÔNG TIN TÀI CHÍNH THÁNG NÀY:\n");
            context.append("- Tổng thu nhập: ").append(String.format(Locale.getDefault(), "%,d", totalIncome)).append(" VND\n");
            context.append("- Tổng chi tiêu: ").append(String.format(Locale.getDefault(), "%,d", totalExpense)).append(" VND\n");
            context.append("- Số dư ước tính: ").append(String.format(Locale.getDefault(), "%,d", (totalIncome - totalExpense))).append(" VND\n");
            
            if (!monthlyBudgets.isEmpty()) {
                BudgetEntity budget = monthlyBudgets.get(0);
                long remaining = budget.getMonthlyLimit() - totalExpense;
                context.append("- Ngân sách tháng: ").append(String.format(Locale.getDefault(), "%,d", budget.getMonthlyLimit())).append(" VND\n");
                context.append("- Còn lại: ").append(String.format(Locale.getDefault(), "%,d", remaining)).append(" VND\n");
                context.append("- Tỷ lệ sử dụng: ").append(String.format("%.1f", (double)totalExpense/budget.getMonthlyLimit()*100)).append("%\n");
            }
            
            context.append("\nCHI TIÊU THEO DANH MỤC:\n");
            for (java.util.Map.Entry<String, Long> entry : expensesByCategory.entrySet()) {
                double percentage = totalExpense > 0 ? (double)entry.getValue()/totalExpense*100 : 0;
                context.append("- ").append(entry.getKey()).append(": ")
                       .append(String.format(Locale.getDefault(), "%,d", entry.getValue()))
                       .append(" VND (").append(String.format("%.1f", percentage)).append("%)\n");
            }
            
            context.append("\nGAO DỊCH GẦN ĐÂY:\n");
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());
            List<TransactionEntity> recentTransactions = monthlyTransactions.stream()
                    .sorted((t1, t2) -> t2.date.compareTo(t1.date))
                    .limit(10)
                    .collect(java.util.stream.Collectors.toList());
            
            for (TransactionEntity t : recentTransactions) {
                context.append("- ").append(dateFormat.format(t.date)).append(": ")
                       .append(t.description).append(" (").append(t.category).append(") - ")
                       .append(String.format(Locale.getDefault(), "%,d", Math.abs(t.amount))).append(" VND\n");
            }

        } catch (Exception e) {
            context.append("Lỗi khi truy xuất dữ liệu tài chính: ").append(e.getMessage());
        }
        
        return context.toString();
    }

    // Send prompt to AI with financial context
    private void sendPromptToAIWithContext(String userQuery, String financialContext) {
        // Add temporary "Đang phân tích..." message
        int analyzingIndex = messages.size();
        messages.add(new ChatMessage("Đang phân tích dữ liệu tài chính...", false, "Bây giờ"));
        chatAdapter.notifyItemInserted(messages.size() - 1);
        messagesRecycler.smoothScrollToPosition(messages.size() - 1);

        try {
            JSONObject json = new JSONObject();

            // Get current date for AI context
            java.util.Calendar currentCalendar = java.util.Calendar.getInstance();
            int currentDay = currentCalendar.get(java.util.Calendar.DAY_OF_MONTH);
            int currentMonth = currentCalendar.get(java.util.Calendar.MONTH) + 1;
            int currentYear = currentCalendar.get(java.util.Calendar.YEAR);
            String currentDateInfo = String.format("Hôm nay là ngày %d/%d/%d", currentDay, currentMonth, currentYear);

            // Enhanced system instruction with financial analysis capabilities
            JSONObject systemInstruction = new JSONObject();
            JSONArray systemParts = new JSONArray();
            JSONObject systemPart = new JSONObject();
            
            // Use helper class for financial analysis instruction
            String enhancedInstruction = AiSystemInstructions.getFinancialAnalysisInstruction(
                currentDateInfo, financialContext
            );
            
            systemPart.put("text", enhancedInstruction);
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
                    .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=AIzaSyB7cKKNvETdnd379olrAJpXzEfmfIGyx-M")
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    getActivity().runOnUiThread(() -> {
                        messages.set(analyzingIndex, new ChatMessage("Lỗi kết nối AI.", false, "Bây giờ"));
                        chatAdapter.notifyItemChanged(analyzingIndex);
                        updateNetworkStatus();
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

                            // Format markdown text để dễ đọc
                            String formattedText = TextFormatHelper.formatMarkdownText(aiText);

                            getActivity().runOnUiThread(() -> {
                                messages.set(analyzingIndex, new ChatMessage(formattedText, false, "Bây giờ"));
                                chatAdapter.notifyItemChanged(analyzingIndex);
                                messagesRecycler.smoothScrollToPosition(messages.size() - 1);
                                textToSpeech.speak(formattedText, TextToSpeech.QUEUE_FLUSH, null, null);
                                updateNetworkStatus();
                            });
                        } catch (Exception e) {
                            getActivity().runOnUiThread(() -> {
                                messages.set(analyzingIndex, new ChatMessage("Lỗi xử lý phản hồi AI.", false, "Bây giờ"));
                                chatAdapter.notifyItemChanged(analyzingIndex);
                                updateNetworkStatus();
                            });
                        }
                    } else {
                        getActivity().runOnUiThread(() -> {
                            messages.set(analyzingIndex, new ChatMessage("Lỗi từ AI: " + response.code(), false, "Bây giờ"));
                            chatAdapter.notifyItemChanged(analyzingIndex);
                            updateNetworkStatus();
                        });
                    }
                }
            });
        } catch (Exception e) {
            messages.set(analyzingIndex, new ChatMessage("Lỗi gửi tin nhắn.", false, "Bây giờ"));
            chatAdapter.notifyItemChanged(analyzingIndex);
        }
    }

    
    // Get comprehensive budget context from database
    private String getBudgetContext() {
        StringBuilder context = new StringBuilder();
        
        try {
            // Get all budgets (last 12 months)
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MONTH, -12);
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date twelveMonthsAgo = cal.getTime();
            
            cal = Calendar.getInstance();
            cal.add(Calendar.MONTH, 6); // Include 6 months in future
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            Date sixMonthsLater = cal.getTime();
            
            List<BudgetEntity> allBudgets = AppDatabase.getInstance(getContext())
                    .budgetDao()
                    .getBudgetsByDateRangeOrdered(twelveMonthsAgo, sixMonthsLater);
            
            SimpleDateFormat monthYearFormat = new SimpleDateFormat("MM/yyyy", new Locale("vi", "VN"));
            
            context.append("THÔNG TIN NGÂN SÁCH:\n");
            
            if (allBudgets != null && !allBudgets.isEmpty()) {
                // Group by month
                java.util.Map<String, BudgetEntity> budgetsByMonth = new java.util.HashMap<>();
                for (BudgetEntity budget : allBudgets) {
                    String monthKey = monthYearFormat.format(budget.date);
                    if (!budgetsByMonth.containsKey(monthKey) || 
                        budget.date.after(budgetsByMonth.get(monthKey).date)) {
                        budgetsByMonth.put(monthKey, budget);
                    }
                }
                
                // Sort months
                java.util.List<String> sortedMonths = new java.util.ArrayList<>(budgetsByMonth.keySet());
                java.util.Collections.sort(sortedMonths);
                
                // Calculate current month
                Calendar currentCal = Calendar.getInstance();
                String currentMonth = monthYearFormat.format(currentCal.getTime());
                
                // List all budgets
                context.append("\nDanh sách ngân sách theo tháng:\n");
                for (String month : sortedMonths) {
                    BudgetEntity budget = budgetsByMonth.get(month);
                    String formattedAmount = String.format(Locale.getDefault(), "%,d", budget.monthlyLimit);
                    
                    String marker = month.equals(currentMonth) ? " (Tháng hiện tại)" : "";
                    context.append("- Tháng ").append(month).append(marker).append(": ")
                           .append(formattedAmount).append(" VND\n");
                }
                
                // Calculate statistics
                long totalBudget = 0;
                long maxBudget = Long.MIN_VALUE;
                long minBudget = Long.MAX_VALUE;
                String maxMonth = "";
                String minMonth = "";
                
                for (String month : sortedMonths) {
                    BudgetEntity budget = budgetsByMonth.get(month);
                    totalBudget += budget.monthlyLimit;
                    
                    if (budget.monthlyLimit > maxBudget) {
                        maxBudget = budget.monthlyLimit;
                        maxMonth = month;
                    }
                    
                    if (budget.monthlyLimit < minBudget) {
                        minBudget = budget.monthlyLimit;
                        minMonth = month;
                    }
                }
                
                long avgBudget = totalBudget / sortedMonths.size();
                
                context.append("\nThống kê ngân sách:\n");
                context.append("- Tổng số tháng đã thiết lập: ").append(sortedMonths.size()).append("\n");
                context.append("- Ngân sách trung bình: ").append(String.format(Locale.getDefault(), "%,d", avgBudget)).append(" VND\n");
                context.append("- Ngân sách cao nhất: ").append(String.format(Locale.getDefault(), "%,d", maxBudget))
                       .append(" VND (Tháng ").append(maxMonth).append(")\n");
                context.append("- Ngân sách thấp nhất: ").append(String.format(Locale.getDefault(), "%,d", minBudget))
                       .append(" VND (Tháng ").append(minMonth).append(")\n");
                
                // Current month budget status
                if (budgetsByMonth.containsKey(currentMonth)) {
                    BudgetEntity currentBudget = budgetsByMonth.get(currentMonth);
                    context.append("\nNgân sách tháng hiện tại: ")
                           .append(String.format(Locale.getDefault(), "%,d", currentBudget.monthlyLimit))
                           .append(" VND\n");
                } else {
                    context.append("\nNgân sách tháng hiện tại: Chưa thiết lập\n");
                }
                
            } else {
                context.append("Chưa có ngân sách nào được thiết lập.\n");
            }
            
            // ========== THÊM THÔNG TIN NGÂN SÁCH DANH MỤC ==========
            context.append("\n");
            context.append("NGÂN SÁCH THEO DANH MỤC (THÁNG HIỆN TẠI):\n");
            
            try {
                // Get current month range
                Calendar currentCal = Calendar.getInstance();
                currentCal.set(Calendar.DAY_OF_MONTH, 1);
                currentCal.set(Calendar.HOUR_OF_DAY, 0);
                currentCal.set(Calendar.MINUTE, 0);
                currentCal.set(Calendar.SECOND, 0);
                currentCal.set(Calendar.MILLISECOND, 0);
                Date startOfMonth = currentCal.getTime();
                
                currentCal.set(Calendar.DAY_OF_MONTH, currentCal.getActualMaximum(Calendar.DAY_OF_MONTH));
                currentCal.set(Calendar.HOUR_OF_DAY, 23);
                currentCal.set(Calendar.MINUTE, 59);
                currentCal.set(Calendar.SECOND, 59);
                currentCal.set(Calendar.MILLISECOND, 999);
                Date endOfMonth = currentCal.getTime();
                
                // Get all category budgets for current month
                List<com.example.spending_management_app.database.CategoryBudgetEntity> categoryBudgets = 
                        AppDatabase.getInstance(getContext())
                                .categoryBudgetDao()
                                .getAllCategoryBudgetsForMonth(startOfMonth, endOfMonth);
                
                if (categoryBudgets != null && !categoryBudgets.isEmpty()) {
                    // Calculate total
                    long totalCategoryBudget = 0;
                    for (com.example.spending_management_app.database.CategoryBudgetEntity budget : categoryBudgets) {
                        totalCategoryBudget += budget.getBudgetAmount();
                    }
                    
                    context.append("Tổng ngân sách đã phân bổ: ")
                           .append(String.format(Locale.getDefault(), "%,d", totalCategoryBudget))
                           .append(" VND\n\n");
                    
                    // Sort by amount (highest first)
                    categoryBudgets.sort((a, b) -> Long.compare(b.getBudgetAmount(), a.getBudgetAmount()));
                    
                    // List all category budgets
                    context.append("Chi tiết ngân sách từng danh mục:\n");
                    for (com.example.spending_management_app.database.CategoryBudgetEntity budget : categoryBudgets) {
                        String formattedAmount = String.format(Locale.getDefault(), "%,d", budget.getBudgetAmount());
                        context.append("- ").append(budget.getCategory()).append(": ")
                               .append(formattedAmount).append(" VND\n");
                    }
                    
                    // Calculate percentage for top categories
                    if (totalCategoryBudget > 0) {
                        context.append("\nTỷ lệ phân bổ ngân sách:\n");
                        for (int i = 0; i < Math.min(5, categoryBudgets.size()); i++) {
                            com.example.spending_management_app.database.CategoryBudgetEntity budget = categoryBudgets.get(i);
                            double percentage = (budget.getBudgetAmount() * 100.0) / totalCategoryBudget;
                            context.append("- ").append(budget.getCategory()).append(": ")
                                   .append(String.format(Locale.getDefault(), "%.1f%%", percentage)).append("\n");
                        }
                    }
                } else {
                    context.append("Chưa có ngân sách danh mục nào được thiết lập.\n");
                }
            } catch (Exception e) {
                context.append("Lỗi khi truy xuất ngân sách danh mục: ").append(e.getMessage()).append("\n");
                android.util.Log.e("AiChatBottomSheet", "Error getting category budget context", e);
            }
            
        } catch (Exception e) {
            context.append("Lỗi khi truy xuất dữ liệu ngân sách: ").append(e.getMessage());
            android.util.Log.e("AiChatBottomSheet", "Error getting budget context", e);
        }
        
        return context.toString();
    }
    
    // Send prompt to AI with budget context
    /**
     * Send prompt to AI with budget context using GeminiAI service
     * This method uses callback pattern to handle UI updates
     */
    private void sendPromptToAIWithBudgetContext(String userQuery, String budgetContext) {
        // Add temporary "Đang phân tích..." message
        int analyzingIndex = messages.size();
        messages.add(new ChatMessage("Đang phân tích ngân sách...", false, "Bây giờ"));
        chatAdapter.notifyItemInserted(messages.size() - 1);
        messagesRecycler.smoothScrollToPosition(messages.size() - 1);

        // Use GeminiAI service with callback
        GeminiAI.sendPromptWithBudgetContext(userQuery, budgetContext, new GeminiAI.AIResponseCallback() {
            @Override
            public void onSuccess(String formattedResponse) {
                // Update UI with AI response
                messages.set(analyzingIndex, new ChatMessage(formattedResponse, false, "Bây giờ"));
                chatAdapter.notifyItemChanged(analyzingIndex);
                messagesRecycler.smoothScrollToPosition(messages.size() - 1);
                textToSpeech.speak(formattedResponse, TextToSpeech.QUEUE_FLUSH, null, null);
                updateNetworkStatus();
            }

            @Override
            public void onFailure(String errorMessage) {
                // Update UI with error message
                messages.set(analyzingIndex, new ChatMessage(errorMessage, false, "Bây giờ"));
                chatAdapter.notifyItemChanged(analyzingIndex);
                updateNetworkStatus();
            }
        });
    }
    
    // Handle delete budget request
    private void handleDeleteBudget(String text) {
        // Add analyzing message
        int analyzingIndex = messages.size();
        messages.add(new ChatMessage("Đang xử lý yêu cầu xóa...", false, "Bây giờ"));
        chatAdapter.notifyItemInserted(messages.size() - 1);
        messagesRecycler.smoothScrollToPosition(messages.size() - 1);
        
        // Extract month and year from text
        int[] monthYear = DateParser.extractMonthYear(text);
        int targetMonth = monthYear[0];
        int targetYear = monthYear[1];
        
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Create calendar for target month
                Calendar targetCal = Calendar.getInstance();
                targetCal.set(Calendar.YEAR, targetYear);
                targetCal.set(Calendar.MONTH, targetMonth - 1);
                targetCal.set(Calendar.DAY_OF_MONTH, 1);
                targetCal.set(Calendar.HOUR_OF_DAY, 0);
                targetCal.set(Calendar.MINUTE, 0);
                targetCal.set(Calendar.SECOND, 0);
                targetCal.set(Calendar.MILLISECOND, 0);
                Date startOfMonth = targetCal.getTime();
                
                targetCal.set(Calendar.DAY_OF_MONTH, targetCal.getActualMaximum(Calendar.DAY_OF_MONTH));
                targetCal.set(Calendar.HOUR_OF_DAY, 23);
                targetCal.set(Calendar.MINUTE, 59);
                targetCal.set(Calendar.SECOND, 59);
                Date endOfMonth = targetCal.getTime();
                
                // Check if budget exists
                List<BudgetEntity> existingBudgets = AppDatabase.getInstance(getContext())
                        .budgetDao()
                        .getBudgetsByDateRangeOrdered(startOfMonth, endOfMonth);
                
                if (existingBudgets != null && !existingBudgets.isEmpty()) {
                    // Get the budget amount before deleting
                    long budgetAmount = existingBudgets.get(0).monthlyLimit;
                    
                    // Delete budget
                    AppDatabase.getInstance(getContext())
                            .budgetDao()
                            .deleteBudgetsByDateRange(startOfMonth, endOfMonth);
                    
                    // Log budget history
                    com.example.spending_management_app.utils.BudgetHistoryLogger.logMonthlyBudgetDeleted(
                            getContext(), budgetAmount, startOfMonth);
                    
                    SimpleDateFormat monthYearFormat = new SimpleDateFormat("MM/yyyy", new Locale("vi", "VN"));
                    String monthYearStr = monthYearFormat.format(startOfMonth);
                    
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            String responseMessage = "✅ Đã xóa ngân sách tháng " + monthYearStr + "!\n\n" +
                                    "Bạn có thể thiết lập lại bất cứ lúc nào. 💰";
                            
                            messages.set(analyzingIndex, new ChatMessage(responseMessage, false, "Bây giờ"));
                            chatAdapter.notifyItemChanged(analyzingIndex);
                            messagesRecycler.smoothScrollToPosition(messages.size() - 1);
                            
                            showToastOnTop("✅ Đã xóa ngân sách tháng " + monthYearStr);
                            refreshHomeFragment();
                        });
                    }
                } else {
                    SimpleDateFormat monthYearFormat = new SimpleDateFormat("MM/yyyy", new Locale("vi", "VN"));
                    String monthYearStr = monthYearFormat.format(startOfMonth);
                    
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            String responseMessage = "⚠️ Không tìm thấy ngân sách tháng " + monthYearStr + " để xóa!";
                            
                            messages.set(analyzingIndex, new ChatMessage(responseMessage, false, "Bây giờ"));
                            chatAdapter.notifyItemChanged(analyzingIndex);
                        });
                    }
                }
                
            } catch (Exception e) {
                android.util.Log.e("AiChatBottomSheet", "Error deleting budget", e);
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        messages.set(analyzingIndex, new ChatMessage(
                                "❌ Có lỗi xảy ra khi xóa ngân sách. Vui lòng thử lại!", 
                                false, "Bây giờ"));
                        chatAdapter.notifyItemChanged(analyzingIndex);
                        showErrorToast("Lỗi xóa ngân sách");
                    });
                }
            }
        });
    }
    
    private void handleCategoryBudgetRequest(String text) {
        android.util.Log.d("AiChatBottomSheet", "handleCategoryBudgetRequest: " + text);
        
        // Add analyzing message
        int analyzingIndex = messages.size();
        messages.add(new ChatMessage("Đang xử lý yêu cầu...", false, "Bây giờ"));
        chatAdapter.notifyItemInserted(messages.size() - 1);
        messagesRecycler.smoothScrollToPosition(messages.size() - 1);
        
        String lowerText = text.toLowerCase();
        
        // Parse multiple operations from text
        List<CategoryBudgetOperation> operations = parseMultipleCategoryOperations(text);
        
        if (operations.isEmpty()) {
            // Unknown command
            getActivity().runOnUiThread(() -> {
                messages.set(analyzingIndex, new ChatMessage(
                        "⚠️ Không hiểu yêu cầu của bạn.\n\n" +
                        "💡 Hướng dẫn:\n" +
                        "• Đặt: 'Đặt ngân sách ăn uống 2 triệu'\n" +
                        "• Sửa: 'Sửa ngân sách di chuyển 1 triệu'\n" +
                        "• Xóa: 'Xóa ngân sách cafe'\n" +
                        "• Nhiều: 'Thêm 500k ăn uống và 300k di chuyển'",
                        false, "Bây giờ"));
                chatAdapter.notifyItemChanged(analyzingIndex);
            });
            return;
        }
        
        // Process all operations
        processCategoryBudgetOperations(operations, analyzingIndex);
    }
    
    // Helper class for category budget operations
    private static class CategoryBudgetOperation {
        String type; // "add", "edit", "delete"
        String category;
        long amount;
        
        CategoryBudgetOperation(String type, String category, long amount) {
            this.type = type;
            this.category = category;
            this.amount = amount;
        }
    }
    
    private List<CategoryBudgetOperation> parseMultipleCategoryOperations(String text) {
        List<CategoryBudgetOperation> operations = new ArrayList<>();
        String lowerText = text.toLowerCase();
        
        // Check if user wants to delete ALL category budgets
        if ((lowerText.contains("xóa") || lowerText.contains("xoá") || 
             lowerText.contains("thiết lập lại") || lowerText.contains("đặt lại") ||
             lowerText.contains("reset")) && 
            (lowerText.contains("tất cả") || lowerText.contains("hết"))) {
            
            // Special operation: delete all categories
            operations.add(new CategoryBudgetOperation("delete_all", "ALL", 0));
            return operations;
        }
        
        // Determine operation type
        String operationType = "edit"; // default
        if (lowerText.contains("xóa") || lowerText.contains("xoá")) {
            operationType = "delete";
        } else if (lowerText.contains("thêm")) {
            operationType = "add";
        } else if (lowerText.contains("sửa") || lowerText.contains("thay đổi")) {
            operationType = "edit";
        }
        
        // List of all categories with their aliases (shortened names)
        java.util.Map<String, String> categoryAliases = new java.util.HashMap<>();
        
        // Full category names
        String[] allCategories = {
            "Ăn uống", "Di chuyển", "Tiện ích", "Y tế", "Nhà ở",
            "Mua sắm", "Giáo dục", "Sách & Học tập", "Thể thao", "Sức khỏe & Làm đẹp",
            "Giải trí", "Du lịch", "Ăn ngoài & Cafe", "Quà tặng & Từ thiện", "Hội họp & Tiệc tụng",
            "Điện thoại & Internet", "Đăng ký & Dịch vụ", "Phần mềm & Apps", "Ngân hàng & Phí",
            "Con cái", "Thú cưng", "Gia đình",
            "Lương", "Đầu tư", "Thu nhập phụ", "Tiết kiệm",
            "Khác"
        };
        
        // Add aliases for categories with "&" (accept first part only)
        categoryAliases.put("sức khỏe", "Sức khỏe & Làm đẹp");
        categoryAliases.put("làm đẹp", "Sức khỏe & Làm đẹp");
        categoryAliases.put("ăn ngoài", "Ăn ngoài & Cafe");
        categoryAliases.put("cafe", "Ăn ngoài & Cafe");
        categoryAliases.put("cà phê", "Ăn ngoài & Cafe");
        categoryAliases.put("quà tặng", "Quà tặng & Từ thiện");
        categoryAliases.put("từ thiện", "Quà tặng & Từ thiện");
        categoryAliases.put("hội họp", "Hội họp & Tiệc tụng");
        categoryAliases.put("tiệc tụng", "Hội họp & Tiệc tụng");
        categoryAliases.put("điện thoại", "Điện thoại & Internet");
        categoryAliases.put("internet", "Điện thoại & Internet");
        categoryAliases.put("đăng ký", "Đăng ký & Dịch vụ");
        categoryAliases.put("dịch vụ", "Đăng ký & Dịch vụ");
        categoryAliases.put("phần mềm", "Phần mềm & Apps");
        categoryAliases.put("apps", "Phần mềm & Apps");
        categoryAliases.put("ngân hàng", "Ngân hàng & Phí");
        categoryAliases.put("phí", "Ngân hàng & Phí");
        categoryAliases.put("sách", "Sách & Học tập");
        categoryAliases.put("học tập", "Sách & Học tập");
        
        // Parse text more carefully by looking for explicit "category + amount" pairs
        // Split text by common separators
        String[] segments = lowerText.split("[,;]");
        
        for (String segment : segments) {
            segment = segment.trim();
            if (segment.isEmpty()) continue;
            
            // Try to find a category in this segment
            String matchedCategory = null;
            int matchedLength = 0;
            
            // First, try to match full category names (prefer longer matches)
            for (String category : allCategories) {
                String categoryLower = category.toLowerCase();
                
                // Check if this segment contains this category
                if (segment.contains(categoryLower)) {
                    // Prefer longer matches (e.g., "Đăng ký & Dịch vụ" over "Dịch vụ")
                    if (matchedCategory == null || categoryLower.length() > matchedLength) {
                        // Verify this is a standalone mention, not part of another word
                        int pos = segment.indexOf(categoryLower);
                        boolean validStart = (pos == 0 || !Character.isLetterOrDigit(segment.charAt(pos - 1)));
                        boolean validEnd = (pos + categoryLower.length() >= segment.length() || 
                                          !Character.isLetterOrDigit(segment.charAt(pos + categoryLower.length())));
                        
                        if (validStart && validEnd) {
                            matchedCategory = category;
                            matchedLength = categoryLower.length();
                        }
                    }
                }
            }
            
            // If no full match, try aliases
            if (matchedCategory == null) {
                for (java.util.Map.Entry<String, String> alias : categoryAliases.entrySet()) {
                    String aliasKey = alias.getKey();
                    
                    if (segment.contains(aliasKey)) {
                        // Verify this is a standalone mention
                        int pos = segment.indexOf(aliasKey);
                        boolean validStart = (pos == 0 || !Character.isLetterOrDigit(segment.charAt(pos - 1)));
                        boolean validEnd = (pos + aliasKey.length() >= segment.length() || 
                                          !Character.isLetterOrDigit(segment.charAt(pos + aliasKey.length())));
                        
                        if (validStart && validEnd) {
                            matchedCategory = alias.getValue();
                            matchedLength = aliasKey.length();
                        }
                    }
                }
            }
            
            if (matchedCategory != null) {
                long amount = 0;
                
                if (!operationType.equals("delete")) {
                    // Extract amount from this segment only
                    amount = BudgetAmountParser.extractBudgetAmount(segment);
                    
                    if (amount <= 0) {
                        continue; // Skip if no valid amount found for add/edit
                    }
                }
                
                operations.add(new CategoryBudgetOperation(operationType, matchedCategory, amount));
            }
        }
        
        return operations;
    }
    
    private long extractAmountNearCategoryPosition(String text, int categoryStart, int categoryEnd) {
        // Look for amount before and after category position (within 50 characters)
        int searchStart = Math.max(0, categoryStart - 50);
        int searchEnd = Math.min(text.length(), categoryEnd + 50);
        String searchArea = text.substring(searchStart, searchEnd);
        
        return BudgetAmountParser.extractBudgetAmount(searchArea);
    }
    
    private long extractAmountNearCategory(String text, String category) {
        // Find category position in text
        int categoryPos = text.toLowerCase().indexOf(category.toLowerCase());
        if (categoryPos == -1) return 0;
        
        return extractAmountNearCategoryPosition(text, categoryPos, categoryPos + category.length());
    }
    
    private void processCategoryBudgetOperations(List<CategoryBudgetOperation> operations, int analyzingIndex) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Get current month range
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.DAY_OF_MONTH, 1);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                Date startOfMonth = cal.getTime();
                
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
                cal.set(Calendar.HOUR_OF_DAY, 23);
                cal.set(Calendar.MINUTE, 59);
                cal.set(Calendar.SECOND, 59);
                cal.set(Calendar.MILLISECOND, 999);
                Date endOfMonth = cal.getTime();
                
                // Get monthly budget to check limit
                List<com.example.spending_management_app.database.BudgetEntity> monthlyBudgets = 
                        AppDatabase.getInstance(getContext()).budgetDao()
                                .getBudgetsByDateRange(startOfMonth, endOfMonth);
                long monthlyBudgetLimit = (monthlyBudgets != null && !monthlyBudgets.isEmpty()) 
                        ? monthlyBudgets.get(0).getMonthlyLimit() : 0;
                
                StringBuilder resultMessage = new StringBuilder();
                final int[] counts = new int[]{0, 0}; // [0] = successCount, [1] = failCount
                
                // Check if this is a "delete all" operation
                if (!operations.isEmpty() && operations.get(0).type.equals("delete_all")) {
                    try {
                        // Get all category budgets for current month
                        List<com.example.spending_management_app.database.CategoryBudgetEntity> allBudgets = 
                                AppDatabase.getInstance(getContext()).categoryBudgetDao()
                                        .getAllCategoryBudgetsForMonth(startOfMonth, endOfMonth);
                        
                        if (allBudgets != null && !allBudgets.isEmpty()) {
                            // Delete all category budgets
                            for (com.example.spending_management_app.database.CategoryBudgetEntity budget : allBudgets) {
                                AppDatabase.getInstance(getContext()).categoryBudgetDao().delete(budget);
                                counts[0]++;
                            }
                            
                            // Log budget history for delete all
                            com.example.spending_management_app.utils.BudgetHistoryLogger.logAllCategoryBudgetsDeleted(
                                    getContext());
                            
                            resultMessage.append("✅ Đã xóa tất cả ngân sách danh mục (")
                                    .append(counts[0]).append(" danh mục)\n\n");
                            resultMessage.append("💡 Tất cả danh mục đã được đặt lại về trạng thái 'Chưa thiết lập'");
                        } else {
                            resultMessage.append("⚠️ Không có ngân sách danh mục nào để xóa!");
                            counts[1]++;
                        }
                        
                        String finalMessage = resultMessage.toString();
                        
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                messages.set(analyzingIndex, new ChatMessage(finalMessage, false, "Bây giờ"));
                                chatAdapter.notifyItemChanged(analyzingIndex);
                                
                                if (counts[0] > 0) {
                                    showToastOnTop("✅ Đã xóa tất cả ngân sách danh mục");
                                    refreshHomeFragment();
                                    refreshCategoryBudgetWelcomeMessage();
                                } else {
                                    showErrorToast("⚠️ Không có ngân sách nào để xóa");
                                }
                            });
                        }
                        
                    } catch (Exception e) {
                        android.util.Log.e("AiChatBottomSheet", "Error deleting all category budgets", e);
                        
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                messages.set(analyzingIndex, new ChatMessage(
                                        "❌ Có lỗi xảy ra khi xóa tất cả ngân sách danh mục!", 
                                        false, "Bây giờ"));
                                chatAdapter.notifyItemChanged(analyzingIndex);
                                showErrorToast("Lỗi xóa ngân sách");
                            });
                        }
                    }
                    return; // Exit early, don't process other operations
                }
                
                for (CategoryBudgetOperation op : operations) {
                    try {
                        if (op.type.equals("delete")) {
                            // Delete operation
                            com.example.spending_management_app.database.CategoryBudgetEntity existing = 
                                    AppDatabase.getInstance(getContext())
                                            .categoryBudgetDao()
                                            .getCategoryBudgetForMonth(op.category, startOfMonth, endOfMonth);
                            
                            if (existing != null) {
                                long deletedAmount = existing.budgetAmount;
                                AppDatabase.getInstance(getContext()).categoryBudgetDao().delete(existing);
                                
                                // Log budget history
                                com.example.spending_management_app.utils.BudgetHistoryLogger.logCategoryBudgetDeleted(
                                        getContext(), op.category, deletedAmount);
                                
                                String icon = CategoryIconHelper.getIconEmoji(op.category);
                                resultMessage.append("✅ Xóa ").append(icon).append(" ").append(op.category).append("\n");
                                counts[0]++;
                            } else {
                                resultMessage.append("⚠️ ").append(op.category).append(": Không tìm thấy\n");
                                counts[1]++;
                            }
                        } else {
                            // Add or Edit operation
                            com.example.spending_management_app.database.CategoryBudgetEntity existing = 
                                    AppDatabase.getInstance(getContext())
                                            .categoryBudgetDao()
                                            .getCategoryBudgetForMonth(op.category, startOfMonth, endOfMonth);
                            
                            boolean isUpdate = (existing != null);
                            
                            // Check if adding/updating will exceed monthly budget
                            if (monthlyBudgetLimit > 0) {
                                List<com.example.spending_management_app.database.CategoryBudgetEntity> allCategoryBudgets = 
                                        AppDatabase.getInstance(getContext()).categoryBudgetDao()
                                                .getAllCategoryBudgetsForMonth(startOfMonth, endOfMonth);
                                
                                long currentTotal = 0;
                                for (com.example.spending_management_app.database.CategoryBudgetEntity cb : allCategoryBudgets) {
                                    if (!cb.getCategory().equals(op.category)) {
                                        currentTotal += cb.getBudgetAmount();
                                    }
                                }
                                
                                long newTotal = currentTotal + op.amount;
                                
                                if (newTotal > monthlyBudgetLimit) {
                                    String icon = CategoryIconHelper.getIconEmoji(op.category);
                                    long available = monthlyBudgetLimit - currentTotal;
                                    resultMessage.append(String.format("⚠️ %s %s: Vượt ngân sách tháng %,d VND (Ngân sách còn lại: %,d VND)\n", 
                                            icon, op.category, monthlyBudgetLimit, available));
                                    counts[1]++;
                                    continue;
                                }
                            }
                            
                            if (isUpdate) {
                                long oldAmount = existing.budgetAmount;
                                existing.budgetAmount = op.amount;
                                AppDatabase.getInstance(getContext()).categoryBudgetDao().update(existing);
                                
                                // Log budget history
                                com.example.spending_management_app.utils.BudgetHistoryLogger.logCategoryBudgetUpdated(
                                        getContext(), op.category, oldAmount, op.amount);
                            } else {
                                com.example.spending_management_app.database.CategoryBudgetEntity newBudget = 
                                        new com.example.spending_management_app.database.CategoryBudgetEntity(
                                                op.category, op.amount, startOfMonth);
                                AppDatabase.getInstance(getContext()).categoryBudgetDao().insert(newBudget);
                                
                                // Log budget history
                                com.example.spending_management_app.utils.BudgetHistoryLogger.logCategoryBudgetCreated(
                                        getContext(), op.category, op.amount);
                            }
                            
                            String icon = CategoryIconHelper.getIconEmoji(op.category);
                            String formattedAmount = String.format("%,d", op.amount);
                            String action = isUpdate ? "Sửa" : "Thêm";
                            resultMessage.append("✅ ").append(action).append(" ").append(icon).append(" ")
                                    .append(op.category).append(": ").append(formattedAmount).append(" VND\n");
                            counts[0]++;
                        }
                    } catch (Exception e) {
                        android.util.Log.e("AiChatBottomSheet", "Error processing operation for " + op.category, e);
                        resultMessage.append("❌ ").append(op.category).append(": Lỗi\n");
                        counts[1]++;
                    }
                }
                
                // Add summary
                resultMessage.append("\n📊 Kết quả: ")
                        .append(counts[0]).append(" thành công");
                if (counts[1] > 0) {
                    resultMessage.append(", ").append(counts[1]).append(" thất bại");
                }
                
                // If there are successful operations, show remaining budget info
                if (counts[0] > 0 && monthlyBudgetLimit > 0) {
                    // Recalculate total after all operations
                    List<com.example.spending_management_app.database.CategoryBudgetEntity> updatedBudgets = 
                            AppDatabase.getInstance(getContext()).categoryBudgetDao()
                                    .getAllCategoryBudgetsForMonth(startOfMonth, endOfMonth);
                    
                    long totalUsed = 0;
                    for (com.example.spending_management_app.database.CategoryBudgetEntity cb : updatedBudgets) {
                        totalUsed += cb.getBudgetAmount();
                    }
                    
                    long remaining = monthlyBudgetLimit - totalUsed;
                    resultMessage.append("\n\n💰 Ngân sách tháng: ").append(String.format("%,d", monthlyBudgetLimit)).append(" VND");
                    resultMessage.append("\n📈 Đã phân bổ: ").append(String.format("%,d", totalUsed)).append(" VND");
                    
                    if (remaining >= 0) {
                        resultMessage.append("\n✅ Còn lại: ").append(String.format("%,d", remaining)).append(" VND");
                    } else {
                        resultMessage.append("\n⚠️ Vượt quá: ").append(String.format("%,d", Math.abs(remaining))).append(" VND");
                    }
                }
                
                String finalMessage = resultMessage.toString();
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        messages.set(analyzingIndex, new ChatMessage(finalMessage, false, "Bây giờ"));
                        chatAdapter.notifyItemChanged(analyzingIndex);
                        
                        // Show toast based on result
                        if (counts[1] > 0) {
                            // Has failures - show error toast in red
                            if (counts[0] > 0) {
                                // Mixed results
                                showErrorToast("⚠️ " + counts[0] + " thành công, " + counts[1] + " thất bại");
                            } else {
                                // All failed
                                showErrorToast("❌ Thất bại: " + counts[1] + " danh mục");
                            }
                        } else {
                            // All success - show success toast in green
                            showToastOnTop("✅ Cập nhật " + counts[0] + " danh mục");
                        }
                        
                        refreshHomeFragment();
                        
                        // Refresh welcome message with updated data
                        refreshCategoryBudgetWelcomeMessage();
                    });
                }
                
            } catch (Exception e) {
                android.util.Log.e("AiChatBottomSheet", "Error processing category budget operations", e);
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        messages.set(analyzingIndex, new ChatMessage(
                                "❌ Có lỗi xảy ra khi xử lý yêu cầu!", 
                                false, "Bây giờ"));
                        chatAdapter.notifyItemChanged(analyzingIndex);
                    });
                }
            }
        });
    }
    
    private void refreshCategoryBudgetWelcomeMessage() {
        // Refresh the first message (welcome message) with updated category budget data
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Get current month range
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.DAY_OF_MONTH, 1);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                Date startOfMonth = cal.getTime();
                
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
                cal.set(Calendar.HOUR_OF_DAY, 23);
                cal.set(Calendar.MINUTE, 59);
                cal.set(Calendar.SECOND, 59);
                cal.set(Calendar.MILLISECOND, 999);
                Date endOfMonth = cal.getTime();
                
                // Get monthly budget for current month
                List<com.example.spending_management_app.database.BudgetEntity> monthlyBudgets = 
                        AppDatabase.getInstance(getContext()).budgetDao()
                                .getBudgetsByDateRange(startOfMonth, endOfMonth);
                long monthlyBudget = (monthlyBudgets != null && !monthlyBudgets.isEmpty()) 
                        ? monthlyBudgets.get(0).getMonthlyLimit() : 0;
                
                // Get all category budgets for current month
                List<com.example.spending_management_app.database.CategoryBudgetEntity> categoryBudgets = 
                        AppDatabase.getInstance(getContext())
                                .categoryBudgetDao()
                                .getAllCategoryBudgetsForMonth(startOfMonth, endOfMonth);
                
                // Define all categories in order
                String[] allCategories = {
                    "Ăn uống", "Di chuyển", "Tiện ích", "Y tế", "Nhà ở",
                    "Mua sắm", "Giáo dục", "Sách & Học tập", "Thể thao", "Sức khỏe & Làm đẹp",
                    "Giải trí", "Du lịch", "Ăn ngoài & Cafe", "Quà tặng & Từ thiện", "Hội họp & Tiệc tụng",
                    "Điện thoại & Internet", "Đăng ký & Dịch vụ", "Phần mềm & Apps", "Ngân hàng & Phí",
                    "Con cái", "Thú cưng", "Gia đình",
                    "Lương", "Đầu tư", "Thu nhập phụ", "Tiết kiệm",
                    "Khác"
                };
                
                // Create map of existing budgets
                java.util.Map<String, Long> budgetMap = new java.util.HashMap<>();
                long totalCategoryBudget = 0;
                if (categoryBudgets != null) {
                    for (com.example.spending_management_app.database.CategoryBudgetEntity budget : categoryBudgets) {
                        budgetMap.put(budget.getCategory(), budget.getBudgetAmount());
                        totalCategoryBudget += budget.getBudgetAmount();
                    }
                }
                
                // Create list with budgets and amounts
                class CategoryInfo {
                    String category;
                    long amount;
                    CategoryInfo(String category, long amount) {
                        this.category = category;
                        this.amount = amount;
                    }
                }
                
                List<CategoryInfo> allCategoryInfo = new ArrayList<>();
                for (String category : allCategories) {
                    long amount = budgetMap.getOrDefault(category, 0L);
                    allCategoryInfo.add(new CategoryInfo(category, amount));
                }
                
                // Sort: budgets set (high to low) then unset categories
                allCategoryInfo.sort((a, b) -> {
                    if (a.amount > 0 && b.amount == 0) return -1;
                    if (a.amount == 0 && b.amount > 0) return 1;
                    if (a.amount > 0 && b.amount > 0) return Long.compare(b.amount, a.amount);
                    return 0;
                });
                
                // Build updated message
                StringBuilder message = new StringBuilder();
                message.append("📊 Ngân sách theo danh mục hiện tại:\n\n");
                
                // Show monthly budget info
                if (monthlyBudget > 0) {
                    message.append(String.format("💰 Ngân sách tháng: %,d VND\n", monthlyBudget));
                    message.append(String.format("📈 Tổng ngân sách danh mục: %,d VND\n", totalCategoryBudget));
                    
                    long remaining = monthlyBudget - totalCategoryBudget;
                    if (remaining >= 0) {
                        message.append(String.format("✅ Còn lại: %,d VND\n\n", remaining));
                    } else {
                        message.append(String.format("⚠️ Vượt quá: %,d VND\n\n", Math.abs(remaining)));
                    }
                } else {
                    message.append("⚠️ Chưa thiết lập ngân sách tháng\n");
                    message.append("💡 Hãy thêm ngân sách tháng trước!\n\n");
                }
                
                for (CategoryInfo info : allCategoryInfo) {
                    String icon = CategoryIconHelper.getIconEmoji(info.category);
                    if (info.amount > 0) {
                        message.append(String.format("%s %s: %,d VND\n", 
                                icon, info.category, info.amount));
                    } else {
                        message.append(String.format("%s %s: Chưa thiết lập\n", 
                                icon, info.category));
                    }
                }
                
                message.append("\n💡 Hướng dẫn:\n");
                message.append("        • Thêm: 'Thêm 500 ngàn ăn uống và 300 ngàn di chuyển'\n");
                message.append("        • Sửa: 'Sửa ăn uống 700 ngàn, mua sắm 400 ngàn'\n");
                message.append("        • Xóa: 'Xóa ngân sách ăn uống và di chuyển'\n");
                message.append("\n⚠️ Lưu ý: Tổng ngân sách danh mục không vượt quá ngân sách tháng");

                
                String finalMessage = message.toString();
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        // Update first message (welcome message)
                        if (!messages.isEmpty() && messages.get(0).message.contains("📊 Ngân sách theo danh mục")) {
                            messages.set(0, new ChatMessage(finalMessage, false, "Bây giờ"));
                            chatAdapter.notifyItemChanged(0);
                        }
                    });
                }
                
            } catch (Exception e) {
                android.util.Log.e("AiChatBottomSheet", "Error refreshing category budget welcome message", e);
            }
        });
    }
    
    
    // ==================== EXPENSE BULK MANAGEMENT ====================
    
    private void handleExpenseBulkRequest(String text) {
        android.util.Log.d("AiChatBottomSheet", "handleExpenseBulkRequest: " + text);
        
        // Add analyzing message
        int analyzingIndex = messages.size();
        messages.add(new ChatMessage("Đang xử lý yêu cầu...", false, "Bây giờ"));
        chatAdapter.notifyItemInserted(messages.size() - 1);
        messagesRecycler.smoothScrollToPosition(messages.size() - 1);
        
        String lowerText = text.toLowerCase();
        
        // Parse multiple expense operations from text
        List<ExpenseOperation> operations = parseMultipleExpenseOperations(text);
        
        if (operations.isEmpty()) {
            // Unknown command
            getActivity().runOnUiThread(() -> {
                messages.set(analyzingIndex, new ChatMessage(
                        "⚠️ Không hiểu yêu cầu của bạn.\n\n" +
                        "💡 Hướng dẫn:\n" +
                        "• Thêm: 'Hôm qua ăn sáng 25k và cafe 30k'\n" +
                        "• Sửa: 'Sửa chi tiêu [ID] thành 50k'\n" +
                        "• Xóa: 'Xóa chi tiêu [ID]'",
                        false, "Bây giờ"));
                chatAdapter.notifyItemChanged(analyzingIndex);
            });
            return;
        }
        
        // Process all operations
        processExpenseOperations(operations, analyzingIndex);
    }
    
    // Helper class for expense operations
    private static class ExpenseOperation {
        String type; // "add", "edit", "delete"
        String description;
        String category;
        long amount;
        Date date;
        int transactionId; // For edit/delete operations
        
        ExpenseOperation(String type, String description, String category, long amount, Date date) {
            this.type = type;
            this.description = description;
            this.category = category;
            this.amount = amount;
            this.date = date;
            this.transactionId = -1;
        }
        
        ExpenseOperation(String type, int transactionId) {
            this.type = type;
            this.transactionId = transactionId;
            this.description = "";
            this.category = "";
            this.amount = 0;
            this.date = new Date();
        }
    }
    
    private List<ExpenseOperation> parseMultipleExpenseOperations(String text) {
        List<ExpenseOperation> operations = new ArrayList<>();
        String lowerText = text.toLowerCase();
        
        // Determine operation type
        String operationType = "add"; // default
        if (lowerText.contains("xóa") || lowerText.contains("xoá")) {
            operationType = "delete";
        } else if (lowerText.contains("sửa") || lowerText.contains("thay đổi") || lowerText.contains("cập nhật")) {
            operationType = "edit";
        }
        
        // For edit/delete, try to extract transaction ID
        if (operationType.equals("delete") || operationType.equals("edit")) {
            // Try to find ID pattern like "#123", "ID 123", "id:123"
            Pattern idPattern = Pattern.compile("(?:#|id[:\\s]+)(\\d+)", Pattern.CASE_INSENSITIVE);
            Matcher matcher = idPattern.matcher(lowerText);
            
            while (matcher.find()) {
                int transactionId = Integer.parseInt(matcher.group(1));
                operations.add(new ExpenseOperation(operationType, transactionId));
            }
            
            // If no ID found but user said delete/edit, inform them
            if (operations.isEmpty()) {
                // Return empty - will show error message
                return operations;
            }
        }
        
        // For add operations, parse expenses from text
        if (operationType.equals("add")) {
            operations = parseExpensesFromText(text);
        }
        
        return operations;
    }
    
    private List<ExpenseOperation> parseExpensesFromText(String text) {
        List<ExpenseOperation> operations = new ArrayList<>();
        
        android.util.Log.d("AiChatBottomSheet", "=== parseExpensesFromText START ===");
        android.util.Log.d("AiChatBottomSheet", "Input text: [" + text + "]");
        
        // List of all categories with their aliases
        java.util.Map<String, String> categoryAliases = new java.util.HashMap<>();
        
        // Full category names
        String[] allCategories = {
            "Ăn uống", "Di chuyển", "Tiện ích", "Y tế", "Nhà ở",
            "Mua sắm", "Giáo dục", "Sách & Học tập", "Thể thao", "Sức khỏe & Làm đẹp",
            "Giải trí", "Du lịch", "Ăn ngoài & Cafe", "Quà tặng & Từ thiện", "Hội họp & Tiệc tụng",
            "Điện thoại & Internet", "Đăng ký & Dịch vụ", "Phần mềm & Apps", "Ngân hàng & Phí",
            "Con cái", "Thú cưng", "Gia đình", "Khác"
        };
        
        // Add aliases
        categoryAliases.put("ăn sáng", "Ăn uống");
        categoryAliases.put("ăn trưa", "Ăn uống");
        categoryAliases.put("ăn tối", "Ăn uống");
        categoryAliases.put("cafe", "Ăn ngoài & Cafe");
        categoryAliases.put("cà phê", "Ăn ngoài & Cafe");
        categoryAliases.put("cơm", "Ăn uống");
        categoryAliases.put("xăng", "Di chuyển");
        categoryAliases.put("xe", "Di chuyển");
        categoryAliases.put("taxi", "Di chuyển");
        categoryAliases.put("grab", "Di chuyển");
        categoryAliases.put("bus", "Di chuyển");
        categoryAliases.put("điện", "Tiện ích");
        categoryAliases.put("nước", "Tiện ích");
        categoryAliases.put("internet", "Điện thoại & Internet");
        categoryAliases.put("điện thoại", "Điện thoại & Internet");
        categoryAliases.put("phim", "Giải trí");
        categoryAliases.put("game", "Giải trí");
        
        // First, split by newlines to handle multi-line input
        String[] lines = text.split("\\r?\\n");
        
        android.util.Log.d("AiChatBottomSheet", "Number of lines: " + lines.length);
        for (int i = 0; i < lines.length; i++) {
            android.util.Log.d("AiChatBottomSheet", "Line " + i + ": [" + lines[i] + "]");
        }
        
        // Process each line separately
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            android.util.Log.d("AiChatBottomSheet", "Processing line: [" + line + "]");
            
            // Extract date from this line (each line can have its own date)
            Date expenseDate = DateParser.extractDateFromText(line);
            android.util.Log.d("AiChatBottomSheet", "Extracted date: " + expenseDate);
            
            // Split each line by common separators (và, ,, ;)
            String[] segments = line.split("\\s+(và|,|;)\\s+");
            
            android.util.Log.d("AiChatBottomSheet", "Number of segments in this line: " + segments.length);
            for (int i = 0; i < segments.length; i++) {
                android.util.Log.d("AiChatBottomSheet", "  Segment " + i + ": [" + segments[i] + "]");
            }
            
            for (String segment : segments) {
                segment = segment.trim();
                if (segment.isEmpty()) continue;
                
                android.util.Log.d("AiChatBottomSheet", "  Processing segment: [" + segment + "]");
                
                // Try to extract: description, amount, and category
                String description = "";
                String category = "Khác"; // default
                long amount = 0;
                
                // Extract amount
                amount = BudgetAmountParser.extractBudgetAmount(segment);
                android.util.Log.d("AiChatBottomSheet", "    Extracted amount: " + amount);
                
                if (amount <= 0) {
                    android.util.Log.d("AiChatBottomSheet", "    Skipping - no valid amount");
                    continue; // Skip if no valid amount
                }
                
                // Try to match category
                String matchedCategory = null;
                
                // First try full category names
                for (String cat : allCategories) {
                    if (segment.toLowerCase().contains(cat.toLowerCase())) {
                        matchedCategory = cat;
                        break;
                    }
                }
                
                // If no match, try aliases
                if (matchedCategory == null) {
                    for (java.util.Map.Entry<String, String> alias : categoryAliases.entrySet()) {
                        if (segment.toLowerCase().contains(alias.getKey())) {
                            matchedCategory = alias.getValue();
                            break;
                        }
                    }
                }
                
                if (matchedCategory != null) {
                    category = matchedCategory;
                }
                
                android.util.Log.d("AiChatBottomSheet", "    Matched category: " + category);
                
                // Extract description (everything except amount and category keywords)
                description = extractDescription(segment, category, amount);
                
                if (description.isEmpty()) {
                    description = category; // Use category as description if no description found
                }
                
                android.util.Log.d("AiChatBottomSheet", "    Final description: " + description);
                android.util.Log.d("AiChatBottomSheet", "    Creating expense: " + description + " - " + amount + " - " + category + " - " + expenseDate);
                
                operations.add(new ExpenseOperation("add", description, category, amount, expenseDate));
            }
        }
        
        android.util.Log.d("AiChatBottomSheet", "Total operations created: " + operations.size());
        android.util.Log.d("AiChatBottomSheet", "=== parseExpensesFromText END ===");
        
        return operations;
    }
    
    private String extractDescription(String text, String category, long amount) {
        String result = text;
        
        // Remove category
        result = result.replaceAll("(?i)" + Pattern.quote(category), "").trim();
        
        // Remove amount patterns
        result = result.replaceAll("\\d+[\\s]*(triệu|tr|ngàn|k|nghìn|n|đ|vnd)", "").trim();
        result = result.replaceAll("\\d+", "").trim();
        
        // Remove common keywords
        result = result.replaceAll("(?i)(chi tiêu|thêm|mua|đi|về)", "").trim();
        
        // Clean up extra spaces
        result = result.replaceAll("\\s+", " ").trim();
        
        return result;
    }

    
    private void processExpenseOperations(List<ExpenseOperation> operations, int analyzingIndex) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                StringBuilder resultMessage = new StringBuilder();
                int[] counts = {0, 0}; // success, failure
                
                for (ExpenseOperation op : operations) {
                    try {
                        if (op.type.equals("delete")) {
                            // Delete transaction
                            TransactionEntity transaction = AppDatabase.getInstance(getContext())
                                    .transactionDao()
                                    .getTransactionById(op.transactionId);
                            
                            if (transaction != null) {
                                AppDatabase.getInstance(getContext()).transactionDao().delete(transaction);
                                resultMessage.append("✅ Xóa: ").append(transaction.description)
                                        .append(" (").append(String.format("%,d", Math.abs(transaction.amount)))
                                        .append(" VND)\n");
                                counts[0]++;
                            } else {
                                resultMessage.append("⚠️ Không tìm thấy chi tiêu #").append(op.transactionId).append("\n");
                                counts[1]++;
                            }
                            
                        } else if (op.type.equals("edit")) {
                            // Edit transaction (not fully implemented in parse, just delete for now)
                            resultMessage.append("⚠️ Chức năng sửa chưa được hỗ trợ. Vui lòng xóa và thêm lại.\n");
                            counts[1]++;
                            
                        } else if (op.type.equals("add")) {
                            // Add new transaction
                            TransactionEntity newTransaction = new TransactionEntity(
                                    op.description,
                                    op.category,
                                    -Math.abs(op.amount), // Expense is negative
                                    op.date,
                                    "expense"
                            );
                            
                            AppDatabase.getInstance(getContext()).transactionDao().insert(newTransaction);
                            
                            String icon = CategoryIconHelper.getIconEmoji(op.category);
                            resultMessage.append("✅ Thêm ").append(icon).append(" ")
                                    .append(op.description).append(": ")
                                    .append(String.format("%,d", op.amount)).append(" VND")
                                    .append(" (").append(op.category).append(")\n");
                            counts[0]++;
                        }
                    } catch (Exception e) {
                        android.util.Log.e("AiChatBottomSheet", "Error processing expense operation", e);
                        resultMessage.append("❌ Lỗi xử lý: ").append(op.description).append("\n");
                        counts[1]++;
                    }
                }
                
                // Add summary
                resultMessage.append("\n📊 Kết quả: ")
                        .append(counts[0]).append(" thành công");
                if (counts[1] > 0) {
                    resultMessage.append(", ").append(counts[1]).append(" thất bại");
                }
                
                String finalMessage = resultMessage.toString();
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        messages.set(analyzingIndex, new ChatMessage(finalMessage, false, "Bây giờ"));
                        chatAdapter.notifyItemChanged(analyzingIndex);
                        
                        // Show toast based on result
                        if (counts[1] > 0) {
                            if (counts[0] > 0) {
                                showErrorToast("⚠️ " + counts[0] + " thành công, " + counts[1] + " thất bại");
                            } else {
                                showErrorToast("❌ Thất bại: " + counts[1] + " giao dịch");
                            }
                        } else {
                            showToastOnTop("✅ Thêm " + counts[0] + " chi tiêu");
                        }
                        
                        refreshHomeFragment();
                        
                        // Refresh welcome message with updated data
                        refreshExpenseWelcomeMessage();
                    });
                }
                
            } catch (Exception e) {
                android.util.Log.e("AiChatBottomSheet", "Error processing expense operations", e);
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        messages.set(analyzingIndex, new ChatMessage(
                                "❌ Có lỗi xảy ra khi xử lý yêu cầu!", 
                                false, "Bây giờ"));
                        chatAdapter.notifyItemChanged(analyzingIndex);
                    });
                }
            }
        });
    }
    
    private void refreshExpenseWelcomeMessage() {
        // Reload recent transactions and update the first message (welcome message)
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                List<TransactionEntity> recentTransactions = AppDatabase.getInstance(getContext())
                        .transactionDao()
                        .getRecentTransactions(5); // Show 5 recent transactions
                
                // Build updated welcome message
                StringBuilder welcomeMessage = new StringBuilder();
                welcomeMessage.append("📋 Quản lý chi tiêu hàng loạt\n\n");
                
                if (!recentTransactions.isEmpty()) {
                    welcomeMessage.append("💳 Chi tiêu gần đây:\n\n");
                    
                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM", new Locale("vi", "VN"));
                    
                    for (TransactionEntity transaction : recentTransactions) {
                        String emoji = CategoryIconHelper.getIconEmoji(transaction.category);
                        String formattedAmount = String.format("%,d", Math.abs(transaction.amount));
                        String dateStr = dateFormat.format(transaction.date);
                        
                        welcomeMessage.append(emoji).append(" ")
                                .append(transaction.description)
                                .append(": ").append(formattedAmount).append(" VND")
                                .append(" - ").append(dateStr)
                                .append("\n");
                    }
                    welcomeMessage.append("\n");
                }
                
                welcomeMessage.append("💡 Hướng dẫn:\n");
                welcomeMessage.append("• Thêm: 'Hôm qua ăn sáng 25k và cafe 30k'\n");
                welcomeMessage.append("• Xóa: 'Xóa chi tiêu #123' (tìm ID ở trang Lịch sử)");
                
                String finalMessage = welcomeMessage.toString();
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (!messages.isEmpty()) {
                            messages.set(0, new ChatMessage(finalMessage, false, "Bây giờ"));
                            chatAdapter.notifyItemChanged(0);
                        }
                    });
                }
                
            } catch (Exception e) {
                android.util.Log.e("AiChatBottomSheet", "Error refreshing expense welcome message", e);
            }
        });
    }
    
    // ==================== OFFLINE HANDLERS ====================
    
    private boolean handleOfflineAddExpense(String text) {
        try {
            // Extract amount using improved parser
            Long amount = BudgetAmountParser.parseAmount(text);
            if (amount == null) {
                return false;
            }
            
            // Extract date
            Date expenseDate = DateParser.parseDate(text);
            if (expenseDate == null) {
                expenseDate = new Date(); // Default to today
            }
            
            // Extract category (check if any category keyword exists)
            String category = CategoryHelper.detectCategory(text);
            
            // Extract description using proper method
            String description = ExpenseDescriptionParser.extractDescriptionOffline(text, category, amount);
            
            if (description.isEmpty()) {
                description = category; // Use category as description if empty
            }
            
            // Create transaction with proper constructor
            final String finalDesc = description;
            final String finalCategory = category;
            final long finalAmount = amount;
            final long expenseAmount = -Math.abs(amount); // Expense is negative
            final Date finalDate = expenseDate;
            
            TransactionEntity transaction = new TransactionEntity(
                    finalDesc,
                    finalCategory,
                    expenseAmount,
                    finalDate,
                    "expense"
            );
            
            // Save to database
            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    AppDatabase.getInstance(getContext())
                            .transactionDao()
                            .insert(transaction);
                    
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            String formattedAmount = String.format("%,d", finalAmount);
                            String successMsg = "✅ Đã thêm chi tiêu (Offline)\n\n" +
                                    "📝 " + finalDesc + "\n" +
                                    "💰 " + formattedAmount + " VND\n" +
                                    "📂 " + finalCategory;
                            messages.add(new ChatMessage(successMsg, false, "Bây giờ"));
                            chatAdapter.notifyItemInserted(messages.size() - 1);
                            messagesRecycler.smoothScrollToPosition(messages.size() - 1);
                            
                            // Toast notification like online
                            showToastOnTop("Đã thêm: " + finalDesc + " - " + formattedAmount + " VND");
                            
                            // Refresh home fragment and welcome message like online
                            refreshHomeFragment();
                            refreshExpenseWelcomeMessage();
                        });
                    }
                } catch (Exception e) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            messages.add(new ChatMessage("❌ Lỗi khi thêm chi tiêu: " + e.getMessage(), false, "Bây giờ"));
                            chatAdapter.notifyItemInserted(messages.size() - 1);
                            messagesRecycler.smoothScrollToPosition(messages.size() - 1);
                        });
                    }
                }
            });
            
            return true;
        } catch (Exception e) {
            android.util.Log.e("AiChatBottomSheet", "Error handling offline add expense", e);
            return false;
        }
    }
    
    private boolean handleOfflineDeleteExpense(String text) {
        try {
            // Extract ID from text: "Xóa chi tiêu #123" or "Xóa #123"
            Pattern idPattern = Pattern.compile("#(\\d+)");
            Matcher idMatcher = idPattern.matcher(text);
            
            if (!idMatcher.find()) {
                messages.add(new ChatMessage("❌ Không tìm thấy ID chi tiêu. Vui lòng sử dụng định dạng: 'Xóa chi tiêu #123'", false, "Bây giờ"));
                chatAdapter.notifyItemInserted(messages.size() - 1);
                messagesRecycler.smoothScrollToPosition(messages.size() - 1);
                return true;
            }
            
            int id = Integer.parseInt(idMatcher.group(1));
            
            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    TransactionEntity transaction = AppDatabase.getInstance(getContext())
                            .transactionDao()
                            .getTransactionById(id);
                    
                    if (transaction != null) {
                        AppDatabase.getInstance(getContext())
                                .transactionDao()
                                .delete(transaction);
                        
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                messages.add(new ChatMessage("✅ Đã xóa chi tiêu #" + id + " (Offline)", false, "Bây giờ"));
                                chatAdapter.notifyItemInserted(messages.size() - 1);
                                messagesRecycler.smoothScrollToPosition(messages.size() - 1);
                                
                                // Toast notification like online
                                showToastOnTop("Đã xóa chi tiêu #" + id);
                                
                                // Refresh home fragment and welcome message like online
                                refreshHomeFragment();
                                refreshExpenseWelcomeMessage();
                            });
                        }
                    } else {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                messages.add(new ChatMessage("❌ Không tìm thấy chi tiêu #" + id, false, "Bây giờ"));
                                chatAdapter.notifyItemInserted(messages.size() - 1);
                                messagesRecycler.smoothScrollToPosition(messages.size() - 1);
                            });
                        }
                    }
                } catch (Exception e) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            messages.add(new ChatMessage("❌ Lỗi khi xóa chi tiêu: " + e.getMessage(), false, "Bây giờ"));
                            chatAdapter.notifyItemInserted(messages.size() - 1);
                            messagesRecycler.smoothScrollToPosition(messages.size() - 1);
                        });
                    }
                }
            });
            
            return true;
        } catch (Exception e) {
            android.util.Log.e("AiChatBottomSheet", "Error handling offline delete expense", e);
            return false;
        }
    }
    
    private boolean handleOfflineUpdateBudget(String text) {
        try {
            String lowerText = text.toLowerCase();
            
            // Extract amount using improved parser
            Long amount = BudgetAmountParser.parseAmount(text);
            if (amount == null) {
                return false;
            }
            
            // Determine if it's absolute or relative change
            boolean isAbsoluteSet = lowerText.contains("lên") || lowerText.contains("xuống");
            boolean isIncrease = lowerText.contains("thêm") || lowerText.contains("nâng") || 
                                lowerText.contains("tăng") || (isAbsoluteSet && lowerText.contains("lên"));
            boolean isDecrease = lowerText.contains("giảm") || lowerText.contains("hạ") || 
                                lowerText.contains("cắt") || lowerText.contains("trừ") || 
                                lowerText.contains("bớt") || (isAbsoluteSet && lowerText.contains("xuống"));
            
            final long finalAmount = amount;
            final boolean finalIsAbsolute = isAbsoluteSet;
            final boolean finalIsIncrease = isIncrease;
            final boolean finalIsDecrease = isDecrease;
            
            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    // Get current month
                    Calendar cal = Calendar.getInstance();
                    cal.set(Calendar.DAY_OF_MONTH, 1);
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                    Date startDate = cal.getTime();
                    
                    cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
                    cal.set(Calendar.HOUR_OF_DAY, 23);
                    cal.set(Calendar.MINUTE, 59);
                    cal.set(Calendar.SECOND, 59);
                    Date endDate = cal.getTime();
                    
                    List<BudgetEntity> existingBudgets = AppDatabase.getInstance(getContext())
                            .budgetDao()
                            .getBudgetsByDateRange(startDate, endDate);
                    
                    long newAmount;
                    if (existingBudgets != null && !existingBudgets.isEmpty()) {
                        // Update existing budget
                        BudgetEntity budget = existingBudgets.get(0);
                        long oldAmount = budget.monthlyLimit;
                        
                        if (finalIsAbsolute) {
                            newAmount = finalAmount;
                        } else if (finalIsIncrease) {
                            newAmount = oldAmount + finalAmount;
                        } else if (finalIsDecrease) {
                            newAmount = oldAmount - finalAmount;
                        } else {
                            newAmount = finalAmount; // Default to set
                        }
                        
                        budget.monthlyLimit = newAmount;
                        AppDatabase.getInstance(getContext())
                                .budgetDao()
                                .update(budget);
                        
                        // Log budget history
                        com.example.spending_management_app.utils.BudgetHistoryLogger.logMonthlyBudgetUpdated(
                                getContext(), oldAmount, newAmount, startDate);
                    } else {
                        // Create new budget
                        newAmount = finalAmount;
                        BudgetEntity budget = new BudgetEntity(
                                null,           // category (null for monthly budget)
                                newAmount,      // monthlyLimit
                                0,              // currentSpent (start at 0)
                                startDate       // date
                        );
                        AppDatabase.getInstance(getContext())
                                .budgetDao()
                                .insert(budget);
                        
                        // Log budget history
                        com.example.spending_management_app.utils.BudgetHistoryLogger.logMonthlyBudgetCreated(
                                getContext(), newAmount, startDate);
                    }
                    
                    if (getActivity() != null) {
                        final long displayAmount = newAmount;
                        getActivity().runOnUiThread(() -> {
                            String formattedAmount = String.format("%,d", displayAmount);
                            messages.add(new ChatMessage("✅ Đã cập nhật ngân sách tháng (Offline)\n\n💰 " + formattedAmount + " VND", false, "Bây giờ"));
                            chatAdapter.notifyItemInserted(messages.size() - 1);
                            messagesRecycler.smoothScrollToPosition(messages.size() - 1);
                            
                            // Toast notification like online
                            showToastOnTop("Ngân sách đã được cập nhật: " + formattedAmount + " VND");
                            
                            // Refresh home fragment like online
                            refreshHomeFragment();
                        });
                    }
                } catch (Exception e) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            messages.add(new ChatMessage("❌ Lỗi khi cập nhật ngân sách: " + e.getMessage(), false, "Bây giờ"));
                            chatAdapter.notifyItemInserted(messages.size() - 1);
                            messagesRecycler.smoothScrollToPosition(messages.size() - 1);
                        });
                    }
                }
            });
            
            return true;
        } catch (Exception e) {
            android.util.Log.e("AiChatBottomSheet", "Error handling offline update budget", e);
            return false;
        }
    }
    
    private boolean handleOfflineDeleteBudget(String text) {
        try {
            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    // Get current month
                    Calendar cal = Calendar.getInstance();
                    cal.set(Calendar.DAY_OF_MONTH, 1);
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                    Date startDate = cal.getTime();
                    
                    cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
                    cal.set(Calendar.HOUR_OF_DAY, 23);
                    cal.set(Calendar.MINUTE, 59);
                    cal.set(Calendar.SECOND, 59);
                    Date endDate = cal.getTime();
                    
                    List<BudgetEntity> budgets = AppDatabase.getInstance(getContext())
                            .budgetDao()
                            .getBudgetsByDateRange(startDate, endDate);
                    
                    if (budgets != null && !budgets.isEmpty()) {
                        BudgetEntity budget = budgets.get(0);
                        long oldAmount = budget.monthlyLimit;
                        
                        AppDatabase.getInstance(getContext())
                                .budgetDao()
                                .delete(budget);
                        
                        // Log budget history
                        com.example.spending_management_app.utils.BudgetHistoryLogger.logMonthlyBudgetDeleted(
                                getContext(), oldAmount, startDate);
                        
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                messages.add(new ChatMessage("✅ Đã xóa ngân sách tháng này (Offline)", false, "Bây giờ"));
                                chatAdapter.notifyItemInserted(messages.size() - 1);
                                messagesRecycler.smoothScrollToPosition(messages.size() - 1);
                                
                                // Toast notification like online
                                showToastOnTop("✅ Đã xóa ngân sách tháng");
                                
                                // Refresh home fragment like online
                                refreshHomeFragment();
                            });
                        }
                    } else {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                messages.add(new ChatMessage("❌ Không tìm thấy ngân sách tháng này để xóa", false, "Bây giờ"));
                                chatAdapter.notifyItemInserted(messages.size() - 1);
                                messagesRecycler.smoothScrollToPosition(messages.size() - 1);
                            });
                        }
                    }
                } catch (Exception e) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            messages.add(new ChatMessage("❌ Lỗi khi xóa ngân sách: " + e.getMessage(), false, "Bây giờ"));
                            chatAdapter.notifyItemInserted(messages.size() - 1);
                            messagesRecycler.smoothScrollToPosition(messages.size() - 1);
                        });
                    }
                }
            });
            
            return true;
        } catch (Exception e) {
            android.util.Log.e("AiChatBottomSheet", "Error handling offline delete budget", e);
            return false;
        }
    }
    
    private boolean handleOfflineUpdateCategoryBudget(String text) {
        try {
            String lowerText = text.toLowerCase();
            
            // Extract category
            String category = null;
            for (String cat : CategoryHelper.getAllCategories()) {
                if (lowerText.contains(cat.toLowerCase())) {
                    category = cat;
                    break;
                }
            }
            
            if (category == null) {
                messages.add(new ChatMessage("❌ Không tìm thấy danh mục. Vui lòng chỉ rõ danh mục (ví dụ: 'ăn uống', 'đi lại')", false, "Bây giờ"));
                chatAdapter.notifyItemInserted(messages.size() - 1);
                messagesRecycler.smoothScrollToPosition(messages.size() - 1);
                return true;
            }
            
            // Extract amount using improved parser
            Long amount = BudgetAmountParser.parseAmount(text);
            if (amount == null) {
                messages.add(new ChatMessage("❌ Không tìm thấy số tiền. Vui lòng nhập số tiền (ví dụ: '500k', '2 triệu', '8 tỷ 6')", false, "Bây giờ"));
                chatAdapter.notifyItemInserted(messages.size() - 1);
                messagesRecycler.smoothScrollToPosition(messages.size() - 1);
                return true;
            }
            
            final String finalCategory = category;
            final long finalAmount = amount;
            
            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    // Get current month
                    Calendar cal = Calendar.getInstance();
                    cal.set(Calendar.DAY_OF_MONTH, 1);
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                    Date startDate = cal.getTime();
                    
                    cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
                    cal.set(Calendar.HOUR_OF_DAY, 23);
                    cal.set(Calendar.MINUTE, 59);
                    cal.set(Calendar.SECOND, 59);
                    Date endDate = cal.getTime();
                    
                    // Check if category budget exists
                    com.example.spending_management_app.database.CategoryBudgetEntity existingBudget = 
                            AppDatabase.getInstance(getContext())
                                    .categoryBudgetDao()
                                    .getCategoryBudgetForMonth(finalCategory, startDate, endDate);
                    
                    if (existingBudget != null) {
                        // Update existing
                        long oldAmount = existingBudget.getBudgetAmount();
                        existingBudget.budgetAmount = finalAmount;  // Direct field access
                        AppDatabase.getInstance(getContext())
                                .categoryBudgetDao()
                                .update(existingBudget);
                        
                        // Log history
                        com.example.spending_management_app.utils.BudgetHistoryLogger.logCategoryBudgetUpdated(
                                getContext(), finalCategory, oldAmount, finalAmount);
                    } else {
                        // Create new
                        com.example.spending_management_app.database.CategoryBudgetEntity newBudget = 
                                new com.example.spending_management_app.database.CategoryBudgetEntity(
                                        finalCategory, finalAmount, startDate);
                        AppDatabase.getInstance(getContext())
                                .categoryBudgetDao()
                                .insert(newBudget);
                        
                        // Log history
                        com.example.spending_management_app.utils.BudgetHistoryLogger.logCategoryBudgetCreated(
                                getContext(), finalCategory, finalAmount);
                    }
                    
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            String formattedAmount = String.format("%,d", finalAmount);
                            messages.add(new ChatMessage("✅ Đã cập nhật ngân sách danh mục (Offline)\n\n" +
                                    "📂 " + finalCategory + "\n💰 " + formattedAmount + " VND", false, "Bây giờ"));
                            chatAdapter.notifyItemInserted(messages.size() - 1);
                            messagesRecycler.smoothScrollToPosition(messages.size() - 1);
                            
                            // Toast notification like online
                            showToastOnTop("Ngân sách '" + finalCategory + "' đã được cập nhật: " + formattedAmount + " VND");
                            
                            // Refresh welcome message and home fragment like online
                            refreshCategoryBudgetWelcomeMessage();
                            refreshHomeFragment();
                        });
                    }
                } catch (Exception e) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            messages.add(new ChatMessage("❌ Lỗi khi cập nhật ngân sách danh mục: " + e.getMessage(), false, "Bây giờ"));
                            chatAdapter.notifyItemInserted(messages.size() - 1);
                            messagesRecycler.smoothScrollToPosition(messages.size() - 1);
                        });
                    }
                }
            });
            
            return true;
        } catch (Exception e) {
            android.util.Log.e("AiChatBottomSheet", "Error handling offline update category budget", e);
            return false;
        }
    }
    
    private boolean handleOfflineDeleteCategoryBudget(String text) {
        try {
            String lowerText = text.toLowerCase();
            
            // Extract category
            String category = null;
            for (String cat : CategoryHelper.getAllCategories()) {
                if (lowerText.contains(cat.toLowerCase())) {
                    category = cat;
                    break;
                }
            }
            
            if (category == null) {
                messages.add(new ChatMessage("❌ Không tìm thấy danh mục. Vui lòng chỉ rõ danh mục (ví dụ: 'ăn uống', 'đi lại')", false, "Bây giờ"));
                chatAdapter.notifyItemInserted(messages.size() - 1);
                messagesRecycler.smoothScrollToPosition(messages.size() - 1);
                return true;
            }
            
            final String finalCategory = category;
            
            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    // Get current month
                    Calendar cal = Calendar.getInstance();
                    cal.set(Calendar.DAY_OF_MONTH, 1);
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                    Date startDate = cal.getTime();
                    
                    cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
                    cal.set(Calendar.HOUR_OF_DAY, 23);
                    cal.set(Calendar.MINUTE, 59);
                    cal.set(Calendar.SECOND, 59);
                    Date endDate = cal.getTime();
                    
                    com.example.spending_management_app.database.CategoryBudgetEntity budget = 
                            AppDatabase.getInstance(getContext())
                                    .categoryBudgetDao()
                                    .getCategoryBudgetForMonth(finalCategory, startDate, endDate);
                    
                    if (budget != null) {
                        long oldAmount = budget.getBudgetAmount();
                        
                        AppDatabase.getInstance(getContext())
                                .categoryBudgetDao()
                                .delete(budget);
                        
                        // Log history
                        com.example.spending_management_app.utils.BudgetHistoryLogger.logCategoryBudgetDeleted(
                                getContext(), finalCategory, oldAmount);
                        
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                messages.add(new ChatMessage("✅ Đã xóa ngân sách danh mục '" + finalCategory + "' (Offline)", false, "Bây giờ"));
                                chatAdapter.notifyItemInserted(messages.size() - 1);
                                messagesRecycler.smoothScrollToPosition(messages.size() - 1);
                                
                                // Toast notification like online
                                showToastOnTop("Đã xóa ngân sách danh mục '" + finalCategory + "'");
                                
                                // Refresh welcome message and home fragment like online
                                refreshCategoryBudgetWelcomeMessage();
                                refreshHomeFragment();
                            });
                        }
                    } else {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                messages.add(new ChatMessage("❌ Không tìm thấy ngân sách danh mục '" + finalCategory + "' để xóa", false, "Bây giờ"));
                                chatAdapter.notifyItemInserted(messages.size() - 1);
                                messagesRecycler.smoothScrollToPosition(messages.size() - 1);
                            });
                        }
                    }
                } catch (Exception e) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            messages.add(new ChatMessage("❌ Lỗi khi xóa ngân sách danh mục: " + e.getMessage(), false, "Bây giờ"));
                            chatAdapter.notifyItemInserted(messages.size() - 1);
                            messagesRecycler.smoothScrollToPosition(messages.size() - 1);
                        });
                    }
                }
            });
            
            return true;
        } catch (Exception e) {
            android.util.Log.e("AiChatBottomSheet", "Error handling offline delete category budget", e);
            return false;
        }
    }
    
}
