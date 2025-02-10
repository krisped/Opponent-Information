package com.krisped;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Shape;
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

    // I static modus beregnes fargen ut fra spillerens helse.
    private Color getCalculatedDynamicColor(Player player) {
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

    // I dynamisk modus henter vi statusbarens farge fra pluginet, hvis tilgjengelig.
    private Color getDynamicColor(Player player) {
        Color c = plugin.getLastDynamicColor();
        return (c != null) ? c : getCalculatedDynamicColor(player);
    }

    // Blink-sjekken brukes kun for static modus; i dynamisk modus ignorerer vi blink.
    private boolean isBlinkActive(Player player) {
        int ratio = player.getHealthRatio();
        int scale = player.getHealthScale();
        double healthPerc = (scale > 0) ? (double) ratio / scale : 1.0;
        return (healthPerc * 100 < config.blinkThresholdValue());
    }

    // Dimmer fargen ved blink – samme som statusbarens logikk.
    private Color dimColor(Color color) {
        return new Color(color.getRed() / 2, color.getGreen() / 2, color.getBlue() / 2, color.getAlpha());
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (config.outlineHighlightMode() == KPOpponentInfoConfig.HighlightMode.NONE &&
                config.hullHighlightMode() == KPOpponentInfoConfig.HighlightMode.NONE &&
                config.tileHighlightMode() == KPOpponentInfoConfig.HighlightMode.NONE)
        {
            return null;
        }

        Actor opponent = plugin.getLastOpponent();
        if (opponent == null || !(opponent instanceof Player)) {
            return null;
        }
        Player player = (Player) opponent;
        Color dynamicColor = getDynamicColor(player);

        // 1. Outline Highlight (Priority 1)
        if (config.outlineHighlightMode() != KPOpponentInfoConfig.HighlightMode.NONE) {
            // Hvis mode er static, bruk blink-innstillingen; i dynamisk mode ignoreres blink
            Color color = (config.outlineHighlightMode() == KPOpponentInfoConfig.HighlightMode.STATIC)
                    ? config.outlineHighlightColor() : dynamicColor;
            if (config.outlineHighlightMode() == KPOpponentInfoConfig.HighlightMode.STATIC &&
                    config.outlineBlink() && isBlinkActive(player)) {
                color = dimColor(color);
            }
            modelOutlineRenderer.drawOutline(player, 1, color, 1);
        }

        // 2. Hull Highlight (Priority 2)
        if (config.hullHighlightMode() != KPOpponentInfoConfig.HighlightMode.NONE) {
            Color color = (config.hullHighlightMode() == KPOpponentInfoConfig.HighlightMode.STATIC)
                    ? config.hullHighlightColor() : dynamicColor;
            if (config.hullHighlightMode() == KPOpponentInfoConfig.HighlightMode.STATIC &&
                    config.hullBlink() && isBlinkActive(player)) {
                color = dimColor(color);
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

        // 3. Tile Highlight (Priority 3)
        if (config.tileHighlightMode() != KPOpponentInfoConfig.HighlightMode.NONE) {
            Color color = (config.tileHighlightMode() == KPOpponentInfoConfig.HighlightMode.STATIC)
                    ? config.tileHighlightColor() : dynamicColor;
            if (config.tileHighlightMode() == KPOpponentInfoConfig.HighlightMode.STATIC &&
                    config.tileBlink() && isBlinkActive(player)) {
                color = dimColor(color);
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
