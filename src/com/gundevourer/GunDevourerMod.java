package com.gundevourer;

import com.gundevourer.config.GunDevourerConfig;
import com.gundevourer.registry.ModItems;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(GunDevourerMod.MOD_ID)
public class GunDevourerMod {
    public static final String MOD_ID = "gundevourer";

    public GunDevourerMod() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, GunDevourerConfig.SPEC);
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.ITEMS.register(modBus);
        modBus.addListener(this::addCreative);
        // GunDamageHandler 通过 @Mod.EventBusSubscriber 自动注册
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if ("combat".equals(event.getTabKey().m_135782_().m_135815_())) {
            event.accept(ModItems.GUN_DEVOURER);
        }
    }
}
