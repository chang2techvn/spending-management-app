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

import com.example.spending_management_app.MainActivity;
import com.example.spending_management_app.R;
import com.example.spending_management_app.utils.CategoryHelper;
import com.example.spending_management_app.utils.AiSystemInstructions;
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
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Locale;
import java.util.stream.Collectors;

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
                
                String lower = prompt.toLowerCase();
                
                // Check if this is "Add expense" request
                if (lower.contains("chi tiêu") || lower.contains("thêm chi tiêu") || lower.contains("chi tieu")) {
                    // For "Add expense", don't send to AI, just show the welcome message
                    // The welcome message with recent transactions is already loaded in setupMessages()
                    android.util.Log.d("AiChatBottomSheet", "Add expense request - showing welcome message only");
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
                String fallbackMessage = "📊 Ngân sách theo danh mục\n\n" +
                        "💡 Hướng dẫn:\n" +
                        "• Thêm: 'Thêm 500 ngàn cho danh mục ăn uống'\n" +
                        "• Sửa: 'Sửa ăn uống 700 ngàn'\n" +
                        "• Xóa: 'Xóa ngân sách danh mục ăn uống'";
                messages.add(new ChatMessage(fallbackMessage, false, "Bây giờ"));
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
                        String fallbackMessage = "Chào bạn! 👋\n\n" +
                                "� Để quản lý ngân sách, hãy cho tôi biết:\n" +
                                "Ví dụ: \"Thêm ngân sách 15 triệu\" hoặc \"Sửa ngân sách lên 20 triệu\"";
                        
                        if (!messages.isEmpty()) {
                            messages.set(0, new ChatMessage(fallbackMessage, false, "Bây giờ"));
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
                        String fallbackMessage = "Chào bạn! 👋\n\n" +
                                "💡 Để thêm chi tiêu mới, hãy cho tôi biết:\n" +
                                "Ví dụ: \"Hôm qua tôi đổ xăng 50k\" hoặc \"Ngày 10/11 mua cafe 25k\"";
                        
                        if (!messages.isEmpty()) {
                            messages.set(0, new ChatMessage(fallbackMessage, false, "Bây giờ"));
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

    private void sendToAI(String text) {
        // Check if this is budget management mode or category budget management mode
        Bundle args = getArguments();
        boolean isBudgetMode = args != null && "budget_management".equals(args.getString("mode"));
        boolean isCategoryBudgetMode = args != null && "category_budget_management".equals(args.getString("mode"));
        
        // Handle category budget management
        if (isCategoryBudgetMode) {
            handleCategoryBudgetRequest(text);
            return;
        }
        
        // Check if user is asking for budget analysis, view, or delete
        if (isBudgetMode || isBudgetQuery(text)) {
            handleBudgetQuery(text);
            return;
        }
        
        // Check if user is asking for financial analysis or reports
        if (!isBudgetMode && isFinancialQuery(text)) {
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
    
    // Check if user is querying about budget
    private boolean isBudgetQuery(String text) {
        String lowerText = text.toLowerCase();
        
        // Nếu có từ "ngân sách"
        if (!lowerText.contains("ngân sách")) {
            return false;
        }
        
        // Các trường hợp luôn là câu hỏi về ngân sách:
        // 1. Có động từ hành động hoặc câu hỏi
        boolean hasActionOrQuestion = 
                lowerText.contains("xem") || lowerText.contains("hiển thị") ||
                lowerText.contains("cho tôi biết") || lowerText.contains("thế nào") ||
                lowerText.contains("bao nhiêu") || lowerText.contains("phân tích") ||
                lowerText.contains("tư vấn") || lowerText.contains("đánh giá") ||
                lowerText.contains("so sánh") || lowerText.contains("xu hướng") ||
                lowerText.contains("xóa") || lowerText.contains("xoá") ||
                lowerText.contains("thêm") || lowerText.contains("đặt") || 
                lowerText.contains("sửa") || lowerText.contains("thay đổi") ||
                lowerText.contains("thiết lập");
        
        // 2. Có từ khóa thời gian (năm, tháng, ngày) - ngầm hiểu là xem ngân sách
        boolean hasTimeKeyword = 
                lowerText.contains("năm") || lowerText.contains("tháng") ||
                lowerText.contains("này") || lowerText.contains("trước") ||
                lowerText.contains("sau") || lowerText.contains("tất cả") ||
                lowerText.contains("toàn bộ") || lowerText.contains("hiện tại");
        
        // 3. Chỉ có "ngân sách" một mình (câu ngắn <= 15 ký tự) - có thể là xem tổng quan
        boolean isShortBudgetQuery = lowerText.trim().length() <= 15;
        
        return hasActionOrQuestion || hasTimeKeyword || isShortBudgetQuery;
    }
    
    // Handle budget queries (view, analyze, add, edit, delete)
    private void handleBudgetQuery(String text) {
        String lowerText = text.toLowerCase();
        
        // Check if user wants to delete budget
        if (lowerText.contains("xóa") || lowerText.contains("xoá")) {
            handleDeleteBudget(text);
            return;
        }
        
        // Check if user wants to add/edit budget
        if (lowerText.contains("thêm") || lowerText.contains("đặt") || 
            lowerText.contains("sửa") || lowerText.contains("thay đổi") ||
            lowerText.contains("thiết lập")) {
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
        
        // Extract amount from text (support various formats like "15 triệu", "20000000", "25tr")
        long amount = extractBudgetAmount(text);
        
        // Extract month and year from text (default to current month if not specified)
        int[] monthYear = extractMonthYear(text);
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
                    
                    android.util.Log.d("AiChatBottomSheet", "Budget date to save: " + budgetDate + ", Amount: " + amount);
                    
                    if (isUpdate) {
                        // Update existing budget
                        BudgetEntity existing = existingBudgets.get(0);
                        android.util.Log.d("AiChatBottomSheet", "Updating existing budget, old date: " + existing.date + ", new date: " + budgetDate);
                        long oldAmount = existing.monthlyLimit;
                        existing.monthlyLimit = amount;
                        existing.date = budgetDate;
                        AppDatabase.getInstance(getContext()).budgetDao().update(existing);
                        
                        // Log budget history
                        com.example.spending_management_app.utils.BudgetHistoryLogger.logMonthlyBudgetUpdated(
                                getContext(), oldAmount, amount, budgetDate);
                    } else {
                        // Insert new budget
                        BudgetEntity budget = new BudgetEntity("Ngân sách tháng", amount, 0L, budgetDate);
                        android.util.Log.d("AiChatBottomSheet", "Inserting new budget: " + budget.date);
                        AppDatabase.getInstance(getContext()).budgetDao().insert(budget);
                        
                        // Log budget history
                        com.example.spending_management_app.utils.BudgetHistoryLogger.logMonthlyBudgetCreated(
                                getContext(), amount, budgetDate);
                    }
                    
                    String formattedAmount = String.format("%,d", amount);
                    SimpleDateFormat monthYearFormat = new SimpleDateFormat("MM/yyyy", new Locale("vi", "VN"));
                    String monthYearStr = monthYearFormat.format(budgetDate);
                    
                    // Update UI
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            String responseMessage = isUpdate ? 
                                    "✅ Đã cập nhật ngân sách tháng " + monthYearStr + " thành " + formattedAmount + " VND!\n\n" +
                                    "Chúc bạn quản lý tài chính tốt! 💪" :
                                    "✅ Đã thiết lập ngân sách tháng " + monthYearStr + " là " + formattedAmount + " VND!\n\n" +
                                    "Chúc bạn chi tiêu hợp lý! 💰";
                            
                            messages.set(analyzingIndex, new ChatMessage(responseMessage, false, "Bây giờ"));
                            chatAdapter.notifyItemChanged(analyzingIndex);
                            messagesRecycler.smoothScrollToPosition(messages.size() - 1);
                            
                            // Show toast
                            String toastMessage = isUpdate ? 
                                    "✅ Đã cập nhật ngân sách tháng " + monthYearStr + ": " + formattedAmount + " VND" :
                                    "✅ Đã thiết lập ngân sách tháng " + monthYearStr + ": " + formattedAmount + " VND";
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
                        "Vui lòng nhập rõ số tiền và tháng (nếu cần), ví dụ:\n" +
                        "   • \"Thêm ngân sách tháng 12 là 15 triệu\"\n" +
                        "   • \"Đặt ngân sách 20 triệu cho tháng 1/2026\"\n" +
                        "   • \"Sửa ngân sách tháng này lên 25tr\"",
                        false, "Bây giờ"));
                chatAdapter.notifyItemChanged(analyzingIndex);
            });
        }
    }
    
    private int[] extractMonthYear(String text) {
        try {
            text = text.toLowerCase().trim();
            
            Calendar currentCal = Calendar.getInstance();
            int currentMonth = currentCal.get(Calendar.MONTH) + 1; // 0-based to 1-based
            int currentYear = currentCal.get(Calendar.YEAR);
            
            // Pattern 1: "tháng X" or "tháng X/YYYY"
            Pattern monthPattern = Pattern.compile("tháng\\s+(\\d{1,2})(?:/(\\d{4}))?");
            Matcher monthMatcher = monthPattern.matcher(text);
            if (monthMatcher.find()) {
                int month = Integer.parseInt(monthMatcher.group(1));
                int year = monthMatcher.group(2) != null ? 
                          Integer.parseInt(monthMatcher.group(2)) : currentYear;
                
                // If month is valid (1-12)
                if (month >= 1 && month <= 12) {
                    return new int[]{month, year};
                }
            }
            
            // Pattern 2: "X/YYYY" or "XX/YYYY"
            Pattern datePattern = Pattern.compile("(\\d{1,2})/(\\d{4})");
            Matcher dateMatcher = datePattern.matcher(text);
            if (dateMatcher.find()) {
                int month = Integer.parseInt(dateMatcher.group(1));
                int year = Integer.parseInt(dateMatcher.group(2));
                
                if (month >= 1 && month <= 12) {
                    return new int[]{month, year};
                }
            }
            
            // Pattern 3: "tháng này" - current month
            if (text.contains("tháng này") || text.contains("thang nay")) {
                return new int[]{currentMonth, currentYear};
            }
            
            // Pattern 4: "tháng sau" or "tháng tới" - next month
            if (text.contains("tháng sau") || text.contains("tháng tới") || 
                text.contains("thang sau") || text.contains("thang toi")) {
                currentCal.add(Calendar.MONTH, 1);
                return new int[]{currentCal.get(Calendar.MONTH) + 1, currentCal.get(Calendar.YEAR)};
            }
            
            // Default: current month
            return new int[]{currentMonth, currentYear};
            
        } catch (Exception e) {
            android.util.Log.e("AiChatBottomSheet", "Error extracting month/year", e);
            Calendar currentCal = Calendar.getInstance();
            return new int[]{currentCal.get(Calendar.MONTH) + 1, currentCal.get(Calendar.YEAR)};
        }
    }
    
    private long extractBudgetAmount(String text) {
        try {
            text = text.toLowerCase().trim();
            
            // Pattern 1: "X triệu" or "X tr"
            Pattern trPattern = Pattern.compile("(\\d+(?:[,.]\\d+)?)\\s*(?:triệu|tr)");
            Matcher trMatcher = trPattern.matcher(text);
            if (trMatcher.find()) {
                String numberStr = trMatcher.group(1).replace(",", ".").replace(".", "");
                double millions = Double.parseDouble(numberStr);
                return (long)(millions * 1000000);
            }
            
            // Pattern 2: "X nghìn" or "X k"
            Pattern kPattern = Pattern.compile("(\\d+(?:[,.]\\d+)?)\\s*(?:nghìn|k|ng)");
            Matcher kMatcher = kPattern.matcher(text);
            if (kMatcher.find()) {
                String numberStr = kMatcher.group(1).replace(",", ".").replace(".", "");
                double thousands = Double.parseDouble(numberStr);
                return (long)(thousands * 1000);
            }
            
            // Pattern 3: Plain number (should be large enough to be a budget)
            Pattern numberPattern = Pattern.compile("(\\d{5,})"); // At least 5 digits
            Matcher numberMatcher = numberPattern.matcher(text);
            if (numberMatcher.find()) {
                return Long.parseLong(numberMatcher.group(1));
            }
            
            return 0;
            
        } catch (Exception e) {
            android.util.Log.e("AiChatBottomSheet", "Error extracting budget amount", e);
            return 0;
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
                                
                                // Format markdown text để dễ đọc hơn
                                String formattedDisplayText = formatMarkdownText(displayText);
                                
                                messages.set(analyzingIndex, new ChatMessage(formattedDisplayText, false, "Bây giờ"));
                                chatAdapter.notifyItemChanged(analyzingIndex);
                                Log.d("AiChatBottomSheet", "AI response: " + formattedDisplayText);

                                messagesRecycler.smoothScrollToPosition(messages.size() - 1);
                                textToSpeech.speak(formattedDisplayText, TextToSpeech.QUEUE_FLUSH, null, null);

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
                    androidx.fragment.app.Fragment currentFragment = navHostFragment.getChildFragmentManager().getPrimaryNavigationFragment();
                    if (currentFragment instanceof com.example.spending_management_app.ui.home.HomeFragment) {
                        com.example.spending_management_app.ui.home.HomeFragment homeFragment = 
                            (com.example.spending_management_app.ui.home.HomeFragment) currentFragment;
                        homeFragment.refreshRecentTransactions();
                        android.util.Log.d("AiChatBottomSheet", "HomeFragment refreshed after transaction save");
                    } else {
                        android.util.Log.d("AiChatBottomSheet", "Current fragment is not HomeFragment: " + 
                            (currentFragment != null ? currentFragment.getClass().getSimpleName() : "null"));
                    }
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
                            Date budgetDate = new Date();
                            if (existingBudgets != null && !existingBudgets.isEmpty()) {
                                // Update existing
                                BudgetEntity existing = existingBudgets.get(0);
                                long oldAmount = existing.monthlyLimit;
                                existing.setMonthlyLimit(amt);
                                existing.setDate(budgetDate);
                                AppDatabase.getInstance(getContext()).budgetDao().update(existing);
                                
                                // Log budget history
                                com.example.spending_management_app.utils.BudgetHistoryLogger.logMonthlyBudgetUpdated(
                                        getContext(), oldAmount, amt, budgetDate);
                            } else {
                                // Insert new
                                BudgetEntity budget = new BudgetEntity("Ngân sách tháng", amt, 0L, budgetDate);
                                AppDatabase.getInstance(getContext()).budgetDao().insert(budget);
                                
                                // Log budget history
                                com.example.spending_management_app.utils.BudgetHistoryLogger.logMonthlyBudgetCreated(
                                        getContext(), amt, budgetDate);
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

    // Check if user is asking for financial analysis
    private boolean isFinancialQuery(String text) {
        String lowerText = text.toLowerCase();
        return lowerText.contains("chi tiêu") && (
                lowerText.contains("hôm nay") || lowerText.contains("hôm qua") || 
                lowerText.contains("tuần") || lowerText.contains("tháng") ||
                lowerText.contains("tổng") || lowerText.contains("bao nhiêu") ||
                lowerText.contains("phân tích") || lowerText.contains("báo cáo") ||
                lowerText.contains("danh mục") || lowerText.contains("thống kê") ||
                lowerText.contains("ngày") && (lowerText.contains("/") || lowerText.matches(".*\\d+.*")) ||
                lowerText.contains("so với") || lowerText.contains("tư vấn")
        );
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
                            String formattedText = formatMarkdownText(aiText);

                            getActivity().runOnUiThread(() -> {
                                messages.set(analyzingIndex, new ChatMessage(formattedText, false, "Bây giờ"));
                                chatAdapter.notifyItemChanged(analyzingIndex);
                                messagesRecycler.smoothScrollToPosition(messages.size() - 1);
                                textToSpeech.speak(formattedText, TextToSpeech.QUEUE_FLUSH, null, null);
                            });
                        } catch (Exception e) {
                            getActivity().runOnUiThread(() -> {
                                messages.set(analyzingIndex, new ChatMessage("Lỗi xử lý phản hồi AI.", false, "Bây giờ"));
                                chatAdapter.notifyItemChanged(analyzingIndex);
                            });
                        }
                    } else {
                        getActivity().runOnUiThread(() -> {
                            messages.set(analyzingIndex, new ChatMessage("Lỗi từ AI: " + response.code(), false, "Bây giờ"));
                            chatAdapter.notifyItemChanged(analyzingIndex);
                        });
                    }
                }
            });
        } catch (Exception e) {
            messages.set(analyzingIndex, new ChatMessage("Lỗi gửi tin nhắn.", false, "Bây giờ"));
            chatAdapter.notifyItemChanged(analyzingIndex);
        }
    }

    // Helper method để format markdown text thành plain text dễ đọc
    private String formatMarkdownText(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        try {
            // Xóa bold markdown (**text**)
            text = text.replaceAll("\\*\\*(.*?)\\*\\*", "$1");
            
            // Xóa italic markdown (*text*)
            text = text.replaceAll("(?<!\\*)\\*(?!\\*)([^*]+)\\*(?!\\*)", "$1");
            
            // Xóa heading markdown (###, ##, #)
            text = text.replaceAll("^#{1,6}\\s+", "");
            text = text.replaceAll("\\n#{1,6}\\s+", "\n");
            
            // Giữ nguyên xuống dòng - KHÔNG xóa
            // Chỉ chuẩn hóa: tối đa 2 xuống dòng liên tiếp
            text = text.replaceAll("\\n{3,}", "\n\n");
            
            // Xóa các asterisk đơn lẻ còn sót lại
            text = text.replaceAll("(?<!\\S)\\*(?!\\S)", "");
            
            // Trim whitespace đầu cuối
            text = text.trim();
            
            android.util.Log.d("AiChatBottomSheet", "Formatted text: " + text);
            
            return text;
            
        } catch (Exception e) {
            android.util.Log.e("AiChatBottomSheet", "Error formatting markdown", e);
            return text; // Return original if error
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
            
        } catch (Exception e) {
            context.append("Lỗi khi truy xuất dữ liệu ngân sách: ").append(e.getMessage());
            android.util.Log.e("AiChatBottomSheet", "Error getting budget context", e);
        }
        
        return context.toString();
    }
    
    // Send prompt to AI with budget context
    private void sendPromptToAIWithBudgetContext(String userQuery, String budgetContext) {
        // Add temporary "Đang phân tích..." message
        int analyzingIndex = messages.size();
        messages.add(new ChatMessage("Đang phân tích ngân sách...", false, "Bây giờ"));
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
                    getActivity().runOnUiThread(() -> {
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

                            String formattedText = formatMarkdownText(aiText);

                            getActivity().runOnUiThread(() -> {
                                messages.set(analyzingIndex, new ChatMessage(formattedText, false, "Bây giờ"));
                                chatAdapter.notifyItemChanged(analyzingIndex);
                                messagesRecycler.smoothScrollToPosition(messages.size() - 1);
                                textToSpeech.speak(formattedText, TextToSpeech.QUEUE_FLUSH, null, null);
                            });
                        } catch (Exception e) {
                            getActivity().runOnUiThread(() -> {
                                messages.set(analyzingIndex, new ChatMessage("Lỗi xử lý phản hồi AI.", false, "Bây giờ"));
                                chatAdapter.notifyItemChanged(analyzingIndex);
                            });
                        }
                    } else {
                        getActivity().runOnUiThread(() -> {
                            messages.set(analyzingIndex, new ChatMessage("Lỗi từ AI: " + response.code(), false, "Bây giờ"));
                            chatAdapter.notifyItemChanged(analyzingIndex);
                        });
                    }
                }
            });
        } catch (Exception e) {
            messages.set(analyzingIndex, new ChatMessage("Lỗi gửi tin nhắn.", false, "Bây giờ"));
            chatAdapter.notifyItemChanged(analyzingIndex);
        }
    }
    
    // Handle delete budget request
    private void handleDeleteBudget(String text) {
        // Add analyzing message
        int analyzingIndex = messages.size();
        messages.add(new ChatMessage("Đang xử lý yêu cầu xóa...", false, "Bây giờ"));
        chatAdapter.notifyItemInserted(messages.size() - 1);
        messagesRecycler.smoothScrollToPosition(messages.size() - 1);
        
        // Extract month and year from text
        int[] monthYear = extractMonthYear(text);
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
                        "• Thêm: 'Thêm 500 ngàn ăn uống và 300 ngàn di chuyển'\n" +
                        "• Sửa: 'Sửa ăn uống 700 ngàn, mua sắm 400 ngàn'\n" +
                        "• Xóa: 'Xóa ngân sách ăn uống và di chuyển'",
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
                    amount = extractBudgetAmount(segment);
                    
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
        
        return extractBudgetAmount(searchArea);
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
                                
                                String icon = getIconEmoji(op.category);
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
                                    String icon = getIconEmoji(op.category);
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
                            
                            String icon = getIconEmoji(op.category);
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
                    String icon = getIconEmoji(info.category);
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
    

    private String getIconEmoji(String category) {
        switch (category) {
            // Nhu cầu thiết yếu
            case "Ăn uống":
                return "🍽️";
            case "Di chuyển":
                return "🚗";
            case "Tiện ích":
                return "⚡";
            case "Y tế":
                return "🏥";
            case "Nhà ở":
                return "🏠";
            
            // Mua sắm & Phát triển bản thân
            case "Mua sắm":
                return "🛍️";
            case "Giáo dục":
                return "📚";
            case "Sách & Học tập":
                return "📖";
            case "Thể thao":
                return "⚽";
            case "Sức khỏe & Làm đẹp":
                return "💆";
            
            // Giải trí & Xã hội
            case "Giải trí":
                return "🎬";
            case "Du lịch":
                return "✈️";
            case "Ăn ngoài & Cafe":
                return "☕";
            case "Quà tặng & Từ thiện":
                return "🎁";
            case "Hội họp & Tiệc tụng":
                return "🎉";
            
            // Công nghệ & Dịch vụ
            case "Điện thoại & Internet":
                return "📱";
            case "Đăng ký & Dịch vụ":
                return "💳";
            case "Phần mềm & Apps":
                return "💻";
            case "Ngân hàng & Phí":
                return "🏦";
            
            // Gia đình & Con cái
            case "Con cái":
                return "👶";
            case "Thú cưng":
                return "🐕";
            case "Gia đình":
                return "👨‍👩‍👧‍👦";
            
            // Thu nhập & Tài chính
            case "Lương":
                return "💰";
            case "Đầu tư":
                return "📈";
            case "Thu nhập phụ":
                return "💵";
            case "Tiết kiệm":
                return "🏦";
            
            // Khác
            case "Khác":
                return "📌";
            default:
                return "💳";
        }
    }
}
