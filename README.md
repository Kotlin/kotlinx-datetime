# kotlinx-datetime

[![Kotlin Alpha](https://kotl.in/badges/alpha.svg)](https://kotlinlang.org/docs/components-stability.html)
[![JetBrains official project](https://jb.gg/badges/official.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](http://www.apache.org/licenses/LICENSE-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/org.jetbrains.kotlinx/kotlinx-datetime?filter=0.7.1)](https://search.maven.org/search?q=g:org.jetbrains.kotlinx%20AND%20a:kotlinx-datetime)
[![Kotlin](https://img.shields.io/badge/kotlin-2.1.20-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![KDoc link](https://img.shields.io/badge/API_reference-KDoc-blue)](https://kotlinlang.org/api/kotlinx-datetime/)
[![Slack channel](https://img.shields.io/badge/chat-slack-blue.svg?logo=slack)](https://kotlinlang.slack.com/messages/kotlinx-datetime/)
[![TeamCity build](https://img.shields.io/teamcity/build/s/KotlinTools_KotlinxDatetime_Build_All.svg?server=http%3A%2F%2Fteamcity.jetbrains.com)](https://teamcity.jetbrains.com/viewType.html?buildTypeId=KotlinTools_KotlinxDatetime_Build_All&guest=1)

A multiplatform Kotlin library for working with date and time.

See [Using in your projects](#using-in-your-projects) for the instructions how to setup a dependency in your project.

## Design overview

There are a few guiding principles in the design of `kotlinx-datetime`. First of all, it is pragmatic, focused
on the most common problems developers face every day (pun intended) when working with dates and times. It is not
all-encompassing and lacks some domain-specific utilities that special-purpose applications might need.
We chose convenience over generality, so the API surface this library provides is as minimal as possible
to meet the use-cases.

The library puts a clear boundary between the physical time of an instant and the local, time-zone dependent
civil time, consisting of components such as year, month, etc that people use when talking about time.
We intentionally avoid entities in the library that mix both together and could be misused.
However, there are convenience operations that take, for example, a physical instant and perform a calendar-based
adjustment (such as adding a month); all such operations
explicitly take a time-zone information as parameter to clearly state that their result depends on the civil time-zone
rules which are subject to change at any time.

The library is based on the ISO 8601 international standard, other ways to represent dates and times are out of
its scope. Internationalization (such as locale-specific month and day names) is out the scope, too.

## Types

The library provides a basic set of types for working with date and time:

- `LocalDateTime` to represent date and time components without a reference to the particular time zone;
- `LocalDate` to represent the components of date only;
- `YearMonth` to represent only the year and month components;
- `LocalTime` to represent the components of time only;
- `TimeZone` and `FixedOffsetTimeZone` provide time zone information to convert between
  `kotlin.time.Instant` and `LocalDateTime`;
- `Month` and `DayOfWeek` enums;
- `DateTimePeriod` to represent a difference between two instants decomposed into date and time units;
- `DatePeriod` is a subclass of `DateTimePeriod` with zero time components,
it represents a difference between two LocalDate values decomposed into date units.
- `DateTimeUnit` provides a set of predefined date and time units to use in arithmetic operations
  on `kotlin.time.Instant` and `LocalDate`.
- `UtcOffset` represents the amount of time the local datetime at a particular time zone differs from the datetime at UTC.

### Type use-cases

Here is some basic advice on how to choose which of the date-carrying types to use in what cases:

- Use `kotlin.time.Instant` to represent a timestamp of the event that had already happened in the past
  (like a timestamp of a log entry) or will definitely happen in a well-defined instant of time in the future
  not far away from now
  (like an order confirmation deadline in 1 hour from now).

- Use `LocalDateTime` to represent a time of the event that is scheduled to happen in the far future at a certain
  local time (like a scheduled meeting in a few months from now). You'll have to keep track of the `TimeZone` of
  the scheduled event separately. Try to avoid converting future events to `Instant` in advance, because time-zone
  rules might change unexpectedly in the future. In this [blog post](https://codeblog.jonskeet.uk/2019/03/27/storing-utc-is-not-a-silver-bullet/), you can read more about why it's not always
  a good idea to use `Instant` everywhere.

  Also use `LocalDateTime` to decode an `Instant` to its local datetime components for display and UIs.

- Use `LocalDate` to represent the date of an event that does not have a specific time associated with it (like a birth date).

- Use `YearMonth` to represent the year and month of an event that does not have a specific day associated with it
  or has a day-of-month that is inferred from the context (like a credit card expiration date).

- Use `LocalTime` to represent the time of an event that does not have a specific date associated with it.

## Operations

With the above types you can get the following operations done.

### Converting an instant to local date and time components

An `Instant` is just a counter of high resolution time intervals since the beginning of time scale.
To get human readable components from an `Instant` value, you need to convert it to the `LocalDateTime`
type that represents date and time components without a reference to the particular time zone.

The `TimeZone` type provides the rules to convert instants from and to datetime components.

```kotlin
val currentMoment: Instant = Clock.System.now()
val datetimeInUtc: LocalDateTime = currentMoment.toLocalDateTime(TimeZone.UTC)
val datetimeInSystemZone: LocalDateTime = currentMoment.toLocalDateTime(TimeZone.currentSystemDefault())
```

A `LocalDateTime` instance exposes familiar components of the Gregorian calendar:
`year`, `month`, `day`, `hour`, and so on up to `nanosecond`.
The property `dayOfWeek` shows what weekday that date is,
and `dayOfYear` shows the day number since the beginning of a year.


Additional time zones can be acquired by their string identifier with the `TimeZone.of(id: String)` function.
```kotlin
val tzBerlin = TimeZone.of("Europe/Berlin")
val datetimeInBerlin = currentMoment.toLocalDateTime(tzBerlin)
```

A `LocalDateTime` instance can be constructed from individual components:

```kotlin
val kotlinReleaseDateTime = LocalDateTime(2016, 2, 15, 16, 57, 0, 0)
```

An instant can be obtained from `LocalDateTime` by interpreting it as a time moment
in a particular `TimeZone`:

```kotlin
val kotlinReleaseInstant = kotlinReleaseDateTime.toInstant(TimeZone.of("UTC+3"))
```

### Getting local date components

A `LocalDate` represents a local date without time. You can obtain one from an `Instant`
by converting it to `LocalDateTime` and taking its `date` property.

```kotlin
val now: Instant = Clock.System.now()
val today: LocalDate = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
// or shorter
val today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
```
Note, that today's date really depends on the time zone in which you're observing the current moment.

`LocalDate` can be constructed from three components, year, month, and day:
```kotlin
val knownDate = LocalDate(2020, 2, 21)
```

### Getting year and month components

A `YearMonth` represents a year and month without a day. You can obtain one from a `LocalDate`
by taking its `yearMonth` property.

```kotlin
val day = LocalDate(2020, 2, 21)
val yearMonth: YearMonth = day.yearMonth
```

### Getting local time components

A `LocalTime` represents local time without date. You can obtain one from an `Instant`
by converting it to `LocalDateTime` and taking its `time` property.

```kotlin
val now: Instant = Clock.System.now()
val thisTime: LocalTime = now.toLocalDateTime(TimeZone.currentSystemDefault()).time
```

A `LocalTime` can be constructed from four components, hour, minute, second and nanosecond:
```kotlin
val knownTime = LocalTime(hour = 23, minute = 59, second = 12)
val timeWithNanos = LocalTime(hour = 23, minute = 59, second = 12, nanosecond = 999)
val hourMinute = LocalTime(hour = 12, minute = 13)
```

### Converting instant and local datetime to and from the ISO 8601 string

`Instant`, `LocalDateTime`, `LocalDate` and `LocalTime` provide shortcuts for
parsing and formatting them using the extended ISO 8601 format.
The `toString()` function is used to convert the value to a string in that format, and
the `parse` function in companion object is used to parse a string representation back.

```kotlin
val localDateTime = LocalDateTime(2025, 3, 21, 12, 27, 35, 124365453)
localDateTime.toString()  // 2025-03-21T12:27:35.124365453
val sameLocalDateTime = LocalDateTime.parse("2025-03-21T12:27:35.124365453")
```

`LocalDate` uses a format with just year, month, and date components, e.g. `2010-06-01`.

`LocalTime` uses a format with just hour, minute, second and (if non-zero) nanosecond components, e.g. `12:01:03`.

```kotlin
LocalDate.parse("2010-06-01")
LocalTime.parse("12:01:03")
LocalTime.parse("12:00:03.999")
LocalTime.parse("12:0:03.999") // fails with an IllegalArgumentException
```

### Working with other string formats

When some data needs to be formatted in some format other than ISO 8601, one
can define their own format or use some of the predefined ones:

```kotlin
// import kotlinx.datetime.format.*

val dateFormat = LocalDate.Format {
    monthNumber(padding = Padding.SPACE)
    char('/')
    day()
    char(' ')
    year()
}

val date = dateFormat.parse("12/24 2023")
println(date.format(LocalDate.Formats.ISO_BASIC)) // "20231224"
```

#### Using Unicode format strings (like `yyyy-MM-dd`)

Given a constant format string like the ones used by Java's
[DateTimeFormatter.ofPattern](https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html) can be
converted to Kotlin code using the following invocation:

```kotlin
// import kotlinx.datetime.format.*

println(DateTimeFormat.formatAsKotlinBuilderDsl(DateTimeComponents.Format {
    byUnicodePattern("uuuu-MM-dd'T'HH:mm:ss[.SSS]Z")
}))

// will print:
/*
date(LocalDate.Formats.ISO)
char('T')
hour()
char(':')
minute()
char(':')
second()
alternativeParsing({
}) {
    char('.')
    secondFraction(3)
}
offset(UtcOffset.Formats.FOUR_DIGITS)
 */
```

When your format string is not constant, with the `FormatStringsInDatetimeFormats` opt-in,
you can use the format without converting it to Kotlin code beforehand:

```kotlin
val formatPattern = "yyyy-MM-dd'T'HH:mm:ss[.SSS]"

@OptIn(FormatStringsInDatetimeFormats::class)
val dateTimeFormat = LocalDateTime.Format {
    byUnicodePattern(formatPattern)
}

dateTimeFormat.parse("2023-12-24T23:59:59")
```

### Parsing and formatting partial, compound or out-of-bounds data

Sometimes, the required string format doesn't fully correspond to any of the
classes `kotlinx-datetime` provides. In these cases, `DateTimeComponents`, a
collection of all datetime fields, can be used instead.

```kotlin
// import kotlinx.datetime.format.*

val monthDay = DateTimeComponents.Format { monthNumber(); char('/'); day() }
    .parse("12/25")
println(monthDay.day) // 25
println(monthDay.monthNumber) // 12

val dateTimeOffset = DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET
    .parse("2023-01-07T23:16:15.53+02:00")
println(dateTimeOffset.toUtcOffset()) // +02:00
println(dateTimeOffset.toLocalDateTime()) // 2023-01-07T23:16:15.53
```

Occasionally, one can encounter strings where the values are slightly off:
for example, `23:59:60`, where `60` is an invalid value for the second.
`DateTimeComponents` allows parsing such values as well and then mutating them
before conversion.

```kotlin
val time = DateTimeComponents.Format { time(LocalTime.Formats.ISO) }
    .parse("23:59:60").apply {
        if (second == 60) second = 59
    }.toLocalTime()
println(time) // 23:59:59
```

Because `DateTimeComponents` is provided specifically for parsing and
formatting, there is no way to construct it normally. If one needs to format
partial, complex or out-of-bounds data, the `format` function allows building
`DateTimeComponents` specifically for formatting it:

```kotlin
DateTimeComponents.Formats.RFC_1123.format {
    // the receiver of this lambda is DateTimeComponents
    setDate(LocalDate(2023, 1, 7))
    hour = 23
    minute = 59
    second = 60
    setOffset(UtcOffset(hours = 2))
} // Sat, 7 Jan 2023 23:59:60 +0200
```

### Instant arithmetic

The `Instant` arithmetic operations that don't involve the calendar are available purely in the standard library
and do not require using `kotlinx-datetime`:

```kotlin
val now = Clock.System.now()
val instantInThePast: Instant = Instant.parse("2020-01-01T00:00:00Z")
val durationSinceThen: Duration = now - instantInThePast
val equidistantInstantInTheFuture: Instant = now + durationSinceThen
```

To get the calendar difference between two instants you can use the `Instant.periodUntil(Instant, TimeZone)` function.

```kotlin
val period: DateTimePeriod = instantInThePast.periodUntil(Clock.System.now(), TimeZone.UTC)
```

A `DateTimePeriod` represents a difference between two particular moments as a sum of calendar components,
like "2 years, 3 months, 10 days, and 22 hours".

The difference can be calculated as an integer amount of specified date or time units:

```kotlin
val diffInMonths = instantInThePast.until(Clock.System.now(), DateTimeUnit.MONTH, TimeZone.UTC)
```
There are also shortcuts `yearsUntil(...)`, `monthsUntil(...)`, and `daysUntil(...)`.

A particular amount of datetime units or a datetime period can be added to an `Instant` with the `plus` function:

```kotlin
val now = Clock.System.now()
val systemTZ = TimeZone.currentSystemDefault()
val tomorrow = now.plus(2, DateTimeUnit.DAY, systemTZ)
val threeYearsAndAMonthLater = now.plus(DateTimePeriod(years = 3, months = 1), systemTZ)
```

Note that `plus` and `...until` operations require a `TimeZone` as a parameter because the calendar interval between
two particular instants can be different, when calculated in different time zones.

### Date arithmetic

Similar operations with date units are provided for `LocalDate` type:

- `LocalDate.plus(number, DateTimeUnit.DateBased)`
- `LocalDate.plus(DatePeriod)`
- `LocalDate.until(LocalDate, DateTimeUnit.DateBased)` and the shortcuts `yearsUntil`, `monthUntil`, `daysUntil`
- `LocalDate.periodUntil(LocalDate): DatePeriod` and `LocalDate.minus(LocalDate): DatePeriod`

Notice that, instead of the general `DateTimeUnit` and `DateTimePeriod` types, we're using their subtypes
`DateTimeUnit.DateBased` and `DatePeriod` respectively. This allows preventing the situations when
time components are being added to a date at compile time.

### Date + time arithmetic

Arithmetic on `LocalDateTime` is intentionally omitted. The reason for this is that the presence of daylight saving time
transitions (changing from standard time to daylight saving time and back) causes `LocalDateTime` arithmetic to be
ill-defined. For example, consider time gaps (or, as [`dst` tag wiki on Stack Overflow](https://stackoverflow.com/tags/dst/info)
calls them, "spring forward" transitions), that is, ranges of date + time combinations that never occur in a given
time zone due to clocks moving forward. If we allowed `LocalDateTime` arithmetic that ignored time zones, then it
could result in `LocalDateTime` instances that are inside a time gap and are invalid in the implied time zone.

Therefore, the recommended way to use a `LocalDateTime` is to treat it as a representation of an `Instant`,
perform all the required arithmetic on `Instant` values, and only convert to `LocalDateTime` when a human-readable
representation is needed.

```kotlin
val timeZone = TimeZone.of("Europe/Berlin")
val localDateTime = LocalDateTime.parse("2021-03-27T02:16:20")
val instant = localDateTime.toInstant(timeZone)

val instantOneDayLater = instant.plus(1, DateTimeUnit.DAY, timeZone)
val localDateTimeOneDayLater = instantOneDayLater.toLocalDateTime(timeZone)
// 2021-03-28T03:16:20, as 02:16:20 that day is in a time gap

val instantTwoDaysLater = instant.plus(2, DateTimeUnit.DAY, timeZone)
val localDateTimeTwoDaysLater = instantTwoDaysLater.toLocalDateTime(timeZone)
// 2021-03-29T02:16:20
```

## Implementation

The implementation of datetime types, such as `LocalDateTime`, `TimeZone` and so on, relies on:

- in JVM: [`java.time`](https://docs.oracle.com/javase/8/docs/api/java/time/package-summary.html) API;
- the other platforms: based on the [ThreeTen backport project](https://www.threeten.org/threetenbp/)
  - time zone support on JS and Wasm/JS is provided by the [`js-joda`](https://js-joda.github.io/js-joda/) library.

## Known/open issues, work TBD

- [ ] Flexible locale-neutral parsing and formatting facilities are needed to support various datetime interchange
  formats that are used in practice (in particular, various RFCs).

## Using in your projects

> Note that the library is experimental, and the API is subject to change.

The library is published to Maven Central.

The library is compatible with the Kotlin Standard Library not lower than `2.1.20`.

If you target Android devices running **below API 26**, you need to use Android Gradle plugin 4.0 or newer
and enable [core library desugaring](https://developer.android.com/studio/write/java8-support#library-desugaring).

### Deprecation of `Instant`

`kotlinx-datetime` versions earlier than `0.7.0` used to provide `kotlinx.datetime.Instant`
and `kotlinx.datetime.Clock`.
The Kotlin standard library started including its own, identical `kotlin.time.Instant` and `kotlin.time.Clock`,
as it became evident that `Instant` was also useful outside the datetime contexts.

Here is the recommended procedure for migrating from `kotlinx-datetime` version `0.6.x` or earlier to `0.7.x`:

* First, simply try upgrading to `0.7.1`.
  If your project has a dependency on `kotlinx-datetime`, but doesn't have dependencies on other libraries that are
  *themselves* reliant on an older `kotlinx-datetime`, you are good to go: the code should compile and run.
  This applies both to applications and to libraries!
* If your project depends on other libraries that themselves use an older version of `kotlinx-datetime`,
  then your code may fail at runtime with a `ClassNotFoundException`
  for `kotlinx.datetime.Instant` or `kotlinx.datetime.Clock`, or maybe even fail to compile.
  In that case, please check if the affected libraries you have as dependencies have already published a new release
  adapted to use `Instant` and `Clock` from `kotlin.time`.
* If you use `kotlinx-serialization` to serialize the `Instant` type, update that dependency to use
  [1.9.0](https://github.com/Kotlin/kotlinx.serialization/releases/tag/v1.9.0) or a newer version.
* If all else fails, use the *compatibility release* of `kotlinx-datetime`.
  Instead of the version `0.7.1`, use `0.7.1-0.6.x-compat`.
  This artifact still contains `kotlinx.datetime.Instant` and `kotlinx.datetime.Clock`,
  ensuring that third-party libraries reliant on them can still be used.
  This artifact is less straightforward to use than `0.7.1`, so only resort to it when libraries you don't control
  require that the removed classes still exist.

Tips for fixing compilation errors:

* If you encounter resolution ambiguity errors for `Instant` or `Clock`,
  see if you have `import kotlin.time.*` along with `import kotlinx.datetime.*`.
  Since both libraries have a `Clock` and an `Instant`, you have to manually add
  `import kotlin.time.Instant` and/or `import kotlin.time.Clock` explicitly.
* When using the compatibility release of `kotlinx-datetime`, you may encounter errors like
  "required `kotlinx.datetime.Instant`, found `kotlin.time.Instant`" or vice versa.
  - First, please check if you have imported a `kotlinx.datetime` class when a `kotlin.time` class would work.
    The final goal is getting rid of `kotlinx.datetime.Instant`, so limit its usage as much as possible!
  - If you have no choice but to use an `Instant` or `Clock` from `kotlinx-datetime` (for example, because a third-party
    library accepts a `kotlinx.datetime.Instant` as a parameter or returns it as a function result),
    you can use the compatibility functions:
    * `kotlin.time.Instant.toDeprecatedInstant(): kotlinx.datetime.Instant`
    * `kotlin.time.Clock.toDeprecatedClock(): kotlinx.datetime.Clock`
    * `kotlinx.datetime.Instant.toStdlibInstant(): kotlin.time.Instant`
    * `kotlinx.datetime.Clock.toStdlibClock(): kotlin.time.Clock`

> Compatibility releases will be published for all `0.7.x` versions of `kotlinx-datetime`, but not longer.

### Gradle

- Add the Maven Central repository if it is not already there:

```kotlin
repositories {
    mavenCentral()
}
```

- In multiplatform projects, add a dependency to the commonMain source set dependencies
```kotlin
kotlin {
    sourceSets {
        commonMain {
             dependencies {
                 implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1")
             }
        }
    }
}
```

- To use the library in a single-platform project, add a dependency to the dependencies block.

```groovy
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1")
}
```

#### Note about time zones in JS

By default, there's only one time zone available in Kotlin/JS: the `SYSTEM` time zone with a fixed offset.

If you want to use all time zones in Kotlin/JS platform, you need to add the following npm dependency:

```kotlin
kotlin {
    sourceSets {
        val jsMain by getting {
            dependencies {
                implementation(npm("@js-joda/timezone", "2.3.0"))
            }
        }
    }
}
```

and after that add the following initialization code in your project:

```kotlin
@JsModule("@js-joda/timezone")
@JsNonModule
external object JsJodaTimeZoneModule

@OptIn(ExperimentalJsExport::class)
@JsExport
val jsJodaTz = JsJodaTimeZoneModule
```

#### Note about time zones in Wasm/JS

Wasm/JS uses the same time zone support as JS, so almost the same instructions apply.

In your Gradle build script, add the following dependency:

```kotlin
kotlin {
    sourceSets {
        val wasmJsMain by getting {
            dependencies {
                implementation(npm("@js-joda/timezone", "2.3.0"))
            }
        }
    }
}
```

and after that add the following initialization code in your project:

```kotlin
@JsModule("@js-joda/timezone")
external object JsJodaTimeZoneModule

private val jsJodaTz = JsJodaTimeZoneModule
```

#### Note about time zones in Wasm/WASI

By default, there's only one time zone available in Kotlin/Wasm WASI: the `UTC` time zone with a fixed offset.

If you want to use all time zones in Kotlin/Wasm WASI platform, you need to add the following dependency:

```kotlin
kotlin {
    sourceSets {
        val wasmWasiMain by getting {
            dependencies {
                implementation("kotlinx-datetime-zoneinfo", "2025b-spi.0.7.1")
            }
        }
    }
}
```

### Maven

Add a dependency to the `<dependencies>` element. Note that you need to use the platform-specific `-jvm` artifact in Maven.

```xml
<dependency>
    <groupId>org.jetbrains.kotlinx</groupId>
    <artifactId>kotlinx-datetime-jvm</artifactId>
    <version>0.7.1</version>
</dependency>
```

## Building

The project requires JDK 8 to build classes and to run tests.
Gradle will try to find it among the installed JDKs or [provision](https://docs.gradle.org/current/userguide/toolchains.html#sec:provisioning) it automatically if it couldn't be found.
The path to JDK 8 can be additionally specified with the environment variable `JDK_8`.
For local builds, you can use a later version of JDK if you don't have that
version installed. Specify the version of this JDK with the `java.mainToolchainVersion` Gradle property.

After that, the project can be opened in IDEA and built with Gradle.

For building and running benchmarks, see [README.md](benchmarks/README.md)
