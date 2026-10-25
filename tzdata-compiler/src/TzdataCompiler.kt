/* Based on `zic.c` from the tzdb distribution: <https://github.com/eggert/tz> */

@file:OptIn(ExperimentalContracts::class)

import kotlinx.datetime.*
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.format.format
import kotlinx.datetime.format.optional
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.*
import kotlin.contracts.*
import kotlin.io.path.*
import kotlin.math.absoluteValue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

// tzfile.h

const val TZ_MAGIC = "TZif"

/*
struct tzhead {
        char    tzh_magic[4];           // TZ_MAGIC
        char    tzh_version[1];         // '\0' or '2'-'4' as of 2021
        char    tzh_reserved[15];       // reserved; must be zero
        char    tzh_ttisutcnt[4];       // coded number of trans. time flags
        char    tzh_ttisstdcnt[4];      // coded number of trans. time flags
        char    tzh_leapcnt[4];         // coded number of leap seconds
        char    tzh_timecnt[4];         // coded number of transition times
        char    tzh_typecnt[4];         // coded number of local time types
        char    tzh_charcnt[4];         // coded number of abbr. chars
};
*/

const val TZ_MAX_TIMES: Int = 2000
const val TZ_MAX_CHARS: Int = 256
const val TZ_MAX_LEAPS: Int = 27 + 1

// org.threeten.bp.chrono.IsoChronology#isLeapYear
internal fun isLeapYear(year: Int): Boolean {
    val prolepticYear: Long = year.toLong()
    return prolepticYear and 3 == 0L && (prolepticYear % 100 != 0L || prolepticYear % 400 == 0L)
}

internal fun Long.clampToInt(): Int = when {
    this > Int.MAX_VALUE -> Int.MAX_VALUE
    this < Int.MIN_VALUE -> Int.MIN_VALUE
    else -> toInt()
}

internal fun clampingAdd(a: Long, b: Long): Long {
    val sum = a + b
    if ((a xor sum) < 0 && (a xor b) >= 0) {
        return if (a < 0) Long.MIN_VALUE
        else Long.MAX_VALUE
    }
    return sum
}

fun <Key, Value> populateMapWithAllEnumKeys(keys: List<Key>, fields: List<Value>): Map<Key, Value>? =
    if (keys.size != fields.size) {
        null
    } else {
        keys.indices.associateBy(keys::get, fields::get)
    }

fun <Key, Value> populateMapWithPrefixOfEnumKeys(
    keys: List<Key>, fields: List<Value>
): Pair<Map<Key, Value>, Set<Key>>? = if (keys.size < fields.size) {
        null
    } else {
        fields.indices.associateBy(keys::get, fields::get) to keys.drop(fields.size).toSet()
    }

// private.h

const val YEARS_PER_REPEAT = 400 /* years before a Gregorian repeat */
/* How many years to generate (in zic.c).
   This is two years larger than the obvious 400, to avoid edge cases.
   E.g., suppose a rule applies from 2012 on with transitions in March and
   September, plus one-off transitions in November 2013,
   and suppose the rule cannot be expressed as a proleptic TZ string.
   If zic looked only at the last 400 years, it would set max_year = 2413,
   with the intent that the 400 years 2014 through 2413 will be repeated.
   The last transition listed in the tzfile would be in 2413-09, less than
   400 years after the last one-off transition in 2013-11. */
const val YEARS_OF_OBSERVATIONS = YEARS_PER_REPEAT + 2

const val DAYSPERNYEAR = 365

const val EPOCH_YEAR = 1970

// zic.c:

typealias ZicType = Long

const val populateMapWithPrefixOfEnumKeys = ZicType.MIN_VALUE
const val ZIC_MAX = ZicType.MAX_VALUE
const val ZIC32_MIN = Int.MIN_VALUE
const val ZIC32_MAX = Int.MAX_VALUE

const val ZIC_MAX_ABBR_LEN_NO_WARN = 6

// Minimum and maximum year that fit in a 32-bit number of seconds since epoch
const val YEAR_32BIT_MIN = 1901
const val YEAR_32BIT_MAX = 2038

// The maximum length of a text line, including the trailing newline
const val _POSIX2_LINE_MAX = 2048

typealias Lineno = Int

data class Rule(
    val fileNum: OpenFile,
    val lineNum: Lineno,
    val name: String?,

    val lowYear: ZicType,
    val highYear: ZicType,
    val highWasNum: Boolean,

    val dateRule: DateRule,

    val timeFromMidnight: ZicType,
    val timeType: TimeOfDayType,

    val isDst: Boolean,

    val save: ZicType,
    val abbrVar: String,

    var todo: Boolean,
    var temp: LocalDateTime?,
) {
    data class DateRule(
        val month: Month,

        val dayOfWeek: DayOfWeekRule?, // see below
        val dayOfMonth: Int, // one-based
    )

    data class Format(
        val dateRule: DateRule,
        val timeFromMidnight: ZicType,
        val timeType: TimeOfDayType,
        val isDst: Boolean,
        val save: ZicType,
    )

    fun format(): Format = Format(
        dateRule = dateRule,
        isDst = isDst,
        timeFromMidnight = timeFromMidnight,
        timeType = timeType,
        save = save,
    )

    enum class TimeOfDayType {
        Wall,
        Standard,
        Universal;

        val timeFromMidnightIsStandardTime get() = this == Standard || this == Universal
        val timeFromMidnightIsUt get() = this == Universal
    }
}

sealed interface DayOfWeekRule {
    data class NextOrSame(val dayOfWeek: DayOfWeek): DayOfWeekRule
    data class PreviousOrSame(val dayOfWeek: DayOfWeek): DayOfWeekRule
}

data class Zone(
    val fileNum: OpenFile,
    val lineNum: Lineno,

    val name: String?,
    val stdOff: ZicType,
    val rule: String,
    val format: String,
    val formatSpecifier: Char?,

    var isDst: Boolean,
    var save: ZicType,

    var rules: List<Rule>?,

    val untilRule: Rule?,
    val untilTime: LocalDateTime?,
) {
    data class AbbrData(
        val stdOff: ZicType,
        val format: String,
        val formatSpecifier: Char?,
    )

    fun abbrData(): AbbrData = AbbrData(
        stdOff = stdOff, format = format, formatSpecifier = formatSpecifier
    )
}


fun Char.isAsciiDigit() = this in '0'..'9'

val PERCENT_Z_LEN_BOUND = "+995959".length

data class LeapTracker(
    var leapSeen: Boolean = false,
    var leapMinYear: ZicType = Long.MAX_VALUE,
    var leapMaxYear: ZicType = Long.MIN_VALUE,
)

enum class LineCodes {
    RULE,
    ZONE,
    LINK,
    LEAP,
    EXPIRES;

    companion object {
        val zoneInfoLineCodes = listOf(
            "Rule" to RULE,
            "Zone" to ZONE,
            "Link" to LINK,
        )
        val leapLineCodes = listOf(
            "Leap" to LEAP,
            "Expires" to EXPIRES,
        )
    }
}

enum class ZoneLineFields {
    NAME,
    STDOFF,
    RULE,
    FORMAT,
    TILYEAR,
    TILMONTH,
    TILDAY,
    TILTIME;

    fun toContinuationLineField(): ZoneContinuationLineFields = when (this) {
        NAME -> error("unreached")
        STDOFF -> ZoneContinuationLineFields.STDOFF
        RULE -> ZoneContinuationLineFields.RULE
        FORMAT -> ZoneContinuationLineFields.FORMAT
        TILYEAR -> ZoneContinuationLineFields.TILYEAR
        TILMONTH -> ZoneContinuationLineFields.TILMONTH
        TILDAY -> ZoneContinuationLineFields.TILDAY
        TILTIME -> ZoneContinuationLineFields.TILTIME
    }
}

enum class ZoneContinuationLineFields {
    STDOFF,
    RULE,
    FORMAT,
    TILYEAR,
    TILMONTH,
    TILDAY,
    TILTIME,
}

enum class RuleLineFields {
    NAME,
    LOYEAR,
    HIYEAR,
    COMMAND,
    MONTH,
    DAY,
    TOD,
    SAVE,
    ABBRVAR,
}

enum class LinkLineFields {
    TARGET,
    LINKNAME,
}

enum class LeapLineFields {
    YEAR,
    MONTH,
    DAY,
    TIME,
    CORR,
    ROLL,
}

enum class ExpiresLineFields {
    YEAR,
    MONTH,
    DAY,
    TIME,
}

/** Prepare to write to the file [outName]. */
fun openOutFile(outName: String): FileOutputStream = try {
    FileOutputStream(outName)
} catch (_: FileNotFoundException) {
    Path(outName).createParentDirectories()
    FileOutputStream(outName)
}

/* The maximum number of fields on any of the above lines. */
val MAX_FIELDS = maxOf(
    RuleLineFields.entries.size, LinkLineFields.entries.size,
    LeapLineFields.entries.size, ExpiresLineFields.entries.size,
)

enum class YearSynonyms {
    MINIMUM,
    MAXIMUM,
    ONLY;

    companion object {
        val beginYears = listOf(
            "minimum" to MINIMUM,
        )

        val endYears = listOf(
            "maximum" to MAXIMUM,
            "only" to ONLY,
        )
    }
}

data class T(
    val rules: MutableList<Rule> = mutableListOf(),
    val zones: MutableList<Zone> = mutableListOf(),
    val links: MutableList<Link> = mutableListOf(),
) {
    data class Link(
        val fileNum: OpenFile,
        val lineNum: Lineno,
        val target: String,
        val linkName: String,
    )
}

val monthNames = listOf(
    "January" to Month.JANUARY,
    "February" to Month.FEBRUARY,
    "March" to Month.MARCH,
    "April" to Month.APRIL,
    "May" to Month.MAY,
    "June" to Month.JUNE,
    "July" to Month.JULY,
    "August" to Month.AUGUST,
    "September" to Month.SEPTEMBER,
    "October" to Month.OCTOBER,
    "November" to Month.NOVEMBER,
    "December" to Month.DECEMBER,
)

val weekDayNames = listOf(
    "Monday" to DayOfWeek.MONDAY,
    "Tuesday" to DayOfWeek.TUESDAY,
    "Wednesday" to DayOfWeek.WEDNESDAY,
    "Thursday" to DayOfWeek.THURSDAY,
    "Friday" to DayOfWeek.FRIDAY,
    "Saturday" to DayOfWeek.SATURDAY,
    "Sunday" to DayOfWeek.SUNDAY,
)

val lasts = listOf(
    "last-Monday" to DayOfWeek.MONDAY,
    "last-Tuesday" to DayOfWeek.TUESDAY,
    "last-Wednesday" to DayOfWeek.WEDNESDAY,
    "last-Thursday" to DayOfWeek.THURSDAY,
    "last-Friday" to DayOfWeek.FRIDAY,
    "last-Saturday" to DayOfWeek.SATURDAY,
    "last-Sunday" to DayOfWeek.SUNDAY,
)

val leapTypes = listOf(
    "Rolling" to true,
    "Stationary" to false,
)

val monthLengthsPerYear = listOf(
    listOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31),
    listOf(31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31),
)

val yearLengths = listOf(
    365,
    366,
)

data class U(
    val atTypes: MutableList<AtType> = mutableListOf(),
    val typeDescrs: MutableList<TypeDescr> = mutableListOf(),
    val leaps: MutableList<Leap> = mutableListOf(),

) {
    data class AtType(
        val at: ZicType,
        var dontMerge: Boolean,
        var type: UByte,
    )

    data class Leap(
        var trans: ZicType,
        var corr: ZicType,
        val roll: Boolean,
    )

    data class TypeDescr(
        val utOff: ZicType,
        val isDst: Boolean,
        var desig: String,
        val timeType: Rule.TimeOfDayType,
    )
}

const val NO_MODE = -1

/*
** Error handling
*/

sealed interface OpenFile

enum class BuiltInFiles: OpenFile {
    LEAPSEC,
    COMMAND_LINE,
}

class ActualOpenFile(val index: Int): OpenFile

