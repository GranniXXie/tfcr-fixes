package com.tfcr.tfcrfixes;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * EnchantingWithTFC port support: TFC 4.2.9 items keep vanilla's default
 * `Item.getEnchantmentValue()` of 0, so the enchanting table refuses to offer
 * enchantments for any TFC armor/weapon even though they pass `isEnchantable`
 * (they are durable). The original 1.18-era ecosystem relied on TFC gear being
 * enchantable; this lookup restores that, scaled to the TFC metal ladder
 * (vanilla reference points: iron 9, diamond 10, gold 25).
 */
public final class EnchantabilityBootstrap {

    private static final Map<String, Integer> METAL_ENCHANTABILITY = new LinkedHashMap<>();
    private static final String[] ARMOR = {"helmet", "chestplate", "greaves", "boots"};
    private static final String[] WEAPONS = {"sword", "mace", "javelin"};
    private static volatile Map<Item, Integer> CACHE;

    static {
        METAL_ENCHANTABILITY.put("copper", 8);
        METAL_ENCHANTABILITY.put("bismuth_bronze", 10);
        METAL_ENCHANTABILITY.put("bronze", 10);
        METAL_ENCHANTABILITY.put("black_bronze", 10);
        METAL_ENCHANTABILITY.put("wrought_iron", 9);
        METAL_ENCHANTABILITY.put("steel", 12);
        METAL_ENCHANTABILITY.put("black_steel", 14);
        METAL_ENCHANTABILITY.put("blue_steel", 15);
        METAL_ENCHANTABILITY.put("red_steel", 15);
    }

    private EnchantabilityBootstrap() {}

    /** Enchantability for the item, or null to leave vanilla behavior. */
    public static Integer lookup(Item item) {
        Map<Item, Integer> cache = CACHE;
        if (cache == null) {
            cache = buildCache();
            CACHE = cache;
        }
        return cache.get(item);
    }

    private static Map<Item, Integer> buildCache() {
        Map<Item, Integer> map = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> metal : METAL_ENCHANTABILITY.entrySet()) {
            for (String piece : ARMOR) {
                add(map, "tfc:metal/" + piece + "/" + metal.getKey(), metal.getValue());
            }
            for (String weapon : WEAPONS) {
                add(map, "tfc:metal/" + weapon + "/" + metal.getKey(), metal.getValue());
            }
        }
        return map;
    }

    private static void add(Map<Item, Integer> map, String id, int value) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
        if (item != null && item != net.minecraft.world.item.Items.AIR) {
            map.put(item, value);
        }
    }
}
