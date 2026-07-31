package pl.olafcio.tedge;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.objectweb.asm.*;
import org.yaml.snakeyaml.Yaml;
import pl.olafcio.accesseditors.AccessEditorLoader;
import pl.olafcio.tedge_mixin.MixinLoader;
import pl.olafcio.tedge_mixin.config.InjectorsConfig;
import pl.olafcio.tedge_mixin.config.MixinConfig;
import pl.olafcio.tedge_mixin.config.TedgeConfig;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.instrument.*;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.jar.JarFile;
import java.util.zip.ZipOutputStream;

final class Agent {
    public static void premain(String args, Instrumentation inst) throws UnmodifiableClassException, ClassNotFoundException, IOException {
        IO.println();

        Path modsDir;

        try {
            nativeTransformations(inst);

            ModList.mods = new HashMap<>();

            modsDir = Path.of("./mods");
            if (!Files.isDirectory(modsDir)) {
                Files.createDirectories(modsDir);
                return;
            }
        } catch (Exception e) {
            IO.println("[Tedge] ");
            IO.println("[Tedge] ");
            IO.println("[Tedge] ");
            IO.println("[Tedge] !@#@! EARLY PREMAIN FAILED !@#@!");
            IO.println("[Tedge] ");
            IO.println("[Tedge] ");
            IO.println("[Tedge] ");
            IO.println();

            e.printStackTrace();

            IO.println();

            throw e;
        }

        loadMods(modsDir, inst);

        IO.println("[Tedge]");
        IO.println("[Tedge] Loading %d mod%s:".formatted(ModList.mods.size(), ModList.mods.size() == 1 ? "" : "s"));

        var lengthOfIndex = String.valueOf(ModList.mods.size()).length();

        int index = 0;
        for (var mod : ModList.mods.values())
            IO.println("[Tedge]   #%d%s // %s".formatted(
                    ++index,
                    " ".repeat(lengthOfIndex - String.valueOf(index).length()),
                    mod.yml().get("name")
            ));

        if (!ModList.mods.isEmpty())
            IO.println("[Tedge]");
    }

    private static void loadMods(Path modsDir, Instrumentation inst) {
        Path transformedMods = Path.of("./.tedge/transformed-mods");

        try                   { Files.createDirectories(transformedMods);                                            }
        catch (IOException e) { throw new RuntimeException("Unable to create .tedge/transformed-mods directory", e); }

        final var accesseditors = new AccessEditorLoader();

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

                    // <----------------------------------->
                    // <--> MIXIN CONFIG IS LOADED HERE <-->
                    // <----------------------------------->
                    JsonObject mixinconfig;

                    try (var stream = jar.getInputStream(jar.getEntry(yml.get("id") + ".mixins.json"))) {
                        mixinconfig = new Gson().fromJson(
                                new String(stream.readAllBytes(), StandardCharsets.UTF_8),
                                JsonObject.class
                        );
                    }

                    // <-------------------------------------->
                    // <--> ACCESS EDITORS ARE LOADED HERE <-->
                    // <-------------------------------------->

                    var accesseditor = jar.getEntry("tedge.mod.accesseditor");
                    if (accesseditor != null) {
                        try (var stream = jar.getInputStream(accesseditor)) {
                            accesseditors.add(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
                        }
                    }

                    // <--------------------------------->
                    // <--> JARS ARE TRANSFORMED HERE <-->
                    // <--------------------------------->
                    var loader = new MixinLoader(inst);

                    var byteout = new ByteArrayOutputStream();
                    var jarout = new ZipOutputStream(byteout);

                    loader.addInjections(
                            new MixinConfig(
                                    mixinconfig.get("required").getAsBoolean(),
                                    mixinconfig.get("refmap").getAsString(),
                                    mixinconfig.get("package").getAsString(),
                                    GsonUtil.mapToArray(mixinconfig.getAsJsonArray("mixins"), JsonElement::getAsString, String[]::new),
                                    GsonUtil.mapToArray(mixinconfig.getAsJsonArray("client"), JsonElement::getAsString, String[]::new),
                                    mixinconfig.get("minVersion").getAsString(),
                                    new InjectorsConfig(
                                        mixinconfig.getAsJsonObject("injectors").get("defaultRequire").getAsInt()
                                    ),
                                    new TedgeConfig(
                                            yml.get("id") + "$"
                                    )
                            ),
                            jar,
                            jarout
                    );

                    jarout.close();

                    var transformedPath = transformedMods.resolve(mod.getFileName());

                    Files.write(transformedPath, byteout.toByteArray());

                    byteout.close();

                    jar = new JarFile(transformedPath.toFile());

                    // <---------------------------->
                    // <--> JARS ARE LOADED HERE <-->
                    // <---------------------------->

                    ModList.mods.put(mod, new Mod(yml, jar));

                    inst.appendToSystemClassLoaderSearch(jar);

                    loader.finishInjections();
                } catch (MalformedURLException e) {
                    throw new RuntimeException("Failed to load mod '%s'".formatted(mod.getFileName().toString()), e);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to operate mod '%s'".formatted(mod.getFileName().toString()), e);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException("Failed to enumerate mods", e);
        }

        // <--------------------------------------->
        // <--> ACCESS EDITORS ARE APPLIED HERE <-->
        // <--------------------------------------->

        accesseditors.apply(inst);
        IO.println("[Tedge] Loaded access editors");
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
