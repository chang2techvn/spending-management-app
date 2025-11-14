package com.example.spending_management_app.ui.helpcenter;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.spending_management_app.R;
import com.example.spending_management_app.databinding.FragmentHelpCenterBinding;
import com.example.spending_management_app.ui.AiChatBottomSheet;

public class HelpCenterFragment extends Fragment {

    private FragmentHelpCenterBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHelpCenterBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        setupFaqToggle();

        binding.askAiButton.setOnClickListener(v -> {
            AiChatBottomSheet bottomSheet = new AiChatBottomSheet();
            bottomSheet.show(getChildFragmentManager(), AiChatBottomSheet.TAG);
        });

        return root;
    }

    private void setupFaqToggle() {
        setToggle(binding.faq1Question, binding.faq1Answer);
        setToggle(binding.faq2Question, binding.faq2Answer);
        setToggle(binding.faq3Question, binding.faq3Answer);
        setToggle(binding.faq4Question, binding.faq4Answer);
        setToggle(binding.faq5Question, binding.faq5Answer);
    }

    private void setToggle(TextView question, TextView answer) {
        question.setOnClickListener(v -> {
            if (answer.getVisibility() == View.GONE) {
                answer.setVisibility(View.VISIBLE);
                question.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_arrow_up, 0);
            } else {
                answer.setVisibility(View.GONE);
                question.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_arrow_down, 0);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
