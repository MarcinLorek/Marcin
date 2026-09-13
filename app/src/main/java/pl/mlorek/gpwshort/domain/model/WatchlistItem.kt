package pl.mlorek.gpwshort.domain.model

data class WatchlistItem(
    val issuer: Issuer,
    val alertThresholdPercentagePoints: Double,
    val latestSummary: IssuerSummary?,
)
