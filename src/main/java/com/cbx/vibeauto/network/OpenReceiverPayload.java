package com.cbx.vibeauto.network;

import com.cbx.vibeauto.VibeAutomation;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenReceiverPayload(BlockPos pos, String log) implements CustomPacketPayload {
    public static final Type<OpenReceiverPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(VibeAutomation.MODID, "open_receiver"));

    public static final StreamCodec<FriendlyByteBuf, OpenReceiverPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, OpenReceiverPayload::pos,
            ByteBufCodecs.STRING_UTF8, OpenReceiverPayload::log,
            OpenReceiverPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
