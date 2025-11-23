package com.example.spending_management_app.presentation.fragment.history;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.spending_management_app.R;
import com.example.spending_management_app.domain.model.Transaction;
import com.example.spending_management_app.utils.CategoryUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SectionedTransactionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_ITEM = 1;

    private List<Object> items;
    private Context context;

    public SectionedTransactionAdapter(List<Transaction> transactions) {
        this.items = groupTransactionsByDate(transactions);
    }

    private List<Object> groupTransactionsByDate(List<Transaction> transactions) {
        List<Object> groupedItems = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        // Sort transactions by date (newest first)
        transactions.sort((t1, t2) -> t2.getDate().compareTo(t1.getDate()));

        String currentDate = "";
        for (Transaction transaction : transactions) {
            String transactionDate = dateFormat.format(transaction.getDate());

            if (!transactionDate.equals(currentDate)) {
                // Add header for new date
                groupedItems.add(getDateHeader(transaction.getDate()));
                currentDate = transactionDate;
            }

            // Add transaction item
            groupedItems.add(transaction);
        }

        return groupedItems;
    }

    private String getDateHeader(Date date) {
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        Calendar yesterday = (Calendar) today.clone();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);

        Calendar transactionDate = Calendar.getInstance();
        transactionDate.setTime(date);
        transactionDate.set(Calendar.HOUR_OF_DAY, 0);
        transactionDate.set(Calendar.MINUTE, 0);
        transactionDate.set(Calendar.SECOND, 0);
        transactionDate.set(Calendar.MILLISECOND, 0);

        if (transactionDate.equals(today)) {
            return "Hôm nay";
        } else if (transactionDate.equals(yesterday)) {
            return "Hôm qua";
        } else {
            SimpleDateFormat headerFormat = new SimpleDateFormat("EEEE, dd/MM/yyyy", new Locale("vi", "VN"));
            return headerFormat.format(date);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (items.get(position) instanceof String) {
            return VIEW_TYPE_HEADER;
        } else {
            return VIEW_TYPE_ITEM;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        if (viewType == VIEW_TYPE_HEADER) {
            View view = inflater.inflate(R.layout.item_date_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_transaction, parent, false);
            return new TransactionViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            String headerText = (String) items.get(position);
            ((HeaderViewHolder) holder).bind(headerText);
        } else if (holder instanceof TransactionViewHolder) {
            Transaction transaction = (Transaction) items.get(position);
            ((TransactionViewHolder) holder).bind(transaction);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void updateTransactions(List<Transaction> transactions) {
        this.items = groupTransactionsByDate(transactions);
        notifyDataSetChanged();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView headerTextView;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            headerTextView = itemView.findViewById(R.id.date_header_text);
        }

        public void bind(String headerText) {
            headerTextView.setText(headerText);
        }
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
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

        public void bind(Transaction transaction) {
            titleTextView.setText(transaction.getDescription());
            // Display localized category name
            categoryTextView.setText(CategoryUtils.getLocalizedCategoryName(itemView.getContext(), transaction.getCategory()));
            dateTextView.setText(transaction.getFormattedDate());
            amountTextView.setText(transaction.getFormattedAmount());

            // Set amount color based on type
            if (transaction.getAmount() >= 0) {
                amountTextView.setTextColor(itemView.getContext().getColor(R.color.income_color));
            } else {
                amountTextView.setTextColor(itemView.getContext().getColor(R.color.expense_color));
            }

            // Set icon emoji based on category
            String iconEmoji = getIconEmoji(transaction.getCategory());
            iconTextView.setText(iconEmoji);

            // Set icon background color based on category
            int backgroundColor = getCategoryColor(transaction.getCategory());
            iconTextView.setBackgroundColor(backgroundColor);
        }

        private String getIconEmoji(String category) {
            String localized = CategoryUtils.getLocalizedCategoryName(itemView.getContext(), category);
            return CategoryUtils.getIconForCategory(localized);
        }

        private int getCategoryColor(String category) {
            Context ctx = itemView.getContext();
            String localized = CategoryUtils.getLocalizedCategoryName(ctx, category);
            int colorRes = CategoryUtils.getColorForCategory(localized);
            return ctx.getColor(colorRes);
        }

        private String getLocalizedCategoryName(String category) {
            return CategoryUtils.getLocalizedCategoryName(itemView.getContext(), category);
        }
    }
}