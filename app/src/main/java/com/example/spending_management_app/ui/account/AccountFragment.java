package com.example.spending_management_app.ui.account;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.spending_management_app.R;
import com.example.spending_management_app.database.AppDatabase;
import com.example.spending_management_app.database.entity.UserEntity;
import com.example.spending_management_app.databinding.FragmentAccountBinding;
import com.example.spending_management_app.ui.login.LoginActivity;
import com.example.spending_management_app.utils.PasswordHasher;

import java.util.concurrent.Executors;

public class AccountFragment extends Fragment {

    private FragmentAccountBinding binding;
    private AppDatabase appDatabase;
    private SharedPreferences sharedPreferences;
    private UserEntity currentUser; // Store the current user object

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAccountBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        appDatabase = AppDatabase.getInstance(requireContext());
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());

        loadUserData();
        setupClickListeners();

        return root;
    }

    private void loadUserData() {
        int userId = sharedPreferences.getInt(LoginActivity.KEY_USER_ID, -1);
        if (userId == -1) {
            Toast.makeText(getContext(), "Error: User not found.", Toast.LENGTH_SHORT).show();
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            currentUser = appDatabase.userDao().findUserById(userId);
            if (currentUser != null && getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    binding.userName.setText(currentUser.firstName + " " + currentUser.lastName);
                    binding.userEmail.setText(currentUser.email);
                });
            }
        });
    }

    private void setupClickListeners() {
        binding.editProfileOption.setOnClickListener(v -> showEditProfileDialog());
        binding.changePasswordOption.setOnClickListener(v -> showChangePasswordDialog());
        binding.settingsOption.setOnClickListener(v -> showSettingsDialog());
        binding.helpSupportOption.setOnClickListener(v -> showHelpSupportDialog());
        binding.signoutButton.setOnClickListener(v -> showLogoutConfirmationDialog());
    }

    private void showEditProfileDialog() {
        if (currentUser == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.RoundedDialog4Corners);
        builder.setTitle("Chỉnh sửa hồ sơ");

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 16, 32, 16);

        final EditText firstNameInput = new EditText(getContext());
        firstNameInput.setHint("Tên");
        firstNameInput.setText(currentUser.firstName);
        layout.addView(firstNameInput);

        final EditText lastNameInput = new EditText(getContext());
        lastNameInput.setHint("Họ");
        lastNameInput.setText(currentUser.lastName);
        layout.addView(lastNameInput);

        final EditText emailInput = new EditText(getContext());
        emailInput.setHint("Email");
        emailInput.setText(currentUser.email);
        layout.addView(emailInput);

        builder.setView(layout);

        builder.setPositiveButton("Lưu", (dialog, which) -> {
            String newFirstName = firstNameInput.getText().toString().trim();
            String newLastName = lastNameInput.getText().toString().trim();
            String newEmail = emailInput.getText().toString().trim();

            if (!newFirstName.isEmpty() && !newLastName.isEmpty() && !newEmail.isEmpty()) {
                // Update user object
                currentUser.firstName = newFirstName;
                currentUser.lastName = newLastName;
                currentUser.email = newEmail;

                // Save to database
                Executors.newSingleThreadExecutor().execute(() -> {
                    appDatabase.userDao().update(currentUser);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            binding.userName.setText(currentUser.firstName + " " + currentUser.lastName);
                            binding.userEmail.setText(currentUser.email);
                            Toast.makeText(getContext(), "Hồ sơ đã được cập nhật", Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            } else {
                Toast.makeText(getContext(), "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    private void showChangePasswordDialog() {
        if (currentUser == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.RoundedDialog4Corners);
        builder.setTitle("Đổi mật khẩu");

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 16, 32, 16);

        final EditText currentPasswordInput = new EditText(getContext());
        currentPasswordInput.setHint("Mật khẩu hiện tại");
        currentPasswordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(currentPasswordInput);

        final EditText newPasswordInput = new EditText(getContext());
        newPasswordInput.setHint("Mật khẩu mới");
        newPasswordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(newPasswordInput);

        final EditText confirmPasswordInput = new EditText(getContext());
        confirmPasswordInput.setHint("Xác nhận mật khẩu mới");
        confirmPasswordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(confirmPasswordInput);

        builder.setView(layout);

        builder.setPositiveButton("Đổi mật khẩu", (dialog, which) -> {
            String currentPassword = currentPasswordInput.getText().toString();
            String newPassword = newPasswordInput.getText().toString();
            String confirmPassword = confirmPasswordInput.getText().toString();

            if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!newPassword.equals(confirmPassword)) {
                Toast.makeText(getContext(), "Mật khẩu xác nhận không khớp", Toast.LENGTH_SHORT).show();
                return;
            }

            Executors.newSingleThreadExecutor().execute(() -> {
                String hashedCurrentPassword = PasswordHasher.hashPassword(currentPassword);

                // Verify current password
                if (!hashedCurrentPassword.equals(currentUser.password)) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Mật khẩu hiện tại không đúng", Toast.LENGTH_SHORT).show());
                    }
                    return;
                }

                // Update to new password
                currentUser.password = PasswordHasher.hashPassword(newPassword);
                appDatabase.userDao().update(currentUser);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Mật khẩu đã được thay đổi", Toast.LENGTH_SHORT).show());
                }
            });
        });

        builder.setNegativeButton("Hủy", null);
        builder.show();
    }
    
    private void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(getContext(), R.style.RoundedDialog4Corners)
                .setTitle("Xác nhận đăng xuất")
                .setMessage("Bạn có chắc chắn muốn đăng xuất?")
                .setPositiveButton("Đăng xuất", (dialog, which) -> {
                    sharedPreferences.edit().remove(LoginActivity.KEY_USER_ID).apply();
                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    if (getActivity() != null) {
                        getActivity().finish();
                    }
                    Toast.makeText(getContext(), "Đã đăng xuất", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
    
    // --- Other dialog methods ---

    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.RoundedDialog4Corners);
        builder.setTitle("Cài đặt");
        String[] settings = {"Thông báo", "Ngôn ngữ", "Chủ đề", "Bảo mật"};
        boolean[] checkedItems = {true, false, false, true};
        builder.setMultiChoiceItems(settings, checkedItems, (dialog, which, isChecked) -> {});
        builder.setPositiveButton("Lưu", (dialog, which) -> Toast.makeText(getContext(), "Cài đặt đã được lưu", Toast.LENGTH_SHORT).show());
        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    private void showHelpSupportDialog() {
        new AlertDialog.Builder(getContext(), R.style.RoundedDialog4Corners)
                .setTitle("Trợ giúp & Hỗ trợ")
                .setMessage("Liên hệ với chúng tôi:\n\nEmail: support@spendingapp.com\nĐiện thoại: 1900-xxxx\n\nPhiên bản: 1.0.0")
                .setPositiveButton("OK", null)
                .show();
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
