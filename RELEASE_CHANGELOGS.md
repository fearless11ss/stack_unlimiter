---
title: Stack Unlimiter Release Changelogs
aliases:
  - Release Changelogs
  - Каталог changelog
tags:
  - stack-unlimiter
  - releases
  - publishing
status: active
updated: 2026-11-11
---

# Stack Unlimiter — каталог релизных changelog

> [!important] Источник истины для публикации
> Перед загрузкой файла на Modrinth, CurseForge или другую площадку найди здесь точную пару Minecraft + loader + версия мода и используй сохранённый Markdown. После подготовки нового changelog сразу добавь его сюда.

> [!note] Описание проекта и changelog — разные тексты
> Общее описание проекта остаётся независимым от конкретной версии Minecraft. Версию Minecraft, loader и версию мода указываем только в metadata файла, display name и changelog конкретного релиза.

## Индекс релизов

| Minecraft | Loader | Версия мода | Java | Площадки | Статус | Changelog |
| --- | --- | --- | --- | --- | --- | --- |
| 1.20.1 | Forge | `0.1.7` | 17 | Modrinth, CurseForge | исторический релиз | Initial public beta |
| 1.20.1 | Forge | `0.1.11` | 17 | Modrinth, CurseForge | загружен | Inventory fixes |
| 1.20.1 | Forge | `0.1.12` | 17 | Modrinth, CurseForge | готовится к публикации | Shulker tooltip count fix |
| 1.20.1 | Fabric | `0.1.12` | 17 | Modrinth, CurseForge | загружен | Initial Fabric release |
| 1.21 | Forge | `0.1.13` | 21 | Modrinth, CurseForge | загружен | Initial Forge 1.21 release |
| 1.21 | Forge | `0.1.14` | 21 | — | не публиковался; заменён `0.1.15` | Non-stackable `/give` fix |
| 1.21 | Forge | `0.1.15` | 21 | Modrinth, CurseForge | загружен | Inventory and shulker tooltip fixes |
| 1.21 | Fabric | `0.1.15` | 21 | Modrinth, CurseForge | загружен | Initial Fabric 1.21 release |
| 1.21 | Fabric | `0.1.16` | 21 | Modrinth, CurseForge | загружен | Inventory and shulker tooltip fixes |
| 1.21 | NeoForge | `0.1.17` | 21 | Modrinth, CurseForge | загружен | Initial NeoForge 1.21 release |
| 1.21 | NeoForge | `0.1.18` | 21 | Modrinth, CurseForge | загружен | Shulker tooltip count fix |
| 26.3 | Forge | `0.1.16` | 25 | Modrinth, CurseForge | загружен | Initial Forge 26.3 release |
| 26.3 | Forge | `0.1.17` | 25 | Modrinth, CurseForge | загружен | Inventory and container tooltip fixes |
| 26.3 | Fabric | `0.1.9` | 25 | Modrinth, CurseForge | загружен | Initial Fabric release |
| 26.3 | Fabric | `0.1.10` | 25 | Modrinth, CurseForge | загружен | Inventory and container tooltip fixes |
| 26.3 | NeoForge | `0.1.9` | 25 | Modrinth, CurseForge | загружен | Initial NeoForge release |
| 26.3 | NeoForge | `0.1.10` | 25 | Modrinth, CurseForge | загружен | Inventory and container tooltip fixes |

> [!success] Ранее опубликованные сборки загружены
> Forge 1.20.1 `0.1.11`, Fabric 1.20.1 `0.1.12`, Forge 1.21 `0.1.15`, Fabric 1.21 `0.1.16`, NeoForge 1.21 `0.1.18`, Forge 26.3 `0.1.17`, Fabric 26.3 `0.1.10` и NeoForge 26.3 `0.1.10` загружены на Modrinth и CurseForge.

## Общие поля публикации

| Поле | Значение |
| --- | --- |
| Display name | `Stack Unlimiter <mod version> — <loader> <Minecraft version>` |
| Environment | Client и Server; мод обязателен с обеих сторон |
| Release type | Release |
| Dependencies | Только соответствующий mod loader; Fabric API для Fabric-сборок игроку не требуется |
| Project description | Без привязки к конкретной версии Minecraft |

## Forge 1.20.1 — 0.1.7

> [!info] Источник
> Исторический текст зафиксирован по опубликованной карточке первого публичного beta-релиза.

