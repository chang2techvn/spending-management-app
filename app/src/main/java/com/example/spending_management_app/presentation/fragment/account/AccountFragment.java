package com.example.spending_management_app.presentation.fragment.account;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;
import android.text.TextWatcher;
import android.text.Editable;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.AutoCompleteTextView;
import androidx.appcompat.widget.SwitchCompat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.example.spending_management_app.R;
import com.example.spending_management_app.data.local.database.AppDatabase;
import com.example.spending_management_app.data.local.entity.UserEntity;
import com.example.spending_management_app.data.repository.UserRepositoryImpl;
import com.example.spending_management_app.databinding.FragmentAccountBinding;
import com.example.spending_management_app.domain.repository.UserRepository;
import com.example.spending_management_app.presentation.activity.LoginActivity;
import com.example.spending_management_app.utils.PasswordUtils;
import com.example.spending_management_app.utils.SessionManager;

public class AccountFragment extends Fragment {

    private FragmentAccountBinding binding;
    private SessionManager sessionManager;
    private UserEntity currentUser;
    private Uri selectedAvatarUri;
    private UserRepository userRepository;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAccountBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Initialize session manager
        sessionManager = new SessionManager(getContext());

        // Initialize repository
        userRepository = new UserRepositoryImpl(AppDatabase.getInstance(getContext()));

        // Setup account data
        setupAccountData();

        // Setup click listeners
        setupClickListeners();

