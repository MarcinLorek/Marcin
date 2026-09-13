package pl.mlorek.gpwshort.data.remote.parser

/**
 * Rzucany, gdy żadna z rozpoznawanych struktur tabel KNF nie została znaleziona
 * w pobranej treści – najbardziej prawdopodobna przyczyna to zmiana układu strony
 * przez KNF. Odróżniany od błędów sieciowych, żeby UI mógł pokazać dedykowany komunikat.
 */
class ParsingException(message: String, cause: Throwable? = null) : Exception(message, cause)