```markdown
# Initial public beta

## Features

- Supports stackable item counts beyond the vanilla limit of 64
- Stores exact stack counts using signed 64-bit values
- Keeps intentionally non-stackable items non-stackable
- Saves and synchronizes oversized stacks
- Combines items received through `/give` into a single stack
- Supports oversized ingredients in crafting recipes
- Supports recipe-book placement with large ingredient stacks
- Processes compatible Shift-click crafting operations in bulk
- Displays large stack counts using compact labels
- Supports player inventories and compatible Forge item handlers

## Important

- The mod must be installed on both the client and server
- Back up your worlds before installing or updating
- Worlds containing oversized stacks depend on this mod
- Compatibility with every mod is not guaranteed
- Custom inventories and crafting systems may retain their own limits
```

## Forge 1.20.1 — 0.1.11

> [!warning] Каноническая реконструкция
> Текст собран по коммитам исправлений `0.1.8–0.1.11`. Он считается каноническим для повторной публикации, но не заявляется как дословная копия уже размещённого changelog.

```markdown
# Inventory interaction and synchronization fixes

## Fixed

- Preserved oversized stacks across player inventory and vanilla container operations
- Fixed dropping an entire oversized stack from the selected hotbar slot and inventory menus
- Fixed item pickup so exact counts grow beyond 64 and synchronize to the client
- Fixed exact count labels for values between 65 and 999
- Preserved exact counts during left-click, right-click, Shift-click, drag, number-key, and double-click operations
- Kept bulk crafting and recipe-book placement compatible with oversized ingredients

## Important

- The mod must be installed on both the client and server
- Back up your worlds before installing or updating
- Worlds containing oversized stacks depend on this mod
- Compatibility with every mod is not guaranteed
```

## Forge 1.20.1 — 0.1.12

> [!note] Подготовлено к публикации
> Release, display name `Stack Unlimiter 0.1.12 — Forge 1.20.1`, Minecraft `1.20.1`, loader `Forge`, Java 17, окружение Client + Server, без дополнительных зависимостей.

```markdown
# Shulker tooltip count fix

## Fixed

- Fixed shulker-box tooltips displaying `64` instead of the exact contained stack count
- Preserved signed 64-bit counts when displaying shulker-box contents
- Added regression coverage for non-stackable `/give` and shulker-box tooltips

## Important

- The mod must be installed on both the client and server
- Worlds containing oversized stacks depend on this mod
- Back up your worlds before installing or updating
- Compatibility with every mod is not guaranteed
```

## Forge 1.21 — 0.1.13

> [!success] Загрузка на Modrinth и CurseForge подтверждена
> Файл загружен на обе площадки как Release с display name `Stack Unlimiter 0.1.13 — Forge 1.21`, Minecraft `1.21`, loader `Forge`, окружением Client + Server и без дополнительных зависимостей.

```markdown
# Initial Forge release for Minecraft 1.21

Stack Unlimiter is now available for Forge on Minecraft 1.21.

## Features

- Supports stackable item counts beyond the vanilla limit of 64
- Stores exact stack counts using signed 64-bit values
- Keeps intentionally non-stackable items non-stackable
- Supports player inventories, vanilla containers, and compatible Forge item handlers
- Supports item pickup, dropping, `/give`, and standard inventory interactions
- Supports left-click, right-click, Shift-click, number keys, double-click, and drag distribution
- Supports crafting, bulk Shift-click crafting, and recipe-book placement
- Displays large stack counts using compact labels
- Preserves oversized stacks during saving and synchronization

## Important

- The mod must be installed on both the client and server
- Worlds containing oversized stacks depend on this mod
- Back up your worlds before installing or updating
- Compatibility with every mod is not guaranteed
```

## Forge 1.21 — 0.1.14

> [!warning] Сборка не публиковалась
> Версия заменена `0.1.15`; отдельная загрузка `0.1.14` не требуется.

```markdown
# Non-stackable item give fix

## Fixed

- Fixed `/give` silently losing intentionally non-stackable items such as tools and shulker boxes
- Preserved item types in zero-count copies used by vanilla inventory insertion
- Prevented inventory insertion from mutating Minecraft's shared empty-stack singleton

## Important

- The mod must be installed on both the client and server
- Worlds containing oversized stacks depend on this mod
- Back up your worlds before installing or updating
- Compatibility with every mod is not guaranteed
```

## Forge 1.21 — 0.1.15

