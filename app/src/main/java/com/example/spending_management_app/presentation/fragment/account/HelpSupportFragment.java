package com.example.spending_management_app.presentation.fragment.account;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.spending_management_app.R;
import com.example.spending_management_app.presentation.dialog.AiChatBottomSheet;
import com.example.spending_management_app.utils.ToastHelper;

public class HelpSupportFragment extends Fragment {

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_help_support, container, false);

        setupFAQ(root);
        setupButtons(root);
        setupBackButton(root);

        return root;
    }

    private void setupFAQ(View root) {
        // Setup click listeners for FAQ items
        setupFAQItem(root, R.id.faq1, R.id.answer1);
        setupFAQItem(root, R.id.faq2, R.id.answer2);
        setupFAQItem(root, R.id.faq3, R.id.answer3);
        setupFAQItem(root, R.id.faq4, R.id.answer4);
        setupFAQItem(root, R.id.faq5, R.id.answer5);
        setupFAQItem(root, R.id.faq6, R.id.answer6);
        setupFAQItem(root, R.id.faq7, R.id.answer7);
        setupFAQItem(root, R.id.faq8, R.id.answer8);
        setupFAQItem(root, R.id.faq9, R.id.answer9);
    }

    private void setupFAQItem(View root, int questionId, int answerId) {
        View questionLayout = root.findViewById(questionId);
        View answerView = root.findViewById(answerId);
        questionLayout.setOnClickListener(v -> {
            if (answerView.getVisibility() == View.VISIBLE) {
                answerView.setVisibility(View.GONE);
            } else {
                answerView.setVisibility(View.VISIBLE);
            }
        });
    }

    private void setupButtons(View root) {
        root.findViewById(R.id.btn_chat_ai).setOnClickListener(v -> {
            AiChatBottomSheet aiChatBottomSheet = new AiChatBottomSheet();
            aiChatBottomSheet.show(getParentFragmentManager(), aiChatBottomSheet.getTag());
        });

        root.findViewById(R.id.btn_feedback).setOnClickListener(v -> showFeedbackDialog());
    }

    private void setupBackButton(View root) {
        root.findViewById(R.id.btn_back).setOnClickListener(v -> {
            Navigation.findNavController(requireView()).navigateUp();
        });
    }

    private void showFeedbackDialog() {
        Dialog dialog = new Dialog(getContext(), R.style.RoundedDialog4Corners);
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_feedback, null);
        dialog.setContentView(dialogView);

        dialogView.findViewById(R.id.btn_cancel).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btn_submit).setOnClickListener(v -> {
            // Handle feedback submission
            ToastHelper.showToastOnTop(getActivity(), getString(R.string.feedback_sent_success));
            dialog.dismiss();
        });

        dialog.show();
    }
}
