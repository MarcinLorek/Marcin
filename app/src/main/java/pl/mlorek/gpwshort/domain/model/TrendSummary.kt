package pl.mlorek.gpwshort.domain.model

/**
 * Wynik [pl.mlorek.gpwshort.domain.usecase.SummarizeTrendUseCase]. Celowo nie zawiera
 * gotowego tekstu (to złamałoby niezależność warstwy domain od zasobów Androida) –
 * UI mapuje warianty na sformatowane stringi z strings.xml.
 */
sealed interface TrendSummary {
    data class Rising(val consecutiveDays: Int, val changeLastMonth: Double) : TrendSummary
    data class Falling(val consecutiveDays: Int, val changeLastMonth: Double) : TrendSummary
    data class Flat(val consecutiveDays: Int) : TrendSummary
    data object New : TrendSummary
    data object InsufficientData : TrendSummary
}
