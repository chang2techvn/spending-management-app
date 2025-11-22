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

import com.example.spending_management_app.R;
import com.example.spending_management_app.service.GeminiAI;
import com.example.spending_management_app.service.AiContextService;
import com.example.spending_management_app.service.ExpenseBulkService;
import com.example.spending_management_app.service.WelcomeMessageService;
import com.example.spending_management_app.utils.FragmentRefreshHelper;
import com.example.spending_management_app.utils.TextFormatHelper;
import com.example.spending_management_app.utils.DateParser;
import com.example.spending_management_app.utils.ExtractorHelper;
import com.example.spending_management_app.utils.BudgetAmountParser;
import com.example.spending_management_app.utils.BudgetMessageHelper;
import com.example.spending_management_app.utils.CategoryHelper;
import com.example.spending_management_app.utils.ExpenseMessageHelper;
import com.example.spending_management_app.utils.CategoryIconHelper;
import com.example.spending_management_app.utils.AiSystemInstructions;
import com.example.spending_management_app.utils.ToastHelper;

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
        
        // Initialize chatAdapter first before calling service methods
        chatAdapter = new ChatAdapter(messages);
        messagesRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        messagesRecycler.setAdapter(chatAdapter);
        
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
                WelcomeMessageService.loadBudgetWelcomeMessage(getContext(), getActivity(), messages, chatAdapter, messagesRecycler, this::refreshHomeFragment);
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
                WelcomeMessageService.loadExpenseBulkWelcomeMessage(getContext(), getActivity(), messages, chatAdapter, messagesRecycler, this::refreshHomeFragment, this::refreshExpenseWelcomeMessage);
            } else {
                // Load expense tracking welcome message
                WelcomeMessageService.loadRecentTransactionsForWelcome(getContext(), getActivity(), messages, chatAdapter, messagesRecycler);
            }
        } else {
            // Load expense tracking welcome message (default)
            WelcomeMessageService.loadRecentTransactionsForWelcome(getContext(), getActivity(), messages, chatAdapter, messagesRecycler);
        }
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
            ToastHelper.showTopToast(getActivity(), "Thiết bị không hỗ trợ nhận diện giọng nói", Toast.LENGTH_SHORT);
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
                    String financialContext = AiContextService.getFinancialContext(getContext());
                    getActivity().runOnUiThread(() -> {
                        AiContextService.sendPromptToAIWithContext(text, financialContext, getActivity(), messages, chatAdapter, messagesRecycler, textToSpeech, this::updateNetworkStatus);
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
    
    // Handle offline requests using OfflineRequestHandler
    private boolean handleOfflineRequest(String text, boolean isBudgetMode, boolean isCategoryBudgetMode, boolean isExpenseBulkMode) {
        OfflineRequestHandler handler = new OfflineRequestHandler(getContext(), new OfflineRequestHandler.OfflineRequestCallback() {
            @Override
            public void onSuccess(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        messages.add(new ChatMessage(message, false, "Bây giờ"));
                        chatAdapter.notifyItemInserted(messages.size() - 1);
                        messagesRecycler.smoothScrollToPosition(messages.size() - 1);
                    });
                }
            }
            
            @Override
            public void onError(String errorMessage) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        messages.add(new ChatMessage(errorMessage, false, "Bây giờ"));
                        chatAdapter.notifyItemInserted(messages.size() - 1);
                        messagesRecycler.smoothScrollToPosition(messages.size() - 1);
                    });
                }
            }
            
            @Override
            public void onToast(String toastMessage, boolean isError) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (isError) {
                            ToastHelper.showErrorToast(getActivity(), toastMessage);
                        } else {
                            ToastHelper.showToastOnTop(getActivity(), toastMessage);
                        }
                    });
                }
            }
            
            @Override
            public void refreshHomeFragment() {
                AiChatBottomSheet.this.refreshHomeFragment();
            }
            
            @Override
            public void refreshExpenseWelcomeMessage() {
                AiChatBottomSheet.this.refreshExpenseWelcomeMessage();
            }
            
            @Override
            public void refreshCategoryBudgetWelcomeMessage() {
                AiChatBottomSheet.this.refreshCategoryBudgetWelcomeMessage();
            }
        });
        
        return handler.handleOfflineRequest(text, isBudgetMode, isCategoryBudgetMode, isExpenseBulkMode);
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
                String budgetContext = AiContextService.getBudgetContext(getContext());
                
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
                    AiContextService.sendPromptToAIWithBudgetContext(finalQuery, budgetContext, messages, chatAdapter, messagesRecycler, textToSpeech, this::updateNetworkStatus);
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
                            
                            ToastHelper.showToastOnTop(getActivity(), toastMessage);
                            
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
                            ToastHelper.showErrorToast(getActivity(), "Lỗi lưu ngân sách");
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
                            ToastHelper.showToastOnTop(requireActivity(), toastMessage);
                            
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
                            ToastHelper.showErrorToast(getActivity(), errorMessage);
                            android.util.Log.e("AiChatBottomSheet", "Error saving expense", e);
                        });
                    }
                }).start();
            }

        } catch (Exception e) {
            e.printStackTrace();
            String errorMessage = "❌ Có lỗi xảy ra khi xử lý dữ liệu: " + e.getMessage();
            ToastHelper.showErrorToast(getActivity(), errorMessage);
            android.util.Log.e("AiChatBottomSheet", "Error processing data", e);
        }
    }


    // Method to refresh HomeFragment after successful transaction save
    // Method to refresh HomeFragment - delegates to FragmentRefreshHelper
    private void refreshHomeFragment() {
        FragmentRefreshHelper.refreshHomeFragment(getActivity());
    }
    
    // Method to refresh HistoryFragment - delegates to FragmentRefreshHelper
    private void refreshHistoryFragment() {
        FragmentRefreshHelper.refreshHistoryFragment(getActivity());
    }

    // Method to refresh expense welcome message - delegates to FragmentRefreshHelper
    private void refreshExpenseWelcomeMessage() {
        FragmentRefreshHelper.refreshExpenseWelcomeMessage(getActivity(), 
            new FragmentRefreshHelper.FragmentRefreshCallback() {
                @Override
                public void onWelcomeMessageUpdated(String message) {
                    if (!messages.isEmpty()) {
                        messages.set(0, new ChatMessage(message, false, "Bây giờ"));
                        chatAdapter.notifyItemChanged(0);
                    }
                }
                
                @Override
                public android.app.Activity getActivity() {
                    return AiChatBottomSheet.this.getActivity();
                }
            });
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
                            
                            ToastHelper.showToastOnTop(getActivity(), "✅ Đã xóa ngân sách tháng " + monthYearStr);
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
                        ToastHelper.showErrorToast(getActivity(), "Lỗi xóa ngân sách");
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
                                    ToastHelper.showToastOnTop(getActivity(), "✅ Đã xóa tất cả ngân sách danh mục");
                                    refreshHomeFragment();
                                    refreshCategoryBudgetWelcomeMessage();
                                } else {
                                    ToastHelper.showErrorToast(getActivity(), "⚠️ Không có ngân sách nào để xóa");
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
                                ToastHelper.showErrorToast(getActivity(), "Lỗi xóa ngân sách");
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
                                ToastHelper.showErrorToast(getActivity(), "⚠️ " + counts[0] + " thành công, " + counts[1] + " thất bại");
                            } else {
                                // All failed
                                ToastHelper.showErrorToast(getActivity(), "❌ Thất bại: " + counts[1] + " danh mục");
                            }
                        } else {
                            // All success - show success toast in green
                            ToastHelper.showToastOnTop(getActivity(), "✅ Cập nhật " + counts[0] + " danh mục");
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
    
    // Method to refresh category budget welcome message - delegates to FragmentRefreshHelper
    private void refreshCategoryBudgetWelcomeMessage() {
        FragmentRefreshHelper.refreshCategoryBudgetWelcomeMessage(getActivity(),
            new FragmentRefreshHelper.FragmentRefreshCallback() {
                @Override
                public void onWelcomeMessageUpdated(String message) {
                    // Update first message (welcome message)
                    if (!messages.isEmpty() && messages.get(0).message.contains("📊 Ngân sách theo danh mục")) {
                        messages.set(0, new ChatMessage(message, false, "Bây giờ"));
                        chatAdapter.notifyItemChanged(0);
                    }
                }
                
                @Override
                public android.app.Activity getActivity() {
                    return AiChatBottomSheet.this.getActivity();
                }
            });
    }
    
    
    // ==================== EXPENSE BULK MANAGEMENT ====================
    
    private void handleExpenseBulkRequest(String text) {
        ExpenseBulkService.handleExpenseBulkRequest(text, getContext(), getActivity(), messages, chatAdapter, messagesRecycler, this::refreshHomeFragment, this::refreshExpenseWelcomeMessage);
    }

}
