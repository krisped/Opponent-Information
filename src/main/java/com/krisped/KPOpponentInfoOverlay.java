package com.krisped;

import net.runelite.api.*;
import net.runelite.api.kit.KitType;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.NPCManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.hiscore.HiscoreManager;
import net.runelite.client.hiscore.HiscoreResult;
import net.runelite.client.hiscore.HiscoreSkill;
import net.runelite.client.ui.overlay.*;
import net.runelite.client.ui.overlay.components.*;
import net.runelite.client.util.Text;

import javax.inject.Inject;
import java.awt.*;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@SuppressWarnings("OptionalGetWithoutIsPresent")
public class KPOpponentInfoOverlay extends OverlayPanel {

    // --- FARGER ---
    private static final Color HP_GREEN = new Color(0, 146, 54, 230);
    private static final Color HP_RED = new Color(255, 0, 0, 230);
    private static final Color HP_YELLOW = new Color(255, 255, 0, 230);
    private static final Color RISK_LABEL_COLOR = Color.decode("#FFD700");

    // --- IKON-IDer for "Style:" og angrepstyper ---
    private static final int STYLE_LABEL_ICON_ID = 168;   // Ikon foran "Style:"
    private static final int STYLE_MELEE_ICON_ID = 129;
    private static final int STYLE_RANGED_ICON_ID = 128;
    private static final int STYLE_MAGIC_ICON_ID = 127;

    // --- Våpenikoner ---
    private static final int CURRENT_WEAPON_ICON_ID = 197;
    private static final int PREVIOUS_WEAPON_ICON_ID = 197;

    // --- Andre ikoner ---
    private static final int SMITED_ICON_ID = 132;
    private static final int RISK_ICON_ID = 523;

    // --- Fast bredde for linjer i IKON-modus ---
    private static final int ICON_LINE_WIDTH = ComponentConstants.STANDARD_WIDTH;

    private final Client client;
    private final KPOpponentInfoPlugin plugin; // Vår plugin med nødvendige metoder
    private final KPOpponentInfoConfig config;
    private final HiscoreManager hiscoreManager;
    private final NPCManager npcManager;

    @Inject
    private ItemManager itemManager;
    @Inject
    private SpriteManager spriteManager;

    // Helseinformasjon
    private Integer lastMaxHealth;
    private int lastRatio = 0;
    private int lastHealthScale = 0;
    private String opponentName;

