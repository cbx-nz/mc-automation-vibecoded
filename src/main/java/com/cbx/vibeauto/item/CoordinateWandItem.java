package com.cbx.vibeauto.item;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class CoordinateWandItem extends Item {
    public CoordinateWandItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    /**
     * Called when a player left-clicks (attacks/hits) a block with the wand.
     * In Minecraft, canAttackBlock returning false cancels block breaking and handles hit logic.
     */
    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        if (!level.isClientSide) {
            String blockName = state.getBlock().getName().getString();
            player.sendSystemMessage(Component.literal("§b[MC Automation] §fTarget: §e" + blockName 
                    + " §fat §a(" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")"));
        }
        return false; // Prevent breaking the block when clicked
    }

    /**
     * Also support right-click on block as a convenience.
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();

        if (!level.isClientSide && player != null) {
            BlockState state = level.getBlockState(pos);
            String blockName = state.getBlock().getName().getString();
            player.sendSystemMessage(Component.literal("§b[MC Automation] §fTarget: §e" + blockName 
                    + " §fat §a(" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")"));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