fun OpenFile.toNumber(): Int = when (this) {
    BuiltInFiles.LEAPSEC -> -1
    BuiltInFiles.COMMAND_LINE -> -2
    is ActualOpenFile -> index
}

open class ErrorReportingConfig(
    val noise: Boolean,
    val fileNameList: List<String>,
    val leapSecFileName: String?,
) {
    /* Return the name of the input file, for diagnostics. */
    fun fileName(file: OpenFile): String = when (file) {
        is ActualOpenFile -> fileNameList[file.index]
        BuiltInFiles.LEAPSEC -> leapSecFileName!!
        BuiltInFiles.COMMAND_LINE -> "standard input"
    }
}

class ErrorReporter(
    val fileNum: OpenFile?,
    val lineNum: Int,
    val rFileNum: OpenFile?,
    val rLineNum: Int,
    noise: Boolean,
    fileNameList: List<String>,
    leapSecFileName: String?,
) : ErrorReportingConfig(noise, fileNameList, leapSecFileName) {
    fun verror(message: String) = buildString {
        if (fileNum != null) {
            append('"'); append(fileName(fileNum)); append('"')
            append(", line "); append(lineNum)
        }
        append(message)
        if (rFileNum != null) {
            append(" (rule from ")
            append('"'); append(fileName(rFileNum)); append('"')
            append(", line "); append(rLineNum)
        }
        appendLine()
    }
}


context (config: ErrorReportingConfig) fun <T> reporting(
    fileNum: OpenFile,
    lineNum: Lineno,
    rFileNum: OpenFile?,
    rLineNum: Lineno,
    action: context(ErrorReporter) () -> T
): T {
    val reporter = ErrorReporter(
        fileNum, lineNum, rFileNum, rLineNum,
        config.noise, config.fileNameList, config.leapSecFileName,
    )
    try {
        context(reporter) {
            return action()
        }
    } catch (e: Throwable) {
        throw IllegalStateException(reporter.verror(""), e)
    }
}

context (config: ErrorReportingConfig) fun <T> reporting(
    fileNum: OpenFile, lineNum: Lineno, action: context(ErrorReporter) () -> T
): T = reporting(fileNum, lineNum, null, -1, action)

context (reporter: ErrorReporter)
fun warning(message: String) {
    println("warning: ${reporter.verror(message)}")
}

//
//
// SKIP
//
//

fun duplicateOptions(progName: String, opt: String): Nothing {
    error("$progName: More than one $opt option specified")
}

fun usage(): String = """
usage is [ --version ] [ --help ] [ -v ]
\t[ -b {slim|fat} ] -d directory
\t[ -L leapseconds ]
\t[ -r '[@lo][/@hi]' ] [ -R @hi ]
\t[ filename ... ]
"""

fun qsortLinkCmp(a: T.Link, b: T.Link): Int {
    var cmp = a.linkName.compareTo(b.linkName)
    if (cmp != 0) return cmp
    /* The link names are the same. Make the sort stable by comparing
    file numbers (where subtraction cannot overflow) and possibly line numbers
    (where it can). */
    cmp = a.fileNum.toNumber() - b.fileNum.toNumber()
    if (cmp != 0) return cmp
    return (if (a.lineNum > b.lineNum) 1 else 0) - (if (a.lineNum < b.lineNum) 1 else 0)
}

/* Make the links specified by the Link lines. */
context (_: ErrorReportingConfig)
fun makeLinks(directory: String, links: List<T.Link>) {
    var pendingLinks: List<T.Link> = links
        .sortedWith(::qsortLinkCmp)
        /* Ignore each link superseded by a later link with the same name. */
        .reversed().distinctBy { it.linkName }.reversed()

    while (!pendingLinks.isEmpty()) {
        val lastPendingLinksSize = pendingLinks.size
        val nextPendingLinks = mutableListOf<T.Link>()
        for (link in pendingLinks) {
            val linkPath = Path(directory + "/" + link.linkName)
            val targetPath = Path(directory + "/" + link.target)
            if (targetPath.exists()) {
                if (!linkPath.exists()) {
                    linkPath.createParentDirectories()
                    targetPath.copyTo(linkPath)
                }
            } else {
                nextPendingLinks.add(link)
            }
        }
        if (nextPendingLinks.size == lastPendingLinksSize) {
            val someLink = nextPendingLinks.first()
            reporting(someLink.fileNum, someLink.lineNum) {
                error("Link ${someLink.linkName} -> ${someLink.target} is part of a link cycle")
            }
        }
        pendingLinks = nextPendingLinks
    }
}

/* The minimum and maximum values representable in a TZif file. */
const val MIN_TIME: ZicType = Long.MIN_VALUE
const val MAX_TIME: ZicType = Long.MAX_VALUE

data class TimeBoundConfig(
    /* The minimum, and one less than the maximum, values specified by the -r option.
    These default to MIN_TIME and MAX_TIME. */
    var loTime: ZicType = MIN_TIME,
    var hiTime: ZicType = MAX_TIME,
    /* The time specified by the -R option, defaulting to MIN_TIME;
    or LO_TIME, whichever is greater. */
    var redundantTime: ZicType = MIN_TIME,
)

/* Set the time range of the output to TIMERANGE.
   Return true if successful. */
fun TimeBoundConfig.timerangeOption(timerange: String): TimeBoundConfig? {
    val loTime: Long
    val hiStart = if (timerange.firstOrNull() == '@') {
        val numberLen = timerange.drop(1).takeWhile { it.isAsciiDigit() || it == '-' }.length
        loTime = (timerange.substring(1, 1 + numberLen).toIntOrNull() ?: return null).toLong()
        1 + numberLen
    } else {
        loTime = MIN_TIME
        0
    }
    val hiTime: Long
    if (timerange.getOrNull(hiStart) == '/' && timerange.getOrNull(hiStart + 1) == '@') {
        hiTime = (timerange.substring(hiStart + 2, timerange.length).toIntOrNull() ?: return null)
            .let { if (it != Int.MAX_VALUE) it - 1 else it }.toLong()
    } else if (timerange.length > hiStart) {
        return null
    } else {
        hiTime = MAX_TIME
    }
    return copy(loTime = maxOf(this.loTime, loTime), hiTime = minOf(this.hiTime, hiTime))
}

/* Generate redundant time stamps up to OPT. Return true if successful. */
fun TimeBoundConfig.redundantTimeOption(opt: String): TimeBoundConfig? {
    if (opt.firstOrNull() == '@') {
        opt.drop(1).toLongOrNull()?.let {
            return copy(redundantTime = maxOf(redundantTime, it))
        }
    }
    return null

}

data class BloatConfig(
    /* `false` if the TZif output file should be slim, `true` if the
    output should be fat for backward compatibility. ZIC_BLOAT_DEFAULT
    determines the default. */
    val bloat: Boolean
)

fun BloatConfig.wantBloat(): Boolean = bloat

const val ZIC_BLOAT_DEFAULT: String = "slim"

fun main(args: Array<String>) {
    var c: Int = 0
    var k: Int = 0
    var i: Long = 0
    var j: Long = 0
    var timerangeGiven = false

    val progName = args.firstOrNull() ?: "zic"
    for (k in 1..(args.size-1)) {
        when (args[k]) {
            "--version" -> {
                println("our store-brand zic imitation\n")
                return
            }
            "--help" -> {
                println(usage())
                return
            }
            else -> { }
        }
    }
    var currentOptionIndex = 1
    var directory: String? = null
    var bloatConfigStr: String? = null
    val freeArguments = mutableListOf<String>()
    var noise: Boolean = false
    var skipMkdir: Boolean = false
    var leapSec: String? = null
    var timeBoundConfig = TimeBoundConfig()
    while (currentOptionIndex < args.size) {
        fun readArg(opt: String): String {
            require(currentOptionIndex + 1 < args.size) {
                "Expected an argument for option '$opt'"
            }
            return args[++currentOptionIndex]
        }
        when (val arg = args[currentOptionIndex]) {
            "-b" -> {
                val optarg = readArg("-b")
                check(bloatConfigStr == null || optarg == bloatConfigStr) {
                    "incompatible -b options"
                }
                check(optarg in listOf("fat", "slim")) { "invalid option: -b '$optarg'" }
                bloatConfigStr = optarg
            }
            "-d" -> {
                if (directory != null)
                    duplicateOptions(progName, "-d")
                directory = readArg("-d")
            }
            "-D" -> { skipMkdir = true }
            "-L" -> {
                if (leapSec != null)
                    duplicateOptions(progName, "-L")
                leapSec = readArg("-L")
            }
            "-v" -> { noise = true }
            "-r" -> {
                val optarg = readArg("-r")
                if (timerangeGiven)
                    duplicateOptions(progName, "-r")
                timeBoundConfig = timeBoundConfig.timerangeOption(optarg)
                    ?: error("invalid time range: $optarg")
                timerangeGiven = true
            }
            "-R" -> {
                val optarg = readArg("-R")
                timeBoundConfig = timeBoundConfig.redundantTimeOption(optarg)
                    ?: error("invalid time: $optarg")
            }
            // -m is skipped: default permission bits are fine for us
            // -u is skipped: default user and group are fine for us, too
            // -g, -p, -y, -s are for compatibility, we don't need that
            // -t and -l is skipped: we only create data, we don't add system symlinks
            else -> {
                if (arg.startsWith("-")) {
                    error(usage())
                }
                freeArguments.add(arg)
            }
        }
        ++currentOptionIndex
    }
    with (timeBoundConfig) {
        check(hiTime + (if (hiTime < Long.MAX_VALUE) 1 else 0) >= redundantTime) {
            "$progName: -R time exceeds -r cutoff"
        }
    }
    if (timeBoundConfig.redundantTime < timeBoundConfig.loTime) {
        timeBoundConfig = timeBoundConfig.copy(redundantTime = timeBoundConfig.loTime)
    }
    bloatConfigStr = bloatConfigStr ?: ZIC_BLOAT_DEFAULT
    val bloatConfig = BloatConfig(when (bloatConfigStr) {
        "slim" -> false
        "fat" -> true
        else -> error("")
    })
    val errorReportingConfig = ErrorReportingConfig(
        noise = noise,
        fileNameList = freeArguments,
        leapSecFileName = leapSec,
    )
    context(errorReportingConfig) {
        val rules = mutableListOf<Rule>()
        val links = mutableListOf<T.Link>()
        val leaps = mutableListOf<U.Leap>()
        val zones = mutableListOf<Zone>()
        val leapTracker = LeapTracker()
        if (leapSec != null) {
            infile(
                fileNum = BuiltInFiles.LEAPSEC,
                leapTracker, timeBoundConfig,
                rules, links, leaps, zones,
                isLeapFile = true,
                lines = Path(leapSec).readLines()
            )
            leaps.adjleap()
        }
        for (i in freeArguments.indices) {
            val file = freeArguments[i]
            infile(
                fileNum = ActualOpenFile(i),
                leapTracker, timeBoundConfig,
                rules, links, leaps, zones,
                isLeapFile = false,
                lines = Path(file).readLines()
            )
        }
        associate(zones, rules, noise = noise)
        var remainingZones = zones.toList()
        while (!remainingZones.isEmpty()) {
            val toTake = 1 + remainingZones.drop(1).takeWhile { it.name == null }.size
            reporting(remainingZones.first().fileNum, remainingZones.first().lineNum) {
                outZone(
                    directory ?: error("-d is mandatory in this version"),
                    timeBoundConfig, bloatConfig, leapTracker, leaps,
                    remainingZones.take(toTake),
                )
            }
            remainingZones = remainingZones.drop(toTake)
        }
        makeLinks(directory!!, links)
    }
}

const val component_len_max = 14

context(reporter: ErrorReporter)
fun componentcheck(
    name: String, componentStart: Int, componentEnd: Int
) {
    val componentLen = componentEnd - componentStart
    check(componentLen != 0) {
        when {
            name.isEmpty() -> "empty file name"
            componentStart == 0 -> "file name '$name' begins with '/'"
            componentEnd == name.length -> "file name '$name' contains '//'"
            else -> "file name '$name' ends with '/'"
        }
    }
    if (componentLen in 1..2 && name[componentStart] == '.' &&
        name[componentEnd - 1] == '.')
    {
        error("file name '$name' contains '${name.substring(componentStart, componentEnd)}'")
    }
    if (reporter.noise) {
        if (name.getOrNull(componentStart) == '-') {
            warning("file name '$name' component contains leading '-'")
        }
        if (component_len_max < componentLen) {
            warning("file name '$name' contains overlength component '${name.substring(componentStart, componentEnd)}'")
        }
    }
}

