/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.zoneinfo

import kotlinx.datetime.*
import kotlinx.datetime.internal.*

/**
 * A synonym for [BundledTimeZoneContext].
 */
public val TimeZoneContext.Companion.Bundled: BundledTimeZoneContext
    get() = BundledTimeZoneContext

/**
 * A [TimeZoneContext] that uses the time zone database from the `kotlinx-datetime-zoneinfo` artifact.
 *
 * This [TimeZoneContext] is drop-in replacement for [TimeZoneContext.System] for cases where
 * the system timezone database is likely outdated.
 */
public object BundledTimeZoneContext : TimeZoneContext {
    /**
     * Returns the time zone identified by the provided [id].
     *
     * The supported variants of time zone identifiers:
     * - `Z`, 'UTC', 'UT' or 'GMT' —
     *   identifies the fixed-offset time zone [TimeZone.UTC],
     * - a string starting with '+', '-', `UTC+`, `UTC-`, `UT+`, `UT-`, `GMT+`, `GMT-` —
     *   identifies the time zone with the fixed offset specified after `+` or `-`,
     * - all other strings are treated as region-based zone identifiers.
     *   In the IANA Time Zone Database (TZDB) which is used
     *   as the source of time zones,
     *   these ids are usually in the form `area/city`, for example,
     *   `Europe/Berlin` or `America/Los_Angeles`.
     *
     * It is guaranteed that passing any value from [availableZoneIds]
     * to this function will return a valid time zone.
     *
     * @throws IllegalTimeZoneException if [id] has an invalid format or a time zone
     * with the identifier [id] is not found.
     * @see getOrNull for a function that returns `null` if a time zone is not available.
     */
    override fun get(id: String): TimeZone = impl.get(id)

    /**
     * Returns the time zone identified by the provided [id], or `null` if it's not found.
     *
     * This is optimized shorthand for `try { get(id) } catch (_: IllegalTimeZoneException) { null }`.
     * Please see the documentation for [get].
     *
     * It is guaranteed that this function never throws an exception for [BundledTimeZoneContext].
     */
    override fun getOrNull(id: String): TimeZone? = impl.getOrNull(id)

    /**
     * Returns the set of identifiers of the time zones whose data is bundled in this timezone database
     * in addition to the fixed-offset time zones.
     */
    override fun availableZoneIds(): Set<String> = impl.availableZoneIds()

    /**
     * Equivalent to [TimeZoneContext.System.currentTimeZoneId].
     */
    override fun currentTimeZoneId(): String = TimeZoneContext.System.currentTimeZoneId()

    /**
     * Get the current system timezone.
     *
     * Equivalent to `get(currentTimeZoneId())`.
     *
     * @throws IllegalTimeZoneException if the identifier returned by [currentTimeZoneId]
     * is not recognized by this database.
     */
    override fun currentTimeZone(): TimeZone =
        getOrNull(currentTimeZoneId()) ?: throw IllegalTimeZoneException(
            "Zone ID '${currentTimeZoneId()}', which is the current system timezone, " +
                    "was not recognized by the bundled timezone database (version $timeZoneDatabaseVersion)."
        )
}

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
private val impl: TimeZoneDatabase = FixedOffsetTimeZoneDatabase(object: TimeZoneDatabase {
    override fun get(id: String): TimeZone = getOrNull(id)
        ?: throw IllegalTimeZoneException(
            "Zone ID '$id' was not recognized by the bundled timezone database (version $timeZoneDatabaseVersion)."
        )

    override fun getOrNull(id: String): TimeZone? {
        val data = zoneDataByNameOrNull(id) ?: return null
        return RuleBasedTimeZoneCalculations(readTzFile(data).toTimeZoneRules(), id).asTimeZone()
    }

    override fun availableZoneIds(): Set<String> = timeZones
})

internal expect fun zoneDataByNameOrNull(name: String): ByteArray?

internal expect val timeZones: Set<String>

internal val timeZoneDatabaseVersion: String = kotlinx.datetime.timezones.tzData.timeZoneDatabaseVersion