    @Inject
    public KPOpponentInfoOverlay(
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
        setPriority(OverlayPriority.HIGH);

        panelComponent.setBorder(new Rectangle(2, 2, 2, 2));
        panelComponent.setGap(new Point(0, 2));

        addMenuEntry(
                MenuAction.RUNELITE_OVERLAY_CONFIG,
                OverlayManager.OPTION_CONFIGURE,
                "[KP] Opponent Information overlay"
        );
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        Font font = graphics.getFont();
        boolean useIcons = (config.overlayIcons() == KPOpponentInfoConfig.OverlayIcons.ICONS);
        panelComponent.getChildren().clear();
        List<OverlayItem> overlayItems = new ArrayList<>();

        // -----------------------
        // 1) DEBUG OVERLAY (hvis ingen motstander)
        // -----------------------
        if (config.debugOverlay() && plugin.getLastOpponent() == null) {
            panelComponent.getChildren().add(TitleComponent.builder().text("Debug").build());

            // (a) Health Bar (debug = 100%)
            ProgressBarComponent dbgHealth = new ProgressBarComponent();
            dbgHealth.setMaximum(100);
            dbgHealth.setValue(100);
            dbgHealth.setForegroundColor(HP_GREEN);
            dbgHealth.setBackgroundColor(new Color(0, 0, 0, 150));
            overlayItems.add(new OverlayItem(config.healthBarPriority(), dbgHealth));

            // (b) Smited
            if (useIcons) {
                BufferedImage icon = spriteManager.getSprite(SMITED_ICON_ID, 0);
                if (icon != null) {
                    icon = scaleImage(icon, 16, 16);
                }
                // Debug: vis "0" eller eventuelt plugin.getSmitedPrayer()
                BufferedImage lineImg = createCompositeIconLeftRight(
                        icon,
                        "Smited:",
                        "0",
                        Color.WHITE,
                        Color.WHITE,
                        font,
                        ICON_LINE_WIDTH
                );
                overlayItems.add(new OverlayItem(config.smitedPriority(), new ImageComponent(lineImg)));
            } else {
                overlayItems.add(new OverlayItem(config.smitedPriority(),
                        LineComponent.builder()
                                .left("Smited:")
                                .right("0")
                                .leftColor(Color.WHITE)
                                .rightColor(Color.WHITE)
                                .build()));
            }

            // (c) Attack Style – Én linje i ikonmodus (debug: "Melee")
            if (useIcons) {
                BufferedImage styleLine = createSingleLineStyle(font, "Melee");
                overlayItems.add(new OverlayItem(config.attackTypePriority(), new ImageComponent(styleLine)));
            } else {
                overlayItems.add(new OverlayItem(config.attackTypePriority(),
                        LineComponent.builder()
                                .left("Style:")
                                .right("Melee")
                                .leftColor(new Color(173, 216, 230))
                                .rightColor(new Color(173, 216, 230))
                                .build()));
            }

            // (d) Current Weapon – IKON-modus: vis våpennavn høyrejustert (ingen "1:")
            if (useIcons) {
                BufferedImage wpnIcon = spriteManager.getSprite(CURRENT_WEAPON_ICON_ID, 0);
                if (wpnIcon != null) {
                    wpnIcon = scaleImage(wpnIcon, 16, 16);
                }
                BufferedImage lineImg = createCompositeIconLeftRight(
                        wpnIcon,
                        "", // Tom venstre
                        "Current weapon",
                        new Color(173, 216, 230),
                        new Color(173, 216, 230),
                        font,
                        ICON_LINE_WIDTH
                );
                overlayItems.add(new OverlayItem(config.weaponPriority(), new ImageComponent(lineImg)));
            } else {
                overlayItems.add(new OverlayItem(config.weaponPriority(),
                        LineComponent.builder()
                                .left("1:")
                                .right("Current weapon")
                                .leftColor(new Color(173, 216, 230))
                                .rightColor(new Color(173, 216, 230))
                                .build()));
            }

            // (e) Previous Weapon – IKON-modus: vis våpennavn høyrejustert (ingen "2:")
            if (useIcons) {
                BufferedImage prevIcon = spriteManager.getSprite(PREVIOUS_WEAPON_ICON_ID, 0);
                if (prevIcon != null) {
                    prevIcon = scaleImage(prevIcon, 16, 16);
                }
                BufferedImage lineImg = createCompositeIconLeftRight(
                        prevIcon,
                        "", // Tom venstre
                        "Previous weapon",
                        new Color(173, 216, 230),
                        new Color(173, 216, 230),
                        font,
                        ICON_LINE_WIDTH
                );
                overlayItems.add(new OverlayItem(config.weaponPriority() + 0.1, new ImageComponent(lineImg)));
            } else {
                overlayItems.add(new OverlayItem(config.weaponPriority() + 0.1,
                        LineComponent.builder()
                                .left("2:")
                                .right("Previous weapon")
                                .leftColor(new Color(173, 216, 230))
                                .rightColor(new Color(173, 216, 230))
                                .build()));
            }

            // (f) Risk
            if (useIcons) {
                BufferedImage rIcon = spriteManager.getSprite(RISK_ICON_ID, 0);
                if (rIcon != null) {
                    rIcon = scaleImage(rIcon, 16, 16);
                }
                BufferedImage lineImg = createCompositeIconLeftRight(
                        rIcon,
                        "Risk:",
                        "0",
                        RISK_LABEL_COLOR,
                        Color.WHITE,
                        font,
                        ICON_LINE_WIDTH
                );
                overlayItems.add(new OverlayItem(config.riskPriority(), new ImageComponent(lineImg)));
            } else {
                overlayItems.add(new OverlayItem(config.riskPriority(),
                        LineComponent.builder()
                                .left("Risk:")
                                .right("0")
                                .leftColor(RISK_LABEL_COLOR)
                                .rightColor(Color.WHITE)
                                .build()));
            }

            overlayItems.sort(Comparator.comparingDouble(OverlayItem::getPriority));
            for (OverlayItem item : overlayItems) {
                panelComponent.getChildren().add(item.getComponent());
            }
            return super.render(graphics);
        }

        // -----------------------
        // 2) NORMAL RENDERING
        // -----------------------
        final Actor opponent = plugin.getLastOpponent();
        if (opponent == null) {
            return null;
        }

        // Oppdater HP
        if (opponent.getName() != null && opponent.getHealthScale() > 0) {
            lastRatio = opponent.getHealthRatio();
            lastHealthScale = opponent.getHealthScale();
            opponentName = Text.removeTags(opponent.getName());
            lastMaxHealth = null;
            if (opponent instanceof NPC) {
                NPC npc = (NPC) opponent;
                lastMaxHealth = npcManager.getHealth(npc.getId());
            } else if (opponent instanceof Player) {
                Player pl = (Player) opponent;
                HiscoreResult hiscore = hiscoreManager.lookupAsync(opponentName, plugin.getHiscoreEndpoint());
                if (hiscore != null) {
                    int hp = hiscore.getSkill(HiscoreSkill.HITPOINTS).getLevel();
                    if (hp > 0) {
                        lastMaxHealth = hp;
                    }
                }
            }
        }
        if (opponentName == null || !config.showOpponentHealthOverlay() || hasHpHud(opponent)) {
            return null;
        }

        panelComponent.setPreferredSize(new Dimension(ComponentConstants.STANDARD_WIDTH, 0));
        panelComponent.getChildren().add(TitleComponent.builder().text(opponentName).build());

        List<OverlayItem> normalItems = new ArrayList<>();

        // (A) HP-bar
        if (lastRatio >= 0 && lastHealthScale > 0) {
            ProgressBarComponent hpBar = new ProgressBarComponent();
            hpBar.setBackgroundColor(new Color(0, 0, 0, 150));
            double healthPerc = 0;
            int hpAbs = 0;
            if ((config.hitpointsDisplayStyle() == HitpointsDisplayStyle.HITPOINTS ||
                    config.hitpointsDisplayStyle() == HitpointsDisplayStyle.BOTH) && lastMaxHealth != null) {
                int health = 0;
                if (lastRatio > 0) {
                    int minHealth = 1;
                    int maxHealth;
                    if (lastHealthScale > 1) {
                        if (lastRatio > 1) {
                            minHealth = (lastMaxHealth * (lastRatio - 1) + lastHealthScale - 2) / (lastHealthScale - 1);
                        }
                        maxHealth = (lastMaxHealth * lastRatio - 1) / (lastHealthScale - 1);
                        if (maxHealth > lastMaxHealth) {
                            maxHealth = lastMaxHealth;
                        }
                    } else {
                        maxHealth = lastMaxHealth;
                    }
                    health = (minHealth + maxHealth + 1) / 2;
                }
                hpAbs = health;
                hpBar.setMaximum(lastMaxHealth);
                hpBar.setValue(health);
                healthPerc = (double) health / lastMaxHealth;
            } else {
                float ratioF = lastRatio / (float) lastHealthScale;
                hpBar.setValue(ratioF * 100f);
                healthPerc = ratioF;
            }
            Color finalColor = HP_GREEN;
            if (config.dynamicHealthColor()) {
                boolean belowRed, belowYellow;
                if (lastMaxHealth != null) {
                    belowRed = isBelowHpOrPercent(hpAbs, healthPerc, config.redThresholdValue(), config.redThresholdUnit());
                    belowYellow = isBelowHpOrPercent(hpAbs, healthPerc, config.yellowThresholdValue(), config.yellowThresholdUnit());
                } else {
                    belowRed = (healthPerc * 100) < config.redThresholdValue();
                    belowYellow = (healthPerc * 100) < config.yellowThresholdValue();
                }
                if (belowRed) finalColor = HP_RED;
                else if (belowYellow) finalColor = HP_YELLOW;
            }
            if (config.enableBlink()) {
                boolean blinkActive;
                if (lastMaxHealth != null) {
                    blinkActive = isBelowHpOrPercent(hpAbs, healthPerc, config.blinkThresholdValue(), config.blinkThresholdUnit());
                } else {
                    blinkActive = (healthPerc * 100) < config.blinkThresholdValue();
                }
                if (blinkActive) {
                    long now = System.currentTimeMillis();
                    if ((now / 500) % 2 == 0) {
                        finalColor = new Color(finalColor.getRed() / 2, finalColor.getGreen() / 2,
                                finalColor.getBlue() / 2, finalColor.getAlpha());
                    }
                }
            }
            hpBar.setForegroundColor(finalColor);
            if ((config.hitpointsDisplayStyle() == HitpointsDisplayStyle.HITPOINTS ||
                    config.hitpointsDisplayStyle() == HitpointsDisplayStyle.BOTH) && lastMaxHealth != null) {
                ProgressBarComponent.LabelDisplayMode mode =
                        (config.hitpointsDisplayStyle() == HitpointsDisplayStyle.BOTH)
                                ? ProgressBarComponent.LabelDisplayMode.BOTH
                                : ProgressBarComponent.LabelDisplayMode.FULL;
                hpBar.setLabelDisplayMode(mode);
            }
            normalItems.add(new OverlayItem(config.healthBarPriority(), hpBar));
        }

        // (B) Smited
        if (config.showSmitedPrayer() && (plugin.getSmitedPrayer() > 0 || plugin.isSmiteActivated())) {
            if (useIcons) {
                BufferedImage icon = spriteManager.getSprite(SMITED_ICON_ID, 0);
                if (icon != null) {
                    icon = scaleImage(icon, 16, 16);
                }
                // Her bruker vi plugin.getSmitedPrayer() som høyre tekst
                String rightText = String.valueOf(plugin.getSmitedPrayer());
                BufferedImage lineImg = createCompositeIconLeftRight(
                        icon,
                        "Smited:",
                        rightText,
                        Color.WHITE,
                        Color.WHITE,
                        font,
                        ICON_LINE_WIDTH
                );
                normalItems.add(new OverlayItem(config.smitedPriority(), new ImageComponent(lineImg)));
            } else {
                normalItems.add(new OverlayItem(config.smitedPriority(),
                        LineComponent.builder()
                                .left("Smited:")
                                .right(String.valueOf(plugin.getSmitedPrayer()))
                                .leftColor(Color.WHITE)
                                .rightColor(Color.WHITE)
                                .build()));
            }
        }

        // (C) Attack style – Én linje i ikonmodus for "Style: <stiling>"
        if (opponent instanceof Player && config.showAttackStyle()) {
            Player p = (Player) opponent;
            String style = determineAttackStyle(p);
            if (useIcons) {
                BufferedImage styleLine = createSingleLineStyle(font, style);
                normalItems.add(new OverlayItem(config.attackTypePriority(), new ImageComponent(styleLine)));
            } else {
                normalItems.add(new OverlayItem(config.attackTypePriority(),
                        LineComponent.builder()
                                .left("Style:")
                                .right(style)
                                .leftColor(new Color(173, 216, 230))
                                .rightColor(new Color(173, 216, 230))
                                .build()));
            }
        }

        // (D) Våpen – i ikonmodus: vis våpennavn som høyrejustert (ingen "1:" eller "2:")
        if (opponent instanceof Player) {
            Player p = (Player) opponent;
            String currentWeapon = determineWeaponName(p);
            if (plugin.getCurrentWeaponName() == null) {
                plugin.setCurrentWeaponName(currentWeapon);
            } else if (!plugin.getCurrentWeaponName().equals(currentWeapon)) {
                plugin.setLastWeaponName(plugin.getCurrentWeaponName());
                plugin.setCurrentWeaponName(currentWeapon);
            }
            KPOpponentInfoConfig.WeaponDisplayOption wOpt = config.weaponDisplay();
            if (wOpt != KPOpponentInfoConfig.WeaponDisplayOption.NONE) {
                String trimmedCurr = trimText(plugin.getCurrentWeaponName(), 50);
                if (useIcons) {
                    BufferedImage cIcon = spriteManager.getSprite(CURRENT_WEAPON_ICON_ID, 0);
                    if (cIcon != null) {
                        cIcon = scaleImage(cIcon, 16, 16);
                    }
                    // Tom venstre, våpennavn som høyrejustert tekst
                    BufferedImage lineImg = createCompositeIconLeftRight(
                            cIcon,
                            "",
                            trimmedCurr,
                            new Color(173, 216, 230),
                            new Color(173, 216, 230),
                            font,
                            ICON_LINE_WIDTH
                    );
                    normalItems.add(new OverlayItem(config.weaponPriority(), new ImageComponent(lineImg)));
                } else {
                    normalItems.add(new OverlayItem(config.weaponPriority(),
                            LineComponent.builder()
                                    .left("1:")
                                    .right(trimmedCurr)
                                    .leftColor(new Color(173, 216, 230))
                                    .rightColor(new Color(173, 216, 230))
                                    .build()));
                }

                if (wOpt == KPOpponentInfoConfig.WeaponDisplayOption.CURRENT_AND_LAST) {
                    String lastWpn = plugin.getLastWeaponName();
                    if (lastWpn != null && !lastWpn.isEmpty()) {
                        String trimmedLast = trimText(lastWpn, 50);
                        if (useIcons) {
                            BufferedImage pIcon = spriteManager.getSprite(PREVIOUS_WEAPON_ICON_ID, 0);
                            if (pIcon != null) {
                                pIcon = scaleImage(pIcon, 16, 16);
                            }
                            BufferedImage lineImg = createCompositeIconLeftRight(
                                    pIcon,
                                    "",
                                    trimmedLast,
                                    new Color(173, 216, 230),
                                    new Color(173, 216, 230),
                                    font,
                                    ICON_LINE_WIDTH
                            );
                            normalItems.add(new OverlayItem(config.weaponPriority() + 0.1, new ImageComponent(lineImg)));
                        } else {
                            normalItems.add(new OverlayItem(config.weaponPriority() + 0.1,
                                    LineComponent.builder()
                                            .left("2:")
                                            .right(trimmedLast)
                                            .leftColor(new Color(173, 216, 230))
                                            .rightColor(new Color(173, 216, 230))
                                            .build()));
                        }
                    }
                }
            }
        }

        // (E) Risk
        if (opponent instanceof Player) {
            if (config.riskDisplayOption() == KPOpponentInfoConfig.RiskDisplayOption.OVERLAY ||
                    config.riskDisplayOption() == KPOpponentInfoConfig.RiskDisplayOption.BOTH) {
                Player opp = (Player) opponent;
                long riskVal = computeRisk(opp);
                Color riskColor = Color.WHITE;
                if (riskVal > 0 && config.enableColorRisk()) {
                    if (riskVal < config.lowRiskThreshold()) {
                        riskColor = Color.WHITE;
                    } else if (riskVal < config.highRiskThreshold()) {
                        riskColor = config.mediumRiskColor();
                    } else if (riskVal < config.insaneRiskThreshold()) {
                        riskColor = config.highRiskColor();
                    } else {
                        riskColor = config.insaneRiskColor();
                    }
                }
                if (useIcons) {
                    BufferedImage rIcon = spriteManager.getSprite(RISK_ICON_ID, 0);
                    if (rIcon != null) {
                        rIcon = scaleImage(rIcon, 16, 16);
                    }
                    String rightVal = (riskVal > 0) ? formatWealth(riskVal) : "0";
                    BufferedImage lineImg = createCompositeIconLeftRight(
                            rIcon,
                            "Risk:",
                            rightVal,
                            RISK_LABEL_COLOR,
                            riskColor,
                            font,
                            ICON_LINE_WIDTH
                    );
                    normalItems.add(new OverlayItem(config.riskPriority(), new ImageComponent(lineImg)));
                } else {
                    normalItems.add(new OverlayItem(config.riskPriority(),
                            LineComponent.builder()
                                    .left("Risk:")
                                    .right((riskVal > 0) ? formatWealth(riskVal) : "0")
                                    .leftColor(RISK_LABEL_COLOR)
                                    .rightColor(riskColor)
                                    .build()));
                }
            }
        }

        normalItems.sort(Comparator.comparingDouble(OverlayItem::getPriority));
        for (OverlayItem item : normalItems) {
            panelComponent.getChildren().add(item.getComponent());
        }
        return super.render(graphics);
    }

