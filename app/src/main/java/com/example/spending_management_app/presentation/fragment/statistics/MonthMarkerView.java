package com.example.spending_management_app.presentation.fragment.statistics;

import android.content.Context;
import android.widget.TextView;

import com.example.spending_management_app.R;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;

import java.util.List;

/**
 * Custom MarkerView to show spending amount when touching chart points
 */
public class MonthMarkerView extends MarkerView {
    private TextView tvContent;
    private List<String> monthLabels;

    public MonthMarkerView(Context context, int layoutResource, List<String> monthLabels) {
        super(context, layoutResource);
        this.monthLabels = monthLabels;
        tvContent = findViewById(R.id.marker_content);
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        int index = (int) e.getX();
        String month = "";
        if (monthLabels != null && index >= 0 && index < monthLabels.size()) {
            month = monthLabels.get(index) + ": ";
        }
        
        String amount = formatCurrency((long) e.getY());
        tvContent.setText(month + amount);
        
        super.refreshContent(e, highlight);
    }

    @Override
    public MPPointF getOffset() {
        return new MPPointF(-(getWidth() / 2f), -getHeight());
    }

    private String formatCurrency(long amount) {
        String amountStr = String.valueOf(Math.abs(amount));
        StringBuilder formatted = new StringBuilder();
        int count = 0;
        for (int i = amountStr.length() - 1; i >= 0; i--) {
            formatted.insert(0, amountStr.charAt(i));
            count++;
            if (count % 3 == 0 && i > 0) {
                formatted.insert(0, ",");
            }
        }
        return formatted.toString() + " VND";
    }
}
