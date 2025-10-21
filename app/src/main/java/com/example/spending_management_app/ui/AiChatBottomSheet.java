package com.example.spending_management_app.ui;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import java.util.Locale;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.WindowManager;
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
import java.util.ArrayList;
import java.util.List;

public class AiChatBottomSheet extends DialogFragment {

    @Override
    public int getTheme() {
        return R.style.RoundedDialog;
    }

    private RecyclerView messagesRecycler;
    private EditText messageInput;
    private ImageButton sendButton;
    private ImageButton closeButton;

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
                sendToAI(message);
                messageInput.setText("");
            }
        });

        closeButton.setOnClickListener(v -> dismiss());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (textToSpeech != null) {
            textToSpeech.shutdown();
        }
    }

    private void sendToAI(String text) {
        messages.add(new ChatMessage(text, true, "Bây giờ"));
        chatAdapter.notifyItemInserted(messages.size() - 1);
        messagesRecycler.smoothScrollToPosition(messages.size() - 1);

        // Add temporary "Đang phân tích..." message
        int analyzingIndex = messages.size();
        messages.add(new ChatMessage("Đang phân tích...", false, "Bây giờ"));
        chatAdapter.notifyItemInserted(messages.size() - 1);
        messagesRecycler.smoothScrollToPosition(messages.size() - 1);

        // Prepare JSON for Gemini API with system instruction
        try {
            JSONObject json = new JSONObject();

            // System instruction
            JSONObject systemInstruction = new JSONObject();
            JSONArray systemParts = new JSONArray();
            JSONObject systemPart = new JSONObject();
            systemPart.put("text", "Bạn là trợ lý ghi chi tiêu. Khi user muốn thêm thu nhập hoặc chi tiêu, hãy trả về JSON với cấu trúc: {\"type\": \"expense\" hoặc \"income\", \"name\": \"tên giao dịch\", \"amount\": số tiền, \"currency\": \"VND\" hoặc \"USD\" v.v., \"category\": \"Ăn uống\" v.v.} và một câu trả lời tự nhiên hài hước để yêu cầu xác nhận, không hiển thị JSON. Ví dụ: {\"type\":\"expense\",\"name\":\"Ăn sáng\",\"amount\":50000,\"currency\":\"VND\",\"category\":\"Ăn uống\"} Bạn đã thêm chi tiêu 50k VND cho việc ăn sáng, ngon miệng nhé! Nếu không phải thêm giao dịch, trả lời bình thường.");
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
                    .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=AIzaSyB7cKKNvETdnd379olrAJpXzEfmfIGyx-M")
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

                            getActivity().runOnUiThread(() -> {
                                // Replace analyzing message with display text
                                messages.set(analyzingIndex, new ChatMessage(displayText, false, "Bây giờ"));
                                chatAdapter.notifyItemChanged(analyzingIndex);
                                messagesRecycler.smoothScrollToPosition(messages.size() - 1);
                                textToSpeech.speak(displayText, TextToSpeech.QUEUE_FLUSH, null, null);

                                // If JSON found, show confirmation dialog
                                if (jsonPart != null) {
                                    showExpenseConfirmationDialog(jsonPart);
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
        // Find JSON object in text
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start != -1 && end != -1 && end > start) {
            return text.substring(start, end + 1);
        }
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

    private void showExpenseConfirmationDialog(String jsonString) {
        try {
            JSONObject json = new JSONObject(jsonString);

            // Create dialog
            Dialog dialog = new Dialog(getContext());
            dialog.setContentView(R.layout.dialog_expense_confirmation);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

            // Get views
            android.widget.Spinner spinnerType = dialog.findViewById(R.id.spinner_type);
            android.widget.TextView badgeType = dialog.findViewById(R.id.badge_type);
            android.widget.EditText editName = dialog.findViewById(R.id.edit_name);
            android.widget.EditText editAmount = dialog.findViewById(R.id.edit_amount);
            android.widget.Spinner spinnerCurrency = dialog.findViewById(R.id.spinner_currency);
            android.widget.Spinner spinnerCategory = dialog.findViewById(R.id.spinner_category);
            android.widget.Button btnCancel = dialog.findViewById(R.id.btn_cancel);
            android.widget.Button btnConfirm = dialog.findViewById(R.id.btn_confirm);

            // Fill data from JSON
            String type = json.optString("type", "expense");
            if ("income".equals(type)) {
                spinnerType.setSelection(1); // Thu nhập
                badgeType.setText("Thu nhập");
                badgeType.setBackgroundResource(R.color.income_color); // Green for income
            } else {
                spinnerType.setSelection(0); // Chi tiêu
                badgeType.setText("Chi tiêu");
                badgeType.setBackgroundResource(R.color.expense_color); // Red for expense
            }

            // Listener to change badge color
            spinnerType.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    if (position == 0) { // Chi tiêu
                        badgeType.setText("Chi tiêu");
                        badgeType.setBackgroundResource(R.color.expense_color);
                    } else { // Thu nhập
                        badgeType.setText("Thu nhập");
                        badgeType.setBackgroundResource(R.color.income_color);
                    }
                }

                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            });

            editName.setText(json.optString("name", ""));
            editAmount.setText(String.valueOf(json.optDouble("amount", 0)));
            String currency = json.optString("currency", "VND");
            String[] currencies = getResources().getStringArray(R.array.currencies);
            for (int i = 0; i < currencies.length; i++) {
                if (currencies[i].equals(currency)) {
                    spinnerCurrency.setSelection(i);
                    break;
                }
            }
            String category = json.optString("category", "");
            String[] categories = getResources().getStringArray(R.array.categories);
            for (int i = 0; i < categories.length; i++) {
                if (categories[i].equals(category)) {
                    spinnerCategory.setSelection(i);
                    break;
                }
            }

            btnCancel.setOnClickListener(v -> dialog.dismiss());
            btnConfirm.setOnClickListener(v -> {
                // TODO: Save to database
                Toast.makeText(getContext(), "Đã xác nhận giao dịch", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });

            dialog.show();

        } catch (Exception e) {
            Log.e("AiChatBottomSheet", "Error parsing JSON: " + jsonString, e);
            Toast.makeText(getContext(), "Lỗi xử lý dữ liệu", Toast.LENGTH_SHORT).show();
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
            } else {
                // AI message - align left
                messageBubble.setBackgroundResource(R.drawable.ai_message_background);
                ((android.widget.LinearLayout.LayoutParams) messageBubble.getLayoutParams()).gravity = android.view.Gravity.START;
            }
        }
    }
}
