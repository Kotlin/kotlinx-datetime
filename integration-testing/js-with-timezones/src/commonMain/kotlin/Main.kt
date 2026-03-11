import kotlinx.datetime.TimeZoneContext

fun main() {
    println(TimeZoneContext.System.get("Europe/Berlin"))
}
