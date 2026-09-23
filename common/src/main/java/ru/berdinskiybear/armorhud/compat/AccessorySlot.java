package ru.berdinskiybear.armorhud.compat;

import net.minecraft.world.item.ItemStack;

/**
 * A single accessory slot of an external accessory mod.
 *
 * @param key   a stable identifier of the slot, used to track durability changes across
 *              slot count modifications
 * @param stack the item currently in that slot
 */
public record AccessorySlot(String key, ItemStack stack) {
}
