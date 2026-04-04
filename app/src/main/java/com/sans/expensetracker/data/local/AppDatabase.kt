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
        CategoryEntity::class,
        TagEntity::class,
        ExpenseTagCrossRef::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract val expenseDao: ExpenseDao
    abstract val categoryDao: CategoryDao
    abstract val installmentDao: InstallmentDao
    abstract val tagDao: TagDao

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

    companion object {
        val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Create tags table
                db.execSQL("CREATE TABLE IF NOT EXISTS `tags` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL)")
                
                // Create expense_tag_ref table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `expense_tag_ref` (
                        `expenseId` INTEGER NOT NULL, 
                        `tagId` INTEGER NOT NULL, 
                        PRIMARY KEY(`expenseId`, `tagId`), 
                        FOREIGN KEY(`expenseId`) REFERENCES `expenses`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, 
                        FOREIGN KEY(`tagId`) REFERENCES `tags`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_expense_tag_ref_expenseId` ON `expense_tag_ref` (`expenseId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_expense_tag_ref_tagId` ON `expense_tag_ref` (`tagId`)")

                // Extract and migrate platform values
                val cursor = db.query("SELECT DISTINCT platform FROM expenses WHERE platform IS NOT NULL AND platform != ''")
                val platforms = mutableListOf<String>()
                while (cursor.moveToNext()) {
                    platforms.add(cursor.getString(0))
                }
                cursor.close()

                for (platform in platforms) {
                    // Pre-insert each platform into tags if it doesn't exist
                    db.execSQL("INSERT OR IGNORE INTO tags (name) VALUES (?)", arrayOf(platform))
                    
                    // Link expenses to these tags
                    db.execSQL("""
                        INSERT INTO expense_tag_ref (expenseId, tagId)
                        SELECT e.id, t.id
                        FROM expenses e, tags t
                        WHERE e.platform = ? AND t.name = ?
                    """.trimIndent(), arrayOf(platform, platform))
                }
            }
        }
    }
}
