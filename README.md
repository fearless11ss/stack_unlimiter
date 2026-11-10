# Stack Unlimiter

Stack Unlimiter removes the practical stack-size limit for normally stackable
items while keeping intentionally non-stackable items non-stackable. Exact
counts are stored as signed 64-bit values and synchronized between the client
and server.

The mod must be installed on both sides. Worlds containing oversized stacks
depend on the mod and should be backed up before installation or updates.

## Available builds

| Source directory | Minecraft | Mod loader | Mod version | Java |
| --- | --- | --- | --- | --- |
| [`forge-1.20.1`](forge-1.20.1/) | 1.20.1 | Forge | 0.1.11 | 17 |
| [`fabric-1.20.1`](fabric-1.20.1/) | 1.20.1 | Fabric | 0.1.12 | 17 |
| [`forge-1.21`](forge-1.21/) | 1.21 | Forge | 0.1.13 | 21 |
| [`fabric-1.21`](fabric-1.21/) | 1.21 | Fabric | 0.1.15 | 21 |
| [`neoforge-1.21`](neoforge-1.21/) | 1.21 | NeoForge | 0.1.17 | 21 |
| [`forge-26.3`](forge-26.3/) | 26.3 | Forge | 0.1.16 | 25 |
| [`fabric-26.3`](fabric-26.3/) | 26.3 | Fabric | 0.1.9 | 25 |
| [`neoforge-26.3`](neoforge-26.3/) | 26.3 | NeoForge | 0.1.9 | 25 |

Every directory is an independent Gradle module with its own wrapper,
dependencies, source files, resources, unit tests, and integration tests. Run
Gradle from the directory for the exact Minecraft and loader pair you want.

For architecture, subsystem ownership, testing commands, and bug-diagnosis
routes, see the [Obsidian repository map](REPOSITORY_MAP.md).
Release-specific publication text is stored in the
[release changelog catalog](RELEASE_CHANGELOGS.md).

## Build

On Windows:

```powershell
cd forge-1.20.1
.\gradlew.bat clean build

cd ..\fabric-1.20.1
.\gradlew.bat clean build

cd ..\forge-1.21
.\gradlew.bat clean build

cd ..\fabric-1.21
.\gradlew.bat clean build

cd ..\neoforge-1.21
.\gradlew.bat clean build

cd ..\forge-26.3
.\gradlew.bat clean build

cd ..\fabric-26.3
.\gradlew.bat clean build

cd ..\neoforge-26.3
.\gradlew.bat clean build
```

On Linux or macOS, use `./gradlew` instead. The resulting JAR is written to the
selected module's `build/libs` directory.

Fabric 26.3 requires the Gradle process itself to run on Java 25. Forge 26.3
and NeoForge 26.3 use Java 25 toolchains. Forge, Fabric, and NeoForge 1.21
target Java 21, while Forge and Fabric 1.20.1 target Java 17.

## Implemented behavior

- exact oversized counts survive saving, loading, and network synchronization;
- `/give` combines eligible items into one stack;
- player inventory insertion and compatible slot transfers merge oversized
  stacks;
- recipe accounting sees the exact ingredient count;
- recipe-book Shift-click placement distributes all available ingredients;
- compatible Shift-click crafting operations are processed in bulk;
- large count labels are compacted and scaled to remain inside the slot; and
- items whose normal maximum is one remain non-stackable.

## Compatibility

The maintained loader scope is Forge, Fabric, and NeoForge. Quilt builds are
not planned.

Stack Unlimiter changes a core Minecraft invariant, so compatibility with every
mod is not possible. Custom inventories, menus, crafting systems, or transfer
implementations may retain their own limits or require dedicated adapters.

Report reproducible problems through
[GitHub Issues](https://github.com/fearless11ss/stack_unlimiter/issues).

## License

Stack Unlimiter is distributed under the [MIT License](LICENSE.txt).