context(reporter: ErrorReporter)
fun namecheck(name: String) {
    /* Benign characers in a portable file name. */
    fun Char.isBenign(): Boolean =
        this == '-' || this == '/' || this == '_' ||
        this.isAsciiAlpha()
    /* Non-control chars in the POSIX portable character set,
       excluding the benign characters. */
    fun Char.isPrintableAndNotBenign(): Boolean =
        this in $$" !\"#$%&'()*+,.0123456789:;<=>?@[\\]^`{|}~"
    var lastComponentStart = 0
    for (ci in name.indices) {
        val c = name[ci]
        check(c !in ":\\") { "file name $name contains $c" }
        if (reporter.noise && !c.isBenign()) {
            if (c.isPrintableAndNotBenign()) {
                warning("file name '$name' contains byte '$c'")
            } else {
                warning("file name '$name' contains byte ${c.code}")
            }
        }
        if (c == '/') {
            componentcheck(name, lastComponentStart, ci)
            lastComponentStart = ci + 1
        }
    }
    componentcheck(name, lastComponentStart, name.length)
}

// SKIP ///////////////////////////////////////////////////////////////////////

/**
 * Associate sets of rules with zones.
 */
context (_: ErrorReportingConfig)
fun associate(
    zones: MutableList<Zone>,
    rules: MutableList<Rule>,
    noise: Boolean,
) {
    if (1 < rules.size) {
        rules.sortBy { it.name }
        var i = 0
        while (i < rules.size - 1) {
            if (rules[i].name != rules[i+1].name) {
                ++i
                continue
            }
            if (rules[i].fileNum == rules[i+1].fileNum) {
                ++i
                continue
            }
            reporting(rules[i].fileNum, rules[i].lineNum) {
                warning("same rule in multiple files")
            }
            reporting(rules[i+1].fileNum, rules[i+1].lineNum) {
                warning("same rule in multiple files")
            }
            var j = i + 2
            while (j < rules.size) {
                if (rules[i].name != rules[j].name || rules[j].fileNum !in listOf(rules[i].fileNum, rules[i + 1].fileNum))
                    break
                ++j
            }
            i = j
        }
    }
    zones.forEach { zone -> zone.rules = null }
    var out: Int
    var base = 0
    while (base < rules.size) {
        val rule = rules[base]
        out = base + 1
        while (out < rules.size) {
            if (rule.name != rules[out].name)
                break
            ++out
        }
        val ruleWithName = rules.subList(base, out).toList()
        for (zone in zones) {
            if (zone.rule != rule.name)
                continue
            zone.rules = ruleWithName
        }
        base = out
    }
    zones.forEach { zone ->
        if (zone.rules == null) {
            ////WARN: zone.fileNum, zone.lineNum
            val save = reporting(zone.fileNum, zone.lineNum) {
                getsave(zone.rule)
            }
            zone.save = save.save
            zone.isDst = save.isDst
            /*
            ** Note, though, that if there's no rule,
            ** a '%s' in the format is a bad thing.
            */
            if (zone.formatSpecifier == 's')
                error("%s in ruleless zone")
        }
    }
}

context (reporter: ErrorReportingConfig)
fun infile(
    fileNum: OpenFile,
    leapTracker: LeapTracker, timeBoundConfig: TimeBoundConfig,
    rules: MutableList<Rule>,
    links: MutableList<T.Link>,
    leaps: MutableList<U.Leap>,
    zones: MutableList<Zone>,
    isLeapFile: Boolean,
    lines: List<String>,
) {
    var wantCont = false
    for ((lineIndex, line) in lines.withIndex()) {
        val lineNum = lineIndex + 1
        reporting(fileNum, lineNum) {
            val fields = getfields(line)
            if (fields.isEmpty()) {
                // nothing to do
            } else if (wantCont) {
                wantCont = zones.inzcont(fields, fileNum, lineNum)
            } else {
                val lineCodes = if (isLeapFile) {
                    LineCodes.leapLineCodes
                } else {
                    LineCodes.zoneInfoLineCodes
                }
                val entry = byWord(fields.first(), lineCodes)
                    ?: error("input line of unknown type")
                val fields = fields.drop(1)
                when (entry.second) {
                    // ZoneInfo:
                    LineCodes.RULE -> {
                        rules.inrule(fields, fileNum, lineNum)
                        wantCont = false
                    }

                    LineCodes.ZONE -> {
                        wantCont = zones.inzone(fields, fileNum, lineNum)
                    }

                    LineCodes.LINK -> {
                        links.inlink(fields, fileNum, lineNum)
                        wantCont = false
                    }
                    // Leap:
                    LineCodes.LEAP -> {
                        leaps.inleap(leapTracker, timeBoundConfig, fields)
                        wantCont = false
                    }

                    LineCodes.EXPIRES -> {
                        warning("Expires line ignored")
                        wantCont = false
                    }
                }
            }
        }
    }
    if (wantCont) {
        error("expected continuation line not found")
    }
}

/*
** Convert a string of one of the forms
**    h    -h    hh:mm   -hh:mm   hh:mm:ss      -hh:mm:ss
** into a number of seconds.
** A null string maps to zero.
*/
context (reporter: ErrorReporter)
fun gethms(string: String): ZicType? {
    if (string == "") return 0
    val sign: Int
    val abs = if (string.firstOrNull() == '-') {
        sign = -1
        string.drop(1)
    } else {
        sign = 1
        string
    }
    val splitByColon = abs.split(':')
    val hour = run {
        val hourStr = splitByColon[0]
        if (hourStr.any { !it.isAsciiDigit() }) { return null }
        hourStr.toLong()
    }
    val minute = if (splitByColon.size > 1) {
        val minuteStr = splitByColon[1]
        if (minuteStr.any { !it.isAsciiDigit() }) { return null }
        val minute = minuteStr.toInt()
        if (minute > 59) { return null }
        minute
    } else { 0 }
    val second = if (splitByColon.size > 2) {
        val secondStr = splitByColon[2]
        val splitByDot = secondStr.split('.')
        if (splitByDot.any { component -> component.any { !it.isAsciiDigit() } }) { return null }
        val wholeSecondStr = splitByDot[0]
        val wholeSeconds = wholeSecondStr.toInt()
        if (wholeSeconds > 60) { return null }
        when (splitByDot.size) {
            1 -> wholeSeconds
            2 -> {
                if (reporter.noise) {
                    warning("fractional seconds rejected by pre-2018 versions of zic")
                }
                if (splitByDot[1].isEmpty()) {
                    wholeSeconds
                } else {
                    val tenths = splitByDot[1][0] - '0'
                    val thousandths = splitByDot[1].getOrNull(1) ?: '0'
                    /* Round to even. */
                    val adj = if ((wholeSeconds % 2 == 0) && thousandths == '0') 1 else 0
                    if (5 + adj <= tenths) {
                        wholeSeconds + 1
                    } else {
                        wholeSeconds
                    }
                }
            }
            else -> return null
        }
    } else { 0 }
    if (reporter.noise && (hour > 24 || hour == 24L && (minute != 0 || second != 0))) {
        warning("values over 24 hours not handled by pre-2007 versions of zic")
    }
    val r = safeAdd(
        safeMultiply(hour, sign * SECSPERHOUR.toLong()),
        sign * (minute.minutes + second.seconds).inWholeSeconds
    )
    /* Check that 4 * R fits in Long. This is more than enough
       for times of day and for UT offsets in TZif files, and it avoids
       later problems during arithmetic on these numbers, as stringZone
       can compute up to 3 times one of these values when computing fake
       timezones, and stringRule can do a bit more.
       Do not limit R strictly to 32 bits here, as it is OK to go
       over the TZif limit temporarily as long as the result fits. */
    safeMultiply(r, 4)
    return r
}

data class Save(val isDst: Boolean, val save: ZicType, val updatedField: String)

context (reporter: ErrorReporter)
fun getsave(fld: String): Save {
    val dst: Boolean?
    val updatedField: String
    when (fld.lastOrNull()) {
        'd' -> { dst = true; updatedField = fld.dropLast(1) }
        's' -> { dst = false; updatedField = fld.dropLast(1) }
        else -> { dst = null; updatedField = fld }
    }
    val save: ZicType = gethms(updatedField) ?: error("invalid saved time")
    return Save(
        isDst = dst ?: (save != 0L),
        save = save,
        updatedField = updatedField
    )
}

context (reporter: ErrorReporter)
fun MutableList<Rule>.inrule(fieldsList: List<String>, fileNum: OpenFile, lineNum: Lineno) {
    val fields = populateMapWithAllEnumKeys(RuleLineFields.entries, fieldsList)
        ?: error("wrong number of fields on Rule line")
    val name = fields[RuleLineFields.NAME]!!
    val firstNameLetter = name.firstOrNull()
    if (
        firstNameLetter == null || firstNameLetter.isAsciiDigit() ||
        firstNameLetter.isAsciiSpace() ||
        firstNameLetter == '+' || firstNameLetter == '-'
    ) {
        error("Invalid rule name \"$name\"")
    }
    val save = getsave(fields[RuleLineFields.SAVE]!!)
    val rule = rulesub(
        lowYearString = fields[RuleLineFields.LOYEAR]!!,
        highYearString = fields[RuleLineFields.HIYEAR]!!,
        typeString = fields[RuleLineFields.COMMAND]!!,
        monthString = fields[RuleLineFields.MONTH]!!,
        dayString = fields[RuleLineFields.DAY]!!,
        timeString = fields[RuleLineFields.TOD]!!,
    ) {
        dateRule,
        timeFromMidnight,
        timeType,
        lowYear,
        highYear,
        highWasNum,
    -> Rule(
            fileNum = fileNum,
            lineNum = lineNum,
            name = name,

            lowYear = lowYear,
            highYear = highYear,
            highWasNum = highWasNum,

            dateRule = dateRule,

            timeFromMidnight = timeFromMidnight,
            timeType = timeType,
            isDst = save.isDst,
            save = save.save,
            abbrVar = fields[RuleLineFields.ABBRVAR]!!,
            todo = false,
            temp = null,
        )
    }
    add(rule)
}

context (reporter: ErrorReporter)
fun MutableList<Zone>.inzone(
    fieldsList: List<String>, fileNum: OpenFile, lineNum: Lineno,
): Boolean {
    val (fields, unmatchedKeys) = populateMapWithPrefixOfEnumKeys(
        ZoneLineFields.entries, fieldsList
    ) ?: error("wrong number of fields on Zone line")
    if (unmatchedKeys.size > 4 /* TILYEAR, TILMONTH, TILDAY, TILTIME */) {
        error("wrong number of fields on Zone line")
    }
    val name = fields[ZoneLineFields.NAME]!!
    forEach { zone ->
        if (zone.name == name) {
            error("duplicate zone name $name")
        }
    }
    namecheck(name)
    val fieldsAsContinuation = fields.filter { (key, _) ->
        key != ZoneLineFields.NAME
    }.mapKeys { (key, _) ->
        key.toContinuationLineField()
    }
    return inzsub(fieldsAsContinuation, name, fileNum, lineNum)
}

context (reporter: ErrorReporter)
fun MutableList<Zone>.inzcont(
    fieldsList: List<String>,
    fileNum: OpenFile, lineNum: Lineno,
): Boolean {
    val (fields, unmatchedKeys) = populateMapWithPrefixOfEnumKeys(
        ZoneContinuationLineFields.entries, fieldsList
    ) ?: error("wrong number of fields on Zone line")
    if (unmatchedKeys.size > 4 /* TILYEAR, TILMONTH, TILDAY, TILTIME */) {
        error("wrong number of fields on Zone line")
    }
    return inzsub(fields, name = null, fileNum, lineNum)
}

