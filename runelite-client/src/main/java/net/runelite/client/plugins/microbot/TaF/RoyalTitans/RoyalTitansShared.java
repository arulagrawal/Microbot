package net.runelite.client.plugins.microbot.TaF.RoyalTitans;

import net.runelite.api.Skill;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.misc.Rs2Potion;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

import java.util.List;

public class RoyalTitansShared {
    public static final Integer ICE_TITAN_ID = 14147;
    public static final Integer FIRE_TITAN_DEAD_ID = 14148;
    public static final Integer ICE_TITAN_DEAD_ID = 14149;
    public static final Integer FIRE_TITAN_ID = 12596;
    public static final int BOSS_REGION = 11669;
    // Fixes attempts at double looting or yo-yo-ing between the dead titans
    public static boolean LootedTitanLastIteration = false;

    public static void evaluateAndConsumePotions(RoyalTitansConfig config) {
        int threshold = config.boostedStatsThreshold();

        if (!isCombatPotionActive(threshold)) {
            consumePotion(Rs2Potion.getCombatPotionsVariants());
            if (Rs2Player.drinkCombatPotionAt(Skill.STRENGTH)) {
                Rs2Player.waitForAnimation();
            }
            if (Rs2Player.drinkCombatPotionAt(Skill.ATTACK)) {
                Rs2Player.waitForAnimation();
            }
            if (Rs2Player.drinkCombatPotionAt(Skill.DEFENCE)) {
                Rs2Player.waitForAnimation();
            }
        }

        if (!isRangingPotionActive(threshold)) {
            consumePotion(Rs2Potion.getRangePotionsVariants());
        }
    }

    public static boolean isInBossRegion() {
        return Rs2Player.getWorldLocation().getRegionID() == BOSS_REGION;
    }

    private static boolean isCombatPotionActive(int threshold) {
        return Rs2Player.hasDivineCombatActive() || (Rs2Player.hasAttackActive(threshold) && Rs2Player.hasStrengthActive(threshold));
    }

    private static boolean isRangingPotionActive(int threshold) {
        return Rs2Player.hasRangingPotionActive(threshold) || Rs2Player.hasDivineBastionActive() || Rs2Player.hasDivineRangedActive();
    }

    private static void consumePotion(List<String> keyword) {
        var potion = Rs2Inventory.get(keyword.toArray(String[]::new));
        if (potion != null) {
            Rs2Inventory.interact(potion, "Drink");
            Rs2Player.waitForAnimation(1200);
        }
    }
}
