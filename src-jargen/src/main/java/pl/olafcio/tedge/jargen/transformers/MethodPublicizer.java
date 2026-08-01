package pl.olafcio.tedge.jargen.transformers;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import pl.olafcio.tedge.jargen.Main;

import java.util.Objects;

public class MethodPublicizer extends ClassVisitor {
    public MethodPublicizer(int api, ClassVisitor classVisitor) {
        super(api, classVisitor);
    }

    private String typeName;

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.typeName = name;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        if (!name.equals("<clinit>")) {
            print:
            {
                // Set as public
                if ((access & Opcodes.ACC_PRIVATE) == Opcodes.ACC_PRIVATE)
                    access = (access - Opcodes.ACC_PRIVATE) | Opcodes.ACC_PUBLIC;
                else if ((access & Opcodes.ACC_PROTECTED) == Opcodes.ACC_PROTECTED)
                    access = (access - Opcodes.ACC_PROTECTED) | Opcodes.ACC_PUBLIC;
                else if ((access & Opcodes.ACC_PUBLIC) != Opcodes.ACC_PUBLIC)
                    access |= Opcodes.ACC_PUBLIC;
                else break print;

                if (Main.verbose)
                    IO.println("[+] public  method     %s %s%s".formatted(typeName, name, Objects.requireNonNullElse(signature, "")));
            }

            if ((access & Opcodes.ACC_FINAL) == Opcodes.ACC_FINAL) {
                access -= Opcodes.ACC_FINAL;

                if (Main.verbose)
                    IO.println("[+] mutable method     %s %s%s".formatted(typeName, name, Objects.requireNonNullElse(signature, "")));
            }
        }

        return new MethodVisitor(Opcodes.ASM9, super.visitMethod(access, name, descriptor, signature, exceptions)) {
            @Override
            public void visitParameter(String name, int access) {
                if ((access & Opcodes.ACC_FINAL) == Opcodes.ACC_FINAL) {
                    access -= Opcodes.ACC_FINAL;

                    if (Main.verbose)
                        IO.println("[+] mutable parameter  %s %s%s -> %s".formatted(typeName, name, Objects.requireNonNullElse(signature, ""), name));
                }

                super.visitParameter(name, access);
            }
        };
    }
}