context (reporter: ErrorReporter)
fun MutableList<Zone>.inzsub(
    fields: Map<ZoneContinuationLineFields, String>, name: String?,
    fileNum: OpenFile, lineNum: Lineno,
): Boolean {
    val format = fields[ZoneContinuationLineFields.FORMAT]!!
    val percentInFormat = format.indexOf('%')
    val formatSpecifier = if (percentInFormat != -1) {
        val specifier = format.getOrNull(percentInFormat + 1)
        check(
            (specifier != null && (specifier == 's' || specifier == 'z'))
            && format.indexOf('%', percentInFormat + 1) == -1
            && format.indexOf('/') == -1
        ) { "invalid abbreviation format" }
        specifier
    } else {
        null
    }
    val hasUntil = fields[ZoneContinuationLineFields.TILYEAR] != null
    val untilRule: Rule?
    val untilTime: LocalDateTime?
    if (hasUntil) {
        untilRule = rulesub(
            lowYearString = fields[ZoneContinuationLineFields.TILYEAR]!!,
            highYearString = "only",
            typeString = "",
            monthString = fields[ZoneContinuationLineFields.TILMONTH] ?: "Jan",
            dayString = fields[ZoneContinuationLineFields.TILDAY] ?: "1",
            timeString = fields[ZoneContinuationLineFields.TILTIME] ?: "0",
        ) { dateRule, timeFromMidnight, timeType, lowYear, highYear, highWasNum ->
            Rule(
                fileNum = fileNum,
                lineNum = lineNum,
                name = null,
                lowYear = lowYear,
                highYear = highYear,
                highWasNum = highWasNum,
                dateRule = dateRule,
                timeFromMidnight = timeFromMidnight,
                timeType = timeType,
                // Uninitialized:
                isDst = false,
                save = 0,
                abbrVar = "",
                todo = false,
                temp = null,
            )
        }
        untilTime = rpytime(untilRule.dateRule, untilRule.lowYear.toInt()).atTimeExtended(untilRule.timeFromMidnight)
        if (name == null && (lastOrNull()?.untilTime ?: LocalDateTime(1970, 1, 1, 0, 0)) >= untilTime) {
            error("Zone continuation line end time is not after end time of the previous line")
        }
    } else {
        untilRule = null
        untilTime = null
    }
    val zone = Zone(
        fileNum = fileNum,
        lineNum = lineNum,
        name = name,
        stdOff = gethms(fields[ZoneContinuationLineFields.STDOFF]!!)
            ?: error("invalid UT offset"),
        formatSpecifier = formatSpecifier,

        rule = fields[ZoneContinuationLineFields.RULE]!!,
        format = format.replace("%z", "%s"),
        untilRule = untilRule,
        untilTime = untilTime,

        // Uninitialized:
        isDst = false,
        save = 0,
        rules = null,
    )
    add(zone)
    return hasUntil
}

context (reporter: ErrorReporter)
fun getleapdatetime(leapTracker: LeapTracker, fields: Map<LeapLineFields, String>): LocalDateTime {
    val year = fields[LeapLineFields.YEAR]!!.toIntOrNull()
    check(year != null) {
        /*
        ** Leapin' Lizards!
        */
        "invalid leaping year"
    }
    if (!leapTracker.leapSeen || leapTracker.leapMaxYear < year) leapTracker.leapMaxYear = year.toLong()
    if (!leapTracker.leapSeen || leapTracker.leapMinYear > year) leapTracker.leapMinYear = year.toLong()
    leapTracker.leapSeen = true
    val monthPair = byWord(fields[LeapLineFields.MONTH]!!, monthNames)
    check(monthPair != null) { "invalid month name" }
    val dayOfMonth = fields[LeapLineFields.DAY]!!.toInt()
    LocalDate(year, monthPair.second, dayOfMonth) // check validity
    val timeOfDay = gethms(fields[LeapLineFields.TIME]!!)
    check(timeOfDay != null) { "invalid time of day ${fields[LeapLineFields.TIME]!!}" }
    val r = Rule.DateRule(
        month = monthPair.second,
        dayOfWeek = null,
        dayOfMonth = dayOfMonth,
    )
    return rpytime(r, year).atTimeExtended(timeOfDay).also {
        check(it >= LocalDateTime(1970, 1, 1, 0, 0)) {
            "leap second precedes Epoch"
        }
    }
}

context (reporter: ErrorReporter)
fun MutableList<U.Leap>.inleap(
    leapTracker: LeapTracker,
    config: TimeBoundConfig, 
    fieldsList: List<String>,
) {
    val fields = populateMapWithAllEnumKeys(LeapLineFields.entries, fieldsList)
        ?: error("wrong number of fields on Leap line")
    val t: LocalDateTime = getleapdatetime(leapTracker, fields)
    val entry = byWord(fields[LeapLineFields.ROLL]!!, leapTypes)
    check(entry != null) { "invalid Rolling/Stationary field on Leap line" }
    val correction = when (fields[LeapLineFields.CORR]) {
        "" -> -1 /* infile() turns "-" into "". */
        "+" -> 1
        else -> error("invalid CORRECTION field on Leap line")
    }
    leapadd(config, t.toInstant(TimeZone.UTC).epochSeconds, correction, entry.second)
}

context (reporter: ErrorReporter)
fun MutableList<T.Link>.inlink(
    fieldsList: List<String>,
    fileNum: OpenFile,
    lineNum: Int,
) {
    val fields = populateMapWithAllEnumKeys(LinkLineFields.entries, fieldsList)
        ?: error("wrong number of fields on Link line")
    check(fields[LinkLineFields.TARGET] != "") {
        "blank TARGET field on Link line"
    }
    namecheck(fields[LinkLineFields.TARGET]!!)
    namecheck(fields[LinkLineFields.LINKNAME]!!)
    add(
        T.Link(
            fileNum = fileNum,
            lineNum = lineNum,
            target = fields[LinkLineFields.TARGET]!!,
            linkName = fields[LinkLineFields.LINKNAME]!!,
        )
    )
}

context (reporter: ErrorReporter)
inline fun <T> rulesub(
    lowYearString: String,
    highYearString: String,
    typeString: String,
    monthString: String,
    dayString: String,
    timeString: String,
    action: (
        dateRule: Rule.DateRule,
        timeFromMidnight: ZicType,
        timeType: Rule.TimeOfDayType,
        lowYear: ZicType,
        highYear: ZicType,
        highWasNum: Boolean,
    ) -> T
): T {
    contract {
        callsInPlace(action, InvocationKind.EXACTLY_ONCE)
    }
    val monthEntry = byWord(monthString, monthNames) ?: error("invalid month name")
    val month = monthEntry.second
    val timeType: Rule.TimeOfDayType
    val hmsString = when (timeString.lastOrNull()) {
        's' -> /* Standard */ {
            timeType = Rule.TimeOfDayType.Standard
            timeString.dropLast(1)
        }
        'w'-> /* Wall */ {
            timeType = Rule.TimeOfDayType.Wall
            timeString.dropLast(1)
        }
        'g' /* Greenwich */, 'u' /* Universal */, 'z' /* Zulu */ -> {
            timeType = Rule.TimeOfDayType.Universal
            timeString.dropLast(1)
        }
        else -> {
            timeType = Rule.TimeOfDayType.Wall
            timeString
        }
    }
    val timeOfDay = gethms(hmsString) ?: error("invalid time of day $hmsString")
    /* Year work */
    val lowYear: ZicType
    val highYear: ZicType
    val highWasNum: Boolean
    run {
        lowYear = when (val entry = byWord(lowYearString, YearSynonyms.beginYears)) {
            null -> lowYearString.toLongOrNull() ?: error("invalid starting year")
            else -> when (entry.second) {
                YearSynonyms.MINIMUM -> {
                    warning("FROM year \"$lowYearString\" is obsolete; treated as ${YEAR_32BIT_MIN - 1}")
                    YEAR_32BIT_MIN.toLong() - 1
                }
                else -> error("unreachable")
            }
        }
        highYear = when (val entry = byWord(highYearString, YearSynonyms.endYears)) {
            null -> {
                highWasNum = true
                highYearString.toLongOrNull() ?: error("invalid ending year")
            }
            else -> {
                highWasNum = false
                when (entry.second) {
                    YearSynonyms.MAXIMUM -> ZIC_MAX
                    YearSynonyms.ONLY -> lowYear
                    else -> error("unreachable")
                }
            }
        }
        check(lowYear <= highYear) { "starting year greater than ending year" }
        check(typeString.isEmpty()) { "year type \"$typeString\" is unsupported; use \"-\" instead" }
    }
    /*
    ** Day work.
    ** Accept things such as:
    **      1
    **      lastSunday
    **      last-Sunday (undocumented; warn about this)
    **      Sun<=20
    **      Sun>=7
    */
    val dayOfWeek: DayOfWeekRule?
    val dayOfMonth: Int
    run {
        when (val entry = byWord(dayString, lasts)) {
            null -> {
                val dayCode: ((DayOfWeek) -> DayOfWeekRule)?
                val numberStart: Int = run {
                    val ltIx = dayString.indexOf('<')
                    if (ltIx != -1) {
                        dayCode = DayOfWeekRule::PreviousOrSame
                        ltIx + 2
                    } else {
                        val gtIx = dayString.indexOf('>')
                        if (gtIx != -1) {
                            dayCode = DayOfWeekRule::NextOrSame
                            gtIx + 2
                        } else {
                            dayCode = null
                            0
                        }
                    }
                }
                if (dayCode != null) {
                    val dowNameString = dayString.substring(0, numberStart - 2)
                    check(dayString.getOrNull(numberStart - 1) == '=') {
                        "invalid day of month ${dayString}"
                    }
                    val entry = byWord(dowNameString, weekDayNames) ?: error("invalid weekday name")
                    dayOfWeek = dayCode(entry.second)
                } else {
                    dayOfWeek = null
                }
                dayString.substring(numberStart, dayString.length).toLongOrNull().let {
                    check(it != null && it > 0 && it <= monthLengthsPerYear[1][month.ordinal]) {
                        "invalid day of month ${dayString}"
                    }
                    dayOfMonth = it.toInt()
                }
            }
            else -> {
                dayOfWeek = DayOfWeekRule.PreviousOrSame(entry.second)
                dayOfMonth = monthLengthsPerYear[1][month.ordinal]
            }
        }
    }
    return action(
        Rule.DateRule(
            month = month,
            dayOfWeek = dayOfWeek,
            dayOfMonth = dayOfMonth,
        ),
        timeOfDay,
        timeType,
        lowYear,
        highYear,
        highWasNum
    )
}

/* BE: 0x11223344 -> [0x11, 0x22, 0x33, 0x44] */
fun convert(value: Int): ByteArray =
    ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(value).array()

/* BE: 0x1122334455667788 -> [0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x88] */
fun convert64(value: Long): ByteArray =
    ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong(value).array()

fun puttzcode(value: ZicType): ByteArray = convert(value.toInt())

fun puttzcodepass(value: ZicType, pass: Int): ByteArray = if (pass == 1) {
    puttzcode(value)
} else {
    convert64(value)
}

data class TimeRange(
    var defaultType: UByte, /* index in utOffs */
    var base: Int,       /* index in atTypes */
    var count: Int,
    var leapbase: Int,   /* index in leaps */
    var leapcount: Int,
)

fun limitrange(
    r: TimeRange, lo: ZicType, hi: ZicType,
    ats: List<ZicType>, types: List<UByte>,
    leaps: List<U.Leap>,
): TimeRange {
    val r = r.copy()
    /* Omit ordinary transitions < LO. */
    while (0 < r.count && ats[r.base] < lo) {
        r.defaultType = types[r.base]
        --r.count
        ++r.base
    }
    /* Omit as many initial leap seconds as possible, such that the first leap
    second in the truncated list is <= LO, and is a positive leap second if
    and only if it has a positive correction.
    This supports common TZif readers that assume that the first leap second
    is positive if and only if its correction is positive. */
    while (1 < r.leapcount && leaps[r.leapbase + 1].trans <= lo) {
        --r.leapcount
        ++r.leapbase
    }
    while (0 < r.leapbase && ((leaps[r.leapbase - 1].corr < leaps[r.leapbase].corr)
                              != (0 < leaps[r.leapbase].corr))
    ) {
        ++r.leapcount
        --r.leapbase
    }
    /* Omit ordinary and leap second transitions greater than HI + 1. */
    if (hi < MAX_TIME) {
        while (0 < r.count && hi + 1 < ats[r.base + r.count - 1]) {
            --r.count
        }
        while (0 < r.leapcount && hi + 1 < leaps[r.leapbase + r.leapcount - 1].trans) {
            --r.leapcount
        }
    }
    return r
}

