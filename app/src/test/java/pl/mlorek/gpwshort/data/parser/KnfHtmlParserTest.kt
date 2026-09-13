package pl.mlorek.gpwshort.data.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import pl.mlorek.gpwshort.data.remote.parser.KnfHtmlParser
import pl.mlorek.gpwshort.data.remote.parser.ParsingException
import java.time.LocalDate

class KnfHtmlParserTest {

    private val parser = KnfHtmlParser()

    @Test
    fun `parses significant and summary tables regardless of column order`() {
        val html = """
            <html><body>
            <h2>Znaczące pozycje krótkie netto</h2>
            <table>
                <thead>
                    <tr><th>Data pozycji</th><th>Nazwa podmiotu ogłaszającego pozycję</th><th>Nazwa emitenta</th><th>ISIN</th><th>Wielkość pozycji w proc.</th></tr>
                </thead>
                <tbody>
                    <tr><td>2024-01-15</td><td>Fundusz Alfa</td><td>Spółka SA</td><td>PLSPLKA00010</td><td>1,25%</td></tr>
                    <tr><td>2024-01-15</td><td>Fundusz Beta</td><td>Spółka SA</td><td>PLSPLKA00010</td><td>0,60%</td></tr>
                </tbody>
            </table>
            <h2>Sumaryczne pozycje krótkie netto</h2>
            <table>
                <thead>
                    <tr><th>Nazwa emitenta</th><th>ISIN</th><th>Wielkość pozycji w proc.</th><th>Data pozycji</th></tr>
                </thead>
                <tbody>
                    <tr><td>Spółka SA</td><td>PLSPLKA00010</td><td>1,85%</td><td>2024-01-15</td></tr>
                </tbody>
            </table>
            </body></html>
        """.trimIndent()

        val result = parser.parse(html)

        assertEquals(2, result.significantPositions.size)
        assertEquals(1, result.summaryPositions.size)

        val summary = result.summaryPositions.first()
        assertEquals("Spółka SA", summary.issuerName)
        assertEquals("PLSPLKA00010", summary.isin)
        assertEquals(1.85, summary.percent, 0.0001)
        assertEquals(LocalDate.of(2024, 1, 15), summary.positionDate)

        val holder = result.significantPositions.first { it.holderName == "Fundusz Alfa" }
        assertEquals(1.25, holder.percent, 0.0001)
    }

    @Test
    fun `throws ParsingException when no table matches known layout`() {
        val html = """
            <html><body>
                <table><tr><th>Kolumna A</th><th>Kolumna B</th></tr><tr><td>x</td><td>y</td></tr></table>
            </body></html>
        """.trimIndent()

        assertThrows(ParsingException::class.java) { parser.parse(html) }
    }

    @Test
    fun `throws ParsingException when page has no tables at all`() {
        assertThrows(ParsingException::class.java) { parser.parse("<html><body><p>Brak danych</p></body></html>") }
    }
}
