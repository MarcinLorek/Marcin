package pl.mlorek.gpwshort.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val PL_LOCALE = Locale("pl", "PL")
private val DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy", PL_LOCALE)

fun formatDate(date: LocalDate): String = date.format(DATE_FORMATTER)

fun formatPercent(value: Double): String = String.format(PL_LOCALE, "%.2f%%", value)

fun formatSignedPercentagePoints(value: Double): String {
    val formatted = String.format(PL_LOCALE, "%.2f", kotlin.math.abs(value))
    val sign = if (value >= 0) "+" else "-"
    return "$sign$formatted p.p."
}
