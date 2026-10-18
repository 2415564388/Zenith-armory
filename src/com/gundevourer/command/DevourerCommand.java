package com.gundevourer.command;

import com.gundevourer.GunDevourerMod;
import com.gundevourer.event.DevourBonusHandler;
import com.gundevourer.event.GunDamageHandler;
import com.gundevourer.item.GunDevourerItem;
import com.gundevourer.registry.ModItems;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;

import java.util.Optional;

/**
 * 测试命令：/gundevourer set <数量>
 * 把玩家饰品栏/背包里第一个天顶军械库的吞噬数设为指定值（0 即清空）。
 * 使用 gundevourer:test_n 形式的假枪械 ID 填充，方便测试 9/368 阈值。
 */
@Mod.EventBusSubscriber(modid = GunDevourerMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DevourerCommand {
    private static final int MAX_COUNT = 4096;

    private DevourerCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.m_82127_("gundevourer")
                .requires(source -> source.m_6761_(2))
                .then(Commands.m_82127_("set")
                        .then(Commands.m_82129_("count", IntegerArgumentType.integer(0, MAX_COUNT))
                                .executes(DevourerCommand::setCount))));
    }

    private static int setCount(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        int count = IntegerArgumentType.getInteger(context, "count");
        ServerPlayer player = source.m_81375_();

        if (applyCount(player, count)) {
            GunDamageHandler.invalidateCount(player);
            DevourBonusHandler.refreshPercentModifier(player);
            source.m_288197_(() -> Component.m_237110_("command.gundevourer.set", count), true);
            return Command.SINGLE_SUCCESS;
        }
        source.m_81352_(Component.m_237115_("command.gundevourer.no_item"));
        return 0;
    }

    private static boolean applyCount(ServerPlayer player, int count) {
        // 1. 优先修改已装备在 Curios 里的宝珠（加成立即生效）
        Optional<ICuriosItemHandler> handlerOpt = CuriosApi.getCuriosInventory(player).resolve();
        if (handlerOpt.isPresent()) {
            ICuriosItemHandler handler = handlerOpt.get();
            Optional<SlotResult> found = handler.findFirstCurio(ModItems.GUN_DEVOURER.get());
            if (found.isPresent()) {
                SlotResult result = found.get();
                ItemStack updated = result.stack().m_41777_();
                writeCount(updated, count);
                handler.setEquippedCurio(result.slotContext().identifier(),
                        result.slotContext().index(), updated);
                return true;
            }
        }

        // 2. 背包/副手/主手
        Inventory inventory = player.m_150109_();
        for (int i = 0; i < inventory.m_6643_(); i++) {
            ItemStack stack = inventory.m_8020_(i);
            if (stack.m_41720_() == ModItems.GUN_DEVOURER.get()) {
                writeCount(stack, count);
                inventory.m_6596_();
                return true;
            }
        }
        return false;
    }

    private static void writeCount(ItemStack stack, int count) {
        CompoundTag tag = stack.m_41784_();
        ListTag list = new ListTag();
        for (int i = 1; i <= count; i++) {
            list.add(StringTag.m_129297_("gundevourer:test_" + i));
        }
        tag.m_128365_(GunDevourerItem.TAG_DEVOURED, list);
    }
}
