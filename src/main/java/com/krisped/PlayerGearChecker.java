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
        // Hvis Risk Display Option er NONE, gjør ingenting
        if (config.riskDisplayOption() == KPOpponentInfoConfig.RiskDisplayOption.NONE)
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
        for (KitType kitType : KitType.values())
        {
            int itemId = opponent.getPlayerComposition().getEquipmentId(kitType);
            if (itemId != -1)
            {
                long price = itemManager.getItemPrice(itemId);
                totalWealth += price;
            }
        }

        // Lagre risiko i pluginet for overlay-visning
        plugin.setRiskValue(totalWealth);

        // Send chatmelding dersom Risk Display Option er CHAT eller BOTH
        KPOpponentInfoConfig.RiskDisplayOption riskOption = config.riskDisplayOption();
        if (riskOption == KPOpponentInfoConfig.RiskDisplayOption.CHAT ||
                riskOption == KPOpponentInfoConfig.RiskDisplayOption.BOTH)
        {
            NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
            String formattedWealth = nf.format(totalWealth);
            String opponentName = opponent.getName();
            String message = "<col=ffff00>" + opponentName + " is risking about " + formattedWealth + " GP.";
            chatMessageManager.queue(QueuedMessage.builder()
                    .type(ChatMessageType.GAMEMESSAGE)
                    .runeLiteFormattedMessage(message)
                    .build());
        }
    }
}
