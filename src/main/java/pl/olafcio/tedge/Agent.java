package pl.olafcio.tedge;

import org.objectweb.asm.*;

import java.io.IOException;
import java.lang.instrument.*;
import java.util.Objects;

final class Agent {
    public static void premain(String args, Instrumentation inst) throws UnmodifiableClassException, ClassNotFoundException, IOException {
        var className = Class.class.getName();
        var classStream = ClassLoader.getSystemResourceAsStream(className.replace(".", "/") + ".class");

        var bytecode = classStream.readAllBytes();

        var reader = new ClassReader(bytecode);
        var writer = new ClassWriter(reader, 0);

        ClassVisitor visitor = writer;

        visitor = new IsEnumMod(Opcodes.ASM9, visitor);

        reader.accept(visitor, 0);
        bytecode = writer.toByteArray();

        inst.redefineClasses(new ClassDefinition(Class.class, bytecode));
    }

    public static class IsEnumMod extends ClassVisitor {
        public IsEnumMod(int api, ClassVisitor classVisitor) {
            super(api, classVisitor);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            var parent = super.visitMethod(access, name, descriptor, signature, exceptions);
            if (!Objects.equals(name, "isEnum"))
                return parent;

            return new MethodVisitor(api, parent) {
                @Override
                public void visitIntInsn(int opcode, int operand) {
                    super.visitIntInsn(opcode, operand == 0x00004000 ? -1 : operand);
                }
            };
        }
    }
}