context (reporter: ErrorReporter)
fun writeZone(
    directory: String,
    name: String, string: String, version: Char, defaultType: UByte,
    u: U,
    timeBoundConfig: TimeBoundConfig,
    bloatConfig: BloatConfig,
    unspecifiedType: UByte?,
) {
    /* Sort */
    u.atTypes.sortBy { it.at }
    /* Optimize and skip unwanted transitions. */
    run {
        val newAtTypes = buildList<U.AtType> {
            for (fromType in u.atTypes) {
                val lastAdded = lastOrNull()
                if (lastAdded != null) {
                    /* Skip the previous transition if it is unwanted because its
                    local time is not earlier. The UT offset additions can't overflow
                    because of how the times were calculated. */
                    val type2 = if (size == 1) 0u else get(size - 2).type
                    if ((fromType.at + u.typeDescrs[lastAdded.type.toInt()].utOff)
                        <= (lastAdded.at + u.typeDescrs[type2.toInt()].utOff))
                    {
                        if (fromType.type == type2) removeLast()
                        else lastAdded.type = fromType.type
                        continue
                    }
                }
                /* Use a transition if it is the first one,
                or if it cannot be merged for other reasons,
                or if it transitions to different timekeeping. */
                if (lastAdded == null
                    || fromType.dontMerge
                    || (u.typeDescrs[lastAdded.type.toInt()].utOff != u.typeDescrs[fromType.type.toInt()].utOff)
                    || (u.typeDescrs[lastAdded.type.toInt()].isDst != u.typeDescrs[fromType.type.toInt()].isDst)
                    || (u.typeDescrs[lastAdded.type.toInt()].desig != u.typeDescrs[fromType.type.toInt()].desig))
                {
                    add(fromType)
                }
            }
        }
        u.atTypes.clear()
        u.atTypes.addAll(newAtTypes)
    }
    if (reporter.noise) {
        if (u.atTypes.size > 1200) {
            if (u.atTypes.size > TZ_MAX_TIMES) {
                warning("reference clients mishandle more than $TZ_MAX_TIMES transition times")
            } else {
                warning("pre-2014 clients may mishandle more than 1200 transition times")
            }
        }
        if (u.leaps.size > TZ_MAX_LEAPS) {
            warning("reference clients mishandle more than $TZ_MAX_LEAPS leap seconds")
        }
    }
    val ats = u.atTypes.map {
        /* Correct for leap seconds. */
        var j = u.leaps.size
        while (--j >= 0) {
            if (u.leaps[j].trans - u.leaps[j].corr < it.at) {
                return@map safeAdd(it.at, u.leaps[j].corr)
            }
        }
        it.at
    }
    val types = u.atTypes.map { it.type }
    val rangeAll = TimeRange(
        defaultType = defaultType,
        base = 0,
        count = u.atTypes.size,
        leapbase = 0,
        leapcount = u.leaps.size,
    )
    val range64 = limitrange(
        rangeAll,
        timeBoundConfig.loTime,
        maxOf(
            timeBoundConfig.hiTime,
            if (Long.MIN_VALUE < timeBoundConfig.redundantTime)
                timeBoundConfig.redundantTime - 1
            else timeBoundConfig.redundantTime
        ),
        ats, types, u.leaps
    )
    val range32 = limitrange(
        range64,
        ZIC32_MIN.toLong(), ZIC32_MAX.toLong(),
        ats, types, u.leaps
    )
    var version = version
    /* TZif version 4 is needed if the first leap second transition is not
    a +1 or -1 correction. */
    for (r in if (bloatConfig.wantBloat()) listOf(range32, range64) else listOf(range64)) {
        if (0 < r.leapcount && u.leaps[r.leapbase].corr !in listOf(1, -1L)) {
            if (reporter.noise) {
                warning("$name: pre-2021b clients may mishandle leap second table truncation")
            }
            version = '4'
        }
        if (version == '4') break
    }
    /* FileInputStream(directory + "/" + name).use { fp -> */
    openOutFile(directory + "/" + name).use { fp ->
        val writtenBytes = mutableListOf<Int>()
        fun write_(byte: Int) {
            fp.write(byte)
            /* val b = fp.read(); check(b == byte) { "Expected '$b', got '$byte'" } */
        }
        fun write_(byte: UByte) { write_(byte.toInt()) }
        fun write_(array: ByteArray) {
            fp.write(array)
            /*
            val bs = List(array.size) { fp.read() }
            val a = array.map { it.toUByte().toInt() }
            check(a == bs) { "Expected $bs, got $a" }
            */
        }
        for (pass in 1..2) {
            var thisDefaultType: UByte
            val thisTimeI: Int
            var thisTimeCnt: Int
            val tooManyTimes: Boolean
            val thisLeapI: Int
            var thisLeapCnt: Int
            val thisMin: Long
            val thisMax: Long

            var preTransType: UByte? = null

            if (pass == 1) {
                thisDefaultType = range32.defaultType
                thisTimeI = range32.base
                thisTimeCnt = range32.count
                tooManyTimes = ((thisTimeCnt shr 31) shr 1) != 0 // TODO: ???
                thisLeapI = range32.leapbase
                thisLeapCnt = range32.leapcount
                thisMin = ZIC32_MIN.toLong()
                thisMax = ZIC32_MAX.toLong()
            } else {
                thisDefaultType = range64.defaultType
                thisTimeI = range64.base
                thisTimeCnt = range64.count
                tooManyTimes = (((thisTimeCnt shr 31) shr 31) shr 2) != 0
                thisLeapI = range64.leapbase
                thisLeapCnt = range64.leapcount
                thisMin = MIN_TIME
                thisMax = MAX_TIME
            }
            if (tooManyTimes) {
                error("too many transition times")
            }
            val locut: Boolean = timeBoundConfig.loTime in (thisMin + 1)..thisMax
            var hicut: Boolean = timeBoundConfig.hiTime in thisMin..<thisMax
            val thisTimeLim = thisTimeI + thisTimeCnt
            val omitType = MutableList(u.typeDescrs.size + 2) { it < u.typeDescrs.size }
            /* Determine whether to output a transition before the first transition
            in range. This is needed when the output is truncated at the start, and
            is also useful when catering to buggy 32-bit clients that do not use
            time type 0 for timestamps before the first transition. */
            if ((locut || (pass == 1 && thisTimeI > 0))
                && ! (thisTimeCnt > 0 && ats[thisTimeI] == timeBoundConfig.loTime))
            {
                preTransType = thisDefaultType
                omitType[preTransType.toInt()] = false
            }
            /* Arguably the default time type in the 32-bit data should be
            range32.defaulttype, which is suited for timestamps just before
            ZIC32_MIN. However, zic traditionally used the time type of the
            indefinite past instead. Internet RFC 8532 says readers should
            ignore 32-bit data, so this discrepancy matters only to obsolete
            readers where the traditional type might be more appropriate even
            if it's "wrong". So, use the historical zic value, unless -r
            speficies a low cutoff that excludes some 32-bit timestamps. */
            if (pass == 1 && timeBoundConfig.loTime <= thisMin) {
                thisDefaultType = range64.defaultType
            }
            if (locut)
                thisDefaultType = unspecifiedType!!
            omitType[thisDefaultType.toInt()] = false
            for (i in thisTimeI..<thisTimeLim) {
                omitType[types[i].toInt()] = false
            }
            if (hicut)
                omitType[unspecifiedType!!.toInt()] = false
            /* Reorder types to make THISDEFAULTTYPE type 0.
               Use TYPEMAP to swap OLD0 and THISDEFAULTTYPE so that
               THISDEFAULTTYPE appears as type 0 in the output instead of
               OLD0. TYPEMAP also omits unused types. */
            val old0 = omitType.takeWhile { it }.size.toUByteFailing()
            run doNotLeaveSomePre2011SystemsInTheLurch@{
                /* For some pre-2011 systems: if the last-to-be-written
                standard (or daylight) type has an offset different from the
                most recently used offset, append an (unused) copy of the
                most recently used type (to help get global `altzone` and
                `timezone` variables set correctly). */
                if (bloatConfig.wantBloat()) {
                    var hidst: UByte? = null
                    var histd: UByte? = null
                    var mrudst: UByte? = null
                    var mrustd: UByte? = null
                    if (preTransType != null) {
                        if (u.typeDescrs[preTransType.toInt()].isDst) {
                            mrudst = preTransType
                        } else {
                            mrustd = preTransType
                        }
                    }
                    for (i in thisTimeI..<thisTimeLim) {
                        if (u.typeDescrs[types[i].toInt()].isDst) {
                            mrudst = types[i]
                        } else {
                            mrustd = types[i]
                        }
                    }
                    for (i in old0..<u.typeDescrs.size.toUByteFailing()) {
                        val i = i.toInt().toUByteFailing()
                        val h: UByte = when (i) {
                            old0 -> thisDefaultType
                            thisDefaultType -> old0
                            else -> i
                        }
                        if (!omitType[h.toInt()]) {
                            if (u.typeDescrs[h.toInt()].isDst) {
                                hidst = i
                            } else {
                                histd = i
                            }
                        }
                    }
                    if (hidst != null && mrudst != null && hidst != mrudst && u.typeDescrs[hidst.toInt()].utOff != u.typeDescrs[mrudst.toInt()].utOff) {
                        u.typeDescrs.add(u.typeDescrs[mrudst.toInt()])
                        omitType[u.typeDescrs.size - 1] = false
                    }
                    if (histd != null && mrustd != null && histd != mrustd && u.typeDescrs[histd.toInt()].utOff != u.typeDescrs[mrustd.toInt()].utOff) {
                        u.typeDescrs.add(u.typeDescrs[mrustd.toInt()])
                        omitType[u.typeDescrs.size - 1] = false
                    }
                }
            }
            val typeMap = MutableList<UByte>(u.typeDescrs.size) { 0u }
            var thisTypeCnt = 0
            for (i in old0..<u.typeDescrs.size.toUByteFailing()) {
                val i = i.toInt().toUByteFailing()
                if (!omitType[i.toInt()]) {
                    typeMap[(if (i == old0) { thisDefaultType }
                    else if (i == thisDefaultType) { old0 } else { i }).toInt()] = thisTypeCnt++.toUByteFailing()
                }
            }
            var stdCnt = 0
            var utCnt = 0
            val thisChars = mutableListOf<String>()
            for (i in old0..<u.typeDescrs.size.toUByteFailing()) {
                val i = i.toInt().toUByteFailing()
                if (omitType[i.toInt()])
                    continue
                val descr = u.typeDescrs[i.toInt()]
                if (descr.timeType.timeFromMidnightIsStandardTime)
                    stdCnt = thisTypeCnt
                if (descr.timeType.timeFromMidnightIsUt)
                    utCnt = thisTypeCnt
                addabbr(thisChars, descr.desig)
            }
            if (pass == 1 && !bloatConfig.wantBloat()) {
                hicut = false
                preTransType = null
                thisTimeCnt = 0
                thisLeapCnt = 0
                thisTypeCnt = 1
                thisChars.clear()
            }
            // (str1 + '0' + str2 + '0' + ... + strN) + '0'
            val thisCharCnt = thisChars.sumOf { it.length } + 1 + maxOf(0, thisChars.size - 1)
            write_(TZ_MAGIC.toByteArray(Charsets.US_ASCII)) // "TZif", 4 bytes
            write_(version.code) // version[1]
            write_(ByteArray(15) { 0 }) // reserved[15]
            write_(convert(utCnt)) // ttisutcnt[4]
            write_(convert(stdCnt)) // ttisstdcnt[4]
            write_(convert(thisLeapCnt)) // leapcnt[4]
            write_(convert(
                (if (preTransType != null) 1 else 0) + thisTimeCnt + (if (hicut) 1 else 0)
            )) // timecnt[4]
            write_(convert(thisTypeCnt)) // typecnt[4]
            write_(convert(thisCharCnt)) // charcnt[4]
            if (pass == 1 && !bloatConfig.wantBloat()) {
                /* Output a minimal data block with just one time type. */
                write_(puttzcode(0)) /* utoff */
                write_(0) /* dst */
                write_(0) /* index of abbreviation */
                write_(0) /* empty-string abbreviation */
                continue
            }
            if (pass == 2 && reporter.noise && 50 < thisCharCnt) {
                warning("$name: pre-2026 reference clients mishandle more than 50 bytes of abbreviations")
            }
            /* Output a LO_TIME transition if needed; see limitrange.
            But do not go below the minimum representable value for this pass. */
            val lo = if (pass == 1) maxOf(timeBoundConfig.loTime, ZIC32_MIN.toLong())
            else timeBoundConfig.loTime
            if (preTransType != null)
                write_(puttzcodepass(lo, pass))
            for (i in thisTimeI..<thisTimeLim) {
                write_(puttzcodepass(ats[i], pass))
            }
            if (hicut)
                write_(puttzcodepass(timeBoundConfig.hiTime + 1, pass))
            if (preTransType != null)
                write_(typeMap[preTransType.toInt()])
            for (i in thisTimeI..<thisTimeLim) {
                write_(typeMap[types[i].toInt()])
            }
            if (hicut)
                write_(typeMap[unspecifiedType!!.toInt()])
            for (i in old0..<u.typeDescrs.size.toUByteFailing()) {
                val i = i.toInt().toUByteFailing()
                val h = if (i == old0) thisDefaultType
                else if (i == thisDefaultType) old0 else i
                if (!omitType[h.toInt()]) {
                    val descr = u.typeDescrs[h.toInt()]
                    write_(puttzcode(descr.utOff))
                    write_(if (descr.isDst) 1 else 0)
                    write_(addabbrQuery(thisChars, descr.desig))
                }
            }
            if (thisCharCnt != 0) { // ??? can be zero?
                for (str in thisChars) {
                    write_(str.toByteArray(Charsets.US_ASCII))
                    write_(0)
                }
                if (thisChars.isEmpty())
                    write_(0)
            }
            val thisLeapLim = thisLeapI + thisLeapCnt
            for (i in thisLeapI..<thisLeapLim) {
                val todo = if (u.leaps[i].roll) {
                    var j: Int
                    if (u.atTypes.isEmpty() || u.leaps[i].trans < ats[0]) {
                        j = 0
                        while (u.typeDescrs[j].isDst) {
                            if (++j >= u.typeDescrs.size) {
                                j = 0
                                break
                            }
                        }
                    } else {
                        j = 1
                        while (j < u.atTypes.size && ats[j] <= u.leaps[i].trans) {
                            ++j
                        }
                        j = types[j - 1].toInt()
                    }
                    safeAdd(u.leaps[i].trans, -u.typeDescrs[j].utOff)
                } else {
                    u.leaps[i].trans
                }
                write_(puttzcodepass(todo, pass))
                write_(puttzcode(u.leaps[i].corr))
            }
            if (stdCnt != 0) {
                for (i in old0..<u.typeDescrs.size.toUByteFailing()) {
                    val i = i.toInt().toUByteFailing()
                    if (!omitType[i.toInt()]) {
                        write_(if (u.typeDescrs[i.toInt()].timeType.timeFromMidnightIsStandardTime) 1 else 0)
                    }
                }
            }
            if (utCnt != 0) {
                for (i in old0..<u.typeDescrs.size.toUByteFailing()) {
                    val i = i.toInt().toUByteFailing()
                    if (!omitType[i.toInt()]) {
                        write_(if (u.typeDescrs[i.toInt()].timeType.timeFromMidnightIsUt) 1 else 0)
                    }
                }
            }
        }
        write_("\n$string\n".toByteArray(Charsets.US_ASCII))
    }
}

