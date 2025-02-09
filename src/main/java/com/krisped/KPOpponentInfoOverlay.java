package com.krisped;

import com.google.common.base.Strings;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import javax.inject.Inject;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.ParamID;
import net.runelite.api.Player;
import net.runelite.api.VarPlayer;
import net.runelite.api.Varbits;
import net.runelite.api.kit.KitType;
import net.runelite.client.game.NPCManager;
import net.runelite.client.hiscore.HiscoreManager;
import net.runelite.client.hiscore.HiscoreResult;
import net.runelite.client.hiscore.HiscoreSkill;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.ComponentConstants;
import net.runelite.client.ui.overlay.components.ProgressBarComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;
import net.runelite.client.util.Text;

class KPOpponentInfoOverlay extends OverlayPanel {

    private static final Color HP_GREEN = new Color(0, 146, 54, 230);
    private static final Color HP_RED = new Color(255, 0, 0, 230);
    private static final Color HP_YELLOW = new Color(255, 255, 0, 230);

    private final Client client;
    private final KPOpponentInfoPlugin plugin;
    private final KPOpponentInfoConfig config;
    private final HiscoreManager hiscoreManager;
    private final NPCManager npcManager;

    @Inject
    private ItemManager itemManager;

    private Integer lastMaxHealth;
    private int lastRatio = 0;
    private int lastHealthScale = 0;
    private String opponentName;

