package dev.ozozorz.enhancementorderhints;

import com.mojang.blaze3d.platform.InputConstants;
import dev.ozozorz.enhancementorderhints.config.ConfigManager;
import dev.ozozorz.enhancementorderhints.screen.PlanListScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 注册客户端配置、快捷键和开发环境下的 Mixin 验证。
 */
public final class EnhancementOrderHintsClient implements ClientModInitializer {
    public static final String MOD_ID = "enhancement-order-hints";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        ConfigManager.load();
        verifyAnvilMixinInDevelopment();
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(MOD_ID, "configuration"));
        KeyMapping openConfig = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.enhancement-order-hints.open_config",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_N,
                category
        ));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openConfig.consumeClick()) {
                // 仅在正常游戏画面响应快捷键，避免抢占聊天框和其他文本输入控件的 N 键。
                if (client.screen == null) {
                    client.setScreen(new PlanListScreen(null));
                }
            }
        });
        LOGGER.info("Enhancement Order Hints 已加载");
    }

    /**
     * 在开发环境提前加载铁砧界面，以便启动时尽早发现失效的 Mixin。
     */
    private static void verifyAnvilMixinInDevelopment() {
        if (!FabricLoader.getInstance().isDevelopmentEnvironment()) return;
        try {
            Class.forName("net.minecraft.client.gui.screens.inventory.AnvilScreen", false,
                    EnhancementOrderHintsClient.class.getClassLoader());
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("无法验证铁砧界面 Mixin", exception);
        }
    }

    /**
     * 从 ModMenu 等入口打开配置界面，并保留调用方作为返回界面。
     *
     * @param parent 关闭配置界面后返回的父界面
     */
    public static void openConfig(net.minecraft.client.gui.screens.Screen parent) {
        Minecraft.getInstance().setScreen(new PlanListScreen(parent));
    }
}
