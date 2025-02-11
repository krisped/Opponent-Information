package com.krisped;

import com.google.common.base.Strings;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
import net.runelite.client.game.SpriteManager;
import net.runelite.client.hiscore.HiscoreManager;
import net.runelite.client.hiscore.HiscoreResult;
import net.runelite.client.hiscore.HiscoreSkill;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.ComponentConstants;
import net.runelite.client.ui.overlay.components.ImageComponent;
import net.runelite.client.ui.overlay.components.ProgressBarComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.LayoutableRenderableEntity;
import net.runelite.client.util.Text;

class KPOpponentInfoOverlay extends OverlayPanel {

    private static final Color HP_GREEN = new Color(0, 146, 54, 230);
    private static final Color HP_RED = new Color(255, 0, 0, 230);
    private static final Color HP_YELLOW = new Color(255, 255, 0, 230);
    private static final Color RISK_LABEL_COLOR = Color.decode("#FFD700");

    private final Client client;
    private final KPOpponentInfoPlugin plugin;
    private final KPOpponentInfoConfig config;
    private final HiscoreManager hiscoreManager;
    private final NPCManager npcManager;
    @Inject
    private ItemManager itemManager;
    @Inject
    private SpriteManager spriteManager;

    private Integer lastMaxHealth;
    private int lastRatio = 0;
    private int lastHealthScale = 0;
    private String opponentName;

    // Sprite-ID-er:
    private final int currentWeaponSpriteId = 168;    // For CURRENT våpen
    private final int previousWeaponSpriteId = 168;     // For PREVIOUS våpen
    private final int smitedSpriteId = 132;             // For Smite
    private final int riskSpriteId = 523;               // For Risk
    // For Attack-linjen:
    private final int attackLabelSpriteId = 237;        // Ikon for "Attack:"
    private final int meleeAttackSpriteId = 129;        // For Melee
    private final int rangedAttackSpriteId = 128;       // For Ranged
    private final int magicAttackSpriteId = 127;        // For Magic

    // Intern klasse for overlay-elementer med prioritet
    private static class OverlayItem {
        private final double priority;
        private final LayoutableRenderableEntity component;

        OverlayItem(double priority, LayoutableRenderableEntity component) {
            this.priority = priority;
            this.component = component;
        }

        double getPriority() {
            return priority;
        }

        LayoutableRenderableEntity getComponent() {
            return component;
        }
    }

