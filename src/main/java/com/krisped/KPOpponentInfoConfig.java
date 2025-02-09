package com.krisped;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("kp_opponentinfo")
public interface KPOpponentInfoConfig extends Config
{
    @ConfigItem(
            keyName = "lookupOnInteraction",
            name = "Lookup players on interaction",
            description = "Display a combat stat comparison panel on player interaction (Attack, Trade, etc.).",
            position = 0
    )
    default boolean lookupOnInteraction() {
        return false;
    }

    @ConfigItem(
            keyName = "hitpointsDisplayStyle",
            name = "Display style",
            description = "Show opponent's hitpoints as a value, percentage, or both.",
            position = 1
    )
    default HitpointsDisplayStyle hitpointsDisplayStyle() {
        return HitpointsDisplayStyle.HITPOINTS;
    }

    @ConfigItem(
            keyName = "showOpponentsInMenu",
            name = "Show opponents in menu",
            description = "Marks opponents' names in the menu (NPC only).",
            position = 2
    )
    default boolean showOpponentsInMenu() {
        return false;
    }

    @ConfigItem(
            keyName = "showOpponentHealthOverlay",
            name = "Show opponent health overlay",
            description = "Shows a health bar overlay when a boss overlay is not present.",
            position = 3
    )
    default boolean showOpponentHealthOverlay() {
        return true;
    }

    @ConfigItem(
            keyName = "overlayDisplayDuration",
            name = "Overlay Display Duration (seconds)",
            description = "The number of seconds before the overlay automatically disappears.",
            position = 4
    )
    default int overlayDisplayDuration() {
        return 5;
    }

    // Extended Features Section
    @ConfigSection(
            name = "Extended Features",
            description = "Options for risk display and target combat details.",
            position = 5,
            closedByDefault = true
    )
    String extendedFeatures = "extendedFeatures";

    @ConfigItem(
            keyName = "riskDisplayOption",
            name = "Risk Display Option",
            description = "Select where to display player's potential risk: None, Chat, Overlay, or Both.",
            position = 0,
            section = extendedFeatures
    )
    default RiskDisplayOption riskDisplayOption() {
        return RiskDisplayOption.NONE;
    }

    @ConfigItem(
            keyName = "targetCombatDisplay",
            name = "Target Combat Display",
            description = "Select how to display target's combat details: None, Attack Style, Weapon, or Both.",
            position = 1,
            section = extendedFeatures
    )
    default TargetCombatDisplay targetCombatDisplay() {
        return TargetCombatDisplay.NONE;
    }

    enum RiskDisplayOption {
        NONE("None"),
        CHAT("Chat"),
        OVERLAY("Overlay"),
        BOTH("Both");

        private final String display;
        RiskDisplayOption(String display) {
            this.display = display;
        }
        public String getDisplay() {
            return display;
        }
        @Override
        public String toString() {
            return display;
        }
    }

    enum TargetCombatDisplay {
        NONE("None"),
        ATTACK_STYLE("Attack Style"),
        WEAPON("Weapon"),
        BOTH("Both");

        private final String display;
        TargetCombatDisplay(String display) {
            this.display = display;
        }
        public String getDisplay() {
            return display;
        }
        @Override
        public String toString() {
            return display;
        }
    }

    // Dynamic Bar Settings Section
    @ConfigSection(
            name = "Dynamic Bar Settings",
            description = "Settings for dynamic bar color and blinking.",
            position = 6,
            closedByDefault = true
    )
    String dynamicBarSettings = "dynamicBarSettings";

    @ConfigItem(
            keyName = "dynamicHealthColor",
            name = "Dynamic Health Bar Color",
            description = "Enable dynamic health bar color (uses dynamic bar settings).",
            position = 0,
            section = dynamicBarSettings
    )
    default boolean dynamicHealthColor() {
        return false;
    }

    @ConfigItem(
            keyName = "yellowThresholdValue",
            name = "Yellow Threshold Value",
            description = "Threshold for yellow color (interpreted as percent or HP).",
            position = 1,
            section = dynamicBarSettings
    )
    default int yellowThresholdValue() {
        return 75;
    }

    @ConfigItem(
            keyName = "yellowThresholdUnit",
            name = "Yellow Threshold Unit",
            description = "Unit for yellow threshold.",
            position = 2,
            section = dynamicBarSettings
    )
    default ThresholdUnit yellowThresholdUnit() {
        return ThresholdUnit.PERCENT;
    }

