package com.cbx.vibeauto.lang.vm;

import com.cbx.vibeauto.lang.compiler.CompiledProgram;
import com.cbx.vibeauto.lang.compiler.OpCode;

import java.util.*;

public class VirtualMachine {
    private final CompiledProgram program;
    private final Map<String, Object> globals = new HashMap<>();
    private final Map<String, NativeFunction> nativeFunctions = new HashMap<>();
    private final Deque<Object> stack = new ArrayDeque<>();
    private final Deque<Integer> callStack = new ArrayDeque<>();

    private int ip = 0;
    private VMState state = VMState.IDLE;
    private String lastError = null;
    private long wakeUpTick = 0;

    public VirtualMachine(CompiledProgram program) {
        this.program = program;
    }

    public void registerNative(String name, NativeFunction fn) {
        nativeFunctions.put(name, fn);
    }

    public void setGlobal(String name, Object value) {
        globals.put(name, value);
    }

    public Object getGlobal(String name) {
        return globals.get(name);
    }

    public VMState getState() {
        return state;
    }

    public String getLastError() {
        return lastError;
    }

    public long getWakeUpTick() {
        return wakeUpTick;
    }

    public void setWakeUpTick(long tick) {
        this.wakeUpTick = tick;
        this.state = VMState.SLEEPING;
    }

    public void wake() {
        if (this.state == VMState.SLEEPING) {
            this.state = VMState.RUNNING;
        }
    }

    public void callFunction(String name) {
        Integer addr = program.getFunctionAddress(name);
        if (addr != null) {
            this.ip = addr;
            this.state = VMState.RUNNING;
        }
    }

    public VMState step(int instructionBudget) {
        if (state == VMState.HALTED || state == VMState.ERROR || state == VMState.SLEEPING) {
            return state;
        }

        state = VMState.RUNNING;
        int instructionsExecuted = 0;
        List<CompiledProgram.Instruction> instructions = program.getInstructions();

        while (instructionsExecuted < instructionBudget && ip < instructions.size() && state == VMState.RUNNING) {
            CompiledProgram.Instruction inst = instructions.get(ip++);
            instructionsExecuted++;

            try {
                executeInstruction(inst);
            } catch (Exception e) {
                this.state = VMState.ERROR;
                this.lastError = "Line " + inst.line() + ": " + e.getMessage();
                return state;
            }
        }

        if (state == VMState.RUNNING) {
            if (ip >= instructions.size()) {
                state = VMState.HALTED;
            } else {
                state = VMState.YIELDED;
            }
        }

        return state;
    }

