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

    private Color getCalculatedStaticColor(Player player) {
        int ratio = player.getHealthRatio();
        int scale = player.getHealthScale();
        double healthPerc = (scale > 0) ? (double) ratio / scale : 1.0;
        if (healthPerc >= 0.75)
            return new Color(0, 146, 54, 230);
        else if (healthPerc >= 0.25)
            return new Color(255, 255, 0, 230);
        else
            return new Color(255, 0, 0, 230);
    }

    private boolean isBlinkActive(Player player) {
        int ratio = player.getHealthRatio();
        int scale = player.getHealthScale();
        double healthPerc = (scale > 0) ? (double) ratio / scale : 1.0;
        if (config.blinkThresholdUnit() == KPOpponentInfoConfig.ThresholdUnit.PERCENT)
            return (healthPerc * 100 < config.blinkThresholdValue());
        else
            return (healthPerc * 100 < config.blinkThresholdValue());
    }

    private Color applyBlink(Color baseColor) {
        long time = System.currentTimeMillis();
        if ((time / 500) % 2 == 0)
            return new Color(baseColor.getRed() / 2, baseColor.getGreen() / 2, baseColor.getBlue() / 2, baseColor.getAlpha());
        return baseColor;
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (config.outlineHighlightMode() == KPOpponentInfoConfig.HighlightMode.NONE &&
                config.hullHighlightMode() == KPOpponentInfoConfig.HighlightMode.NONE &&
                config.tileHighlightMode() == KPOpponentInfoConfig.HighlightMode.NONE)
            return null;

        Actor opponent = plugin.getLastOpponent();
        if (opponent == null || !(opponent instanceof Player))
            return null;
        Player player = (Player) opponent;
        if (player.getHealthRatio() <= 0)
            return null;

        if (config.outlineHighlightMode() != KPOpponentInfoConfig.HighlightMode.NONE) {
            Color color;
            if (config.outlineHighlightMode() == KPOpponentInfoConfig.HighlightMode.STATIC)
                color = config.outlineHighlightColor();
            else
                color = getCalculatedStaticColor(player);
            if (config.outlineBlink() && isBlinkActive(player))
                color = applyBlink(color);
            modelOutlineRenderer.drawOutline(player, 1, color, 1);
        }

        if (config.hullHighlightMode() != KPOpponentInfoConfig.HighlightMode.NONE) {
            Color color;
            if (config.hullHighlightMode() == KPOpponentInfoConfig.HighlightMode.STATIC)
                color = config.hullHighlightColor();
            else
                color = getCalculatedStaticColor(player);
            if (config.hullBlink() && isBlinkActive(player))
                color = applyBlink(color);
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

        if (config.tileHighlightMode() != KPOpponentInfoConfig.HighlightMode.NONE) {
            Color color;
            if (config.tileHighlightMode() == KPOpponentInfoConfig.HighlightMode.STATIC)
                color = config.tileHighlightColor();
            else
                color = getCalculatedStaticColor(player);
            if (config.tileBlink() && isBlinkActive(player))
                color = applyBlink(color);
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
