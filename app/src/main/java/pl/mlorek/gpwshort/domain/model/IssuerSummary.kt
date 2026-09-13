package pl.mlorek.gpwshort.domain.model

import java.time.LocalDate

/**
 * Wiersz listy głównej: bieżący stan sumarycznej pozycji krótkiej dla emitenta
 * wraz ze zmianą względem poprzedniego zapisanego lokalnie snapshotu.
 */
data class IssuerSummary(
    val issuer: Issuer,
    val totalShortPercent: Double,
    val holderCount: Int,
    val positionDate: LocalDate,
    val previousTotalShortPercent: Double?,
    val isClosed: Boolean,
) {
    val changePercentagePoints: Double?
        get() = previousTotalShortPercent?.let { totalShortPercent - it }

    val changeDirection: ChangeDirection
        get() = ChangeDirection.fromDelta(changePercentagePoints)

    val isNew: Boolean
        get() = previousTotalShortPercent == null
}
