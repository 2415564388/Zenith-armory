package com.gundevourer.client;

import net.minecraft.client.gui.screens.Screen;

/**
 * 仅客户端加载的辅助类。公共代码必须通过 FMLEnvironment.dist 判断后再调用，
 * 避免专用服务端加载 net.minecraft.client.* 导致 NoClassDefFoundError。
 */
public final class ClientTooltipHelper {
    private ClientTooltipHelper() {
    }

    public static boolean isShiftDown() {
        return Screen.m_96638_();
    }
}