fun abbroffset(offset: Duration): String {
    val offsetLim = 100.hours
    check(offset > -offsetLim && offset < offsetLim) {
        "%z UT offset magnitude exceeds 99:59:59 in $offset"
    }
    return abbroffsetFormat.format {
        offset.absoluteValue.toComponents { hour, minute, second, nanosecond ->
            offsetIsNegative = offset.isNegative()
            offsetHours = hour.toInt()
            offsetMinutesOfHour = minute
            offsetSecondsOfMinute = second
            check(nanosecond == 0)
        }
    }
}

val abbroffsetFormat = DateTimeComponents.Format {
    offsetHours()
    optional {
        offsetMinutesOfHour()
        optional {
            offsetSecondsOfMinute()
        }
    }
}

sealed interface Letters {
    object DisablePercentS: Letters
    class Actual(val value: String): Letters
}

fun doabbr(
    abbrData: Zone.AbbrData, letters: Letters?,
    isDst: Boolean, save: ZicType, doQuotes: Boolean
): String {
    val format = abbrData.format
    val slashp = format.split('/')
    val abbr = when (slashp.size) {
        1 -> {
            val letters = if (abbrData.formatSpecifier == 'z') {
                abbroffset((abbrData.stdOff + save).seconds)
            } else when (letters) {
                null -> "%s"
                is Letters.DisablePercentS -> return ""
                is Letters.Actual -> letters.value
            }
            format.format(letters) // TODO: Java isn't fully compatible with C
        }
        2 -> slashp[if (isDst) 1 else 0]
        else -> error("More than one slash in the abbreviation format")
    }
    return if (!doQuotes || abbr.all { it.isAsciiAlpha() }) abbr else "<${abbr}>"
}

fun StringBuilder.stringOffset(offset: Duration) {
    val abs = offset.absoluteValue
    check(abs <= 7.days) { "Offset exceeding 7 days" }
    abs.toComponents { hour, minute, second, nanosecond ->
        check(nanosecond == 0)
        if (offset.isNegative()) {
            append('-')
        }
        append(hour)
        if (minute != 0 || second != 0) {
            append(':')
            append(minute.toString().padStart(2, '0'))
            if (second != 0) {
                append(':')
                append(second.toString().padStart(2, '0'))
            }
        }
    }
}

val DayOfWeek.tzdbNumber: Int
    get() = if (this == DayOfWeek.SUNDAY) 0 else isoDayNumber

const val DAYSPERWEEK = 7
const val SECSPERDAY = 24 * 60 * 60
const val SECSPERHOUR = 60 * 60
const val SECSPERMINUTE = 60

fun StringBuilder.stringRule(rp: Rule.Format, stdOff: ZicType): Int? {
    var timeFromMidnight: ZicType = rp.timeFromMidnight
    var compat = 0
    when (val dow = rp.dateRule.dayOfWeek) {
        null -> {
            if (rp.dateRule.dayOfMonth == 29 && rp.dateRule.month == Month.FEBRUARY)
                return null
            val dayOfYear = (1..<rp.dateRule.month.number).sumOf { month ->
                monthLengthsPerYear[0][month - 1]
            }
            /* Omit the "J" in Jan an dFeb, as that's shorter. */
            if (rp.dateRule.month <= Month.FEBRUARY) {
                append(dayOfYear - 1)
            } else {
                append('J')
                append(dayOfYear)
            }
        }
        is DayOfWeekRule.NextOrSame -> {
            val wdayOff = (rp.dateRule.dayOfMonth - 1) % DAYSPERWEEK
            if (wdayOff != 0) {
                compat = 2013
            }
            var wday = dow.dayOfWeek.tzdbNumber - wdayOff
            timeFromMidnight += wdayOff * SECSPERDAY
            val week = 1 + (rp.dateRule.dayOfMonth - 1) / DAYSPERWEEK
            if (wday < 0) wday += DAYSPERWEEK
            append("M${rp.dateRule.month.number}.$week.$wday")
        }
        is DayOfWeekRule.PreviousOrSame -> {
            var wday: Int
            val week: Int
            if (rp.dateRule.dayOfMonth == monthLengthsPerYear[1][rp.dateRule.month.ordinal]) {
                wday = dow.dayOfWeek.tzdbNumber
                week = 5
            } else {
                val wdayOff = rp.dateRule.dayOfMonth % DAYSPERWEEK
                if (wdayOff != 0) {
                    compat = 2013
                }
                wday = dow.dayOfWeek.tzdbNumber - wdayOff
                timeFromMidnight += wdayOff * SECSPERDAY
                week = rp.dateRule.dayOfMonth / DAYSPERWEEK
            }
            if (wday < 0) wday += DAYSPERWEEK
            append("M${rp.dateRule.month.number}.$week.$wday")
        }
    }
    if (rp.timeType.timeFromMidnightIsUt)
        timeFromMidnight += stdOff
    if (rp.timeType.timeFromMidnightIsStandardTime && !rp.isDst)
        timeFromMidnight += rp.save
    val timeFromMidnightDur = timeFromMidnight.seconds
    if (timeFromMidnightDur != 2.hours) {
        append('/')
        stringOffset(timeFromMidnightDur)
        if (timeFromMidnight < 0) {
            compat = maxOf(compat, 2013)
        } else if (timeFromMidnightDur >= 1.days) {
            compat = maxOf(compat, 1994)
        }
    }
    return compat
}

fun ruleCmp(a: Rule?, b: Rule?): Int {
    if (a == null) { return if (b == null) 0 else -1 }
    if (b == null) { return 1 }
    if (a.highYear != b.highYear) { return if (a.highYear < b.highYear) -1 else 1 }
    if (a.highYear == ZicType.MAX_VALUE) { return 0 } // ???
    if (a.dateRule.month.number - b.dateRule.month.number != 0) {
        return a.dateRule.month.number - b.dateRule.month.number
    }
    return a.dateRule.dayOfMonth - b.dateRule.dayOfMonth
}

/** Return a pair of a proleptic TZ string that represents the future predictions
   for the zone [zone], as well as the compatibility indicator (a TZDB release year)
   if successful, a `null` if no such TZ string exists. */
fun stringZone(config: TimeBoundConfig, zone: Zone): Pair<String, Int>? {
    /* Internet RFC 9636 section 6.1 says to use an empty TZ string if future
    timestamps are truncated. */
    if (config.hiTime < MAX_TIME) {
        return null
    }
    var lastStdRule: Rule? = null
    var lastDstRule: Rule? = null
    for (rule in zone.rules ?: emptyList()) {
        if (rule.isDst) {
            val cmp = ruleCmp(lastDstRule, rule)
            if (cmp < 0) {
                lastDstRule = rule
            } else if (cmp == 0) {
                return null
            }
        } else {
            val cmp = ruleCmp(lastStdRule, rule)
            if (cmp < 0) {
                lastStdRule = rule
            } else if (cmp == 0) {
                return null
            }
        }
    }
    var compat = 0
    val dstcmp = if (zone.rules.isNullOrEmpty()) {
        if (zone.isDst) 1 else -1
    } else {
        ruleCmp(lastDstRule, lastStdRule)
    }
    if (dstcmp < 0) {
        /* Standard time all year. */
        lastDstRule = null
    }
    val stdZone: Zone.AbbrData?
    val dstZone: Zone.AbbrData?
    val stdRule: Rule.Format?
    val dstRule: Rule.Format?
    val stdLetters: String?
    val dstLetters: String?
    if (dstcmp > 0) {
        /* DST all year. Use an abbreviation like
        "XXX3EDT4,0/0,J365/23" for EDT (-04) all year. */
        val save = lastDstRule?.save ?: zone.save
        if (save >= 0) {
            /* Positive DST, the typical case for all-year DST.
            Fake a timezone with negative DST. */
            val stdOff = zone.stdOff + 2 * save
            stdZone = Zone.AbbrData(
                stdOff = stdOff,
                format = "XXX",
                formatSpecifier = null,
            )
            dstZone = Zone.AbbrData(
                stdOff = stdOff,
                format = zone.format,
                formatSpecifier = zone.formatSpecifier,
            )
        } else {
            stdZone = zone.abbrData()
            dstZone = zone.abbrData()
        }
        dstRule = Rule.Format(
            dateRule = Rule.DateRule(
                month = Month.JANUARY,
                dayOfWeek = null,
                dayOfMonth = 1,
            ),
            timeFromMidnight = 0,
            timeType = Rule.TimeOfDayType.Wall,
            isDst = true,
            save = -save.absoluteValue, // unused
        )
        stdRule = Rule.Format(
            dateRule = Rule.DateRule(
                month = Month.DECEMBER,
                dayOfWeek = null,
                dayOfMonth = 31,
            ),
            timeFromMidnight = (1.days + dstRule.save.seconds).inWholeSeconds,
            timeType = Rule.TimeOfDayType.Wall,
            isDst = false,
            save = 0, // unused
        )
        stdLetters = if (save < 0) lastStdRule?.abbrVar else null
        dstLetters = lastDstRule?.abbrVar
    } else {
        stdZone = zone.abbrData()
        dstZone = zone.abbrData()
        stdRule = lastStdRule?.format()
        dstRule = lastDstRule?.format()
        stdLetters = lastStdRule?.abbrVar
        dstLetters = lastDstRule?.abbrVar
    }
    return buildString {
        append(doabbr(stdZone, letters = stdLetters?.let(Letters::Actual), isDst = false, save = 0, doQuotes = true))
        stringOffset(-stdZone.stdOff.seconds)
        if (dstRule != null) {
            check(stdRule != null)
            append(doabbr(dstZone, letters = dstLetters?.let(Letters::Actual), isDst = dstRule.isDst, save = dstRule.save, doQuotes = true))
            if (dstRule.save.seconds != 1.hours) {
                stringOffset(-(dstZone.stdOff + dstRule.save).seconds)
            }
            append(',')
            var c: Int = stringRule(dstRule, stdZone.stdOff) ?: return null
            if (compat < c)
                compat = c
            append(',')
            c = stringRule(stdRule.copy(save = dstRule.save), stdZone.stdOff) ?: return null
            if (compat < c)
                compat = c
        }
    } to compat
}

