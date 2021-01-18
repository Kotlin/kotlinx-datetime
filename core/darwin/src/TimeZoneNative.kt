/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import platform.Foundation.*

private fun dateWithTimeIntervalSince1970Saturating(epochSeconds: Long): NSDate {
    val date = NSDate.dateWithTimeIntervalSince1970(epochSeconds.toDouble())
    return when {
        date.timeIntervalSinceDate(NSDate.distantPast) < 0 -> NSDate.distantPast
        date.timeIntervalSinceDate(NSDate.distantFuture) > 0 -> NSDate.distantFuture
        else -> date
    }
}

private fun systemDateByLocalDate(zone: NSTimeZone, localDate: NSDate): NSDate? {
    val iso8601 = NSCalendar.calendarWithIdentifier(NSCalendarIdentifierISO8601)!!
    val utc = NSTimeZone.timeZoneForSecondsFromGMT(0)
    /* Now, we say that the date that we initially meant is `date`, only with
       the context of being in a timezone `zone`. */
    val dateComponents = iso8601.componentsInTimeZone(utc, localDate)
    dateComponents.timeZone = zone
    return iso8601.dateFromComponents(dateComponents)
}

internal actual class PlatformTimeZoneImpl(private val value: NSTimeZone, override val id: String): TimeZoneImpl {
    actual companion object {
        actual fun of(zoneId: String): PlatformTimeZoneImpl {
            val abbreviations = NSTimeZone.abbreviationDictionary
            val trueZoneId = abbreviations[zoneId] as String? ?: zoneId
            val zone = NSTimeZone.timeZoneWithName(trueZoneId)
                ?: throw IllegalTimeZoneException("No timezone found with zone ID '$zoneId'")
            return PlatformTimeZoneImpl(zone, zoneId)
        }

        actual fun currentSystemDefault(): PlatformTimeZoneImpl {
            /* The framework has its own cache of the system timezone. Calls to
            [NSTimeZone systemTimeZone] do not reflect changes to the system timezone
            and instead just return the cached value. Thus, to acquire the current
            system timezone, first, the cache should be cleared.

            This solution is not without flaws, however. In particular, resetting the
            system timezone also resets the default timezone ([NSTimeZone default]) if
            it's the same as the cached system timezone:

                NSTimeZone.defaultTimeZone = [NSTimeZone
                    timeZoneWithName: [[NSTimeZone systemTimeZone] name]];
                NSLog(@"%@", NSTimeZone.defaultTimeZone.name);
                NSLog(@"Change the system time zone, then press Enter");
                getchar();
                [NSTimeZone resetSystemTimeZone];
                NSLog(@"%@", NSTimeZone.defaultTimeZone.name); // will also change

            This is a fairly marginal problem:
                * It is only a problem when the developer deliberately sets the default
                  timezone to the region that just happens to be the one that the user
                  is in, and then the user moves to another region, and the app also
                  uses the system timezone.
                * Since iOS 11, the significance of the default timezone has been
                  de-emphasized. In particular, it is not included in the API for
                  Swift: https://forums.swift.org/t/autoupdating-type-properties/4608/4

            Another possible solution could involve using [NSTimeZone localTimeZone].
            This is documented to reflect the current, uncached system timezone on
            iOS 11 and later:
            https://developer.apple.com/documentation/foundation/nstimezone/1387209-localtimezone
            However:
                * Before iOS 11, this was the same as the default timezone and did not
                  reflect the system timezone.
                * Worse, on a Mac (10.15.5), I failed to get it to work as documented.
                      NSLog(@"%@", NSTimeZone.localTimeZone.name);
                      NSLog(@"Change the system time zone, then press Enter");
                      getchar();
                      // [NSTimeZone resetSystemTimeZone]; // uncomment to make it work
                      NSLog(@"%@", NSTimeZone.localTimeZone.name);
                  The printed strings are the same even if I wait for good 10 minutes
                  before pressing Enter, unless the line with "reset" is uncommented--
                  then the timezone is updated, as it should be. So, for some reason,
                  NSTimeZone.localTimeZone, too, is cached.
                  With no iOS device to test this on, it doesn't seem worth the effort
                  to avoid just resetting the system timezone due to one edge case
                  that's hard to avoid.
            */
            NSTimeZone.resetSystemTimeZone()
            val zone = NSTimeZone.systemTimeZone
            return PlatformTimeZoneImpl(zone, zone.name)
        }

        actual val availableZoneIds: Set<String>
            get() {
                val set = mutableSetOf("UTC")
                val zones = NSTimeZone.knownTimeZoneNames
                for (zone in zones) {
                    if (zone is NSString) {
                        set.add(zone as String)
                    } else throw RuntimeException("$zone is expected to be NSString")
                }
                val abbrevs = NSTimeZone.abbreviationDictionary
                for ((key, value) in abbrevs) {
                    if (key is NSString && value is NSString) {
                        if (set.contains(value as String)) {
                            set.add(key as String)
                        }
                    } else throw RuntimeException("$key and $value are expected to be NSString")
                }
                return set
            }
    }

    override fun atStartOfDay(date: LocalDate): Instant {
        val ldt = LocalDateTime(date, LocalTime.MIN)
        val epochSeconds = ldt.toEpochSecond(ZoneOffsetImpl.UTC)
        // timezone
        val nsDate = NSDate.dateWithTimeIntervalSince1970(epochSeconds.toDouble())
        val newDate = systemDateByLocalDate(value, nsDate)
            ?: throw RuntimeException("Unable to acquire the time of start of day at $nsDate for zone $this")
        val offset = value.secondsFromGMTForDate(newDate).toInt()
        /* if `epoch_sec` is not in the range supported by Darwin, assume that it
           is the correct local time for the midnight and just convert it to
           the system time. */
        if (nsDate.timeIntervalSinceDate(NSDate.distantPast) < 0 ||
            nsDate.timeIntervalSinceDate(NSDate.distantFuture) > 0)
            return Instant(epochSeconds - offset, 0)
        // The ISO-8601 calendar.
        val iso8601 = NSCalendar.calendarWithIdentifier(NSCalendarIdentifierISO8601)!!
        iso8601.timeZone = value
        // start of the day denoted by `newDate`
        val midnight = iso8601.startOfDayForDate(newDate)
        return Instant(midnight.timeIntervalSince1970.toLong(), 0)
    }

    override fun LocalDateTime.atZone(preferred: ZoneOffsetImpl?): ZonedDateTime {
        val epochSeconds = toEpochSecond(ZoneOffsetImpl.UTC)
        var offset = preferred?.totalSeconds ?: Int.MAX_VALUE
        val transitionDuration = run {
            /* a date in an unspecified timezone, defined by the number of seconds since
               the start of the epoch in *that* unspecified timezone */
            val date = dateWithTimeIntervalSince1970Saturating(epochSeconds)
            val newDate = systemDateByLocalDate(value, date)
                ?: throw RuntimeException("Unable to acquire the offset at ${this@atZone} for zone ${this@PlatformTimeZoneImpl}")
            // we now know the offset of that timezone at this time.
            offset = value.secondsFromGMTForDate(newDate).toInt()
            /* `dateFromComponents` automatically corrects the date to avoid gaps. We
               need to learn which adjustments it performed. */
            (newDate.timeIntervalSince1970.toLong() +
                offset.toLong() - date.timeIntervalSince1970.toLong()).toInt()
        }
        val dateTime = try {
            this@atZone.plusSeconds(transitionDuration)
        } catch (e: IllegalArgumentException) {
            throw DateTimeArithmeticException("Overflow whet correcting the date-time to not be in the transition gap", e)
        } catch (e: ArithmeticException) {
            throw RuntimeException("Anomalously long timezone transition gap reported", e)
        }
        return ZonedDateTime(dateTime, TimeZone(this@PlatformTimeZoneImpl), ZoneOffset.ofSeconds(offset).offset)
    }

    override fun offsetAt(instant: Instant): ZoneOffsetImpl {
        val date = dateWithTimeIntervalSince1970Saturating(instant.epochSeconds)
        return ZoneOffset.ofSeconds(value.secondsFromGMTForDate(date).toInt()).offset
    }

    // org.threeten.bp.ZoneId#equals
    override fun equals(other: Any?): Boolean =
        this === other || other is PlatformTimeZoneImpl && this.id == other.id

    // org.threeten.bp.ZoneId#hashCode
    override fun hashCode(): Int = id.hashCode()

    // org.threeten.bp.ZoneId#toString
    override fun toString(): String = id
}

internal actual fun currentTime(): Instant = NSDate.date().toKotlinInstant()
