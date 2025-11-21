package com.example.spending_management_app.ui.helpcenter;

import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

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
        setupBackButton();

        binding.askAiButton.setOnClickListener(v -> {
            AiChatBottomSheet bottomSheet = new AiChatBottomSheet();
            bottomSheet.show(getChildFragmentManager(), AiChatBottomSheet.TAG);
        });

        return root;
    }

    private void setupBackButton() {
        // Add underline to the back button text
        binding.tvBack.setPaintFlags(binding.tvBack.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

        binding.backButton.setOnClickListener(v -> {
            NavHostFragment.findNavController(this).navigateUp();
        });
    }

    private void setupFaqToggle() {
        // Pass the specific icon resource for each question to preserve it
        setToggle(binding.faq1Question, binding.faq1Answer, R.drawable.ic_faq_add);
        setToggle(binding.faq2Question, binding.faq2Answer, R.drawable.ic_faq_edit);
        setToggle(binding.faq3Question, binding.faq3Answer, R.drawable.ic_faq_chart);
        setToggle(binding.faq4Question, binding.faq4Answer, R.drawable.ic_faq_backup);
        setToggle(binding.faq5Question, binding.faq5Answer, R.drawable.ic_faq_ai);
    }

    private void setToggle(TextView question, TextView answer, int startIconResId) {
        question.setOnClickListener(v -> {
            if (answer.getVisibility() == View.GONE) {
                answer.setVisibility(View.VISIBLE);
                // Preserve the start icon, update the end icon to 'up' arrow. Note: ic_chevron_right rotated -90 deg is up
                question.setCompoundDrawablesWithIntrinsicBounds(startIconResId, 0, R.drawable.ic_arrow_up, 0);
            } else {
                answer.setVisibility(View.GONE);
                // Preserve the start icon, update the end icon to 'down' arrow. Note: ic_chevron_right is right, we used ic_chevron_right in xml
                question.setCompoundDrawablesWithIntrinsicBounds(startIconResId, 0, R.drawable.ic_chevron_right, 0);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
