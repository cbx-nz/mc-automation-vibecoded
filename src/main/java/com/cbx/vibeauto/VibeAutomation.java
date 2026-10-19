package com.cbx.vibeauto;

import com.cbx.vibeauto.block.ComputerBlock;
import com.cbx.vibeauto.block.SerialReceiverBlock;
import com.cbx.vibeauto.blockentity.ComputerBlockEntity;
import com.cbx.vibeauto.blockentity.SerialReceiverBlockEntity;
import com.cbx.vibeauto.client.VibeAutomationClient;
import com.cbx.vibeauto.network.OpenComputerPayload;
import com.cbx.vibeauto.network.OpenReceiverPayload;
import com.cbx.vibeauto.network.UpdateScriptPayload;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

@Mod(VibeAutomation.MODID)
public class VibeAutomation {
    public static final String MODID = "vibeautomation";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // Blocks
    public static final DeferredBlock<ComputerBlock> COMPUTER_BLOCK = BLOCKS.registerBlock(
            "computer",
            ComputerBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0f).requiresCorrectToolForDrops()
    );

    public static final DeferredBlock<SerialReceiverBlock> SERIAL_RECEIVER_BLOCK = BLOCKS.registerBlock(
            "serial_receiver",
            SerialReceiverBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0f).requiresCorrectToolForDrops()
    );

    // Items
    public static final DeferredItem<BlockItem> COMPUTER_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("computer", COMPUTER_BLOCK);
    public static final DeferredItem<BlockItem> SERIAL_RECEIVER_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("serial_receiver", SERIAL_RECEIVER_BLOCK);
    public static final DeferredItem<com.cbx.vibeauto.item.CoordinateWandItem> COORDINATE_WAND = ITEMS.registerItem(
            "coordinate_wand",
            com.cbx.vibeauto.item.CoordinateWandItem::new,
            new Item.Properties()
    );

    // Block Entities
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ComputerBlockEntity>> COMPUTER_BE = BLOCK_ENTITIES.register(
            "computer",
            () -> BlockEntityType.Builder.of(ComputerBlockEntity::new, COMPUTER_BLOCK.get()).build(null)
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SerialReceiverBlockEntity>> SERIAL_RECEIVER_BE = BLOCK_ENTITIES.register(
            "serial_receiver",
            () -> BlockEntityType.Builder.of(SerialReceiverBlockEntity::new, SERIAL_RECEIVER_BLOCK.get()).build(null)
    );

    // Creative Tab
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> AUTOMATION_TAB = CREATIVE_MODE_TABS.register("automation_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.vibeautomation"))
            .withTabsBefore(CreativeModeTabs.REDSTONE_BLOCKS)
            .icon(() -> COMPUTER_BLOCK_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(COMPUTER_BLOCK_ITEM.get());
                output.accept(SERIAL_RECEIVER_BLOCK_ITEM.get());
                output.accept(COORDINATE_WAND.get());
            }).build());

    public VibeAutomation(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerPayloads);

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("VibeAutomation initialized");
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1.0");

        registrar.playToClient(
                OpenComputerPayload.TYPE,
                OpenComputerPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> VibeAutomationClient.handleOpenComputer(payload))
        );

        registrar.playToClient(
                OpenReceiverPayload.TYPE,
                OpenReceiverPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> VibeAutomationClient.handleOpenReceiver(payload))
        );

        registrar.playToServer(
                UpdateScriptPayload.TYPE,
                UpdateScriptPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    var player = context.player();
                    if (player.level().getBlockEntity(payload.pos()) instanceof ComputerBlockEntity be) {
                        be.updateAndRun(payload.code(), payload.run(), payload.interval());
                    }
                })
        );
    }
}