> [!success] Загрузка на Modrinth и CurseForge подтверждена
> Файл загружен на обе площадки как Release с display name `Stack Unlimiter 0.1.15 — Forge 1.21`, Minecraft `1.21`, loader `Forge`, Java 21, окружением Client + Server и без дополнительных зависимостей.

```markdown
# Inventory and shulker tooltip fixes

## Fixed

- Fixed `/give` silently losing intentionally non-stackable items such as tools and shulker boxes
- Fixed shulker-box tooltips displaying `64` instead of the exact contained stack count
- Preserved item types in zero-count copies used by vanilla inventory insertion
- Prevented inventory insertion from mutating Minecraft's shared empty-stack singleton
- Preserved signed 64-bit counts when displaying shulker-box contents

## Important

- The mod must be installed on both the client and server
- Worlds containing oversized stacks depend on this mod
- Back up your worlds before installing or updating
- Compatibility with every mod is not guaranteed
```

## Forge 26.3 — 0.1.16

> [!warning] Каноническая реконструкция
> Текст собран по реализации и серии исправлений Forge 26.3 до `0.1.16`.

```markdown
# Initial Forge release

Stack Unlimiter is now available for Forge on Minecraft 26.3.

## Features

- Supports stackable item counts beyond the vanilla limit of 64
- Stores exact stack counts using signed 64-bit values
- Keeps intentionally non-stackable items non-stackable
- Supports player inventories, vanilla containers, and compatible Forge item handlers
- Supports item pickup, dropping, `/give`, and all standard inventory interactions
- Supports crafting, bulk Shift-click crafting, and recipe-book placement
- Displays large stack counts using compact labels
- Preserves oversized stacks during saving and synchronization

## Fixes included

- Keeps oversized inventory transfers atomic instead of splitting them into vanilla-sized stacks
- Splits oversized stacks using their exact count
- Keeps bulk crafting authoritative on the server
- Uses exact counts for drag previews, double-click collection, and nearby item pickup

## Important

- The mod must be installed on both the client and server
- Worlds containing oversized stacks depend on this mod
- Back up your worlds before installing or updating
- Compatibility with every mod is not guaranteed
```

## Forge 26.3 — 0.1.17

> [!success] Загрузка на Modrinth и CurseForge подтверждена
> Файл загружен на обе площадки как Release с display name `Stack Unlimiter 0.1.17 — Forge 26.3`, Minecraft `26.3`, loader `Forge`, Java 25, окружением Client + Server и без дополнительных зависимостей.

```markdown
# Inventory and container tooltip fixes

## Fixed

- Fixed `/give` silently losing intentionally non-stackable items such as tools and shulker boxes
- Fixed container and shulker-box tooltips displaying `64` instead of the exact contained stack count
- Preserved item types in zero-count copies used by vanilla inventory insertion
- Prevented inventory insertion from mutating Minecraft's shared empty-stack singleton
- Preserved signed 64-bit counts when displaying container contents

## Important

- The mod must be installed on both the client and server
- Worlds containing oversized stacks depend on this mod
- Back up your worlds before installing or updating
- Compatibility with every mod is not guaranteed
```

## Fabric 1.21 — 0.1.15

> [!success] Загрузка на Modrinth и CurseForge подтверждена
> Файл загружен на обе площадки как Release с display name `Stack Unlimiter 0.1.15 — Fabric 1.21`, Minecraft `1.21`, loader `Fabric`, окружением Client + Server и без обязательной зависимости от Fabric API.

```markdown
# Initial Fabric release for Minecraft 1.21

Stack Unlimiter is now available for Fabric on Minecraft 1.21.

## Features

- Supports stackable item counts beyond the vanilla limit of 64
- Stores exact stack counts using signed 64-bit values
- Keeps intentionally non-stackable items non-stackable
- Supports player inventories and vanilla containers
- Supports item pickup, dropping, `/give`, and standard inventory interactions
- Supports left-click, right-click, Shift-click, number keys, double-click, and drag distribution
- Supports crafting, bulk Shift-click crafting, and recipe-book placement
- Displays large stack counts using compact labels
- Preserves oversized stacks during saving and synchronization
- Does not require Fabric API at runtime

## Fixes included

- Fixed a startup crash in remapped Fabric installations

## Important

- The mod must be installed on both the client and server
- Worlds containing oversized stacks depend on this mod
- Back up your worlds before installing or updating
- Compatibility with every mod is not guaranteed
```

## Fabric 1.21 — 0.1.16

