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
    default boolean lookupOnInteraction() { return false; }

    @ConfigItem(
            keyName = "hitpointsDisplayStyle",
            name = "Display style",
            description = "Show opponent's hitpoints as a value, percentage, or both.",
            position = 1
    )
    default HitpointsDisplayStyle hitpointsDisplayStyle() { return HitpointsDisplayStyle.HITPOINTS; }

    @ConfigItem(
            keyName = "showOpponentsInMenu",
            name = "Show opponents in menu",
            description = "Marks opponents' names in the menu (NPC only).",
            position = 2
    )
    default boolean showOpponentsInMenu() { return false; }

    @ConfigItem(
            keyName = "showOpponentHealthOverlay",
            name = "Show opponent health overlay",
            description = "Shows a health bar overlay when a boss overlay is not present.",
            position = 3
    )
    default boolean showOpponentHealthOverlay() { return true; }

    @ConfigItem(
            keyName = "overlayDisplayDuration",
            name = "Overlay Display Duration (seconds)",
            description = "Number of seconds before the overlay automatically disappears.",
            position = 4
    )
    default int overlayDisplayDuration() { return 5; }

    // Extended Features
    @ConfigSection(
            name = "Extended Features",
            description = "Options for risk display and target combat details.",
            position = 5,
            closedByDefault = true
    )
    String extendedFeatures = "extendedFeatures";

    @ConfigItem(
            keyName = "showAttackStyle",
            name = "Show Attack Style",
            description = "Viser angrepsstil for motstanderen.",
            position = 0,
            section = extendedFeatures
    )
    default boolean showAttackStyle() { return false; }

    @ConfigItem(
            keyName = "weaponDisplay",
            name = "Weapon Display",
            description = "Velg våpenvisning: None, Current, Current & Last",
            position = 1,
            section = extendedFeatures
    )
    default WeaponDisplayOption weaponDisplay() { return WeaponDisplayOption.NONE; }

    enum WeaponDisplayOption
    {
        NONE("None"),
        CURRENT("Current"),
        CURRENT_AND_LAST("Current & Last");

        private final String display;
        WeaponDisplayOption(String display){ this.display=display; }
        public String getDisplay(){ return display; }
        @Override
        public String toString(){ return display; }
    }

    @ConfigItem(
            keyName = "showSmitedPrayer",
            name = "Show Smited Prayer",
            description = "Toggle to display the total smited prayer points on the overlay.",
            position = 2,
            section = extendedFeatures
    )
    default boolean showSmitedPrayer() { return false; }

    // Toggle for AttackStyle/Weapon: Icons vs Text
    @ConfigItem(
            keyName = "overlayIcons",
            name = "Overlay Icons",
            description = "Velg om overlay skal vise ikoner eller ren tekst for Attack Style / Weapon",
            position = 3,
            section = extendedFeatures
    )
    default OverlayIcons overlayIcons() { return OverlayIcons.ICONS; }

    enum OverlayIcons
    {
        ICONS("Icons"),
        TEXT("Text");

        private final String display;
        OverlayIcons(String display) { this.display=display; }
        public String getDisplay(){return display;}
        @Override
        public String toString(){return display;}
    }

    // Spell-linje: None, Text, Icons
    enum SpellDisplayMode
    {
        NONE("None"),
        TEXT("Text"),
        ICONS("Icons");

        private final String display;
        SpellDisplayMode(String display){this.display=display;}
        public String getDisplay(){return display;}
        @Override
        public String toString(){return display;}
    }

    @ConfigItem(
            keyName = "spellDisplayMode",
            name = "Show Spells",
            description = "Choose how spells are displayed: None, Text, Icons",
            position = 4,
            section = extendedFeatures
    )
    default SpellDisplayMode spellDisplayMode() { return SpellDisplayMode.NONE; }

    // Dynamic Bar
    @ConfigSection(
            name = "Dynamic Bar Settings",
            description = "Settings for dynamic bar color and blinking.",
            position = 6,
            closedByDefault = true
    )
    String dynamicBarSettings="dynamicBarSettings";

    @ConfigItem(
            keyName = "dynamicHealthColor",
            name = "Dynamic Health Bar Color",
            description = "Enable dynamic health bar color (uses dynamic bar settings).",
            position = 0,
            section = dynamicBarSettings
    )
    default boolean dynamicHealthColor(){return false;}

    @ConfigItem(
            keyName = "yellowThresholdValue",
            name = "Yellow Threshold Value",
            description = "Threshold for yellow color (interpreted as percent or HP).",
            position = 1,
            section = dynamicBarSettings
    )
    default int yellowThresholdValue(){return 75;}

    @ConfigItem(
            keyName = "yellowThresholdUnit",
            name = "Yellow Threshold Unit",
            description = "Unit for yellow threshold.",
            position = 2,
            section = dynamicBarSettings
    )
    default ThresholdUnit yellowThresholdUnit(){return ThresholdUnit.PERCENT;}

    @ConfigItem(
            keyName = "redThresholdValue",
            name = "Red Threshold Value",
            description = "Threshold for red color (interpreted as percent or HP).",
            position = 3,
            section = dynamicBarSettings
    )
    default int redThresholdValue(){return 25;}

    @ConfigItem(
            keyName = "redThresholdUnit",
            name = "Red Threshold Unit",
            description = "Unit for red threshold.",
            position = 4,
            section = dynamicBarSettings
    )
    default ThresholdUnit redThresholdUnit(){return ThresholdUnit.HP;}

    @ConfigItem(
            keyName = "enableBlink",
            name = "Enable Blink",
            description = "Blink effect when below blink threshold.",
            position = 5,
            section = dynamicBarSettings
    )
    default boolean enableBlink(){return false;}

    @ConfigItem(
            keyName = "blinkThresholdValue",
            name = "Blink Threshold Value",
            description = "Threshold for blinking effect (interpreted as percent or HP).",
            position = 6,
            section = dynamicBarSettings
    )
    default int blinkThresholdValue(){return 10;}

    @ConfigItem(
            keyName = "blinkThresholdUnit",
            name = "Blink Threshold Unit",
            description = "Unit for blink threshold.",
            position = 7,
            section = dynamicBarSettings
    )
    default ThresholdUnit blinkThresholdUnit(){return ThresholdUnit.HP;}

    enum ThresholdUnit
    {
        PERCENT("%"),
        HP("HP");
        private final String display;
        ThresholdUnit(String display){this.display=display;}
        public String getDisplay(){return display;}
        @Override
        public String toString(){return display;}
    }

    // Risk Settings
    @ConfigSection(
            name = "Risk Settings",
            description = "Settings for risk thresholds and colors.",
            position = 7,
            closedByDefault = true
    )
    String riskSettings="riskSettings";

    @ConfigItem(
            keyName = "riskDisplayOption",
            name = "Risk Display Option",
            description = "Select where to display risk: None, Chat, Overlay, or Both.",
            position = 0,
            section = riskSettings
    )
    default RiskDisplayOption riskDisplayOption(){return RiskDisplayOption.NONE;}

    enum RiskDisplayOption
    {
        NONE("None"),
        CHAT("Chat"),
        OVERLAY("Overlay"),
        BOTH("Both");

        private final String display;
        RiskDisplayOption(String display){this.display=display;}
        public String getDisplay(){return display;}
        @Override
        public String toString(){return display;}
    }

    @ConfigItem(
            keyName = "enableColorRisk",
            name = "Enable Color Risk",
            description = "If enabled, risk value is shown in a color based on thresholds; otherwise white.",
            position = 1,
            section = riskSettings
    )
    default boolean enableColorRisk(){return false;}

    @ConfigItem(
            keyName = "lowRiskThreshold",
            name = "Low Risk Threshold",
            description = "Risk threshold for low bracket.",
            position = 2,
            section = riskSettings
    )
    default int lowRiskThreshold(){return 20000;} // 20k

    @Alpha
    @ConfigItem(
            keyName = "lowRiskColor",
            name = "Low Risk Color",
            description = "Color for the 'low' bracket (20k–100k).",
            position = 3,
            section = riskSettings
    )
    default Color lowRiskColor(){return new Color(0xFF66B2FF,true);}

    @ConfigItem(
            keyName = "mediumRiskThreshold",
            name = "Medium Risk Threshold",
            description = "Risk threshold for medium bracket.",
            position = 4,
            section = riskSettings
    )
    default int mediumRiskThreshold(){return 100000;} // 100k

    @Alpha
    @ConfigItem(
            keyName = "mediumRiskColor",
            name = "Medium Risk Color",
            description = "Color for medium bracket (100k–1m).",
            position = 5,
            section = riskSettings
    )
    default Color mediumRiskColor(){return new Color(0xFF99FF99,true);}

    @ConfigItem(
            keyName = "highRiskThreshold",
            name = "High Risk Threshold",
            description = "Risk threshold for high bracket (1m–10m).",
            position = 6,
            section = riskSettings
    )
    default int highRiskThreshold(){return 1_000_000;} // 1m

    @Alpha
    @ConfigItem(
            keyName = "highRiskColor",
            name = "High Risk Color",
            description = "Color for high bracket (1m–10m).",
            position = 7,
            section = riskSettings
    )
    default Color highRiskColor(){return new Color(0xFFFF9600,true);}

    @ConfigItem(
            keyName = "insaneRiskThreshold",
            name = "Insane Risk Threshold",
            description = "Risk threshold for insane bracket (10m+).",
            position = 8,
            section = riskSettings
    )
    default int insaneRiskThreshold(){return 10_000_000;} // 10m

    @Alpha
    @ConfigItem(
            keyName = "insaneRiskColor",
            name = "Insane Risk Color",
            description = "Color for the 'insane' bracket (10m+).",
            position = 9,
            section = riskSettings
    )
    default Color insaneRiskColor(){return new Color(0xFFFF66B2,true);}

    // Highlight Section
    @ConfigSection(
            name = "Highlight",
            description = "Options for highlighting the opponent.",
            position = 8,
            closedByDefault = true
    )
    String highlightSection="highlightSection";

    @ConfigItem(
            keyName = "outlineHighlightMode",
            name = "Outline Highlight",
            description = "Select mode for outline highlight: None, Static, or Dynamic.",
            position = 0,
            section = highlightSection
    )
    default HighlightMode outlineHighlightMode(){return HighlightMode.NONE;}

    @ConfigItem(
            keyName = "outlineBlink",
            name = "Outline Blink",
            description = "Enable blink effect for outline highlight (static mode only).",
            position = 1,
            section = highlightSection
    )
    default boolean outlineBlink(){return false;}

    @Alpha
    @ConfigItem(
            keyName = "outlineHighlightColor",
            name = "Outline Color",
            description = "Color for outline highlight in Static mode.",
            position = 2,
            section = highlightSection
    )
    default Color outlineHighlightColor(){return Color.RED;}

    @ConfigItem(
            keyName = "hullHighlightMode",
            name = "Hull Highlight",
            description = "Select mode for hull highlight: None, Static, or Dynamic.",
            position = 3,
            section = highlightSection
    )
    default HighlightMode hullHighlightMode(){return HighlightMode.NONE;}

    @ConfigItem(
            keyName = "hullBlink",
            name = "Hull Blink",
            description = "Enable blink effect for hull highlight (static mode only).",
            position = 4,
            section = highlightSection
    )
    default boolean hullBlink(){return false;}

    @Alpha
    @ConfigItem(
            keyName = "hullHighlightColor",
            name = "Hull Color",
            description = "Color for hull highlight in Static mode.",
            position = 5,
            section = highlightSection
    )
    default Color hullHighlightColor(){return Color.RED;}

    @ConfigItem(
            keyName = "tileHighlightMode",
            name = "Tile Highlight",
            description = "Select mode for tile highlight: None, Static, or Dynamic.",
            position = 6,
            section = highlightSection
    )
    default HighlightMode tileHighlightMode(){return HighlightMode.NONE;}

    @ConfigItem(
            keyName = "tileBlink",
            name = "Tile Blink",
            description = "Enable blink effect for tile highlight (static mode only).",
            position = 7,
            section = highlightSection
    )
    default boolean tileBlink(){return false;}

    @Alpha
    @ConfigItem(
            keyName = "tileHighlightColor",
            name = "Tile Color",
            description = "Color for tile highlight in Static mode.",
            position = 8,
            section = highlightSection
    )
    default Color tileHighlightColor(){return Color.RED;}

    // *** ADDED: Enable NPC highlight ***
    @ConfigItem(
            keyName = "enableNpcHighlight",
            name = "Enable NPC Highlight",
            description = "Include NPCs in highlight (outline/hull/tile).",
            position = 9,
            section = highlightSection
    )
    default boolean enableNpcHighlight() { return false; }

    enum HighlightMode
    {
        NONE("None"),
        STATIC("Static"),
        DYNAMIC("Dynamic");

        private final String display;
        HighlightMode(String display){this.display=display;}
        public String getDisplay(){return display;}
        @Override
        public String toString(){return display;}
    }

    // Overlay Priority
    @ConfigSection(
            name = "Overlay Priority",
            description = "Angi prioritet (lavere tall vises først)",
            position = 9,
            closedByDefault = true
    )
    String overlayPriority="overlayPriority";

    @ConfigItem(
            keyName = "healthBarPriority",
            name = "Health Bar Priority",
            description = "Prioritet for helsebjelken.",
            position = 0,
            section = overlayPriority
    )
    default int healthBarPriority(){return 1;}

    @ConfigItem(
            keyName = "smitedPriority",
            name = "Smited Priority",
            description = "Prioritet for smited prayer.",
            position = 1,
            section = overlayPriority
    )
    default int smitedPriority(){return 2;}

    @ConfigItem(
            keyName = "attackTypePriority",
            name = "Attack Type Priority",
            description = "Prioritet for attack type.",
            position = 2,
            section = overlayPriority
    )
    default int attackTypePriority(){return 3;}

    @ConfigItem(
            keyName = "weaponPriority",
            name = "Weapon Priority",
            description = "Prioritet for våpeninfo.",
            position = 3,
            section = overlayPriority
    )
    default int weaponPriority(){return 4;}

    @ConfigItem(
            keyName = "riskPriority",
            name = "Risk Priority",
            description = "Prioritet for risikovisning.",
            position = 4,
            section = overlayPriority
    )
    default int riskPriority(){return 5;}

    // Debug
    @ConfigSection(
            name = "Debug",
            description = "Innstillinger for debug-overlay",
            position = 10,
            closedByDefault = true
    )
    String debugSection="debugSection";

    @ConfigItem(
            keyName = "debugOverlay",
            name = "Debug Overlay",
            description = "Hvis aktivert, vises overlay med eksempeldata uten timer. Deaktiveres ved kamp.",
            position = 0,
            section = debugSection
    )
    default boolean debugOverlay(){return false;}
}
