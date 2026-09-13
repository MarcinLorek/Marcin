package pl.mlorek.gpwshort.domain.model

import java.time.LocalDate

data class IssuerDetail(
    val issuer: Issuer,
    val isClosed: Boolean,
    val closedSince: LocalDate?,
    val fullHistory: List<PositionHistoryPoint>,
    val holders: List<HolderPosition>,
) {
    fun historyFor(range: DateRange): List<PositionHistoryPoint> {
        val start = range.startDate(fullHistory.lastOrNull()?.date ?: LocalDate.now()) ?: return fullHistory
        return fullHistory.filter { !it.date.isBefore(start) }
    }
}
