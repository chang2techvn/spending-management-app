package com.example.spending_management_app.ui.account;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.spending_management_app.R;
import com.example.spending_management_app.databinding.FragmentAccountBinding;

public class AccountFragment extends Fragment {

    private FragmentAccountBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAccountBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Setup account data
        setupAccountData();

        // Setup click listeners
        setupClickListeners();

        return root;
    }

    private void setupAccountData() {
        // Set user information
        binding.userName.setText("Nguyễn Văn A");
        binding.userEmail.setText("nguyenvana@example.com");
    }

    private void setupClickListeners() {
        binding.editProfileOption.setOnClickListener(v -> showEditProfileDialog());

        binding.changePasswordOption.setOnClickListener(v -> showChangePasswordDialog());

        binding.settingsOption.setOnClickListener(v -> showSettingsDialog());

        binding.helpSupportOption.setOnClickListener(v -> showHelpSupportDialog());

        binding.logoutButton.setOnClickListener(v -> showLogoutConfirmationDialog());
    }

    private void showEditProfileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.RoundedDialog);
        builder.setTitle("Chỉnh sửa hồ sơ");

        // Create input fields
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 16, 32, 16);

        final EditText nameInput = new EditText(getContext());
        nameInput.setHint("Tên đầy đủ");
        nameInput.setText("Nguyễn Văn A");
        layout.addView(nameInput);

        final EditText emailInput = new EditText(getContext());
        emailInput.setHint("Email");
        emailInput.setText("nguyenvana@example.com");
        layout.addView(emailInput);

        builder.setView(layout);

        builder.setPositiveButton("Lưu", (dialog, which) -> {
            String newName = nameInput.getText().toString().trim();
            String newEmail = emailInput.getText().toString().trim();

            if (!newName.isEmpty() && !newEmail.isEmpty()) {
                binding.userName.setText(newName);
                binding.userEmail.setText(newEmail);
                Toast.makeText(getContext(), "Hồ sơ đã được cập nhật", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.RoundedDialog);
        builder.setTitle("Đổi mật khẩu");

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 16, 32, 16);

        final EditText currentPasswordInput = new EditText(getContext());
        currentPasswordInput.setHint("Mật khẩu hiện tại");
        currentPasswordInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(currentPasswordInput);

        final EditText newPasswordInput = new EditText(getContext());
        newPasswordInput.setHint("Mật khẩu mới");
        newPasswordInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(newPasswordInput);

        final EditText confirmPasswordInput = new EditText(getContext());
        confirmPasswordInput.setHint("Xác nhận mật khẩu mới");
        confirmPasswordInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(confirmPasswordInput);

        builder.setView(layout);

        builder.setPositiveButton("Đổi mật khẩu", (dialog, which) -> {
            String currentPassword = currentPasswordInput.getText().toString();
            String newPassword = newPasswordInput.getText().toString();
            String confirmPassword = confirmPasswordInput.getText().toString();

            if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            } else if (!newPassword.equals(confirmPassword)) {
                Toast.makeText(getContext(), "Mật khẩu xác nhận không khớp", Toast.LENGTH_SHORT).show();
            } else {
                // TODO: Implement actual password change logic
                Toast.makeText(getContext(), "Mật khẩu đã được thay đổi", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.RoundedDialog);
        builder.setTitle("Cài đặt");

        String[] settings = {"Thông báo", "Ngôn ngữ", "Chủ đề", "Bảo mật"};
        boolean[] checkedItems = {true, false, false, true};

        builder.setMultiChoiceItems(settings, checkedItems, (dialog, which, isChecked) -> {
            checkedItems[which] = isChecked;
        });

        builder.setPositiveButton("Lưu", (dialog, which) -> {
            Toast.makeText(getContext(), "Cài đặt đã được lưu", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    private void showHelpSupportDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.RoundedDialog);
        builder.setTitle("Trợ giúp & Hỗ trợ");
        builder.setMessage("Liên hệ với chúng tôi:\n\nEmail: support@spendingapp.com\nĐiện thoại: 1900-xxxx\n\nPhiên bản: 1.0.0");

        builder.setPositiveButton("OK", null);
        builder.show();
    }

    private void showLogoutConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.RoundedDialog);
        builder.setTitle("Xác nhận đăng xuất");
        builder.setMessage("Bạn có chắc chắn muốn đăng xuất?");

        builder.setPositiveButton("Đăng xuất", (dialog, which) -> {
            // TODO: Implement actual logout logic
            Toast.makeText(getContext(), "Đã đăng xuất", Toast.LENGTH_SHORT).show();
            // Navigate to login screen or close app
        });

        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
