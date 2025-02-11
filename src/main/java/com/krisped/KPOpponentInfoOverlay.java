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

public class KPOpponentInfoOverlay extends OverlayPanel {

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

    // Sprite-ID-er
    private final int currentWeaponSpriteId = 168;    // For CURRENT våpen
    private final int previousWeaponSpriteId = 168;     // For PREVIOUS våpen
    private final int smitedSpriteId = 132;             // For Smite
    private final int riskSpriteId = 523;               // For Risk

    // For Attack/Style-linjen:
    private final int attackLabelSpriteId = 237;        // For label "Style:" (brukes sammen med attack style)
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
     * Lager et composite-bilde for Attack/Style-linjen.
     * Først lages et bilde for "Style:" med sprite 237, deretter et bilde for angrepsstilen med riktig ikon
     * (129 for Melee, 128 for Ranged, 127 for Magic) foran stilen.
     * Til slutt kombineres de to bildene med et gap.
     */
    private BufferedImage createAttackComposite(Graphics2D graphics, String attackStyle) {
        Font font = graphics.getFont();
        // Composite for "Style:"-delen med sprite 237
        BufferedImage attackLabelSprite = spriteManager.getSprite(attackLabelSpriteId, 0);
        BufferedImage scaledAttackLabel = (attackLabelSprite != null) ? scaleImage(attackLabelSprite, 16, 16) : null;
        BufferedImage compositeLabel = createCompositeWeaponImage(scaledAttackLabel, "Style:", new Color(173,216,230), font);

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

        // Kombiner label og stil med et gap
        return concatImages(compositeLabel, compositeStyle, 5);
    }

    /**
     * HorizontalWeaponComponent lager et composite-bilde for våpenlinjen med sprite og tekst.
     * Spritet erstatter etiketten ("1:" eller "2:").
     */
    private static class HorizontalWeaponComponent implements LayoutableRenderableEntity {
        private final BufferedImage icon;
        private final String weaponText;
        private final Color textColor;
        private Dimension preferredSize = new Dimension(0, 0);
        private Point preferredLocation = new Point(0, 0);

        public HorizontalWeaponComponent(String weaponText, BufferedImage icon, Color textColor) {
            this.weaponText = weaponText;
            this.icon = icon;
            this.textColor = textColor;
        }

        public Dimension render(Graphics2D graphics) {
            FontMetrics fm = graphics.getFontMetrics();
            int iconWidth = (icon != null) ? icon.getWidth() : 0;
            int gap = (icon != null) ? 5 : 0;
            int textWidth = fm.stringWidth(weaponText);
            int height = Math.max(fm.getHeight(), (icon != null) ? icon.getHeight() : 0);

            if (icon != null) {
                graphics.drawImage(icon, 0, 0, null);
            }
            graphics.setColor(textColor);
            graphics.drawString(weaponText, iconWidth + gap, fm.getAscent());

            int totalWidth = iconWidth + gap + textWidth;
            preferredSize = new Dimension(totalWidth, height);
            return preferredSize;
        }

        public void setPreferredSize(Dimension d) {
            this.preferredSize = d;
        }

        public Dimension getPreferredSize() {
            return preferredSize;
        }

        public void setPreferredLocation(Point p) {
            this.preferredLocation = p;
        }

        public Point getPreferredLocation() {
            return preferredLocation;
        }

        public Rectangle getBounds() {
            return new Rectangle(preferredLocation, preferredSize);
        }
    }

    // Konfigurasjonsvalg for om overlayet skal vise ikoner eller tekst.
    // Dette forutsetter at KPOpponentInfoConfig har et enumfelt "overlayIcons" med verdiene ICONS og TEXT.
    // For eksempel: config.overlayIcons() == KPOpponentInfoConfig.OverlayIcons.ICONS
    // (Se config-filen for definisjon.)

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
        // Bestem om vi skal bruke ikoner (composite bilder) eller ren tekst
        boolean useIcons = (config.overlayIcons() == KPOpponentInfoConfig.OverlayIcons.ICONS);
        List<OverlayItem> overlayItems = new ArrayList<>();
        panelComponent.getChildren().clear();

