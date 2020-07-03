# kotlinx-datetime

[![JetBrains incubator project](https://jb.gg/badges/incubator.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub) 
[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](http://www.apache.org/licenses/LICENSE-2.0) 
<!-- [ ![Download](https://api.bintray.com/packages/kotlin/kotlinx/kotlinx.datetime/images/download.svg) ](https://bintray.com/kotlin/kotlinx/kotlinx.datetime/_latestVersion) -->


A multiplatform Kotlin library for working with date and time.

## Design overview

There are a few guiding principles in the design of `kotlinx-datetime`. First of all, it is pragmatic, focused
on the most common problems developers face every day (pun intended) when working with dates and times. It is not
all-encompassing and lacks some domain-specific utilities that special-purpose applications might need.
We chose convenience over generality, so the API surface this library provides is as minimal as possible
to meet the use-cases.

The library puts a clear boundary between physical time of an instant and a local, time-zone dependent civil time, 
consisting of components such as year, month, etc that people use when talking about time. 
We intentionally avoid entities in the library that mix both together and could be misused.
However, there are convenience operations that take, for example, a physical instant and perform a calendar-based 
adjustment (such as adding a month); all such operation
explicitly take a time-zone information as parameter to clearly state that their result depends on the civil time-zone
rules which are subject to change at any time.

The library is based on the ISO 8601 international standard, other ways to represent dates and times are out of
its scope. Internationalization (such as locale-specific month and day names) is out the scope, too. 

## Types

The library provides the basic set of types for working with date and time:

- `Instant` to represent a moment on the UTC-SLS time scale;
- `LocalDateTime` to represent date and time components without a reference to the particular time zone; 
- `LocalDate` to represent the components of date only;
- `TimeZone` and `ZoneOffset` provide time zone information to convert between `Instant` and `LocalDateTime`;
- `Month` and `DayOfWeek` enums;
- `CalendarPeriod` to represent a difference between two instants decomposed into calendar units. The latter are
  listed in `CalendarUnit` enum.
  
### Type use-cases

Here is some basic advice on how to choose which of the date-carrying types to use in what cases:

- Use `Instant` to represent a timestamp of the event that had already happened in the past (like a timestamp of 
  a log entry) or will definitely happen in a well-defined instant of time in the future not far away from now 
  (like an order confirmation deadline in 1 hour from now).
- Use `LocalDateTime` to represent a time of the event that is scheduled to happen in the far future at a certain 
  local time (like a scheduled meeting in a few months from now). You'll have to keep track of the `TimeZone` of 
  the scheduled event separately. Try to avoid converting future events to `Instant` in advance, because time-zone 
  rules might change unexpectedly in the future. 
  Also, use `LocalDateTime` to decode an `Instant` to its local date-time components for display and UIs.
- Use `LocalDate` to represent a date of the event that does not have a specific time associated with it (like a birthday).
 
## Operations

With the above types you can get the following operations done.


### Getting the current moment of time

The current moment of time can be captured with the `Instant` type. 
Use `Instant.now()` function in its companion object.

```kotlin
val currentMoment = Instant.now()
```

### Converting an instant to local date and time components

`Instant` is just a counter of high resolution time intervals since the beginning of time scale.
To get human readable components from an `Instant` value you need to convert it to `LocalDateTime` type
that represents date and time components without a reference to the particular time zone.

The `TimeZone` type provides the rules to convert instants from and to date/time components.

```kotlin
val currentMoment: Instant = Instant.now()
val datetimeInUtc: LocalDateTime = currentMoment.toLocalDateTime(TimeZone.UTC)
val datetimeInSystemZone: LocalDateTime = currentMoment.toLocalDateTime(TimeZone.SYSTEM)
```

`LocalDateTime` instance exposes familiar components of the Gregorian calendar: 
`year`, `month`, `dayOfMonth`, `hour`, and so on up to `nanosecond`.
The property `dayOfWeek` shows what weekday that date is,
and `dayOfYear` shows the day number since the beginning of a year.


Additional time zones can be acquired by their string identifier with the `TimeZone.of(id: String)` function.
```kotlin
val tzBerlin = TimeZone.of("Europe/Berlin")
val datetimeInBerlin = currentMoment.toLocalDateTime(tzBerlin)
```

`LocalDateTime` instance can be constructed from individual components:

```kotlin
val kotlinReleaseDateTime = LocalDateTime(2016, 2, 15, 16, 57, 0, 0)
```

An instant can be obtained from `LocalDateTime` by interpreting it as a time moment
in a particular `TimeZone`:

```kotlin
val kotlinReleaseInstant = kotlinReleaseDateTime.toInstant(TimeZone.of("UTC+3"))
```

### Getting local date components

`LocalDate` type represents local date without time. You can obtain it from `Instant`
by converting it to `LocalDateTime` and taking its `date` property.

```kotlin
val now: Instant = Instant.now()
val today: LocalDate = now.toLocalDateTime(TimeZone.SYSTEM).date
```
Note, that today's date really depends on the time zone in which you're observing the current moment.

`LocalDate` can be constructed from three components, year, month, and day:
```kotlin
val knownDate = LocalDate(2020, 2, 21)
```

### Converting instant to and from unix time

An `Instant` can be converted to unix millisecond time with the `toUnixMillis()` function.
To convert back use `Instant.fromUnixMillis(Long)` companion object function.

### Converting instant and local date/time to and from string

Currently, `Instant`, `LocalDateTime`, and `LocalDate` only support ISO-8601 format.
The `toString()` function is used to convert the value to a string in that format, and 
the `parse` function in companion object is used to parse a string representation back. 


```kotlin
val instantNow = Instant.now()
instantNow.toString()  // returns something like 2015-12-31T12:30:00Z
val instantBefore = Instant.parse("2010-06-01T22:19:44.475Z")
```

Alternatively, `String.to...()` extension functions can be used instead of `parse`, 
where it feels more convenient:

`LocalDateTime` uses the similar format, but without `Z` UTC time zone designator in the end.

`LocalDate` uses format with just year, month, and date components, e.g. `2010-06-01`.

```kotlin
"2010-06-01T22:19:44.475Z".toInstant()
"2010-06-01T22:19:44".toLocalDateTime()
"2010-06-01".toLocalDate()
```

### Instant arithmetic

```kotlin
val now = Instant.now()
val instantInThePast: Instant = Instant.parse("2020-01-01T00:00:00Z")
val durationSinceThen: Duration = now - instantInThePast
val equidistantInstantInTheFuture: Instant = now + durationSinceThen
```

`Duration` is a type from the experimental `kotlin.time` package in the Kotlin standard library.
This type holds the amount of time that can be represented in different time units: from nanoseconds to 24H days.

To get the calendar difference between two instants you can use `Instant.periodUntil(Instant, TimeZone)` function.

```kotlin
val period: CalendarPeriod = instantInThePast.periodUntil(Instant.now(), TimeZone.UTC)
```

`CalendarPeriod` represents a difference between two particular moments as a sum of calendar components, 
like "2 years, 3 months, 10 days, and 22 hours".

The difference can be calculated as an integer amount of specified calendar units:

```kotlin
val diffInMonths = instantInThePast.until(Instant.now(), CalendarUnit.MONTH, TimeZone.UTC)
```
There are also shortcuts `yearsUntil(...)`, `monthsUntil(...)`, and `daysUntil(...)`.

A particular amount of calendar units or a calendar period can be added to an `Instant` with the `plus` function:

```kotlin
val now = Instant.now()
val tomorrow = now.plus(1, CalendarUnit.DAY, TimeZone.SYSTEM)
val threeYearsAndAMonthLater = now.plus(CalendarPeriod(years = 3, months = 1), TimeZone.SYSTEM)
```

Note that `plus` and `...until` operations require `TimeZone` as a parameter because the calendar interval between 
two particular instants can be different, when calculated in different time zones.

### Date arithmetic

The similar operations with calendar units are provided for `LocalDate` type:

- `LocalDate.plus(number, CalendarUnit)`
- `LocalDate.plus(CalendarPeriod)`
- `LocalDate.periodUntil(LocalDate)` and `LocalDate.minus(LocalDate)`

## Implementation

The implementation of date/time types, such as `Instant`, `LocalDateTime`, `TimeZone` and so on, relies on:

- [`java.time`](https://docs.oracle.com/javase/8/docs/api/java/time/package-summary.html) API in JVM;
- [`js-joda`](https://js-joda.github.io/js-joda/) library in JS;
- based on [ThreeTen backport project](https://www.threeten.org/threetenbp/) in Native.

## Known/open issues, work TBD

* Some kind of `Clock` interface is needed as a pluggable replacement for `Instant.now()`.
* Flexible locale-neutral parsing and formatting facilities are needed to support various date/time interchange
  formats that are used in practice (in particular, various RFCs).
* An alternative JVM implementation for Android might be needed.  

## Building

Before building, ensure that you have [thirdparty/date](thirdparty/date) submodule initialized and updated. 
IDEA does that automatically when cloning the repository, and if you cloned it in the command line, you may need
to run additionally:

```kotlin
git submodule init
git submodule update
```

The path to JDK 8 must be specified either with the environment variable `JDK_8` or 
with the gradle property `JDK_8`. For local builds, you can use a later version of JDK if you don't have that 
version installed.

After that, the project can be opened in IDEA and built with Gradle.
