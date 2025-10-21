package com.example.spending_management_app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.spending_management_app.databinding.ActivityMainBinding;
import com.example.spending_management_app.ui.AiChatBottomSheet;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private NavController navController;
    private LinearLayout navHome, navStatistics, navHistory, navAccount;
    private View navAiAssistant;
    private View homeHeader;
    private SpeechRecognizer speechRecognizer;
    private Intent speechIntent;
    private ImageView aiIcon;
    private boolean isRecording = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        homeHeader = findViewById(R.id.home_header);

        navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
    // We don't want NavigationUI to automatically set the ActionBar title
    // because the app uses a custom activity-level header for Home.
    // So we skip setupActionBarWithNavController and manage our header manually.

        // Setup custom bottom navigation
        setupBottomNavigation();

        // Init speech recognizer
        initSpeechRecognizer();

        // Listen to destination changes to update UI
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            updateNavigationUI(destination.getId());
            // Sau: Header chỉ hiển thị trên tab Home
            if (destination.getId() == R.id.navigation_home) {
                homeHeader.setVisibility(View.VISIBLE);
            } else {
                homeHeader.setVisibility(View.GONE);
            }
        });
    }

    private void setupBottomNavigation() {
        navHome = findViewById(R.id.nav_home);
        navStatistics = findViewById(R.id.nav_statistics);
        navHistory = findViewById(R.id.nav_history);
        navAccount = findViewById(R.id.nav_account);
        navAiAssistant = findViewById(R.id.nav_ai_assistant);
        aiIcon = navAiAssistant.findViewById(R.id.ai_icon); // Assuming the ImageView has id ai_icon

        navHome.setOnClickListener(v -> navController.navigate(R.id.navigation_home));
        navStatistics.setOnClickListener(v -> navController.navigate(R.id.navigation_statistics));
        navHistory.setOnClickListener(v -> navController.navigate(R.id.navigation_history));
        navAccount.setOnClickListener(v -> navController.navigate(R.id.navigation_account));

        navAiAssistant.setOnClickListener(v -> {
            // Mở dialog chat AI
            AiChatBottomSheet aiChatBottomSheet = new AiChatBottomSheet();
            aiChatBottomSheet.show(getSupportFragmentManager(), aiChatBottomSheet.getTag());
        });

        navAiAssistant.setOnLongClickListener(v -> {
            // Kích hoạt voice chat
            if (speechRecognizer == null) {
                Toast.makeText(MainActivity.this, "Thiết bị không hỗ trợ nhận diện giọng nói", Toast.LENGTH_SHORT).show();
                return true;
            }
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
                return true;
            }
            startVoiceRecognition();
            return true; // Prevent onClick
        });
    }

    private void updateNavigationUI(int destinationId) {
        // Reset all to gray
        setNavItemColor(navHome, false);
        setNavItemColor(navStatistics, false);
        setNavItemColor(navHistory, false);
        setNavItemColor(navAccount, false);

        // Set selected to blue
        if (destinationId == R.id.navigation_home) {
            setNavItemColor(navHome, true);
        } else if (destinationId == R.id.navigation_statistics) {
            setNavItemColor(navStatistics, true);
        } else if (destinationId == R.id.navigation_history) {
            setNavItemColor(navHistory, true);
        } else if (destinationId == R.id.navigation_account) {
            setNavItemColor(navAccount, true);
        }
    }

    private void setNavItemColor(LinearLayout navItem, boolean selected) {
        ImageView icon = (ImageView) navItem.getChildAt(0);
        TextView text = (TextView) navItem.getChildAt(1);
        int color = selected ? getResources().getColor(R.color.blue_600) : getResources().getColor(R.color.nav_item_color);
        icon.setColorFilter(color);
        text.setTextColor(color);
    }

    private void initSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi_VN"); // Set to Vietnamese
        speechIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Nói gì đó...");            speechRecognizer.setRecognitionListener(new android.speech.RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    isRecording = true;
                    Toast.makeText(MainActivity.this, "Đang nghe...", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onBeginningOfSpeech() {}

                @Override
                public void onRmsChanged(float rmsdB) {}

                @Override
                public void onBufferReceived(byte[] buffer) {}

                @Override
                public void onEndOfSpeech() {
                    isRecording = false;
                    Toast.makeText(MainActivity.this, "Đã dừng nghe", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(int error) {
                    isRecording = false;
                    String errorMsg = "Lỗi nhận diện: " + error;
                    if (error == SpeechRecognizer.ERROR_NO_MATCH) {
                        errorMsg = "Không nghe rõ, thử lại";
                    } else if (error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                        errorMsg = "Hết thời gian, thử lại";
                    }
                    Toast.makeText(MainActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        String spokenText = matches.get(0);
                        android.util.Log.d("SpeechRecognition", "Recognized text: " + spokenText);
                        Toast.makeText(MainActivity.this, "Bạn nói: " + spokenText, Toast.LENGTH_SHORT).show();
                        // Gửi đến AI
                        processVoiceInput(spokenText);
                    } else {
                        Toast.makeText(MainActivity.this, "Không nhận diện được giọng nói", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onPartialResults(Bundle partialResults) {}

                @Override
                public void onEvent(int eventType, Bundle params) {}
            });
        } else {
            Toast.makeText(this, "Thiết bị không hỗ trợ nhận diện giọng nói", Toast.LENGTH_LONG).show();
        }
    }

    private void processVoiceInput(String text) {
        // Gửi text đến Gemini API
        // Hiện tại, chỉ mở chat với text
        AiChatBottomSheet aiChatBottomSheet = new AiChatBottomSheet();
        Bundle args = new Bundle();
        args.putString("voice_input", text);
        aiChatBottomSheet.setArguments(args);
        aiChatBottomSheet.show(getSupportFragmentManager(), aiChatBottomSheet.getTag());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startVoiceRecognition();
            } else {
                Toast.makeText(this, "Cần quyền ghi âm để sử dụng voice chat", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startVoiceRecognition() {
        if (!isRecording) {
            isRecording = true;
            aiIcon.setImageResource(R.drawable.ic_microphone);
            speechRecognizer.startListening(speechIntent);
        }
    }

    private void stopVoiceRecognition() {
        if (isRecording) {
            isRecording = false;
            aiIcon.setImageResource(R.drawable.ic_robot);
            speechRecognizer.stopListening();
        }
    }
}