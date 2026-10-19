package com.cbx.vibeauto.blockentity;

import com.cbx.vibeauto.lang.vm.VirtualMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.Objects;

public class DeviceHandle implements VirtualMachine.DeviceRef {
    private final Level level;
    private final BlockPos pos;
    private final String name;
    private final int slotMin;
    private final int slotMax; // -1 means all slots

    public DeviceHandle(Level level, BlockPos pos, String name) {
        this(level, pos, name, 0, -1);
    }

    public DeviceHandle(Level level, BlockPos pos, String name, int slotMin, int slotMax) {
        this.level = level;
        this.pos = pos;
        this.name = name;
        this.slotMin = slotMin;
        this.slotMax = slotMax;
    }

    public BlockPos getPos() {
        return pos;
    }

    public String getName() {
        return name;
    }

    public IItemHandler getItemHandler() {
        if (level == null || pos == null) return null;
        return level.getCapability(Capabilities.ItemHandler.BLOCK, pos, Direction.UP);
    }

    public boolean containsItem(String itemStr) {
        return getItemCount(itemStr) > 0;
    }

    public int getItemCount(String itemStr) {
        IItemHandler handler = getItemHandler();
        if (handler == null) return 0;
        Item target = resolveItem(itemStr);
        if (target == null) return 0;

        int count = 0;
        int max = slotMax == -1 ? handler.getSlots() : Math.min(slotMax + 1, handler.getSlots());
        for (int i = slotMin; i < max; i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (stack.is(target)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    public boolean isFull() {
        IItemHandler handler = getItemHandler();
        if (handler == null) return true;
        int max = slotMax == -1 ? handler.getSlots() : Math.min(slotMax + 1, handler.getSlots());
        for (int i = slotMin; i < max; i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (stack.isEmpty() || stack.getCount() < stack.getMaxStackSize()) {
                return false;
            }
        }
        return true;
    }

    public boolean isEmpty() {
        IItemHandler handler = getItemHandler();
        if (handler == null) return true;
        int max = slotMax == -1 ? handler.getSlots() : Math.min(slotMax + 1, handler.getSlots());
        for (int i = slotMin; i < max; i++) {
            if (!handler.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public static Item resolveItem(String identifier) {
        if (identifier == null) return null;
        if (identifier.startsWith("item.")) {
            identifier = identifier.substring(5); // strip "item."
        }
        identifier = identifier.replace('.', ':');
        int firstColon = identifier.indexOf(':');
        if (firstColon != -1 && identifier.indexOf(':', firstColon + 1) != -1) {
            // in case multiple dots were converted, e.g. minecraft.raw_iron -> minecraft:raw_iron
            String namespace = identifier.substring(0, firstColon);
            String path = identifier.substring(firstColon + 1).replace(':', '_');
            identifier = namespace + ":" + path;
        } else if (!identifier.contains(":")) {
            identifier = "minecraft:" + identifier;
        }

        try {
            ResourceLocation rl = ResourceLocation.parse(identifier);
            return BuiltInRegistries.ITEM.get(rl);
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean transfer(DeviceHandle source, DeviceHandle dest, String itemStr, int amount) {
        if (source == null || dest == null) return false;
        IItemHandler srcHandler = source.getItemHandler();
        IItemHandler dstHandler = dest.getItemHandler();
        if (srcHandler == null || dstHandler == null) return false;

        Item target = resolveItem(itemStr);
        if (target == null) return false;

        int remaining = amount;
        int srcMax = source.slotMax == -1 ? srcHandler.getSlots() : Math.min(source.slotMax + 1, srcHandler.getSlots());

        for (int i = source.slotMin; i < srcMax && remaining > 0; i++) {
            ItemStack inSlot = srcHandler.getStackInSlot(i);
            if (inSlot.is(target)) {
                int toExtract = Math.min(remaining, inSlot.getCount());
                // Test simulate extract
                ItemStack simExtract = srcHandler.extractItem(i, toExtract, true);
                if (simExtract.isEmpty()) continue;

                // Test simulate insert
                ItemStack remainder = ItemHandlerHelper.insertItemStacked(dstHandler, simExtract, true);
                int accepted = simExtract.getCount() - remainder.getCount();

                if (accepted > 0) {
                    ItemStack realExtract = srcHandler.extractItem(i, accepted, false);
                    ItemHandlerHelper.insertItemStacked(dstHandler, realExtract, false);
                    remaining -= accepted;
                }
            }
        }

        return remaining < amount;
    }

    @Override
    public Object getProperty(String propName) {
        // Furnace-like sub slots
        if ("input".equalsIgnoreCase(propName)) {
            return new DeviceHandle(level, pos, name + ".input", 0, 0);
        }
        if ("fuel".equalsIgnoreCase(propName)) {
            return new DeviceHandle(level, pos, name + ".fuel", 1, 1);
        }
        if ("output".equalsIgnoreCase(propName)) {
            return new DeviceHandle(level, pos, name + ".output", 2, 2);
        }
        return VirtualMachine.NullVal.INSTANCE;
    }

    @Override
    public Object invokeMethod(VirtualMachine vm, String method) {
        if ("contains".equalsIgnoreCase(method)) {
            Object arg = vm.popStack();
            return containsItem(String.valueOf(arg));
        }
        if ("count".equalsIgnoreCase(method)) {
            Object arg = vm.popStack();
            return (long) getItemCount(String.valueOf(arg));
        }
        if ("is_full".equalsIgnoreCase(method) || "isFull".equalsIgnoreCase(method)) {
            return isFull();
        }
        if ("is_empty".equalsIgnoreCase(method) || "isEmpty".equalsIgnoreCase(method)) {
            return isEmpty();
        }
        return VirtualMachine.NullVal.INSTANCE;
    }
}
