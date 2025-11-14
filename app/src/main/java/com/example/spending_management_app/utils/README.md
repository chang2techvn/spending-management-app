# Utils Package

Thư mục này chứa các helper classes để tổ chức code tốt hơn và tái sử dụng.

## CategoryHelper.java

Helper class quản lý danh mục chi tiêu và emoji icons.

**Các method:**
- `getEmojiForCategory(String category)`: Trả về emoji tương ứng với danh mục
- `getCategoriesDescription()`: Trả về mô tả đầy đủ các danh mục có sẵn
- `getCategoryRules()`: Trả về quy tắc phân loại chi tiêu

**Danh mục được quản lý:**
- Nhu cầu thiết yếu: Ăn uống, Di chuyển, Tiện ích, Y tế, Nhà ở
- Mua sắm & Phát triển: Mua sắm, Giáo dục, Sách & Học tập, Thể thao, Sức khỏe & Làm đẹp
- Giải trí & Xã hội: Giải trí, Du lịch, Ăn ngoài & Cafe, Quà tặng & Từ thiện, Hội họp & Tiệc tụng
- Công nghệ & Dịch vụ: Điện thoại & Internet, Đăng ký & Dịch vụ, Phần mềm & Apps, Ngân hàng & Phí
- Gia đình: Con cái, Thú cưng, Gia đình
- Thu nhập: Lương, Đầu tư, Thu nhập phụ, Tiết kiệm
- Khác: Danh mục khác

## AiSystemInstructions.java

Helper class quản lý các system instructions cho AI.

**Các method:**
- `getExpenseTrackingInstruction(...)`: Trả về instruction cho tính năng ghi chi tiêu
- `getFinancialAnalysisInstruction(...)`: Trả về instruction cho tính năng phân tích tài chính

**Lợi ích:**
- Dễ dàng cập nhật instructions ở một nơi duy nhất
- Tái sử dụng logic cho nhiều features
- Code dễ đọc và bảo trì hơn

## Cách sử dụng

```java
// Trong AiChatBottomSheet.java hoặc các class khác

// Lấy emoji cho danh mục
String emoji = CategoryHelper.getEmojiForCategory("Ăn uống"); // Returns "🍽️"

// Lấy system instruction cho expense tracking
String instruction = AiSystemInstructions.getExpenseTrackingInstruction(
    currentDateInfo, 
    currentDay, currentMonth, currentYear,
    yesterdayDay, yesterdayMonth, yesterdayYear
);

// Lấy system instruction cho financial analysis
String analysisInstruction = AiSystemInstructions.getFinancialAnalysisInstruction(
    currentDateInfo, 
    financialContext
);
```
