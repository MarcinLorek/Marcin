package pl.mlorek.gpwshort.data.remote.parser

import org.jsoup.Jsoup
import pl.mlorek.gpwshort.data.remote.dto.KnfParseResult
import pl.mlorek.gpwshort.data.remote.dto.SignificantPositionRow
import pl.mlorek.gpwshort.data.remote.dto.SummaryPositionRow
import javax.inject.Inject

/**
 * Parsuje HTML rejestru krótkiej sprzedaży KNF (https://rss.knf.gov.pl/rss_pub/rssH.html).
 *
 * UWAGA (ryzyko): w środowisku, w którym powstał ten kod, domena knf.gov.pl była
 * zablokowana przez proxy sieciowe, więc dokładny bieżący układ HTML nie został
 * zweryfikowany na żywo. Parser celowo NIE zakłada stałej liczby/kolejności kolumn ani
 * konkretnego id/class tabeli – zamiast tego przegląda wszystkie tabele na stronie,
 * odczytuje wiersz nagłówka i rozpoznaje role kolumn po słowach kluczowych
 * (patrz [HeaderMatcher]). Jeśli KNF przebuduje stronę tak, że nagłówki nie będą już
 * zawierać rozpoznawanych słów kluczowych, sparsowanie zwróci [InterpretedTable.Unrecognized]
 * dla każdej tabeli i zostanie rzucony [ParsingException] – jest to sygnał dla
 * repozytorium/UI, że parser wymaga aktualizacji (patrz strings.xml: common_error_parsing).
 */
class KnfHtmlParser @Inject constructor() {

    fun parse(html: String): KnfParseResult {
        val document = Jsoup.parse(html)
        val tables = document.select("table")
        if (tables.isEmpty()) {
            throw ParsingException("Strona KNF nie zawiera żadnej tabeli - prawdopodobna zmiana struktury strony")
        }

        val significant = mutableListOf<SignificantPositionRow>()
        val summary = mutableListOf<SummaryPositionRow>()

        for (table in tables) {
            val headRows = table.select("thead tr")
            val headerCells: List<String>
            val bodyRows: List<List<String>>

            if (headRows.isNotEmpty()) {
                headerCells = headRows.first()!!.select("th,td").map { it.text() }
                bodyRows = table.select("tbody tr").map { row -> row.select("td,th").map { it.text() } }
            } else {
                val allRows = table.select("tr")
                if (allRows.isEmpty()) continue
                headerCells = allRows.first()!!.select("th,td").map { it.text() }
                bodyRows = allRows.drop(1).map { row -> row.select("td,th").map { it.text() } }
            }

            if (headerCells.isEmpty() || bodyRows.isEmpty()) continue

            when (val interpreted = KnfTableInterpreter.interpret(headerCells, bodyRows)) {
                is InterpretedTable.Significant -> significant += interpreted.rows
                is InterpretedTable.Summary -> summary += interpreted.rows
                InterpretedTable.Unrecognized -> Unit // np. tabela nawigacyjna/stopka strony - pomijamy
            }
        }

        val result = KnfParseResult(significant, summary)
        if (result.isEmpty) {
            throw ParsingException(
                "Żadna tabela na stronie KNF nie pasuje do rozpoznawanego układu kolumn - parser wymaga aktualizacji",
            )
        }
        return result
    }
}
