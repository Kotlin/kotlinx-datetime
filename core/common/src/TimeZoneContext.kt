/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlinx.datetime.internal.currentSystemDefaultTimeZone
import kotlinx.datetime.internal.systemTimeZoneIdProvider
import kotlinx.datetime.internal.systemTimezoneDatabase

public interface TimeZoneDatabase {
    public fun get(id: String): TimeZone =
        getOrNull(id) ?: error("Time zone ID '$id' not recognized by the timezone database $this")
    public fun getOrNull(id: String): TimeZone?
    public fun availableZoneIds(): Set<String>
}

public interface TimeZoneIdProvider {
    public fun currentTimeZoneId(): String
}

/**
 * Facilities for accessing the current timezone information.
 *
 * This interface combines two aspects of working with timezones:
 * - Querying which timezone the system is configured to use by default
 *   (fulfilled by the [TimeZoneIdProvider] interface).
 * - Obtaining the transitions corresponding to a specific timezone from
 *   the timezone database (the [TimeZoneDatabase] interface).
 *
 * In addition to combining these two interfaces,
 * the new [currentTimeZone] function is provided that
 * is in most cases equivalent to `get(currentSystemDefaultId())`,
 * but can additionally return a useful timezone
 * even when the [TimeZoneDatabase] does not recognize the timezone identifier.
 */
public interface TimeZoneContext: TimeZoneDatabase, TimeZoneIdProvider {
    /**
     * Get the current system timezone.
     *
     * In most cases, this is equivalent to `get(currentSystemDefaultId())`.
     * However, it is designed to also work in scenarios
     * when the timezone database does not recognize
     * the timezone identifier returned by [currentTimeZoneId] if possible.
     */
    public fun currentTimeZone(): TimeZone = get(currentTimeZoneId())

    /**
     * The system's timezone context.
     *
     * It is encouraged to avoid direct calls to [System] methods in your code
     * in favor of dependency injection of a [TimeZoneContext] instance,
     * or if only a part of that functionality is needed,
     * of either a [TimeZoneDatabase] or a [TimeZoneIdProvider] instance.
     * When writing library code, this is a strict requirement.
     */
    public object System: TimeZoneContext {
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
         * How exactly the region-based time zone is acquired is system-dependent.
         * The current implementation:
         * - JVM: `java.time.ZoneId.of(zoneId)` is used.
         * - Kotlin/Native:
         *     - Darwin: the timezone database in a predefined location
         *       (`/var/db/timezone/zoneinfo` for devices, `/usr/share/zoneinfo.default`
         *       for simulators) is used by default.
         *       If it is not a valid timezone database,
         *       the same search procedure as on Linux is used.
         *       If that also doesn't yield a valid timezone database,
         *       the Foundation framework APIs are used to obtain the timezone rules.
         *     - Linux: `/usr/share/zoneinfo`, `/usr/share/lib/zoneinfo`,
         *       and `/etc/zoneinfo`
         *       are checked in turn for the timezone database.
         *       If none of them is a valid timezone database,
         *       `/etc/localtime` is checked.
         *       If it is a symlink of the form `.../zoneinfo/...`,
         *       the target of the symlink with the last part stripped is used
         *       as the timezone database.
         *     - Windows: the contents of the
         *       `HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Time Zones`
         *       registry key are queried to obtain the timezone database.
         *       Then, the Windows-specific timezone name is mapped to the corresponding
         *       IANA identifier using the information from the CLDR project:
         *       https://github.com/unicode-org/cldr/blob/main/common/supplemental/windowsZones.xml
         * - JavaScript and Wasm/JS:
         *   if the `@js-joda/timezone` library is loaded,
         *   it is used to obtain the timezone rules.
         *   Otherwise, the [IllegalTimeZoneException] is thrown.
         *   See https://github.com/Kotlin/kotlinx-datetime/blob/master/README.md#note-about-time-zones-in-js
         * - Wasm/WASI:
         *   if the `kotlinx-datetime-zoneinfo` artifact is added to the project
         *   as a dependency, it is used to obtain the timezone rules.
         *   Otherwise, the [IllegalTimeZoneException] is thrown.
         *
         * @throws IllegalTimeZoneException if [id] has an invalid format or a time zone
         * with the identifier [id] is not found.
         * @throws RuntimeException if the timezone database is corrupted or not recognized by `kotlinx-datetime`.
         * @see getOrNull for a function that returns `null` if a time zone is not available.
         */
        override fun get(id: String): TimeZone = systemTimezoneDatabase.get(id)

        /**
         * Returns the time zone identified by the provided [id], or `null` if it's not found.
         *
         * This is an optimized shorthand for `try { get(id) } catch (_: IllegalTimeZoneException) { null }`.
         * Please see the documentation for [get].
         *
         * Note that non-[IllegalTimeZoneException] exceptions will still be thrown by this function,
         * as they don't indicate that a timezone is missing but imply a runtime error.
         */
        override fun getOrNull(id: String): TimeZone? = systemTimezoneDatabase.getOrNull(id)

        /**
         * Returns the set of timezone identifiers the system timezone
         * database recognizes in addition to the fixed-offset ones.
         */
        override fun availableZoneIds(): Set<String> =
            systemTimezoneDatabase.availableZoneIds()

        /**
         * Get the current name of the system timezone.
         *
         * In most cases, this is equivalent to taking the [TimeZone.id]
         * of [currentTimeZone], but not always.
         *
         * - On Kotlin/JS and Wasm/JS, this function will return the timezone name
         *   even if the `@js-joda/timezone` library is not loaded.
         * - On Kotlin/Native for Windows, this function will return the timezone name
         *   according to the IANA timezone database naming scheme instead of
         *   the *Windows-specific* conventions.
         *   For example, if the user chose `W. Europe Standard Time`,
         *   then `Europe/Berlin` will be returned.
         */
        override fun currentTimeZoneId(): String =
            systemTimeZoneIdProvider.currentTimeZoneId()

        /**
         * Get the current system timezone.
         *
         * - On the JVM and Kotlin/Native Linux, this is equivalent to
         *   `get(currentSystemDefaultId())`.
         * - On Kotlin/Native:
         *   - On Darwin targets, this is the timezone returned by `NSTimeZone.system`.
         *   - On Windows, `get(currentSystemDefaultId())` is returned, unless
         *     the "Automatically adjust clock for Daylight Saving Time" checkbox
         *     in the system settings is disabled.
         *     In that case, a fixed-offset timezone
         *     corresponding to the user preferences
         *     will be returned instead.
         * - On Kotlin/JS and Wasm/JS, this function checks if the `@js-joda/timezone`
         *   library is loaded.
         *   If it is, it will be used to obtain the timezone corresponding
         *   to [currentTimeZoneId].
         *   If not, a synthetic [TimeZone] instance
         *   using the `Date()` objects for timezone arithmetics will be returned.
         *   This behavior may change in the future once JS runtimes provide access to
         *   the system timezone database.
         * - On Wasm/WASI, this is always `UTC`.
         *   This behavior may change in the future once Wasm/WASI provides access to
         *   the system timezone.
         *
         * If the system is reconfigured to use another default timezone,
         * the [TimeZone] object returned from this
         * function will not react to the change
         * (except for Kotlin/JS and Wasm/JS when `@js-joda/timezone` is not loaded).
         * Periodically re-checking the result of [currentTimeZone] is required
         * to obtain the up-to-date information.
         *
         * @throws RuntimeException if a system misconfiguration prevents obtaining
         * the default system timezone.
         */
        override fun currentTimeZone(): TimeZone = currentSystemDefaultTimeZone()
    }

    /** @suppress */
    public companion object
}
