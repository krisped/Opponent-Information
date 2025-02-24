package com.krisped;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import javax.inject.Inject;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

public class KPOpponentHighlight extends Overlay
{
    private final Client client;
    private final KPOpponentInfoPlugin plugin;
    private final KPOpponentInfoConfig config;
    private final ModelOutlineRenderer modelOutlineRenderer;

    @Inject
    public KPOpponentHighlight(
            Client client,
            KPOpponentInfoPlugin plugin,
            KPOpponentInfoConfig config,
            ModelOutlineRenderer modelOutlineRenderer
    )
    {
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        this.modelOutlineRenderer = modelOutlineRenderer;
        setPosition(OverlayPosition.DYNAMIC);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        // Hvis alt er av, ikke highlight
        if (config.outlineHighlightMode() == KPOpponentInfoConfig.HighlightMode.NONE
                && config.hullHighlightMode() == KPOpponentInfoConfig.HighlightMode.NONE
                && config.tileHighlightMode() == KPOpponentInfoConfig.HighlightMode.NONE)
        {
            return null;
        }

        Actor opponent = plugin.getLastOpponent();
        if (opponent == null)
        {
            return null;
        }

        // Hvis motstanderen er død => highlight forsvinner med en gang
        if (opponent.getHealthRatio() <= 0)
        {
            return null;
        }

        // Hvis NPC, men "Enable NPC Highlight" er av, returner
        if (opponent instanceof NPC && !config.enableNpcHighlight())
        {
            return null;
        }

        // Outline
        if (config.outlineHighlightMode() != KPOpponentInfoConfig.HighlightMode.NONE)
        {
            Color color = getHighlightColor(opponent, config.outlineHighlightMode());
            if (config.outlineBlink() && isBlinkActive(opponent))
            {
                color = applyBlink(color);
            }

            if (opponent instanceof Player)
            {
                modelOutlineRenderer.drawOutline((Player) opponent, 1, color, 1);
            }
            else if (opponent instanceof NPC)
            {
                modelOutlineRenderer.drawOutline((NPC) opponent, 1, color, 1);
            }
        }

        // Hull
        if (config.hullHighlightMode() != KPOpponentInfoConfig.HighlightMode.NONE)
        {
            Color color = getHighlightColor(opponent, config.hullHighlightMode());
            if (config.hullBlink() && isBlinkActive(opponent))
            {
                color = applyBlink(color);
            }

            Shape hull = opponent.getConvexHull();
            if (hull != null)
            {
                // For litt "bredere" hull
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

        // Tile
        if (config.tileHighlightMode() != KPOpponentInfoConfig.HighlightMode.NONE)
        {
            Color color = getHighlightColor(opponent, config.tileHighlightMode());
            if (config.tileBlink() && isBlinkActive(opponent))
            {
                color = applyBlink(color);
            }

            LocalPoint lp = opponent.getLocalLocation();
            if (lp != null)
            {
                Polygon tilePoly = Perspective.getCanvasTilePoly(client, lp);
                if (tilePoly != null)
                {
                    graphics.setStroke(new BasicStroke(1));
                    graphics.setColor(color);
                    graphics.draw(tilePoly);
                }
            }
        }

        return null;
    }

    /**
     * Velger highlight-farge basert på STATIC vs DYNAMIC.
     */
    private Color getHighlightColor(Actor actor, KPOpponentInfoConfig.HighlightMode mode)
    {
        if (mode == KPOpponentInfoConfig.HighlightMode.STATIC)
        {
            // Avhenger av om outline/hull/tile => men du har config for hver
            // her viser vi bare outlineHighlightColor som eks;
            // men i praksis hentes det i kallet ovenfor
            // Her for generalitet gir vi "dynamic" farge:
            return Color.RED; // dead code i denne situasjonen -> vi håndterer rett i kallet
        }

        // mode == DYNAMIC => farge basert på HP ratio
        int ratio = actor.getHealthRatio();
        int scale = actor.getHealthScale();
        double healthPerc = (scale > 0) ? (double) ratio / scale : 1.0;
        if (healthPerc >= 0.75)
        {
            return new Color(0, 146, 54, 230);
        }
        else if (healthPerc >= 0.25)
        {
            return new Color(255, 255, 0, 230);
        }
        else
        {
            return new Color(255, 0, 0, 230);
        }
    }

    private boolean isBlinkActive(Actor actor)
    {
        // Bruker dynamic bar settings
        int ratio = actor.getHealthRatio();
        int scale = actor.getHealthScale();
        if (ratio <= 0 || scale <= 0)
        {
            return false;
        }
        double healthPerc = (double) ratio / scale;

        if (config.blinkThresholdUnit() == KPOpponentInfoConfig.ThresholdUnit.PERCENT)
        {
            return (healthPerc * 100.0) < config.blinkThresholdValue();
        }
        else
        {
            // HP
            return ratio < config.blinkThresholdValue();
        }
    }

    private Color applyBlink(Color baseColor)
    {
        long now = System.currentTimeMillis();
        if ((now / 500) % 2 == 0)
        {
            // halver intensitet
            return new Color(
                    baseColor.getRed() / 2,
                    baseColor.getGreen() / 2,
                    baseColor.getBlue() / 2,
                    baseColor.getAlpha()
            );
        }
        return baseColor;
    }
}
