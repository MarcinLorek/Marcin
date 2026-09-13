package pl.mlorek.gpwshort.ui.navigation

object Destinations {
    const val LIST = "list"
    const val WATCHLIST = "watchlist"
    const val DETAIL_ARG_ISSUER_ID = "issuerId"
    const val DETAIL = "detail/{$DETAIL_ARG_ISSUER_ID}"

    fun detailRoute(issuerId: Long): String = "detail/$issuerId"
}
