package pl.mlorek.gpwshort.data.remote

import pl.mlorek.gpwshort.BuildConfig

/**
 * Adresy rejestru krótkiej sprzedaży KNF.
 *
 * [REGISTER_HTML_URL] to jedyny adres podany i potwierdzony w treści zadania.
 * Publiczny opis rejestru KNF wspomina też o możliwości pobrania danych jako
 * plik XLSX/CSV, jednak dokładny URL takiego eksportu nie został zweryfikowany
 * na żywo w tym środowisku (domena knf.gov.pl była zablokowana przez proxy sieciowe
 * użyte do wygenerowania tego kodu) - stąd [KnfXlsxExportUrl] jest wartością `null`
 * do uzupełnienia po ręcznej weryfikacji w przeglądarce (Narzędzia deweloperskie ->
 * karta Sieć -> kliknięcie przycisku "Eksportuj"/"Pobierz" na stronie rejestru).
 */
object KnfEndpoints {
    const val REGISTER_HTML_URL = "${BuildConfig.KNF_BASE_URL}rss_pub/rssH.html"

    /** Uzupełnić po potwierdzeniu rzeczywistego adresu eksportu XLSX. */
    val KNF_XLSX_EXPORT_URL: String? = null
}