    // -------------------------------------------------------------------------
    // HJELPEKLASSER OG METODER
    // -------------------------------------------------------------------------
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
     * Lager en EN-linje for "Style: Melee/Ranged/Magic" i ikonmodus.
     * Kombinerer (ikon 168 + "Style:") med (stilikon + stilenavn) i én rad.
     */
    private BufferedImage createSingleLineStyle(Font font, String styleName) {
        // Hent ikon for "Style:" (sprite ID 168) og skaler til 16x16
        BufferedImage styleLabelIcon = spriteManager.getSprite(STYLE_LABEL_ICON_ID, 0);
        if (styleLabelIcon != null) {
            styleLabelIcon = scaleImage(styleLabelIcon, 16, 16);
        }
        // Bestem stilikon basert på styleName
        int styleIconId;
        String lower = styleName.toLowerCase();
        if (lower.contains("ranged"))
            styleIconId = STYLE_RANGED_ICON_ID;
        else if (lower.contains("magic"))
            styleIconId = STYLE_MAGIC_ICON_ID;
        else
            styleIconId = STYLE_MELEE_ICON_ID;
        BufferedImage styleIcon = spriteManager.getSprite(styleIconId, 0);
        if (styleIcon != null) {
            styleIcon = scaleImage(styleIcon, 16, 16);
        }
        // Partial A: (ikon 168 + "Style:")
        BufferedImage partialA = createCompositeIconLeftRight(
                styleLabelIcon,
                "Style:",
                "",
                new Color(173, 216, 230),
                new Color(173, 216, 230),
                font,
                ICON_LINE_WIDTH / 2
        );
        // Partial B: (stilikon + styleName)
        BufferedImage partialB = createCompositeIconLeftRight(
                styleIcon,
                styleName,
                "",
                new Color(173, 216, 230),
                new Color(173, 216, 230),
                font,
                ICON_LINE_WIDTH / 2
        );
        return concatImages(partialA, partialB, 5);
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
     * Lager en linje med [ikon + leftText] venstrejustert og [rightText] høyrejustert
     * i en fast bredde-linje. Hvis rightText er tom, vises bare venstre.
     */
    private BufferedImage createCompositeIconLeftRight(
            BufferedImage icon,
            String leftText,
            String rightText,
            Color leftColor,
            Color rightColor,
            Font font,
            int totalWidth
    ) {
        BufferedImage temp = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gTemp = temp.createGraphics();
        gTemp.setFont(font);
        FontMetrics fm = gTemp.getFontMetrics();
        int leftTextWidth = fm.stringWidth(leftText);
        int rightTextWidth = fm.stringWidth(rightText);
        int textHeight = fm.getHeight();
        gTemp.dispose();

        int iconWidth = (icon != null) ? icon.getWidth() : 0;
        int iconHeight = (icon != null) ? icon.getHeight() : 0;
        int lineHeight = Math.max(iconHeight, textHeight);
        BufferedImage composite = new BufferedImage(totalWidth, lineHeight, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = composite.createGraphics();
        g2d.setFont(font);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int xPadding = 2;
        if (icon != null) {
            int iconY = (lineHeight - iconHeight) / 2;
            g2d.drawImage(icon, xPadding, iconY, null);
            xPadding += iconWidth + 3;
        }
        int baseline = (lineHeight + fm.getAscent()) / 2 - 2;
        g2d.setColor(leftColor);
        g2d.drawString(leftText, xPadding, baseline);
        if (!rightText.isEmpty()) {
            int rightPadding = 5;
            int rightX = totalWidth - rightTextWidth - rightPadding;
            g2d.setColor(rightColor);
            g2d.drawString(rightText, rightX, baseline);
        }
        g2d.dispose();
        return composite;
    }

    /**
     * Skalerer et bilde til ønsket bredde og høyde.
     */
    private BufferedImage scaleImage(BufferedImage src, int w, int h) {
        if (src == null) return null;
        BufferedImage scaled = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = scaled.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(src, 0, 0, w, h, null);
        g2d.dispose();
        return scaled;
    }

    /**
     * Sjekker om HP er under en gitt threshold (tolket i HP eller prosent).
     */
    private boolean isBelowHpOrPercent(int hpAbs, double healthPerc, int thresholdValue, KPOpponentInfoConfig.ThresholdUnit unit) {
        if (unit == KPOpponentInfoConfig.ThresholdUnit.PERCENT) {
            return (healthPerc * 100) < thresholdValue;
        } else {
            return hpAbs < thresholdValue;
        }
    }

    /**
     * Sjekker om motstander har aktiv boss-overlay (HP HUD).
     */
    private boolean hasHpHud(Actor opponent) {
        boolean settingEnabled = client.getVarbitValue(Varbits.BOSS_HEALTH_OVERLAY) == 0;
        if (settingEnabled && opponent instanceof NPC) {
            int oppId = client.getVarpValue(VarPlayer.HP_HUD_NPC_ID);
            NPC npc = (NPC) opponent;
            return (oppId != -1 && npc.getComposition() != null && oppId == npc.getComposition().getId());
        }
        return false;
    }

    private String trimText(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() > maxLength) {
            return text.substring(0, maxLength - 3) + "...";
        }
        return text;
    }

    /**
     * Bestemmer attack style ("Melee", "Ranged", "Magic") basert på våpennavn.
     * Inkluderer at "eclipse" regnes som Ranged, og nå også at "anchor" blir behandlet som Melee.
     */
    private String determineAttackStyle(Player p) {
        int weaponId = p.getPlayerComposition().getEquipmentId(KitType.WEAPON);
        if (weaponId == -1) return "Melee";
        ItemComposition comp = itemManager.getItemComposition(weaponId);
        if (comp == null) return "Unknown";
        String name = comp.getName().toLowerCase();
        if (name.contains("knife") || name.contains("bow") || name.contains("dart")
                || name.contains("blowpipe") || name.contains("throwing") || name.contains("thrown")
                || name.contains("toktz-xil-ul") || name.contains("eclipse")) {
            return "Ranged";
        }
        if (name.contains("staff") || name.contains("wand") || name.contains("crozier") || name.contains("salamander")) {
            return "Magic";
        }
        if (name.contains("tzhaar") || name.contains("maul") || name.contains("axe")
                || name.contains("bulwark") || name.contains("banners") || name.contains("machete")
                || name.contains("mjolnir") || name.contains("scythe") || name.contains("sickle")
                || name.contains("cutlass") || name.contains("hammer") || name.contains("claws")
                || name.contains("sword") || name.contains("scimitar") || name.contains("halberd")
                || name.contains("spear") || name.contains("whip") || name.contains("dagger")
                || name.contains("hasta") || name.contains("blade") || name.contains("abyssal")
                || name.contains("anchor")) {
            return "Melee";
        }
        return "Unknown";
    }

    /**
     * Bestemmer våpenets navn, eller "None" hvis ingen våpen.
     */
    private String determineWeaponName(Player p) {
        int wpnId = p.getPlayerComposition().getEquipmentId(KitType.WEAPON);
        if (wpnId == -1) return "None";
        ItemComposition comp = itemManager.getItemComposition(wpnId);
        if (comp == null) return "Unknown";
        return comp.getName();
    }

    /**
     * Beregner total verdi (risk) av utstyret.
     */
    private long computeRisk(Player p) {
        long total = 0;
        for (KitType kit : KitType.values()) {
            int itemId = p.getPlayerComposition().getEquipmentId(kit);
            if (itemId != -1) {
                total += itemManager.getItemPrice(itemId);
            }
        }
        return total;
    }

    /**
     * Formaterer verdien til en lesbar streng, f.eks. "850K" eller "1.2M".
     */
    private String formatWealth(long value) {
        if (value < 1000) {
            return Long.toString(value);
        } else if (value < 1_000_000) {
            return (value / 1000) + "K";
        } else {
            double millions = value / 1_000_000.0;
            return String.format("%.1fM", millions);
        }
    }
}
