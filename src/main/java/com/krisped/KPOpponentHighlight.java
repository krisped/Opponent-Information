package com.krisped;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.Polygon;
import java.awt.Shape;

import javax.inject.Inject;

import net.runelite.api.*;
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
        // 1) Hvis alt highlight er av -> ingenting å gjøre
        if (config.outlineHighlightMode() == KPOpponentInfoConfig.HighlightMode.NONE
                && config.hullHighlightMode() == KPOpponentInfoConfig.HighlightMode.NONE
                && config.tileHighlightMode() == KPOpponentInfoConfig.HighlightMode.NONE)
        {
            return null;
        }

        // 2) Få tak i siste motstander
        final Actor opponent = plugin.getLastOpponent();
        if (opponent == null)
        {
            return null;
        }

        // 3) Ikke highlight lokal spiller (deg selv)
        if (opponent == client.getLocalPlayer())
        {
            return null;
        }

        // 4) Er motstanderen død -> ingen highlight
        if (opponent.getHealthRatio() <= 0)
        {
            return null;
        }

        // 5) Sjekk NPC toggle
        //    Hvis det er en NPC, men !enableNpcHighlight() => tidlig retur
        if (opponent instanceof NPC && !config.enableNpcHighlight())
        {
            return null;
        }

        // 6) Outline highlight
        if (config.outlineHighlightMode() != KPOpponentInfoConfig.HighlightMode.NONE)
        {
            Color outlineColor = getHighlightColor(opponent, config.outlineHighlightMode(), config.outlineHighlightColor());
            if (outlineColor != null)
            {
                // Sjekk om blinking skal på
                if (config.outlineBlink() && isBlinkActive(opponent))
                {
                    outlineColor = applyBlink(outlineColor);
                }

                // ModelOutlineRenderer krever spesifikk type (NPC/Player)
                if (opponent instanceof NPC)
                {
                    modelOutlineRenderer.drawOutline((NPC) opponent, 1, outlineColor, 1);
                }
                else if (opponent instanceof Player)
                {
                    modelOutlineRenderer.drawOutline((Player) opponent, 1, outlineColor, 1);
                }
            }
        }

        // 7) Hull highlight
        if (config.hullHighlightMode() != KPOpponentInfoConfig.HighlightMode.NONE)
        {
            Color hullColor = getHighlightColor(opponent, config.hullHighlightMode(), config.hullHighlightColor());
            if (hullColor != null)
            {
                if (config.hullBlink() && isBlinkActive(opponent))
                {
                    hullColor = applyBlink(hullColor);
                }

                Shape hull = opponent.getConvexHull();
                if (hull != null)
                {
                    // For litt "bredere" hull (skalér formen ~1.1x)
                    hull = expandShape(hull, 1.1);

                    graphics.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    graphics.setColor(hullColor);
                    graphics.draw(hull);
                }
            }
        }

        // 8) Tile highlight
        if (config.tileHighlightMode() != KPOpponentInfoConfig.HighlightMode.NONE)
        {
            Color tileColor = getHighlightColor(opponent, config.tileHighlightMode(), config.tileHighlightColor());
            if (tileColor != null)
            {
                if (config.tileBlink() && isBlinkActive(opponent))
                {
                    tileColor = applyBlink(tileColor);
                }

                LocalPoint lp = opponent.getLocalLocation();
                if (lp != null)
                {
                    Polygon tilePoly = Perspective.getCanvasTilePoly(client, lp);
                    if (tilePoly != null)
                    {
                        graphics.setStroke(new BasicStroke(1));
                        graphics.setColor(tileColor);
                        graphics.draw(tilePoly);
                    }
                }
            }
        }

        return null;
    }

    /**
     * Gitt highlightMode == STATIC eller DYNAMIC,
     * returner riktig farge.
     * STATIC -> bruk config-verdien (staticColor).
     * DYNAMIC -> bruk "dynamic bar settings" (gul/rød/hp–avhengig).
     */
    private Color getHighlightColor(Actor actor, KPOpponentInfoConfig.HighlightMode mode, Color staticColor)
    {
        switch (mode)
        {
            case NONE:
                return null;
            case STATIC:
                return staticColor;
            case DYNAMIC:
                return getDynamicColor(actor);
            default:
                return null;
        }
    }

    /**
     * Farge basert på dine "Dynamic Bar Settings":
     *  - redThresholdValue / redThresholdUnit
     *  - yellowThresholdValue / yellowThresholdUnit
     *  - dynamic av/på? (Hvis du vil sjekke config.dynamicHealthColor(), kan du trekke inn
     *    men her antas DYNAMIC highlight skal "alltid" bruke HP-baserte farger).
     */
    private Color getDynamicColor(Actor actor)
    {
        // Hent HP ratio og scale
        int ratio = actor.getHealthRatio();
        int scale = actor.getHealthScale();

        // Ukjent HP => grå
        if (ratio <= 0 || scale <= 0)
        {
            return new Color(128, 128, 128, 180);
        }

        // healthPerc i prosent
        double healthPerc = ((double) ratio / scale) * 100.0;

        // Sjekk om vi er "under" rød-terskel
        boolean belowRed;
        if (config.redThresholdUnit() == KPOpponentInfoConfig.ThresholdUnit.PERCENT)
        {
            belowRed = (healthPerc < config.redThresholdValue());
        }
        else
        {
            // Terskel i HP (f.eks. 10 HP)
            belowRed = (ratio < config.redThresholdValue());
        }

        // Sjekk om vi er "under" gul-terskel
        boolean belowYellow;
        if (config.yellowThresholdUnit() == KPOpponentInfoConfig.ThresholdUnit.PERCENT)
        {
            belowYellow = (healthPerc < config.yellowThresholdValue());
        }
        else
        {
            belowYellow = (ratio < config.yellowThresholdValue());
        }

        if (belowRed)
        {
            // Rød
            return new Color(255, 0, 0, 230);
        }
        else if (belowYellow)
        {
            // Gul
            return new Color(255, 255, 0, 230);
        }
        else
        {
            // Grønn
            return new Color(0, 146, 54, 230);
        }
    }

    /**
     * Sjekker om blinking er aktiv for denne motstanderen
     * basert på "blinkThresholdValue" / "blinkThresholdUnit".
     * Samme logikk som i 'onGameTick()' for dynamic bar.
     */
    private boolean isBlinkActive(Actor actor)
    {
        // Vi bruker dynamic bar–terskelen for blinking
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

    /**
     * "Dimmer" fargen annenhvert 500ms.
     */
    private Color applyBlink(Color baseColor)
    {
        long now = System.currentTimeMillis();
        // Hvert 500ms -> halver intensitet
        if ((now / 500) % 2 == 0)
        {
            return new Color(
                    baseColor.getRed() / 2,
                    baseColor.getGreen() / 2,
                    baseColor.getBlue() / 2,
                    baseColor.getAlpha()
            );
        }
        return baseColor;
    }

    /**
     * Skalér (expander) formen litt, f.eks. hull, for å få en større omkrets.
     */
    private Shape expandShape(Shape shape, double scaleFactor)
    {
        Rectangle2D bounds = shape.getBounds2D();
        double cx = bounds.getCenterX();
        double cy = bounds.getCenterY();

        AffineTransform tx = new AffineTransform();
        tx.translate(cx, cy);
        tx.scale(scaleFactor, scaleFactor);
        tx.translate(-cx, -cy);

        return tx.createTransformedShape(shape);
    }
}
