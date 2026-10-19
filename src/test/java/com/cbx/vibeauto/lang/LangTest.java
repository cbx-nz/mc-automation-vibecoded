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
}
