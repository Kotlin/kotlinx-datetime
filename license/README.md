The Apache 2 license (given in full in [LICENSE.txt](../LICENSE.txt)) applies to all code in this repository which is copyright
by JetBrains s.r.o. and contributors. The following sections of the repository contain third-party code, to which different licenses
may apply:

- Path: `core/common/src/internal/dateCalculations.kt`
    - Origin: implementation of date/time calculations is based on ThreeTen backport project.
    - License: BSD 3-Clause ([license/thirdparty/threetenbp_license.txt][threetenbp])

- Path: `core/commonKotlin/src`
    - Origin: implementation of date/time entities is based on ThreeTen backport project.
    - License: BSD 3-Clause ([license/thirdparty/threetenbp_license.txt][threetenbp])

- Path: `core/commonKotlin/src`
    - Origin: Derived from tests of ThreeTen backport project
    - License: BSD 3-Clause ([license/thirdparty/threetenbp_license.txt][threetenbp])

- Path: `core/commonTest/src`
    - Origin: Some tests are derived from tests of ThreeTen backport project
    - License: BSD 3-Clause ([license/thirdparty/threetenbp_license.txt][threetenbp])

- Path: `core/windows/src/internal/WindowsZoneNames.kt`
    - Origin: time zone name mappings for Windows are generated from
      https://raw.githubusercontent.com/unicode-org/cldr/master/common/supplemental/windowsZones.xml
    - License: Unicode ([license/thirdparty/unicode_license.txt](thirdparty/unicode_license.txt))

- Path: `core/androidNative/src`
    - Origin: implementation is based on the bionic project.
    - License: BSD ([license/thirdparty/bionic_license.txt](thirdparty/bionic_license.txt))

- Path: `core/commonJs/src`
    - Origin: implementation accesses the internals of js-joda.
    - License: BSD ([license/thirdparty/js-joda_license.txt](thirdparty/js-joda_license.txt))

[threetenbp]: thirdparty/threetenbp_license.txt
