package com.krisped;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import javax.swing.*;
import java.awt.*;

@ConfigGroup("kp_opponentinfo")
public interface KPOpponentInfoConfig extends Config
{
    @ConfigItem(
            keyName = "lookupOnInteraction",
            name = "Lookup players on interaction",
            description = "Display a combat stat comparison panel on player interaction (Attack, Trade, etc.).",
            position = 0
    )
    default boolean lookupOnInteraction()
    {
        return false;
    }

    @ConfigItem(
            keyName = "hitpointsDisplayStyle",
            name = "Display style",
            description = "Show opponent's hitpoints as a value, percentage, or both.",
            position = 1
    )
    default HitpointsDisplayStyle hitpointsDisplayStyle()
    {
        return HitpointsDisplayStyle.HITPOINTS;
    }

    @ConfigItem(
            keyName = "showOpponentsInMenu",
            name = "Show opponents in menu",
            description = "Marks opponents' names in the menu (NPC only).",
            position = 3
    )
    default boolean showOpponentsInMenu()
    {
        return false;
    }

    @ConfigItem(
            keyName = "showOpponentHealthOverlay",
            name = "Show opponent health overlay",
            description = "Shows a health bar overlay when a boss overlay is not present.",
            position = 4
    )
    default boolean showOpponentHealthOverlay()
    {
        return true;
    }

    @ConfigItem(
            keyName = "overlayDisplayDuration",
            name = "Overlay Display Duration (seconds)",
            description = "The number of seconds before the overlay automatically disappears.",
            position = 5
    )
    default int overlayDisplayDuration()
    {
        return 5;
    }

    // Dynamic Bar Settings-seksjonen
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
    default boolean dynamicHealthColor()
    {
        return false;
    }

    @ConfigItem(
            keyName = "yellowThresholdValue",
            name = "Yellow Threshold Value",
            description = "Threshold for yellow color (interpreted as percent or HP).",
            position = 1,
            section = dynamicBarSettings
    )
    default int yellowThresholdValue()
    {
        return 75;
    }

    @ConfigItem(
            keyName = "yellowThresholdUnit",
            name = "Yellow Threshold Unit",
            description = "Unit for yellow threshold.",
            position = 2,
            section = dynamicBarSettings
    )
    default ThresholdUnit yellowThresholdUnit()
    {
        return ThresholdUnit.PERCENT;
    }

    @ConfigItem(
            keyName = "redThresholdValue",
            name = "Red Threshold Value",
            description = "Threshold for red color (interpreted as percent or HP).",
            position = 3,
            section = dynamicBarSettings
    )
    default int redThresholdValue()
    {
        return 25;
    }

    @ConfigItem(
            keyName = "redThresholdUnit",
            name = "Red Threshold Unit",
            description = "Unit for red threshold.",
            position = 4,
            section = dynamicBarSettings
    )
    default ThresholdUnit redThresholdUnit()
    {
        return ThresholdUnit.HP;
    }

    @ConfigItem(
            keyName = "enableBlink",
            name = "Enable Blink",
            description = "Enable blinking effect when below blink threshold.",
            position = 5,
            section = dynamicBarSettings
    )
    default boolean enableBlink()
    {
        return false;
    }

    @ConfigItem(
            keyName = "blinkThresholdValue",
            name = "Blink Threshold Value",
            description = "Threshold for blinking effect (interpreted as percent or HP).",
            position = 6,
            section = dynamicBarSettings
    )
    default int blinkThresholdValue()
    {
        return 10;
    }

    @ConfigItem(
            keyName = "blinkThresholdUnit",
            name = "Blink Threshold Unit",
            description = "Unit for blink threshold.",
            position = 7,
            section = dynamicBarSettings
    )
    default ThresholdUnit blinkThresholdUnit()
    {
        return ThresholdUnit.HP;
    }

    // Enum for units
    enum ThresholdUnit
    {
        PERCENT("%"),
        HP("HP");

        private final String display;

        ThresholdUnit(String display)
        {
            this.display = display;
        }

        public String getDisplay()
        {
            return display;
        }

        @Override
        public String toString()
        {
            return display;
        }
    }
}
