# Hướng dẫn hoàn thiện tính năng Multi-User

## ✅ Đã hoàn thành

### 1. Database Entities - Thêm userId field
- ✅ BudgetEntity.java - Added userId field, getter/setter
- ✅ CategoryBudgetEntity.java - Added userId field, getter/setter
- ✅ TransactionEntity.java - Added userId field, getter/setter
- ✅ BudgetHistoryEntity.java - Added userId field, getter/setter
- ✅ RecurringExpenseEntity.java - Added userId field, getter/setter

### 2. DAO Interfaces - Thêm userId vào queries
- ✅ BudgetDao.java - Updated all queries with userId parameter
- ✅ CategoryBudgetDao.java - Updated all queries with userId parameter
- ✅ TransactionDao.java - Updated all queries with userId parameter
- ✅ BudgetHistoryDao.java - Updated all queries with userId parameter
- ✅ RecurringExpenseDao.java - Updated all queries with userId parameter

### 3. Database Version
- ✅ AppDatabase.java - Updated version từ 5 → 6

### 4. UserSession Manager
- ✅ UserSession.java - Created new utility class để quản lý session

### 5. Repository Interfaces và Implementations
- ✅ BudgetRepository.java & BudgetRepositoryImpl.java
- ✅ CategoryBudgetRepository.java & CategoryBudgetRepositoryImpl.java
- ✅ ExpenseRepository.java & ExpenseRepositoryImpl.java

## 🔄 Cần làm tiếp

### 1. Cập nhật Use Cases
Cần thêm userId parameter và lấy từ UserSession trong các file sau:

#### Budget Related
- `BudgetUseCase.java` - Cần inject UserSession, lấy userId và pass vào repository calls
  ```java
  private UserSession userSession;
  int userId = userSession.getCurrentUserId();
  budgetRepository.getBudgetsByDateRange(userId, startDate, endDate);
  ```

#### Category Budget Related
- `CategoryBudgetUseCase.java` - Update để sử dụng userId
- `CategoryBudgetParserUseCase.java` - Update nếu có query database

#### Expense Related
- `ExpenseUseCase.java` - Update tất cả calls với userId
- `ExpenseBulkUseCase.java` - Update bulk operations

#### AI và Context
- `AiContextUseCase.java` - Update để lọc data theo userId
- `RequestRouterUseCase.java` - Có thể cần userId context

#### User Related
- `UserUseCase.java` - Có thể cần update login/logout logic

#### Common
- `WelcomeMessageUseCase.java` - Update nếu cần user-specific data

### 2. Cập nhật BudgetHistoryLogger
File này tạo budget history records, cần set userId:
```java
BudgetHistoryEntity history = new BudgetHistoryEntity(...);
history.setUserId(userSession.getCurrentUserId());
```

### 3. Cập nhật UI Fragments và Activities
Tất cả fragments/activities cần inject UserSession:

#### Main Fragments
- `HomeFragment.java` - Update tất cả database queries
- `HistoryFragment.java` - Filter by userId
- `StatisticsFragment.java` - Filter by userId
- `SettingsFragment.java` - Show current user info

#### Dialog và Adapter
- Các dialog tạo/edit budget/expense cần set userId
- Các adapter hiển thị data cần filter theo userId

### 4. Xử lý Entity khi tạo mới
Mỗi khi tạo entity mới, cần set userId:
```java
BudgetEntity budget = new BudgetEntity(...);
budget.setUserId(userSession.getCurrentUserId());
```

### 5. Testing và Validation
- Test với nhiều user khác nhau
- Verify data isolation giữa các users
- Test login/logout workflow
- Test database migration từ version 5 → 6

## 📝 Code Pattern để Follow

### 1. Inject UserSession vào UseCase
```java
public class BudgetUseCase {
    private final BudgetRepository budgetRepository;
    private final UserSession userSession;
    
    public BudgetUseCase(BudgetRepository budgetRepository, Context context) {
        this.budgetRepository = budgetRepository;
        this.userSession = UserSession.getInstance(context);
    }
    
    public void someBudgetOperation() {
        int userId = userSession.getCurrentUserId();
        // Use userId in all repository calls
        budgetRepository.getBudgetsByDateRange(userId, startDate, endDate);
    }
}
```

### 2. Set userId khi tạo Entity
```java
TransactionEntity transaction = new TransactionEntity(desc, category, amount, date, type);
transaction.setUserId(userSession.getCurrentUserId());
repository.insert(transaction);
```

### 3. Lấy userId trong Fragment
```java
public class HomeFragment extends Fragment {
    private UserSession userSession;
    
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        userSession = UserSession.getInstance(requireContext());
        loadData();
    }
    
    private void loadData() {
        int userId = userSession.getCurrentUserId();
        // Use userId to fetch data
    }
}
```

## ⚠️ Lưu ý quan trọng

1. **Database Migration**: Database đang dùng `fallbackToDestructiveMigration()` nên sẽ xóa data cũ. Nếu muốn giữ data, cần implement proper migration.

2. **Default User**: Tất cả entities đều có default userId = 1 trong constructor. Data hiện tại sẽ thuộc về user với id = 1.

3. **UserSession**: Session được lưu trong SharedPreferences, persist qua app restarts.

4. **Login/Logout**: Hiện tại chưa có UI cho login/logout. Cần implement:
   - Login screen
   - User registration
   - Logout functionality
   - Remember me feature

5. **Compile Errors**: Sau khi update DAOs, sẽ có nhiều compile errors ở UseCases và Fragments vì signature của methods đã thay đổi. Cần fix từng file một.

## 🚀 Bước tiếp theo đề xuất

1. Fix compile errors trong UseCases (ưu tiên: BudgetUseCase, CategoryBudgetUseCase, ExpenseUseCase)
2. Fix compile errors trong Fragments (ưu tiên: HomeFragment, HistoryFragment)
3. Test app với single user (userId = 1)
4. Implement Login/Logout UI
5. Test với multiple users
6. Add proper database migration nếu cần preserve data

## 📊 Progress Tracker

- [x] Entities: 5/5 (100%)
- [x] DAOs: 5/5 (100%)
- [x] Database: 1/1 (100%)
- [x] UserSession: 1/1 (100%)
- [x] Repositories: 3/3 (100%)
- [ ] UseCases: 0/10 (0%)
- [ ] UI/Fragments: 0/20+ (0%)
- [ ] Testing: 0% 
- [ ] Login/Logout UI: 0%

**Tổng Progress: ~40%**
