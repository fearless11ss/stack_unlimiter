# Contributing to Stack Unlimiter

Contributions can include bug reports, compatibility findings, documentation,
tests, and code changes.

## Reporting bugs

Use the repository's bug report form and include:

- the exact Stack Unlimiter, Minecraft, and mod-loader versions;
- the source directory or downloaded file you tested;
- whether the problem occurs in singleplayer, on a dedicated server, or both;
- the smallest reliable reproduction sequence;
- expected and actual behavior;
- relevant logs, crash reports, screenshots, or videos; and
- the installed mod list and a result with only required dependencies.

Search existing issues first and back up affected worlds before reproducing
inventory or serialization problems.

## Repository layout

Each supported Minecraft and loader pair is an independent Gradle module:

- `forge-1.20.1`
- `fabric-1.20.1`
- `forge-1.21`
- `fabric-1.21`
- `neoforge-1.21`
- `forge-26.3`
- `fabric-26.3`
- `neoforge-26.3`

The maintained loader scope is Forge, Fabric, and NeoForge. Quilt ports are not
planned.

Make changes only in the module they target. When a fix applies to more than one
module, port and test it separately in every affected directory. Do not assume
that mixin targets or loader hooks are interchangeable between versions.

## Development setup

1. Install the Java version listed for the target in the root README.
2. Fork and clone the repository.
3. Change into the target module directory.
4. Import that directory as a Gradle project in the IDE.
5. Generate or refresh run configurations when required.

Useful commands for Forge modules:

```text
./gradlew clean build
./gradlew runClient
./gradlew runServer
./gradlew runGameTestServer
```

Useful commands for Fabric modules:

```text
./gradlew clean build
./gradlew runClient
./gradlew runServer
./gradlew runGametest    # Fabric 1.20.1 and 1.21
./gradlew runGameTest    # Fabric 26.3
```

NeoForge modules use the same `clean build`, `runClient`, `runServer`, and
`runGameTestServer` commands as Forge modules.

On Windows, use `gradlew.bat` instead of `./gradlew`.

## Implementation requirements

Changes to stack behavior must preserve these guarantees:

- exact oversized counts are never silently truncated;
- intentionally non-stackable items remain non-stackable;
- client and server behavior remains synchronized;
- saving, loading, commands, menus, crafting, and transfers do not create or
  delete items;
- vanilla-sized stacks continue to behave normally; and
- unsupported modded behavior falls back safely whenever possible.

Prefer focused mixins with narrow injection points. Document compatibility
decisions and avoid broad changes to unrelated vanilla behavior.

## Testing

Before submitting a pull request:

1. Run `clean build` inside every modified module.
2. Run the module's GameTest task for changes affecting game behavior.
3. Test both singleplayer and dedicated-server behavior when networking,
   inventories, menus, or synchronization are involved.
4. Test counts above vanilla limits and near important numeric boundaries.
5. Confirm that no item duplication or item loss occurs.

Add or update automated tests for reproducible fixes when practical. Include
manual testing steps and known limitations in the pull request description.

Do not include generated build output, game directories, logs, credentials, or
unrelated formatting changes. If generative AI assisted with a contribution,
disclose that use in the pull request. Contributors remain responsible for
reviewing and understanding their changes.

## License

By submitting a contribution, you agree that it will be licensed under the
repository's [MIT License](LICENSE.txt).
