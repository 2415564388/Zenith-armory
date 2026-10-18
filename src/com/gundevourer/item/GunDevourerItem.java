package com.gundevourer.item;

import com.gundevourer.client.ClientTooltipHelper;
import com.gundevourer.config.GunDevourerConfig;
import com.gundevourer.event.DevourBonusHandler;
import com.gundevourer.event.GunDamageHandler;
import com.gundevourer.registry.ModItems;
import com.tacz.guns.api.item.IGun;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import javax.annotation.Nullable;
import java.util.List;

public class GunDevourerItem extends Item implements ICurioItem {
    public static final String TAG_DEVOURED = "DevouredGuns";

    public GunDevourerItem(Properties properties) {
        super(properties);
    }

    /**
     * 同一位玩家只能装备一个天顶军械库。
     * 正常装备时如果其他槽位已经有同名饰品则拒绝；同一槽位内移动 / 同一个 ItemStack 不算重复。
     */
    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        LivingEntity wearer = slotContext.entity();
        if (wearer == null) {
            return true;
        }
        return CuriosApi.getCuriosInventory(wearer).resolve()
                .map(handler -> handler.findCurios(ModItems.GUN_DEVOURER.get()).stream()
                        .noneMatch(result -> result.stack() != stack
                                && !sameSlot(result.slotContext(), slotContext)))
                .orElse(true);
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return canEquip(slotContext, stack);
    }

    private static boolean sameSlot(SlotContext a, SlotContext b) {
        return a.identifier().equals(b.identifier()) && a.index() == b.index();
    }

    /**
     * 鼠标拿着本物品，在任意容器里右键槽位中的 TaCZ 枪械时触发。
     */
    @Override
    public boolean m_142207_(ItemStack stack, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY || stack.m_41613_() != 1) {
            return false;
        }
        if (!slot.m_8010_(player)) {
            return false;
        }
        ItemStack target = slot.m_7993_();
        if (target.m_41619_()) {
            return false;
        }
        IGun gun = IGun.getIGunOrNull(target);
        if (gun == null) {
            return false;
        }
        ResourceLocation gunId = gun.getGunId(target);
        if (gunId == null) {
            return false;
        }

        CompoundTag tag = stack.m_41784_();
        ListTag devoured = tag.m_128437_(TAG_DEVOURED, 8);
        StringTag idTag = StringTag.m_129297_(gunId.toString());
        Level level = player.m_9236_();

        if (devoured.contains(idTag)) {
            if (!level.f_46443_) {
                level.m_247517_(player, player.m_20097_(), SoundEvents.f_12600_, SoundSource.PLAYERS);
                player.m_5661_(Component.m_237115_("message.gundevourer.already"), true);
            }
            return false;
        }

        // 消耗一支枪（兼容所有容器槽位），并记录枪械 ID
        slot.m_6201_(1);
        slot.m_6654_();
        devoured.add(idTag);
        tag.m_128365_(TAG_DEVOURED, devoured);

        if (!level.f_46443_) {
            int total = devoured.size();
            level.m_247517_(player, player.m_20097_(), SoundEvents.f_11783_, SoundSource.PLAYERS);
            player.m_5661_(Component.m_237110_("message.gundevourer.devoured",
                    total, total * GunDevourerConfig.FLAT_DAMAGE_PER_GUN.get()), true);
        }
        GunDamageHandler.invalidateCount(player);
        return true;
    }

    /**
     * 只取列表长度，避免每次命中/每帧 tooltip 都构造 Set。
     * 吞噬时已用 ListTag.contains 保证同一 ID 不重复。
     */
    public static int getDevouredCount(ItemStack stack) {
        if (stack.m_41619_() || !stack.m_41782_()) {
            return 0;
        }
        CompoundTag tag = stack.m_41783_();
        if (tag == null || !tag.m_128425_(TAG_DEVOURED, 9)) {
            return 0;
        }
        return tag.m_128437_(TAG_DEVOURED, 8).size();
    }

    @Override
    public void m_7373_(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        int count = getDevouredCount(stack);
        int flatPerGun = GunDevourerConfig.FLAT_DAMAGE_PER_GUN.get();
        int percent = DevourBonusHandler.getPercentBonus(count);
        int threshold = GunDevourerConfig.TRUE_DAMAGE_THRESHOLD.get();
        String truePercent = GunDevourerConfig.display(GunDevourerConfig.TRUE_DAMAGE_PERCENT.get());
        String minHealth = GunDevourerConfig.display(GunDevourerConfig.TRUE_DAMAGE_MIN_HEALTH.get());

        tooltip.add(Component.m_237115_("tooltip.gundevourer.simple"));
        tooltip.add(Component.m_237110_("tooltip.gundevourer.stats",
                count, count * flatPerGun, percent));
        if (count >= threshold) {
            tooltip.add(Component.m_237110_("tooltip.gundevourer.true.active",
                    truePercent, minHealth));
        } else {
            tooltip.add(Component.m_237110_("tooltip.gundevourer.true.progress",
                    count, threshold));
        }
        if (FMLEnvironment.dist == Dist.CLIENT && ClientTooltipHelper.isShiftDown()) {
            tooltip.add(Component.m_237115_("tooltip.gundevourer.detail.title"));
            tooltip.add(Component.m_237115_("tooltip.gundevourer.detail.1"));
            tooltip.add(Component.m_237115_("tooltip.gundevourer.detail.2"));
            tooltip.add(Component.m_237110_("tooltip.gundevourer.detail.3",
                    flatPerGun,
                    GunDevourerConfig.GUNS_PER_PERCENT_STAGE.get(),
                    GunDevourerConfig.PERCENT_PER_STAGE.get()));
            tooltip.add(Component.m_237115_("tooltip.gundevourer.detail.4"));
            tooltip.add(Component.m_237110_("tooltip.gundevourer.detail.5",
                    threshold, truePercent));
            tooltip.add(Component.m_237115_("tooltip.gundevourer.detail.6"));
            tooltip.add(Component.m_237115_("tooltip.gundevourer.detail.7"));
            tooltip.add(Component.m_237110_("tooltip.gundevourer.detail.8", minHealth));
        } else {
            tooltip.add(Component.m_237115_("tooltip.gundevourer.shift"));
        }
    }
}