> [!success] Загрузка на Modrinth и CurseForge подтверждена
> Файл загружен на обе площадки как Release с display name `Stack Unlimiter 0.1.16 — Fabric 1.21`, Minecraft `1.21`, loader `Fabric`, Java 21, окружением Client + Server и без обязательной зависимости от Fabric API.

```markdown
# Inventory and shulker tooltip fixes

## Fixed

- Fixed `/give` silently losing intentionally non-stackable items such as tools and shulker boxes
- Fixed shulker-box tooltips displaying `64` instead of the exact contained stack count
- Preserved item types in zero-count copies used by vanilla inventory insertion
- Prevented inventory insertion from mutating Minecraft's shared empty-stack singleton
- Preserved signed 64-bit counts when displaying shulker-box contents

## Important

- The mod must be installed on both the client and server
- Worlds containing oversized stacks depend on this mod
- Back up your worlds before installing or updating
- Compatibility with every mod is not guaranteed
```

## NeoForge 1.21 — 0.1.17

> [!success] Загрузка на Modrinth и CurseForge подтверждена
> Файл загружен на обе площадки как Release с display name `Stack Unlimiter 0.1.17 — NeoForge 1.21`, Minecraft `1.21`, loader `NeoForge`, Java 21 и окружением Client + Server.

```markdown
# Initial NeoForge release for Minecraft 1.21

Stack Unlimiter is now available for NeoForge on Minecraft 1.21.

## Features

- Supports stackable item counts beyond the vanilla limit of 64
- Stores exact stack counts using signed 64-bit values
- Keeps intentionally non-stackable items non-stackable
- Supports player inventories, vanilla containers, and compatible NeoForge item handlers
- Supports item pickup, dropping, `/give`, and standard inventory interactions
- Supports left-click, right-click, Shift-click, number keys, double-click, and drag distribution
- Supports crafting, bulk Shift-click crafting, and recipe-book placement
- Displays large stack counts using compact labels
- Preserves oversized stacks during saving and synchronization

## Fixes included

- Fixed `/give` silently losing intentionally non-stackable items such as tools and shulker boxes

## Important

- The mod must be installed on both the client and server
- Worlds containing oversized stacks depend on this mod
- Back up your worlds before installing or updating
- Compatibility with every mod is not guaranteed
```

## NeoForge 1.21 — 0.1.18

> [!success] Загрузка на Modrinth и CurseForge подтверждена
> Файл загружен на обе площадки как Release с display name `Stack Unlimiter 0.1.18 — NeoForge 1.21`, Minecraft `1.21`, loader `NeoForge`, Java 21, окружением Client + Server и без дополнительных зависимостей.

```markdown
# Shulker tooltip count fix

## Fixed

- Fixed shulker-box tooltips displaying `64` instead of the exact contained stack count
- Preserved signed 64-bit counts when displaying shulker-box contents

## Important

- The mod must be installed on both the client and server
- Worlds containing oversized stacks depend on this mod
- Back up your worlds before installing or updating
- Compatibility with every mod is not guaranteed
```

## Fabric 1.20.1 — 0.1.12

> [!success] Загрузка на Modrinth и CurseForge подтверждена
> Файл загружен на обе площадки как Release с display name `Stack Unlimiter 0.1.12 — Fabric 1.20.1`, Minecraft `1.20.1`, loader `Fabric`, окружением Client + Server и без обязательной зависимости от Fabric API.

```markdown
# Initial Fabric release

Stack Unlimiter is now available for Fabric on Minecraft 1.20.1.

## Features

- Supports stackable item counts beyond the vanilla limit of 64
- Stores exact stack counts using signed 64-bit values
- Keeps intentionally non-stackable items non-stackable
- Supports player inventories and vanilla containers
- Supports item pickup, dropping, `/give`, and inventory interactions
- Supports left-click, right-click, Shift-click, number keys, double-click, and drag distribution
- Supports crafting, bulk Shift-click crafting, and recipe-book placement
- Displays large stack counts using compact labels
- Preserves oversized stacks during saving and synchronization
- Does not require Fabric API at runtime

## Important

- The mod must be installed on both the client and server
- Worlds containing oversized stacks depend on this mod
- Back up your worlds before installing or updating
- Compatibility with every mod is not guaranteed
```

## Fabric 26.3 — 0.1.9