    @ConfigItem(
            keyName = "redThresholdValue",
            name = "Red Threshold Value",
            description = "Threshold for red color (interpreted as percent or HP).",
            position = 3,
            section = dynamicBarSettings
    )
    default int redThresholdValue() {
        return 25;
    }

    @ConfigItem(
            keyName = "redThresholdUnit",
            name = "Red Threshold Unit",
            description = "Unit for red threshold.",
            position = 4,
            section = dynamicBarSettings
    )
    default ThresholdUnit redThresholdUnit() {
        return ThresholdUnit.HP;
    }

    @ConfigItem(
            keyName = "enableBlink",
            name = "Enable Blink",
            description = "Enable blinking effect when below blink threshold.",
            position = 5,
            section = dynamicBarSettings
    )
    default boolean enableBlink() {
        return false;
    }

    @ConfigItem(
            keyName = "blinkThresholdValue",
            name = "Blink Threshold Value",
            description = "Threshold for blinking effect (interpreted as percent or HP).",
            position = 6,
            section = dynamicBarSettings
    )
    default int blinkThresholdValue() {
        return 10;
    }

    @ConfigItem(
            keyName = "blinkThresholdUnit",
            name = "Blink Threshold Unit",
            description = "Unit for blink threshold.",
            position = 7,
            section = dynamicBarSettings
    )
    default ThresholdUnit blinkThresholdUnit() {
        return ThresholdUnit.HP;
    }

    enum ThresholdUnit {
        PERCENT("%"),
        HP("HP");

        private final String display;
        ThresholdUnit(String display) {
            this.display = display;
        }
        public String getDisplay() {
            return display;
        }
        @Override
        public String toString() {
            return display;
        }
    }

    // Highlight Section - Modified Dropdown Menus and Color Selections
    @ConfigSection(
            name = "Highlight",
            description = "Options for highlighting the opponent.",
            position = 7,
            closedByDefault = true
    )
    String highlightSection = "highlightSection";

    // Priority 1: Outline Highlight
    @ConfigItem(
            keyName = "outlineHighlightMode",
            name = "Outline Highlight",
            description = "Select mode for outline highlight: None, Static, or Dynamic.",
            position = 0,
            section = highlightSection
    )
    default HighlightMode outlineHighlightMode() {
        return HighlightMode.NONE;
    }

    @Alpha
    @ConfigItem(
            keyName = "outlineHighlightColor",
            name = "Outline Color",
            description = "Color for outline highlight in Static mode.",
            position = 1,
            section = highlightSection
    )
    default Color outlineHighlightColor() {
        return Color.RED;
    }

    // Priority 2: Hull Highlight
    @ConfigItem(
            keyName = "hullHighlightMode",
            name = "Hull Highlight",
            description = "Select mode for hull highlight: None, Static, or Dynamic.",
            position = 2,
            section = highlightSection
    )
    default HighlightMode hullHighlightMode() {
        return HighlightMode.NONE;
    }

    @Alpha
    @ConfigItem(
            keyName = "hullHighlightColor",
            name = "Hull Color",
            description = "Color for hull highlight in Static mode.",
            position = 3,
            section = highlightSection
    )
    default Color hullHighlightColor() {
        return Color.RED;
    }

    // Priority 3: Tile Highlight
    @ConfigItem(
            keyName = "tileHighlightMode",
            name = "Tile Highlight",
            description = "Select mode for tile highlight: None, Static, or Dynamic.",
            position = 4,
            section = highlightSection
    )
    default HighlightMode tileHighlightMode() {
        return HighlightMode.NONE;
    }

    @Alpha
    @ConfigItem(
            keyName = "tileHighlightColor",
            name = "Tile Color",
            description = "Color for tile highlight in Static mode.",
            position = 5,
            section = highlightSection
    )
    default Color tileHighlightColor() {
        return Color.RED;
    }

    enum HighlightMode {
        NONE("None"),
        STATIC("Static"),
        DYNAMIC("Dynamic");

        private final String display;
        HighlightMode(String display) {
            this.display = display;
        }
        public String getDisplay() {
            return display;
        }
        @Override
        public String toString() {
            return display;
        }
    }
}
