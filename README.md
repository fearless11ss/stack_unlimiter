# Stack Unlimiter

Experimental Minecraft Forge 1.20.1 mod that stores stack counts as signed 64-bit values while preserving the vanilla `int` API as a compatibility facade.

## Current status

This is an early vertical slice, not a release-ready mod. It currently provides:

- an exact-count API (`StackCounts`);
- checked long arithmetic;
- `ItemStack` copy, split, grow, shrink, save, load, and comparison hooks;
- exact-count transport inside the existing item packet NBT shape;
- raised base `Slot` and Forge `ItemStackHandler` limits;
- single-stack insertion for eligible items created with `/give`;
- vanilla crafting recipes accept oversized ingredient stacks;
- recipe-book placement returns ingredients to an existing compatible stack instead of scattering them across free slots;
- Shift-click recipe placement uses all available oversized ingredients instead of counting only 64;
- Shift-clicking a crafting result processes compatible recipes in one bulk operation instead of crafting items one by one;
- compact, slot-width-limited count labels such as `2K` for `2004` items.

Further inventory and menu interactions, item entities, complex crafting interactions, other command paths, mod compatibility adapters, and migration safety still require dedicated integration tests and additional patches.

Both client and server must install exactly compatible versions. Worlds containing extended stacks must not be opened without the mod.

Every completed change increments the mod version. Published artifact versions do not use pre-release suffixes such as `alpha`.

## Build

Use Java 17:

```powershell
$env:JAVA_HOME='C:\Program Files\Microsoft\jdk-17.0.21.21-hotspot'
.\gradlew.bat build
```
