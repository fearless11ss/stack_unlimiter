---
title: Stack Unlimiter Repository Map
aliases:
  - Repository Map
  - Карта репозитория
tags:
  - stack-unlimiter
  - development
  - repository-map
status: active
updated: 2026-11-08
---

# Stack Unlimiter — карта репозитория

> [!abstract] Назначение
> Эта заметка — точка входа в кодовую базу. Открой корень репозитория как Obsidian vault и начинай работу отсюда.

> [!important] Главный инвариант
> `StackCounts.get(stack)` — источник истины. `ItemStack#getCount()` хранит только совместимый с Minecraft фасад и не должен использоваться для определения полного количества oversized-стака.

## Быстрая навигация

- [[README|Обзор проекта и поддерживаемые сборки]]
- [[CONTRIBUTING|Правила участия]]
- [[RELEASE_CHANGELOGS|Каталог релизных changelog и полей публикации]]
- [[LICENSE.txt|MIT License]]
- [[.github/ISSUE_TEMPLATE/bug_report.yml|Шаблон баг-репорта]]
- [[forge-1.20.1/gradle.properties|Forge 1.20.1 — версии и метаданные]]
- [[forge-26.3/gradle.properties|Forge 26.3 — версии и метаданные]]
- [[fabric-26.3/gradle.properties|Fabric 26.3 — версии и метаданные]]
- [[neoforge-26.3/gradle.properties|NeoForge 26.3 — версии и метаданные]]

## Модули

Каждая пара Minecraft + mod loader — полностью самостоятельный Gradle-модуль. Сборку и тесты запускай из каталога нужной пары.

