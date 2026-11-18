package com.example.finsight.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * Demo data seeder for user presentations.
 * Generates realistic income, recurring bills, expenses, and monthly category budgets.
 */
class DemoDataSeeder(private val db: AppDatabase) {

    suspend fun seed(userId: Long) = withContext(Dispatchers.IO) {
        val txDao = db.transactionDao()
        val budgetDao = db.budgetDao()

        // 1. Delete user's existing data
        txDao.deleteUserTransactions(userId)
        budgetDao.deleteUserBudgets(userId)

        // 2. Insert Budgets
        val budgets = listOf(
            Budget(userId = userId, category = "Food", monthlyLimit = 4000.0),
            Budget(userId = userId, category = "Shopping", monthlyLimit = 5500.0),
            Budget(userId = userId, category = "Travel", monthlyLimit = 3000.0),
            Budget(userId = userId, category = "Bills", monthlyLimit = 2000.0),
            Budget(userId = userId, category = "Entertainment", monthlyLimit = 2000.0),
            Budget(userId = userId, category = "Health", monthlyLimit = 1500.0)
        )

        for (budget in budgets) {
            budgetDao.insertOrReplace(budget)
        }

        // 3. Build Transactions for last 6 months (offsets 0 to -5)
        val transactions = mutableListOf<Transaction>()

        for (monthOffset in 0 downTo -5) {
            // Salary on day 1
            transactions.add(
                Transaction(
                    userId = userId,
                    title = "Monthly allowance",
                    amount = 25000.0,
                    isExpense = false,
                    category = "Salary",
                    paymentMethod = "UPI",
                    date = getDateForMonthOffset(monthOffset, 1)
                )
            )

            // Recurring Expenses
            transactions.add(
                Transaction(
                    userId = userId,
                    title = "Netflix",
                    amount = 499.0,
                    isExpense = true,
                    category = "Entertainment",
                    paymentMethod = "Card",
                    date = getDateForMonthOffset(monthOffset, 3),
                    isRecurring = true
                )
            )
            transactions.add(
                Transaction(
                    userId = userId,
                    title = "Spotify",
                    amount = 119.0,
                    isExpense = true,
                    category = "Entertainment",
                    paymentMethod = "UPI",
                    date = getDateForMonthOffset(monthOffset, 4),
                    isRecurring = true
                )
            )
            transactions.add(
                Transaction(
                    userId = userId,
                    title = "Wi-Fi bill",
                    amount = 699.0,
                    isExpense = true,
                    category = "Bills",
                    paymentMethod = "UPI",
                    date = getDateForMonthOffset(monthOffset, 5),
                    isRecurring = true
                )
            )
            transactions.add(
                Transaction(
                    userId = userId,
                    title = "Mobile recharge",
                    amount = 399.0,
                    isExpense = true,
                    category = "Bills",
                    paymentMethod = "UPI",
                    date = getDateForMonthOffset(monthOffset, 6),
                    isRecurring = true
                )
            )
            transactions.add(
                Transaction(
                    userId = userId,
                    title = "Gym membership",
                    amount = 999.0,
                    isExpense = true,
                    category = "Health",
                    paymentMethod = "Card",
                    date = getDateForMonthOffset(monthOffset, 8),
                    isRecurring = true
                )
            )
        }

        // Extra Income: Freelance project in month -2 (day 12) and month 0 (day 5)
        transactions.add(
            Transaction(
                userId = userId,
                title = "Freelance project",
                amount = 4000.0,
                isExpense = false,
                category = "Salary",
                paymentMethod = "UPI",
                date = getDateForMonthOffset(-2, 12)
            )
        )
        transactions.add(
            Transaction(
                userId = userId,
                title = "Freelance project",
                amount = 3500.0,
                isExpense = false,
                category = "Salary",
                paymentMethod = "UPI",
                date = getDateForMonthOffset(0, 5)
            )
        )

        // Month 0 (Current month) non-recurring
        transactions.add(Transaction(userId = userId, title = "Groceries", amount = 1450.0, isExpense = true, category = "Food", paymentMethod = "UPI", date = getDateForMonthOffset(0, 2)))
        transactions.add(Transaction(userId = userId, title = "Swiggy dinner", amount = 640.0, isExpense = true, category = "Food", paymentMethod = "UPI", date = getDateForMonthOffset(0, 6)))
        transactions.add(Transaction(userId = userId, title = "Cafe", amount = 380.0, isExpense = true, category = "Food", paymentMethod = "Cash", date = getDateForMonthOffset(0, 9)))
        transactions.add(Transaction(userId = userId, title = "Zomato", amount = 520.0, isExpense = true, category = "Food", paymentMethod = "UPI", date = getDateForMonthOffset(0, 14)))
        transactions.add(Transaction(userId = userId, title = "Weekend lunch", amount = 610.0, isExpense = true, category = "Food", paymentMethod = "Card", date = getDateForMonthOffset(0, 18)))
        transactions.add(Transaction(userId = userId, title = "Snacks", amount = 600.0, isExpense = true, category = "Food", paymentMethod = "Cash", date = getDateForMonthOffset(0, 22)))

        transactions.add(Transaction(userId = userId, title = "Headphones", amount = 2499.0, isExpense = true, category = "Shopping", paymentMethod = "Card", date = getDateForMonthOffset(0, 7)))
        transactions.add(Transaction(userId = userId, title = "T-shirts", amount = 1299.0, isExpense = true, category = "Shopping", paymentMethod = "UPI", date = getDateForMonthOffset(0, 15)))
        transactions.add(Transaction(userId = userId, title = "Phone case", amount = 702.0, isExpense = true, category = "Shopping", paymentMethod = "UPI", date = getDateForMonthOffset(0, 21)))

        transactions.add(Transaction(userId = userId, title = "Metro card", amount = 500.0, isExpense = true, category = "Travel", paymentMethod = "UPI", date = getDateForMonthOffset(0, 3)))
        transactions.add(Transaction(userId = userId, title = "Uber", amount = 450.0, isExpense = true, category = "Travel", paymentMethod = "Cash", date = getDateForMonthOffset(0, 11)))
        transactions.add(Transaction(userId = userId, title = "Train ticket home", amount = 850.0, isExpense = true, category = "Travel", paymentMethod = "Card", date = getDateForMonthOffset(0, 19)))

        transactions.add(Transaction(userId = userId, title = "Electricity bill", amount = 752.0, isExpense = true, category = "Bills", paymentMethod = "UPI", date = getDateForMonthOffset(0, 12)))
        transactions.add(Transaction(userId = userId, title = "Movie night", amount = 482.0, isExpense = true, category = "Entertainment", paymentMethod = "UPI", date = getDateForMonthOffset(0, 16)))

        // Month -1
        transactions.add(Transaction(userId = userId, title = "Groceries", amount = 1800.0, isExpense = true, category = "Food", paymentMethod = "UPI", date = getDateForMonthOffset(-1, 4)))
        transactions.add(Transaction(userId = userId, title = "Dining out", amount = 1300.0, isExpense = true, category = "Food", paymentMethod = "Card", date = getDateForMonthOffset(-1, 18)))
        transactions.add(Transaction(userId = userId, title = "Clothes", amount = 1500.0, isExpense = true, category = "Shopping", paymentMethod = "Card", date = getDateForMonthOffset(-1, 8)))
        transactions.add(Transaction(userId = userId, title = "Shoes", amount = 700.0, isExpense = true, category = "Shopping", paymentMethod = "UPI", date = getDateForMonthOffset(-1, 20)))
        transactions.add(Transaction(userId = userId, title = "Cab rides", amount = 1100.0, isExpense = true, category = "Travel", paymentMethod = "UPI", date = getDateForMonthOffset(-1, 5)))
        transactions.add(Transaction(userId = userId, title = "Train ticket", amount = 1500.0, isExpense = true, category = "Travel", paymentMethod = "Card", date = getDateForMonthOffset(-1, 15)))
        transactions.add(Transaction(userId = userId, title = "Electricity bill", amount = 752.0, isExpense = true, category = "Bills", paymentMethod = "UPI", date = getDateForMonthOffset(-1, 10)))
        transactions.add(Transaction(userId = userId, title = "Concert ticket", amount = 282.0, isExpense = true, category = "Entertainment", paymentMethod = "Card", date = getDateForMonthOffset(-1, 22)))

        // Month -2
        transactions.add(Transaction(userId = userId, title = "Supermarket", amount = 2100.0, isExpense = true, category = "Food", paymentMethod = "UPI", date = getDateForMonthOffset(-2, 7)))
        transactions.add(Transaction(userId = userId, title = "Restaurant", amount = 1200.0, isExpense = true, category = "Food", paymentMethod = "Card", date = getDateForMonthOffset(-2, 16)))
        transactions.add(Transaction(userId = userId, title = "Books", amount = 800.0, isExpense = true, category = "Shopping", paymentMethod = "Cash", date = getDateForMonthOffset(-2, 11)))
        transactions.add(Transaction(userId = userId, title = "Backpack", amount = 700.0, isExpense = true, category = "Shopping", paymentMethod = "UPI", date = getDateForMonthOffset(-2, 22)))
        transactions.add(Transaction(userId = userId, title = "Fuel", amount = 1400.0, isExpense = true, category = "Travel", paymentMethod = "Card", date = getDateForMonthOffset(-2, 6)))
        transactions.add(Transaction(userId = userId, title = "Taxi", amount = 1000.0, isExpense = true, category = "Travel", paymentMethod = "UPI", date = getDateForMonthOffset(-2, 19)))
        transactions.add(Transaction(userId = userId, title = "Electricity bill", amount = 752.0, isExpense = true, category = "Bills", paymentMethod = "UPI", date = getDateForMonthOffset(-2, 12)))
        transactions.add(Transaction(userId = userId, title = "Gaming pass", amount = 282.0, isExpense = true, category = "Entertainment", paymentMethod = "Card", date = getDateForMonthOffset(-2, 15)))

        // Month -3
        transactions.add(Transaction(userId = userId, title = "Groceries", amount = 1900.0, isExpense = true, category = "Food", paymentMethod = "UPI", date = getDateForMonthOffset(-3, 5)))
        transactions.add(Transaction(userId = userId, title = "Food delivery", amount = 1000.0, isExpense = true, category = "Food", paymentMethod = "Cash", date = getDateForMonthOffset(-3, 17)))
        transactions.add(Transaction(userId = userId, title = "Watch", amount = 2200.0, isExpense = true, category = "Shopping", paymentMethod = "Card", date = getDateForMonthOffset(-3, 9)))
        transactions.add(Transaction(userId = userId, title = "Stationery", amount = 1000.0, isExpense = true, category = "Shopping", paymentMethod = "UPI", date = getDateForMonthOffset(-3, 21)))
        transactions.add(Transaction(userId = userId, title = "Intercity bus", amount = 1800.0, isExpense = true, category = "Travel", paymentMethod = "Card", date = getDateForMonthOffset(-3, 12)))
        transactions.add(Transaction(userId = userId, title = "Auto fare", amount = 1200.0, isExpense = true, category = "Travel", paymentMethod = "Cash", date = getDateForMonthOffset(-3, 25)))
        transactions.add(Transaction(userId = userId, title = "Electricity bill", amount = 752.0, isExpense = true, category = "Bills", paymentMethod = "UPI", date = getDateForMonthOffset(-3, 10)))
        transactions.add(Transaction(userId = userId, title = "Amusement park", amount = 182.0, isExpense = true, category = "Entertainment", paymentMethod = "Card", date = getDateForMonthOffset(-3, 18)))

        // Month -4
        transactions.add(Transaction(userId = userId, title = "Groceries", amount = 2200.0, isExpense = true, category = "Food", paymentMethod = "UPI", date = getDateForMonthOffset(-4, 8)))
        transactions.add(Transaction(userId = userId, title = "Eating out", amount = 1200.0, isExpense = true, category = "Food", paymentMethod = "Card", date = getDateForMonthOffset(-4, 20)))
        transactions.add(Transaction(userId = userId, title = "Jacket", amount = 1900.0, isExpense = true, category = "Shopping", paymentMethod = "Card", date = getDateForMonthOffset(-4, 14)))
        transactions.add(Transaction(userId = userId, title = "Socks", amount = 600.0, isExpense = true, category = "Shopping", paymentMethod = "Cash", date = getDateForMonthOffset(-4, 26)))
        transactions.add(Transaction(userId = userId, title = "Metro recharge", amount = 800.0, isExpense = true, category = "Travel", paymentMethod = "UPI", date = getDateForMonthOffset(-4, 7)))
        transactions.add(Transaction(userId = userId, title = "Cab", amount = 700.0, isExpense = true, category = "Travel", paymentMethod = "Cash", date = getDateForMonthOffset(-4, 18)))
        transactions.add(Transaction(userId = userId, title = "Electricity bill", amount = 802.0, isExpense = true, category = "Bills", paymentMethod = "UPI", date = getDateForMonthOffset(-4, 11)))
        transactions.add(Transaction(userId = userId, title = "Cinema", amount = 382.0, isExpense = true, category = "Entertainment", paymentMethod = "Card", date = getDateForMonthOffset(-4, 22)))

        // Month -5
        transactions.add(Transaction(userId = userId, title = "Groceries", amount = 1900.0, isExpense = true, category = "Food", paymentMethod = "UPI", date = getDateForMonthOffset(-5, 6)))
        transactions.add(Transaction(userId = userId, title = "Swiggy", amount = 1100.0, isExpense = true, category = "Food", paymentMethod = "Card", date = getDateForMonthOffset(-5, 16)))
        transactions.add(Transaction(userId = userId, title = "Headphones case", amount = 1100.0, isExpense = true, category = "Shopping", paymentMethod = "UPI", date = getDateForMonthOffset(-5, 10)))
        transactions.add(Transaction(userId = userId, title = "Belts", amount = 700.0, isExpense = true, category = "Shopping", paymentMethod = "Cash", date = getDateForMonthOffset(-5, 22)))
        transactions.add(Transaction(userId = userId, title = "Train fare", amount = 1300.0, isExpense = true, category = "Travel", paymentMethod = "Card", date = getDateForMonthOffset(-5, 12)))
        transactions.add(Transaction(userId = userId, title = "Local travel", amount = 900.0, isExpense = true, category = "Travel", paymentMethod = "UPI", date = getDateForMonthOffset(-5, 24)))
        transactions.add(Transaction(userId = userId, title = "Electricity bill", amount = 752.0, isExpense = true, category = "Bills", paymentMethod = "UPI", date = getDateForMonthOffset(-5, 14)))
        transactions.add(Transaction(userId = userId, title = "Streaming rental", amount = 82.0, isExpense = true, category = "Entertainment", paymentMethod = "UPI", date = getDateForMonthOffset(-5, 20)))

        for (tx in transactions) {
            txDao.insert(tx)
        }
    }

    suspend fun clear(userId: Long) = withContext(Dispatchers.IO) {
        db.transactionDao().deleteUserTransactions(userId)
        db.budgetDao().deleteUserBudgets(userId)
    }

    private fun getDateForMonthOffset(monthOffset: Int, day: Int): Long {
        val cal = Calendar.getInstance()
        val todayDay = cal.get(Calendar.DAY_OF_MONTH)
        cal.add(Calendar.MONTH, monthOffset)

        val targetDay = if (monthOffset == 0) {
            minOf(day, todayDay)
        } else {
            val maxDaysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            minOf(day, maxDaysInMonth)
        }

        cal.set(Calendar.DAY_OF_MONTH, targetDay)
        cal.set(Calendar.HOUR_OF_DAY, 12)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
