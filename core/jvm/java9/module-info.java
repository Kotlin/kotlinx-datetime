module kotlinx.datetime {
    requires transitive kotlin.stdlib;
    requires static kotlinx.serialization.core;

    exports kotlinx.datetime;
    exports kotlinx.datetime.serializers;
    exports kotlinx.datetime.format;
    exports kotlinx.datetime.internal to kotlinx.datetime.zoneinfo;
}
