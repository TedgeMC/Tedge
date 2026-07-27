package pl.olafcio.tedge;

import org.objectweb.asm.*;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.lang.instrument.*;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.jar.JarFile;

final class Agent {
    public static void premain(String args, Instrumentation inst) throws UnmodifiableClassException, ClassNotFoundException, IOException {
        nativeTransformations(inst);

        ModList.mods = new HashMap<>();

        var modsDir = Path.of("./mods");
        if (!Files.isDirectory(modsDir)) {
            Files.createDirectories(modsDir);
            return;
        }

        try (var mods = Files.list(modsDir)) {
            mods.forEach(mod -> {
                try {
                    var jar = new JarFile(mod.toFile());
                    var modYaml = jar.getInputStream(jar.getEntry("tedge.mod.yaml"));

                    if (modYaml == null) {
                        IO.println("[WARNING] Invalid mod '%s'".formatted(mod.getFileName().toString()));
                        return;
                    }

                    Map<String, Object> yml = new Yaml().load(modYaml);

                    modYaml.close();

                    ModList.mods.put(mod, new Mod(yml, jar));
                } catch (MalformedURLException e) {
                    throw new RuntimeException("Failed to load mod '%s'".formatted(mod.getFileName().toString()), e);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to operate mod '%s'".formatted(mod.getFileName().toString()), e);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException("Failed to enumerate mods", e);
        }
    }

    private static void nativeTransformations(Instrumentation inst) throws IOException, ClassNotFoundException, UnmodifiableClassException {
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
