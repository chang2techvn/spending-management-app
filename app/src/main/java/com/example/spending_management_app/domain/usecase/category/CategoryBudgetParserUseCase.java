package com.example.spending_management_app.domain.usecase.category;

import com.example.spending_management_app.utils.BudgetAmountParser;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for parsing category budget operations from text
 */
public class CategoryBudgetParserUseCase {

    /**
     * Helper class for category budget operations
     */
    public static class CategoryBudgetOperation {
        public String type; // "add", "edit", "delete"
        public String category;
        public long amount;

        public CategoryBudgetOperation(String type, String category, long amount) {
            this.type = type;
            this.category = category;
            this.amount = amount;
        }
    }

    /**
     * Parse multiple category budget operations from text
     */
    public static List<CategoryBudgetOperation> parseMultipleCategoryOperations(String text) {
        List<CategoryBudgetOperation> operations = new ArrayList<>();
        String lowerText = text.toLowerCase();

        // Check if user wants to delete ALL category budgets
        if ((lowerText.contains("xóa") || lowerText.contains("xoá") ||
             lowerText.contains("thiết lập lại") || lowerText.contains("đặt lại") ||
             lowerText.contains("reset")) &&
            (lowerText.contains("tất cả") || lowerText.contains("hết"))) {

            // Special operation: delete all categories
            operations.add(new CategoryBudgetOperation("delete_all", "ALL", 0));
            return operations;
        }

        // Check if user wants to delete ALL category budgets (English)
        if ((lowerText.contains("delete") || lowerText.contains("remove") ||
             lowerText.contains("clear") || lowerText.contains("reset") ||
             lowerText.contains("erase")) &&
            (lowerText.contains("all") || lowerText.contains("everything") ||
             lowerText.contains("entire"))) {

            // Special operation: delete all categories
            operations.add(new CategoryBudgetOperation("delete_all", "ALL", 0));
            return operations;
        }

        // Determine operation type
        String operationType = "edit"; // default
        if (lowerText.contains("xóa") || lowerText.contains("xoá")) {
            operationType = "delete";
        } else if (lowerText.contains("thêm")) {
            operationType = "add";
        } else if (lowerText.contains("sửa") || lowerText.contains("thay đổi")) {
            operationType = "edit";
        }

        // Determine operation type (English)
        if (lowerText.contains("delete") || lowerText.contains("remove")) {
            operationType = "delete";
        } else if (lowerText.contains("add") || lowerText.contains("set")) {
            operationType = "add";
        } else if (lowerText.contains("edit") || lowerText.contains("change") ||
                   lowerText.contains("update") || lowerText.contains("modify")) {
            operationType = "edit";
        }

        // List of all categories with their aliases (shortened names)
        java.util.Map<String, String> categoryAliases = new java.util.HashMap<>();

        // Full category names (Vietnamese + English)
        String[] allCategories = {
            // Vietnamese names
            "Ăn uống", "Di chuyển", "Tiện ích", "Y tế", "Nhà ở",
            "Mua sắm", "Giáo dục", "Sách & Học tập", "Thể thao", "Sức khỏe & Làm đẹp",
            "Giải trí", "Du lịch", "Ăn ngoài & Cafe", "Quà tặng & Từ thiện", "Hội họp & Tiệc tụng",
            "Điện thoại & Internet", "Đăng ký & Dịch vụ", "Phần mềm & Apps", "Ngân hàng & Phí",
            "Con cái", "Thú cưng", "Gia đình",
            "Lương", "Đầu tư", "Thu nhập phụ", "Tiết kiệm",
            "Khác",
            // English names
            "Food", "Transport", "Utilities", "Healthcare", "Housing",
            "Shopping", "Education", "Books & Learning", "Sports", "Beauty & Health",
            "Entertainment", "Travel", "Cafe & Dining Out", "Gifts & Charity", "Events & Parties",
            "Phone & Internet", "Services & Subscriptions", "Software & Apps", "Banking & Fees",
            "Children", "Pets", "Family",
            "Salary", "Investment", "Side Income", "Savings",
            "Other", "Budget"
        };

        // Add aliases for categories with "&" (accept first part only) - Vietnamese
        categoryAliases.put("sức khỏe", "Sức khỏe & Làm đẹp");
        categoryAliases.put("làm đẹp", "Sức khỏe & Làm đẹp");
        categoryAliases.put("ăn ngoài", "Ăn ngoài & Cafe");
        categoryAliases.put("cafe", "Ăn ngoài & Cafe");
        categoryAliases.put("cà phê", "Ăn ngoài & Cafe");
        categoryAliases.put("quà tặng", "Quà tặng & Từ thiện");
        categoryAliases.put("từ thiện", "Quà tặng & Từ thiện");
        categoryAliases.put("hội họp", "Hội họp & Tiệc tụng");
        categoryAliases.put("tiệc tụng", "Hội họp & Tiệc tụng");
        categoryAliases.put("điện thoại", "Điện thoại & Internet");
        categoryAliases.put("internet", "Điện thoại & Internet");
        categoryAliases.put("đăng ký", "Đăng ký & Dịch vụ");
        categoryAliases.put("dịch vụ", "Đăng ký & Dịch vụ");
        categoryAliases.put("phần mềm", "Phần mềm & Apps");
        categoryAliases.put("apps", "Phần mềm & Apps");
        categoryAliases.put("ngân hàng", "Ngân hàng & Phí");
        categoryAliases.put("phí", "Ngân hàng & Phí");
        categoryAliases.put("sách", "Sách & Học tập");
        categoryAliases.put("học tập", "Sách & Học tập");

        // Add aliases for categories with "&" (accept first part only) - English
        categoryAliases.put("beauty", "Beauty & Health");
        categoryAliases.put("health", "Beauty & Health");
        categoryAliases.put("dining", "Cafe & Dining Out");
        categoryAliases.put("restaurant", "Cafe & Dining Out");
        categoryAliases.put("coffee", "Cafe & Dining Out");
        categoryAliases.put("gifts", "Gifts & Charity");
        categoryAliases.put("charity", "Gifts & Charity");
        categoryAliases.put("events", "Events & Parties");
        categoryAliases.put("parties", "Events & Parties");
        categoryAliases.put("phone", "Phone & Internet");
        categoryAliases.put("internet", "Phone & Internet");
        categoryAliases.put("services", "Services & Subscriptions");
        categoryAliases.put("subscriptions", "Services & Subscriptions");
        categoryAliases.put("software", "Software & Apps");
        categoryAliases.put("apps", "Software & Apps");
        categoryAliases.put("banking", "Banking & Fees");
        categoryAliases.put("fees", "Banking & Fees");
        categoryAliases.put("books", "Books & Learning");
        categoryAliases.put("learning", "Books & Learning");

        // Parse text more carefully by looking for explicit "category + amount" pairs
        // Split text by common separators
        String[] segments = lowerText.split("[,;]");

        for (String segment : segments) {
            segment = segment.trim();
            if (segment.isEmpty()) continue;

            // Try to find a category in this segment
            String matchedCategory = null;
            int matchedLength = 0;

            // First, try to match full category names (prefer longer matches)
            for (String category : allCategories) {
                String categoryLower = category.toLowerCase();

                // Check if this segment contains this category
                if (segment.contains(categoryLower)) {
                    // Prefer longer matches (e.g., "Đăng ký & Dịch vụ" over "Dịch vụ")
                    if (matchedCategory == null || categoryLower.length() > matchedLength) {
                        // Verify this is a standalone mention, not part of another word
                        int pos = segment.indexOf(categoryLower);
                        boolean validStart = (pos == 0 || !Character.isLetterOrDigit(segment.charAt(pos - 1)));
                        boolean validEnd = (pos + categoryLower.length() >= segment.length() ||
                                          !Character.isLetterOrDigit(segment.charAt(pos + categoryLower.length())));

                        if (validStart && validEnd) {
                            matchedCategory = category;
                            matchedLength = categoryLower.length();
                        }
                    }
                }
            }

            // If no full match, try aliases
            if (matchedCategory == null) {
                for (java.util.Map.Entry<String, String> alias : categoryAliases.entrySet()) {
                    String aliasKey = alias.getKey();

                    if (segment.contains(aliasKey)) {
                        // Verify this is a standalone mention
                        int pos = segment.indexOf(aliasKey);
                        boolean validStart = (pos == 0 || !Character.isLetterOrDigit(segment.charAt(pos - 1)));
                        boolean validEnd = (pos + aliasKey.length() >= segment.length() ||
                                          !Character.isLetterOrDigit(segment.charAt(pos + aliasKey.length())));

                        if (validStart && validEnd) {
                            matchedCategory = alias.getValue();
                            matchedLength = aliasKey.length();
                        }
                    }
                }
            }

            if (matchedCategory != null) {
                long amount = 0;

                if (!operationType.equals("delete")) {
                    // Extract amount from this segment only
                    amount = BudgetAmountParser.extractBudgetAmount(segment);

                    if (amount <= 0) {
                        continue; // Skip if no valid amount found for add/edit
                    }
                }

                operations.add(new CategoryBudgetOperation(operationType, matchedCategory, amount));
            }
        }

        return operations;
    }
}