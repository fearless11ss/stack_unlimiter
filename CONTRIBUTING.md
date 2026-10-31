# Contributing to Stack Unlimiter

Thank you for helping improve Stack Unlimiter. Contributions can include bug
reports, compatibility findings, documentation updates, tests, and code changes.

## Reporting bugs

Use the repository's bug report form and provide as much of the following as
possible:

- the exact Stack Unlimiter, Minecraft, and Forge versions;
- whether the problem occurs in singleplayer, on a dedicated server, or both;
- the smallest reliable sequence of steps that reproduces the problem;
- the expected and actual behavior;
- relevant logs, crash reports, screenshots, or videos;
- a list of other installed mods; and
- whether the problem still occurs with only Forge and Stack Unlimiter installed.

Search existing issues before opening a new one. Back up affected worlds before
reproducing inventory or serialization problems.

## Suggesting changes

Open an issue before starting a large feature or architectural change. Describe
the use case, expected behavior, compatibility impact, and any changes required
to saved worlds or network data. Small fixes and documentation improvements may
be submitted directly as pull requests.

## Development setup

1. Install the JDK version required by the current Forge toolchain.
2. Fork and clone the repository.
3. Check `gradle.properties` for the currently targeted Minecraft and Forge
   versions.
4. Import the Gradle project into your IDE.
5. Generate or refresh IDE run configurations when required by your setup.

On Windows, use `gradlew.bat`. On Linux and macOS, use `./gradlew`.

Useful commands:

```text
./gradlew build
./gradlew runClient
./gradlew runServer
./gradlew runGameTestServer
```

## Implementation requirements

Changes to stack behavior should preserve the core guarantees of the project:

- exact oversized counts are stored without silently truncating them to vanilla
  limits;
- items that are intentionally non-stackable remain non-stackable;
- client and server behavior stays synchronized;
- saving, loading, commands, menus, crafting, and item transfers do not create
  or delete items;
- vanilla-sized stacks continue to behave normally; and
- unsupported modded behavior falls back safely whenever possible.

Prefer focused mixins with narrowly defined injection points. Document unusual
compatibility decisions and avoid broad changes that affect unrelated vanilla
behavior.

## Testing

Before submitting a pull request:

1. Run `./gradlew build`.
2. Run `./gradlew runGameTestServer` for changes that affect game behavior.
3. Test both singleplayer and dedicated-server behavior when networking,
   inventories, menus, or synchronization are involved.
4. Test with counts above vanilla limits and near important numeric boundaries.
5. Confirm that no item duplication or item loss occurs.

Add or update automated tests for every reproducible bug fix when practical.
Include manual testing steps in the pull request description.

## Pull requests

Keep each pull request focused on one change. The description should explain:

- what changed and why;
- how the change was tested;
- any known limitations or compatibility risks; and
- whether saved-world or network compatibility is affected.

Do not include generated build output, game directories, logs, credentials, or
unrelated formatting changes. Write clear commit messages that describe the
purpose of the change.

If generative AI assisted with a contribution, disclose that use in the pull
request. Contributors remain responsible for reviewing, understanding, testing,
and having the right to submit every part of their contribution.

## License

By submitting a contribution, you agree that it will be licensed under the
repository's [MIT License](LICENSE.txt).
