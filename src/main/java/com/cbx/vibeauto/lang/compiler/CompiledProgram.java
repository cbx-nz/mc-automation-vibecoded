package com.cbx.vibeauto.lang.compiler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CompiledProgram {
    private final List<Instruction> instructions = new ArrayList<>();
    private final List<Object> constants = new ArrayList<>();
    private final Map<String, Integer> functionAddresses = new HashMap<>();

    public record Instruction(OpCode op, int arg, int line) {}

    public int addConstant(Object value) {
        for (int i = 0; i < constants.size(); i++) {
            if (java.util.Objects.equals(constants.get(i), value)) {
                return i;
            }
        }
        constants.add(value);
        return constants.size() - 1;
    }

    public int emit(OpCode op, int arg, int line) {
        instructions.add(new Instruction(op, arg, line));
        return instructions.size() - 1;
    }

    public void patch(int index, int newArg) {
        Instruction old = instructions.get(index);
        instructions.set(index, new Instruction(old.op(), newArg, old.line()));
    }

    public void registerFunction(String name, int address) {
        functionAddresses.put(name, address);
    }

    public Integer getFunctionAddress(String name) {
        return functionAddresses.get(name);
    }

    public List<Instruction> getInstructions() {
        return instructions;
    }

    public List<Object> getConstants() {
        return constants;
    }

    public Map<String, Integer> getFunctionAddresses() {
        return functionAddresses;
    }
}
