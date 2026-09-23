package ru.berdinskiybear.armorhud;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.uku3lig.ukulib.config.ConfigManager;
import net.uku3lig.ukulib.utils.Ukutils;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import ru.berdinskiybear.armorhud.compat.AccessoryProvider;
import ru.berdinskiybear.armorhud.compat.AccessorySlot;
import ru.berdinskiybear.armorhud.compat.ModCompat;
import ru.berdinskiybear.armorhud.config.ArmorHudConfig;
import ru.berdinskiybear.armorhud.mixin.InventoryMenuAccessor;

import java.util.*;
import java.util.stream.Stream;

@Slf4j
public final class ArmorHudMod {
    public static final String MOD_ID = "ukus-armor-hud";

    @Getter
    private static final ConfigManager<ArmorHudConfig> manager = ConfigManager.createDefault(ArmorHudConfig.class, MOD_ID);

    public static final int STEP = 20;
    public static final int SIZE = 22;
    public static final int HOTBAR_OFFSET = 98;
    public static final int OFFHAND_OFFSET = 29;
    public static final int ATTACK_INDICATOR_OFFSET = 23;
    public static final int WARNING_SIZE = 8;

    public static final SoundEvent ARMOR_BREAKING_SOUND = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(MOD_ID, "armor_breaking"));

    public static final EquipmentSlot[] SLOT_IDS = InventoryMenuAccessor.getSLOT_IDS();

    private static final List<ItemStack> lastStacks = new ArrayList<>(Collections.nCopies(SLOT_IDS.length, ItemStack.EMPTY));

    private static final Map<String, ItemStack> lastTrinketStacks = new HashMap<>();

    @Getter @Setter
    private static ModCompat modCompat = new ModCompat.NoOpModCompat();

    @Getter @Setter
    private static AccessoryProvider accessoryProvider = new AccessoryProvider.NoOpAccessoryProvider();

    /**
     * The items to be drawn, split into the armor group and the trinkets group.
     */
    public record DisplayGroups(List<ItemStack> armor, List<ItemStack> trinkets) {
        public boolean isEmpty() {
            return armor.isEmpty() && trinkets.isEmpty();
        }
    }

    /**
     * @return the length in pixels taken up by a group of {@code slots} slots, along the
     * axis of the current orientation
     */
    public static int groupLength(int slots) {
        return slots == 0 ? 0 : SIZE + ((slots - 1) * STEP);
    }

    @Nullable
    public static Player getCameraPlayer() {
        return Minecraft.getInstance().getCameraEntity() instanceof Player player ? player : null;
    }

    /**
     * A single widget to be drawn, with the items of both groups it contains. There are two
     * of these when the trinkets are anchored to the other side of the screen.
     */
    public record Widget(Rect2i rect, List<ItemStack> armor, List<ItemStack> trinkets) {
    }

    /**
     * @return whether both groups share a single widget, which is the case when they are on
     * the same side, or when the side setting does nothing at all
     */
    public static boolean isSingleWidget(ArmorHudConfig config) {
        return config.getAnchor() == ArmorHudConfig.Anchor.TOP_CENTER
                || config.getTrinketsSide() == config.getSide();
    }

    /**
     * @return whether any widget is anchored to the given side of the screen
     */
    public static boolean isOnSide(ArmorHudConfig config, ArmorHudConfig.Side side) {
        return config.getSide() == side
                || (!isSingleWidget(config) && config.getTrinketsSide() == side);
    }

    /**
     * Returns every widget to be drawn, in no particular order.
     */
    public static List<Widget> getWidgets(GuiGraphicsExtractor graphics, Player player) {
        ArmorHudConfig config = manager.getConfig();
        DisplayGroups groups = getDisplayGroups(player);

        if (groups.isEmpty()) {
            return List.of();
        }

        if (isSingleWidget(config)) {
            return getRect(graphics, player, getTotalLength(groups, config), config.getSide())
                    .map(rect -> List.of(new Widget(rect, groups.armor(), groups.trinkets())))
                    .orElseGet(List::of);
        }

        List<Widget> widgets = new ArrayList<>(2);
        getRect(graphics, player, groupLength(groups.armor().size()), config.getSide())
                .ifPresent(rect -> widgets.add(new Widget(rect, groups.armor(), List.of())));
        getRect(graphics, player, groupLength(groups.trinkets().size()), config.getTrinketsSide())
                .ifPresent(rect -> widgets.add(new Widget(rect, List.of(), groups.trinkets())));
        return widgets;
    }

    /**
     * Returns the bounding box of every widget merged together,
     * <strong>excluding</strong> "external" information like warning icon
     */
    public static Optional<Rect2i> getWidgetRect(GuiGraphicsExtractor graphics, Player player) {
        return getWidgets(graphics, player).stream().map(Widget::rect).reduce(ArmorHudMod::union);
    }

    private static Rect2i union(Rect2i a, Rect2i b) {
        final int x = Math.min(a.getX(), b.getX());
        final int y = Math.min(a.getY(), b.getY());
        final int endX = Math.max(a.getX() + a.getWidth(), b.getX() + b.getWidth());
        final int endY = Math.max(a.getY() + a.getHeight(), b.getY() + b.getHeight());

        return new Rect2i(x, y, endX - x, endY - y);
    }

    /**
     * Returns the bounding box of a single widget of the given length, anchored to the given side
     */
    private static Optional<Rect2i> getRect(GuiGraphicsExtractor graphics, Player player, int length, ArmorHudConfig.Side side) {
        ArmorHudConfig config = manager.getConfig();

        if (length == 0) {
            return Optional.empty();
        }

        // hotbar offset is relative to the bar, so when we are on the left it needs to be flipped
        // and on the right side, we need to flip the offset, except when anchored to the hotbar
        final int sideMultiplier, sideOffsetMultiplier;
        if ((config.getAnchor() == ArmorHudConfig.Anchor.HOTBAR && side == ArmorHudConfig.Side.LEFT)
                || (config.getAnchor() != ArmorHudConfig.Anchor.HOTBAR && side == ArmorHudConfig.Side.RIGHT)) {
            sideMultiplier = -1;
            sideOffsetMultiplier = -1;
        } else {
            sideMultiplier = 1;
            sideOffsetMultiplier = 0;
        }

        final int addedHotbarOffset = switch (config.getOffhandSlotBehavior()) {
            case ALWAYS_IGNORE -> 0;
            case ALWAYS_LEAVE_SPACE ->
                    player.getMainArm() == side.asArm() ? ATTACK_INDICATOR_OFFSET : OFFHAND_OFFSET;
            case ADHERE -> {
                if (player.getMainArm() == side.asArm()) {
                    if (Minecraft.getInstance().options.attackIndicator().get() == AttackIndicatorStatus.HOTBAR &&
                            player.getAttackStrengthScale(0) < 1) {
                        yield ATTACK_INDICATOR_OFFSET;
                    }
                } else if (!player.getOffhandItem().isEmpty()) {
                    yield OFFHAND_OFFSET;
                }

                yield 0;
            }
        };

        final int widgetWidth = config.getOrientation() == ArmorHudConfig.Orientation.VERTICAL ? SIZE : length;
        final int widgetHeight = config.getOrientation() == ArmorHudConfig.Orientation.VERTICAL ? length : SIZE;

        final int armorWidgetX = config.getOffsetX() * sideMultiplier + switch (config.getAnchor()) {
            case TOP_CENTER -> (graphics.guiWidth() - widgetWidth) / 2;
            case TOP, BOTTOM -> (widgetWidth - graphics.guiWidth()) * sideOffsetMultiplier;
            case HOTBAR ->
                    graphics.guiWidth() / 2 + ((HOTBAR_OFFSET + addedHotbarOffset) * sideMultiplier) + (widgetWidth * sideOffsetMultiplier);
        };

        final int armorWidgetY = switch (config.getAnchor()) {
            case BOTTOM, HOTBAR ->
                    graphics.guiHeight() - widgetHeight - config.getOffsetY() - modCompat.screenSafeArea();
            case TOP, TOP_CENTER -> config.getOffsetY();
        };

        return Optional.of(new Rect2i(armorWidgetX, armorWidgetY, widgetWidth, widgetHeight));
    }

    /**
     * Returns the effective bounding box, <strong>including</strong> "external" information like warning icon
     */
    public static Optional<Rect2i> getEffectiveWidgetRect(GuiGraphicsExtractor graphics, Player player) {
        ArmorHudConfig config = manager.getConfig();

        return getWidgetRect(graphics, player).map(rect -> {
            // TODO should probably extend the bbox horizontally too
            if (config.getOrientation() == ArmorHudConfig.Orientation.HORIZONTAL) {
                int additionalHeight = 0;

                if (config.isWarningShown()) {
                    additionalHeight += WARNING_SIZE + 2 + (config.getWarningBobIntensity() / 2);
                }

                if (config.getDurabilityDisplay() != ArmorHudConfig.DurabilityDisplay.BAR) {
                    additionalHeight += Minecraft.getInstance().font.lineHeight;
                }

                rect.setHeight(rect.getHeight() + additionalHeight);
                if (!config.getAnchor().isTop()) {
                    rect.setY(rect.getY() - additionalHeight);
                }
            }

            return rect;
        });
    }

    public static List<ItemStack> getArmorItems(Player player) {
        Stream<ItemStack> items = Arrays.stream(SLOT_IDS).map(player::getItemBySlot);
        items = switch (manager.getConfig().getWidgetShown()) {
            case ALWAYS -> items;
            case IF_ANY_PRESENT -> {
                List<ItemStack> itemList = items.toList();
                yield itemList.stream().allMatch(ItemStack::isEmpty) ? Stream.of() : itemList.stream();
            }
            case NOT_EMPTY -> items.filter(s -> !s.isEmpty());
            case DAMAGED_PIECES -> items.filter(ArmorHudMod::shouldShowWarning);
        };

        return items.toList();
    }

    /**
     * @return the total length in pixels of both groups, gap included
     */
    public static int getTotalLength(DisplayGroups groups, ArmorHudConfig config) {
        int length = groupLength(groups.armor().size()) + groupLength(groups.trinkets().size());
        if (!groups.armor().isEmpty() && !groups.trinkets().isEmpty()) {
            length += config.getTrinketsGap();
        }
        return length;
    }

    public static DisplayGroups getDisplayGroups(Player player) {
        return new DisplayGroups(getArmorItems(player), getTrinketItems(player));
    }

    public static List<ItemStack> getTrinketItems(Player player) {
        return getTrinketSlots(player).stream().map(AccessorySlot::stack).toList();
    }

    private static List<AccessorySlot> getTrinketSlots(Player player) {
        ArmorHudConfig config = manager.getConfig();
        if (!config.isTrinketsShown()) {
            return List.of();
        }

        // empty trinket slots are never drawn: there is no placeholder sprite for them and
        // their amount varies, which would make the widget jump around
        return accessoryProvider.getAccessories(player).stream()
                .filter(slot -> !slot.stack().isEmpty())
                .filter(slot -> switch (config.getTrinketsFilter()) {
                    case ALL -> true;
                    case DAMAGEABLE -> slot.stack().isDamageableItem();
                    case DAMAGED -> shouldShowWarning(slot.stack());
                })
                .toList();
    }

    public static boolean shouldPlayBreakSound(Player player) {
        // every slot is always visited, so that the tracked stacks never go stale
        boolean breaking = false;

        for (int i = 0; i < SLOT_IDS.length; i++) {
            EquipmentSlot slot = SLOT_IDS[i];
            ItemStack current = player.getItemBySlot(slot);
            ItemStack last = lastStacks.set(i, current);
            if (last.getDamageValue() != current.getDamageValue() && shouldShowWarning(current)) {
                breaking = true;
            }
        }

        Set<String> seen = new HashSet<>();
        for (AccessorySlot slot : getTrinketSlots(player)) {
            seen.add(slot.key());
            ItemStack current = slot.stack();
            ItemStack last = lastTrinketStacks.put(slot.key(), current);
            if (last != null && last.getDamageValue() != current.getDamageValue() && shouldShowWarning(current)) {
                breaking = true;
            }
        }
        lastTrinketStacks.keySet().retainAll(seen);

        return breaking;
    }

    public static boolean shouldShowWarning(ItemStack stack) {
        if (stack.isEmpty() || !stack.isDamageableItem()) return false;

        final int damage = stack.getDamageValue();
        final int maxDamage = stack.getMaxDamage();
        double percentage = 1.0 - ((double) damage / maxDamage);

        return percentage <= manager.getConfig().getMinDurabilityPercentage()
                || maxDamage - damage <= manager.getConfig().getMinDurabilityValue();
    }

    public static void onInitialize() {
        Ukutils.registerToggleBind(new KeyMapping("armorhud.keybind.toggle", GLFW.GLFW_KEY_UNKNOWN, KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "key"))),
                () -> manager.getConfig().isEnabled(), b -> manager.getConfig().setEnabled(b), Component.translatable("armorhud.keybind.toggle.msg"));
    }
}
