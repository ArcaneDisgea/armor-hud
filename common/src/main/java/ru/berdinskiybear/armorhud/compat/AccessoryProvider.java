package ru.berdinskiybear.armorhud.compat;

import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * Provides the accessory slots of an external mod (such as Trinkets) to the widget.
 * Implementations live in the platform modules, as those apis are loader specific.
 */
public interface AccessoryProvider {
    /**
     * @return every visible, non cosmetic accessory slot of the player, in display order.
     * Empty slots are included, they are filtered out later on.
     */
    List<AccessorySlot> getAccessories(Player player);

    class NoOpAccessoryProvider implements AccessoryProvider {
        @Override
        public List<AccessorySlot> getAccessories(Player player) {
            return List.of();
        }
    }
}
