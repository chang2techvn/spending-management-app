package com.example.spending_management_app.presentation.fragment.home;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.spending_management_app.R;
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
        holder.categoryTextView.setText(transaction.getCategory());
        holder.dateTextView.setText(transaction.getFormattedDate());
        holder.amountTextView.setText(transaction.getFormattedAmount());

        // Set amount color based on type
        if (transaction.getAmount() >= 0) {
            holder.amountTextView.setTextColor(context.getColor(R.color.income_color));
        } else {
            holder.amountTextView.setTextColor(context.getColor(R.color.expense_color));
        }

        // Set icon emoji based on category
        String iconEmoji = getIconEmoji(transaction.getCategory());
        holder.iconTextView.setText(iconEmoji);

        // Set icon background color based on category
        int backgroundColor = getCategoryColor(transaction.getCategory());
        holder.iconTextView.setBackgroundColor(backgroundColor);
    }

    @Override
    public int getItemCount() {
        return transactions.size();
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