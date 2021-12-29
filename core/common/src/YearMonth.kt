package kotlinx.datetime

public data class YearMonth(override val year: Int, public val month: Month) : ArbitraryPrecisionDate {

    override fun compareTo(other: ArbitraryPrecisionDate): Int {
        TODO("Not yet implemented")
    }
}
