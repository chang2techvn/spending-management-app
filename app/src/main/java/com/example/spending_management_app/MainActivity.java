package com.example.spending_management_app;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.spending_management_app.databinding.ActivityMainBinding;
import com.example.spending_management_app.ui.AiChatBottomSheet;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private NavController navController;
    private LinearLayout navHome, navStatistics, navHistory, navAccount;
    private View navAiAssistant;
    private View homeHeader;

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
            AiChatBottomSheet aiChatBottomSheet = new AiChatBottomSheet();
            aiChatBottomSheet.show(getSupportFragmentManager(), aiChatBottomSheet.getTag());
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
}