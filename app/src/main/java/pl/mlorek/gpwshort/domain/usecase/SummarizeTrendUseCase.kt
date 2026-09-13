package pl.mlorek.gpwshort.domain.usecase

import pl.mlorek.gpwshort.domain.model.ChangeDirection
import pl.mlorek.gpwshort.domain.model.PositionHistoryPoint
import pl.mlorek.gpwshort.domain.model.TrendSummary
import javax.inject.Inject

/**
 * Czysta logika domenowa (bez zależności od Androida) – analizuje historię
 * sumarycznej pozycji krótkiej i ocenia bieżący trend.
 */
class SummarizeTrendUseCase @Inject constructor() {

    operator fun invoke(history: List<PositionHistoryPoint>): TrendSummary {
        if (history.isEmpty()) return TrendSummary.InsufficientData
        if (history.size == 1) return TrendSummary.New

        val sorted = history.sortedBy { it.date }
        val latest = sorted.last()

        var direction: ChangeDirection? = null
        var consecutiveDays = 0
        for (index in sorted.indices.reversed()) {
            val change = sorted[index].changeVsPrevious ?: continue
            val dir = ChangeDirection.fromDelta(change)
            if (dir == ChangeDirection.NONE) break
            if (direction == null) {
                direction = dir
                consecutiveDays = 1
            } else if (dir == direction) {
                consecutiveDays++
            } else {
                break
            }
        }

        val monthAgo = latest.date.minusDays(30)
        val monthAgoPoint = sorted.lastOrNull { !it.date.isAfter(monthAgo) } ?: sorted.first()
        val changeLastMonth = latest.totalShortPercent - monthAgoPoint.totalShortPercent

        return when (direction) {
            ChangeDirection.UP -> TrendSummary.Rising(consecutiveDays, changeLastMonth)
            ChangeDirection.DOWN -> TrendSummary.Falling(consecutiveDays, changeLastMonth)
            else -> TrendSummary.Flat(consecutiveDays)
        }
    }
}
