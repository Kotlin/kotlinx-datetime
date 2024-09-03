### Benchmarks utility module

Module that provides benchmarking infrastructure for kotlinx-datetime.
Please note that these benchmarks are typically written with the specific target, hypothesis and effect in mind.

They provide numbers, not insights, and shouldn't be used as the generic comparison and statements like
"X implementaiton or format is faster/slower than Y"


#### Usage

```
// Build `benchmarks.jar` into the project's root
./gradlew :benchmarks:jmhJar

// Run all benchmarks
java -jar benchmarks.jar

// Run dedicated benchmark(s)
java -jar benchmarks.jar Formatting
java -jar benchmarks.jar FormattingBenchmark.formatIso

// Run with the specified number of warmup iterations, measurement iterations, timeunit and mode
java -jar benchmarks.jar -wi 5 -i 5 -tu us -bm thrpt Formatting

// Extensive help
java -jar benchmarks.jar -help
```
