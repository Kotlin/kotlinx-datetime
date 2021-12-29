package kotlinx.datetime

public sealed interface ArbitraryPrecisionDate : Comparable<ArbitraryPrecisionDate> {

    public val year: Int

    public companion object {

        public fun parse(value: String): ArbitraryPrecisionDate {
            TODO()
        }
    }
}
