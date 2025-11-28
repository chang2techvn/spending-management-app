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

public class TransactionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_TRANSACTION = 0;
    private static final int VIEW_TYPE_SKELETON = 1;

    private List<Transaction> transactions;
    private Context context;
    private boolean isLoading = false;

    public TransactionAdapter(List<Transaction> transactions) {
        this.transactions = transactions;
    }

    public void setLoading(boolean loading) {
        this.isLoading = loading;
        notifyDataSetChanged();
    }

    public void updateTransactions(List<Transaction> newTransactions) {
        this.transactions.clear();
        this.transactions.addAll(newTransactions);
        this.isLoading = false;
        notifyDataSetChanged();
        android.util.Log.d("TransactionAdapter", "Updated with " + newTransactions.size() + " transactions");
    }

    @Override
    public int getItemViewType(int position) {
        return isLoading ? VIEW_TYPE_SKELETON : VIEW_TYPE_TRANSACTION;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        if (viewType == VIEW_TYPE_SKELETON) {
            View view = LayoutInflater.from(context).inflate(R.layout.skeleton_item_transaction, parent, false);
            return new SkeletonViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_transaction, parent, false);
            return new TransactionViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof TransactionViewHolder && !isLoading && position < transactions.size()) {
            Transaction transaction = transactions.get(position);

            TransactionViewHolder transactionHolder = (TransactionViewHolder) holder;
            transactionHolder.titleTextView.setText(transaction.getDescription());
            transactionHolder.categoryTextView.setText(CategoryUtils.getLocalizedCategoryName(context, transaction.getCategory()));
            transactionHolder.dateTextView.setText(transaction.getFormattedDate());
            transactionHolder.amountTextView.setText(transaction.getFormattedAmount(context));

            // Set amount color based on type
            if (transaction.getAmount() >= 0) {
                transactionHolder.amountTextView.setTextColor(context.getColor(R.color.income_color));
            } else {
                transactionHolder.amountTextView.setTextColor(context.getColor(R.color.expense_color));
            }

            // Set icon emoji based on category
            String iconEmoji = CategoryUtils.getIconForCategory(CategoryUtils.getLocalizedCategoryName(context, transaction.getCategory()));
            transactionHolder.iconTextView.setText(iconEmoji);

            // Set icon background color based on category
            int backgroundColor = context.getColor(CategoryUtils.getColorForCategory(CategoryUtils.getLocalizedCategoryName(context, transaction.getCategory())));
            transactionHolder.iconTextView.setBackgroundColor(backgroundColor);
        }
        // Skeleton view holder doesn't need binding
    }

    @Override
    public int getItemCount() {
        return isLoading ? 5 : transactions.size(); // Show 5 skeleton items when loading
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

    public static class SkeletonViewHolder extends RecyclerView.ViewHolder {
        public SkeletonViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}