package pl.mlorek.gpwshort.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.mlorek.gpwshort.domain.model.PositionHistoryPoint
import pl.mlorek.gpwshort.domain.model.TrendSummary
import java.time.LocalDate

class SummarizeTrendUseCaseTest {

    private val useCase = SummarizeTrendUseCase()

    @Test
    fun `single data point is reported as new position`() {
        assertEquals(TrendSummary.New, useCase(history(1.0)))
    }

    @Test
    fun `empty history is insufficient data`() {
        assertEquals(TrendSummary.InsufficientData, useCase(emptyList()))
    }

    @Test
    fun `consecutive increases are reported as rising with days counted`() {
        val result = useCase(history(1.0, 1.2, 1.4, 1.6)) as TrendSummary.Rising

        assertEquals(3, result.consecutiveDays)
        assertTrue(result.changeLastMonth > 0)
    }

    @Test
    fun `direction change breaks the consecutive day streak`() {
        val result = useCase(history(1.0, 1.5, 1.2)) as TrendSummary.Falling

        assertEquals(1, result.consecutiveDays)
    }

    /** Buduje historię tak, jak zrobiłby to [pl.mlorek.gpwshort.data.mapper.buildHistory]. */
    private fun history(vararg percents: Double): List<PositionHistoryPoint> =
        percents.mapIndexed { index, percent ->
            PositionHistoryPoint(
                date = LocalDate.of(2024, 1, 1).plusDays(index.toLong()),
                totalShortPercent = percent,
                holderCount = 1,
                changeVsPrevious = if (index == 0) null else percent - percents[index - 1],
            )
        }
}
