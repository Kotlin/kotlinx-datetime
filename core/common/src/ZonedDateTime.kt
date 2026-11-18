package kotlinx.datetime

/** A timezone-aware date-time object. */
public sealed class ZonedDateTime(
    protected val localDateTime: LocalDateTime,
) : Comparable<ZonedDateTime> {

    public abstract val timeZone: TimeZone

    // XXX: If the underlying time zone database can change while the current process is running
    // this value could become incorrect. Maybe don't cache at all? Or detect time zone db changes?
    private val instant: Instant by lazy { localDateTime.toInstant(timeZone) }

    public val year: Int get() = localDateTime.year
    public val monthNumber: Int get() = localDateTime.monthNumber
    public val month: Month get() = localDateTime.month
    public val dayOfMonth: Int get() = localDateTime.dayOfMonth
    public val dayOfWeek: DayOfWeek get() = localDateTime.dayOfWeek
    public val dayOfYear: Int get() = localDateTime.dayOfYear
    public val hour: Int get() = localDateTime.hour
    public val minute: Int get() = localDateTime.minute
    public val second: Int get() = localDateTime.second
    public val nanosecond: Int get() = localDateTime.nanosecond

    public fun toInstant(): Instant = instant

    public fun toLocalDateTime(): LocalDateTime = localDateTime

    public fun toLocalDateTime(timeZone: TimeZone): LocalDateTime =
        toInstant().toLocalDateTime(timeZone)

    public fun toLocalDate(): LocalDate = toLocalDateTime().date

    public fun toLocalDate(timeZone: TimeZone): LocalDate = toLocalDateTime(timeZone).date

    override fun compareTo(other: ZonedDateTime): Int = toInstant().compareTo(other.toInstant())

    override fun equals(other: Any?): Boolean =
        this === other || (other is ZonedDateTime && compareTo(other) == 0)

    override fun hashCode(): Int = localDateTime.hashCode() xor timeZone.hashCode()

    public companion object {

        public fun parse(isoString: String): ZonedDateTime {
            TODO()
        }
    }
}

/** Constructs a new [ZonedDateTime] from the given [localDateTime] and [timeZone]. */
public fun ZonedDateTime(localDateTime: LocalDateTime, timeZone: TimeZone): ZonedDateTime =
    when (timeZone) {
        is FixedOffsetTimeZone -> OffsetDateTime(localDateTime, timeZone)
        // TODO: Define a common RegionTimeZone and make TimeZone a sealed class/interface
        else -> RegionDateTime(localDateTime, timeZone)
    }

public fun String.toZonedDateTime(): ZonedDateTime = ZonedDateTime.parse(this)

/**
 * A [ZonedDateTime] describing a region-based [TimeZone].
 *
 * This class tries to represent how humans think in terms of dates.
 * For example, adding one day will result in the same local time even if a DST change happens
 * within that day.
 * Also, you can safely represent future dates because time zone database changes are taken into
 * account.
 */
public class RegionDateTime(
    localDateTime: LocalDateTime,
    // TODO: this should be a RegionTimeZone
    override val timeZone: TimeZone,
    // TODO: Add optional DST offset or at least a UTC offset (should it be part of RegionTimeZone?)
) : ZonedDateTime(localDateTime) {

    public constructor(instant: Instant, timeZone: TimeZone) :
            this(instant.toLocalDateTime(timeZone), timeZone)

    // TODO: Should RegionTimeZone.toString() print with surrounding `[]`?
    override fun toString(): String = "$localDateTime[$timeZone]"
}

/**
 * A [ZonedDateTime] with a [FixedOffsetTimeZone]. Use this only for representing past events.
 *
 * Don't use this to represent future dates (e.g. in a calendar) because this fails to work
 * correctly under time zone database changes. Use [RegionDateTime] instead.
 */
public class OffsetDateTime(
    localDateTime: LocalDateTime,
    override val timeZone: FixedOffsetTimeZone,
) : ZonedDateTime(localDateTime) {

    public constructor(instant: Instant, timeZone: FixedOffsetTimeZone) :
            this(instant.toLocalDateTime(timeZone), timeZone)

    override fun toString(): String = "$localDateTime$timeZone"
}
