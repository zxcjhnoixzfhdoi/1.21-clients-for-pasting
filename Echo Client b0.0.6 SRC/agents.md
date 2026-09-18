This repo uses Stonecutter for Minecraft multiversion support. Keep shared logic version-agnostic, put version-specific branching behind Stonecutter macros in small compat classes under src/main/java/hack/echo/client/api, preserve both 1.21.11 and 26.0+ support, default/reset active version is 1.21.11, 26.0+ uses unobfuscated Loom + official access widener, and optimize for very readable, skimmable code with early returns. Version past 26.2 have native Vulkan support.

Echo\stonecutter.gradle.kts is used to manage preprocessor settings
After changing a value you must run `.\gradlew "Refresh active project"` 

To compile for a single version run `.\gradlew 26.2-snapshot-2:build`

When adding support for a new Minecraft minor line (e.g. 26.2), do not modify `*_26_1` mixins. Create dedicated `*_26_2` mixins and wire them in `echo.mixins.26.2.json`. Use `>X.Y.Z` (last patch of the previous minor) as the Stonecutter version predicate, not `~X.Y` — pre-release versions like `26.2-snapshot-2` parse below `26.2` in semver and won't match `~26.2`.

Mixin file version suffixes denote the start of the range, not a single version. `MixinFoo_1_21_11` covers all versions up to the next breaking change (e.g. `<=26.1.2`), not just 1.21.11.