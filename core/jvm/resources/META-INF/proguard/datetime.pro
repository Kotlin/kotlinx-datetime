# We depend on kotlinx-serialization as compileOnly.
# If the serializers don't get used, the library works properly even without the
# kotlinx-serialization dependency, but Proguard emits warnings about datetime
# classes mentioning some serialization-related entities, for example:
# Missing class kotlinx.serialization.KSerializer (referenced from: kotlinx.datetime.serializers.InstantIso8601Serializer)
# Missing class kotlinx.serialization.Serializable (referenced from: kotlinx.datetime.Instant)
-dontwarn kotlinx.datetime.**
