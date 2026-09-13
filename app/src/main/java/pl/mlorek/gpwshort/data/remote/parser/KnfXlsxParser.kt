package pl.mlorek.gpwshort.data.remote.parser

import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import pl.mlorek.gpwshort.data.remote.dto.KnfParseResult
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.xml.parsers.SAXParserFactory

/**
 * Lekki, samodzielny parser eksportu XLSX rejestru KNF - bez Apache POI.
 *
 * Decyzja architektoniczna: mimo że w treści zadania POI jest wskazany jako opcja,
 * świadomie z niego rezygnujemy na rzecz własnego parsera opartego o [ZipInputStream]
 * (plik .xlsx to zwykłe archiwum ZIP) i wbudowany w JDK/Android [javax.xml.parsers.SAXParserFactory]:
 * - poi-ooxml ma bardzo duży rozmiar (xmlbeans, commons-compress, log4j) i nie jest oficjalnie
 *   wspierany na Androidzie - część klas dot. stylów/rysunków odwołuje się do java.awt,
 *   którego na Androidzie nie ma (ryzyko NoClassDefFoundError w czasie działania),
 * - potrzebujemy odczytać tylko płaskie komórki tekstowe/liczbowe z jednego arkusza,
 *   więc pełny model obiektowy POI jest zbędny.
 *
 * Ograniczenia tej uproszczonej implementacji (świadomy kompromis):
 * - obsługuje jeden, wskazany z góry arkusz (domyślnie pierwszy: xl/worksheets/sheet1.xml),
 *   bez odwzorowania nazw arkuszy z workbook.xml.rels,
 * - nie interpretuje formatowania/stylów/dat jako liczb szeregowych Excela (KNF eksportuje
 *   daty jako tekst w kolumnie, co jest zgodne z tym, czego oczekuje [KnfTableInterpreter]);
 *   jeśli w praktyce eksport KNF koduje daty jako liczby szeregowe, tę klasę trzeba rozszerzyć
 *   o konwersję numeru seryjnego Excela na [java.time.LocalDate].
 */
class KnfXlsxParser @Inject constructor() {

    fun parse(bytes: ByteArray, sheetEntryName: String = DEFAULT_SHEET_ENTRY): KnfParseResult {
        val entries = readRelevantZipEntries(bytes)
        val sharedStrings = entries[SHARED_STRINGS_ENTRY]?.let(::parseSharedStrings) ?: emptyList()
        val sheetBytes = entries[sheetEntryName]
            ?: throw ParsingException("Plik XLSX nie zawiera arkusza $sheetEntryName")

        val rows = parseSheetRows(sheetBytes, sharedStrings)
        if (rows.size < 2) {
            throw ParsingException("Arkusz XLSX nie zawiera wystarczającej liczby wierszy danych")
        }
        val header = rows.first()
        val dataRows = rows.drop(1)

        return when (val interpreted = KnfTableInterpreter.interpret(header, dataRows)) {
            is InterpretedTable.Significant -> KnfParseResult(interpreted.rows, emptyList())
            is InterpretedTable.Summary -> KnfParseResult(emptyList(), interpreted.rows)
            InterpretedTable.Unrecognized ->
                throw ParsingException("Nagłówki arkusza XLSX nie pasują do znanego układu kolumn KNF")
        }
    }

    private fun readRelevantZipEntries(bytes: ByteArray): Map<String, ByteArray> {
        val result = mutableMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && (entry.name == SHARED_STRINGS_ENTRY || entry.name.startsWith("xl/worksheets/"))) {
                    result[entry.name] = zis.readBytes()
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        return result
    }

    private fun parseSharedStrings(xmlBytes: ByteArray): List<String> {
        val strings = mutableListOf<String>()
        val currentText = StringBuilder()
        var insideSi = false

        val handler = object : DefaultHandler() {
            override fun startElement(uri: String?, localName: String?, qName: String?, attributes: Attributes?) {
                if (qName == "si") {
                    insideSi = true
                    currentText.setLength(0)
                }
            }

            override fun characters(ch: CharArray, start: Int, length: Int) {
                if (insideSi) currentText.append(ch, start, length)
            }

            override fun endElement(uri: String?, localName: String?, qName: String?) {
                if (qName == "si") {
                    strings.add(currentText.toString())
                    insideSi = false
                }
            }
        }
        newSaxParser().parse(ByteArrayInputStream(xmlBytes), handler)
        return strings
    }

    private fun parseSheetRows(xmlBytes: ByteArray, sharedStrings: List<String>): List<List<String>> {
        val rows = mutableListOf<MutableList<String>>()
        var currentRow: MutableList<String>? = null
        var currentCellType: String? = null
        var currentColumnIndex = -1
        var insideValueTag = false
        val currentValue = StringBuilder()

        val handler = object : DefaultHandler() {
            override fun startElement(uri: String?, localName: String?, qName: String?, attributes: Attributes?) {
                when (qName) {
                    "row" -> currentRow = mutableListOf()
                    "c" -> {
                        currentCellType = attributes?.getValue("t")
                        currentColumnIndex = columnLetterToIndex(attributes?.getValue("r"))
                    }
                    "v", "t" -> {
                        insideValueTag = true
                        currentValue.setLength(0)
                    }
                }
            }

            override fun characters(ch: CharArray, start: Int, length: Int) {
                if (insideValueTag) currentValue.append(ch, start, length)
            }

            override fun endElement(uri: String?, localName: String?, qName: String?) {
                when (qName) {
                    "v", "t" -> {
                        insideValueTag = false
                        val raw = currentValue.toString()
                        val resolved = if (currentCellType == "s") {
                            raw.toIntOrNull()?.let { sharedStrings.getOrNull(it) } ?: ""
                        } else {
                            raw
                        }
                        currentRow?.let { row ->
                            if (currentColumnIndex < 0) {
                                row.add(resolved)
                            } else {
                                while (row.size < currentColumnIndex) row.add("")
                                if (row.size == currentColumnIndex) row.add(resolved) else row[currentColumnIndex] = resolved
                            }
                        }
                    }
                    "row" -> {
                        currentRow?.let { rows.add(it) }
                        currentRow = null
                        currentColumnIndex = -1
                    }
                }
            }
        }
        newSaxParser().parse(ByteArrayInputStream(xmlBytes), handler)
        return rows
    }

    private fun newSaxParser() = SAXParserFactory.newInstance().apply {
        isNamespaceAware = false
    }.newSAXParser()

    private fun columnLetterToIndex(cellRef: String?): Int {
        if (cellRef.isNullOrEmpty()) return -1
        val letters = cellRef.takeWhile { it.isLetter() }
        if (letters.isEmpty()) return -1
        var index = 0
        for (ch in letters) {
            index = index * 26 + (ch.uppercaseChar() - 'A' + 1)
        }
        return index - 1
    }

    companion object {
        private const val SHARED_STRINGS_ENTRY = "xl/sharedStrings.xml"
        private const val DEFAULT_SHEET_ENTRY = "xl/worksheets/sheet1.xml"
    }
}
