package com.cbx.vibeauto.blockentity;

import com.cbx.vibeauto.VibeAutomation;
import com.cbx.vibeauto.lang.ast.Stmt;
import com.cbx.vibeauto.lang.compiler.Compiler;
import com.cbx.vibeauto.lang.compiler.CompiledProgram;
import com.cbx.vibeauto.lang.parser.Parser;
import com.cbx.vibeauto.lang.token.Lexer;
import com.cbx.vibeauto.lang.vm.VMState;
import com.cbx.vibeauto.lang.vm.VirtualMachine;
import com.cbx.vibeauto.network.OpenComputerPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class ComputerBlockEntity extends BlockEntity {
    private String sourceCode = """
        void setup() {
            serial.Begin();
            serial.output("Computer initialized");
        }

        void loop() {
            // Write your automation logic here
        }
        """;
    private boolean isRunning = false;
    private int loopInterval = 20; // Default 1 second (20 ticks), configurable by player!
    private long tickCounter = 0;
    private String statusMessage = "IDLE";

    private final List<String> serialLog = new ArrayList<>();
    private final Map<String, BlockPos> namedDevices = new HashMap<>();

    private VirtualMachine vm = null;
    private CompiledProgram compiledProgram = null;

    public ComputerBlockEntity(BlockPos pos, BlockState blockState) {
        super(VibeAutomation.COMPUTER_BE.get(), pos, blockState);
    }

    public void openGui(ServerPlayer player) {
        player.connection.send(new OpenComputerPayload(
                this.getBlockPos(),
                this.sourceCode,
                this.isRunning,
                this.statusMessage,
                this.loopInterval
        ));
    }

    public void updateAndRun(String code, boolean run, int interval) {
        this.sourceCode = code;
        this.loopInterval = Math.max(1, interval);
        setChanged();

        if (run) {
            startExecution();
        } else {
            stopExecution();
        }
    }

    public void startExecution() {
        this.isRunning = false;
        this.statusMessage = "COMPILING...";

        Lexer lexer = new Lexer(sourceCode);
        var tokens = lexer.tokenize();
        if (lexer.hasErrors()) {
            this.statusMessage = "LEX ERROR: " + lexer.getErrors().get(0);
            addSerialLog(statusMessage);
            return;
        }

        Parser parser = new Parser(tokens);
        Stmt.Program ast = parser.parse();
        if (parser.hasErrors()) {
            this.statusMessage = "PARSE ERROR: " + parser.getErrors().get(0);
            addSerialLog(statusMessage);
            return;
        }

        Compiler compiler = new Compiler();
        this.compiledProgram = compiler.compile(ast);
        if (compiler.hasErrors()) {
            this.statusMessage = "COMPILE ERROR: " + compiler.getErrors().get(0);
            addSerialLog(statusMessage);
            return;
        }

        this.vm = new VirtualMachine(compiledProgram);
        bindNatives(this.vm);

        // Run setup() once
        this.vm.callFunction("setup");
        VMState state = this.vm.step(10000);
        if (state == VMState.ERROR) {
            this.statusMessage = "SETUP ERROR: " + vm.getLastError();
            addSerialLog(statusMessage);
            return;
        }

        this.isRunning = true;
        this.statusMessage = "RUNNING";
        this.tickCounter = 0;
        addSerialLog("Program started (loop every " + loopInterval + " ticks)");
    }

    public void stopExecution() {
        this.isRunning = false;
        this.vm = null;
        this.statusMessage = "STOPPED";
        addSerialLog("Program stopped");
    }

    public void tickServer() {
        if (!isRunning || vm == null || level == null) return;
        tickCounter++;

        if (vm.getState() == VMState.SLEEPING) {
            if (tickCounter >= vm.getWakeUpTick()) {
                vm.wake();
            } else {
                return;
            }
        }

        if (tickCounter % loopInterval == 0) {
            vm.callFunction("loop");
            VMState state = vm.step(10000); // 10k instruction budget per tick
            if (state == VMState.ERROR) {
                this.statusMessage = "RUNTIME ERROR: " + vm.getLastError();
                addSerialLog(statusMessage);
                this.isRunning = false;
            }
        }
    }

    private void bindNatives(VirtualMachine vm) {
        // Builtin serial object
        vm.setGlobal("serial", (VirtualMachine.SerialRef) (machine, method) -> {
            if ("Begin".equalsIgnoreCase(method)) {
                return null;
            }
            if ("output".equalsIgnoreCase(method) || "warning".equalsIgnoreCase(method) || "error".equalsIgnoreCase(method)) {
                Object msg = machine.popStack();
                String formatted = "[" + method.toUpperCase() + "] " + String.valueOf(msg);
                addSerialLog(formatted);
                broadcastToReceivers(formatted);
                return null;
            }
            return VirtualMachine.NullVal.INSTANCE;
        });

        // Builtin 'item' root for paths like item.minecraft.raw_iron
        vm.setGlobal("item", "item");

        // Native function: device(name) or device(relativeX, relativeY, relativeZ)
        vm.registerNative("device", (machine, args) -> {
            Object arg = machine.popStack();
            if (arg instanceof String name) {
                BlockPos targetPos = namedDevices.get(name);
                if (targetPos == null) {
                    // Try adjacent scan if name matches or relative default
                    targetPos = scanForDevice(name);
                }
                if (targetPos != null) {
                    return new DeviceHandle(level, targetPos, name);
                }
            }
            return new DeviceHandle(level, getBlockPos().relative(Direction.NORTH), String.valueOf(arg));
        });

        // Native function: input(type, x, y, z)
        vm.registerNative("input", (machine, args) -> {
            Object z = machine.popStack();
            Object y = machine.popStack();
            Object x = machine.popStack();
            machine.popStack(); // type
            int rx = x instanceof Number n ? n.intValue() : 0;
            int ry = y instanceof Number n ? n.intValue() : 0;
            int rz = z instanceof Number n ? n.intValue() : 0;
            BlockPos targetPos = getBlockPos().offset(rx, ry, rz);
            return new DeviceHandle(level, targetPos, "input(" + rx + "," + ry + "," + rz + ")");
        });

        // Native function: output(type, x, y, z)
        vm.registerNative("output", (machine, args) -> {
            Object z = machine.popStack();
            Object y = machine.popStack();
            Object x = machine.popStack();
            machine.popStack(); // type
            int rx = x instanceof Number n ? n.intValue() : 0;
            int ry = y instanceof Number n ? n.intValue() : 0;
            int rz = z instanceof Number n ? n.intValue() : 0;
            BlockPos targetPos = getBlockPos().offset(rx, ry, rz);
            return new DeviceHandle(level, targetPos, "output(" + rx + "," + ry + "," + rz + ")");
        });

        // Native function: transfer(item, from, to, amount = 64)
        vm.registerNative("transfer", (machine, args) -> {
            // In our compiler: callee is evaluated first, then args in call order
            // When popped from stack: last argument is top of stack!
            Object a4 = machine.popStack();
            Object a3 = machine.popStack();
            Object a2 = machine.popStack();

            Object itemObj;
            DeviceHandle from;
            DeviceHandle to;
            int amount = 64;

            if (a4 instanceof Number num) {
                amount = num.intValue();
                to = (DeviceHandle) a3;
                from = (DeviceHandle) a2;
                itemObj = machine.popStack();
            } else {
                to = (DeviceHandle) a4;
                from = (DeviceHandle) a3;
                itemObj = a2;
            }

            String itemStr = String.valueOf(itemObj);
            boolean success = DeviceHandle.transfer(from, to, itemStr, amount);
            return success;
        });

        // Native function: timestamp()
        vm.registerNative("timestamp", (machine, args) -> {
            if (level != null) {
                long time = level.getDayTime();
                long hours = (time / 1000 + 6) % 24;
                long minutes = (time % 1000) * 60 / 1000;
                return String.format("%02d:%02d", hours, minutes);
            }
            return "00:00";
        });

        // Native function: delay(ticks)
        vm.registerNative("delay", (machine, args) -> {
            Object val = machine.popStack();
            int ticks = val instanceof Number n ? n.intValue() : 20;
            vm.setWakeUpTick(tickCounter + ticks);
            return null;
        });
    }

    private BlockPos scanForDevice(String name) {
        // Search in a 5-block radius for blocks with inventories
        for (BlockPos p : BlockPos.betweenClosed(getBlockPos().offset(-5, -3, -5), getBlockPos().offset(5, 3, 5))) {
            if (p.equals(getBlockPos())) continue;
            var handler = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, p, Direction.UP);
            if (handler != null) {
                String blockName = level.getBlockState(p).getBlock().getDescriptionId();
                if (blockName.toLowerCase().contains(name.toLowerCase())) {
                    return p.immutable();
                }
            }
        }
        return null;
    }

    public void addSerialLog(String msg) {
        serialLog.add(msg);
        if (serialLog.size() > 100) {
            serialLog.remove(0);
        }
        setChanged();
    }

    public List<String> getSerialLog() {
        return serialLog;
    }

    private void broadcastToReceivers(String msg) {
        if (level == null) return;
        // Broadcast to any nearby serial receivers within 16 blocks
        for (BlockPos p : BlockPos.betweenClosed(getBlockPos().offset(-16, -8, -16), getBlockPos().offset(16, 8, 16))) {
            BlockEntity be = level.getBlockEntity(p);
            if (be instanceof SerialReceiverBlockEntity receiver) {
                receiver.addMessage(msg);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("SourceCode", sourceCode);
        tag.putBoolean("IsRunning", isRunning);
        tag.putInt("LoopInterval", loopInterval);
        tag.putString("StatusMessage", statusMessage);

        ListTag logTag = new ListTag();
        for (String msg : serialLog) {
            logTag.add(StringTag.valueOf(msg));
        }
        tag.put("SerialLog", logTag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("SourceCode")) sourceCode = tag.getString("SourceCode");
        if (tag.contains("IsRunning")) isRunning = tag.getBoolean("IsRunning");
        if (tag.contains("LoopInterval")) loopInterval = Math.max(1, tag.getInt("LoopInterval"));
        if (tag.contains("StatusMessage")) statusMessage = tag.getString("StatusMessage");

        serialLog.clear();
        if (tag.contains("SerialLog", Tag.TAG_LIST)) {
            ListTag list = tag.getList("SerialLog", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                serialLog.add(list.getString(i));
            }
        }

        if (isRunning) {
            startExecution();
        }
    }
}
