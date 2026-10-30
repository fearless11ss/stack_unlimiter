# Stack Unlimiter

Experimental Minecraft Forge 1.20.1 mod that stores stack counts as signed 64-bit values while preserving the vanilla `int` API as a compatibility facade.

## Current status

This is an early vertical slice, not a release-ready mod. It currently provides:

- an exact-count API (`StackCounts`);
- checked long arithmetic;
- `ItemStack` copy, split, grow, shrink, save, load, and comparison hooks;
- exact-count transport inside the existing item packet NBT shape;
- raised base `Slot` and Forge `ItemStackHandler` limits.

Inventories, menu interactions, item entities, crafting, commands, UI rendering, mod compatibility adapters, and migration safety still require dedicated integration tests and additional patches.

Both client and server must install exactly compatible versions. Worlds containing extended stacks must not be opened without the mod.

## Build

Use Java 17:

```powershell
$env:JAVA_HOME='C:\Program Files\Microsoft\jdk-17.0.21.21-hotspot'
.\gradlew.bat build
```
