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
        int ch = 3;

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

        var _enum = (access & Opcodes.ACC_ENUM) == Opcodes.ACC_ENUM;
        if (_enum)
            access -= Opcodes.ACC_ENUM;
        else ch--;

        String objectType = record                                                      ? "-record   " :
                            _enum                                                       ? "-enum     " :
                            (access & Opcodes.ACC_INTERFACE)  == Opcodes.ACC_INTERFACE  ? "interface " :
                            (access & Opcodes.ACC_ANNOTATION) == Opcodes.ACC_ANNOTATION ? "@interface" :
                                                                                           "class    ";

        if (ch > 0)
            if (Main.verbose)
                IO.println("[+] public  %s %s".formatted(
                        objectType,
                        name
                ));

        if ((access & Opcodes.ACC_FINAL) == Opcodes.ACC_FINAL) {
            access -= Opcodes.ACC_FINAL;

            if (Main.verbose)
                IO.println("[+] mutable %s %s".formatted(objectType, name));
        } else if ((access & Opcodes.ACC_MANDATED) == Opcodes.ACC_MANDATED) {
            access -= Opcodes.ACC_MANDATED;

            if (Main.verbose)
                IO.println("[+] open %s %s".formatted(objectType, name));
        }

        if (
                (access & Opcodes.ACC_MODULE) == Opcodes.ACC_MODULE &&
                (access & Opcodes.ACC_OPEN) != Opcodes.ACC_OPEN
        )
            access |= Opcodes.ACC_OPEN;

        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public void visitPermittedSubclass(String permittedSubclass) {}
}
