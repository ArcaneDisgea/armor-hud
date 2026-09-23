package ru.berdinskiybear.armorhud.fabric;

import eu.pb4.trinkets.api.SlotGroup;
import eu.pb4.trinkets.api.SlotType;
import eu.pb4.trinkets.api.TrinketAttachment;
import eu.pb4.trinkets.api.TrinketInventory;
import eu.pb4.trinkets.api.TrinketsApi;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.berdinskiybear.armorhud.compat.AccessoryProvider;
import ru.berdinskiybear.armorhud.compat.AccessorySlot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Reads the accessory slots of Trinkets Updated ({@code eu.pb4:trinkets}).
 */
public class TrinketsCompat implements AccessoryProvider {
    private static final Logger log = LoggerFactory.getLogger(TrinketsCompat.class);

    private static final Comparator<SlotGroup> GROUP_ORDER = Comparator.comparingInt(SlotGroup::order).thenComparing(SlotGroup::name);
    private static final Comparator<SlotType> SLOT_ORDER = Comparator.comparingInt(SlotType::order).thenComparing(SlotType::getId);

    @Override
    public List<AccessorySlot> getAccessories(Player player) {
        try {
            TrinketAttachment attachment = TrinketsApi.getAttachment(player);
            List<AccessorySlot> slots = new ArrayList<>();

            attachment.getGroups().values().stream().sorted(GROUP_ORDER)
                    .flatMap(group -> group.getSlots().stream().sorted(SLOT_ORDER))
                    .forEach(slotType -> {
                        TrinketInventory inventory = attachment.getInventory(slotType.getId());
                        if (inventory == null || slotType.isHidden()) return;

                        for (int i = 0; i < inventory.getContainerSize(); i++) {
                            if (!inventory.isValidSlot(i) || !inventory.isVisible(i)) continue;
                            // cosmetic items are purely visual, so their durability is irrelevant
                            slots.add(new AccessorySlot(slotType.getId() + "@" + i, inventory.getItem(i)));
                        }
                    });

            return slots;
        } catch (Exception e) {
            // slots are datapack driven, so they may be missing or out of sync on the client
            log.debug("Could not read trinket slots", e);
            return List.of();
        }
    }
}
