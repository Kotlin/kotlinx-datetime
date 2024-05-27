# kotlinx-datetime

[![Kotlin Alpha](https://kotl.in/badges/alpha.svg)](https://kotlinlang.org/docs/components-stability.html)
[![JetBrains official project](https://jb.gg/badges/official.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub) 
[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](http://www.apache.org/licenses/LICENSE-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/org.jetbrains.kotlinx/kotlinx-datetime.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22org.jetbrains.kotlinx%22%20AND%20a:%22kotlinx-datetime%22)
[![Kotlin](https://img.shields.io/badge/kotlin-1.9.21-blue.svg?logo=kotlin)](http://kotlinlang.org)
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

- `Instant` to represent a moment on the UTC-SLS time scale;
- `Clock` to obtain the current instant;
- `LocalDateTime` to represent date and time components without a reference to the particular time zone; 
- `LocalDate` to represent the components of date only;
- `LocalTime` to represent the components of time only;
- `TimeZone` and `FixedOffsetTimeZone` provide time zone information to convert between `Instant` and `LocalDateTime`;
- `Month` and `DayOfWeek` enums;
- `DateTimePeriod` to represent a difference between two instants decomposed into date and time units;
- `DatePeriod` is a subclass of `DateTimePeriod` with zero time components,
it represents a difference between two LocalDate values decomposed into date units.
- `DateTimeUnit` provides a set of predefined date and time units to use in arithmetic operations on `Instant` and `LocalDate`. 
- `UtcOffset` represents the amount of time the local date/time at a particular time zone differs from the date/time at UTC.

### Type use-cases

Here is some basic advice on how to choose which of the date-carrying types to use in what cases:

- Use `Instant` to represent a timestamp of the event that had already happened in the past (like a timestamp of 
  a log entry) or will definitely happen in a well-defined instant of time in the future not far away from now 
  (like an order confirmation deadline in 1 hour from now).
  
- Use `LocalDateTime` to represent a time of the event that is scheduled to happen in the far future at a certain 
  local time (like a scheduled meeting in a few months from now). You'll have to keep track of the `TimeZone` of 
  the scheduled event separately. Try to avoid converting future events to `Instant` in advance, because time-zone 
  rules might change unexpectedly in the future. In this [blog post](https://codeblog.jonskeet.uk/2019/03/27/storing-utc-is-not-a-silver-bullet/), you can read more about why it's not always 
  a good idea to use `Instant` everywhere.
  
  Also, use `LocalDateTime` to decode an `Instant` to its local date-time components for display and UIs.
  
- Use `LocalDate` to represent the date of an event that does not have a specific time associated with it (like a birth date).

- Use `LocalTime` to represent the time of an event that does not have a specific date associated with it.
 
## Operations

With the above types you can get the following operations done.


### Getting the current moment of time

The current moment of time can be captured with the `Instant` type. 
To obtain an `Instant` corresponding to the current moment of time, 
use `now()` function of the `Clock` interface:

```kotlin
val clock: Clock = ...
val currentMoment = clock.now()
```

An instance of `Clock` can be injected through the function/class parameters, 
or you can use its default implementation `Clock.System` that represents the system clock:

```kotlin
val currentMoment = Clock.System.now()
```


### Converting an instant to local date and time components

An `Instant` is just a counter of high resolution time intervals since the beginning of time scale.
To get human readable components from an `Instant` value, you need to convert it to the `LocalDateTime`
type that represents date and time components without a reference to the particular time zone.

The `TimeZone` type provides the rules to convert instants from and to date/time components.

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

### Converting instant to and from unix time

An `Instant` can be converted to a number of milliseconds since the Unix/POSIX epoch with the `toEpochMilliseconds()` function.
To convert back, use the companion object function `Instant.fromEpochMilliseconds(Long)`.

### Converting instant and local date/time to and from the ISO 8601 string

`Instant`, `LocalDateTime`, `LocalDate` and `LocalTime` provide shortcuts for
parsing and formatting them using the extended ISO 8601 format.
The `toString()` function is used to convert the value to a string in that format, and 
the `parse` function in companion object is used to parse a string representation back. 

```kotlin
val instantNow = Clock.System.now()
instantNow.toString()  // returns something like 2015-12-31T12:30:00Z
val instantBefore = Instant.parse("2010-06-01T22:19:44.475Z")
```

`LocalDateTime` uses a similar format, but without `Z` UTC time zone designator in the end.

`LocalDate` uses a format with just year, month, and date components, e.g. `2010-06-01`.

`LocalTime` uses a format with just hour, minute, second and (if non-zero) nanosecond components, e.g. `12:01:03`.

```kotlin
LocalDateTime.parse("2010-06-01T22:19:44")
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
collection of all date-time fields, can be used instead.

```kotlin
// import kotlinx.datetime.format.*

val yearMonth = DateTimeComponents.Format { year(); char('-'); monthNumber() }
    .parse("2024-01")
println(yearMonth.year)
println(yearMonth.monthNumber)

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

```kotlin
val now = Clock.System.now()
val instantInThePast: Instant = Instant.parse("2020-01-01T00:00:00Z")
val durationSinceThen: Duration = now - instantInThePast
val equidistantInstantInTheFuture: Instant = now + durationSinceThen
```

`Duration` is a type from the experimental `kotlin.time` package in the Kotlin standard library.
This type holds the amount of time that can be represented in different time units: from nanoseconds to 24H days.

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

A particular amount of date/time units or a date/time period can be added to an `Instant` with the `plus` function:

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

The implementation of date/time types, such as `Instant`, `LocalDateTime`, `TimeZone` and so on, relies on:

- in JVM: [`java.time`](https://docs.oracle.com/javase/8/docs/api/java/time/package-summary.html) API;
- in Js and Wasm-Js: [`js-joda`](https://js-joda.github.io/js-joda/) library;
- in Native: based on the [ThreeTen backport project](https://www.threeten.org/threetenbp/)
  - time zone support is provided by [date](https://github.com/HowardHinnant/date/) C++ library;

## Known/open issues, work TBD

- [x] Some kind of `Clock` interface is needed as a pluggable replacement for `Instant.now()`.
- [ ] Flexible locale-neutral parsing and formatting facilities are needed to support various date/time interchange
  formats that are used in practice (in particular, various RFCs).

## Using in your projects

> Note that the library is experimental, and the API is subject to change.

The library is published to Maven Central.

The library is compatible with the Kotlin Standard Library not lower than `1.9.0`.

If you target Android devices running **below API 26**, you need to use Android Gradle plugin 4.0 or newer 
and enable [core library desugaring](https://developer.android.com/studio/write/java8-support#library-desugaring).

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
                 implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
             }
        }
    }
}
```

- To use the library in a single-platform project, add a dependency to the dependencies block.

```groovy
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
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

private val jsJodaTz = JsJodaTimeZoneModule
```

### Maven

Add a dependency to the `<dependencies>` element. Note that you need to use the platform-specific `-jvm` artifact in Maven.

```xml
<dependency>
    <groupId>org.jetbrains.kotlinx</groupId>
    <artifactId>kotlinx-datetime-jvm</artifactId>
    <version>0.6.0</version>
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
