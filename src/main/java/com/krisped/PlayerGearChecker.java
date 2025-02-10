package com.krisped;

import net.runelite.api.ChatMessageType;
import net.runelite.api.Player;
import net.runelite.api.kit.KitType;
import net.runelite.api.events.InteractingChanged;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.game.ItemManager;
import net.runelite.client.eventbus.Subscribe;
import javax.inject.Inject;
import java.text.NumberFormat;
import java.util.Locale;
import java.awt.Color;
import net.runelite.client.util.Text;

public class PlayerGearChecker
{
    @Inject
    private KPOpponentInfoConfig config;

    @Inject
    private ChatMessageManager chatMessageManager;

    @Inject
    private ItemManager itemManager;

    @Inject
    private KPOpponentInfoPlugin plugin;

    private long lastGearMessageTime = 0;

    @Subscribe
    public void onInteractingChanged(InteractingChanged event)
    {
        // Send chat-melding kun dersom riskDisplayOption er CHAT eller BOTH.
        if (config.riskDisplayOption() != KPOpponentInfoConfig.RiskDisplayOption.CHAT &&
                config.riskDisplayOption() != KPOpponentInfoConfig.RiskDisplayOption.BOTH)
        {
            return;
        }
        if (!(event.getTarget() instanceof Player))
        {
            return;
        }
        long now = System.currentTimeMillis();
        int cooldown = config.overlayDisplayDuration() * 1000;
        if (now - lastGearMessageTime < cooldown)
        {
            return;
        }
        lastGearMessageTime = now;
        Player opponent = (Player) event.getTarget();
        long totalWealth = 0;
        for (KitType kitType : KitType.values()) {
            int itemId = opponent.getPlayerComposition().getEquipmentId(kitType);
            if (itemId != -1) {
                long price = itemManager.getItemPrice(itemId);
                totalWealth += price;
            }
        }
        plugin.setRiskValue(totalWealth);
        String playerName = Text.removeTags(opponent.getName());
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        String formattedRisk = nf.format(totalWealth);
        String message = "<col=ffff00>Player " + playerName + " has a potential risk of " + formattedRisk + " GP</col>";
        chatMessageManager.queue(QueuedMessage.builder()
                .type(ChatMessageType.GAMEMESSAGE)
                .runeLiteFormattedMessage(message)
                .build());
    }
}