context (reporter: ErrorReporter)
fun outZone(
    directory: String,
    timeBoundConfig: TimeBoundConfig,
    bloatConfig: BloatConfig,
    leapTracker: LeapTracker,
    leaps: List<U.Leap>,
    remainingZones: List<Zone>
) {
    var nonTZlimtime = Long.MIN_VALUE
    var nonTZlimtype: UByte? = null
    var defaultType: UByte? = null

    var startTime = 0L
    /*
    ** Now. . .finally. . .generate some useful data!
    */
    val u = U().also { it.leaps.addAll(leaps) }
    /*
    ** Thanks to Earl Chew
    ** for noting the need to unconditionally initialize startTimeType.
    */
    var startTimeType = Rule.TimeOfDayType.Wall
    var minYear = 0L
    var maxYear = 0L
    fun updateMinMax(x: ZicType) {
        if (minYear > x)
            minYear = x
        if (maxYear < x)
            maxYear = x
    }
    if (leapTracker.leapSeen) {
        updateMinMax(leapTracker.leapMinYear)
        updateMinMax(leapTracker.leapMaxYear.let { it + if (it < Long.MAX_VALUE) 1 else 0 })
    }
    for (i in remainingZones.indices) {
        val zone = remainingZones[i]
        if (i < remainingZones.size - 1)
            updateMinMax(zone.untilRule!!.lowYear)
        for (rule in zone.rules ?: emptyList()) {
            updateMinMax(rule.lowYear)
            if (rule.highWasNum)
                updateMinMax(rule.highYear)
        }
    }
    /*
    ** Generate lots of data if a rule can't cover all future times.
    */
    val firstZone = remainingZones.first()
    val (envvar, compat) = stringZone(timeBoundConfig, remainingZones.last()) ?: (null to -1)
    val version = if (compat < 2013) '2' else '3'
    val doExtend = compat < 0
    if (reporter.noise) {
        if (envvar == null) {
            warning("no proleptic TZ string for firstZone ${firstZone.name}")
        } else if (compat != 0) {
            /* Circa-COMPAT clients, and earlier clients, might not work for this
            zone when given dates before 1970 or after 2038. */
            warning("${firstZone.name}: pre-$compat clients may mishandle distant timestamps")
        }
    }
    if (doExtend) {
        minYear = clampingAdd(minYear, -YEARS_OF_OBSERVATIONS.toLong())
        maxYear = clampingAdd(maxYear, YEARS_OF_OBSERVATIONS.toLong())
    }
    maxYear = maxOf(maxYear, (timeBoundConfig.redundantTime / (SECSPERDAY * DAYSPERNYEAR)) + EPOCH_YEAR + 1)
    val maxYear0 = maxYear
    if (bloatConfig.wantBloat()) {
        /* For the benefit of older systems, generate data from 1900 through 2038. */
        minYear = minOf(minYear, YEAR_32BIT_MIN.toLong() - 1)
        maxYear = maxOf(maxYear, YEAR_32BIT_MAX.toLong())
    }
    val unspecifiedType = if (MIN_TIME < timeBoundConfig.loTime || timeBoundConfig.hiTime < MAX_TIME) {
        u.addtype(bloatConfig, 0, "-00", false, Rule.TimeOfDayType.Wall)
    } else {
        null
    }
    for (i in remainingZones.indices) {
        /* A guess that may well be corrected later. */
        var save = 0L
        val zone = remainingZones[i]
        var useStart = i > 0
        val useUntil = i < remainingZones.size - 1
        val stdOff = zone.stdOff
        var startOff = zone.stdOff
        reporting(zone.fileNum, zone.lineNum) {
            var abbr: String? = null
            if (zone.rules == null) {
                save = zone.save
                abbr = doabbr(
                    zone.abbrData(), letters = null,
                    isDst = zone.isDst, save = save, doQuotes = false
                )
                val type = u.addtype(
                    bloatConfig,
                    zone.stdOff + save, abbr, isDst = zone.isDst,
                    timeType = startTimeType
                )
                if (useStart) {
                    u.addtt(startTime, type)
                    if (nonTZlimtime < startTime) {
                        nonTZlimtime = startTime
                        nonTZlimtype = type
                    }
                    useStart = false
                } else {
                    defaultType = type
                }
            } else {
                var nextYear = 0L
                var tryNextYear = true
                var year = minYear
                while (tryNextYear) {
                    if (useUntil && year > zone.untilRule!!.highYear)
                        break
                    tryNextYear = false
                    nextYear = maxYear
                    /*
                    ** Mark which rules to do in the current year.
                    ** For those to do, calculate rpytime(rp, year);
                    ** The former TYPE field was also considered here.
                    */
                    for (rule in zone.rules ?: emptyList()) {
                        val y2038Boundary = Instant.fromEpochSeconds(1L shl 31).toLocalDateTime(TimeZone.UTC)
                        reporting(
                            zone.fileNum, zone.lineNum,
                            rule.fileNum, rule.lineNum,
                        ) {
                            /* Keep track of the earliest year after YEAR
                            in which some rule earlier than the current one
                            applies. */
                            if (year < rule.highYear) {
                                val next = maxOf(year + 1, rule.lowYear)
                                if (next <= nextYear) {
                                    nextYear = next
                                    tryNextYear = true
                                }
                            }
                            if (year in rule.lowYear..rule.highYear) {
                                rule.temp = rpytime(rule.dateRule, year.toInt()).atTimeExtended(rule.timeFromMidnight)
                                rule.todo = (rule.temp!! < y2038Boundary || year <= maxYear0)
                            } else {
                                rule.todo = false
                            }
                        }
                    }
                    while (true) {
                        var ktime = 0L
                        var untilTime = 0L
                        if (useUntil) {
                            /*
                            ** Turn untiltime into UT
                            ** assuming the current stdoff and save values.
                            */
                            untilTime = zone.untilTime!!.toInstant(TimeZone.UTC).epochSeconds
                            if (!zone.untilRule!!.timeType.timeFromMidnightIsUt) {
                                untilTime = safeAdd(untilTime, -zone.stdOff)
                            }
                            if (!zone.untilRule.timeType.timeFromMidnightIsStandardTime) {
                                untilTime = safeAdd(untilTime, -save)
                            }
                        }
                        /*
                        ** Find the rule (of those to do, if any)
                        ** that takes effect earliest in the year.
                        */
                        var k = -1
                        for (j in zone.rules?.indices ?: emptyList()) {
                            val rule = zone.rules!![j]
                            if (!rule.todo) continue
                            reporting(
                                zone.fileNum, zone.lineNum,
                                rule.fileNum, rule.lineNum,
                            ) {
                                var offset = if (rule.timeType.timeFromMidnightIsUt) 0 else zone.stdOff
                                if (!rule.timeType.timeFromMidnightIsStandardTime) {
                                    offset += save
                                }
                                val jtime = safeAdd(rule.temp!!.toInstant(TimeZone.UTC).epochSeconds, -offset)
                                if (k < 0 || jtime < ktime) {
                                    k = j
                                    ktime = jtime
                                } else if (jtime == ktime) {
                                    warning("two rules for same instant")
                                    reporting(
                                        zone.fileNum, zone.lineNum,
                                        zone.rules!![k].fileNum, zone.rules!![k].lineNum,
                                    ) {
                                        error("two rules for same instant")
                                    }
                                }
                            }
                        }
                        if (k < 0) break // go on to next year
                        val rule = zone.rules!![k]
                        rule.todo = false
                        if (useUntil && ktime >= untilTime) {
                            if (abbr == null && zone.stdOff + rule.save == startOff) {
                                abbr = doabbr(
                                    zone.abbrData(), letters = Letters.Actual(rule.abbrVar),
                                    isDst = zone.isDst, save = save, doQuotes = false
                                )
                            }
                            break
                        }
                        save = rule.save
                        if (useStart && ktime == startTime) {
                            useStart = false
                        }
                        if (useStart) {
                            if (ktime < startTime) {
                                startOff = zone.stdOff + save
                                abbr = doabbr(
                                    zone.abbrData(), letters = Letters.Actual(rule.abbrVar),
                                    isDst = rule.isDst, save = rule.save, doQuotes = false
                                )
                                continue
                            }
                            if (abbr == null && startOff == (zone.stdOff + save)) {
                                abbr = doabbr(
                                    zone.abbrData(), letters = Letters.Actual(rule.abbrVar),
                                    isDst = rule.isDst, save = rule.save, doQuotes = false
                                )
                            }
                        }
                        reporting(
                            zone.fileNum, zone.lineNum,
                            rule.fileNum, rule.lineNum,
                        ) {
                            val ab = doabbr(
                                zone.abbrData(), letters = Letters.Actual(rule.abbrVar),
                                isDst = rule.isDst, save = rule.save, doQuotes = false,
                            )
                            val offset = zone.stdOff + rule.save
                            val type = u.addtype(
                                bloatConfig, offset, ab,
                                isDst = rule.isDst,
                                rule.timeType,
                            )
                            if (defaultType == null && !rule.isDst)
                                defaultType = type
                            u.addtt(ktime, type)
                            if (nonTZlimtime < ktime && (useUntil || rule.highYear != Long.MAX_VALUE)) {
                                nonTZlimtime = ktime
                                nonTZlimtype = type
                            }
                        }
                    }
                    year = nextYear
                }
            }
            if (useStart) {
                val isDst = startOff != zone.stdOff
                if (abbr == null && !zone.format.isEmpty()) {
                    abbr = doabbr(
                        zone.abbrData(), Letters.DisablePercentS,
                        isDst = isDst, save = save, doQuotes = false
                    )
                }
                reporting(zone.fileNum, zone.lineNum) {
                    check(abbr != "") {
                        "can't determine time zone abbreviation " +
                            "to use just after until time"
                    }
                    val type = u.addtype(
                        bloatConfig, startOff, abbr!!, isDst, startTimeType
                    )
                    if (defaultType == null && !isDst)
                        defaultType = type
                    u.addtt(startTime, type)
                }
            }
            /*
            ** Now we may get to set starttime for the next zone line.
            */
            if (useUntil) {
                startTimeType = zone.untilRule!!.timeType
                startTime = zone.untilTime!!.toInstant(TimeZone.UTC).epochSeconds
                if (!zone.untilRule.timeType.timeFromMidnightIsUt)
                    startTime = safeAdd(startTime, -stdOff)
                if (!zone.untilRule.timeType.timeFromMidnightIsStandardTime)
                    startTime = safeAdd(startTime, -save)
            }
        }
    }
    if (defaultType == null)
        defaultType = 0u
    if (!doExtend && !bloatConfig.wantBloat()) {
        /* Keep trailing transitions that are no greater than this. */
       var keepAtMax = 0L
       /* The earliest transition into a time governed by the TZ string. */
       var TZstartTime = Long.MAX_VALUE
       for (atType in u.atTypes) {
           val at = atType.at
           if (nonTZlimtime < at && at < TZstartTime) {
               TZstartTime = at
           }
       }
       if (TZstartTime == Long.MAX_VALUE)
           TZstartTime = nonTZlimtime
       /* Omit trailing transitions deduible from the TZ string,
          and not needed for -r or -R. */
       keepAtMax = maxOf(TZstartTime, timeBoundConfig.redundantTime)
       val newAtTypes = buildList {
           for (atType in u.atTypes) {
               if (atType.at <= keepAtMax) {
                   add(U.AtType(
                        at = atType.at,
                        dontMerge = (
                            atType.at == TZstartTime
                            && (nonTZlimtype != atType.type || ',' in (envvar ?: ""))
                        ),
                        type = atType.type
                   ))
               }
           }
       }
       u.atTypes.clear()
       u.atTypes.addAll(newAtTypes)
    }
    if (doExtend) {
        /*
        ** If we're extending the explicitly listed observations for
        ** 400 years because we can't fill the proleptic TZ field,
        ** check whether we actually ended up explicitly listing
        ** observations through that period. If there aren't any
        ** near the end of the 400-year period, add a redundant
        ** one at the end of the final year, to make i tclear
        ** that we are claiming to have definite knowledge of
        ** the lack of transitions up to that point.
        */
        val xr = Rule.DateRule(
            month = Month.JANUARY,
            dayOfWeek = null,
            dayOfMonth = 1,
        )
        var lastAt = u.atTypes.maxByOrNull { it.at }
        if (lastAt == null || lastAt.at < rpytime(xr, (maxYear - 1).toInt()).atTime(0, 0).toInstant(TimeZone.UTC).epochSeconds) {
            val startTime = rpytime(xr, (maxYear + 1).toInt()).atTime(0, 0).toInstant(TimeZone.UTC).epochSeconds
            u.addtt(startTime, if (lastAt == null) defaultType else lastAt.type)
            u.atTypes.last().dontMerge = true
        }
    }
    writeZone(
        directory = directory,
        name = firstZone.name!!, string = envvar ?: "", version = version, defaultType = defaultType,
        u, timeBoundConfig, bloatConfig,
        unspecifiedType
    )
}

