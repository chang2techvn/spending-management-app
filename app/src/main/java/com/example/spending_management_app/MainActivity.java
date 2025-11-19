package com.example.spending_management_app;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
    private static final int VOICE_REQUEST_CODE = 1001;

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

        // Setup user avatar click listener
        ImageView userAvatar = findViewById(R.id.user_avatar);
        userAvatar.setOnClickListener(v -> navController.navigate(R.id.navigation_account));

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
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
                return true;
            }
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN");
            intent.putExtra(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES, new String[]{"en-US", "vi-VN"});
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Nói gì đó...");
            try {
                startActivityForResult(intent, VOICE_REQUEST_CODE);
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, "Thiết bị không hỗ trợ nhận diện giọng nói", Toast.LENGTH_SHORT).show();
            }
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
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN");
                intent.putExtra(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES, new String[]{"en-US", "vi-VN"});
                intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Nói gì đó...");
                try {
                    startActivityForResult(intent, VOICE_REQUEST_CODE);
                } catch (Exception e) {
                    Toast.makeText(this, "Thiết bị không hỗ trợ nhận diện giọng nói", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Cần quyền ghi âm để sử dụng voice chat", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VOICE_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                String spokenText = results.get(0);
                processVoiceInput(spokenText);
            }
        }
    }

    public void openAiChat(String prompt) {
        AiChatBottomSheet aiChatBottomSheet = new AiChatBottomSheet();
        Bundle args = new Bundle();
        args.putString("initial_prompt", prompt);
        aiChatBottomSheet.setArguments(args);
        aiChatBottomSheet.show(getSupportFragmentManager(), aiChatBottomSheet.getTag());
    }
    
    public void openExpenseBulkManagement() {
        AiChatBottomSheet aiChatBottomSheet = new AiChatBottomSheet();
        Bundle args = new Bundle();
        args.putString("mode", "expense_bulk_management");
        args.putString("initial_prompt", "expense_bulk");
        aiChatBottomSheet.setArguments(args);
        aiChatBottomSheet.show(getSupportFragmentManager(), aiChatBottomSheet.getTag());
    }
}