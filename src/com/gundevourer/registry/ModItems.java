package com.gundevourer.registry;

import com.gundevourer.GunDevourerMod;
import com.gundevourer.item.GunDevourerItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, GunDevourerMod.MOD_ID);

    public static final RegistryObject<Item> GUN_DEVOURER =
            ITEMS.register("gun_devourer", () -> new GunDevourerItem(new Item.Properties().m_41503_(1)));

    private ModItems() {
    }
}
