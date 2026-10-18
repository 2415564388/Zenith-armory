package com.gundevourer.compat;

import com.Polarice3.Goety.common.entities.boss.Apostle;
import com.mega.revelationfix.safe.entity.Apollyon2Interface;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.fml.ModList;
import z1gned.goetyrevelation.util.ApollyonAbilityHelper;

import javax.annotation.Nullable;

/**
 * 亚波伦（Goety Revelation / RevelationFix）兼容。
 *
 * 参考 abisburst_truedamage 的 ApollyonTrueDamageHandler：
 * - 冥界内亚波伦使用自定义血量 revelaionfix$get/setApollyonHealth
 * - 非冥界需要临时清零两个 hitCooldown，否则 setHealth 会被它的受击冷却挡住
 *
 * 这里是软兼容：mods 不在时不会调用到这些类。
 */
public final class ApollyonCompat {
    // RevelationFix 是 GoetyRevelation 的嵌套 JarJar，部分启动方式下
    // ModList 里的 modId 检测不可靠，因此只检查前两个；缺类时会由外层 try/catch 兜底。
    private static final boolean AVAILABLE = ModList.get().isLoaded("goety")
            && ModList.get().isLoaded("goety_revelation");

    private ApollyonCompat() {
    }

    public static boolean isAvailable() {
        return AVAILABLE;
    }

    public static boolean isApollyon(LivingEntity target) {
        return target instanceof Apostle
                && target instanceof ApollyonAbilityHelper helper
                && helper.allTitlesApostle_1_20_1$isApollyon();
    }

    /**
     * @return true 表示目标是亚波伦并且已经在这里处理完真伤
     */
    public static boolean applyTrueDamage(LivingEntity target, float amount, float minHealth,
                                          @Nullable EntityDataAccessor<Float> healthData) {
        if (!(target instanceof Apostle apostle)
                || !(target instanceof ApollyonAbilityHelper helper)
                || !(target instanceof Apollyon2Interface apollyon2)
                || !helper.allTitlesApostle_1_20_1$isApollyon()) {
            return false;
        }

        if (apostle.isInNether()) {
            float current = apollyon2.revelaionfix$getApollyonHealth();
            apollyon2.revelaionfix$setApollyonHealth(Math.max(minHealth, current - amount));
            return true;
        }

        int cooldown1 = apollyon2.revelaionfix$getHitCooldown();
        int cooldown2 = helper.allTitlesApostle_1_20_1$getHitCooldown();
        if (cooldown1 > 0) {
            apollyon2.revelaionfix$setHitCooldown(0);
        }
        if (cooldown2 > 0) {
            helper.allTitlesApostle_1_20_1$setHitCooldown(0);
        }

        float before = apostle.m_21223_();
        float after = Math.max(minHealth, before - amount);
        apostle.m_21153_(after);
        // 兜底：如果 setHealth 仍然被重写/限伤，直接写同步血量
        if (apostle.m_21223_() > after + 1.0E-3f && healthData != null) {
            apostle.m_20088_().m_135381_(healthData, after);
        }

        if (cooldown1 > 0) {
            apollyon2.revelaionfix$setHitCooldown(cooldown1);
        }
        if (cooldown2 > 0) {
            helper.allTitlesApostle_1_20_1$setHitCooldown(cooldown2);
        }
        return true;
    }
}
