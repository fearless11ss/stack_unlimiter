package dev.asp1r.stackunlimiter.gametest;

import dev.asp1r.stackunlimiter.StackUnlimiter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Consumer;

public final class GameTestRegistry {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(
            BuiltInRegistries.TEST_FUNCTION,
            StackUnlimiter.MOD_ID
    );

    static {
        TEST_FUNCTIONS.register("exact_count_survives_persistence_and_network", () -> StackCountGameTests::exactCountSurvivesPersistenceAndNetwork);
        TEST_FUNCTIONS.register("intentionally_non_stackable_items_remain_non_stackable", () -> StackCountGameTests::intentionallyNonStackableItemsRemainNonStackable);
        TEST_FUNCTIONS.register("inventory_merges_oversized_stacks", () -> StackCountGameTests::inventoryMergesOversizedStacks);
        TEST_FUNCTIONS.register("give_command_uses_one_eligible_stack", () -> StackCountGameTests::giveCommandUsesOneEligibleStack);
        TEST_FUNCTIONS.register("give_command_preserves_non_stackable_items", () -> StackCountGameTests::giveCommandPreservesNonStackableItems);
        TEST_FUNCTIONS.register("container_tooltip_uses_exact_contained_count", () -> StackCountGameTests::containerTooltipUsesExactContainedCount);
        TEST_FUNCTIONS.register("drop_selected_item_removes_entire_oversized_stack", () -> StackCountGameTests::dropSelectedItemRemovesEntireOversizedStack);
        TEST_FUNCTIONS.register("inventory_throw_drops_one_exact_stack", () -> StackCountGameTests::inventoryThrowDropsOneExactStack);
        TEST_FUNCTIONS.register("inventory_shift_click_preserves_oversized_stack", () -> StackCountGameTests::inventoryShiftClickPreservesOversizedStack);
        TEST_FUNCTIONS.register("inventory_right_click_splits_exactly_in_half", () -> StackCountGameTests::inventoryRightClickSplitsExactlyInHalf);
        TEST_FUNCTIONS.register("inventory_left_click_picks_up_entire_exact_stack", () -> StackCountGameTests::inventoryLeftClickPicksUpEntireExactStack);
        TEST_FUNCTIONS.register("inventory_left_click_places_entire_exact_carried_stack", () -> StackCountGameTests::inventoryLeftClickPlacesEntireExactCarriedStack);
        TEST_FUNCTIONS.register("inventory_right_click_places_one_from_exact_carried_stack", () -> StackCountGameTests::inventoryRightClickPlacesOneFromExactCarriedStack);
        TEST_FUNCTIONS.register("inventory_left_click_merges_entire_exact_carried_stack", () -> StackCountGameTests::inventoryLeftClickMergesEntireExactCarriedStack);
        TEST_FUNCTIONS.register("inventory_right_click_merges_one_from_exact_carried_stack", () -> StackCountGameTests::inventoryRightClickMergesOneFromExactCarriedStack);
        TEST_FUNCTIONS.register("inventory_left_click_swaps_exact_stacks", () -> StackCountGameTests::inventoryLeftClickSwapsExactStacks);
        TEST_FUNCTIONS.register("inventory_outside_clicks_drop_exact_carried_amounts", () -> StackCountGameTests::inventoryOutsideClicksDropExactCarriedAmounts);
        TEST_FUNCTIONS.register("inventory_number_key_swaps_exact_stacks", () -> StackCountGameTests::inventoryNumberKeySwapsExactStacks);
        TEST_FUNCTIONS.register("inventory_pickup_all_collects_all_exact_stacks", () -> StackCountGameTests::inventoryPickupAllCollectsAllExactStacks);
        TEST_FUNCTIONS.register("inventory_pickup_all_from_single_item_collects_all_exact_stacks", () -> StackCountGameTests::inventoryPickupAllFromSingleItemCollectsAllExactStacks);
        TEST_FUNCTIONS.register("inventory_primary_drag_evenly_distributes_exact_stack", () -> StackCountGameTests::inventoryPrimaryDragEvenlyDistributesExactStack);
        TEST_FUNCTIONS.register("inventory_drag_preview_uses_exact_stack_count", () -> StackCountGameTests::inventoryDragPreviewUsesExactStackCount);
        TEST_FUNCTIONS.register("inventory_secondary_drag_places_one_per_slot", () -> StackCountGameTests::inventorySecondaryDragPlacesOnePerSlot);
        TEST_FUNCTIONS.register("inventory_throw_one_preserves_exact_remainder", () -> StackCountGameTests::inventoryThrowOnePreservesExactRemainder);
        TEST_FUNCTIONS.register("inventory_menu_close_returns_exact_carried_stack", () -> StackCountGameTests::inventoryMenuCloseReturnsExactCarriedStack);
        TEST_FUNCTIONS.register("inventory_clone_uses_the_unlimited_maximum", () -> StackCountGameTests::inventoryCloneUsesTheUnlimitedMaximum);
        TEST_FUNCTIONS.register("chest_shift_click_moves_exact_stack_to_player", () -> StackCountGameTests::chestShiftClickMovesExactStackToPlayer);
        TEST_FUNCTIONS.register("chest_shift_click_moves_exact_stack_from_player", () -> StackCountGameTests::chestShiftClickMovesExactStackFromPlayer);
        TEST_FUNCTIONS.register("chest_shift_click_merges_into_existing_exact_stack", () -> StackCountGameTests::chestShiftClickMergesIntoExistingExactStack);
        TEST_FUNCTIONS.register("oversized_item_entity_pickup_preserves_exact_count", () -> StackCountGameTests::oversizedItemEntityPickupPreservesExactCount);
        TEST_FUNCTIONS.register("item_entity_pickup_grows_existing_full_stack", () -> StackCountGameTests::itemEntityPickupGrowsExistingFullStack);
        TEST_FUNCTIONS.register("container_click_packet_bulk_crafts_all_results", () -> StackCountGameTests::containerClickPacketBulkCraftsAllResults);
        TEST_FUNCTIONS.register("recipe_accounting_uses_the_exact_count", () -> StackCountGameTests::recipeAccountingUsesTheExactCount);
        TEST_FUNCTIONS.register("shift_recipe_placement_uses_all_ingredients", () -> StackCountGameTests::shiftRecipePlacementUsesAllIngredients);
        TEST_FUNCTIONS.register("shift_crafting_collects_all_results", () -> StackCountGameTests::shiftCraftingCollectsAllResults);
    }

    private GameTestRegistry() {
    }

    public static void register(IEventBus modBus) {
        TEST_FUNCTIONS.register(modBus);
    }
}
