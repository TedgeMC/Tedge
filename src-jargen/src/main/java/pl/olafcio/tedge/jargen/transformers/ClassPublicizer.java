package pl.olafcio.tedge.jargen.transformers;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import pl.olafcio.tedge.jargen.Main;

public class ClassPublicizer extends ClassVisitor {
    public ClassPublicizer(int api, ClassVisitor classVisitor) {
        super(api, classVisitor);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        int ch = 2;

        if ((access & Opcodes.ACC_PROTECTED) == Opcodes.ACC_PROTECTED)
            access = (access - Opcodes.ACC_PROTECTED) | Opcodes.ACC_PUBLIC;
        else if ((access & Opcodes.ACC_PRIVATE) == Opcodes.ACC_PRIVATE)
            access = (access - Opcodes.ACC_PRIVATE) | Opcodes.ACC_PUBLIC;
        else if ((access & Opcodes.ACC_PUBLIC) != Opcodes.ACC_PUBLIC)
            access |= Opcodes.ACC_PUBLIC;
        else ch--;

        var record = (access & Opcodes.ACC_RECORD) == Opcodes.ACC_RECORD;
        if (record)
            access -= Opcodes.ACC_RECORD;
        else ch--;

        if (ch > 0)
            if (Main.verbose)
                IO.println("[+] public  %s %s".formatted(
                        record                                                      ? "-record   " :
                        (access & Opcodes.ACC_INTERFACE)  == Opcodes.ACC_INTERFACE  ? "interface " :
                        (access & Opcodes.ACC_ENUM)       == Opcodes.ACC_ENUM       ? "enum      " :
                        (access & Opcodes.ACC_ANNOTATION) == Opcodes.ACC_ANNOTATION ? "@interface" :
                                                                                      "class     ",
                        name
                ));

        if ((access & Opcodes.ACC_FINAL) == Opcodes.ACC_FINAL) {
            access -= Opcodes.ACC_FINAL;

            if (Main.verbose)
                IO.println("[+] mutable %s %s".formatted(
                        record                                                      ? "-record   " :
                        (access & Opcodes.ACC_INTERFACE)  == Opcodes.ACC_INTERFACE  ? "interface " :
                        (access & Opcodes.ACC_ENUM)       == Opcodes.ACC_ENUM       ? "enum      " :
                        (access & Opcodes.ACC_ANNOTATION) == Opcodes.ACC_ANNOTATION ? "@interface" :
                                                                                      "class     ",
                        name
                ));
        }

        super.visit(version, access, name, signature, superName, interfaces);
    }
}
