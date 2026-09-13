package pl.mlorek.gpwshort.domain.model

import java.time.LocalDate

data class PositionHistoryPoint(
    val date: LocalDate,
    val totalShortPercent: Double,
    val holderCount: Int,
    val changeVsPrevious: Double?,
)