        // DEBUG-modus
        if (config.debugOverlay() && plugin.getLastOpponent() == null) {
            panelComponent.getChildren().add(TitleComponent.builder().text("Debug").build());

            // Health Bar
            ProgressBarComponent debugHealth = new ProgressBarComponent();
            debugHealth.setMaximum(100);
            debugHealth.setValue(100);
            debugHealth.setForegroundColor(HP_GREEN);
            debugHealth.setBackgroundColor(new Color(0, 0, 0, 150));
            overlayItems.add(new OverlayItem(config.healthBarPriority(), debugHealth));

            // Smited
            if (useIcons) {
                BufferedImage debugSmitedSprite = spriteManager.getSprite(smitedSpriteId, 0);
                BufferedImage scaledDebugSmited = (debugSmitedSprite != null) ? scaleImage(debugSmitedSprite, 16, 16) : null;
                String debugSmitedText = "Smited: 0";
                BufferedImage compositeDebugSmited = createCompositeWeaponImage(scaledDebugSmited, debugSmitedText, Color.WHITE, graphics.getFont());
                ImageComponent debugSmitedComponent = new ImageComponent(compositeDebugSmited);
                debugSmitedComponent.setPreferredSize(new Dimension(compositeDebugSmited.getWidth(), compositeDebugSmited.getHeight()));
                overlayItems.add(new OverlayItem(config.smitedPriority(), debugSmitedComponent));
            } else {
                overlayItems.add(new OverlayItem(config.smitedPriority(),
                        LineComponent.builder().left("Smited:").right("0").leftColor(Color.WHITE).rightColor(Color.WHITE).build()));
            }

            // Attack / Style
            if (useIcons) {
                String debugAttackStyle = "Melee"; // for debug
                BufferedImage compositeDebugAttack = createAttackComposite(graphics, debugAttackStyle);
                ImageComponent debugAttackComponent = new ImageComponent(compositeDebugAttack);
                debugAttackComponent.setPreferredSize(new Dimension(compositeDebugAttack.getWidth(), compositeDebugAttack.getHeight()));
                overlayItems.add(new OverlayItem(config.attackTypePriority(), debugAttackComponent));
            } else {
                overlayItems.add(new OverlayItem(config.attackTypePriority(),
                        LineComponent.builder().left("Style:").right("Melee").leftColor(new Color(173,216,230)).rightColor(new Color(173,216,230)).build()));
            }

            // CURRENT weapon
            if (useIcons) {
                BufferedImage debugWeaponSprite = spriteManager.getSprite(currentWeaponSpriteId, 0);
                BufferedImage scaledDebugWeapon = (debugWeaponSprite != null) ? scaleImage(debugWeaponSprite, 16, 16) : null;
                String debugWeaponText = "1: " + trimText("Current weapon", 50);
                BufferedImage compositeDebugWeapon = createCompositeWeaponImage(scaledDebugWeapon, debugWeaponText, new Color(173,216,230), graphics.getFont());
                ImageComponent debugWeaponComponent = new ImageComponent(compositeDebugWeapon);
                debugWeaponComponent.setPreferredSize(new Dimension(compositeDebugWeapon.getWidth(), compositeDebugWeapon.getHeight()));
                overlayItems.add(new OverlayItem(config.weaponPriority(), debugWeaponComponent));
            } else {
                overlayItems.add(new OverlayItem(config.weaponPriority(),
                        LineComponent.builder().left("1:").right(trimText("Current weapon", 50))
                                .leftColor(new Color(173,216,230)).rightColor(new Color(173,216,230)).build()));
            }

            // PREVIOUS weapon (DEBUG) – valgfritt
            if (useIcons) {
                BufferedImage debugWeaponSprite = spriteManager.getSprite(currentWeaponSpriteId, 0);
                BufferedImage scaledDebugWeapon = (debugWeaponSprite != null) ? scaleImage(debugWeaponSprite, 16, 16) : null;
                String debugPrevText = "2: " + trimText("Previous weapon", 50);
                BufferedImage compositeDebugPrevWeapon = createCompositeWeaponImage(scaledDebugWeapon, debugPrevText, new Color(173,216,230), graphics.getFont());
                ImageComponent debugPrevComponent = new ImageComponent(compositeDebugPrevWeapon);
                debugPrevComponent.setPreferredSize(new Dimension(compositeDebugPrevWeapon.getWidth(), compositeDebugPrevWeapon.getHeight()));
                overlayItems.add(new OverlayItem(config.weaponPriority() + 0.1, debugPrevComponent));
            } else {
                overlayItems.add(new OverlayItem(config.weaponPriority() + 0.1,
                        LineComponent.builder().left("2:").right(trimText("Previous weapon", 50))
                                .leftColor(new Color(173,216,230)).rightColor(new Color(173,216,230)).build()));
            }

            // Risk
            if (useIcons) {
                BufferedImage debugRiskSprite = spriteManager.getSprite(riskSpriteId, 0);
                BufferedImage scaledDebugRisk = (debugRiskSprite != null) ? scaleImage(debugRiskSprite, 16, 16) : null;
                String debugRiskText = "Risk: 0";
                BufferedImage compositeDebugRisk = createCompositeWeaponImage(scaledDebugRisk, debugRiskText, RISK_LABEL_COLOR, graphics.getFont());
                ImageComponent debugRiskComponent = new ImageComponent(compositeDebugRisk);
                debugRiskComponent.setPreferredSize(new Dimension(compositeDebugRisk.getWidth(), compositeDebugRisk.getHeight()));
                overlayItems.add(new OverlayItem(config.riskPriority(), debugRiskComponent));
            } else {
                overlayItems.add(new OverlayItem(config.riskPriority(),
                        LineComponent.builder().left("Risk:").right("0").leftColor(RISK_LABEL_COLOR).rightColor(Color.WHITE).build()));
            }

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

        // Smited (NORMAL)
        if (config.showSmitedPrayer() && (plugin.getSmitedPrayer() > 0 || plugin.isSmiteActivated())) {
            if (useIcons) {
                BufferedImage smitedSprite = spriteManager.getSprite(smitedSpriteId, 0);
                BufferedImage scaledSmitedSprite = (smitedSprite != null) ? scaleImage(smitedSprite, 16, 16) : null;
                String smitedText = "Smited: " + plugin.getSmitedPrayer();
                BufferedImage compositeSmited = createCompositeWeaponImage(scaledSmitedSprite, smitedText, Color.WHITE, graphics.getFont());
                ImageComponent smitedComponent = new ImageComponent(compositeSmited);
                smitedComponent.setPreferredSize(new Dimension(compositeSmited.getWidth(), compositeSmited.getHeight()));
                overlayItems.add(new OverlayItem(config.smitedPriority(), smitedComponent));
            } else {
                overlayItems.add(new OverlayItem(config.smitedPriority(),
                        LineComponent.builder().left("Smited:").right("" + plugin.getSmitedPrayer())
                                .leftColor(Color.WHITE).rightColor(Color.WHITE).build()));
            }
        }

        // Attack / Style (NORMAL)
        if (opponent instanceof Player && config.showAttackStyle()) {
            Player targetPlayer = (Player) opponent;
            String attackStyle = determineAttackStyle(targetPlayer);
            if (useIcons) {
                BufferedImage compositeAttack = createAttackComposite(graphics, attackStyle);
                ImageComponent attackComponent = new ImageComponent(compositeAttack);
                attackComponent.setPreferredSize(new Dimension(compositeAttack.getWidth(), compositeAttack.getHeight()));
                overlayItems.add(new OverlayItem(config.attackTypePriority(), attackComponent));
            } else {
                overlayItems.add(new OverlayItem(config.attackTypePriority(),
                        LineComponent.builder().left("Style:").right(attackStyle)
                                .leftColor(new Color(173,216,230)).rightColor(new Color(173,216,230)).build()));
            }
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
                if (useIcons) {
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
                } else {
                    // Tekstmodus for våpen
                    overlayItems.add(new OverlayItem(config.weaponPriority(),
                            LineComponent.builder().left("1:").right(trimText(currentWeapon, 50))
                                    .leftColor(new Color(173,216,230)).rightColor(new Color(173,216,230)).build()));
                    if (weaponOption == KPOpponentInfoConfig.WeaponDisplayOption.CURRENT_AND_LAST) {
                        String lastWeapon = plugin.getLastWeaponName();
                        if (lastWeapon != null && !lastWeapon.isEmpty()) {
                            overlayItems.add(new OverlayItem(config.weaponPriority() + 0.1,
                                    LineComponent.builder().left("2:").right(trimText(lastWeapon, 50))
                                            .leftColor(new Color(173,216,230)).rightColor(new Color(173,216,230)).build()));
                        }
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
            if (useIcons) {
                BufferedImage riskSprite = spriteManager.getSprite(riskSpriteId, 0);
                BufferedImage scaledRiskSprite = (riskSprite != null) ? scaleImage(riskSprite, 16, 16) : null;
                String riskText = "Risk: " + (riskValue > 0 ? formatWealth(riskValue) : "0");
                BufferedImage compositeRisk = createCompositeWeaponImage(scaledRiskSprite, riskText, riskColor, graphics.getFont());
                ImageComponent riskComponent = new ImageComponent(compositeRisk);
                riskComponent.setPreferredSize(new Dimension(compositeRisk.getWidth(), compositeRisk.getHeight()));
                overlayItems.add(new OverlayItem(config.riskPriority(), riskComponent));
            } else {
                overlayItems.add(new OverlayItem(config.riskPriority(),
                        LineComponent.builder().left("Risk:").right(riskValue > 0 ? formatWealth(riskValue) : "0")
                                .leftColor(RISK_LABEL_COLOR).rightColor(Color.WHITE).build()));
            }
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
