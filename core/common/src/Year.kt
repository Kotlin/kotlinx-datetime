package kotlinx.datetime

public data class Year(override val year: Int) : ArbitraryPrecisionDate {

    override fun compareTo(other: ArbitraryPrecisionDate): Int =
        year.compareTo(other.year)
}
