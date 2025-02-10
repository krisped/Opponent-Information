package com.krisped;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import javax.inject.Inject;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

public class KPOpponentHighlight extends Overlay {

    private final Client client;
    private final KPOpponentInfoPlugin plugin;
    private final KPOpponentInfoConfig config;
    private final ModelOutlineRenderer modelOutlineRenderer;

    @Inject
    public KPOpponentHighlight(Client client, KPOpponentInfoPlugin plugin, KPOpponentInfoConfig config, ModelOutlineRenderer modelOutlineRenderer) {
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        this.modelOutlineRenderer = modelOutlineRenderer;
        setPosition(OverlayPosition.DYNAMIC);
    }

    // Beregn farge basert på spillerens helse – brukes både for static og dynamic modus
    private Color getCalculatedStaticColor(Player player) {
        int ratio = player.getHealthRatio();
        int scale = player.getHealthScale();
        double healthPerc = (scale > 0) ? (double) ratio / scale : 1.0;
        if (healthPerc >= 0.75) {
            return new Color(0, 146, 54, 230); // grønn
        } else if (healthPerc >= 0.25) {
            return new Color(255, 255, 0, 230); // gul
        } else {
            return new Color(255, 0, 0, 230); // rød
        }
    }

    // Sjekk om blink skal være aktivt basert på spillerens helse
    private boolean isBlinkActive(Player player) {
        int ratio = player.getHealthRatio();
        int scale = player.getHealthScale();
        double healthPerc = (scale > 0) ? (double) ratio / scale : 1.0;
        if (config.blinkThresholdUnit() == KPOpponentInfoConfig.ThresholdUnit.PERCENT) {
            return (healthPerc * 100 < config.blinkThresholdValue());
        } else {
            // For HP-enhet bruker vi prosent-sammenligning
            return (healthPerc * 100 < config.blinkThresholdValue());
        }
    }

    // Returnerer en dimmet farge (blink-effekt)
    private Color applyBlink(Color baseColor) {
        long time = System.currentTimeMillis();
        if ((time / 500) % 2 == 0) {
            return new Color(baseColor.getRed() / 2, baseColor.getGreen() / 2, baseColor.getBlue() / 2, baseColor.getAlpha());
        }
        return baseColor;
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        // Dersom ingen highlight-modus er aktivert, avslutt
        if (config.outlineHighlightMode() == KPOpponentInfoConfig.HighlightMode.NONE &&
                config.hullHighlightMode() == KPOpponentInfoConfig.HighlightMode.NONE &&
                config.tileHighlightMode() == KPOpponentInfoConfig.HighlightMode.NONE) {
            return null;
        }

        Actor opponent = plugin.getLastOpponent();
        if (opponent == null || !(opponent instanceof Player)) {
            return null;
        }
        Player player = (Player) opponent;

        // --- Outline Highlight ---
        if (config.outlineHighlightMode() != KPOpponentInfoConfig.HighlightMode.NONE) {
            Color color;
            if (config.outlineHighlightMode() == KPOpponentInfoConfig.HighlightMode.STATIC) {
                color = config.outlineHighlightColor();
            } else { // DYNAMIC – bruk beregnet farge, uavhengig av statusbarens blink
                color = getCalculatedStaticColor(player);
            }
            if (config.outlineBlink() && isBlinkActive(player)) {
                color = applyBlink(color);
            }
            modelOutlineRenderer.drawOutline(player, 1, color, 1);
        }

        // --- Hull Highlight ---
        if (config.hullHighlightMode() != KPOpponentInfoConfig.HighlightMode.NONE) {
            Color color;
            if (config.hullHighlightMode() == KPOpponentInfoConfig.HighlightMode.STATIC) {
                color = config.hullHighlightColor();
            } else { // DYNAMIC
                color = getCalculatedStaticColor(player);
            }
            if (config.hullBlink() && isBlinkActive(player)) {
                color = applyBlink(color);
            }
            Shape hull = player.getConvexHull();
            if (hull != null) {
                Rectangle2D bounds = hull.getBounds2D();
                double centerX = bounds.getCenterX();
                double centerY = bounds.getCenterY();
                double scale = 1.1;
                AffineTransform at = AffineTransform.getTranslateInstance(centerX, centerY);
                at.scale(scale, scale);
                at.translate(-centerX, -centerY);
                Shape expandedHull = at.createTransformedShape(hull);
                graphics.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                graphics.setColor(color);
                graphics.draw(expandedHull);
            }
        }

        // --- Tile Highlight ---
        if (config.tileHighlightMode() != KPOpponentInfoConfig.HighlightMode.NONE) {
            Color color;
            if (config.tileHighlightMode() == KPOpponentInfoConfig.HighlightMode.STATIC) {
                color = config.tileHighlightColor();
            } else { // DYNAMIC
                color = getCalculatedStaticColor(player);
            }
            if (config.tileBlink() && isBlinkActive(player)) {
                color = applyBlink(color);
            }
            LocalPoint lp = player.getLocalLocation();
            if (lp != null) {
                Polygon tilePoly = Perspective.getCanvasTilePoly(client, lp);
                if (tilePoly != null) {
                    graphics.setStroke(new BasicStroke(1));
                    graphics.setColor(color);
                    graphics.draw(tilePoly);
                }
            }
        }
        return null;
    }
}
