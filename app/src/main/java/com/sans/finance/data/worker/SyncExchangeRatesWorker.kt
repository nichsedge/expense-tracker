package com.sans.finance.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sans.finance.data.local.dao.CurrencyDao
import com.sans.finance.data.local.entity.ExchangeRateEntity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

@HiltWorker
class SyncExchangeRatesWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val currencyDao: CurrencyDao,
    private val client: OkHttpClient
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (runAttemptCount >= 3) {
            return@withContext Result.failure()
        }

        val url = "https://open.er-api.com/v6/latest/IDR"

        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body.string()
                    val json = JSONObject(body)
                    val rates = json.optJSONObject("rates") ?: return@withContext Result.retry()
                    val exchangeRates = ArrayList<ExchangeRateEntity>(rates.length())

                    val keys = rates.keys()
                    while (keys.hasNext()) {
                        val code = keys.next()
                        val rateToBase = rates.getDouble(code)
                        if (rateToBase > 0) {
                            exchangeRates.add(
                                ExchangeRateEntity(
                                    code = code,
                                    rateToIdr = 1.0 / rateToBase
                                )
                            )
                        }
                    }
                    currencyDao.insertRates(exchangeRates)
                    Result.success()
                } else {
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            if (runAttemptCount >= 2) Result.failure() else Result.retry()
        }
    }
}
