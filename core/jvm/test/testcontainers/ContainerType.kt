/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package testcontainers

enum class ContainerType(val dockerfilePath: String, val imageName: String) {
    // Standard Jessie with Arctic/Longyearbyen timezone
    JESSIE_CORRECT(
        "./jvm/test/testcontainers/debian-jessie/correct-config.dockerfile",
        "debian-jessie-timezone-test"
    ),

    JESSIE_DEFAULT(
        "./jvm/test/testcontainers/debian-jessie/default-config.dockerfile",
        "debian-jessie-timezone-test-default"
    ),

    JESSIE_MISSING_LOCALTIME(
        "./jvm/test/testcontainers/debian-jessie/missing-localtime.dockerfile",
        "debian-jessie-timezone-test-missing-localtime"
    ),

    // Standard Noble with Arctic/Longyearbyen timezone
    NOBLE_CORRECT(
        "./jvm/test/testcontainers/ubuntu-noble/correct-config.dockerfile",
        "ubuntu-noble-timezone-test"
    ),

    NOBLE_DEFAULT(
        "./jvm/test/testcontainers/ubuntu-noble/default-config.dockerfile",
        "ubuntu-noble-timezone-test-default"
    ),

    NOBLE_INCORRECT_SYMLINK(
        "./jvm/test/testcontainers/ubuntu-noble/incorrect-symlink.dockerfile",
        "ubuntu-noble-timezone-test-incorrect-symlink"
    )
}