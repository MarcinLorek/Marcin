package pl.mlorek.gpwshort.data.remote.parser

import pl.mlorek.gpwshort.data.remote.dto.SignificantPositionRow
import pl.mlorek.gpwshort.data.remote.dto.SummaryPositionRow

/**
 * Wspólna logika interpretacji surowej tabeli (nagłówek + wiersze tekstowe) niezależna od
 * źródła (HTML przez Jsoup albo XLSX przez [KnfXlsxParser]) – dzięki temu obie ścieżki
 * pobierania danych rozpoznają te same warianty nazw kolumn KNF.
 */
sealed interface InterpretedTable {
    data class Significant(val rows: List<SignificantPositionRow>) : InterpretedTable
    data class Summary(val rows: List<SummaryPositionRow>) : InterpretedTable
    data object Unrecognized : InterpretedTable
}

object KnfTableInterpreter {

    fun interpret(header: List<String>, dataRows: List<List<String>>): InterpretedTable {
        val roleToColumn = HeaderMatcher.mapHeader(header)
        val percentCol = roleToColumn[ColumnRole.PERCENT] ?: return InterpretedTable.Unrecognized
        val dateCol = roleToColumn[ColumnRole.POSITION_DATE] ?: return InterpretedTable.Unrecognized
        val issuerCol = roleToColumn[ColumnRole.ISSUER_NAME] ?: return InterpretedTable.Unrecognized
        val isinCol = roleToColumn[ColumnRole.ISIN]
        val holderCol = roleToColumn[ColumnRole.HOLDER_NAME]

        fun cell(row: List<String>, index: Int?): String? = index?.let { row.getOrNull(it) }

        return if (holderCol != null) {
            val rows = dataRows.mapNotNull { row ->
                val holder = cell(row, holderCol)?.let(KnfValueParsing::cleanText)
                val issuer = cell(row, issuerCol)?.let(KnfValueParsing::cleanText)
                val percent = cell(row, percentCol)?.let(KnfValueParsing::parsePercent)
                val date = cell(row, dateCol)?.let(KnfValueParsing::parseDate)
                if (holder.isNullOrBlank() || issuer.isNullOrBlank() || percent == null || date == null) {
                    null
                } else {
                    SignificantPositionRow(
                        holderName = holder,
                        issuerName = issuer,
                        isin = KnfValueParsing.cleanIsin(cell(row, isinCol)),
                        percent = percent,
                        positionDate = date,
                    )
                }
            }
            InterpretedTable.Significant(rows)
        } else {
            val rows = dataRows.mapNotNull { row ->
                val issuer = cell(row, issuerCol)?.let(KnfValueParsing::cleanText)
                val percent = cell(row, percentCol)?.let(KnfValueParsing::parsePercent)
                val date = cell(row, dateCol)?.let(KnfValueParsing::parseDate)
                if (issuer.isNullOrBlank() || percent == null || date == null) {
                    null
                } else {
                    SummaryPositionRow(
                        issuerName = issuer,
                        isin = KnfValueParsing.cleanIsin(cell(row, isinCol)),
                        percent = percent,
                        positionDate = date,
                    )
                }
            }
            InterpretedTable.Summary(rows)
        }
    }
}
