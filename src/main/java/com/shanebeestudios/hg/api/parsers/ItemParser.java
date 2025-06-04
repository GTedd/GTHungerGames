package com.shanebeestudios.hg.api.parsers;

import com.shanebeestudios.hg.api.registry.Registries;
import com.shanebeestudios.hg.api.util.NBTApi;
import com.shanebeestudios.hg.api.util.Util;
import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.DyedItemColor;
import io.papermc.paper.datacomponent.item.ItemEnchantments;
import io.papermc.paper.datacomponent.item.PotionContents;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ItemParser {

    @SuppressWarnings({"UnstableApiUsage"})
    public static @Nullable ItemStack parseItem(@Nullable ConfigurationSection config) {
        if (config == null) return null;

        // ID
        String stringId = config.getString("id");
        if (stringId == null) return null;

        NamespacedKey key = NamespacedKey.fromString(stringId.toLowerCase(Locale.ROOT));
        if (key == null) return null;

        ItemType itemType = Registries.ITEM_TYPE_REGISTRY.get(key);
        if (itemType == null) return null;

        // COUNT
        int count = 1;
        if (config.contains("count")) {
            count = config.getInt("count");
        }
        ItemStack itemStack = itemType.createItemStack(count);

        // MAX_STACK_SIZE
        if (config.contains("max_stack_size")) {
            int maxStackSize = config.getInt("max_stack_size");
            itemStack.setData(DataComponentTypes.MAX_STACK_SIZE, Math.clamp(maxStackSize, 1, 99));
        }

        // NAME
        if (config.contains("custom_name")) {
            String name = config.getString("custom_name");
            itemStack.setData(DataComponentTypes.CUSTOM_NAME, Util.getMini(name));
        } else if (config.contains("item_name")) {
            String name = config.getString("item_name");
            itemStack.setData(DataComponentTypes.ITEM_NAME, Util.getMini(name));
        }

        // MODEL
        if (config.contains("item_model")) {
            String model = config.getString("item_model");
            assert model != null;
            NamespacedKey modelKey = NamespacedKey.fromString(model);
            if (modelKey == null) {
                Util.warning("Invalid item model: " + model);
            } else {
                itemStack.setData(DataComponentTypes.ITEM_MODEL, modelKey);
            }
        }

        // LORE
        if (config.contains("lore")) {
            List<String> lore = config.getStringList("lore");
            List<Component> loreComponents = new ArrayList<>();
            lore.forEach(l -> loreComponents.add(Util.getMini(l)));
            itemStack.lore(loreComponents);
        }

        // ENCHANTMENTS
        if (config.isConfigurationSection("enchantments")) {
            ConfigurationSection enchantments = config.getConfigurationSection("enchantments");
            assert enchantments != null;
            ItemEnchantments.Builder builder = ItemEnchantments.itemEnchantments();
            for (String eKey : enchantments.getKeys(false)) {
                NamespacedKey namespacedKey = NamespacedKey.fromString(eKey);
                if (namespacedKey != null) {
                    Enchantment enchantment = Registries.ENCHANTMENT_REGISTRY.get(namespacedKey);
                    if (enchantment != null) {
                        int level = enchantments.getInt(eKey);
                        builder.add(enchantment, level);
                    } else {
                        Util.warning("Invalid enchantment key '%s'", namespacedKey.toString());
                    }
                } else {
                    Util.warning("Invalid enchantment key '%s'", eKey);
                }

            }
            itemStack.setData(DataComponentTypes.ENCHANTMENTS, builder.build());
        }

        // MAX_DAMAGE
        if (config.contains("max_damage")) {
            int maxDamage = config.getInt("max_damage");
            itemStack.setData(DataComponentTypes.MAX_DAMAGE, maxDamage);
        }

        // DAMAGE
        if (config.contains("damage")) {
            int damage = config.getInt("damage");
            itemStack.setData(DataComponentTypes.DAMAGE, damage);
        }

        // POTION
        if (config.contains("potion")) {
            String string = config.getString("potion");
            assert string != null;
            NamespacedKey namespacedKey = NamespacedKey.fromString(string);
            if (namespacedKey != null) {
                PotionType potionType = Registries.POTION_TYPE_REGISTRY.get(namespacedKey);
                if (potionType != null) {
                    PotionContents.Builder builder = PotionContents.potionContents();
                    builder.potion(potionType);
                    itemStack.setData(DataComponentTypes.POTION_CONTENTS, builder.build());
                }
            }
        }

        // POTION_EFFECT
        if (config.isConfigurationSection("potion_effects")) {
            ConfigurationSection potionEffectsSection = config.getConfigurationSection("potion_effects");
            assert potionEffectsSection != null;
            PotionContents.Builder builder = PotionContents.potionContents();
            for (String peKey : potionEffectsSection.getKeys(false)) {
                PotionEffect potionEffect = parsePotionEffect(peKey, potionEffectsSection);
                if (potionEffect != null) {
                    builder.addCustomEffect(potionEffect);
                }
            }
            if (config.isInt("custom_color")) {
                int color = config.getInt("custom_color");
                builder.customColor(Color.fromRGB(color));
            }
            itemStack.setData(DataComponentTypes.POTION_CONTENTS, builder.build());
        }

        // DYED_COLOR
        if (config.contains("dyed_color")) {
            int color = config.getInt("dyed_color");
            itemStack.setData(DataComponentTypes.DYED_COLOR, DyedItemColor.dyedItemColor().color(Color.fromRGB(color)));
/*            if (!Util.RUNNING_1_21_5) {
                itemStack.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            }*/
        }

        // HIDDEN COMPONENTS
        if (Util.RUNNING_1_21_5 && config.isList("hidden_components")) {
            TooltipDisplay.Builder builder = TooltipDisplay.tooltipDisplay();
            for (String compKey : config.getStringList("hidden_components")) {
                NamespacedKey namespacedKey = NamespacedKey.fromString(compKey);
                if (namespacedKey == null) {
                    Util.warning("Invalid component key: " + compKey);
                    continue;
                }
                DataComponentType type = Registries.DATA_COMPONENT_TYPE_REGISTRY.get(namespacedKey);
                if (type == null) {
                    Util.warning("Invalid component key: " + compKey);
                    continue;
                }
                builder.addHiddenComponents(type);
            }
            itemStack.setData(DataComponentTypes.TOOLTIP_DISPLAY, builder.build());
        }

        // NBT
        if (config.contains("nbt")) {
            String nbtString = config.getString("nbt");
            NBTApi.applyNBTToItem(itemStack, nbtString);
        }

        return itemStack;
    }

    public static PotionEffect parsePotionEffect(String key, ConfigurationSection potionEffectsSection) {
        NamespacedKey namespacedKey = NamespacedKey.fromString(key);
        if (namespacedKey != null) {
            PotionEffectType potionEffectType = Registries.POTION_EFFECT_TYPE_REGISTRY.get(namespacedKey);
            ConfigurationSection potionEffectSection = potionEffectsSection.getConfigurationSection(key);
            assert potionEffectSection != null;
            if (potionEffectType != null) {
                int duration = potionEffectSection.getInt("duration", 300); // Default to 15 seconds
                int amplifier = potionEffectSection.getInt("amplifier", 0);
                boolean ambient = potionEffectSection.getBoolean("ambient", false);
                boolean show_icon = potionEffectSection.getBoolean("show_icon", true);
                boolean show_particles = potionEffectSection.getBoolean("show_particles", true);
                return new PotionEffect(potionEffectType, duration, amplifier, ambient, show_particles, show_icon);
            }
        }
        return null;
    }

}
