package com.example.spending_management_app.ui.history;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.spending_management_app.R;
import com.example.spending_management_app.ui.home.Transaction;

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
            categoryTextView.setText(transaction.getCategory());
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
            switch (category) {
                // Nhu cầu thiết yếu
                case "Ăn uống":
                    return "🍽️";
                case "Di chuyển":
                    return "🚗";
                case "Tiện ích":
                    return "⚡";
                case "Y tế":
                    return "🏥";
                case "Nhà ở":
                    return "🏠";
                
                // Mua sắm & Phát triển bản thân
                case "Mua sắm":
                    return "🛍️";
                case "Giáo dục":
                    return "📚";
                case "Sách & Học tập":
                    return "📖";
                case "Thể thao":
                    return "⚽";
                case "Sức khỏe & Làm đẹp":
                    return "💆";
                
                // Giải trí & Xã hội
                case "Giải trí":
                    return "🎬";
                case "Du lịch":
                    return "✈️";
                case "Ăn ngoài & Cafe":
                    return "☕";
                case "Quà tặng & Từ thiện":
                    return "🎁";
                case "Hội họp & Tiệc tụng":
                    return "🎉";
                
                // Công nghệ & Dịch vụ
                case "Điện thoại & Internet":
                    return "📱";
                case "Đăng ký & Dịch vụ":
                    return "💳";
                case "Phần mềm & Apps":
                    return "💻";
                case "Ngân hàng & Phí":
                    return "🏦";
                
                // Gia đình & Con cái
                case "Con cái":
                    return "👶";
                case "Thú cưng":
                    return "🐕";
                case "Gia đình":
                    return "👨‍👩‍👧‍👦";
                
                // Thu nhập & Tài chính
                case "Lương":
                    return "💰";
                case "Đầu tư":
                    return "📈";
                case "Thu nhập phụ":
                    return "💵";
                case "Tiết kiệm":
                    return "🏦";
                
                // Khác
                case "Khác":
                    return "�";
                default:
                    return "💳";
            }
        }

        private int getCategoryColor(String category) {
            Context context = itemView.getContext();
            switch (category) {
                // Nhu cầu thiết yếu
                case "Ăn uống":
                    return context.getColor(R.color.category_food);
                case "Di chuyển":
                    return context.getColor(R.color.category_transport);
                case "Tiện ích":
                    return context.getColor(R.color.category_utility);
                case "Y tế":
                    return context.getColor(R.color.category_health);
                case "Nhà ở":
                    return context.getColor(R.color.category_housing);
                
                // Mua sắm & Phát triển bản thân
                case "Mua sắm":
                    return context.getColor(R.color.category_shopping);
                case "Giáo dục":
                case "Sách & Học tập":
                    return context.getColor(R.color.category_education);
                case "Thể thao":
                case "Sức khỏe & Làm đẹp":
                    return context.getColor(R.color.category_fitness);
                
                // Giải trí & Xã hội
                case "Giải trí":
                case "Du lịch":
                    return context.getColor(R.color.category_entertainment);
                case "Ăn ngoài & Cafe":
                    return context.getColor(R.color.category_cafe);
                case "Quà tặng & Từ thiện":
                case "Hội họp & Tiệc tụng":
                    return context.getColor(R.color.category_gift);
                
                // Công nghệ & Dịch vụ
                case "Điện thoại & Internet":
                case "Phần mềm & Apps":
                    return context.getColor(R.color.category_tech);
                case "Đăng ký & Dịch vụ":
                case "Ngân hàng & Phí":
                    return context.getColor(R.color.category_service);
                
                // Gia đình & Con cái
                case "Con cái":
                case "Thú cưng":
                case "Gia đình":
                    return context.getColor(R.color.category_family);
                
                // Thu nhập & Tài chính
                case "Lương":
                case "Đầu tư":
                case "Thu nhập phụ":
                case "Tiết kiệm":
                    return context.getColor(R.color.category_income);
                
                // Khác
                case "Khác":
                default:
                    return context.getColor(R.color.category_default);
            }
        }
    }
}