| Модуль | Minecraft | Loader | Java | Версия | Changelog | Статус |
| --- | --- | --- | --- | --- | --- | --- |
| [[forge-1.20.1/gradle.properties|forge-1.20.1]] | 1.20.1 | Forge | 17 | `0.1.11` | [[RELEASE_CHANGELOGS#Forge 1.20.1 — 0.1.11|0.1.11]] | активно тестируется |
| [[forge-26.3/gradle.properties|forge-26.3]] | 26.3 | Forge | 25 | `0.1.16` | [[RELEASE_CHANGELOGS#Forge 26.3 — 0.1.16|0.1.16]] | активно тестируется |
| [[fabric-26.3/gradle.properties|fabric-26.3]] | 26.3 | Fabric | 25+ для Gradle | `0.1.9` | [[RELEASE_CHANGELOGS#Fabric 26.3 — 0.1.9|0.1.9]] | исправлена полная матрица vanilla-инвентаря |
| [[neoforge-26.3/gradle.properties|neoforge-26.3]] | 26.3 | NeoForge | 25 | `0.1.9` | [[RELEASE_CHANGELOGS#NeoForge 26.3 — 0.1.9|0.1.9]] | 34 GameTest проходят; файл загружен на Modrinth |

> [!warning] Независимые реализации
> Исправление в одном модуле не переносится автоматически в остальные. Перед портированием сравни `stack_unlimiter.mixins.json`, Minecraft API и набор GameTest конкретной пары.

## Архитектура

```mermaid
flowchart LR
    Input[Команды, подбор, клики, крафт] --> Integration[Mixin-интеграции Minecraft]
    Integration --> API[StackCounts API]
    API --> Exact[Exact long count]
    Exact --> Facade[Vanilla int facade]
    Exact --> Persistence[Сохранение и загрузка]
    Exact --> Network[Синхронизация клиент ↔ сервер]
    Exact --> Render[Отображение количества]
    Facade --> Compatibility[Совместимость с vanilla и модами]
```

### Модель количества

- [[forge-1.20.1/src/main/java/dev/asp1r/stackunlimiter/api/StackCounts.java|StackCounts]] — публичные операции `get`, `set`, `grow`, `shrink`, `split`, `copyWithCount` и проверка eligibility.
- [[forge-1.20.1/src/main/java/dev/asp1r/stackunlimiter/internal/LongCountMath.java|LongCountMath]] — безопасная арифметика, защита от отрицательных значений и переполнения.
- [[forge-1.20.1/src/main/java/dev/asp1r/stackunlimiter/internal/LongCountAccess.java|LongCountAccess]] — внутренний мост между API и `ItemStackMixin`.
- [[forge-1.20.1/src/main/java/dev/asp1r/stackunlimiter/mixin/ItemStackMixin.java|ItemStackMixin]] — перехватывает основные операции `ItemStack` и поддерживает фасадный `int count`.

Правила:

1. Exact-count — неотрицательный `long`.
2. Предметы с ванильным максимумом `1` остаются нестакаемыми.
3. Фасадный `count` равен `0` для пустого стака, иначе ограничивается ванильным максимумом предмета.
4. Сравнение стака для синхронизации должно учитывать exact-count.
5. Сравнение типа и компонентов для слияния не должно считать внутренний маркер exact-count частью идентичности предмета.

### Хранение и сеть

| Ветка | Exact-count в памяти | Сохранение | Сеть |
| --- | --- | --- | --- |
| Forge 1.20.1 | `@Unique long` в `ItemStackMixin` | [[forge-1.20.1/src/main/java/dev/asp1r/stackunlimiter/internal/LongCountNbt.java|LongCountNbt]] | [[forge-1.20.1/src/main/java/dev/asp1r/stackunlimiter/mixin/FriendlyByteBufMixin.java|FriendlyByteBufMixin]] добавляет временный exact-count в NBT пакета |
| Forge 26.3 | `DataComponents.CUSTOM_DATA` | внутренний ключ в custom data | сериализация компонентов Minecraft |
| Fabric 26.3 | `DataComponents.CUSTOM_DATA` | внутренний ключ в custom data | сериализация компонентов Minecraft |
| NeoForge 26.3 | `DataComponents.CUSTOM_DATA` | внутренний ключ в custom data | сериализация компонентов Minecraft |

## Карта подсистем

| Задача | Основные файлы | Что проверять |
| --- | --- | --- |
| Базовые операции стака | `api/StackCounts`, `internal/LongCountMath`, `mixin/ItemStackMixin` | точный остаток, split/copy, переполнение, non-stackable |
| Добавление в инвентарь и подбор | `mixin/InventoryMixin` | merge в заполненный фасадный стак, исходный остаток, свободный слот |
| Клики, переносы и drag | `mixin/AbstractContainerMenuInteractionMixin` или реализация меню конкретной ветки | ЛКМ, ПКМ, Shift, number key, double-click, drag, выброс |
| Ограничение слота | `mixin/SlotMixin` | допустимый максимум и принципиально нестакаемые предметы |
| `/give` | `mixin/GiveCommandMixin` | один exact-стак вместо множества по 64 |
| Крафт в инвентаре | `mixin/InventoryMenuMixin` | ингредиенты, результат, возврат остатков, bulk Shift-click |
| Recipe book | `mixin/StackedContentsMixin`, `mixin/ServerPlaceRecipeMixin`, `mixin/AbstractCraftingMenuAccessor` | расчёт количества рецептов и распределение всех ингредиентов |
| Loader-specific storage API | Forge: `mixin/ItemStackHandlerMixin`; NeoForge 26.3: legacy API отсутствует | insert/extract/simulate без потерь там, где loader предоставляет отдельный handler API |
| Синхронизация меню | `ItemStack.matches`, сравнение remote slot, packet serialization | сервер изменился → клиент обязательно получил новый exact-count |
| Отрисовка | `client/StackCountDisplay`, `mixin/GuiGraphicsMixin`, иногда `AbstractContainerScreenMixin` | `65`, `999`, `1K`, крупные значения и ширина подписи |
| Интеграционные тесты | `gametest/StackCountGameTests` | полный игровой маршрут, не только изолированный метод |

## Forge 1.20.1

### Точки входа

- [[forge-1.20.1/src/main/java/dev/asp1r/stackunlimiter/StackUnlimiter.java|StackUnlimiter]] — Forge entry point.
- [[forge-1.20.1/src/main/resources/META-INF/mods.toml|mods.toml]] — loader, Minecraft range, версия и зависимости.
- [[forge-1.20.1/src/main/resources/stack_unlimiter.mixins.json|Mixin config]] — фактический список активных mixin.

### Особенности ветки

- Minecraft и Forge используют старую NBT-модель `ItemStack`.
- Сеть требует отдельного [[forge-1.20.1/src/main/java/dev/asp1r/stackunlimiter/mixin/FriendlyByteBufMixin.java|FriendlyByteBufMixin]].
- Клиентская синхронизация меню дополнительно проходит через Forge-сравнение `ItemStack.equals(stack, limitTags)`.
- [[forge-1.20.1/src/main/java/dev/asp1r/stackunlimiter/mixin/AbstractContainerMenuInteractionMixin.java|AbstractContainerMenuInteractionMixin]] содержит основную exact-логику кликов и remote-slot синхронизации.
- [[forge-1.20.1/src/main/java/dev/asp1r/stackunlimiter/mixin/GuiGraphicsMixin.java|GuiGraphicsMixin]] обязан явно рисовать exact-count и ниже `1000`, если фасад уже ограничен `64`.

### Тесты и сборка

```powershell
cd forge-1.20.1
.\gradlew.bat test
.\gradlew.bat runGameTestServer
.\gradlew.bat clean build
```

- Unit: [[forge-1.20.1/src/test/java/dev/asp1r/stackunlimiter/internal/LongCountMathTest.java|LongCountMathTest]], [[forge-1.20.1/src/test/java/dev/asp1r/stackunlimiter/client/StackCountDisplayTest.java|StackCountDisplayTest]].
- Integration: [[forge-1.20.1/src/main/java/dev/asp1r/stackunlimiter/gametest/StackCountGameTests.java|StackCountGameTests]].
- Результат: `forge-1.20.1/build/libs/`.

## Forge 26.3

### Точки входа

- [[forge-26.3/src/main/java/dev/asp1r/stackunlimiter/StackUnlimiter.java|StackUnlimiter]].
- [[forge-26.3/src/main/resources/META-INF/mods.toml|mods.toml]].
- [[forge-26.3/src/main/resources/stack_unlimiter.mixins.json|Mixin config]].

### Особенности ветки

- Exact-count хранится в `DataComponents.CUSTOM_DATA`.
- [[forge-26.3/src/main/java/dev/asp1r/stackunlimiter/api/StackCounts.java|StackCounts.sameItemSameComponents]] игнорирует только внутренний exact-count при сравнении custom data.
- [[forge-26.3/src/main/java/dev/asp1r/stackunlimiter/internal/BulkCraftingDispatch.java|BulkCraftingDispatch]] координирует bulk crafting.
- Forge-совместимость с capability-инвентарями находится в [[forge-26.3/src/main/java/dev/asp1r/stackunlimiter/mixin/ItemStackHandlerMixin.java|ItemStackHandlerMixin]].
- Описания GameTest лежат в `src/main/resources/data/stack_unlimiter/test_instance/`.

### Тесты и сборка

```powershell
cd forge-26.3
.\gradlew.bat test
.\gradlew.bat runGameTestServer
.\gradlew.bat clean build
```

- Unit: `src/test/java/dev/asp1r/stackunlimiter/`.
- Integration: [[forge-26.3/src/main/java/dev/asp1r/stackunlimiter/gametest/StackCountGameTests.java|StackCountGameTests]].
- Результат: `forge-26.3/build/libs/`.

## Fabric 26.3

### Точки входа

- [[fabric-26.3/src/main/java/dev/asp1r/stackunlimiter/StackUnlimiter.java|StackUnlimiter]].
- [[fabric-26.3/src/main/resources/fabric.mod.json|fabric.mod.json]].
- [[fabric-26.3/src/main/resources/stack_unlimiter.mixins.json|Mixin config]].

### Особенности ветки

- Модель exact-count близка к Forge 26.3, но loader-интеграции независимы.
- Здесь нет Forge `ItemStackHandlerMixin`.
- [[fabric-26.3/src/main/java/dev/asp1r/stackunlimiter/mixin/AbstractContainerMenuInteractionMixin.java|AbstractContainerMenuInteractionMixin]] обрабатывает exact-count для ЛКМ, ПКМ, Shift-click, выброса, double-click, number key и drag.
- [[fabric-26.3/src/main/java/dev/asp1r/stackunlimiter/mixin/InventoryMixin.java|InventoryMixin]] отдельно перехватывает выброс всего выбранного хотбар-стака через `removeFromSelected`.
- [[fabric-26.3/src/main/java/dev/asp1r/stackunlimiter/internal/BulkCraftingDispatch.java|BulkCraftingDispatch]] оставляет массовый крафт авторитетным серверу и не позволяет клиентскому предсказанию повторно обработать результат.
- [[fabric-26.3/src/main/java/dev/asp1r/stackunlimiter/mixin/AbstractContainerScreenMixin.java|AbstractContainerScreenMixin]] использует exact-count при предпросмотре drag-распределения.
- Для текста количества нужен непрозрачный ARGB `0xFFFFFFFF`: `0xFFFFFF` имеет нулевой alpha и делает подпись невидимой в Minecraft 26.3.
- Fabric API используется только source set `src/gametest` для автоматических тестов. Production-код зависит от Fabric Loader и Mixin, поэтому Fabric API не является обязательной зависимостью для игрока и не добавляется в metadata релиза.
- `net.fabricmc.api.ModInitializer` входит в Fabric Loader, несмотря на имя пакета `fabricmc.api`.
- GameTest вынесены в отдельный source set `src/gametest`.
- Gradle-процесс должен запускаться на Java 25 или новее, а не только использовать toolchain.

### Тесты и сборка

```powershell
cd fabric-26.3
.\gradlew.bat test
.\gradlew.bat runGameTest
.\gradlew.bat clean build
```

- Unit: `src/test/java/dev/asp1r/stackunlimiter/`.
- Integration: [[fabric-26.3/src/gametest/java/dev/asp1r/stackunlimiter/gametest/StackCountGameTests.java|StackCountGameTests]].
- Подтверждённое состояние `0.1.9`: `test`, `clean build` и все 34 теста, обнаруженные `runGameTest`, проходят.
- Результат: `fabric-26.3/build/libs/`.

## NeoForge 26.3

### Точки входа

- [[neoforge-26.3/src/main/java/dev/asp1r/stackunlimiter/StackUnlimiter.java|StackUnlimiter]] — NeoForge entry point и регистрация GameTest-функций через mod event bus.
- [[neoforge-26.3/src/main/templates/META-INF/neoforge.mods.toml|neoforge.mods.toml]] — шаблон metadata; итоговый файл генерируется Gradle.
- [[neoforge-26.3/src/main/resources/stack_unlimiter.mixins.json|Mixin config]].

### Особенности ветки

- Exact-count хранится в `DataComponents.CUSTOM_DATA`; модель и полный набор vanilla-инвентарных операций перенесены из исправленной реализации 26.3.
- [[neoforge-26.3/src/main/java/dev/asp1r/stackunlimiter/mixin/AbstractContainerMenuInteractionMixin.java|AbstractContainerMenuInteractionMixin]] обрабатывает ЛКМ, ПКМ, Shift-click, number key, double-click, drag и выброс без обрезания до `64`.
- [[neoforge-26.3/src/main/java/dev/asp1r/stackunlimiter/mixin/AbstractContainerScreenMixin.java|AbstractContainerScreenMixin]] использует exact-count в клиентском предпросмотре drag-распределения.
- [[neoforge-26.3/src/main/java/dev/asp1r/stackunlimiter/internal/BulkCraftingDispatch.java|BulkCraftingDispatch]] координирует серверный массовый крафт.
- [[neoforge-26.3/src/main/java/dev/asp1r/stackunlimiter/mixin/InventoryMenuMixin.java|InventoryMenuMixin]] устанавливает crafting player через `CommonHooks` и отправляет NeoForge crafting event через `EventHooks`, сохраняя совместимость bulk crafting с событиями loader.
- В NeoForge 26.3 нет legacy Forge `ItemStackHandler`/`IItemHandler`, поэтому Forge-специфичный `ItemStackHandlerMixin` не переносится. Vanilla-инвентари и меню покрыты полностью.
- [[neoforge-26.3/src/main/java/dev/asp1r/stackunlimiter/gametest/GameTestRegistry.java|GameTestRegistry]] регистрирует 33 функции через `DeferredRegister` в `BuiltInRegistries.TEST_FUNCTION`.
- Data-driven описания находятся в `src/main/resources/data/stack_unlimiter/test_instance/`, а общая пустая структура — в [[neoforge-26.3/src/main/resources/data/stack_unlimiter/structure/empty.nbt|empty.nbt]].

### Тесты и сборка

Gradle запускается на Java 21, а Java 25 используется как toolchain компиляции. Это обходит несовместимость asset-задач NeoForge с запуском Gradle непосредственно на Java 25.

```powershell
cd neoforge-26.3
$env:JAVA_HOME='C:\Program Files\Microsoft\jdk-21.0.13.21-hotspot'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test '-Dorg.gradle.java.installations.paths=C:\Program Files\Microsoft\jdk-21.0.13.21-hotspot,C:\Users\asp1r\.gradle\jdks\eclipse_adoptium-25-amd64-windows.2'
.\gradlew.bat runGameTestServer '-Dorg.gradle.java.installations.paths=C:\Program Files\Microsoft\jdk-21.0.13.21-hotspot,C:\Users\asp1r\.gradle\jdks\eclipse_adoptium-25-amd64-windows.2'
.\gradlew.bat clean build '-Dorg.gradle.java.installations.paths=C:\Program Files\Microsoft\jdk-21.0.13.21-hotspot,C:\Users\asp1r\.gradle\jdks\eclipse_adoptium-25-amd64-windows.2'
```

- Unit: `src/test/java/dev/asp1r/stackunlimiter/`.
- Integration: [[neoforge-26.3/src/main/java/dev/asp1r/stackunlimiter/gametest/StackCountGameTests.java|StackCountGameTests]].
- Подтверждённое состояние `0.1.9`: `test`, `clean build` и все 34 теста, обнаруженные `runGameTestServer`, проходят.
- Результат: `neoforge-26.3/build/libs/`.

### Публикация

- Modrinth: `0.1.9` загружен как Release с display name `Stack Unlimiter 0.1.9 — NeoForge 26.3` и changelog из [[RELEASE_CHANGELOGS#NeoForge 26.3 — 0.1.9]].
- CurseForge: используются те же display name, metadata и changelog; загрузка подготовлена.

## Диагностические маршруты

### Предмет подбирается, но цифра не меняется

1. Проверить exact-count серверного слота после `ItemEntity.playerTouch`.
2. Проверить, удалена ли `ItemEntity`.
3. Проверить, заметил ли `AbstractContainerMenu.broadcastChanges()` отличие remote slot.
4. Проверить packet round-trip одного слота.
5. Проверить, что клиент применил exact-count.
6. Проверить отрисовку: значения `65–999` не должны уходить в ванильный `getCount()`.

Начальные файлы: [[forge-1.20.1/src/main/java/dev/asp1r/stackunlimiter/mixin/InventoryMixin.java|InventoryMixin]], [[forge-1.20.1/src/main/java/dev/asp1r/stackunlimiter/mixin/AbstractContainerMenuInteractionMixin.java|AbstractContainerMenuInteractionMixin]], [[forge-1.20.1/src/main/java/dev/asp1r/stackunlimiter/mixin/FriendlyByteBufMixin.java|FriendlyByteBufMixin]], [[forge-1.20.1/src/main/java/dev/asp1r/stackunlimiter/mixin/GuiGraphicsMixin.java|GuiGraphicsMixin]].

### Переносится только 64 предмета

1. Найти первый вызов, где exact-count заменяется `getCount()`.
2. Разделить источник, переносимый стак и остаток; проверить каждый exact-count.
3. Проверить оба направления и пустой/совместимый целевой слот.
4. Для Forge отдельно проверить `ItemStackHandler` и режим simulation; в NeoForge 26.3 legacy handler API отсутствует.

### Дюп или потеря предметов

Считать сумму exact-count до и после операции во всех участвующих местах: исходный слот, целевой слот, carried stack, crafting grid, item entity. Проверять не только итоговый слот, но и остаток источника.

### Ломается крафт или recipe book

Проверять по цепочке:

```text
StackedContents → ServerPlaceRecipe → crafting grid → result slot → bulk transfer → remaining items
```

### Неверная цифра или переполнение ячейки

Начинать с `StackCountDisplay` и `GuiGraphicsMixin`. Отдельные пороги: `64 → 65`, `999 → 1000`, суффиксы `K/M/B/T/Q/E`, максимальная ширина и `Long.MAX_VALUE`.

Если подпись полностью отсутствует при правильном exact-count:

1. Проверить, что `StackCountDisplay.requiresExactLabel(exactCount, vanillaCount)` возвращает `true`.
2. Проверить загрузку `GuiGraphicsMixin` в секции `client` файла `stack_unlimiter.mixins.json`.
3. Проверить alpha-канал цвета. Для белого текста использовать `0xFFFFFFFF`, а не прозрачный `0x00FFFFFF`.
4. Проверить синхронизацию exact-count на клиенте до начала отрисовки.

## Минимальная матрица регрессии

- exact-count: `65`, `999`, `1_000`, `100_000`, `1_000_000`, значение выше `Integer.MAX_VALUE`;
- ЛКМ/ПКМ по слоту и carried stack;
- Shift-click в обе стороны между игроком и контейнером;
- number key, double-click, primary/secondary drag;
- выброс одного предмета и всего стака;
- подбор одного предмета в стак `64` и подбор oversized item entity;
- `/give`;
- recipe book, обычный крафт и Shift-click результата;
- сохранение/загрузка мира;
- packet round-trip и реальная remote-slot синхронизация;
- loader-specific storage handler: для Forge — insert, extract, simulate; для NeoForge 26.3 legacy Forge handler не применяется;
- предмет с ванильным максимумом `1`.

## Рабочий цикл изменения

> [!todo] Definition of done
> 1. Ограничить работу одной запрошенной парой версия + loader.
> 2. Добавить регрессионный тест, который сначала воспроизводит ошибку.
> 3. Исправить причину, а не только визуальный симптом.
> 4. Запустить unit-тесты и GameTest нужного модуля.
> 5. Выполнить `clean build`.
> 6. Если менялся код или поведение мода — повысить версию модуля и обновить таблицу в [[README]]. Документация и публикационные файлы версию не повышают.
> 7. Проверить версию и Minecraft range внутри JAR.
> 8. Обновить эту карту, если изменились структура, версия, тестовая матрица, сборка, диагностика или важное техническое решение.
> 9. Не добавлять `artwork/` и `.obsidian/` в коммит без отдельного запроса.
> 10. Сделать содержательный commit и push.

### Рабочий цикл публикации

1. Сверить версию JAR и пару Minecraft + loader с таблицей модулей.
2. Взять точный текст из [[RELEASE_CHANGELOGS]].
3. После подготовки нового текста сразу записать его в каталог с указанием площадки.
4. Не добавлять конкретную версию Minecraft в общее описание проекта.
5. Не повышать версию мода при изменении только публикационных документов.

## Когда обновлять эту карту

- добавлена новая пара Minecraft + loader;
- появился новый mixin или отдельный адаптер мода;
- изменился способ хранения или сетевой передачи exact-count;
- изменились команды сборки или тестов;
- диагностический маршрут оказался неполным.
- изменились версия модуля, покрытие операций или подтверждённое состояние тестов;
- обнаружена полезная причина бага или ограничение конкретной версии Minecraft/loader.
- подготовлен или изменён changelog релиза — обновить [[RELEASE_CHANGELOGS]] и ссылку из таблицы модулей;

Версии в таблице модулей — снимок состояния. Источник истины всегда находится в соответствующем `gradle.properties` и в [[README#Available builds|таблице сборок README]].
