package pl.mlorek.gpwshort.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Rejestruje cykliczne odświeżanie danych KNF raz dziennie. WorkManager nie ma natywnego
 * pojęcia "tylko dni robocze" dla [androidx.work.PeriodicWorkRequest], więc harmonogram
 * jest codzienny, a pominięcie weekendu obsługuje [DailyRefreshWorker.doWork].
 */
@Singleton
class SyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun scheduleDaily() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<DailyRefreshWorker>(1, TimeUnit.DAYS)
            .setConstraints(constraints)
            .setInitialDelay(minutesUntil(TARGET_HOUR, TARGET_MINUTE), TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    /** Minuty do najbliższego wystąpienia godziny [hour]:[minute] (dziś lub jutro). */
    private fun minutesUntil(hour: Int, minute: Int): Long {
        val now = LocalDateTime.now()
        var target = now.toLocalDate().atTime(LocalTime.of(hour, minute))
        if (!target.isAfter(now)) {
            target = target.plusDays(1)
        }
        return ChronoUnit.MINUTES.between(now, target)
    }

    companion object {
        const val WORK_NAME = "daily_knf_refresh"

        // KNF publikuje dane po zakończeniu sesji GPW (17:00) - odświeżamy wieczorem.
        private const val TARGET_HOUR = 19
        private const val TARGET_MINUTE = 0
    }
}
