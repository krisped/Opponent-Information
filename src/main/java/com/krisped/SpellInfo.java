package com.krisped;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpellInfo
{
    public static class Spell
    {
        private final String name;
        private final List<Integer> projectileIds;
        private final Integer animationSelf;
        private final Integer graphicSelf;

        public Spell(String name, List<Integer> projectileIds, Integer animationSelf, Integer graphicSelf)
        {
            this.name = name;
            this.projectileIds = projectileIds;
            this.animationSelf = animationSelf;
            this.graphicSelf = graphicSelf;
        }

        public String getName() { return name; }
        public List<Integer> getProjectileIds() { return projectileIds; }
        public Integer getAnimationSelf() { return animationSelf; }
        public Integer getGraphicSelf() { return graphicSelf; }
    }

    // Din eksisterende liste med spells
    public static final List<Spell> SPELLS = Arrays.asList(
            new Spell("Wind Strike", Arrays.asList(91), 711, 90),
            new Spell("Water Strike", Arrays.asList(94), 711, 93),
            new Spell("Earth Strike", Arrays.asList(97), 711, 96),
            new Spell("Fire Strike", Arrays.asList(100), 711, null),
            new Spell("Wind Bolt", Arrays.asList(117), 711, 117),
            new Spell("Water Bolt", Arrays.asList(121), 711, 120),
            new Spell("Earth Bolt", Arrays.asList(124), 711, 123),
            new Spell("Fire Bolt", Arrays.asList(127), 711, 1266),
            new Spell("Wind Blast", Arrays.asList(133), 711, 132),
            new Spell("Water Blast", Arrays.asList(136), 711, 135),
            new Spell("Earth Blast", Arrays.asList(139), 711, 138),
            new Spell("Fire Blast", Arrays.asList(130), 711, 129),
            new Spell("Wind Wave", Arrays.asList(159), 727, 158),
            new Spell("Water Wave", Arrays.asList(162), 727, 161),
            new Spell("Earth Wave", Arrays.asList(165), 727, 164),
            new Spell("Fire Wave", Arrays.asList(156), 727, 155),
            new Spell("Wind Surge", Arrays.asList(1456), 7855, 1455),
            new Spell("Water Surge", Arrays.asList(1459), 7855, 1458),
            new Spell("Earth Surge", Arrays.asList(1462), 7855, 1461),
            new Spell("Fire Surge", Arrays.asList(1465), 7855, 1464),
            new Spell("Entangle", Arrays.asList(178), 710, 177),
            new Spell("Tele Block", Arrays.asList(1300, 1299), 1819, null),
            new Spell("Ice Burst", Arrays.asList(366), 1979, null),
            new Spell("Ice Blitz", Collections.emptyList(), 1978, 366),
            new Spell("Ice Barrage", Arrays.asList(368), 1979, null),
            new Spell("Blood Blitz", Arrays.asList(374), 1978, null)
    );

    /**
     * Søker i spells med projectile IDs først,
     * deretter i spells uten (match på anim/gfx).
     */
    public static String determineSpellName(int projectileId, int animationTarget, int graphicSelf)
    {
        // 1) Sjekk spells med projectile
        for (Spell spell : SPELLS)
        {
            if (spell.getProjectileIds() != null && !spell.getProjectileIds().isEmpty())
            {
                if (spell.getProjectileIds().contains(projectileId))
                {
                    return spell.getName();
                }
            }
        }
        // 2) Sjekk spells uten projectile => match anim/gfx
        for (Spell spell : SPELLS)
        {
            if (spell.getProjectileIds() == null || spell.getProjectileIds().isEmpty())
            {
                if ((spell.getAnimationSelf() != null && spell.getAnimationSelf() == animationTarget)
                        || (spell.getGraphicSelf() != null && spell.getGraphicSelf() == graphicSelf))
                {
                    return spell.getName();
                }
            }
        }
        return "";
    }

    // Map: Spellnavn -> sprite ID (for “ICONS”–visning)
    private static final Map<String, Integer> SPELL_SPRITES = new HashMap<>();
    static {
        SPELL_SPRITES.put("Entangle", 321);
        SPELL_SPRITES.put("Wind Strike", 15);
        SPELL_SPRITES.put("Water Strike", 17);
        SPELL_SPRITES.put("Earth Strike", 19);
        SPELL_SPRITES.put("Fire Strike", 21);
        SPELL_SPRITES.put("Wind Bolt", 23);
        SPELL_SPRITES.put("Water Bolt", 26);
        SPELL_SPRITES.put("Earth Bolt", 29);
        SPELL_SPRITES.put("Fire Bolt", 32);

        SPELL_SPRITES.put("Wind Blast", 35);   // <-- lagt til
        SPELL_SPRITES.put("Water Blast", 38); // <-- lagt til
        SPELL_SPRITES.put("Earth Blast", 40); // <-- lagt til
        SPELL_SPRITES.put("Fire Blast", 44);  // <-- lagt til

        SPELL_SPRITES.put("Wind Surge", 362);
        SPELL_SPRITES.put("Water Surge", 363);
        SPELL_SPRITES.put("Earth Surge", 364);
        SPELL_SPRITES.put("Fire Surge", 365);

        SPELL_SPRITES.put("Wind Wave", 46);
        SPELL_SPRITES.put("Water Wave", 48);
        SPELL_SPRITES.put("Earth Wave", 51);
        SPELL_SPRITES.put("Fire Wave", 52);

        SPELL_SPRITES.put("Tele Block", 352);
        SPELL_SPRITES.put("Ice Barrage", 328);
        SPELL_SPRITES.put("Blood Barrage", 336);
        SPELL_SPRITES.put("Ice Blitz", 327);
        SPELL_SPRITES.put("Blood Blitz", 335);
        SPELL_SPRITES.put("Ice Burst", 326);
    }

    public static Integer getSpellSpriteId(String spellName)
    {
        return SPELL_SPRITES.get(spellName);
    }
}
