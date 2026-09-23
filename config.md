## Configuration
Here you can find description of all parameters for **BerdinskiyBear's Armor HUD** mod. Configuration file
is a JSON file with a JSON object (curly brackets) with all config parameters as its members (name-value pairs
inside curly brackets). If any of the Configuration parameters are missing their default values will be used.

1. ##### `"enabled"`
    * Master switch for this mod.
    * Default value: `true`
1. ##### `"anchor"`
    * Place that the HUD widget is attached to.
    * Possible values:
        * `"TOP_CENTER"` - widget is placed at the top in the middle.
        * `"TOP"` - widget is placed at the upper corner on the preferred side.
        * `"BOTTOM"` - widget is placed at the bottom corner on the preferred side.
        * `"HOTBAR"` - widget is placed to the side from your hotbar.
    * Default value: `true`
1. ##### `"side"`
    * Side on which widget is shown.
      If widget is anchored at the top in the middle this setting does nothing.
    * Possible values:
        * `"LEFT"`
        * `"RIGHT"`
    * Default value: `"LEFT"`
1. ##### `"offsetX"`
    * Offsets widget position on a horizontal axis. Positive numbers moves away from anchor point,
      negative numbers move widget onto the anchor point.
    * Default value: `0`
1. ##### `"offsetY"`
    * Offsets widget position on a vertical axis, otherwise the same as X offset.
    * Default value: `0`
1. ##### `"style"`
    * Widget slot style defines how slots are drawn on the screen. If widget slots look weird
      (for example, parts of texture do not match) with your resource pack you should try different styles to find
      ones that work and choose one that you prefer.
    * Possible values:
        * `"STYLE_1_E"`
        * `"STYLE_1_H"`
        * `"STYLE_1_S"`
        * `"STYLE_2_E"`
        * `"STYLE_2_H"`
        * `"STYLE_2_S"`
        * `"STYLE_3"`
    * Default value: `"STYLE_1_E"`
1. ##### `"widgetShown"`
    * This setting defines when slots of the HUD widget are shown.
    * Possible values:
        * `"ALWAYS"`: slots are always shown.
        * `"IF_ANY_PRESENT"`: all slots are shown if at least one of the armor slots is not empty.
        * `"NOT_EMPTY"`: only not empty slots are shown
    * Default value: `"NOT_EMPTY"`
1. ##### `"offhandSlotBehavior"`
    * This setting defines the way widget reacts to offhand slot and attack indicator if it is at the hotbar.
      Setting does nothing unless widget is anchored at the hotbar.
    * Possible values:
        * `"ALWAYS_IGNORE"`: widget never moves away to make space for hotbar or attack indicator.
        * `"ADHERE"`: widget moves away when offhand slot is shown or attack indicator is at hotbar.
        * `"ALWAYS_LEAVE_SPACE"`: widget always leaves space for the offhand slot even if it is not shown.
    * Default value: `"ADHERE"`
1. ##### `"pushBossbars"`
   * If widget is at the top in the middle bossbars will be pushed down by the widget.
   * Default value: `true`
1. ##### `"pushStatusEffectIcons"`
   * If widget is at the top right corner effect icons will be pushed down by the widget.
   * Default value: `true`
1. ##### `"pushSubtitles"`
   * If widget is at the bottom right corner subtitles will be pushed up by the widget.
   * Default value: `true`
1. ##### `"reversed"`
    * Reverses order of armor items in the slots of the widget.
    * Default value: `true`
1. ##### `"iconsShown"`
    * Shows special icons in empty slots.
    * Default value: `true`
1. ##### `"warningShown"`
    * If enabled, a small warning will appear at the slot of the item with low durability.
    * Default value: `true`
1. ##### `"minDurabilityValue"`
    * If durability value of a displayed item is equal or lower than this setting, a warning can be shown.
    * Default value: `5`
1. ##### `"minDurabilityPercentage"`
    * If durability of a displayed item is equal or below this percentage, a warning can be shown.
    * Default value: `0.05`
1. ##### `"warningIconBobbingIntervalMs"`
    * This parameter defines how quickly warning icon will move up and down when shown.
    Lower the number quicker the motion.
    * Set to 0 if you want to disable bobbing.
    * Default value: `2000.0`
1. ##### `"trinketsShown"`
    * Shows items equipped in [Trinkets Updated](https://modrinth.com/mod/trinkets-updated) accessory
      slots as a second group next to the armor slots. Does nothing if Trinkets is not installed.
    * Default value: `true`
1. ##### `"trinketsSide"`
    * Side on which the trinkets group is shown, independently of `"side"`. When it differs
      from `"side"`, the trinkets become their own widget anchored to the other side of the
      screen, and `"trinketsPlacement"` and `"trinketsGap"` no longer apply. Setting does
      nothing if the widget is anchored at the top in the middle.
    * Possible values:
        * `"LEFT"`
        * `"RIGHT"`
    * Default value: `"LEFT"`
1. ##### `"trinketsFilter"`
    * Selects which equipped trinkets are shown.
    * Possible values:
        * `"ALL"`: every equipped trinket is shown, including ones without durability such as a totem.
        * `"DAMAGEABLE"`: only trinkets that have durability are shown.
        * `"DAMAGED"`: only trinkets whose durability is low enough to warrant a warning are shown.
    * Default value: `"DAMAGEABLE"`
1. ##### `"trinketsPlacement"`
    * Places the trinkets group before or after the armor group. Only applies when
      `"trinketsSide"` matches `"side"`, as the two groups then share one widget.
    * Possible values:
        * `"BEFORE"`
        * `"AFTER"`
    * Default value: `"AFTER"`
1. ##### `"trinketsGap"`
    * Space in pixels left between the armor group and the trinkets group. Only applies when
      `"trinketsSide"` matches `"side"`.
    * Default value: `4`
