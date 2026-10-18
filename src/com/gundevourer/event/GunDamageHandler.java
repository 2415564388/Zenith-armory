package com.gundevourer.event;

import com.gundevourer.GunDevourerMod;
import com.gundevourer.config.GunDevourerConfig;
import com.gundevourer.item.GunDevourerItem;
import com.gundevourer.registry.ModItems;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.event.common.EntityHurtByGunEvent;
import com.tacz.guns.entity.EntityKineticBullet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = GunDevourerMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class GunDamageHandler {
    /**
     * TaCZ 的霰弹会把一发总伤害按弹丸数平摊（damageModifier = 1 / bullet_amount）。
     * 读取该系数，让 +N 也按弹丸数平摊，整发全中时总加成保持 +N。
     */
    private static final Field BULLET_DAMAGE_MODIFIER = findBulletDamageModifier();

    /**
     * 同一 tick 内同一佩戴者只扫描一次 Curios / NBT。
     * 霰弹一次 20 颗弹丸、高射速武器每 tick 多次命中时能显著降低开销。
     */
    private static final Map<LivingEntity, CachedCount> COUNT_CACHE = new WeakHashMap<>();

    private static final class CachedCount {
        final long tick;
        final int count;

        CachedCount(long tick, int count) {
            this.tick = tick;
            this.count = count;
        }
    }

    private GunDamageHandler() {
    }

    private static Field findBulletDamageModifier() {
        try {
            Field field = EntityKineticBullet.class.getDeclaredField("damageModifier");
            field.setAccessible(true);
            return field;
        } catch (Throwable ignored) {
            return null;
        }
    }

    @SubscribeEvent
    public static void onGunHurt(EntityHurtByGunEvent.Pre event) {
        if (event.getLogicalSide() != LogicalSide.SERVER) {
            return;
        }
        LivingEntity attacker = event.getAttacker();
        if (attacker == null) {
            return;
        }
        // 玩家对玩家且 PvP/队伍允许关系不成立：撤销本次命中里我们自己的百分比，
        // 并跳过固定加伤（基础子弹伤害仍由 TaCZ 自己处理）。
        if (attacker instanceof Player attackerPlayer
                && event.getHurtEntity() instanceof Player targetPlayer
                && !attackerPlayer.m_7099_(targetPlayer)) {
            double activePercent = DevourBonusHandler.getActivePercent(attackerPlayer);
            if (activePercent > 0.0) {
                event.setBaseAmount((float) (event.getBaseAmount() / (1.0 + activePercent)));
            }
            return;
        }
        int types = getDevouredCount(attacker);
        if (types <= 0) {
            return;
        }
        float pelletFactor = getPelletFactor(event);
        float flat = types * GunDevourerConfig.FLAT_DAMAGE_PER_GUN.get() * pelletFactor;
        float percent = DevourBonusHandler.getPercentBonus(types) / 100.0f;
        if (attacker instanceof Player) {
            // gunsmithlib 已经把百分比作用在枪械基础伤害上；这里把饰品自己的固定加伤
            // 也乘上同一个百分比，使总伤害等价于“（枪械基础伤害 + 饰品固定加伤）× (1 + 百分比)”。
            event.setBaseAmount(event.getBaseAmount() + flat * (1.0f + percent));
        } else {
            // 非玩家射手（例如车万女仆）没有我们维护的属性修正，
            // 直接对“基础伤害 + 固定加伤”整体乘百分比。
            event.setBaseAmount((event.getBaseAmount() + flat) * (1.0f + percent));
        }
    }

    /**
     * 每颗弹丸应分摊的加成系数：普通枪为 1，霰弹为 1/弹丸数。
     */
    private static float getPelletFactor(EntityHurtByGunEvent.Pre event) {
        Entity bullet = event.getBullet();
        if (bullet != null && BULLET_DAMAGE_MODIFIER != null) {
            try {
                float factor = BULLET_DAMAGE_MODIFIER.getFloat(bullet);
                if (factor > 0.0f && factor <= 1.0f) {
                    return factor;
                }
            } catch (Throwable ignored) {
                // 反射失败时走下面的枪械数据兜底
            }
        }
        ResourceLocation gunId = event.getGunId();
        if (gunId != null) {
            int amount = TimelessAPI.getCommonGunIndex(gunId)
                    .map(index -> index.getBulletData().getBulletAmount())
                    .orElse(1);
            return 1.0f / Math.max(1, amount);
        }
        return 1.0f;
    }

    /**
     * 只读取第一个装备中的天顶军械库；同一佩戴者只允许一个生效。
     * 支持玩家，也支持车万女仆等带 Curios 的 LivingEntity。
     */
    public static int getDevouredCount(LivingEntity wearer) {
        long now = wearer.m_9236_() == null ? 0L : wearer.m_9236_().m_46467_();
        CachedCount cached = COUNT_CACHE.get(wearer);
        if (cached != null && cached.tick == now) {
            return cached.count;
        }
        int count = CuriosApi.getCuriosInventory(wearer).resolve()
                .flatMap(handler -> handler.findFirstCurio(stack -> stack.m_41720_() == ModItems.GUN_DEVOURER.get()))
                .map(result -> GunDevourerItem.getDevouredCount(result.stack()))
                .orElse(0);
        COUNT_CACHE.put(wearer, new CachedCount(now, count));
        return count;
    }

    /** 命令等直接修改 NBT 后立即失效，避免同一 tick 内读到旧值。 */
    public static void invalidateCount(LivingEntity wearer) {
        COUNT_CACHE.remove(wearer);
    }
}
