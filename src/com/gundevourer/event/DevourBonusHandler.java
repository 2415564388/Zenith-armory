package com.gundevourer.event;

import com.gundevourer.GunDevourerMod;
import com.gundevourer.compat.ApollyonCompat;
import com.gundevourer.config.GunDevourerConfig;
import com.mojang.logging.LogUtils;
import com.tacz.guns.api.event.common.EntityHurtByGunEvent;
import mod.chloeprime.gunsmithlib.api.common.GunAttributes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.util.UUID;

/**
 * 按配置的种数阶梯提升枪械百分比伤害（走 gunsmithlib:bullet_damage 属性），
 * 以及达到配置阈值后解锁的绝对真伤（默认最低留 1 血，不直接击杀）。
 * 所有数值见 {@link GunDevourerConfig}。
 */
@Mod.EventBusSubscriber(modid = GunDevourerMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DevourBonusHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final UUID PERCENT_MODIFIER_ID =
            UUID.fromString("d4f0c9a2-3b1e-4c5a-9e7f-1a2b3c4d5e6f");

    /**
     * 明确免疫真伤的实体。可通过数据包扩展；默认包含测试假人、
     * 盔甲架，以及 feibiblade 的小菲比（hurt 被改成永远返回 false 的无敌实体）。
     */
    private static final TagKey<EntityType<?>> TRUE_DAMAGE_IMMUNE =
            TagKey.m_203882_(Registries.f_256939_,
                    new ResourceLocation(GunDevourerMod.MOD_ID, "true_damage_immune"));

    /**
     * LivingEntity 的同步血量字段（f_20961_）。部分 boss 重写了 setHealth，
     * 需要直接写这个字段才能造成“绝对真伤”。
     */
    private static final EntityDataAccessor<Float> DATA_HEALTH_ID = resolveDataHealthId();

    private DevourBonusHandler() {
    }

    /** 当前百分比加成（整数百分数，例如默认 9 种 -> 10）。 */
    public static int getPercentBonus(int devouredCount) {
        int perStage = GunDevourerConfig.GUNS_PER_PERCENT_STAGE.get();
        return (devouredCount / perStage) * GunDevourerConfig.PERCENT_PER_STAGE.get();
    }

    private static EntityDataAccessor<Float> resolveDataHealthId() {
        try {
            Field field = LivingEntity.class.getDeclaredField("f_20961_");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            EntityDataAccessor<Float> accessor = (EntityDataAccessor<Float>) field.get(null);
            return accessor;
        } catch (Throwable t) {
            System.err.println("[gundevourer] failed to resolve LivingEntity health data: " + t);
            return null;
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Player player = event.player;
        if (player.m_9236_().f_46443_ || player.f_19797_ % 20 != 0) {
            return;
        }
        refreshPercentModifier(player);
    }

    /**
     * 读取当前挂在玩家 bullet_damage 属性上的百分比修正（0 表示没有）。
     * 用于 PvP 关闭时把这部分加成从本次伤害里撤销。
     */
    public static double getActivePercent(Player player) {
        AttributeInstance instance = player.m_21051_(GunAttributes.BULLET_DAMAGE.get());
        if (instance == null) {
            return 0.0;
        }
        AttributeModifier modifier = instance.m_22111_(PERCENT_MODIFIER_ID);
        return modifier == null ? 0.0 : modifier.m_22218_();
    }

    /**
     * 立即刷新玩家身上的百分比伤害修正（命令/装备变化后调用）。
     */
    public static void refreshPercentModifier(Player player) {
        AttributeInstance instance = player.m_21051_(GunAttributes.BULLET_DAMAGE.get());
        if (instance == null) {
            return;
        }
        int percent = getPercentBonus(GunDamageHandler.getDevouredCount(player));
        AttributeModifier existing = instance.m_22111_(PERCENT_MODIFIER_ID);
        if (percent <= 0) {
            if (existing != null) {
                instance.m_22120_(PERCENT_MODIFIER_ID);
            }
            return;
        }
        double amount = percent / 100.0;
        if (existing != null && Math.abs(existing.m_22218_() - amount) < 1.0E-9) {
            return;
        }
        if (existing != null) {
            instance.m_22120_(PERCENT_MODIFIER_ID);
        }
        instance.m_22118_(new AttributeModifier(PERCENT_MODIFIER_ID,
                "gundevourer_percent_bonus", amount,
                AttributeModifier.Operation.MULTIPLY_TOTAL));
    }

    /**
     * 在 TaCZ 正常伤害结算之前施加真伤。
     * 真伤最低只把目标压到 1 点血，不负责击杀；
     * 最后一刀、死亡、掉落和击杀归属交给普通子弹伤害与原版死亡流程。
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onGunHurtPreTrueDamage(EntityHurtByGunEvent.Pre event) {
        if (event.getLogicalSide() != LogicalSide.SERVER) {
            return;
        }
        LivingEntity attacker = event.getAttacker();
        if (attacker == null) {
            return;
        }
        int count = GunDamageHandler.getDevouredCount(attacker);
        if (count < GunDevourerConfig.TRUE_DAMAGE_THRESHOLD.get()) {
            return;
        }
        Entity hurt = event.getHurtEntity();
        LivingEntity target;
        if (hurt instanceof LivingEntity living) {
            target = living;
        } else if (hurt instanceof PartEntity<?> part && part.getParent() instanceof LivingEntity parent) {
            target = parent;
        } else {
            return;
        }
        if (!target.m_6084_() || target.m_213877_()) {
            return;
        }
        // 玩家对玩家：尊重服务端 PvP 设置/队伍关系
        if (attacker instanceof Player attackerPlayer
                && target instanceof Player targetPlayer
                && !attackerPlayer.m_7099_(targetPlayer)) {
            return;
        }
        // 明确免疫真伤的实体（测试假人、盔甲架、小菲比等）
        if (target.m_6095_().m_204039_(TRUE_DAMAGE_IMMUNE)) {
            return;
        }
        // 车万女仆：有主女仆属于受保护 NPC，不给真伤；野生女仆正常吃
        if (isProtectedMaid(target)) {
            return;
        }
        // 不检查 isInvulnerableTo：boss 的无敌帧要能被真伤穿透。
        float damage = event.getBaseAmount();
        if (event.isHeadShot()) {
            damage *= event.getHeadshotMultiplier();
        }
        float amount = damage * (float) (GunDevourerConfig.TRUE_DAMAGE_PERCENT.get() / 100.0);
        if (amount <= 0.0f) {
            return;
        }
        applyTrueDamage(target, amount);
    }

    private static boolean isProtectedMaid(LivingEntity target) {
        if (!target.getClass().getName().startsWith("com.github.tartaricacid.touhoulittlemaid.")) {
            return false;
        }
        return target instanceof OwnableEntity ownable && ownable.m_21805_() != null;
    }

    /**
     * 直接扣血，绕过护甲、减伤、无敌帧和伤害上限；最低保留 1 点血。
     */
    private static void applyTrueDamage(LivingEntity target, float amount) {
        float minHealth = GunDevourerConfig.TRUE_DAMAGE_MIN_HEALTH.get().floatValue();
        float before = target.m_21223_();
        if (before <= minHealth) {
            return;
        }
        // 亚波伦特判：自定义冥界血量 + 受击冷却
        try {
            if (ApollyonCompat.isAvailable()
                    && ApollyonCompat.applyTrueDamage(target, amount, minHealth, DATA_HEALTH_ID)) {
                return;
            }
        } catch (Throwable t) {
            LOGGER.warn("[gundevourer] Apollyon compat failed", t);
        }

        // 真伤最多把目标打到配置的最低血量，默认不直接击杀。
        // 这样死亡/掉落完全交给原版与目标自己模组的正常死亡流程。
        float after = Math.max(minHealth, before - amount);
        target.m_21153_(after);

        // 某些 boss 重写 setHealth，直接同步血量兜底
        if (target.m_21223_() > after + 1.0E-3f && DATA_HEALTH_ID != null) {
            target.m_20088_().m_135381_(DATA_HEALTH_ID, after);
        }
    }
}
