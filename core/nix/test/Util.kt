/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test

// od --format=x1 --output-duplicates --address-radix=n --width=16 /usr/share/zoneinfo/Europe/Berlin |
// sed -e 's/\b\(\w\)/0x\1/g' -e 's/\(\w\)\b/\1,/g'
// Do not remove the type annotation, otherwise the compiler slows down to a crawl for this file even more.
// This constant is in a separate file to avoid recompiling it on every change to the test file, which is slow to the
// point of freezing the IDE.
internal val EuropeBerlinTzFile = listOf<Int>(
    0x54, 0x5a, 0x69, 0x66, 0x32, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x09, 0x00, 0x00, 0x00, 0x09, 0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x8f, 0x00, 0x00, 0x00, 0x09, 0x00, 0x00, 0x00, 0x12, 0x80, 0x00, 0x00, 0x00,
    0x9b, 0x0c, 0x17, 0x60, 0x9b, 0xd5, 0xda, 0xf0, 0x9c, 0xd9, 0xae, 0x90, 0x9d, 0xa4, 0xb5, 0x90,
    0x9e, 0xb9, 0x90, 0x90, 0x9f, 0x84, 0x97, 0x90, 0xc8, 0x09, 0x71, 0x90, 0xcc, 0xe7, 0x4b, 0x10,
    0xcd, 0xa9, 0x17, 0x90, 0xce, 0xa2, 0x43, 0x10, 0xcf, 0x92, 0x34, 0x10, 0xd0, 0x82, 0x25, 0x10,
    0xd1, 0x72, 0x16, 0x10, 0xd1, 0xb6, 0x96, 0x00, 0xd2, 0x58, 0xbe, 0x80, 0xd2, 0xa1, 0x4f, 0x10,
    0xd3, 0x63, 0x1b, 0x90, 0xd4, 0x4b, 0x23, 0x90, 0xd5, 0x39, 0xd1, 0x20, 0xd5, 0x67, 0xe7, 0x90,
    0xd5, 0xa8, 0x73, 0x00, 0xd6, 0x29, 0xb4, 0x10, 0xd7, 0x2c, 0x1a, 0x10, 0xd8, 0x09, 0x96, 0x10,
    0xd9, 0x02, 0xc1, 0x90, 0xd9, 0xe9, 0x78, 0x10, 0x13, 0x4d, 0x44, 0x10, 0x14, 0x33, 0xfa, 0x90,
    0x15, 0x23, 0xeb, 0x90, 0x16, 0x13, 0xdc, 0x90, 0x17, 0x03, 0xcd, 0x90, 0x17, 0xf3, 0xbe, 0x90,
    0x18, 0xe3, 0xaf, 0x90, 0x19, 0xd3, 0xa0, 0x90, 0x1a, 0xc3, 0x91, 0x90, 0x1b, 0xbc, 0xbd, 0x10,
    0x1c, 0xac, 0xae, 0x10, 0x1d, 0x9c, 0x9f, 0x10, 0x1e, 0x8c, 0x90, 0x10, 0x1f, 0x7c, 0x81, 0x10,
    0x20, 0x6c, 0x72, 0x10, 0x21, 0x5c, 0x63, 0x10, 0x22, 0x4c, 0x54, 0x10, 0x23, 0x3c, 0x45, 0x10,
    0x24, 0x2c, 0x36, 0x10, 0x25, 0x1c, 0x27, 0x10, 0x26, 0x0c, 0x18, 0x10, 0x27, 0x05, 0x43, 0x90,
    0x27, 0xf5, 0x34, 0x90, 0x28, 0xe5, 0x25, 0x90, 0x29, 0xd5, 0x16, 0x90, 0x2a, 0xc5, 0x07, 0x90,
    0x2b, 0xb4, 0xf8, 0x90, 0x2c, 0xa4, 0xe9, 0x90, 0x2d, 0x94, 0xda, 0x90, 0x2e, 0x84, 0xcb, 0x90,
    0x2f, 0x74, 0xbc, 0x90, 0x30, 0x64, 0xad, 0x90, 0x31, 0x5d, 0xd9, 0x10, 0x32, 0x72, 0xb4, 0x10,
    0x33, 0x3d, 0xbb, 0x10, 0x34, 0x52, 0x96, 0x10, 0x35, 0x1d, 0x9d, 0x10, 0x36, 0x32, 0x78, 0x10,
    0x36, 0xfd, 0x7f, 0x10, 0x38, 0x1b, 0x94, 0x90, 0x38, 0xdd, 0x61, 0x10, 0x39, 0xfb, 0x76, 0x90,
    0x3a, 0xbd, 0x43, 0x10, 0x3b, 0xdb, 0x58, 0x90, 0x3c, 0xa6, 0x5f, 0x90, 0x3d, 0xbb, 0x3a, 0x90,
    0x3e, 0x86, 0x41, 0x90, 0x3f, 0x9b, 0x1c, 0x90, 0x40, 0x66, 0x23, 0x90, 0x41, 0x84, 0x39, 0x10,
    0x42, 0x46, 0x05, 0x90, 0x43, 0x64, 0x1b, 0x10, 0x44, 0x25, 0xe7, 0x90, 0x45, 0x43, 0xfd, 0x10,
    0x46, 0x05, 0xc9, 0x90, 0x47, 0x23, 0xdf, 0x10, 0x47, 0xee, 0xe6, 0x10, 0x49, 0x03, 0xc1, 0x10,
    0x49, 0xce, 0xc8, 0x10, 0x4a, 0xe3, 0xa3, 0x10, 0x4b, 0xae, 0xaa, 0x10, 0x4c, 0xcc, 0xbf, 0x90,
    0x4d, 0x8e, 0x8c, 0x10, 0x4e, 0xac, 0xa1, 0x90, 0x4f, 0x6e, 0x6e, 0x10, 0x50, 0x8c, 0x83, 0x90,
    0x51, 0x57, 0x8a, 0x90, 0x52, 0x6c, 0x65, 0x90, 0x53, 0x37, 0x6c, 0x90, 0x54, 0x4c, 0x47, 0x90,
    0x55, 0x17, 0x4e, 0x90, 0x56, 0x2c, 0x29, 0x90, 0x56, 0xf7, 0x30, 0x90, 0x58, 0x15, 0x46, 0x10,
    0x58, 0xd7, 0x12, 0x90, 0x59, 0xf5, 0x28, 0x10, 0x5a, 0xb6, 0xf4, 0x90, 0x5b, 0xd5, 0x0a, 0x10,
    0x5c, 0xa0, 0x11, 0x10, 0x5d, 0xb4, 0xec, 0x10, 0x5e, 0x7f, 0xf3, 0x10, 0x5f, 0x94, 0xce, 0x10,
    0x60, 0x5f, 0xd5, 0x10, 0x61, 0x7d, 0xea, 0x90, 0x62, 0x3f, 0xb7, 0x10, 0x63, 0x5d, 0xcc, 0x90,
    0x64, 0x1f, 0x99, 0x10, 0x65, 0x3d, 0xae, 0x90, 0x66, 0x08, 0xb5, 0x90, 0x67, 0x1d, 0x90, 0x90,
    0x67, 0xe8, 0x97, 0x90, 0x68, 0xfd, 0x72, 0x90, 0x69, 0xc8, 0x79, 0x90, 0x6a, 0xdd, 0x54, 0x90,
    0x6b, 0xa8, 0x5b, 0x90, 0x6c, 0xc6, 0x71, 0x10, 0x6d, 0x88, 0x3d, 0x90, 0x6e, 0xa6, 0x53, 0x10,
    0x6f, 0x68, 0x1f, 0x90, 0x70, 0x86, 0x35, 0x10, 0x71, 0x51, 0x3c, 0x10, 0x72, 0x66, 0x17, 0x10,
    0x73, 0x31, 0x1e, 0x10, 0x74, 0x45, 0xf9, 0x10, 0x75, 0x11, 0x00, 0x10, 0x76, 0x2f, 0x15, 0x90,
    0x76, 0xf0, 0xe2, 0x10, 0x78, 0x0e, 0xf7, 0x90, 0x78, 0xd0, 0xc4, 0x10, 0x79, 0xee, 0xd9, 0x90,
    0x7a, 0xb0, 0xa6, 0x10, 0x7b, 0xce, 0xbb, 0x90, 0x7c, 0x99, 0xc2, 0x90, 0x7d, 0xae, 0x9d, 0x90,
    0x7e, 0x79, 0xa4, 0x90, 0x7f, 0x8e, 0x7f, 0x90, 0x02, 0x01, 0x02, 0x03, 0x04, 0x03, 0x04, 0x03,
    0x04, 0x03, 0x04, 0x03, 0x04, 0x03, 0x05, 0x01, 0x04, 0x03, 0x04, 0x03, 0x06, 0x01, 0x04, 0x03,
    0x04, 0x03, 0x04, 0x07, 0x08, 0x07, 0x08, 0x07, 0x08, 0x07, 0x08, 0x07, 0x08, 0x07, 0x08, 0x07,
    0x08, 0x07, 0x08, 0x07, 0x08, 0x07, 0x08, 0x07, 0x08, 0x07, 0x08, 0x07, 0x08, 0x07, 0x08, 0x07,
    0x08, 0x07, 0x08, 0x07, 0x08, 0x07, 0x08, 0x07, 0x08, 0x07, 0x08, 0x07, 0x08, 0x07, 0x08, 0x07,
    0x08, 0x07, 0x08, 0x07, 0x08, 0x07, 0x08, 0x07, 0x08, 0x07, 0x08, 0x07, 0x08, 0x07, 0x08, 0x07,
    0x08, 0x07, 0x08, 0x07, 0x08, 0x07, 0x08, 0x07, 0x08, 0x07, 0x08, 0x07, 0x08, 0x07, 0x08, 0x07,
    0x08, 0x07, 0x08, 0x07, 0x08, 0x07, 0x08, 0x07, 0x08, 0x07, 0x08, 0x07, 0x08, 0x07, 0x08, 0x07,
    0x08, 0x07, 0x08, 0x07, 0x08, 0x07, 0x08, 0x07, 0x08, 0x07, 0x08, 0x07, 0x08, 0x07, 0x08, 0x07,
    0x08, 0x07, 0x08, 0x07, 0x08, 0x07, 0x08, 0x00, 0x00, 0x0c, 0x88, 0x00, 0x00, 0x00, 0x00, 0x1c,
    0x20, 0x01, 0x04, 0x00, 0x00, 0x0e, 0x10, 0x00, 0x09, 0x00, 0x00, 0x1c, 0x20, 0x01, 0x04, 0x00,
    0x00, 0x0e, 0x10, 0x00, 0x09, 0x00, 0x00, 0x2a, 0x30, 0x01, 0x0d, 0x00, 0x00, 0x2a, 0x30, 0x01,
    0x0d, 0x00, 0x00, 0x1c, 0x20, 0x01, 0x04, 0x00, 0x00, 0x0e, 0x10, 0x00, 0x09, 0x4c, 0x4d, 0x54,
    0x00, 0x43, 0x45, 0x53, 0x54, 0x00, 0x43, 0x45, 0x54, 0x00, 0x43, 0x45, 0x4d, 0x54, 0x00, 0x00,
    0x00, 0x00, 0x01, 0x01, 0x00, 0x01, 0x01, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01,
    0x01, 0x54, 0x5a, 0x69, 0x66, 0x32, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x09, 0x00, 0x00, 0x00, 0x09, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00, 0x8f, 0x00, 0x00, 0x00, 0x09, 0x00, 0x00, 0x00, 0x12, 0xff, 0xff, 0xff,
    0xff, 0x6f, 0xa2, 0x61, 0xf8, 0xff, 0xff, 0xff, 0xff, 0x9b, 0x0c, 0x17, 0x60, 0xff, 0xff, 0xff,
    0xff, 0x9b, 0xd5, 0xda, 0xf0, 0xff, 0xff, 0xff, 0xff, 0x9c, 0xd9, 0xae, 0x90, 0xff, 0xff, 0xff,
    0xff, 0x9d, 0xa4, 0xb5, 0x90, 0xff, 0xff, 0xff, 0xff, 0x9e, 0xb9, 0x90, 0x90, 0xff, 0xff, 0xff,
    0xff, 0x9f, 0x84, 0x97, 0x90, 0xff, 0xff, 0xff, 0xff, 0xc8, 0x09, 0x71, 0x90, 0xff, 0xff, 0xff,
    0xff, 0xcc, 0xe7, 0x4b, 0x10, 0xff, 0xff, 0xff, 0xff, 0xcd, 0xa9, 0x17, 0x90, 0xff, 0xff, 0xff,
    0xff, 0xce, 0xa2, 0x43, 0x10, 0xff, 0xff, 0xff, 0xff, 0xcf, 0x92, 0x34, 0x10, 0xff, 0xff, 0xff,
    0xff, 0xd0, 0x82, 0x25, 0x10, 0xff, 0xff, 0xff, 0xff, 0xd1, 0x72, 0x16, 0x10, 0xff, 0xff, 0xff,
    0xff, 0xd1, 0xb6, 0x96, 0x00, 0xff, 0xff, 0xff, 0xff, 0xd2, 0x58, 0xbe, 0x80, 0xff, 0xff, 0xff,
    0xff, 0xd2, 0xa1, 0x4f, 0x10, 0xff, 0xff, 0xff, 0xff, 0xd3, 0x63, 0x1b, 0x90, 0xff, 0xff, 0xff,
    0xff, 0xd4, 0x4b, 0x23, 0x90, 0xff, 0xff, 0xff, 0xff, 0xd5, 0x39, 0xd1, 0x20, 0xff, 0xff, 0xff,
    0xff, 0xd5, 0x67, 0xe7, 0x90, 0xff, 0xff, 0xff, 0xff, 0xd5, 0xa8, 0x73, 0x00, 0xff, 0xff, 0xff,
    0xff, 0xd6, 0x29, 0xb4, 0x10, 0xff, 0xff, 0xff, 0xff, 0xd7, 0x2c, 0x1a, 0x10, 0xff, 0xff, 0xff,
    0xff, 0xd8, 0x09, 0x96, 0x10, 0xff, 0xff, 0xff, 0xff, 0xd9, 0x02, 0xc1, 0x90, 0xff, 0xff, 0xff,
    0xff, 0xd9, 0xe9, 0x78, 0x10, 0x00, 0x00, 0x00, 0x00, 0x13, 0x4d, 0x44, 0x10, 0x00, 0x00, 0x00,
    0x00, 0x14, 0x33, 0xfa, 0x90, 0x00, 0x00, 0x00, 0x00, 0x15, 0x23, 0xeb, 0x90, 0x00, 0x00, 0x00,
    0x00, 0x16, 0x13, 0xdc, 0x90, 0x00, 0x00, 0x00, 0x00, 0x17, 0x03, 0xcd, 0x90, 0x00, 0x00, 0x00,
    0x00, 0x17, 0xf3, 0xbe, 0x90, 0x00, 0x00, 0x00, 0x00, 0x18, 0xe3, 0xaf, 0x90, 0x00, 0x00, 0x00,
    0x00, 0x19, 0xd3, 0xa0, 0x90, 0x00, 0x00, 0x00, 0x00, 0x1a, 0xc3, 0x91, 0x90, 0x00, 0x00, 0x00,
    0x00, 0x1b, 0xbc, 0xbd, 0x10, 0x00, 0x00, 0x00, 0x00, 0x1c, 0xac, 0xae, 0x10, 0x00, 0x00, 0x00,
    0x00, 0x1d, 0x9c, 0x9f, 0x10, 0x00, 0x00, 0x00, 0x00, 0x1e, 0x8c, 0x90, 0x10, 0x00, 0x00, 0x00,
    0x00, 0x1f, 0x7c, 0x81, 0x10, 0x00, 0x00, 0x00, 0x00, 0x20, 0x6c, 0x72, 0x10, 0x00, 0x00, 0x00,
    0x00, 0x21, 0x5c, 0x63, 0x10, 0x00, 0x00, 0x00, 0x00, 0x22, 0x4c, 0x54, 0x10, 0x00, 0x00, 0x00,
    0x00, 0x23, 0x3c, 0x45, 0x10, 0x00, 0x00, 0x00, 0x00, 0x24, 0x2c, 0x36, 0x10, 0x00, 0x00, 0x00,
    0x00, 0x25, 0x1c, 0x27, 0x10, 0x00, 0x00, 0x00, 0x00, 0x26, 0x0c, 0x18, 0x10, 0x00, 0x00, 0x00,
    0x00, 0x27, 0x05, 0x43, 0x90, 0x00, 0x00, 0x00, 0x00, 0x27, 0xf5, 0x34, 0x90, 0x00, 0x00, 0x00,
    0x00, 0x28, 0xe5, 0x25, 0x90, 0x00, 0x00, 0x00, 0x00, 0x29, 0xd5, 0x16, 0x90, 0x00, 0x00, 0x00,
    0x00, 0x2a, 0xc5, 0x07, 0x90, 0x00, 0x00, 0x00, 0x00, 0x2b, 0xb4, 0xf8, 0x90, 0x00, 0x00, 0x00,
    0x00, 0x2c, 0xa4, 0xe9, 0x90, 0x00, 0x00, 0x00, 0x00, 0x2d, 0x94, 0xda, 0x90, 0x00, 0x00, 0x00,
    0x00, 0x2e, 0x84, 0xcb, 0x90, 0x00, 0x00, 0x00, 0x00, 0x2f, 0x74, 0xbc, 0x90, 0x00, 0x00, 0x00,
    0x00, 0x30, 0x64, 0xad, 0x90, 0x00, 0x00, 0x00, 0x00, 0x31, 0x5d, 0xd9, 0x10, 0x00, 0x00, 0x00,
    0x00, 0x32, 0x72, 0xb4, 0x10, 0x00, 0x00, 0x00, 0x00, 0x33, 0x3d, 0xbb, 0x10, 0x00, 0x00, 0x00,
    0x00, 0x34, 0x52, 0x96, 0x10, 0x00, 0x00, 0x00, 0x00, 0x35, 0x1d, 0x9d, 0x10, 0x00, 0x00, 0x00,
    0x00, 0x36, 0x32, 0x78, 0x10, 0x00, 0x00, 0x00, 0x00, 0x36, 0xfd, 0x7f, 0x10, 0x00, 0x00, 0x00,
    0x00, 0x38, 0x1b, 0x94, 0x90, 0x00, 0x00, 0x00, 0x00, 0x38, 0xdd, 0x61, 0x10, 0x00, 0x00, 0x00,
    0x00, 0x39, 0xfb, 0x76, 0x90, 0x00, 0x00, 0x00, 0x00, 0x3a, 0xbd, 0x43, 0x10, 0x00, 0x00, 0x00,
    0x00, 0x3b, 0xdb, 0x58, 0x90, 0x00, 0x00, 0x00, 0x00, 0x3c, 0xa6, 0x5f, 0x90, 0x00, 0x00, 0x00,
    0x00, 0x3d, 0xbb, 0x3a, 0x90, 0x00, 0x00, 0x00, 0x00, 0x3e, 0x86, 0x41, 0x90, 0x00, 0x00, 0x00,
    0x00, 0x3f, 0x9b, 0x1c, 0x90, 0x00, 0x00, 0x00, 0x00, 0x40, 0x66, 0x23, 0x90, 0x00, 0x00, 0x00,
    0x00, 0x41, 0x84, 0x39, 0x10, 0x00, 0x00, 0x00, 0x00, 0x42, 0x46, 0x05, 0x90, 0x00, 0x00, 0x00,
    0x00, 0x43, 0x64, 0x1b, 0x10, 0x00, 0x00, 0x00, 0x00, 0x44, 0x25, 0xe7, 0x90, 0x00, 0x00, 0x00,
    0x00, 0x45, 0x43, 0xfd, 0x10, 0x00, 0x00, 0x00, 0x00, 0x46, 0x05, 0xc9, 0x90, 0x00, 0x00, 0x00,
    0x00, 0x47, 0x23, 0xdf, 0x10, 0x00, 0x00, 0x00, 0x00, 0x47, 0xee, 0xe6, 0x10, 0x00, 0x00, 0x00,
    0x00, 0x49, 0x03, 0xc1, 0x10, 0x00, 0x00, 0x00, 0x00, 0x49, 0xce, 0xc8, 0x10, 0x00, 0x00, 0x00,
    0x00, 0x4a, 0xe3, 0xa3, 0x10, 0x00, 0x00, 0x00, 0x00, 0x4b, 0xae, 0xaa, 0x10, 0x00, 0x00, 0x00,
    0x00, 0x4c, 0xcc, 0xbf, 0x90, 0x00, 0x00, 0x00, 0x00, 0x4d, 0x8e, 0x8c, 0x10, 0x00, 0x00, 0x00,
    0x00, 0x4e, 0xac, 0xa1, 0x90, 0x00, 0x00, 0x00, 0x00, 0x4f, 0x6e, 0x6e, 0x10, 0x00, 0x00, 0x00,
    0x00, 0x50, 0x8c, 0x83, 0x90, 0x00, 0x00, 0x00, 0x00, 0x51, 0x57, 0x8a, 0x90, 0x00, 0x00, 0x00,
    0x00, 0x52, 0x6c, 0x65, 0x90, 0x00, 0x00, 0x00, 0x00, 0x53, 0x37, 0x6c, 0x90, 0x00, 0x00, 0x00,
    0x00, 0x54, 0x4c, 0x47, 0x90, 0x00, 0x00, 0x00, 0x00, 0x55, 0x17, 0x4e, 0x90, 0x00, 0x00, 0x00,
    0x00, 0x56, 0x2c, 0x29, 0x90, 0x00, 0x00, 0x00, 0x00, 0x56, 0xf7, 0x30, 0x90, 0x00, 0x00, 0x00,
    0x00, 0x58, 0x15, 0x46, 0x10, 0x00, 0x00, 0x00, 0x00, 0x58, 0xd7, 0x12, 0x90, 0x00, 0x00, 0x00,
    0x00, 0x59, 0xf5, 0x28, 0x10, 0x00, 0x00, 0x00, 0x00, 0x5a, 0xb6, 0xf4, 0x90, 0x00, 0x00, 0x00,
    0x00, 0x5b, 0xd5, 0x0a, 0x10, 0x00, 0x00, 0x00, 0x00, 0x5c, 0xa0, 0x11, 0x10, 0x00, 0x00, 0x00,
    0x00, 0x5d, 0xb4, 0xec, 0x10, 0x00, 0x00, 0x00, 0x00, 0x5e, 0x7f, 0xf3, 0x10, 0x00, 0x00, 0x00,
    0x00, 0x5f, 0x94, 0xce, 0x10, 0x00, 0x00, 0x00, 0x00, 0x60, 0x5f, 0xd5, 0x10, 0x00, 0x00, 0x00,
    0x00, 0x61, 0x7d, 0xea, 0x90, 0x00, 0x00, 0x00, 0x00, 0x62, 0x3f, 0xb7, 0x10, 0x00, 0x00, 0x00,
    0x00, 0x63, 0x5d, 0xcc, 0x90, 0x00, 0x00, 0x00, 0x00, 0x64, 0x1f, 0x99, 0x10, 0x00, 0x00, 0x00,
    0x00, 0x65, 0x3d, 0xae, 0x90, 0x00, 0x00, 0x00, 0x00, 0x66, 0x08, 0xb5, 0x90, 0x00, 0x00, 0x00,
    0x00, 0x67, 0x1d, 0x90, 0x90, 0x00, 0x00, 0x00, 0x00, 0x67, 0xe8, 0x97, 0x90, 0x00, 0x00, 0x00,
    0x00, 0x68, 0xfd, 0x72, 0x90, 0x00, 0x00, 0x00, 0x00, 0x69, 0xc8, 0x79, 0x90, 0x00, 0x00, 0x00,
    0x00, 0x6a, 0xdd, 0x54, 0x90, 0x00, 0x00, 0x00, 0x00, 0x6b, 0xa8, 0x5b, 0x90, 0x00, 0x00, 0x00,
    0x00, 0x6c, 0xc6, 0x71, 0x10, 0x00, 0x00, 0x00, 0x00, 0x6d, 0x88, 0x3d, 0x90, 0x00, 0x00, 0x00,
    0x00, 0x6e, 0xa6, 0x53, 0x10, 0x00, 0x00, 0x00, 0x00, 0x6f, 0x68, 0x1f, 0x90, 0x00, 0x00, 0x00,
    0x00, 0x70, 0x86, 0x35, 0x10, 0x00, 0x00, 0x00, 0x00, 0x71, 0x51, 0x3c, 0x10, 0x00, 0x00, 0x00,
    0x00, 0x72, 0x66, 0x17, 0x10, 0x00, 0x00, 0x00, 0x00, 0x73, 0x31, 0x1e, 0x10, 0x00, 0x00, 0x00,
    0x00, 0x74, 0x45, 0xf9, 0x10, 0x00, 0x00, 0x00, 0x00, 0x75, 0x11, 0x00, 0x10, 0x00, 0x00, 0x00,
    0x00, 0x76, 0x2f, 0x15, 0x90, 0x00, 0x00, 0x00, 0x00, 0x76, 0xf0, 0xe2, 0x10, 0x00, 0x00, 0x00,
    0x00, 0x78, 0x0e, 0xf7, 0x90, 0x00, 0x00, 0x00, 0x00, 0x78, 0xd0, 0xc4, 0x10, 0x00, 0x00, 0x00,
    0x00, 0x79, 0xee, 0xd9, 0x90, 0x00, 0x00, 0x00, 0x00, 0x7a, 0xb0, 0xa6, 0x10, 0x00, 0x00, 0x00,
    0x00, 0x7b, 0xce, 0xbb, 0x90, 0x00, 0x00, 0x00, 0x00, 0x7c, 0x99, 0xc2, 0x90, 0x00, 0x00, 0x00,
    0x00, 0x7d, 0xae, 0x9d, 0x90, 0x00, 0x00, 0x00, 0x00, 0x7e, 0x79, 0xa4, 0x90, 0x00, 0x00, 0x00,
    0x00, 0x7f, 0x8e, 0x7f, 0x90, 0x02, 0x01, 0x02, 0x03, 0x04, 0x03, 0x04, 0x03, 0x04, 0x03, 0x04,
    0x03, 0x04, 0x03, 0x05, 0x01, 0x04, 0x03, 0x04, 0x03, 0x06, 0x01, 0x04, 0x03, 0x04, 0x03, 0x04,
    0x07, 0x08, 0x07, 0x08, 0x07, 0x08, 0x07, 0x08, 0x07, 0x08, 0x07, 0x08, 0x07, 0x08, 0x07, 0x08,
    0x07, 0x08, 0x07, 0x08, 0x07, 0x08, 0x07, 0x08, 0x07, 0x08, 0x07, 0x08, 0x07, 0x08, 0x07, 0x08,
    0x07, 0x08, 0x07, 0x08, 0x07, 0x08, 0x07, 0x08, 0x07, 0x08, 0x07, 0x08, 0x07, 0x08, 0x07, 0x08,
    0x07, 0x08, 0x07, 0x08, 0x07, 0x08, 0x07, 0x08, 0x07, 0x08, 0x07, 0x08, 0x07, 0x08, 0x07, 0x08,
    0x07, 0x08, 0x07, 0x08, 0x07, 0x08, 0x07, 0x08, 0x07, 0x08, 0x07, 0x08, 0x07, 0x08, 0x07, 0x08,
    0x07, 0x08, 0x07, 0x08, 0x07, 0x08, 0x07, 0x08, 0x07, 0x08, 0x07, 0x08, 0x07, 0x08, 0x07, 0x08,
    0x07, 0x08, 0x07, 0x08, 0x07, 0x08, 0x07, 0x08, 0x07, 0x08, 0x07, 0x08, 0x07, 0x08, 0x07, 0x08,
    0x07, 0x08, 0x07, 0x08, 0x00, 0x00, 0x0c, 0x88, 0x00, 0x00, 0x00, 0x00, 0x1c, 0x20, 0x01, 0x04,
    0x00, 0x00, 0x0e, 0x10, 0x00, 0x09, 0x00, 0x00, 0x1c, 0x20, 0x01, 0x04, 0x00, 0x00, 0x0e, 0x10,
    0x00, 0x09, 0x00, 0x00, 0x2a, 0x30, 0x01, 0x0d, 0x00, 0x00, 0x2a, 0x30, 0x01, 0x0d, 0x00, 0x00,
    0x1c, 0x20, 0x01, 0x04, 0x00, 0x00, 0x0e, 0x10, 0x00, 0x09, 0x4c, 0x4d, 0x54, 0x00, 0x43, 0x45,
    0x53, 0x54, 0x00, 0x43, 0x45, 0x54, 0x00, 0x43, 0x45, 0x4d, 0x54, 0x00, 0x00, 0x00, 0x00, 0x01,
    0x01, 0x00, 0x01, 0x01, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x01, 0x0a, 0x43,
    0x45, 0x54, 0x2d, 0x31, 0x43, 0x45, 0x53, 0x54, 0x2c, 0x4d, 0x33, 0x2e, 0x35, 0x2e, 0x30, 0x2c,
    0x4d, 0x31, 0x30, 0x2e, 0x35, 0x2e, 0x30, 0x2f, 0x33, 0x0a,
).map { it.toByte() }.toByteArray()