fun U.addtt(startTime: ZicType, type: UByte) {
    atTypes.add(U.AtType(at = startTime, dontMerge = false, type = type))
}

context (reporter: ErrorReporter)
fun U.addtype(
    config: BloatConfig,
    utOff: ZicType, abbr: String, isDst: Boolean, timeType: Rule.TimeOfDayType
): UByte {
    /* RFC 9636 section 3.2 specifies this range for utOff. */
    check(utOff in (Int.MIN_VALUE + 1)..Int.MAX_VALUE) { "UT offset out of range" }
    var timeType = if (config.wantBloat()) timeType else Rule.TimeOfDayType.Wall
    checkabbr(abbr)
    val newTypeDescr = U.TypeDescr(
        utOff = utOff,
        isDst = isDst,
        timeType = timeType,
        desig = abbr,
    )
    /* If there's already an entry, return its index. */
    val index = typeDescrs.indexOf(newTypeDescr)
    if (index != -1)
        return index.toUByteFailing()
    /* There isn't one; add a new one. */
    check(typeDescrs.size < 256) { "too many local time types" }
    typeDescrs.add(newTypeDescr)
    return (typeDescrs.size - 1).toUByte()
}

fun MutableList<U.Leap>.leapadd(
    config: TimeBoundConfig, t: ZicType, correction: Int, rolling: Boolean
) {
    var i = 0
    if (rolling && (config.loTime != MIN_TIME || config.hiTime != MAX_TIME)) {
        error("Rolling leap seconds not supported with -r")
    }
    while (i < this.size) {
        if (t <= this[i].trans)
            break
        ++i
    }
    add(i, U.Leap(trans = t, corr = correction.toLong(), roll = rolling))
}

fun MutableList<U.Leap>.adjleap() {
    var last: ZicType = 0
    var prevTrans: ZicType = 0
    forEach { cur ->
        check((cur.trans - prevTrans).seconds >= 28.days) { "Leap seconds too close together" }
        prevTrans = cur.trans
        cur.trans = safeAdd(prevTrans, last)
        cur.corr += last
        last = cur.corr
    }
}

/** Is `this` a space character in the C locale? */
fun Char.isAsciiSpace() = this in listOf(
    ' ', 0x0C.toChar() /* \f */, '\n', '\r', '\t', 0x0B.toChar() /* '\v' */
)

/** Is `this` alphabetic character in the C locale? */
fun Char.isAsciiAlpha() = (this in 'A'..'Z') || (this in 'a'..'z')

fun itsabbr(abbr: String, word: String): Boolean {
    if (abbr.firstOrNull()?.lowercase() != word.firstOrNull()?.lowercase())
        return false
    var wordIndex = 0
    for (abbrLetter in abbr) {
        wordIndex = word.indexOf(abbrLetter, startIndex = wordIndex, ignoreCase = true).let {
            if (it == -1) return false
            it + 1
        }
    }
    return true
}

/** Return true if [abbr] is an initial prefix of [word], ignoring ASCII case. */ 
fun ciprefix(abbr: String, word: String): Boolean = word.lowercase().startsWith(abbr.lowercase())

context (reporter: ErrorReporter)
fun <T> byWord(word: String, table: List<Pair<String, T>>): Pair<String, T>? {
    /* If `table` is LASTS and the word starts with `last`
    followed by a non-`-`, skip the "last" and look in `weekDayNames` instead.
    Warn about any usage of the undocumented prefix "last-". */
    val (updatedWord, updatedTable: List<Pair<String, T>>) = if (table === lasts && ciprefix("last", word) && word.length > 4) {
        if (word[4] == '-') {
            warning("\"$word\" is undocumented; use \"last${word.drop(5)}\" instead")
            word to table
        } else {
            @Suppress("UNCHECKED_CAST") // T = DayOfWeek
            word.drop(4) to (weekDayNames as List<Pair<String, T>>)
        }
    } else {
        word to table
    }
    /* Look for exact match */
    for (entry in updatedTable) {
        if (entry.first.equals(updatedWord, ignoreCase = true)) {
            return entry
        }
    }
    /* Look for inexact match */
    val match = updatedTable.singleOrNull { ciprefix(updatedWord, it.first) }
    if (match != null && reporter.noise) {
        var pre2017cMatch = false
        for ((first, _) in updatedTable) {
            if (itsabbr(updatedWord, first)) {
                if (pre2017cMatch) {
                    warning("\"${updatedWord}\" is ambiguous in pre-2017c zic")
                }
                pre2017cMatch = true
            }
        }
    }
    return match
}

fun getfields(cpStr: String): List<String> = buildList {
    var cIx = 0
    while (true) {
        while (cIx < cpStr.length && cpStr[cIx].isAsciiSpace())
            ++cIx
        if (cIx == cpStr.length || cpStr[cIx] == '#')
            break
        val dStart = cIx
        do {
            if (cpStr[cIx] == '"') {
                do {
                    ++cIx
                    check(cIx < cpStr.length) { "Odd number of quotation marks" }
                } while (cpStr[cIx] != '"')
            }
            ++cIx
        } while (cIx < cpStr.length && cpStr[cIx] != '#' && !cpStr[cIx].isAsciiSpace())
        val stringWithoutQuotes = cpStr.substring(dStart, cIx).replace("\"", "")
        add(if (stringWithoutQuotes == "-") "" else stringWithoutQuotes)
    }
}

private fun safeAdd(a: Long, b: Long): Long {
    val sum = a + b
    if ((a xor sum) < 0 && (a xor b) >= 0) {
        throw ArithmeticException("Addition overflows a long: $a + $b")
    }
    return sum
}

private fun safeMultiply(a: Long, b: Long): Long {
    if (b == 1L) {
        return a
    }
    if (a == 1L) {
        return b
    }
    if (a == 0L || b == 0L) {
        return 0
    }
    val total = a * b
    if (total / b != a || a == Long.MIN_VALUE && b == -1L || b == Long.MIN_VALUE && a == -1L) {
        throw ArithmeticException("Multiplication overflows a long: $a * $b")
    }
    return total
}

private fun Int.toUByteFailing(): UByte {
    if (this in 0..255) return toUByte()
    else error("Int $this exceeds the bounds of UByte")
}

/**
 * Given a [rule] and a [wantedYear], compute the date (in seconds since
 * January 1, 1970, 00:00 LOCAL time) in that year that the rule refers to.
 * Do not count leap seconds. On error, diagnose and exit.
 */
context (reporter: ErrorReporter)
fun rpytime(rule: Rule.DateRule, wantedYear: Int): LocalDate {
    val i = rule.dayOfMonth
    val dayOfMonth = if (rule.month == Month.FEBRUARY && i == 29 && !isLeapYear(wantedYear)) {
        check(rule.dayOfWeek is DayOfWeekRule.PreviousOrSame) { "Use of 2/29 in non leap-year" }
        i - 1
    } else {
        i
    }
    val date = LocalDate(wantedYear, rule.month, dayOfMonth).let {
        when (val dow = rule.dayOfWeek) {
            null -> it
            is DayOfWeekRule.PreviousOrSame -> it.previousOrSame(dow.dayOfWeek)
            is DayOfWeekRule.NextOrSame -> it.nextOrSame(dow.dayOfWeek)
        }
    }
    if (date.month != rule.month && reporter.noise) {
        warning(
            "rule goes past start/end of month; " +
            "will not work with pre-2004 versions of zic"
        )
    }
    return date
}

fun LocalDate.atTimeExtended(timeFromMidnight: ZicType): LocalDateTime =
    atTime(0, 0).toInstant(TimeZone.UTC)
        .plus(timeFromMidnight, DateTimeUnit.SECOND)
        .toLocalDateTime(TimeZone.UTC)

context (reporter: ErrorReporter)
fun checkabbr(string: String) {
    var warning: String? = null
    if (string.length < 3) {
        warning = "time zone abbreviation has fewer than 3 characters ($string)"
    }
    if (string.length > ZIC_MAX_ABBR_LEN_NO_WARN) {
        warning = "time zone abbreviation has too many characters ($string)"
    }
    if (!string.all {
          it.isAsciiAlpha() || it.isAsciiDigit() || it == '-' || it == '+'
    }) {
        warning = "time zone abbreviation differs from POSIX standard ($string)"
    }
    warning?.let { warning("$it ($string)") }
}

/**
 * Put into [chs], which currently contains strings containing
 * abbreviations none of which are suffixes of another,
 * the abbreviation [abbr].
 * If [abbr] does not already appear in [chs], possibly as a suffix of an
 * existing abbreviation, add [abbr] to [chs] and remove from [chs] any
 * abbreviation that is a suffix of [abbr].
 */
fun addabbr(chs: MutableList<String>, abbr: String) {
    for (i in chs.indices) {
        val current = chs[i]
        if (current.endsWith(abbr))
            return
        if (abbr.endsWith(current)) {
            chs[i] = abbr
            return
        }
    }
    chs.add(abbr)
}

/**
 * Finds the index of [abbr] + `'\0'` in a byte array resulting from
 * putting `'\0'`-separated [chs] strings.
 */
fun addabbrQuery(chs: List<String>, abbr: String): Int {
    var runningTotal = 0
    for (current in chs) {
        if (current.endsWith(abbr))
            return runningTotal + current.length - abbr.length
        runningTotal += current.length + 1
    }
    error("Not found")
}

