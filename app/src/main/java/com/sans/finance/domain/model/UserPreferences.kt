package com.sans.finance.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class UserPreferences(
    val isPrivacyModeEnabled: Boolean = false,
    val fireManualEnabled: Boolean = false,
    val manualFireAnnualExpense: Long = 0L,
    val backupFrequency: String = "WEEKLY",
    val backupWifiOnly: Boolean = true,
    val backupRequiresCharging: Boolean = true,
    val lastBackupTime: Long = 0L,
    val lastBackupSizeBytes: Long = 0L,
    val cloudBackupProvider: String = "CLOUDFLARE_R2",
    val r2AccountId: String = "",
    val r2AccessKeyId: String = "",
    val r2SecretAccessKey: String = "",
    val r2BucketName: String = "ichsanul-dev",
    val gcsBucketName: String = "ichsanul-portfolio-snapshots"
)
