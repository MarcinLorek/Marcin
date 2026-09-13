package pl.mlorek.gpwshort.data.remote

import kotlinx.coroutines.delay
import pl.mlorek.gpwshort.data.remote.dto.KnfParseResult
import pl.mlorek.gpwshort.data.remote.parser.KnfHtmlParser
import pl.mlorek.gpwshort.data.remote.parser.KnfXlsxParser
import pl.mlorek.gpwshort.data.remote.parser.ParsingException
import java.io.IOException
import javax.inject.Inject

class KnfRemoteDataSource @Inject constructor(
    private val api: KnfApiService,
    private val htmlParser: KnfHtmlParser,
    private val xlsxParser: KnfXlsxParser,
) {

    /** Główna ścieżka pobierania - jedyny potwierdzony adres rejestru KNF, parsowany jako HTML. */
    suspend fun fetchLatest(): KnfParseResult = withRetry {
        val response = api.fetchHtml(KnfEndpoints.REGISTER_HTML_URL)
        val body = response.body()
        if (!response.isSuccessful || body.isNullOrBlank()) {
            throw IOException("KNF zwróciło niepoprawną odpowiedź (kod ${response.code()})")
        }
        htmlParser.parse(body)
    }

    /**
     * Ścieżka alternatywna: pobranie i sparsowanie eksportu XLSX, gdy jego adres zostanie
     * potwierdzony (patrz [KnfEndpoints.KNF_XLSX_EXPORT_URL]). Nieużywana domyślnie.
     */
    suspend fun fetchLatestFromXlsx(exportUrl: String): KnfParseResult = withRetry {
        val response = api.fetchBinary(exportUrl)
        val body = response.body()
        if (!response.isSuccessful || body == null) {
            throw IOException("KNF zwróciło niepoprawną odpowiedź (kod ${response.code()})")
        }
        xlsxParser.parse(body.bytes())
    }

    private suspend fun <T> withRetry(
        maxAttempts: Int = 3,
        initialDelayMillis: Long = 1_000,
        block: suspend () -> T,
    ): T {
        var attempt = 0
        var delayMillis = initialDelayMillis
        var lastError: Throwable? = null
        while (attempt < maxAttempts) {
            try {
                return block()
            } catch (e: ParsingException) {
                // Zmiana struktury strony nie naprawi się ponowieniem próby - przerywamy od razu.
                throw e
            } catch (e: IOException) {
                lastError = e
            }
            attempt++
            if (attempt < maxAttempts) {
                delay(delayMillis)
                delayMillis *= 2
            }
        }
        throw lastError ?: IOException("Nieznany błąd pobierania danych z KNF")
    }
}
