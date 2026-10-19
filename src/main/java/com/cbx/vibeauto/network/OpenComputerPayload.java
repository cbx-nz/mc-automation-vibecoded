package com.cbx.vibeauto.network;

import com.cbx.vibeauto.VibeAutomation;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenComputerPayload(BlockPos pos, String code, boolean running, String status, int interval) implements CustomPacketPayload {
    public static final Type<OpenComputerPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(VibeAutomation.MODID, "open_computer"));

    public static final StreamCodec<FriendlyByteBuf, OpenComputerPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, OpenComputerPayload::pos,
            ByteBufCodecs.STRING_UTF8, OpenComputerPayload::code,
            ByteBufCodecs.BOOL, OpenComputerPayload::running,
            ByteBufCodecs.STRING_UTF8, OpenComputerPayload::status,
            ByteBufCodecs.INT, OpenComputerPayload::interval,
            OpenComputerPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
