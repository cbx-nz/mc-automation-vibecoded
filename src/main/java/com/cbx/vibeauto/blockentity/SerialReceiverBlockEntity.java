package com.cbx.vibeauto.blockentity;

import com.cbx.vibeauto.VibeAutomation;
import com.cbx.vibeauto.network.OpenReceiverPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class SerialReceiverBlockEntity extends BlockEntity {
    private final List<String> messages = new ArrayList<>();

    public SerialReceiverBlockEntity(BlockPos pos, BlockState blockState) {
        super(VibeAutomation.SERIAL_RECEIVER_BE.get(), pos, blockState);
    }

    public void addMessage(String msg) {
        messages.add(msg);
        if (messages.size() > 100) {
            messages.remove(0);
        }
        setChanged();
    }

    public void openGui(ServerPlayer player) {
        String combined = String.join("\n", messages);
        player.connection.send(new OpenReceiverPayload(this.getBlockPos(), combined));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        for (String msg : messages) {
            list.add(StringTag.valueOf(msg));
        }
        tag.put("Messages", list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        messages.clear();
        if (tag.contains("Messages", Tag.TAG_LIST)) {
            ListTag list = tag.getList("Messages", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                messages.add(list.getString(i));
            }
        }
    }
}
