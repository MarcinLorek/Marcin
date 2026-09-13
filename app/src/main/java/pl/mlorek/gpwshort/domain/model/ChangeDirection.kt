package pl.mlorek.gpwshort.domain.model

/**
 * Kierunek zmiany wielkości pozycji krótkiej względem poprzedniego odczytu.
 */
enum class ChangeDirection {
    UP,
    DOWN,
    NONE;

    companion object {
        private const val EPSILON = 0.0001

        fun fromDelta(deltaPercentagePoints: Double?): ChangeDirection = when {
            deltaPercentagePoints == null -> NONE
            deltaPercentagePoints > EPSILON -> UP
            deltaPercentagePoints < -EPSILON -> DOWN
            else -> NONE
        }
    }
}
