package pl.mlorek.gpwshort.domain.model

import java.time.LocalDate

enum class DateRange(val days: Long?) {
    DAYS_30(30),
    DAYS_90(90),
    YEAR_1(365),
    MAX(null);

    fun startDate(today: LocalDate): LocalDate? = days?.let { today.minusDays(it) }
}
