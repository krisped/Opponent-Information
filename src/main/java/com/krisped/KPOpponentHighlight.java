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

    // Predefinerte farger for dynamisk modus (kan justeres)
    private static final Color HP_GREEN = new Color(0, 146, 54, 230);
    private static final Color HP_YELLOW = new Color(255, 255, 0, 230);
    private static final Color HP_RED = new Color(255, 0, 0, 230);

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

    // Metode for å beregne dynamisk farge basert på spillerens helse
    private Color getDynamicColor(Player player) {
        int healthRatio = player.getHealthRatio();
        int healthScale = player.getHealthScale();
        double healthPercentage = (healthScale > 0) ? (double) healthRatio / healthScale : 1.0;
        if (healthPercentage >= 0.75) {
            return HP_GREEN;
        } else if (healthPercentage >= 0.25) {
            return HP_YELLOW;
        } else {
            return HP_RED;
        }
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        // Sjekk at minst ett av highlight-modusene er aktivert
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

        // Beregn dynamisk farge (basert på spillerens helse)
        Color dynamicColor = getDynamicColor(player);

        // 1. Outline Highlight (Priority 1)
        if (config.outlineHighlightMode() != KPOpponentInfoConfig.HighlightMode.NONE) {
            Color color = (config.outlineHighlightMode() == KPOpponentInfoConfig.HighlightMode.STATIC)
                    ? config.outlineHighlightColor()
                    : dynamicColor;
            // Tegn en outline på spillerens 3D-modell med zOffset=1 og stroke-width=1
            modelOutlineRenderer.drawOutline(player, 1, color, 1);
        }

        // 2. Hull Highlight (Priority 2)
        if (config.hullHighlightMode() != KPOpponentInfoConfig.HighlightMode.NONE) {
            Color color = (config.hullHighlightMode() == KPOpponentInfoConfig.HighlightMode.STATIC)
                    ? config.hullHighlightColor()
                    : dynamicColor;
            Shape hull = player.getConvexHull();
            if (hull != null) {
                Rectangle2D bounds = hull.getBounds2D();
                double centerX = bounds.getCenterX();
                double centerY = bounds.getCenterY();
                double scale = 1.1; // Utvid convex hull med 10%
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
                    ? config.tileHighlightColor()
                    : dynamicColor;
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