    @Inject
    private KPOpponentInfoOverlay(
            Client client,
            KPOpponentInfoPlugin plugin,
            KPOpponentInfoConfig config,
            HiscoreManager hiscoreManager,
            NPCManager npcManager)
    {
        super(plugin);
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        this.hiscoreManager = hiscoreManager;
        this.npcManager = npcManager;
        setPosition(OverlayPosition.TOP_LEFT);
        setPriority(PRIORITY_HIGH);
        panelComponent.setBorder(new Rectangle(2, 2, 2, 2));
        panelComponent.setGap(new Point(0, 2));
        addMenuEntry(net.runelite.api.MenuAction.RUNELITE_OVERLAY_CONFIG,
                net.runelite.client.ui.overlay.OverlayManager.OPTION_CONFIGURE,
                "[KP] Opponent Information overlay");
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        final Actor opponent = plugin.getLastOpponent();
        if (opponent == null)
        {
            opponentName = null;
            return null;
        }
        if (opponent.getName() != null && opponent.getHealthScale() > 0)
        {
            lastRatio = opponent.getHealthRatio();
            lastHealthScale = opponent.getHealthScale();
            opponentName = Text.removeTags(opponent.getName());
            lastMaxHealth = null;
            if (opponent instanceof NPC)
            {
                NPCComposition composition = ((NPC) opponent).getTransformedComposition();
                if (composition != null)
                {
                    String longName = composition.getStringValue(ParamID.NPC_HP_NAME);
                    if (!Strings.isNullOrEmpty(longName))
                    {
                        opponentName = longName;
                    }
                }
                lastMaxHealth = npcManager.getHealth(((NPC) opponent).getId());
            }
            else if (opponent instanceof Player)
            {
                final HiscoreResult hiscoreResult = hiscoreManager.lookupAsync(opponentName, plugin.getHiscoreEndpoint());
                if (hiscoreResult != null)
                {
                    final int hp = hiscoreResult.getSkill(HiscoreSkill.HITPOINTS).getLevel();
                    if (hp > 0)
                    {
                        lastMaxHealth = hp;
                    }
                }
            }
        }
        if (opponentName == null || hasHpHud(opponent) || !config.showOpponentHealthOverlay())
        {
            return null;
        }
        final FontMetrics fontMetrics = graphics.getFontMetrics();
        int panelWidth = Math.max(ComponentConstants.STANDARD_WIDTH,
                fontMetrics.stringWidth(opponentName) + ComponentConstants.STANDARD_BORDER * 2);
        panelComponent.setPreferredSize(new Dimension(panelWidth, 0));
        panelComponent.getChildren().add(TitleComponent.builder().text(opponentName).build());

        // Legg til progress bar for HP
        if (lastRatio >= 0 && lastHealthScale > 0)
        {
            final ProgressBarComponent progressBarComponent = new ProgressBarComponent();
            progressBarComponent.setBackgroundColor(new Color(0, 0, 0, 150));
            final HitpointsDisplayStyle displayStyle = config.hitpointsDisplayStyle();
            double healthPercentage = 0;
            int currentHealthAbsolute = 0;
            if ((displayStyle == HitpointsDisplayStyle.HITPOINTS || displayStyle == HitpointsDisplayStyle.BOTH)
                    && lastMaxHealth != null)
            {
                int health = 0;
                if (lastRatio > 0)
                {
                    int minHealth = 1;
                    int maxHealth;
                    if (lastHealthScale > 1)
                    {
                        if (lastRatio > 1)
                        {
                            minHealth = (lastMaxHealth * (lastRatio - 1) + lastHealthScale - 2)
                                    / (lastHealthScale - 1);
                        }
                        maxHealth = (lastMaxHealth * lastRatio - 1) / (lastHealthScale - 1);
                        if (maxHealth > lastMaxHealth)
                        {
                            maxHealth = lastMaxHealth;
                        }
                    }
                    else
                    {
                        maxHealth = lastMaxHealth;
                    }
                    health = (minHealth + maxHealth + 1) / 2;
                }
                currentHealthAbsolute = health;
                progressBarComponent.setMaximum(lastMaxHealth);
                progressBarComponent.setValue(health);
                healthPercentage = (double) health / lastMaxHealth;
            }
            else
            {
                float floatRatio = lastRatio / (float) lastHealthScale;
                progressBarComponent.setValue(floatRatio * 100d);
                healthPercentage = lastRatio / (double) lastHealthScale;
            }

            Color finalColor = HP_GREEN;
            if (config.dynamicHealthColor())
            {
                boolean belowRed = false;
                boolean belowYellow = false;
                if (lastMaxHealth != null)
                {
                    if (config.redThresholdUnit() == KPOpponentInfoConfig.ThresholdUnit.HP)
                    {
                        belowRed = currentHealthAbsolute < config.redThresholdValue();
                    }
                    else
                    {
                        belowRed = (healthPercentage * 100) < config.redThresholdValue();
                    }
                    if (config.yellowThresholdUnit() == KPOpponentInfoConfig.ThresholdUnit.HP)
                    {
                        belowYellow = currentHealthAbsolute < config.yellowThresholdValue();
                    }
                    else
                    {
                        belowYellow = (healthPercentage * 100) < config.yellowThresholdValue();
                    }
                }
                else
                {
                    belowRed = (healthPercentage * 100) < config.redThresholdValue();
                    belowYellow = (healthPercentage * 100) < config.yellowThresholdValue();
                }
                if (belowRed)
                {
                    finalColor = HP_RED;
                }
                else if (belowYellow)
                {
                    finalColor = HP_YELLOW;
                }
            }

            if (config.enableBlink())
            {
                boolean blinkActive = false;
                if (lastMaxHealth != null)
                {
                    if (config.blinkThresholdUnit() == KPOpponentInfoConfig.ThresholdUnit.HP)
                    {
                        blinkActive = currentHealthAbsolute < config.blinkThresholdValue();
                    }
                    else
                    {
                        blinkActive = (healthPercentage * 100) < config.blinkThresholdValue();
                    }
                }
                else
                {
                    blinkActive = (healthPercentage * 100) < config.blinkThresholdValue();
                }
                if (blinkActive)
                {
                    long time = System.currentTimeMillis();
                    if ((time / 500) % 2 == 0)
                    {
                        finalColor = new Color(finalColor.getRed() / 2,
                                finalColor.getGreen() / 2,
                                finalColor.getBlue() / 2,
                                finalColor.getAlpha());
                    }
                }
            }

            progressBarComponent.setForegroundColor(finalColor);

            if ((config.hitpointsDisplayStyle() == HitpointsDisplayStyle.HITPOINTS ||
                    config.hitpointsDisplayStyle() == HitpointsDisplayStyle.BOTH)
                    && lastMaxHealth != null)
            {
                ProgressBarComponent.LabelDisplayMode mode = config.hitpointsDisplayStyle() == HitpointsDisplayStyle.BOTH ?
                        ProgressBarComponent.LabelDisplayMode.BOTH :
                        ProgressBarComponent.LabelDisplayMode.FULL;
                progressBarComponent.setLabelDisplayMode(mode);
            }
            panelComponent.getChildren().add(progressBarComponent);
        }

        // Legg til ekstra linje for target combat display dersom aktivert
        if (plugin.getLastOpponent() instanceof Player)
        {
            Player targetPlayer = (Player) plugin.getLastOpponent();
            KPOpponentInfoConfig.TargetCombatDisplay tcDisplay = config.targetCombatDisplay();
            if (tcDisplay != KPOpponentInfoConfig.TargetCombatDisplay.NONE)
            {
                String attackStyle = determineAttackStyle(targetPlayer);
                String weaponName = determineWeaponName(targetPlayer);
                if (tcDisplay == KPOpponentInfoConfig.TargetCombatDisplay.ATTACK_STYLE)
                {
                    panelComponent.getChildren().add(
                            TitleComponent.builder()
                                    .text("Attack: " + attackStyle)
                                    .color(new Color(173, 216, 230)) // Lys blå
                                    .build());
                }
                else if (tcDisplay == KPOpponentInfoConfig.TargetCombatDisplay.WEAPON)
                {
                    panelComponent.getChildren().add(
                            TitleComponent.builder()
                                    .text("Weapon: " + weaponName)
                                    .color(new Color(173, 216, 230))
                                    .build());
                }
                else if (tcDisplay == KPOpponentInfoConfig.TargetCombatDisplay.BOTH)
                {
                    panelComponent.getChildren().add(
                            TitleComponent.builder()
                                    .text("Attack: " + attackStyle)
                                    .color(new Color(173, 216, 230))
                                    .build());
                    panelComponent.getChildren().add(
                            TitleComponent.builder()
                                    .text("Weapon: " + weaponName)
                                    .color(new Color(173, 216, 230))
                                    .build());
                }
            }
        }

        // Legg til ekstra linje for risiko dersom Risk Display Option er OVERLAY eller BOTH
        KPOpponentInfoConfig.RiskDisplayOption riskOption = config.riskDisplayOption();
        if ((riskOption == KPOpponentInfoConfig.RiskDisplayOption.OVERLAY ||
                riskOption == KPOpponentInfoConfig.RiskDisplayOption.BOTH) &&
                plugin.getRiskValue() > 0)
        {
            String riskString = "Risk: " + formatWealth(plugin.getRiskValue());
            panelComponent.getChildren().add(
                    TitleComponent.builder()
                            .text(riskString)
                            .color(new Color(204, 153, 0)) // Mørk gul/gull
                            .build());
        }

        return super.render(graphics);
    }