        return root;
    }

    private void setupAccountData() {
        // Get current user from session
        currentUser = sessionManager.getUserData();

        if (currentUser != null) {
            // Set user information
            binding.userName.setText(currentUser.getName());
            binding.userEmail.setText(currentUser.getEmailOrPhone());

            // Load user avatar
            if (currentUser.getAvatar() != null && !currentUser.getAvatar().isEmpty()) {
                Glide.with(this)
                    .load(currentUser.getAvatar())
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .circleCrop()
                    .into(binding.userAvatar);
            } else {
                binding.userAvatar.setImageResource(R.drawable.ic_launcher_foreground);
            }
        } else {
            // Fallback if no user data
            binding.userName.setText("Người dùng");
            binding.userEmail.setText("Chưa đăng nhập");
        }
    }

    private void setupClickListeners() {
        binding.editProfileOption.setOnClickListener(v -> showEditProfileDialog());
        
        binding.profileCard.setOnClickListener(v -> showEditProfileDialog());
        
        binding.changePasswordOption.setOnClickListener(v -> showChangePasswordDialog());

        binding.settingsOption.setOnClickListener(v -> showSettingsDialog());

        binding.helpSupportOption.setOnClickListener(v -> navigateToHelpSupport());

        binding.logoutButton.setOnClickListener(v -> showLogoutConfirmationDialog());
    }    private void showEditProfileDialog() {
        Dialog dialog = new Dialog(getContext(), R.style.RoundedDialog4Corners);
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_profile, null);
        dialog.setContentView(dialogView);

        // Initialize views
        ImageView userAvatar = dialogView.findViewById(R.id.user_avatar);
        ImageButton editAvatarButton = dialogView.findViewById(R.id.edit_avatar_button);
        EditText nameInput = dialogView.findViewById(R.id.name_input);
        EditText emailPhoneInput = dialogView.findViewById(R.id.email_phone_input);

        // Set current data
        if (currentUser != null) {
            nameInput.setText(currentUser.getName());
            emailPhoneInput.setText(currentUser.getEmailOrPhone());

            if (currentUser.getAvatar() != null && !currentUser.getAvatar().isEmpty()) {
                Glide.with(this)
                    .load(currentUser.getAvatar())
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .circleCrop()
                    .into(userAvatar);
            } else {
                userAvatar.setImageResource(R.drawable.ic_launcher_foreground);
            }
        }

        // Edit avatar click listener
        editAvatarButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, 1001);
        });

        // Cancel button
        dialogView.findViewById(R.id.btn_cancel).setOnClickListener(v -> dialog.dismiss());

        // Save button
        dialogView.findViewById(R.id.btn_save).setOnClickListener(v -> {
            String newName = nameInput.getText().toString().trim();
            String newEmailPhone = emailPhoneInput.getText().toString().trim();

            if (newName.isEmpty() || newEmailPhone.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            // Update user entity
            if (currentUser != null) {
                currentUser.setName(newName);
                currentUser.setEmailOrPhone(newEmailPhone);
                if (selectedAvatarUri != null) {
                    currentUser.setAvatar(selectedAvatarUri.toString());
                }

                // Save to database
                new Thread(() -> {
                    userRepository.updateUser(currentUser);
                    getActivity().runOnUiThread(() -> {
                        // Update session
                        sessionManager.updateUserData(currentUser);

                        // Update UI
                        binding.userName.setText(newName);
                        binding.userEmail.setText(newEmailPhone);
                        if (selectedAvatarUri != null) {
                            Glide.with(this)
                                .load(selectedAvatarUri)
                                .circleCrop()
                                .into(binding.userAvatar);
                        }

                        Toast.makeText(getContext(), "Hồ sơ đã được cập nhật", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    });
                }).start();
            }
        });

        dialog.show();
    }

    private void showChangePasswordDialog() {
        Dialog dialog = new Dialog(getContext(), R.style.RoundedDialog4Corners);
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_change_password, null);
        dialog.setContentView(dialogView);

        // Find views
        TextInputLayout currentPasswordLayout = dialogView.findViewById(R.id.current_password_layout);
        TextInputEditText currentPasswordInput = dialogView.findViewById(R.id.current_password_input);
        TextInputEditText newPasswordInput = dialogView.findViewById(R.id.new_password_input);
        TextInputEditText confirmPasswordInput = dialogView.findViewById(R.id.confirm_password_input);

        // Add text change listener for real-time validation
        currentPasswordInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String password = s.toString();
                if (password.isEmpty()) {
                    currentPasswordLayout.setEndIconDrawable(null);
                } else if (PasswordUtils.verifyPassword(password, currentUser.getPasswordHash())) {
                    currentPasswordLayout.setEndIconDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_check_green));
                    currentPasswordLayout.setEndIconTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
                } else {
                    currentPasswordLayout.setEndIconDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_check_red));
                    currentPasswordLayout.setEndIconTintList(ColorStateList.valueOf(Color.parseColor("#F44336")));
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Cancel button
        dialogView.findViewById(R.id.btn_cancel).setOnClickListener(v -> dialog.dismiss());

        // Change password button
        dialogView.findViewById(R.id.btn_change_password).setOnClickListener(v -> {
            String currentPassword = currentPasswordInput.getText().toString().trim();
            String newPassword = newPasswordInput.getText().toString().trim();
            String confirmPassword = confirmPasswordInput.getText().toString().trim();

            if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!PasswordUtils.verifyPassword(currentPassword, currentUser.getPasswordHash())) {
                Toast.makeText(getContext(), "Mật khẩu hiện tại không đúng", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                Toast.makeText(getContext(), "Mật khẩu xác nhận không khớp", Toast.LENGTH_SHORT).show();
                return;
            }

            // Update password
            String hashedNewPassword = PasswordUtils.hashPassword(newPassword);
            currentUser.setPasswordHash(hashedNewPassword);

            // Save to database
            new Thread(() -> {
                userRepository.updateUser(currentUser);
                getActivity().runOnUiThread(() -> {
                    // Update session
                    sessionManager.updateUserData(currentUser);

                    Toast.makeText(getContext(), "Mật khẩu đã được thay đổi", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                });
            }).start();
        });

        dialog.show();
    }

    private void showSettingsDialog() {
        Dialog dialog = new Dialog(getContext(), R.style.RoundedDialog4Corners);
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_settings, null);
        dialog.setContentView(dialogView);

        // Find views
        SwitchCompat notificationSwitch = dialogView.findViewById(R.id.notification_switch);
        AutoCompleteTextView languageDropdown = dialogView.findViewById(R.id.language_dropdown);
        AutoCompleteTextView currencyDropdown = dialogView.findViewById(R.id.currency_dropdown);

        // Setup language dropdown
        ArrayAdapter<String> languageAdapter = new ArrayAdapter<>(getContext(),
            android.R.layout.simple_dropdown_item_1line, new String[]{"Việt", "Anh"});
        languageDropdown.setAdapter(languageAdapter);

        // Setup currency dropdown
        ArrayAdapter<String> currencyAdapter = new ArrayAdapter<>(getContext(),
            android.R.layout.simple_dropdown_item_1line, new String[]{"VND", "USD"});
        currencyDropdown.setAdapter(currencyAdapter);

        // Load current settings (for demo, set defaults)
        // In real app, load from SharedPreferences
        notificationSwitch.setChecked(true); // Default on
        languageDropdown.setText("Việt", false); // Default Vietnamese
        currencyDropdown.setText("VND", false); // Default VND

        // Cancel button
        dialogView.findViewById(R.id.btn_cancel).setOnClickListener(v -> dialog.dismiss());

        // Save button
        dialogView.findViewById(R.id.btn_save).setOnClickListener(v -> {
            boolean notificationsEnabled = notificationSwitch.isChecked();
            String selectedLanguage = languageDropdown.getText().toString();
            String selectedCurrency = currencyDropdown.getText().toString();

            // Save settings (for demo, just show toast)
            // In real app, save to SharedPreferences or database
            Toast.makeText(getContext(),
                "Thông báo: " + (notificationsEnabled ? "Bật" : "Tắt") +
                ", Ngôn ngữ: " + selectedLanguage +
                ", Tiền tệ: " + selectedCurrency, Toast.LENGTH_LONG).show();

            dialog.dismiss();
        });

        dialog.show();
    }

    private void navigateToHelpSupport() {
        // Navigate to Help & Support fragment using Navigation Component
        Navigation.findNavController(requireView()).navigate(R.id.action_navigation_account_to_navigation_help_support);
    }

    private void showLogoutConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.RoundedDialog4Corners);
        builder.setTitle("Xác nhận đăng xuất");
        builder.setMessage("Bạn có chắc chắn muốn đăng xuất?");

        builder.setPositiveButton("Đăng xuất", (dialog, which) -> {
            // Perform actual logout
            sessionManager.logout();

            // Navigate to login screen
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            getActivity().finish();
        });

        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == getActivity().RESULT_OK && data != null) {
            selectedAvatarUri = data.getData();
            // Note: The avatar will be updated in the dialog when save is clicked
        }
    }
}
