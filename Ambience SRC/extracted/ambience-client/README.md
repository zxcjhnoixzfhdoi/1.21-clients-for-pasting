# Ambience — reconstructed client (source)

Complete, self-contained, compilable source for the Ambience Minecraft cheat
client, reconstructed from the offline extractor for authorized security
research. Everything needed to build a valid client jar lives in this one
folder — source, resources, Gradle build, and a bundled Gradle wrapper.

> Research reconstruction. Auth is pointed at `localhost` (see below). Some
> non-essential pieces are stubbed to compile — noted under **Stubs**.

## Layout

```
build.gradle / settings.gradle / gradle.properties   Fabric-Loom build config
gradlew / gradle/                                     bundled Gradle 9.6.1 wrapper
src/main/java/dev/ambience/                           the mod (all 42 modules, events, mixins, loader, GUI)
src/main/java/com/mixininject/                        reconstructed MixinInject framework
    api/                                              @Mixin/@Inject/@Overwrite/@Cancel + AgentHooks
    agent/                                            Java-agent bootstrap + ASM weaving engine
src/main/resources/                                   mixins.json, accesswidener, fabric.mod.json,
                                                      assets, payload, TLS roots, agent bootstrap
```

## Build

```sh
./gradlew build          # first run needs network for MC/yarn/fabric deps
./gradlew build --offline # after deps are cached
```

Output: `build/libs/ambience-recon-1.0.0-recon.jar` (~700 KB).

Toolchain: Java 21, Fabric-Loom 1.14.10, Gradle 9.6.1 (wrapper bundled),
Minecraft 1.21.11, yarn `1.21.11+build.1`, loader `0.19.2`,
fabric-api `0.141.4+1.21.11`.

## Run

Two entry points, both from the one jar:

1. **Fabric mod** — drop the jar in `mods/`. `fabric.mod.json` is the entry.
2. **MixinInject agent** — `java -javaagent:ambience-recon-1.0.0-recon.jar=<mods-dir> ...`
   The manifest carries `Premain-Class`/`Agent-Class` =
   `com.mixininject.agent.MixinAgent`. The agent reads
   `META-INF/mixininject.bootstrap`, weaves mixins at class-load into the Knot
   classloader, and boots the payload.

## Auth = localhost

Real auth server commented out, swapped for localhost in
`src/main/java/dev/ambience/util/BuildConfig.java`:

```java
// public static final String AUTH_SERVER = "https://auth.ambienceclient.com"; // original
public static final String AUTH_SERVER = System.getenv("AMBIENCE_AUTH_SERVER") != null
    ? System.getenv("AMBIENCE_AUTH_SERVER") : "http://localhost:8080";
```

Override at runtime with `AMBIENCE_AUTH_SERVER` / `AMBIENCE_AUTH_TOKEN`.
A minimal stand-in server + client live one level up (`server.py`, `client.py`).

## Stubs

Compiled but not functional (documented, not load-bearing): loader auth,
some GUI widgets + font rendering, `LowFirePack` filesystem writer. Everything
else — all 42 feature modules, event bus, the MixinInject engine — is real
reconstructed source.