    private static String formatWealth(long value)
    {
        if (value >= 1_000_000)
        {
            double millions = value / 1_000_000.0;
            return String.format("%.2fm", millions);
        }
        else if (value >= 1_000)
        {
            double thousands = value / 1_000.0;
            return String.format("%.2fk", thousands);
        }
        else
        {
            return Long.toString(value);
        }
    }

    private String determineAttackStyle(Player player)
    {
        int weaponId = player.getPlayerComposition().getEquipmentId(KitType.WEAPON);
        if (weaponId == -1)
        {
            return "Melee";
        }
        ItemComposition comp = itemManager.getItemComposition(weaponId);
        if (comp == null)
        {
            return "Melee";
        }
        String name = comp.getName().toLowerCase();
        if (name.contains("bow") || name.contains("crossbow") || name.contains("javelin"))
        {
            return "Ranged";
        }
        else if (name.contains("staff") || name.contains("wand") || name.contains("scepter") || name.contains("blowpipe"))
        {
            return "Magic";
        }
        else
        {
            return "Melee";
        }
    }

    private String determineWeaponName(Player player)
    {
        int weaponId = player.getPlayerComposition().getEquipmentId(KitType.WEAPON);
        if (weaponId == -1)
        {
            return "None";
        }
        ItemComposition comp = itemManager.getItemComposition(weaponId);
        if (comp == null)
        {
            return "Unknown";
        }
        return comp.getName();
    }

    private boolean hasHpHud(Actor opponent)
    {
        boolean settingEnabled = client.getVarbitValue(Varbits.BOSS_HEALTH_OVERLAY) == 0;
        if (settingEnabled && opponent instanceof NPC)
        {
            int opponentId = client.getVarpValue(VarPlayer.HP_HUD_NPC_ID);
            NPC npc = (NPC) opponent;
            return opponentId != -1 && npc.getComposition() != null &&
                    opponentId == npc.getComposition().getId();
        }
        return false;
    }
}
