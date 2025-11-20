package com.example.spending_management_app.ui.account;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.spending_management_app.MainActivity;
import com.example.spending_management_app.R;
import com.example.spending_management_app.database.AppDatabase;
import com.example.spending_management_app.database.entity.UserEntity;
import com.example.spending_management_app.databinding.FragmentAccountBinding;
import com.example.spending_management_app.ui.login.LoginActivity;
import com.example.spending_management_app.utils.PasswordHasher;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.shape.ShapeAppearanceModel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.Executors;

import static android.app.Activity.RESULT_OK;

public class AccountFragment extends Fragment {

    private static final int PICK_IMAGE_REQUEST = 1;
    public static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";

    private FragmentAccountBinding binding;
    private AppDatabase appDatabase;
    private SharedPreferences sharedPreferences;
    private UserEntity currentUser;
    private ShapeableImageView dialogAvatarImageView;
    private String newAvatarPath; // To temporarily store the new avatar file path

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
            if (getActivity() != null) {
                getActivity().runOnUiThread(this::updateUI);
            }
        });
    }

    private void setupClickListeners() {
        binding.userAvatar.setOnClickListener(v -> showEditProfileDialog());
        binding.editProfileOption.setOnClickListener(v -> showEditProfileDialog());
        binding.changePasswordOption.setOnClickListener(v -> showChangePasswordDialog());
        binding.settingsOption.setOnClickListener(v -> showSettingsDialog());
        binding.helpSupportOption.setOnClickListener(v -> {
            NavHostFragment.findNavController(this).navigate(R.id.action_navigation_account_to_helpCenterFragment);
        });
        binding.signoutButton.setOnClickListener(v -> showLogoutConfirmationDialog());
    }

    private void updateUI() {
        if (currentUser == null || getContext() == null || binding == null) {
            return;
        }
        String firstName = currentUser.firstName != null ? currentUser.firstName : "";
        String lastName = currentUser.lastName != null ? currentUser.lastName : "";
        String email = currentUser.email != null ? currentUser.email : "";

        binding.userName.setText(String.format("%s %s", firstName, lastName).trim());
        binding.userEmail.setText(email);

        if (currentUser.avatarUri != null && !currentUser.avatarUri.isEmpty()) {
            File avatarFile = new File(currentUser.avatarUri);
            if (avatarFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(avatarFile.getAbsolutePath());
                binding.userAvatar.setImageBitmap(bitmap);
            } else {
                binding.userAvatar.setImageResource(R.drawable.ic_launcher_foreground);
            }
        } else {
            binding.userAvatar.setImageResource(R.drawable.ic_launcher_foreground);
        }
    }

    private void showEditProfileDialog() {
        if (currentUser == null || getContext() == null) return;

        newAvatarPath = null; // Reset temp path each time dialog is opened

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Chỉnh sửa hồ sơ");

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 24);
        layout.setGravity(android.view.Gravity.CENTER_HORIZONTAL);

        dialogAvatarImageView = new ShapeableImageView(getContext());
        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(128, 128);
        avatarParams.setMargins(0, 0, 0, 16);
        dialogAvatarImageView.setLayoutParams(avatarParams);
        dialogAvatarImageView.setScaleType(ShapeableImageView.ScaleType.CENTER_CROP);

        dialogAvatarImageView.setShapeAppearanceModel(ShapeAppearanceModel.builder()
                .setAllCornerSizes(ShapeAppearanceModel.PILL)
                .build());

        dialogAvatarImageView.setStrokeWidth(6.0f);
        dialogAvatarImageView.setStrokeColorResource(R.color.primaryBlue);

        if (currentUser.avatarUri != null && !currentUser.avatarUri.isEmpty()) {
            File avatarFile = new File(currentUser.avatarUri);
            if (avatarFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(avatarFile.getAbsolutePath());
                dialogAvatarImageView.setImageBitmap(bitmap);
            } else {
                dialogAvatarImageView.setImageResource(R.drawable.ic_launcher_foreground);
            }
        } else {
            dialogAvatarImageView.setImageResource(R.drawable.ic_launcher_foreground);
        }

        dialogAvatarImageView.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        });

        layout.addView(dialogAvatarImageView);

        final EditText firstNameInput = new EditText(getContext());
        firstNameInput.setHint("Tên");
        firstNameInput.setText(currentUser.firstName != null ? currentUser.firstName : "");
        layout.addView(firstNameInput);

        final EditText lastNameInput = new EditText(getContext());
        lastNameInput.setHint("Họ");
        lastNameInput.setText(currentUser.lastName != null ? currentUser.lastName : "");
        layout.addView(lastNameInput);

        final EditText emailInput = new EditText(getContext());
        emailInput.setHint("Email");
        emailInput.setText(currentUser.email != null ? currentUser.email : "");
        layout.addView(emailInput);

        builder.setView(layout);

        builder.setPositiveButton("Lưu", (dialog, which) -> {
            String newFirstName = firstNameInput.getText().toString().trim();
            String newLastName = lastNameInput.getText().toString().trim();
            String newEmail = emailInput.getText().toString().trim();

            if (!newFirstName.isEmpty() && !newLastName.isEmpty() && !newEmail.isEmpty()) {
                currentUser.firstName = newFirstName;
                currentUser.lastName = newLastName;
                currentUser.email = newEmail;

                if (newAvatarPath != null) {
                    currentUser.avatarUri = newAvatarPath;
                }

                Executors.newSingleThreadExecutor().execute(() -> {
                    appDatabase.userDao().update(currentUser);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            updateUI();
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            try {
                InputStream inputStream = requireContext().getContentResolver().openInputStream(imageUri);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                File file = new File(requireContext().getFilesDir(), "avatar_" + currentUser.userId + ".png");
                FileOutputStream outputStream = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                outputStream.close();
                if(inputStream != null) {
                    inputStream.close();
                }

                newAvatarPath = file.getAbsolutePath();
                dialogAvatarImageView.setImageBitmap(bitmap);

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getContext(), "Lỗi khi tải ảnh", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showChangePasswordDialog() {
        if (currentUser == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
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

                if (!hashedCurrentPassword.equals(currentUser.password)) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Mật khẩu hiện tại không đúng", Toast.LENGTH_SHORT).show());
                    }
                    return;
                }

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
        new AlertDialog.Builder(requireContext())
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

    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Cài đặt");

        // Inflate custom layout
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_settings, null);
        builder.setView(dialogView);

        // Initialize Views
        LinearLayout layoutNotifications = dialogView.findViewById(R.id.layout_notifications);
        CheckBox checkboxNotifications = dialogView.findViewById(R.id.checkbox_notifications);
        LinearLayout layoutLanguage = dialogView.findViewById(R.id.layout_language);

        // Set initial state
        boolean notificationsEnabled = sharedPreferences.getBoolean(KEY_NOTIFICATIONS_ENABLED, true);
        checkboxNotifications.setChecked(notificationsEnabled);

        // Setup Listeners
        final AlertDialog dialog = builder.create();

        layoutNotifications.setOnClickListener(v -> {
            boolean newState = !checkboxNotifications.isChecked();
            checkboxNotifications.setChecked(newState);
            sharedPreferences.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, newState).apply();
            Toast.makeText(getContext(), newState ? "Thông báo được bật" : "Thông báo được tắt", Toast.LENGTH_SHORT).show();
        });

        layoutLanguage.setOnClickListener(v -> {
            dialog.dismiss();
            showLanguageDialog();
        });

        builder.setPositiveButton("OK", null);
        builder.show();
    }

    private void showLanguageDialog() {
        final String[] languages = {getString(R.string.english), getString(R.string.vietnamese)};
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(getString(R.string.choose_language));
        builder.setSingleChoiceItems(languages, -1, (dialog, which) -> {
            if (getActivity() instanceof MainActivity) {
                String lang = (which == 0) ? "en" : "vi";
                ((MainActivity) getActivity()).setLocale(lang);
                requireActivity().finish();
                requireActivity().startActivity(requireActivity().getIntent());
            }
            dialog.dismiss();
        });
        builder.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
