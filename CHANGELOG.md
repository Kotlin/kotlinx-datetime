# CHANGELOG

## 0.3.0

#### Features

- Added `iosSimulatorArm64`, `watchosSimulatorArm64`, `tvosSimulatorArm64`, `macosArm64` target support ([141](https://github.com/Kotlin/kotlinx-datetime/issues/141), [144](https://github.com/Kotlin/kotlinx-datetime/issues/144)).

#### Changes

- `ZoneOffset` was replaced by two other classes: `FixedOffsetTimeZone`, which represents a time zone with a fixed offset, and `UtcOffset`, which represents just the UTC offset ([PR#125](https://github.com/Kotlin/kotlinx-datetime/pull/125)).
- The `DayBased` and `MonthBased` subclasses of `DateTimeUnit.DateBased` are now accessed as `DateTimeUnit.DayBased` and `DateTimeUnit.MonthBased` as opposed to `DateTimeUnit.DateBased.DayBased` and `DateTimeUnit.DateBased.MonthBased` respectively ([PR#131](https://github.com/Kotlin/kotlinx-datetime/pull/131)).

## 0.2.1

#### Fixes

- Fixed the library being incompatible with kotlinx.serialization 1.2.0 and above ([#118](https://github.com/Kotlin/kotlinx-datetime/issues/118)).

#### Features

- `watchosX64` target support. In practice, this means the ability to run projects that depend on this library in the iOS Simulator for Apple Watch.

## 0.2.0

#### Fixes

- Fixed `TimeZone.currentSystemDefault()` crashing on Darwin if the resulting time zone is not listed among `TimeZone.knownTimeZoneIdentifiers` ([#94](https://github.com/Kotlin/kotlinx-datetime/issues/94))

#### Features

- `kotlinx-serialization` support ([#37](https://github.com/Kotlin/kotlinx-datetime/issues/37))
- Normalization of `DateTimePeriod` components, meaning that periods that are semantically equivalent are considered equal ([#81](https://github.com/Kotlin/kotlinx-datetime/issues/81))
- `Instant` can now be parsed from an ISO-8601 string with an offset other than `Z` ([#56](https://github.com/Kotlin/kotlinx-datetime/issues/56))

## 0.1.1

#### Fixes
 
- Fix a crash when getting the current time on iOS 9 ([#52](https://github.com/Kotlin/kotlinx-datetime/issues/52))
- Wrong answers in some cases when adding date-based units to instants on Darwin and Windows ([#51](https://github.com/Kotlin/kotlinx-datetime/issues/51)) 

#### Features

- Zone-agnostic time-based arithmetic on Instants, e.g. `Instant.plus(value, DateTimeUnit.TimeBased)`
- Add `Instant.fromEpochSeconds(epochSeconds: Long, nanosecondAdjustment: Int)` construction function
- Introduce `minus` operations complementary to existing `plus` arithmetic operations ([#42](https://github.com/Kotlin/kotlinx-datetime/issues/42))

## 0.1.0

#### Initial implementation 

A minimal, but still valuable multiplatform implementation of date and time types.
