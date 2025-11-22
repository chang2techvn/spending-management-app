package com.example.spending_management_app.presentation.dialog;

import static android.app.Activity.RESULT_OK;

import android.content.Intent;
import android.os.Bundle;
import java.util.Locale;

import android.speech.RecognizerIntent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.speech.tts.TextToSpeech;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.spending_management_app.R;
import com.example.spending_management_app.domain.usecase.expense.ExpenseBulkUseCase;
import com.example.spending_management_app.domain.usecase.common.WelcomeMessageUseCase;
import com.example.spending_management_app.domain.usecase.budget.BudgetUseCase;
import com.example.spending_management_app.domain.usecase.routing.RequestRouterUseCase;
import com.example.spending_management_app.domain.usecase.offline.OfflineRequestHandler;
import com.example.spending_management_app.utils.FragmentRefreshHelper;
import com.example.spending_management_app.utils.ToastHelper;

import okhttp3.OkHttpClient;
import java.util.ArrayList;
import java.util.List;


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
                WelcomeMessageUseCase.loadBudgetWelcomeMessage(getContext(), getActivity(), messages, chatAdapter, messagesRecycler, this::refreshHomeFragment);
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
                WelcomeMessageUseCase.loadExpenseBulkWelcomeMessage(getContext(), getActivity(), messages, chatAdapter, messagesRecycler, this::refreshHomeFragment, this::refreshExpenseWelcomeMessage);
            } else {
                // Load expense tracking welcome message
                WelcomeMessageUseCase.loadRecentTransactionsForWelcome(getContext(), getActivity(), messages, chatAdapter, messagesRecycler);
            }
        } else {
            // Load expense tracking welcome message (default)
            WelcomeMessageUseCase.loadRecentTransactionsForWelcome(getContext(), getActivity(), messages, chatAdapter, messagesRecycler);
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
        RequestRouterUseCase.routeRequest(text, getContext(), getActivity(), getArguments(),
                                         messages, chatAdapter, messagesRecycler, textToSpeech,
                                         this::updateNetworkStatus, new RequestRouterUseCase.RequestRouterCallback() {
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

            @Override
            public boolean handleOfflineRequest(String text, boolean isBudgetMode, boolean isCategoryBudgetMode, boolean isExpenseBulkMode) {
                return AiChatBottomSheet.this.handleOfflineRequest(text, isBudgetMode, isCategoryBudgetMode, isExpenseBulkMode);
            }

            @Override
            public void handleBudgetQuery(String text) {
                AiChatBottomSheet.this.handleBudgetQuery(text);
            }

            @Override
            public void handleExpenseBulkRequest(String text) {
                AiChatBottomSheet.this.handleExpenseBulkRequest(text);
            }

            @Override
            public boolean isNetworkAvailable() {
                return AiChatBottomSheet.this.isNetworkAvailable();
            }

            @Override
            public void updateNetworkStatus() {
                AiChatBottomSheet.this.updateNetworkStatus();
            }
        });
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
        BudgetUseCase.handleBudgetQuery(text, getContext(), getActivity(), messages, chatAdapter, messagesRecycler, textToSpeech, this::updateNetworkStatus, this::refreshHomeFragment);
    }
    

    // Method to refresh HomeFragment after successful transaction save
    // Method to refresh HomeFragment - delegates to FragmentRefreshHelper
    private void refreshHomeFragment() {
        FragmentRefreshHelper.refreshHomeFragment(getActivity());
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
        ExpenseBulkUseCase.handleExpenseBulkRequest(text, getContext(), getActivity(), messages, chatAdapter, messagesRecycler, this::refreshHomeFragment, this::refreshExpenseWelcomeMessage);
    }

}
