package com.krisped;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Provides;
import java.awt.Color;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import javax.inject.Inject;
import lombok.AccessLevel;
import lombok.Getter;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.ScriptID;
import net.runelite.api.Varbits;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.hiscore.HiscoreEndpoint;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
        name = "[KP] Opponent Information",
        description = "Vis navn og hitpoints for motstanderen du kjemper med",
        tags = {"combat", "health", "hitpoints", "npcs", "overlay"}
)
public class KPOpponentInfoPlugin extends Plugin {

    private static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("0.0");

    @Inject
    private Client client;

    @Inject
    private KPOpponentInfoConfig config;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private KPOpponentInfoOverlay overlay;

    @Inject
    private KPPlayerComparisonOverlay comparisonOverlay;

    @Inject
    private KPOpponentHighlight opponentHighlight;

    @Inject
    private EventBus eventBus;

    @Inject
    private PlayerGearChecker playerGearChecker;

    @Getter(AccessLevel.PACKAGE)
    private HiscoreEndpoint hiscoreEndpoint = HiscoreEndpoint.NORMAL;

    @Getter(AccessLevel.PACKAGE)
    @VisibleForTesting
    private Instant lastTime;

    // Denne variabelen lagrer siste motstander.
    private Actor lastOpponent;

    // Public getter for at andre klasser skal få tilgang til den.
    public Actor getLastOpponent() {
        return lastOpponent;
    }

    @Getter
    private long riskValue = 0;

    public void setRiskValue(long riskValue) {
        this.riskValue = riskValue;
    }

    // Nytt felt for å lagre siste dynamiske fargen (brukes av highlight)
    private Color lastDynamicColor = Color.GREEN;

    public void setLastDynamicColor(Color color) {
        this.lastDynamicColor = color;
    }

    public Color getLastDynamicColor() {
        return lastDynamicColor;
    }

    @Provides
    KPOpponentInfoConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(KPOpponentInfoConfig.class);
    }

    @Override
    protected void startUp() throws Exception {
        overlayManager.add(overlay);
        overlayManager.add(comparisonOverlay);
        overlayManager.add(opponentHighlight);
        eventBus.register(playerGearChecker);
        eventBus.register(this);
    }

    @Override
    protected void shutDown() throws Exception {
        overlayManager.remove(overlay);
        overlayManager.remove(comparisonOverlay);
        overlayManager.remove(opponentHighlight);
        eventBus.unregister(playerGearChecker);
        eventBus.unregister(this);
        lastOpponent = null;
        lastTime = null;
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged) {
        if (gameStateChanged.getGameState() != GameState.LOGGED_IN) {
            return;
        }
        hiscoreEndpoint = HiscoreEndpoint.fromWorldTypes(client.getWorldType());
    }

    @Subscribe
    public void onInteractingChanged(InteractingChanged event) {
        if (event.getSource() != client.getLocalPlayer()) {
            return;
        }
        Actor opponent = event.getTarget();
        if (opponent == null) {
            lastTime = Instant.now();
            return;
        }
        // Oppdater siste motstander
        lastOpponent = opponent;
    }

    @Subscribe
    public void onGameTick(GameTick gameTick) {
        // Hvis spilleren ikke lenger interagerer, og overlayet har vært vist i for lang tid,
        // fjern motstanderen (slik at overlayet forsvinner).
        if (lastOpponent != null && lastTime != null && client.getLocalPlayer().getInteracting() == null) {
            if (Duration.between(lastTime, Instant.now()).toSeconds() > config.overlayDisplayDuration()) {
                lastOpponent = null;
            }
        }
    }

    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded menuEntryAdded) {
        if (menuEntryAdded.getType() != MenuAction.NPC_SECOND_OPTION.getId()
                || !menuEntryAdded.getOption().equals("Attack")
                || !config.showOpponentsInMenu()) {
            return;
        }
        NPC npc = menuEntryAdded.getMenuEntry().getNpc();
        if (npc == null) {
            return;
        }
        MenuEntry[] menuEntries = client.getMenuEntries();
        menuEntries[menuEntries.length - 1].setTarget("*" + menuEntries[menuEntries.length - 1].getTarget());
    }

    @Subscribe
    public void onScriptPostFired(ScriptPostFired event) {
        if (event.getScriptId() == ScriptID.HP_HUD_UPDATE) {
            updateBossHealthBarText();
        }
    }

    private void updateBossHealthBarText() {
        Widget widget = client.getWidget(ComponentID.HEALTH_HEALTHBAR_TEXT);
        if (widget == null) {
            return;
        }
        final int currHp = client.getVarbitValue(Varbits.BOSS_HEALTH_CURRENT);
        final int maxHp = client.getVarbitValue(Varbits.BOSS_HEALTH_MAXIMUM);
        if (maxHp <= 0) {
            return;
        }
        switch (config.hitpointsDisplayStyle()) {
            case PERCENTAGE:
                widget.setText(getPercentText(currHp, maxHp));
                break;
            case BOTH:
                widget.setText(widget.getText() + " (" + getPercentText(currHp, maxHp) + ")");
                break;
        }
    }

    private static String getPercentText(int current, int maximum) {
        double percent = 100.0 * current / maximum;
        return PERCENT_FORMAT.format(percent) + "%";
    }
}
