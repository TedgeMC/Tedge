package pl.olafcio.tedge.jargen.transformers;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;
import pl.olafcio.tedge.jargen.Main;

public class FieldPublicizer extends ClassVisitor {
    private String typeName;

    public FieldPublicizer(int api, ClassVisitor classVisitor) {
        super(api, classVisitor);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.typeName = name;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        int origAccess = access;

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
                IO.println("[+] public  field      %s %s".formatted(typeName, name));
        }

        if ((origAccess & Opcodes.ACC_PUBLIC) != Opcodes.ACC_PUBLIC) {
            // Remove final
            if ((access & Opcodes.ACC_FINAL) == Opcodes.ACC_FINAL) {
                access -= Opcodes.ACC_FINAL;

                if (Main.verbose)
                    IO.println("[+] mutable field      %s %s".formatted(typeName, name));
            }
        }

        return super.visitField(access, name, descriptor, signature, value);
    }
}
