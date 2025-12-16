# Contributing Guidelines

The main way to help `kotlinx-datetime` is to describe use cases that
the library doesn't cover well or at all.

## Submitting issues

Bug reports, feature requests, usability complaints, quality-of-life
suggestions are all welcome!
Submit issues [here](https://github.com/Kotlin/kotlinx-datetime/issues).

Questions about usage and general inquiries are better suited for [StackOverflow](https://stackoverflow.com)
or the `#kotlinx-datetime` channel in [KotlinLang Slack](https://surveys.jetbrains.com/s3/kotlin-slack-sign-up).

## Submitting PRs

* Pull requests that improve code samples or documentation are very welcome.
* We reject pull requests that introduce new APIs.
  Please describe your use case under an existing issue or open a new one
  instead of going straight to implementing a new API in `kotlinx-datetime`.
  - Datetime APIs are fraught with peril.
    What may look like a small convenience function may hide an incorrect
    behavior that will surface once a year.
  - The core Kotlin libraries go for a consistent naming scheme that is
    tricky to coordinate with third-party contributors.
* Do send us pull requests with bugfixes!
  Those should be accompanied by a test verifying the correct behavior.
