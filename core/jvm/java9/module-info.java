module kotlinx.datetime {
    requires transitive kotlin.stdlib;
    requires transitive static kotlinx.serialization.core;

    exports kotlinx.datetime;
    exports kotlinx.datetime.serializers;
}