    private void executeInstruction(CompiledProgram.Instruction inst) {
        switch (inst.op()) {
            case CONST -> {
                Object val = program.getConstants().get(inst.arg());
                stack.push(val != null ? val : NullVal.INSTANCE);
            }
            case POP -> {
                if (!stack.isEmpty()) stack.pop();
            }
            case DUP -> {
                if (!stack.isEmpty()) stack.push(stack.peek());
            }
            case LOAD_VAR -> {
                String varName = (String) program.getConstants().get(inst.arg());
                Object val = globals.getOrDefault(varName, NullVal.INSTANCE);
                stack.push(val);
            }
            case STORE_VAR -> {
                String varName = (String) program.getConstants().get(inst.arg());
                Object val = stack.isEmpty() ? NullVal.INSTANCE : stack.pop();
                globals.put(varName, val);
            }
            case JUMP -> {
                ip = inst.arg();
            }
            case JUMP_IF_FALSE -> {
                Object cond = stack.isEmpty() ? false : stack.pop();
                if (!isTruthy(cond)) {
                    ip = inst.arg();
                }
            }
            case ADD -> {
                Object b = unwrap(stack.pop());
                Object a = unwrap(stack.pop());
                if (a instanceof String || b instanceof String) {
                    stack.push(String.valueOf(a) + String.valueOf(b));
                } else if (a instanceof Number na && b instanceof Number nb) {
                    if (a instanceof Double || b instanceof Double || a instanceof Float || b instanceof Float) {
                        stack.push(na.doubleValue() + nb.doubleValue());
                    } else {
                        stack.push(na.longValue() + nb.longValue());
                    }
                } else {
                    stack.push(String.valueOf(a) + String.valueOf(b));
                }
            }
            case SUB -> {
                Object b = unwrap(stack.pop());
                Object a = unwrap(stack.pop());
                if (a instanceof Number na && b instanceof Number nb) {
                    if (a instanceof Double || b instanceof Double) {
                        stack.push(na.doubleValue() - nb.doubleValue());
                    } else {
                        stack.push(na.longValue() - nb.longValue());
                    }
                } else {
                    stack.push(0L);
                }
            }
            case MUL -> {
                Object b = unwrap(stack.pop());
                Object a = unwrap(stack.pop());
                if (a instanceof Number na && b instanceof Number nb) {
                    if (a instanceof Double || b instanceof Double) {
                        stack.push(na.doubleValue() * nb.doubleValue());
                    } else {
                        stack.push(na.longValue() * nb.longValue());
                    }
                } else {
                    stack.push(0L);
                }
            }
            case DIV -> {
                Object b = unwrap(stack.pop());
                Object a = unwrap(stack.pop());
                if (a instanceof Number na && b instanceof Number nb) {
                    if (nb.doubleValue() == 0) {
                        throw new ArithmeticException("Division by zero");
                    }
                    if (a instanceof Double || b instanceof Double) {
                        stack.push(na.doubleValue() / nb.doubleValue());
                    } else {
                        stack.push(na.longValue() / nb.longValue());
                    }
                } else {
                    stack.push(0L);
                }
            }
            case MOD -> {
                Object b = unwrap(stack.pop());
                Object a = unwrap(stack.pop());
                if (a instanceof Number na && b instanceof Number nb) {
                    stack.push(na.longValue() % nb.longValue());
                } else {
                    stack.push(0L);
                }
            }
            case NEG -> {
                Object a = unwrap(stack.pop());
                if (a instanceof Number na) {
                    if (a instanceof Double || a instanceof Float) stack.push(-na.doubleValue());
                    else stack.push(-na.longValue());
                } else {
                    stack.push(0L);
                }
            }
            case NOT -> {
                Object a = stack.pop();
                stack.push(!isTruthy(a));
            }
            case EQ -> {
                Object b = unwrap(stack.pop());
                Object a = unwrap(stack.pop());
                stack.push(Objects.equals(a, b));
            }
            case NEQ -> {
                Object b = unwrap(stack.pop());
                Object a = unwrap(stack.pop());
                stack.push(!Objects.equals(a, b));
            }
            case LT -> {
                Object b = unwrap(stack.pop());
                Object a = unwrap(stack.pop());
                if (a instanceof Number na && b instanceof Number nb) {
                    stack.push(na.doubleValue() < nb.doubleValue());
                } else {
                    stack.push(false);
                }
            }
            case LTE -> {
                Object b = unwrap(stack.pop());
                Object a = unwrap(stack.pop());
                if (a instanceof Number na && b instanceof Number nb) {
                    stack.push(na.doubleValue() <= nb.doubleValue());
                } else {
                    stack.push(false);
                }
            }
            case GT -> {
                Object b = unwrap(stack.pop());
                Object a = unwrap(stack.pop());
                if (a instanceof Number na && b instanceof Number nb) {
                    stack.push(na.doubleValue() > nb.doubleValue());
                } else {
                    stack.push(false);
                }
            }
            case GTE -> {
                Object b = unwrap(stack.pop());
                Object a = unwrap(stack.pop());
                if (a instanceof Number na && b instanceof Number nb) {
                    stack.push(na.doubleValue() >= nb.doubleValue());
                } else {
                    stack.push(false);
                }
            }
            case CALL -> {
                String fnName = (String) program.getConstants().get(inst.arg());
                NativeFunction nativeFn = nativeFunctions.get(fnName);
                if (nativeFn != null) {
                    // For native calls, we pass the arguments that were pushed
                    List<Object> args = new ArrayList<>();
                    // Native functions can read directly from stack or args
                    Object res = nativeFn.call(this, args);
                    stack.push(res != null ? res : NullVal.INSTANCE);
                } else {
                    Integer addr = program.getFunctionAddress(fnName);
                    if (addr != null) {
                        callStack.push(ip);
                        ip = addr;
                    } else {
                        throw new RuntimeException("Unknown function '" + fnName + "'");
                    }
                }
            }
            case RETURN -> {
                Object retVal = stack.isEmpty() ? NullVal.INSTANCE : stack.pop();
                if (!callStack.isEmpty()) {
                    ip = callStack.pop();
                    stack.push(retVal);
                } else {
                    state = VMState.HALTED;
                }
            }
            case GET_PROP -> {
                String propName = (String) program.getConstants().get(inst.arg());
                Object target = unwrap(stack.pop());
                Object propVal = resolveProperty(target, propName);
                stack.push(propVal != null ? propVal : NullVal.INSTANCE);
            }
            case CALL_METHOD -> {
                String methodName = (String) program.getConstants().get(inst.arg());
                // In our compiler: callee is pushed, then args
                // We resolve method invocation on the target
                Object target = unwrap(stack.pop());
                Object res = invokeMethod(target, methodName);
                stack.push(res != null ? res : NullVal.INSTANCE);
            }
            case HALT -> {
                state = VMState.HALTED;
            }
        }
    }

    public Object popStack() {
        return stack.isEmpty() ? NullVal.INSTANCE : stack.pop();
    }

    public void pushStack(Object val) {
        stack.push(val != null ? val : NullVal.INSTANCE);
    }

    private Object resolveProperty(Object target, String prop) {
        if (target instanceof Map<?, ?> map) {
            return map.get(prop);
        }
        if (target instanceof DeviceRef dev) {
            return dev.getProperty(prop);
        }
        if ("item".equals(target)) {
            return "item." + prop;
        }
        if (target instanceof String s && s.startsWith("item.")) {
            return s + "." + prop;
        }
        return NullVal.INSTANCE;
    }

    private Object invokeMethod(Object target, String method) {
        if (target instanceof DeviceRef dev) {
            return dev.invokeMethod(this, method);
        }
        if (target instanceof SerialRef s) {
            return s.invokeMethod(this, method);
        }
        return NullVal.INSTANCE;
    }

    public static boolean isTruthy(Object obj) {
        if (obj == null || obj == NullVal.INSTANCE) return false;
        if (obj instanceof Boolean b) return b;
        if (obj instanceof Number n) return n.doubleValue() != 0;
        if (obj instanceof String s) return !s.isEmpty();
        return true;
    }

    public static Object unwrap(Object obj) {
        return obj == NullVal.INSTANCE ? null : obj;
    }

    public enum NullVal { INSTANCE }

    public interface DeviceRef {
        Object getProperty(String name);
        Object invokeMethod(VirtualMachine vm, String method);
    }

    public interface SerialRef {
        Object invokeMethod(VirtualMachine vm, String method);
    }
}
