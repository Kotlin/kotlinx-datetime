# We depend on kotlinx-serialization as compileOnly.
# If the serializers don't get used, the library works properly even without the
# kotlinx-serialization dependency, but Proguard emits warnings about datetime
# classes mentioning some serialization-related entities.
# These rules should not cause problems: if a project actually relies on
# serialization, then much more than just these two classes will be required,
# so telling Proguard not to worry if these two are missing will not prevent it
# from emitting errors for code that does use serialization but somehow forgot
# to depend on it.
-dontwarn kotlinx.serialization.KSerializer
-dontwarn kotlinx.serialization.Serializable
