package com.krisped;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Provides;
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
import net.runelite.client.hiscore.HiscoreManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

/**
 * Viser motstander-info i overlay.
 * Merk at overlay NÅ kun trigges ved hitsplat fra deg (isMine=true).
 */
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

    // Overlay for HP/stats/risiko
    @Inject
    private KPOpponentInfoOverlay overlay;

    // (Valgfri) Player-comparison overlay
    @Inject
    private KPPlayerComparisonOverlay comparisonOverlay;

    // Highlight
    @Inject
    private KPOpponentHighlight opponentHighlight;

    @Inject
    private EventBus eventBus;

    @Inject
    private PlayerGearChecker playerGearChecker;

    @Inject
    private HiscoreManager hiscoreManager;

    @Getter(AccessLevel.PACKAGE)
    private HiscoreEndpoint hiscoreEndpoint = HiscoreEndpoint.NORMAL;

    // For sist kjente fiende
    private Actor lastOpponent;
    public Actor getLastOpponent() {
        return lastOpponent;
    }

    @VisibleForTesting
    private Instant lastTime;

    // For SMITE
    private int smitedPrayer;
    public int getSmitedPrayer() {
        return smitedPrayer;
    }

    private boolean smiteActivated;
    public boolean isSmiteActivated() {
        return smiteActivated;
    }

    private Hitsplat lastHitsplat;

    // Våpen
    private String currentWeaponName;
    public String getCurrentWeaponName() {
        return currentWeaponName;
    }
    public void setCurrentWeaponName(String weapon) {
        this.currentWeaponName = weapon;
    }

    private String lastWeaponName;
    public String getLastWeaponName() {
        return lastWeaponName;
    }
    public void setLastWeaponName(String weapon) {
        this.lastWeaponName = weapon;
    }

    // For risk
    @Getter
    private long riskValue;
    public void setRiskValue(long riskValue) {
        this.riskValue = riskValue;
    }

    private boolean riskMessageSent;
    public boolean isRiskMessageSent() {
        return riskMessageSent;
    }
    public void setRiskMessageSent(boolean riskMessageSent) {
        this.riskMessageSent = riskMessageSent;
    }

    // For spells
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
        overlayManager.add(opponentHighlight);

        eventBus.register(playerGearChecker);
        eventBus.register(this);

        smitedPrayer = 0;
        smiteActivated = false;
        riskMessageSent = false;
        riskValue = 0;
        lastTime = null;
        lastOpponent = null;
    }

    @Override
    protected void shutDown() throws Exception
    {
        overlayManager.remove(overlay);
        overlayManager.remove(comparisonOverlay);
        overlayManager.remove(opponentHighlight);

        eventBus.unregister(playerGearChecker);
        eventBus.unregister(this);

        lastOpponent = null;
        lastTime = null;
        smitedPrayer = 0;
        smiteActivated = false;
        riskValue = 0;
        riskMessageSent = false;
        currentWeaponName = null;
        lastWeaponName = null;
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

    /**
     * onInteractingChanged brukes ikke til å sette motstander.
     * -> Vi vil IKKE vise overlay før vi faktisk har truffet fienden med en hitsplat.
     * Du kan evt. fjerne hele metoden, men her lar vi den stå tom.
     */
    @Subscribe
    public void onInteractingChanged(InteractingChanged event)
    {
        // Ingen oppdatering av lastOpponent her
        // Fordi vi vil ikke vise overlay bare ved "Attack" klikk
        // (Kun på hitsplat)
    }

    /**
     * Hovedmetoden: If hitsplat isMine => vi har truffet en fiende for første gang
     * => Sett lastOpponent = event.getActor().
     */
    @Subscribe
    public void onHitsplatApplied(HitsplatApplied event)
    {
        // 1) Hvis hitsplaten er fra deg (isMine), oppdater lastOpponent
        if (event.getHitsplat().isMine())
        {
            Actor targetActor = event.getActor();

            // Pass på at actor != local player
            if (targetActor != null && targetActor != client.getLocalPlayer())
            {
                if (targetActor != lastOpponent)
                {
                    // reset litt
                    smitedPrayer = 0;
                    smiteActivated = false;
                    riskMessageSent = false;
                    currentWeaponName = null;
                    lastWeaponName = null;
                    currentSpellName = "";
                }

                lastOpponent = targetActor;
                lastTime = Instant.now();
            }
        }

        // 2) Smite–logikk
        if (!client.isPrayerActive(Prayer.SMITE))
        {
            return;
        }
        if (event.getActor() == lastOpponent)
        {
            // Unngå dobbel-sjekk av samme hitsplat
            if (event.getHitsplat() != lastHitsplat)
            {
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
        }
    }

    /**
     * Fanger opp spells (bare for info).
     */
    @Subscribe
    public void onProjectileMoved(ProjectileMoved event)
    {
        Projectile projectile = event.getProjectile();
        if (projectile == null)
        {
            return;
        }
        Actor target = projectile.getInteracting();

        // Hvis target = localPlayer => setCurrentSpellName
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

    /**
     * onGameTick: fjerner overlay hvis
     * det har gått config.overlayDisplayDuration() sekunder siden sist
     * vi traff fienden med en hitsplat (eller smite).
     */
    @Subscribe
    public void onGameTick(GameTick event)
    {
        if (lastOpponent == null || lastTime == null)
        {
            return;
        }
        long secs = Duration.between(lastTime, Instant.now()).toSeconds();
        if (secs > config.overlayDisplayDuration())
        {
            lastOpponent = null;
            smitedPrayer = 0;
            smiteActivated = false;
        }
    }

    /**
     * onMenuEntryAdded: merk eventuelle fiender i menyen om config.showOpponentsInMenu().
     */
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
        // Hvis NPC er "vår" motstander => merk med *
        if (npc == lastOpponent || npc.getInteracting() == client.getLocalPlayer())
        {
            MenuEntry[] entries = client.getMenuEntries();
            entries[entries.length - 1].setTarget("*" + entries[entries.length - 1].getTarget());
            client.setMenuEntries(entries);
        }
    }

    @Subscribe
    public void onScriptPostFired(ScriptPostFired event)
    {
        // Oppdater boss overlay text
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
            default:
                // HITPOINTS => la vanilla UI vise HP
                break;
        }
    }

    private static String getPercentText(int current, int maximum)
    {
        double percent = 100.0 * current / maximum;
        return PERCENT_FORMAT.format(percent) + "%";
    }
}
