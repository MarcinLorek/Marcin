package pl.mlorek.gpwshort.data.remote.dto

import java.time.LocalDate

/** Wiersz tabeli "znaczące pozycje krótkie netto" (≥0,5%) – jeden podmiot na emitenta. */
data class SignificantPositionRow(
    val holderName: String,
    val issuerName: String,
    val isin: String?,
    val percent: Double,
    val positionDate: LocalDate,
)

/** Wiersz tabeli "sumaryczne pozycje krótkie netto" (≥0,1%) – suma dla emitenta. */
data class SummaryPositionRow(
    val issuerName: String,
    val isin: String?,
    val percent: Double,
    val positionDate: LocalDate,
)

data class KnfParseResult(
    val significantPositions: List<SignificantPositionRow>,
    val summaryPositions: List<SummaryPositionRow>,
) {
    val isEmpty: Boolean get() = significantPositions.isEmpty() && summaryPositions.isEmpty()
}