```markdown
# Initial Fabric release

Stack Unlimiter is now available for Fabric.

## Features

- Supports stackable item counts beyond the vanilla limit of 64
- Stores exact stack counts using signed 64-bit values
- Keeps intentionally non-stackable items non-stackable
- Supports player inventories and vanilla containers
- Supports item pickup, dropping, `/give`, and inventory interactions
- Supports left-click, right-click, Shift-click, number keys, double-click, and drag distribution
- Supports crafting, bulk Shift-click crafting, and recipe-book placement
- Displays large stack counts using compact labels
- Preserves oversized stacks during saving and synchronization
- Does not require Fabric API at runtime

## Important

- The mod must be installed on both the client and server
- Worlds containing oversized stacks depend on this mod
- Back up your worlds before installing or updating
- Compatibility with every mod is not guaranteed
```

## Fabric 26.3 — 0.1.10

> [!success] Загрузка на Modrinth и CurseForge подтверждена
> Файл загружен на обе площадки как Release с display name `Stack Unlimiter 0.1.10 — Fabric 26.3`, Minecraft `26.3`, loader `Fabric`, Java 25, окружением Client + Server и без обязательной зависимости от Fabric API.

```markdown
# Inventory and container tooltip fixes

## Fixed

- Fixed `/give` silently losing intentionally non-stackable items such as tools and shulker boxes
- Fixed container and shulker-box tooltips displaying `64` instead of the exact contained stack count
- Preserved item types in zero-count copies used by vanilla inventory insertion
- Prevented inventory insertion from mutating Minecraft's shared empty-stack singleton
- Preserved signed 64-bit counts when displaying container contents

## Important

- The mod must be installed on both the client and server
- Worlds containing oversized stacks depend on this mod
- Back up your worlds before installing or updating
- Fabric API is not required at runtime
- Compatibility with every mod is not guaranteed
```

## NeoForge 26.3 — 0.1.9

> [!success] Загрузка на Modrinth и CurseForge подтверждена
> Файл загружен на обе площадки как Release с display name `Stack Unlimiter 0.1.9 — NeoForge 26.3`, Minecraft `26.3`, loader `NeoForge`, окружением Client + Server и одинаковым changelog.

```markdown
# Initial NeoForge release

Stack Unlimiter is now available for NeoForge.

## Features

- Supports stackable item counts beyond the vanilla limit of 64
- Stores exact stack counts using signed 64-bit values
- Keeps intentionally non-stackable items non-stackable
- Supports player inventories and vanilla containers
- Supports item pickup, dropping, `/give`, and inventory interactions
- Supports left-click, right-click, Shift-click, number keys, double-click, and drag distribution
- Supports crafting, bulk Shift-click crafting, and recipe-book placement
- Displays large stack counts using compact labels
- Preserves oversized stacks during saving and synchronization

## Important

- The mod must be installed on both the client and server
- Worlds containing oversized stacks depend on this mod
- Back up your worlds before installing or updating
- Compatibility with every mod is not guaranteed
```

## NeoForge 26.3 — 0.1.10

> [!success] Загрузка на Modrinth и CurseForge подтверждена
> Файл загружен на обе площадки как Release с display name `Stack Unlimiter 0.1.10 — NeoForge 26.3`, Minecraft `26.3`, loader `NeoForge`, Java 25, окружением Client + Server и без дополнительных зависимостей.

```markdown
# Inventory and container tooltip fixes

## Fixed

- Fixed `/give` silently losing intentionally non-stackable items such as tools and shulker boxes
- Fixed container and shulker-box tooltips displaying `64` instead of the exact contained stack count
- Preserved item types in zero-count copies used by vanilla inventory insertion
- Prevented inventory insertion from mutating Minecraft's shared empty-stack singleton
- Preserved signed 64-bit counts when displaying container contents

## Important

- The mod must be installed on both the client and server
- Worlds containing oversized stacks depend on this mod
- Back up your worlds before installing or updating
- Compatibility with every mod is not guaranteed
```

## Как добавлять следующий релиз

1. Добавить строку в [индекс релизов](#индекс-релизов).
2. Создать заголовок `<Loader> <Minecraft> — <mod version>`.
3. Вставить точный Markdown, переданный пользователю для публикации.
4. Указать площадки, для которых текст подготовлен.
5. Если текст восстановлен по Git, пометить его как реконструкцию.
6. Обновить ссылки на текущие changelog в [таблице модулей](REPOSITORY_MAP.md#модули).
7. Не повышать версию мода из-за одного изменения этого документа.
