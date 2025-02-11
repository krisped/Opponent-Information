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
    // Generelle innstillinger
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

    // --- Nytt: Checkbox for Attack Style ---
    @ConfigItem(
            keyName = "showAttackStyle",
            name = "Show Attack Style",
            description = "Viser angrepsstil for motstanderen.",
            position = 0,
            section = extendedFeatures
    )
    default boolean showAttackStyle() {
        return false;
    }

    // --- Nytt: Dropdown for Weapon Display ---
    @ConfigItem(
            keyName = "weaponDisplay",
            name = "Weapon Display",
            description = "Velg våpenvisning: None, Current, Current & Last",
            position = 1,
            section = extendedFeatures
    )
    default WeaponDisplayOption weaponDisplay() {
        return WeaponDisplayOption.NONE;
    }

    enum WeaponDisplayOption {
        NONE("None"),
        CURRENT("Current"),
        CURRENT_AND_LAST("Current & Last");

        private final String display;

        WeaponDisplayOption(String display) {
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

    // Gammel targetCombatDisplay fjernes (eller kommenteres ut)
    /*
    @ConfigItem(
            keyName = "targetCombatDisplay",
            name = "Target Combat Display",
            description = "Select how to display target's combat details: None, Attack Style, Wep, or Both.",
            position = 0,
            section = extendedFeatures
    )
    default TargetCombatDisplay targetCombatDisplay() {
        return TargetCombatDisplay.NONE;
    }

    enum TargetCombatDisplay {
        NONE("None"),
        ATTACK_STYLE("Attack Style"),
        WEAPON("Wep"),
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
    */

    @ConfigItem(
            keyName = "showSmitedPrayer",
            name = "Show Smited Prayer",
            description = "Toggle to display the total smited prayer points on the overlay.",
            position = 1,
            section = extendedFeatures
    )
    default boolean showSmitedPrayer() {
        return false;
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

    // Risk Settings Section
    @ConfigSection(
            name = "Risk Settings",
            description = "Settings for risk thresholds and colors.",
            position = 7,
            closedByDefault = true
    )
    String riskSettings = "riskSettings";

    @ConfigItem(
            keyName = "riskDisplayOption",
            name = "Risk Display Option",
            description = "Select where to display risk: None, Chat, Overlay, or Both.",
            position = 0,
            section = riskSettings
    )
    default RiskDisplayOption riskDisplayOption() {
        return RiskDisplayOption.NONE;
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

    @ConfigItem(
            keyName = "enableColorRisk",
            name = "Enable Color Risk",
            description = "If enabled, risk value is shown in a color based on thresholds; otherwise, it is always white.",
            position = 1,
            section = riskSettings
    )
    default boolean enableColorRisk() {
        return false;
    }

    @ConfigItem(
            keyName = "lowRiskThreshold",
            name = "Low Risk Threshold",
            description = "Risk threshold for low value items.",
            position = 2,
            section = riskSettings
    )
    default int lowRiskThreshold() {
        return 20000;
    }

    @Alpha
    @ConfigItem(
            keyName = "lowRiskColor",
            name = "Low Risk Color",
            description = "Color for risk below the low threshold (default #FF66B2FF).",
            position = 3,
            section = riskSettings
    )
    default Color lowRiskColor() {
        return new Color(0xFF66B2FF, true);
    }

    @ConfigItem(
            keyName = "mediumRiskThreshold",
            name = "Medium Risk Threshold",
            description = "Risk threshold for medium value items.",
            position = 4,
            section = riskSettings
    )
    default int mediumRiskThreshold() {
        return 100000;
    }

    @Alpha
    @ConfigItem(
            keyName = "mediumRiskColor",
            name = "Medium Risk Color",
            description = "Color for risk between low and high thresholds (default #FF99FF99).",
            position = 5,
            section = riskSettings
    )
    default Color mediumRiskColor() {
        return new Color(0xFF99FF99, true);
    }

    @ConfigItem(
            keyName = "highRiskThreshold",
            name = "High Risk Threshold",
            description = "Risk threshold for high value items (risk over this becomes green).",
            position = 6,
            section = riskSettings
    )
    default int highRiskThreshold() {
        return 1000000;
    }

    @Alpha
    @ConfigItem(
            keyName = "highRiskColor",
            name = "High Risk Color",
            description = "Color for risk over high threshold and under insane threshold (default #FFFF9600).",
            position = 7,
            section = riskSettings
    )
    default Color highRiskColor() {
        return new Color(0xFFFF9600, true);
    }

    @ConfigItem(
            keyName = "insaneRiskThreshold",
            name = "Insane Risk Threshold",
            description = "Risk threshold for insane value items (risk over this becomes pink).",
            position = 8,
            section = riskSettings
    )
    default int insaneRiskThreshold() {
        return 10000000;
    }

    @Alpha
    @ConfigItem(
            keyName = "insaneRiskColor",
            name = "Insane Risk Color",
            description = "Color for risk over insane threshold (default #FFFF62B2).",
            position = 9,
            section = riskSettings
    )
    default Color insaneRiskColor() {
        return new Color(0xFFFF66B2, true);
    }

    // Highlight Section
    @ConfigSection(
            name = "Highlight",
            description = "Options for highlighting the opponent.",
            position = 8,
            closedByDefault = true
    )
    String highlightSection = "highlightSection";

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

    @ConfigItem(
            keyName = "outlineBlink",
            name = "Outline Blink",
            description = "Enable blink effect for outline highlight (static mode only).",
            position = 1,
            section = highlightSection
    )
    default boolean outlineBlink() {
        return false;
    }

    @Alpha
    @ConfigItem(
            keyName = "outlineHighlightColor",
            name = "Outline Color",
            description = "Color for outline highlight in Static mode.",
            position = 2,
            section = highlightSection
    )
    default Color outlineHighlightColor() {
        return Color.RED;
    }

    @ConfigItem(
            keyName = "hullHighlightMode",
            name = "Hull Highlight",
            description = "Select mode for hull highlight: None, Static, or Dynamic.",
            position = 3,
            section = highlightSection
    )
    default HighlightMode hullHighlightMode() {
        return HighlightMode.NONE;
    }

    @ConfigItem(
            keyName = "hullBlink",
            name = "Hull Blink",
            description = "Enable blink effect for hull highlight (static mode only).",
            position = 4,
            section = highlightSection
    )
    default boolean hullBlink() {
        return false;
    }

    @Alpha
    @ConfigItem(
            keyName = "hullHighlightColor",
            name = "Hull Color",
            description = "Color for hull highlight in Static mode.",
            position = 5,
            section = highlightSection
    )
    default Color hullHighlightColor() {
        return Color.RED;
    }

    @ConfigItem(
            keyName = "tileHighlightMode",
            name = "Tile Highlight",
            description = "Select mode for tile highlight: None, Static, or Dynamic.",
            position = 6,
            section = highlightSection
    )
    default HighlightMode tileHighlightMode() {
        return HighlightMode.NONE;
    }

    @ConfigItem(
            keyName = "tileBlink",
            name = "Tile Blink",
            description = "Enable blink effect for tile highlight (static mode only).",
            position = 7,
            section = highlightSection
    )
    default boolean tileBlink() {
        return false;
    }

    @Alpha
    @ConfigItem(
            keyName = "tileHighlightColor",
            name = "Tile Color",
            description = "Color for tile highlight in Static mode.",
            position = 8,
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
