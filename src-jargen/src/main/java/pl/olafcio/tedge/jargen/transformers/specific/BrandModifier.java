package pl.olafcio.tedge.jargen.transformers.specific;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import pl.olafcio.tedge.jargen.Main;

public class BrandModifier extends ClassVisitor {
    public BrandModifier(int api, ClassVisitor classVisitor) {
        super(api, classVisitor);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        return new MethodVisitor(api, super.visitMethod(access, name, descriptor, signature, exceptions)) {
            @Override
            public void visitLdcInsn(Object value) {
                super.visitLdcInsn(value == "vanilla" ? "Tedge" : value);
            }
        };
    }
}
