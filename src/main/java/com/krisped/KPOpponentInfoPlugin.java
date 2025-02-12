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
import net.runelite.api.Hitsplat;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.Prayer;
import net.runelite.api.Projectile;
import net.runelite.api.ScriptID;
import net.runelite.api.Varbits;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.ProjectileMoved;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
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
public class KPOpponentInfoPlugin extends Plugin
{
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
    private EventBus eventBus;

    @Inject
    private PlayerGearChecker playerGearChecker;

    // Gjeninnfører highlight:
    @Inject
    private KPOpponentHighlight opponentHighlight;

    @Getter(AccessLevel.PACKAGE)
    private HiscoreEndpoint hiscoreEndpoint = HiscoreEndpoint.NORMAL;

    @Getter(AccessLevel.PACKAGE)
    @VisibleForTesting
    private Instant lastTime;

    private Actor lastOpponent;
    public Actor getLastOpponent() {
        return lastOpponent;
    }

    @Getter
    private long riskValue = 0;
    public void setRiskValue(long riskValue) {
        this.riskValue = riskValue;
    }

    private Color lastDynamicColor = Color.GREEN;
    public Color getLastDynamicColor() {
        return lastDynamicColor;
    }
    public void setLastDynamicColor(Color lastDynamicColor) {
        this.lastDynamicColor = lastDynamicColor;
    }

    private int smitedPrayer = 0;
    public int getSmitedPrayer() {
        return smitedPrayer;
    }
    private boolean smiteActivated = false;
    public boolean isSmiteActivated() {
        return smiteActivated;
    }

    private Hitsplat lastHitsplat;

    private String currentWeaponName;
    private String lastWeaponName;

    public String getCurrentWeaponName() {
        return currentWeaponName;
    }
    public void setCurrentWeaponName(String weapon) {
        this.currentWeaponName = weapon;
    }
    public String getLastWeaponName() {
        return lastWeaponName;
    }
    public void setLastWeaponName(String weapon) {
        this.lastWeaponName = weapon;
    }

    private boolean riskMessageSent = false;
    public boolean isRiskMessageSent() {
        return riskMessageSent;
    }
    public void setRiskMessageSent(boolean riskMessageSent) {
        this.riskMessageSent = riskMessageSent;
    }

    private String currentSpellName = "";
    public String getCurrentSpellName() {
        return currentSpellName;
    }
    public void setCurrentSpellName(String spellName) {
        this.currentSpellName = spellName;
    }

    @Provides
    KPOpponentInfoConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(KPOpponentInfoConfig.class);
    }

    @Override
    protected void startUp() throws Exception
    {
        overlayManager.add(overlay);
        overlayManager.add(comparisonOverlay);
        overlayManager.add(opponentHighlight); // Legg til highlight

        eventBus.register(playerGearChecker);
        eventBus.register(this);
    }

    @Override
    protected void shutDown() throws Exception
    {
        overlayManager.remove(overlay);
        overlayManager.remove(comparisonOverlay);
        overlayManager.remove(opponentHighlight); // Fjern highlight

        eventBus.unregister(playerGearChecker);
        eventBus.unregister(this);

        lastOpponent = null;
        lastTime = null;
        smiteActivated = false;
        smitedPrayer = 0;
        currentWeaponName = null;
        lastWeaponName = null;
        riskMessageSent = false;
        currentSpellName = "";
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() == GameState.LOGGED_IN)
        {
            hiscoreEndpoint = HiscoreEndpoint.fromWorldTypes(client.getWorldType());
        }
    }

    @Subscribe
    public void onInteractingChanged(InteractingChanged event)
    {
        if (event.getSource() != client.getLocalPlayer())
        {
            return;
        }
        Actor opponent = event.getTarget();
        if (opponent == null)
        {
            lastTime = Instant.now();
            return;
        }

        if (lastOpponent != opponent)
        {
            smitedPrayer = 0;
            smiteActivated = false;
            lastTime = Instant.now();
            currentWeaponName = null;
            lastWeaponName = null;
            riskMessageSent = false;
            currentSpellName = "";
        }
        lastOpponent = opponent;
    }

    @Subscribe
    public void onHitsplatApplied(HitsplatApplied event)
    {
        if (!client.isPrayerActive(Prayer.SMITE))
        {
            return;
        }
        if (event.getActor() != lastOpponent)
        {
            return;
        }
        if (event.getHitsplat() == lastHitsplat)
        {
            return;
        }
        lastHitsplat = event.getHitsplat();

        int damage = event.getHitsplat().getAmount();
        int drain = damage / 4;
        if (drain > 0)
        {
            smitedPrayer += drain;
            smiteActivated = true;
            lastTime = Instant.now();
        }
    }

    // Fanger opp projectile, for spells som har projectile
    @Subscribe
    public void onProjectileMoved(ProjectileMoved event)
    {
        Projectile projectile = event.getProjectile();
        if (projectile == null)
        {
            return;
        }
        Actor target = projectile.getInteracting();
        if (target == null)
        {
            return;
        }
        if (target == client.getLocalPlayer())
        {
            int pid = projectile.getId();
            String found = SpellInfo.determineSpellName(pid, -1, -1);
            if (!found.isEmpty())
            {
                setCurrentSpellName(found);
            }
        }
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        if (client.getLocalPlayer() != null && client.getLocalPlayer().getInteracting() != null)
        {
            lastTime = Instant.now();
        }
        if (client.isPrayerActive(Prayer.SMITE))
        {
            smiteActivated = true;
        }
        if (lastOpponent != null && lastTime != null)
        {
            long sec = Duration.between(lastTime, Instant.now()).toSeconds();
            if (sec > config.overlayDisplayDuration())
            {
                lastOpponent = null;
                smitedPrayer = 0;
                smiteActivated = false;
            }
        }
    }

    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded event)
    {
        if (event.getType() != MenuAction.NPC_SECOND_OPTION.getId()
                || !event.getOption().equals("Attack")
                || !config.showOpponentsInMenu())
        {
            return;
        }
        NPC npc = event.getMenuEntry().getNpc();
        if (npc == null)
        {
            return;
        }
        MenuEntry[] entries = client.getMenuEntries();
        entries[entries.length - 1].setTarget("*" + entries[entries.length - 1].getTarget());
        client.setMenuEntries(entries);
    }

    @Subscribe
    public void onScriptPostFired(ScriptPostFired event)
    {
        if (event.getScriptId() == ScriptID.HP_HUD_UPDATE)
        {
            updateBossHealthBarText();
        }
    }

    private void updateBossHealthBarText()
    {
        Widget hpText = client.getWidget(WidgetInfo.HEALTH_OVERLAY_BAR_TEXT);
        if (hpText == null)
        {
            return;
        }
        final int currHp = client.getVarbitValue(Varbits.BOSS_HEALTH_CURRENT);
        final int maxHp = client.getVarbitValue(Varbits.BOSS_HEALTH_MAXIMUM);
        if (maxHp <= 0)
        {
            return;
        }
        switch (config.hitpointsDisplayStyle())
        {
            case PERCENTAGE:
                hpText.setText(getPercentText(currHp, maxHp));
                break;
            case BOTH:
                hpText.setText(hpText.getText() + " (" + getPercentText(currHp, maxHp) + ")");
                break;
        }
    }

    private static String getPercentText(int current, int maximum)
    {
        double percent = 100.0 * current / maximum;
        return PERCENT_FORMAT.format(percent) + "%";
    }
}
