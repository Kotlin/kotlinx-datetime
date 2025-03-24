/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package testcontainers

enum class ContainerType(val dockerfilePath: String, val imageName: String) {
    // Standard Debian Jessie container with Arctic/Longyearbyen timezone
    JESSIE_CORRECT(
        "./jvm/test/testcontainers/debian-jessie/correct-config.dockerfile",
        "debian-jessie-timezone-test"
    ),

    // Debian Jessie container with default system configuration
    JESSIE_DEFAULT(
        "./jvm/test/testcontainers/debian-jessie/default-config.dockerfile",
        "debian-jessie-timezone-test-default"
    ),

    // Debian Jessie container with missing /etc/localtime file
    JESSIE_MISSING_LOCALTIME(
        "./jvm/test/testcontainers/debian-jessie/missing-localtime.dockerfile",
        "debian-jessie-timezone-test-missing-localtime"
    ),

    // Debian Jessie container with missing /etc/timezone file
    JESSIE_MISSING_TIMEZONE(
        "./jvm/test/testcontainers/debian-jessie/missing-timezone.dockerfile",
        "debian-jessie-timezone-test-missing-timezone"
    ),

    // Debian Jessie container with garbage inside /etc/timezone file
    JESSIE_INCORRECT_TIMEZONE(
        "./jvm/test/testcontainers/debian-jessie/incorrect-timezone.dockerfile",
        "debian-jessie-timezone-test-incorrect-timezone"
    ),

    // Debian Jessie container with conflicting timezone configurations
    // `/etc/localtime` and `/usr/share/zoneinfo/$(cat /etc/timezone)` files are different
    JESSIE_DIFFERENT_TIMEZONES(
        "./jvm/test/testcontainers/debian-jessie/different-timezones.dockerfile",
        "debian-jessie-timezone-test-different-timezones"
    ),

    // Standard Ubuntu Noble container with Arctic/Longyearbyen timezone
    NOBLE_CORRECT(
        "./jvm/test/testcontainers/ubuntu-noble/correct-config.dockerfile",
        "ubuntu-noble-timezone-test"
    ),

    // Ubuntu Noble container with default system configuration
    NOBLE_DEFAULT(
        "./jvm/test/testcontainers/ubuntu-noble/default-config.dockerfile",
        "ubuntu-noble-timezone-test-default"
    ),

    // Ubuntu Noble container with incorrect symbolic link for /etc/localtime
    NOBLE_INCORRECT_SYMLINK(
        "./jvm/test/testcontainers/ubuntu-noble/incorrect-symlink.dockerfile",
        "ubuntu-noble-timezone-test-incorrect-symlink"
    )
}