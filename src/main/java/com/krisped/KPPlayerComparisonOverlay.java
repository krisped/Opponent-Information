package com.krisped;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import static net.runelite.api.MenuAction.RUNELITE_OVERLAY_CONFIG;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.client.hiscore.HiscoreManager;
import net.runelite.client.hiscore.HiscoreResult;
import net.runelite.client.hiscore.HiscoreSkill;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import static net.runelite.client.ui.overlay.OverlayManager.OPTION_CONFIGURE;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;
import net.runelite.client.util.Text;

class KPPlayerComparisonOverlay extends Overlay {

    private static final Color HIGHER_STAT_TEXT_COLOR = Color.GREEN;
    private static final Color LOWER_STAT_TEXT_COLOR = Color.RED;
    private static final Color NEUTRAL_STAT_TEXT_COLOR = Color.WHITE;
    private static final Color HIGHLIGHT_COLOR = new Color(255, 200, 0, 255);

    private static final Skill[] COMBAT_SKILLS = {
            Skill.ATTACK,
            Skill.STRENGTH,
            Skill.DEFENCE,
            Skill.HITPOINTS,
            Skill.RANGED,
            Skill.MAGIC,
            Skill.PRAYER
    };

    private static final HiscoreSkill[] HISCORE_COMBAT_SKILLS = {
            HiscoreSkill.ATTACK,
            HiscoreSkill.STRENGTH,
            HiscoreSkill.DEFENCE,
            HiscoreSkill.HITPOINTS,
            HiscoreSkill.RANGED,
            HiscoreSkill.MAGIC,
            HiscoreSkill.PRAYER
    };

    private static final String LEFT_COLUMN_HEADER = "Skill";
    private static final String RIGHT_COLUMN_HEADER = "You/Them";

    private final Client client;
    private final KPOpponentInfoPlugin plugin;
    private final KPOpponentInfoConfig config;
    private final HiscoreManager hiscoreManager;
    private final PanelComponent panelComponent = new PanelComponent();

    @Inject
    private KPPlayerComparisonOverlay(Client client, KPOpponentInfoPlugin plugin, KPOpponentInfoConfig config, HiscoreManager hiscoreManager)
    {
        super(plugin);
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        this.hiscoreManager = hiscoreManager;

        setPosition(OverlayPosition.BOTTOM_LEFT);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        addMenuEntry(RUNELITE_OVERLAY_CONFIG, OPTION_CONFIGURE, "[KP] Opponent Information overlay");
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!config.lookupOnInteraction())
        {
            return null;
        }
        final Actor opponent = plugin.getLastOpponent();
        if (opponent == null)
        {
            return null;
        }
        if (!(opponent instanceof Player))
        {
            return null;
        }
        final String opponentName = Text.removeTags(opponent.getName());
        final HiscoreResult hiscoreResult = hiscoreManager.lookupAsync(opponentName, plugin.getHiscoreEndpoint());
        if (hiscoreResult == null)
        {
            return null;
        }
        panelComponent.getChildren().clear();
        generateComparisonTable(panelComponent, hiscoreResult);
        return panelComponent.render(graphics);
    }

    private void generateComparisonTable(PanelComponent panelComponent, HiscoreResult opponentSkills)
    {
        final String opponentName = opponentSkills.getPlayer();
        panelComponent.getChildren().add(
                TitleComponent.builder()
                        .text(opponentName)
                        .color(HIGHLIGHT_COLOR)
                        .build());
        panelComponent.getChildren().add(
                LineComponent.builder()
                        .left(LEFT_COLUMN_HEADER)
                        .leftColor(HIGHLIGHT_COLOR)
                        .right(RIGHT_COLUMN_HEADER)
                        .rightColor(HIGHLIGHT_COLOR)
                        .build());
        for (int i = 0; i < COMBAT_SKILLS.length; ++i)
        {
            final HiscoreSkill hiscoreSkill = HISCORE_COMBAT_SKILLS[i];
            final Skill skill = COMBAT_SKILLS[i];
            final net.runelite.client.hiscore.Skill opponentSkill = opponentSkills.getSkill(hiscoreSkill);
            if (opponentSkill == null || opponentSkill.getLevel() == -1)
            {
                continue;
            }
            final int playerSkillLevel = client.getRealSkillLevel(skill);
            final int opponentSkillLevel = opponentSkill.getLevel();
            panelComponent.getChildren().add(
                    LineComponent.builder()
                            .left(hiscoreSkill.getName())
                            .right(playerSkillLevel + "/" + opponentSkillLevel)
                            .rightColor(comparisonStatColor(playerSkillLevel, opponentSkillLevel))
                            .build());
        }
    }

    private static Color comparisonStatColor(int a, int b)
    {
        if (a > b)
        {
            return HIGHER_STAT_TEXT_COLOR;
        }
        if (a < b)
        {
            return LOWER_STAT_TEXT_COLOR;
        }
        return NEUTRAL_STAT_TEXT_COLOR;
    }
}
