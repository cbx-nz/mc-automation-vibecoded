package com.cbx.vibeauto.network;

import com.cbx.vibeauto.VibeAutomation;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record UpdateScriptPayload(BlockPos pos, String code, boolean run, int interval) implements CustomPacketPayload {
    public static final Type<UpdateScriptPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(VibeAutomation.MODID, "update_script"));

    public static final StreamCodec<FriendlyByteBuf, UpdateScriptPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, UpdateScriptPayload::pos,
            ByteBufCodecs.STRING_UTF8, UpdateScriptPayload::code,
            ByteBufCodecs.BOOL, UpdateScriptPayload::run,
            ByteBufCodecs.INT, UpdateScriptPayload::interval,
            UpdateScriptPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
