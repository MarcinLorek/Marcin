package pl.mlorek.gpwshort.domain.model

import java.time.LocalDate

/**
 * Pojedyncza, znacząca (≥0,5%) pozycja krótka konkretnego podmiotu na danego emitenta,
 * w jego najnowszym zgłoszonym stanie.
 */
data class HolderPosition(
    val holderName: String,
    val percent: Double,
    val positionDate: LocalDate,
    val previousPercent: Double?,
) {
    val changeDirection: ChangeDirection
        get() = ChangeDirection.fromDelta(previousPercent?.let { percent - it })
}
