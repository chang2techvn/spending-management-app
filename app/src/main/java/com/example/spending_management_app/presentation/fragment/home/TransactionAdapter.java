package com.example.spending_management_app.presentation.fragment.home;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.spending_management_app.R;
import com.example.spending_management_app.utils.CategoryUtils;
import com.example.spending_management_app.domain.model.Transaction;

import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    private List<Transaction> transactions;
    private Context context;

    public TransactionAdapter(List<Transaction> transactions) {
        this.transactions = transactions;
    }
    
    public void updateTransactions(List<Transaction> newTransactions) {
        this.transactions.clear();
        this.transactions.addAll(newTransactions);
        notifyDataSetChanged();
        android.util.Log.d("TransactionAdapter", "Updated with " + newTransactions.size() + " transactions");
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
        holder.categoryTextView.setText(CategoryUtils.getLocalizedCategoryName(context, transaction.getCategory()));
        holder.dateTextView.setText(transaction.getFormattedDate());
        holder.amountTextView.setText(transaction.getFormattedAmount());

        // Set amount color based on type
        if (transaction.getAmount() >= 0) {
            holder.amountTextView.setTextColor(context.getColor(R.color.income_color));
        } else {
            holder.amountTextView.setTextColor(context.getColor(R.color.expense_color));
        }

        // Set icon emoji based on category
        String iconEmoji = CategoryUtils.getIconForCategory(CategoryUtils.getLocalizedCategoryName(context, transaction.getCategory()));
        holder.iconTextView.setText(iconEmoji);

        // Set icon background color based on category
        int backgroundColor = context.getColor(CategoryUtils.getColorForCategory(CategoryUtils.getLocalizedCategoryName(context, transaction.getCategory())));
        holder.iconTextView.setBackgroundColor(backgroundColor);
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    public static class TransactionViewHolder extends RecyclerView.ViewHolder {
        TextView iconTextView;
        TextView titleTextView;
        TextView categoryTextView;
        TextView dateTextView;
        TextView amountTextView;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            iconTextView = itemView.findViewById(R.id.transaction_icon);
            titleTextView = itemView.findViewById(R.id.transaction_title);
            categoryTextView = itemView.findViewById(R.id.transaction_category);
            dateTextView = itemView.findViewById(R.id.transaction_date);
            amountTextView = itemView.findViewById(R.id.transaction_amount);
        }
    }
}