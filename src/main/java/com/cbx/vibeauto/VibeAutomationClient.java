package com.cbx.vibeauto.client;

import com.cbx.vibeauto.VibeAutomation;
import com.cbx.vibeauto.client.screen.ComputerScreen;
import com.cbx.vibeauto.client.screen.SerialReceiverScreen;
import com.cbx.vibeauto.network.OpenComputerPayload;
import com.cbx.vibeauto.network.OpenReceiverPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@Mod(value = VibeAutomation.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = VibeAutomation.MODID, value = Dist.CLIENT)
public class VibeAutomationClient {
    public VibeAutomationClient(ModContainer container) {
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        VibeAutomation.LOGGER.info("VibeAutomation client setup complete");
    }

    public static void handleOpenComputer(OpenComputerPayload payload) {
        Minecraft.getInstance().setScreen(new ComputerScreen(
                payload.pos(),
                payload.code(),
                payload.running(),
                payload.status(),
                payload.interval()
        ));
    }

    public static void handleOpenReceiver(OpenReceiverPayload payload) {
        Minecraft.getInstance().setScreen(new SerialReceiverScreen(
                payload.pos(),
                payload.log()
        ));
    }
}
