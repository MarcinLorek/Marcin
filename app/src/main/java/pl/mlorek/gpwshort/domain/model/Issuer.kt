package pl.mlorek.gpwshort.domain.model

/**
 * Emitent (spółka notowana na GPW), na którego zgłoszono pozycję krótką w rejestrze KNF.
 * [isin] bywa nieznany, gdy KNF publikuje dany wiersz bez tego pola – nazwa emitenta
 * (znormalizowana w [KnfHtmlParser]/[KnfXlsxParser]) jest wtedy jedynym stabilnym kluczem.
 */
data class Issuer(
    val id: Long,
    val name: String,
    val isin: String?,
)