    /**
     * Lager et composite-bilde ved å kombinere et ikon (sprite) med tekst.
     */
    private BufferedImage createCompositeWeaponImage(BufferedImage icon, String text, Color textColor, Font font) {
        int gap = (icon != null) ? 5 : 0;
        BufferedImage temp = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gTemp = temp.createGraphics();
        gTemp.setFont(font);
        FontMetrics fm = gTemp.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getHeight();
        gTemp.dispose();
        int iconWidth = (icon != null) ? icon.getWidth() : 0;
        int compositeWidth = iconWidth + gap + textWidth;
        int compositeHeight = Math.max((icon != null ? icon.getHeight() : 0), textHeight);
        BufferedImage composite = new BufferedImage(compositeWidth, compositeHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gComposite = composite.createGraphics();
        gComposite.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        gComposite.setFont(font);
        if (icon != null) {
            gComposite.drawImage(icon, 0, 0, null);
        }
        gComposite.setColor(textColor);
        gComposite.drawString(text, iconWidth + gap, fm.getAscent());
        gComposite.dispose();
        return composite;
    }

    /**
     * Kombinerer to bilder horisontalt med et gap.
     */
    private BufferedImage concatImages(BufferedImage img1, BufferedImage img2, int gap) {
        int width = img1.getWidth() + gap + img2.getWidth();
        int height = Math.max(img1.getHeight(), img2.getHeight());
        BufferedImage combined = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = combined.createGraphics();
        g.drawImage(img1, 0, 0, null);
        g.drawImage(img2, img1.getWidth() + gap, 0, null);
        g.dispose();
        return combined;
    }

    /**
     * Lager et composite-bilde for Attack-linjen. Først lages et bilde for "Attack:" med sprite 237,
     * deretter lages et bilde for angrepsstilen med riktig ikon (129, 128 eller 127) foran stilen.
     * Til slutt kombineres de to til ett bilde.
     */
    private BufferedImage createAttackComposite(Graphics2D graphics, String attackStyle) {
        Font font = graphics.getFont();
        // Lag composite for "Attack:"-delen med sprite 237
        BufferedImage attackLabelSprite = spriteManager.getSprite(attackLabelSpriteId, 0);
        BufferedImage scaledAttackLabel = (attackLabelSprite != null) ? scaleImage(attackLabelSprite, 16, 16) : null;
        BufferedImage compositeAttackLabel = createCompositeWeaponImage(scaledAttackLabel, "Attack:", new Color(173,216,230), font);

        // Velg riktig ikon for angrepsstilen
        int styleSpriteId;
        if (attackStyle.equalsIgnoreCase("Melee")) {
            styleSpriteId = meleeAttackSpriteId;
        } else if (attackStyle.equalsIgnoreCase("Ranged")) {
            styleSpriteId = rangedAttackSpriteId;
        } else if (attackStyle.equalsIgnoreCase("Magic")) {
            styleSpriteId = magicAttackSpriteId;
        } else {
            styleSpriteId = meleeAttackSpriteId;
        }
        BufferedImage styleSprite = spriteManager.getSprite(styleSpriteId, 0);
        BufferedImage scaledStyleSprite = (styleSprite != null) ? scaleImage(styleSprite, 16, 16) : null;
        BufferedImage compositeStyle = createCompositeWeaponImage(scaledStyleSprite, attackStyle, new Color(173,216,230), font);

        // Kombiner de to composite bildene med et gap på 5 piksler
        return concatImages(compositeAttackLabel, compositeStyle, 5);
    }

    @Inject
    KPOpponentInfoOverlay(
            Client client,
            KPOpponentInfoPlugin plugin,
            KPOpponentInfoConfig config,
            HiscoreManager hiscoreManager,
            NPCManager npcManager
    ) {
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
    public Dimension render(Graphics2D graphics) {
        List<OverlayItem> overlayItems = new ArrayList<>();
        panelComponent.getChildren().clear();

        // DEBUG-modus
        if (config.debugOverlay() && plugin.getLastOpponent() == null) {
            panelComponent.getChildren().add(TitleComponent.builder().text("Debug").build());
            ProgressBarComponent debugHealth = new ProgressBarComponent();
            debugHealth.setMaximum(100);
            debugHealth.setValue(100);
            debugHealth.setForegroundColor(HP_GREEN);
            debugHealth.setBackgroundColor(new Color(0, 0, 0, 150));
            overlayItems.add(new OverlayItem(config.healthBarPriority(), debugHealth));

            // Smited (DEBUG)
            BufferedImage debugSmitedSprite = spriteManager.getSprite(smitedSpriteId, 0);
            BufferedImage scaledDebugSmited = (debugSmitedSprite != null) ? scaleImage(debugSmitedSprite, 16, 16) : null;
            String debugSmitedText = "Smited: 0";
            BufferedImage compositeDebugSmited = createCompositeWeaponImage(scaledDebugSmited, debugSmitedText, Color.WHITE, graphics.getFont());
            ImageComponent debugSmitedComponent = new ImageComponent(compositeDebugSmited);
            debugSmitedComponent.setPreferredSize(new Dimension(compositeDebugSmited.getWidth(), compositeDebugSmited.getHeight()));
            overlayItems.add(new OverlayItem(config.smitedPriority(), debugSmitedComponent));

            // Attack (DEBUG) – lag composite for Attack-linjen
            String debugAttackStyle = "Melee"; // for debug
            BufferedImage compositeDebugAttack = createAttackComposite(graphics, debugAttackStyle);
            ImageComponent debugAttackComponent = new ImageComponent(compositeDebugAttack);
            debugAttackComponent.setPreferredSize(new Dimension(compositeDebugAttack.getWidth(), compositeDebugAttack.getHeight()));
            overlayItems.add(new OverlayItem(config.attackTypePriority(), debugAttackComponent));

            // CURRENT weapon (DEBUG)
            BufferedImage debugWeaponSprite = spriteManager.getSprite(currentWeaponSpriteId, 0);
            BufferedImage scaledDebugWeapon = (debugWeaponSprite != null) ? scaleImage(debugWeaponSprite, 16, 16) : null;
            String debugWeaponText = "1: " + trimText("Current weapon", 50);
            BufferedImage compositeDebugWeapon = createCompositeWeaponImage(scaledDebugWeapon, debugWeaponText, new Color(173,216,230), graphics.getFont());
            ImageComponent debugWeaponComponent = new ImageComponent(compositeDebugWeapon);
            debugWeaponComponent.setPreferredSize(new Dimension(compositeDebugWeapon.getWidth(), compositeDebugWeapon.getHeight()));
            overlayItems.add(new OverlayItem(config.weaponPriority(), debugWeaponComponent));

            // PREVIOUS weapon (DEBUG)
            String debugPrevText = "2: " + trimText("Previous weapon", 50);
            // Her bruker vi samme sprite som CURRENT (ID 168)
            BufferedImage compositeDebugPrevWeapon = createCompositeWeaponImage(scaledDebugWeapon, debugPrevText, new Color(173,216,230), graphics.getFont());
            ImageComponent debugPrevComponent = new ImageComponent(compositeDebugPrevWeapon);
            debugPrevComponent.setPreferredSize(new Dimension(compositeDebugPrevWeapon.getWidth(), compositeDebugPrevWeapon.getHeight()));
            overlayItems.add(new OverlayItem(config.weaponPriority() + 0.1, debugPrevComponent));

            // Risk (DEBUG)
            BufferedImage debugRiskSprite = spriteManager.getSprite(riskSpriteId, 0);
            BufferedImage scaledDebugRisk = (debugRiskSprite != null) ? scaleImage(debugRiskSprite, 16, 16) : null;
            String debugRiskText = "Risk: 0";
            BufferedImage compositeDebugRisk = createCompositeWeaponImage(scaledDebugRisk, debugRiskText, RISK_LABEL_COLOR, graphics.getFont());
            ImageComponent debugRiskComponent = new ImageComponent(compositeDebugRisk);
            debugRiskComponent.setPreferredSize(new Dimension(compositeDebugRisk.getWidth(), compositeDebugRisk.getHeight()));
            overlayItems.add(new OverlayItem(config.riskPriority(), debugRiskComponent));

            overlayItems.sort(Comparator.comparingDouble(OverlayItem::getPriority));
            for (OverlayItem item : overlayItems) {
                panelComponent.getChildren().add(item.getComponent());
            }
            return super.render(graphics);
        }

        // NORMAL rendering (mot aktiv opponent)
        final Actor opponent = plugin.getLastOpponent();
        if (opponent == null)
            return null;
        if (opponent.getName() != null && opponent.getHealthScale() > 0) {
            lastRatio = opponent.getHealthRatio();
            lastHealthScale = opponent.getHealthScale();
            opponentName = Text.removeTags(opponent.getName());
            lastMaxHealth = null;
            if (opponent instanceof NPC) {
                NPCComposition composition = ((NPC) opponent).getTransformedComposition();
                if (composition != null) {
                    String longName = composition.getStringValue(ParamID.NPC_HP_NAME);
                    if (!Strings.isNullOrEmpty(longName))
                        opponentName = longName;
                }
                lastMaxHealth = npcManager.getHealth(((NPC) opponent).getId());
            } else if (opponent instanceof Player) {
                final HiscoreResult hiscoreResult = hiscoreManager.lookupAsync(opponentName, plugin.getHiscoreEndpoint());
                if (hiscoreResult != null) {
                    final int hp = hiscoreResult.getSkill(HiscoreSkill.HITPOINTS).getLevel();
                    if (hp > 0)
                        lastMaxHealth = hp;
                }
            }
        }
        if (opponentName == null || hasHpHud(opponent) || !config.showOpponentHealthOverlay())
            return null;

        FontMetrics fm = graphics.getFontMetrics(graphics.getFont());
        int opponentWidth = fm.stringWidth(opponentName) + ComponentConstants.STANDARD_BORDER * 2;
        int panelWidth = Math.max(ComponentConstants.STANDARD_WIDTH, opponentWidth);
        panelComponent.setPreferredSize(new Dimension(panelWidth, 0));
        panelComponent.getChildren().add(TitleComponent.builder().text(opponentName).build());

        // Health Bar
        if (lastRatio >= 0 && lastHealthScale > 0) {
            ProgressBarComponent progressBarComponent = new ProgressBarComponent();
            progressBarComponent.setBackgroundColor(new Color(0, 0, 0, 150));
            final HitpointsDisplayStyle displayStyle = config.hitpointsDisplayStyle();
            double healthPercentage = 0;
            int currentHealthAbsolute = 0;
            if ((displayStyle == HitpointsDisplayStyle.HITPOINTS || displayStyle == HitpointsDisplayStyle.BOTH)
                    && lastMaxHealth != null) {
                int health = 0;
                if (lastRatio > 0) {
                    int minHealth = 1;
                    int maxHealth;
                    if (lastHealthScale > 1) {
                        if (lastRatio > 1) {
                            minHealth = (lastMaxHealth * (lastRatio - 1) + lastHealthScale - 2)
                                    / (lastHealthScale - 1);
                        }
                        maxHealth = (lastMaxHealth * lastRatio - 1) / (lastHealthScale - 1);
                        if (maxHealth > lastMaxHealth)
                            maxHealth = lastMaxHealth;
                    } else {
                        maxHealth = lastMaxHealth;
                    }
                    health = (minHealth + maxHealth + 1) / 2;
                }
                currentHealthAbsolute = health;
                progressBarComponent.setMaximum(lastMaxHealth);
                progressBarComponent.setValue(health);
                healthPercentage = (double) health / lastMaxHealth;
            } else {
                float floatRatio = lastRatio / (float) lastHealthScale;
                progressBarComponent.setValue(floatRatio * 100d);
                healthPercentage = lastRatio / (double) lastHealthScale;
            }
            Color finalColor = HP_GREEN;
            if (config.dynamicHealthColor()) {
                boolean belowRed, belowYellow;
                if (lastMaxHealth != null) {
                    belowRed = (config.redThresholdUnit() == KPOpponentInfoConfig.ThresholdUnit.HP)
                            ? currentHealthAbsolute < config.redThresholdValue()
                            : (healthPercentage * 100) < config.redThresholdValue();
                    belowYellow = (config.yellowThresholdUnit() == KPOpponentInfoConfig.ThresholdUnit.HP)
                            ? currentHealthAbsolute < config.yellowThresholdValue()
                            : (healthPercentage * 100) < config.yellowThresholdValue();
                } else {
                    belowRed = (healthPercentage * 100) < config.redThresholdValue();
                    belowYellow = (healthPercentage * 100) < config.yellowThresholdValue();
                }
                if (belowRed)
                    finalColor = HP_RED;
                else if (belowYellow)
                    finalColor = HP_YELLOW;
            }
            if (config.enableBlink()) {
                boolean blinkActive;
                if (lastMaxHealth != null) {
                    blinkActive = (config.blinkThresholdUnit() == KPOpponentInfoConfig.ThresholdUnit.HP)
                            ? currentHealthAbsolute < config.blinkThresholdValue()
                            : (healthPercentage * 100) < config.blinkThresholdValue();
                } else {
                    blinkActive = (healthPercentage * 100) < config.blinkThresholdValue();
                }
                if (blinkActive) {
                    long time = System.currentTimeMillis();
                    if ((time / 500) % 2 == 0)
                        finalColor = new Color(finalColor.getRed() / 2,
                                finalColor.getGreen() / 2,
                                finalColor.getBlue() / 2,
                                finalColor.getAlpha());
                }
            }
            progressBarComponent.setForegroundColor(finalColor);
            if ((config.hitpointsDisplayStyle() == HitpointsDisplayStyle.HITPOINTS ||
                    config.hitpointsDisplayStyle() == HitpointsDisplayStyle.BOTH)
                    && lastMaxHealth != null) {
                ProgressBarComponent.LabelDisplayMode mode = config.hitpointsDisplayStyle() == HitpointsDisplayStyle.BOTH
                        ? ProgressBarComponent.LabelDisplayMode.BOTH
                        : ProgressBarComponent.LabelDisplayMode.FULL;
                progressBarComponent.setLabelDisplayMode(mode);
            }
            overlayItems.add(new OverlayItem(config.healthBarPriority(), progressBarComponent));
            plugin.setLastDynamicColor(finalColor);
        }

        // Smited Prayer (NORMAL)
        if (config.showSmitedPrayer() && (plugin.getSmitedPrayer() > 0 || plugin.isSmiteActivated())) {
            BufferedImage smitedSprite = spriteManager.getSprite(smitedSpriteId, 0);
            BufferedImage scaledSmitedSprite = (smitedSprite != null) ? scaleImage(smitedSprite, 16, 16) : null;
            String smitedText = "Smited: " + plugin.getSmitedPrayer();
            BufferedImage compositeSmited = createCompositeWeaponImage(scaledSmitedSprite, smitedText, Color.WHITE, graphics.getFont());
            ImageComponent smitedComponent = new ImageComponent(compositeSmited);
            smitedComponent.setPreferredSize(new Dimension(compositeSmited.getWidth(), compositeSmited.getHeight()));
            overlayItems.add(new OverlayItem(config.smitedPriority(), smitedComponent));
        }

        // Attack Type (NORMAL)
        if (opponent instanceof Player && config.showAttackStyle()) {
            Player targetPlayer = (Player) opponent;
            String attackStyle = determineAttackStyle(targetPlayer);
            BufferedImage compositeAttack = createAttackComposite(graphics, attackStyle);
            ImageComponent attackComponent = new ImageComponent(compositeAttack);
            attackComponent.setPreferredSize(new Dimension(compositeAttack.getWidth(), compositeAttack.getHeight()));
            overlayItems.add(new OverlayItem(config.attackTypePriority(), attackComponent));
        }

        // Weapons (NORMAL)
        if (opponent instanceof Player) {
            Player targetPlayer = (Player) opponent;
            String currentWeapon = determineWeaponName(targetPlayer);
            if (plugin.getCurrentWeaponName() == null) {
                plugin.setCurrentWeaponName(currentWeapon);
            } else if (!plugin.getCurrentWeaponName().equals(currentWeapon)) {
                plugin.setLastWeaponName(plugin.getCurrentWeaponName());
                plugin.setCurrentWeaponName(currentWeapon);
            }
            KPOpponentInfoConfig.WeaponDisplayOption weaponOption = config.weaponDisplay();
            if (weaponOption != KPOpponentInfoConfig.WeaponDisplayOption.NONE) {
                // CURRENT weapon: bruk sprite 168 med etikett "1:"
                BufferedImage weaponSprite = spriteManager.getSprite(currentWeaponSpriteId, 0);
                BufferedImage scaledWeaponSprite = (weaponSprite != null) ? scaleImage(weaponSprite, 16, 16) : null;
                Font font = graphics.getFont();
                String currentText = "1: " + trimText(currentWeapon, 50);
                BufferedImage compositeWeapon = createCompositeWeaponImage(scaledWeaponSprite, currentText, new Color(173,216,230), font);
                ImageComponent weaponComponent = new ImageComponent(compositeWeapon);
                weaponComponent.setPreferredSize(new Dimension(compositeWeapon.getWidth(), compositeWeapon.getHeight()));
                overlayItems.add(new OverlayItem(config.weaponPriority(), weaponComponent));
                if (weaponOption == KPOpponentInfoConfig.WeaponDisplayOption.CURRENT_AND_LAST) {
                    String lastWeapon = plugin.getLastWeaponName();
                    if (lastWeapon != null && !lastWeapon.isEmpty()) {
                        // PREVIOUS weapon: bruk sprite 168 med etikett "2:"
                        BufferedImage prevWeaponSprite = spriteManager.getSprite(previousWeaponSpriteId, 0);
                        BufferedImage scaledPrevWeaponSprite = (prevWeaponSprite != null) ? scaleImage(prevWeaponSprite, 16, 16) : null;
                        String prevText = "2: " + trimText(lastWeapon, 50);
                        BufferedImage compositePrevWeapon = createCompositeWeaponImage(scaledPrevWeaponSprite, prevText, new Color(173,216,230), font);
                        ImageComponent prevWeaponComponent = new ImageComponent(compositePrevWeapon);
                        prevWeaponComponent.setPreferredSize(new Dimension(compositePrevWeapon.getWidth(), compositePrevWeapon.getHeight()));
                        overlayItems.add(new OverlayItem(config.weaponPriority() + 0.1, prevWeaponComponent));
                    }
                }
            }
        }

        // Risk (NORMAL)
        if (config.riskDisplayOption() == KPOpponentInfoConfig.RiskDisplayOption.OVERLAY ||
                config.riskDisplayOption() == KPOpponentInfoConfig.RiskDisplayOption.BOTH) {
            long riskValue = computeRisk((Player) opponent);
            Color riskColor = Color.WHITE;
            if (riskValue > 0 && config.enableColorRisk()) {
                if (riskValue < config.lowRiskThreshold())
                    riskColor = Color.WHITE;
                else if (riskValue < config.highRiskThreshold())
                    riskColor = config.mediumRiskColor();
                else if (riskValue < config.insaneRiskThreshold())
                    riskColor = config.highRiskColor();
                else
                    riskColor = config.insaneRiskColor();
            }
            String riskText = "Risk: " + (riskValue > 0 ? formatWealth(riskValue) : "0");
            BufferedImage riskSprite = spriteManager.getSprite(riskSpriteId, 0);
            BufferedImage scaledRiskSprite = (riskSprite != null) ? scaleImage(riskSprite, 16, 16) : null;
            BufferedImage compositeRisk = createCompositeWeaponImage(scaledRiskSprite, riskText, riskColor, graphics.getFont());
            ImageComponent riskComponent = new ImageComponent(compositeRisk);
            riskComponent.setPreferredSize(new Dimension(compositeRisk.getWidth(), compositeRisk.getHeight()));
            overlayItems.add(new OverlayItem(config.riskPriority(), riskComponent));
        }

        overlayItems.sort(Comparator.comparingDouble(OverlayItem::getPriority));
        for (OverlayItem item : overlayItems) {
            panelComponent.getChildren().add(item.getComponent());
        }
        return super.render(graphics);
    }

    private String trimText(String text, int maxLength) {
        if (text == null)
            return "";
        if (text.length() > maxLength)
            return text.substring(0, maxLength - 3) + "...";
        return text;
    }

    private BufferedImage scaleImage(BufferedImage src, int newWidth, int newHeight) {
        BufferedImage scaled = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = scaled.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(src, 0, 0, newWidth, newHeight, null);
        g2d.dispose();
        return scaled;
    }

    private String determineAttackStyle(Player player) {
        int weaponId = player.getPlayerComposition().getEquipmentId(KitType.WEAPON);
        if (weaponId == -1)
            return "Melee";
        ItemComposition comp = itemManager.getItemComposition(weaponId);
        if (comp == null)
            return "Unknown";
        String name = comp.getName().toLowerCase();
        if (name.contains("knife") || name.contains("bow") || name.contains("dart") ||
                name.contains("blowpipe") || name.contains("eclipse") ||
                name.contains("throwing") || name.contains("thrown") || name.contains("toktz-xil-ul"))
            return "Ranged";
        if (name.contains("staff") || name.contains("wand") ||
                name.contains("crozier") || name.contains("salamander"))
            return "Magic";
        if (name.contains("tzhaar") || name.contains("maul") || name.contains("axe") ||
                name.contains("bulwark") || name.contains("banners") || name.contains("machete") ||
                name.contains("mjolnir") || name.contains("scythe") || name.contains("sickle") ||
                name.contains("cutlass") || name.contains("hammer") || name.contains("claws") ||
                name.contains("sword") || name.contains("scimitar") || name.contains("halberd") ||
                name.contains("spear") || name.contains("whip") || name.contains("dagger") ||
                name.contains("hasta") || name.contains("blade") || name.contains("abyssal"))
            return "Melee";
        return "Unknown";
    }

    private String determineWeaponName(Player player) {
        int weaponId = player.getPlayerComposition().getEquipmentId(KitType.WEAPON);
        if (weaponId == -1)
            return "None";
        ItemComposition comp = itemManager.getItemComposition(weaponId);
        if (comp == null)
            return "Unknown";
        return comp.getName();
    }

    private boolean hasHpHud(Actor opponent) {
        boolean settingEnabled = client.getVarbitValue(Varbits.BOSS_HEALTH_OVERLAY) == 0;
        if (settingEnabled && opponent instanceof NPC) {
            int opponentId = client.getVarpValue(VarPlayer.HP_HUD_NPC_ID);
            NPC npc = (NPC) opponent;
            return opponentId != -1 && npc.getComposition() != null &&
                    opponentId == npc.getComposition().getId();
        }
        return false;
    }

    private long computeRisk(Player opponent) {
        long totalWealth = 0;
        for (KitType kitType : KitType.values()) {
            int itemId = opponent.getPlayerComposition().getEquipmentId(kitType);
            if (itemId != -1) {
                long price = itemManager.getItemPrice(itemId);
                totalWealth += price;
            }
        }
        return totalWealth;
    }

    private static String formatWealth(long value) {
        if (value < 1000)
            return Long.toString(value);
        else if (value < 1000000) {
            long thousands = value / 1000;
            return thousands + "K";
        } else {
            double millions = value / 1000000.0;
            String formatted = String.format("%.1f", millions);
            formatted = formatted.replace('.', ',');
            return formatted + "M";
        }
    }
}
