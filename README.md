# kotlinx-datetime

[![JetBrains incubator project](https://jb.gg/badges/incubator.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub) 
[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](http://www.apache.org/licenses/LICENSE-2.0) 
<!-- [ ![Download](https://api.bintray.com/packages/kotlin/kotlinx/kotlinx.datetime/images/download.svg) ](https://bintray.com/kotlin/kotlinx/kotlinx.datetime/_latestVersion) -->


A multiplatform Kotlin library for working with date and time.

## Types and operations

The library provides the basic set of types for working with date and time:

- `Instant` to represent a moment on the UTC-SLS time scale;
- `LocalDateTime` to represent the date and time components without a reference to the particular time zone; 
- `LocalDate` to represent the components of date only;
- `TimeZone` and `ZoneOffset` provide time zone information to convert between `Instant` and `LocalDateTime`;
- `Month` and `DayOfWeek` enums;
- `CalendarPeriod` to represent a difference between two instants decomposed into calendar units. The latter are
  listed in `CalendarUnit` enum.
  