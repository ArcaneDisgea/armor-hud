package ru.berdinskiybear.armorhud.mixin;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.berdinskiybear.armorhud.ArmorHudMod;
import ru.berdinskiybear.armorhud.config.ArmorHudConfig;

import java.util.List;
import java.util.Optional;

import static ru.berdinskiybear.armorhud.ArmorHudMod.*;

@Mixin(Hud.class)
public abstract class MixinHud {
    @Shadow
    @Final
    private RandomSource random;

    @Unique
    private static final Identifier WARNING_TEXTURE = Identifier.fromNamespaceAndPath(MOD_ID, "warn.png");

    @Shadow
    protected abstract void extractSlot(GuiGraphicsExtractor graphics, int x, int y, DeltaTracker tickCounter, Player player, ItemStack stack, int seed);

    @Shadow
    public abstract Font getFont();

    @Shadow
    @Final
    private static Identifier HOTBAR_SPRITE;

    @Shadow
    @Final
    private static Identifier HOTBAR_OFFHAND_LEFT_SPRITE;

    @Inject(method = "extractItemHotbar", at = @At("TAIL"))
    public void renderArmorHud(GuiGraphicsExtractor graphics, DeltaTracker tickCounter, CallbackInfo ci) {
        Profiler.get().push(MOD_ID);

        // this was extracted to a different method to be able to return whenever I want
        // without messing up the profiler
        drawArmorHud(graphics, tickCounter);

        // pop this out of profiler
        Profiler.get().pop();
    }

    @Unique
    private void drawArmorHud(GuiGraphicsExtractor graphics, DeltaTracker tickCounter) {
        ArmorHudConfig config = getManager().getConfig();
        if (!config.isEnabled()) return;

        Player player = getCameraPlayer();
        if (player == null) return;

        final Optional<Rect2i> rect = getWidgetRect(graphics, player);
        // return if there is nothing to draw
        if (rect.isEmpty()) return;

        // fetch armor and trinket items
        DisplayGroups groups = getDisplayGroups(player);
        List<ItemStack> armorItems = groups.armor();
        List<ItemStack> trinketItems = groups.trinkets();
        if (config.isReversed()) {
            armorItems = armorItems.reversed();
            trinketItems = trinketItems.reversed();
        }

        // both groups are laid out one after the other along the orientation axis
        final int gap = armorItems.isEmpty() || trinketItems.isEmpty() ? 0 : config.getTrinketsGap();
        final int armorOffset, trinketOffset;
        if (config.getTrinketsPlacement() == ArmorHudConfig.TrinketsPlacement.BEFORE) {
            trinketOffset = 0;
            armorOffset = groupLength(trinketItems.size()) + gap;
        } else {
            armorOffset = 0;
            trinketOffset = groupLength(armorItems.size()) + gap;
        }

        drawGroupBackground(graphics, config, rect.get(), armorOffset, armorItems.size());
        drawGroupBackground(graphics, config, rect.get(), trinketOffset, trinketItems.size());

        drawGroupSlots(graphics, tickCounter, player, config, rect.get(), armorOffset, armorItems, true, 1);
        drawGroupSlots(graphics, tickCounter, player, config, rect.get(), trinketOffset, trinketItems, false, armorItems.size() + 1);
    }

