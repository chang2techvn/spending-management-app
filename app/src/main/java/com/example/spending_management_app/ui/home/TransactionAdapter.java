package com.example.spending_management_app.ui.home;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.spending_management_app.R;

import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    private List<Transaction> transactions;
    private Context context;

    public TransactionAdapter(List<Transaction> transactions) {
        this.transactions = transactions;
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        Transaction transaction = transactions.get(position);

        holder.titleTextView.setText(transaction.getDescription());
        holder.categoryTextView.setText(transaction.getCategory());
        holder.dateTextView.setText(transaction.getFormattedDate());
        holder.amountTextView.setText(transaction.getFormattedAmount());

        // Set amount color based on type
        if (transaction.getAmount() >= 0) {
            holder.amountTextView.setTextColor(context.getColor(R.color.income_color));
        } else {
            holder.amountTextView.setTextColor(context.getColor(R.color.expense_color));
        }

        // Set icon based on category
        int iconResId = getIconResourceId(transaction.getIconResName());
        holder.iconImageView.setImageResource(iconResId);

        // Set icon background color based on category
        int backgroundColor = getCategoryColor(transaction.getCategory());
        holder.iconImageView.setBackgroundColor(backgroundColor);
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    private int getIconResourceId(String iconName) {
        switch (iconName) {
            case "ic_home_black_24dp":
                return R.drawable.ic_home_black_24dp;
            case "ic_bar_chart":
                return R.drawable.ic_bar_chart;
            default:
                return R.drawable.ic_bar_chart;
        }
    }

    private int getCategoryColor(String category) {
        switch (category) {
            case "Ăn uống":
                return context.getColor(R.color.category_food);
            case "Di chuyển":
                return context.getColor(R.color.category_transport);
            case "Mua sắm":
                return context.getColor(R.color.category_shopping);
            case "Ngân sách":
                return context.getColor(R.color.category_income);
            case "Tiện ích":
                return context.getColor(R.color.category_utility);
            case "Giáo dục":
                return context.getColor(R.color.category_education);
            default:
                return context.getColor(R.color.category_default);
        }
    }

    public static class TransactionViewHolder extends RecyclerView.ViewHolder {
        ImageView iconImageView;
        TextView titleTextView;
        TextView categoryTextView;
        TextView dateTextView;
        TextView amountTextView;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            iconImageView = itemView.findViewById(R.id.transaction_icon);
            titleTextView = itemView.findViewById(R.id.transaction_title);
            categoryTextView = itemView.findViewById(R.id.transaction_category);
            dateTextView = itemView.findViewById(R.id.transaction_date);
            amountTextView = itemView.findViewById(R.id.transaction_amount);
        }
    }
}