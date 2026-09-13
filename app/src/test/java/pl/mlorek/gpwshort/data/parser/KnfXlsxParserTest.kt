package pl.mlorek.gpwshort.data.parser

import org.junit.Assert.assertEquals
import org.junit.Test
import pl.mlorek.gpwshort.data.remote.parser.KnfXlsxParser
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class KnfXlsxParserTest {

    private val parser = KnfXlsxParser()

    private val sharedStrings = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="6" uniqueCount="6">
            <si><t>Nazwa emitenta</t></si>
            <si><t>ISIN</t></si>
            <si><t>Wielkość pozycji w proc.</t></si>
            <si><t>Data pozycji</t></si>
            <si><t>Spółka SA</t></si>
            <si><t>PLSPLKA00010</t></si>
        </sst>
    """.trimIndent()

    private val sheet = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
            <sheetData>
                <row r="1">
                    <c r="A1" t="s"><v>0</v></c>
                    <c r="B1" t="s"><v>1</v></c>
                    <c r="C1" t="s"><v>2</v></c>
                    <c r="D1" t="s"><v>3</v></c>
                </row>
                <row r="2">
                    <c r="A2" t="s"><v>4</v></c>
                    <c r="B2" t="s"><v>5</v></c>
                    <c r="C2"><v>1.85</v></c>
                    <c r="D2" t="str"><v>2024-01-15</v></c>
                </row>
            </sheetData>
        </worksheet>
    """.trimIndent()

    @Test
    fun `parses summary sheet using shared strings and header-based columns`() {
        val bytes = buildXlsx(sharedStrings, sheet)

        val result = parser.parse(bytes)

        assertEquals(1, result.summaryPositions.size)
        val row = result.summaryPositions.first()
        assertEquals("Spółka SA", row.issuerName)
        assertEquals("PLSPLKA00010", row.isin)
        assertEquals(1.85, row.percent, 0.0001)
        assertEquals(LocalDate.of(2024, 1, 15), row.positionDate)
    }

    private fun buildXlsx(sharedStringsXml: String, sheetXml: String): ByteArray {
        val outputStream = ByteArrayOutputStream()
        ZipOutputStream(outputStream).use { zip ->
            zip.putNextEntry(ZipEntry("xl/sharedStrings.xml"))
            zip.write(sharedStringsXml.toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
            zip.write(sheetXml.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        return outputStream.toByteArray()
    }
}