    /**
     * Draws the widget background of a single group, starting {@code offset} pixels along
     * the orientation axis from the widget origin.
     */
    @Unique
    private void drawGroupBackground(GuiGraphicsExtractor graphics, ArmorHudConfig config, Rect2i rect, int offset, int slotCount) {
        if (slotCount == 0) return;

        final int textureWidth = groupLength(slotCount);

        graphics.pose().pushMatrix();
        graphics.pose().translate(rect.getX(), rect.getY());

        if (config.getOrientation() == ArmorHudConfig.Orientation.VERTICAL) {
            graphics.pose().rotate(Mth.HALF_PI).translate(0, -SIZE);
        }

        graphics.pose().translate(offset, 0);

        int color = ARGB.white(ArmorHudMod.getModCompat().hudOpacity());

        switch (config.getStyle()) {
            case HOTBAR -> {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SPRITE, 182, 22, 0, 0, 0, 0, textureWidth - 3, SIZE, color);
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SPRITE, 182, 22, 182 - 3, 0, textureWidth - 3, 0, 3, SIZE, color);
            }
            case ROUNDED_CORNERS -> {
                if (slotCount > 1) {
                    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_OFFHAND_LEFT_SPRITE, 29, 24, 0, 1, 0, 0, 3, SIZE, color);
                    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SPRITE, 182, 22, 3, 0, 3, 0, textureWidth - 6, SIZE, color);
                    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_OFFHAND_LEFT_SPRITE, 29, 24, SIZE - 3, 1, textureWidth - 3, 0, 3, SIZE, color);
                } else {
                    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_OFFHAND_LEFT_SPRITE, 29, 24, 0, 1, 0, 0, SIZE, SIZE, color);
                }
            }
            case ROUNDED -> {
                if (slotCount > 1) {
                    int borderWidth = (SIZE - STEP) / 2;
                    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_OFFHAND_LEFT_SPRITE, 29, 24, 0, 1, 0, 0, SIZE - borderWidth, SIZE, color);
                    // nothing happens if slots <= 2
                    for (int i = 1; i < slotCount - 1; i++) {
                        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_OFFHAND_LEFT_SPRITE, 29, 24, borderWidth, 1, borderWidth + i * STEP, 0, STEP, SIZE, color);
                    }
                    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_OFFHAND_LEFT_SPRITE, 29, 24, 1, 1, textureWidth - STEP - borderWidth, 0, SIZE - borderWidth, SIZE, color);
                } else {
                    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_OFFHAND_LEFT_SPRITE, 29, 24, 0, 1, 0, 0, SIZE, SIZE, color);
                }
            }
            // case NONE -> // nothing to draw ^_^
        }
        graphics.pose().popMatrix();
    }

    /**
     * Draws the items of a single group, along with their durability and warning icons.
     *
     * @param armorGroup whether this group is the vanilla armor one, which is the only one
     *                   with placeholder icons for empty slots
     * @param seedBase   offset of the item render seed, so that both groups do not share
     *                   the same item animations
     */
    @Unique
    private void drawGroupSlots(GuiGraphicsExtractor graphics, DeltaTracker tickCounter, Player player, ArmorHudConfig config,
                                Rect2i rect, int offset, List<ItemStack> stacks, boolean armorGroup, int seedBase) {
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i);
            int x = rect.getX();
            int y = rect.getY();

            switch (config.getOrientation()) {
                case HORIZONTAL -> x += offset + (STEP * i);
                case VERTICAL -> y += offset + (STEP * i);
            }

            // here I blend in slot icons if so tells the current config
            if (armorGroup && config.isIconsShown() && config.getWidgetShown().shouldDrawEmptySlots() && stack.isEmpty()) {
                int slotIndex = config.isReversed() ? stacks.size() - 1 - i : i;
                Identifier identifier = InventoryMenuAccessor.getTEXTURE_EMPTY_SLOTS().get(SLOT_IDS[slotIndex]);
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, identifier, x + 3, y + 3, 16, 16);
            }

            // here I draw the armour items
            this.extractSlot(graphics, x + 3, y + 3, tickCounter, player, stack, seedBase + i);

            // when anchoring to the hotbar, we want the warning to be on the other side to avoid clipping with the hotbar
            ArmorHudConfig.Side extrasSide = config.getAnchor() == ArmorHudConfig.Anchor.HOTBAR ? config.getSide() : config.getSide().getOpposite();

            if (config.getAnchor().isTop() && config.getOrientation() == ArmorHudConfig.Orientation.HORIZONTAL) {
                y += SIZE;
            } else if (extrasSide == ArmorHudConfig.Side.RIGHT && config.getOrientation() == ArmorHudConfig.Orientation.VERTICAL) {
                x += SIZE;
            }

            if (config.getDurabilityDisplay() != ArmorHudConfig.DurabilityDisplay.BAR && !stack.isEmpty() && stack.isDamageableItem()) {
                String dura = switch (config.getDurabilityDisplay()) {
                    case NUMERIC -> String.valueOf(stack.getMaxDamage() - stack.getDamageValue());
                    case PERCENTAGE -> {
                        if (stack.getDamageValue() == 0) yield "";
                        double percentage = 1 - (double) stack.getDamageValue() / stack.getMaxDamage();
                        yield (int) Math.floor(percentage * 100) + "%";
                    }
                    case BAR -> throw new IllegalStateException("unreachable");
                };
                int textHeight = this.getFont().lineHeight;

                if (config.getOrientation() == ArmorHudConfig.Orientation.HORIZONTAL) {
                    if (!config.getAnchor().isTop()) y -= textHeight;
                    graphics.centeredText(this.getFont(), dura, x + (SIZE / 2), y, ARGB.opaque(stack.getBarColor()));
                    if (config.getAnchor().isTop()) y += textHeight;
                } else {
                    int textWidth = this.getFont().width(dura) + 2;
                    int textY = (SIZE - textHeight) / 2;

                    if (extrasSide == ArmorHudConfig.Side.LEFT) x -= textWidth;
                    graphics.text(this.getFont(), dura, x + 1, y + textY, ARGB.opaque(stack.getBarColor()));
                    if (extrasSide == ArmorHudConfig.Side.RIGHT) x += textWidth;
                }
            }

            // here I draw warning icons if necessary
            if (config.isWarningShown() && shouldShowWarning(stack)) {
                if (config.getWarningBobIntensity() != 0) {
                    int intensity = config.getWarningBobIntensity();
                    y += (int) (this.random.nextInt(intensity) - Math.ceil(intensity / 2F));
                }

                if (config.getOrientation() == ArmorHudConfig.Orientation.HORIZONTAL) {
                    if (!config.getAnchor().isTop()) y -= WARNING_SIZE + 2;

                    int warnX = (SIZE - WARNING_SIZE) / 2;
                    graphics.blit(RenderPipelines.GUI_TEXTURED, WARNING_TEXTURE, x + warnX, y + 1, 0, 0, WARNING_SIZE, WARNING_SIZE, WARNING_SIZE, WARNING_SIZE);
                } else {
                    if (extrasSide == ArmorHudConfig.Side.LEFT) x -= WARNING_SIZE + 2;

                    int warnY = (SIZE - WARNING_SIZE) / 2;
                    graphics.blit(RenderPipelines.GUI_TEXTURED, WARNING_TEXTURE, x + 1, y + warnY, 0, 0, WARNING_SIZE, WARNING_SIZE, WARNING_SIZE, WARNING_SIZE);
                }

            }
        }
    }

    @Inject(method = "extractEffects", at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;"))
    public void calculateStatusEffectIconsOffset(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci, @Share("shift") LocalIntRef shiftRef) {
        ArmorHudConfig config = getManager().getConfig();
        if (!config.isEnabled() || !config.isPushStatusEffectIcons() || config.getAnchor() != ArmorHudConfig.Anchor.TOP
                || config.getSide() != ArmorHudConfig.Side.RIGHT) return;

        Player player = getCameraPlayer();
        if (player == null) return;

        Optional<Rect2i> rect = getEffectiveWidgetRect(graphics, player);
        if (rect.isEmpty()) return;

        shiftRef.set(rect.get().getY() + rect.get().getHeight());
    }

    @ModifyVariable(method = "extractEffects", at = @At(value = "STORE"), name = "y")
    public int statusEffectIconsOffset(int y, @Share("shift") LocalIntRef shiftRef) {
        return y + shiftRef.get();
    }
}
