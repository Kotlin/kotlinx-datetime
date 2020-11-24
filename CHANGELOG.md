# CHANGELOG

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
