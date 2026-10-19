package com.cbx.vibeauto.lang;

import com.cbx.vibeauto.lang.ast.Stmt;
import com.cbx.vibeauto.lang.compiler.Compiler;
import com.cbx.vibeauto.lang.compiler.CompiledProgram;
import com.cbx.vibeauto.lang.parser.Parser;
import com.cbx.vibeauto.lang.token.Lexer;
import com.cbx.vibeauto.lang.token.Token;
import com.cbx.vibeauto.lang.vm.VirtualMachine;
import com.cbx.vibeauto.lang.vm.VMState;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class LangTest {

    @Test
    public void testBasicArithmeticAndVariables() {
        String code = """
            void setup() {
                int a = 10;
                int b = 20;
                int c = a + b * 2;
                result = c;
            }
        """;

        Lexer lexer = new Lexer(code);
        List<Token> tokens = lexer.tokenize();
        assertFalse(lexer.hasErrors());

        Parser parser = new Parser(tokens);
        Stmt.Program ast = parser.parse();
        assertFalse(parser.hasErrors());

        Compiler compiler = new Compiler();
        CompiledProgram prog = compiler.compile(ast);

        VirtualMachine vm = new VirtualMachine(prog);
        vm.callFunction("setup");
        VMState state = vm.step(1000);

        assertEquals(VMState.HALTED, state);
        assertEquals(50L, vm.getGlobal("result"));
    }

    @Test
    public void testLoopsAndNativeOutput() {
        String code = """
            void setup() {
                int sum = 0;
                for (int i = 1; i <= 5; i = i + 1) {
                    sum = sum + i;
                }
                print(sum);
            }
        """;

        Lexer lexer = new Lexer(code);
        Parser parser = new Parser(lexer.tokenize());
        Compiler compiler = new Compiler();
        CompiledProgram prog = compiler.compile(parser.parse());

        List<Object> printed = new ArrayList<>();
        VirtualMachine vm = new VirtualMachine(prog);
        vm.registerNative("print", (machine, args) -> {
            printed.add(machine.popStack());
            return null;
        });

        vm.callFunction("setup");
        vm.step(5000);

        assertEquals(1, printed.size());
        assertEquals(15L, printed.get(0));
    }

    @Test
    public void testMethodCallWithArguments() {
        String code = """
            void setup() {
                serial.output("Hello World");
            }
        """;

        Lexer lexer = new Lexer(code);
        Parser parser = new Parser(lexer.tokenize());
        Compiler compiler = new Compiler();
        CompiledProgram prog = compiler.compile(parser.parse());

        List<String> outputLogs = new ArrayList<>();
        VirtualMachine vm = new VirtualMachine(prog);
        vm.setGlobal("serial", (VirtualMachine.SerialRef) (machine, method) -> {
            if ("output".equals(method)) {
                outputLogs.add(String.valueOf(machine.popStack()));
            }
            return null;
        });

        vm.callFunction("setup");
        vm.step(1000);

        assertEquals(1, outputLogs.size());
        assertEquals("Hello World", outputLogs.get(0));
    }

    @Test
    public void testChestTransferScript() {
        String code = """
            void setup() {
                serial.Begin();
                serial.output("Init");
            }

            void loop() {
                sourceChest = input("chest", -1, 0, 0);
                destChest = output("chest", 1, 0, 0);
                itemTarget = item.minecraft.iron_ingot;

                if (!destChest.is_full()) {
                    if (sourceChest.contains(itemTarget)) {
                        int count = sourceChest.count(itemTarget);
                        bool success = transfer(itemTarget, sourceChest, destChest, 16);
                        if (success) {
                            serial.output("Transferred successfully");
                        }
                    }
                }
            }
        """;

        Lexer lexer = new Lexer(code);
        Parser parser = new Parser(lexer.tokenize());
        Compiler compiler = new Compiler();
        CompiledProgram prog = compiler.compile(parser.parse());

        assertFalse(lexer.hasErrors());
        assertFalse(parser.hasErrors());
        assertFalse(compiler.hasErrors());

        VirtualMachine vm = new VirtualMachine(prog);
        List<String> logs = new ArrayList<>();
        vm.setGlobal("serial", (VirtualMachine.SerialRef) (machine, method) -> {
            if ("output".equals(method)) {
                logs.add(String.valueOf(machine.popStack()));
            }
            return null;
        });
        vm.setGlobal("item", "item");

        // Mock devices
        class MockDevice implements VirtualMachine.DeviceRef {
            int items = 32;
            @Override
            public Object getProperty(String name) { return VirtualMachine.NullVal.INSTANCE; }
            @Override
            public Object invokeMethod(VirtualMachine m, String method) {
                if ("is_full".equals(method)) return items >= 64;
                if ("contains".equals(method)) {
                    m.popStack(); // item
                    return items > 0;
                }
                if ("count".equals(method)) {
                    m.popStack(); // item
                    return (long) items;
                }
                return VirtualMachine.NullVal.INSTANCE;
            }
        }

        MockDevice src = new MockDevice();
        MockDevice dst = new MockDevice();

        vm.registerNative("input", (m, args) -> {
            m.popStack(); m.popStack(); m.popStack(); m.popStack();
            return src;
        });
        vm.registerNative("output", (m, args) -> {
            m.popStack(); m.popStack(); m.popStack(); m.popStack();
            return dst;
        });
        vm.registerNative("transfer", (m, args) -> {
            Object a4 = m.popStack();
            Object a3 = m.popStack();
            Object a2 = m.popStack();
            Object a1 = m.popStack();
            return true;
        });

        vm.callFunction("setup");
        vm.step(1000);
        assertEquals("Init", logs.get(0));

        vm.callFunction("loop");
        vm.step(1000);
        assertEquals(2, logs.size());
        assertEquals("Transferred successfully", logs.get(1));
    }
}
