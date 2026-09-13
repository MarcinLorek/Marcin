package pl.mlorek.gpwshort.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import pl.mlorek.gpwshort.domain.usecase.GetWatchlistUseCase
import pl.mlorek.gpwshort.domain.usecase.RefreshDataUseCase
import pl.mlorek.gpwshort.util.AppResult
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.abs

/**
 * Codzienne (w dni robocze) odświeżenie danych z rejestru KNF w tle, wywoływane przez
 * [SyncScheduler]. Po udanym odświeżeniu porównuje stan obserwowanych spółek sprzed i po
 * imporcie, i wysyła powiadomienie, gdy zmiana przekroczy zdefiniowany przez użytkownika próg.
 */
@HiltWorker
class DailyRefreshWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val refreshDataUseCase: RefreshDataUseCase,
    private val getWatchlistUseCase: GetWatchlistUseCase,
    private val notificationHelper: NotificationHelper,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val today = LocalDate.now().dayOfWeek
        if (today == DayOfWeek.SATURDAY || today == DayOfWeek.SUNDAY) {
            return Result.success()
        }

        val beforePercentByIssuer = getWatchlistUseCase().first()
            .associate { it.issuer.id to it.latestSummary?.totalShortPercent }

        return when (val result = refreshDataUseCase(force = true)) {
            is AppResult.Success -> {
                notifyThresholdCrossings(beforePercentByIssuer)
                Result.success()
            }
            is AppResult.Error -> if (runAttemptCount < MAX_RUN_ATTEMPTS) Result.retry() else Result.failure()
        }
    }

    private suspend fun notifyThresholdCrossings(beforePercentByIssuer: Map<Long, Double?>) {
        val after = getWatchlistUseCase().first()
        for (item in after) {
            val before = beforePercentByIssuer[item.issuer.id] ?: continue
            val current = item.latestSummary?.totalShortPercent ?: continue
            val delta = abs(current - before)
            if (delta >= item.alertThresholdPercentagePoints) {
                notificationHelper.notifyThresholdCrossed(item.issuer.name, current - before, current)
            }
        }
    }

    companion object {
        private const val MAX_RUN_ATTEMPTS = 3
    }
}
