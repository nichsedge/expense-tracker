package com.sans.expensetracker.data.local

import androidx.room.*
import com.sans.expensetracker.data.local.dao.*
import com.sans.expensetracker.data.local.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.MainScope

@Database(
    entities = [
        ExpenseEntity::class,
        InstallmentEntity::class,
        InstallmentItemEntity::class,
        CategoryEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract val expenseDao: ExpenseDao
    abstract val categoryDao: CategoryDao
    abstract val installmentDao: InstallmentDao

    fun checkpoint() {
        val cursor = query(androidx.sqlite.db.SimpleSQLiteQuery("PRAGMA wal_checkpoint(TRUNCATE)"), null)
        if (cursor.moveToFirst()) {
            cursor.getInt(0) // Forces evaluation
        }
        cursor.close()
    }

    class Callback(
        private val context: android.content.Context,
        private val categoryDaoProvider: javax.inject.Provider<CategoryDao>,
        private val expenseDaoProvider: javax.inject.Provider<ExpenseDao>,
        private val installmentDaoProvider: javax.inject.Provider<InstallmentDao>
    ) : RoomDatabase.Callback() {
        override fun onOpen(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            super.onOpen(db)
            MainScope().launch {
                val categoryDao = categoryDaoProvider.get()
                val expenseDao = expenseDaoProvider.get()
                val installmentDao = installmentDaoProvider.get()

                // Always ensure basic categories exist
                if (categoryDao.getCount() == 0) {
                    categoryDao.insertCategory(CategoryEntity(name = "Food", icon = "restaurant"))
                    categoryDao.insertCategory(CategoryEntity(name = "Health", icon = "health_and_safety"))
                    categoryDao.insertCategory(CategoryEntity(name = "Shopping", icon = "shopping_bag"))
                    categoryDao.insertCategory(CategoryEntity(name = "Transport", icon = "commute"))
                    categoryDao.insertCategory(CategoryEntity(name = "Subscriptions", icon = "language"))
                    categoryDao.insertCategory(CategoryEntity(name = "Others", icon = "category"))
                }

                // Inject Seed Data from CSV ONLY if EVERYTHING is empty
                if (expenseDao.getExpenseCount() == 0 && installmentDao.getInstallmentCount() == 0) {
                    try {
                        val expenses = com.sans.expensetracker.data.util.CsvParser.parse(context)
                        if (expenses.isNotEmpty()) {
                            expenseDao.insertExpenses(expenses)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}
