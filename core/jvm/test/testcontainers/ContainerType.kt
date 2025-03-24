/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package testcontainers

enum class ContainerType(val dockerfilePath: String, val imageName: String) {
    // Standard Jessie with Arctic/Longyearbyen timezone
    DEBIAN_JESSIE_CORRECT(
        "./jvm/test/testcontainers/debian-jessie/correct-config.dockerfile",
        "debian-jessie-timezone-test"
    ),

    DEBIAN_JESSIE_DEFAULT(
        "./jvm/test/testcontainers/debian-jessie/default-config.dockerfile",
        "debian-jessie-timezone-test-default"
    ),

    DEBIAN_JESSIE_MISSING_LOCALTIME(
        "./jvm/test/testcontainers/debian-jessie/missing-localtime.dockerfile",
        "debian-jessie-timezone-test-missing-localtime"
    ),

    // Standard Noble with Arctic/Longyearbyen timezone
    UBUNTU_NOBLE_CORRECT(
        "./jvm/test/testcontainers/ubuntu-noble/correct-config.dockerfile",
        "ubuntu-noble-timezone-test"
    ),

    UBUNTU_NOBLE_DEFAULT(
        "./jvm/test/testcontainers/ubuntu-noble/default-config.dockerfile",
        "ubuntu-noble-timezone-test-default"
    )
}