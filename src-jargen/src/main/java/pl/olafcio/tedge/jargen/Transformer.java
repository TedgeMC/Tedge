package pl.olafcio.tedge.jargen;

import org.jspecify.annotations.NullMarked;
import org.objectweb.asm.*;
import pl.olafcio.tedge.jargen.transformers.ClassPublicizer;
import pl.olafcio.tedge.jargen.transformers.MethodPublicizer;

import java.io.*;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

@NullMarked
public class Transformer {
    protected final ByteArrayOutputStream byteOut;
    protected final ZipOutputStream zipOut;

    public Transformer() {
        byteOut = new ByteArrayOutputStream();
        zipOut = new ZipOutputStream(byteOut);
    }

    public void transform(String arg) {
        try (var zip = new ZipFile(arg)) {
            var entries = zip.entries();

            do {
                var entry = entries.nextElement();
                var stream = zip.getInputStream(entry);

                handleEntry(entry, stream);
            } while (entries.hasMoreElements());
        } catch (IOException e) {
            throw new RuntimeException("Failed to open .jar file", e);
        } catch (NoSuchElementException e) {
            throw new RuntimeException("Empty .jar file", e);
        }

        try {
            zipOut.close();
            byteOut.close();
        } catch (IOException e) {
            throw new RuntimeException("Unable to close .jar file", e);
        }
    }

    protected void handleEntry(ZipEntry entry, InputStream stream) {
        if (entry.getName().endsWith(".class")) {
            transformClass(entry, stream);
        } else {
            try                   { write(entry, stream.readAllBytes());                               }
            catch (IOException e) { throw new RuntimeException("Failed to copy entry inside .jar", e); }
        }
    }

    protected void transformClass(ZipEntry entry, InputStream stream) {
        byte[] bytes;

        try                   { bytes = stream.readAllBytes();                                                  }
        catch (IOException e) { throw new RuntimeException("Failed to read .jar class: " + entry.getName(), e); }

        var reader = new ClassReader(bytes);
        var writer = new ClassWriter(reader, 0);

        ClassVisitor visitor = writer;

        visitor = new ClassPublicizer(Opcodes.ASM9, visitor);
        visitor = new MethodPublicizer(Opcodes.ASM9, visitor);

        transformClassFields(reader);

        reader.accept(visitor, 0);
        bytes = writer.toByteArray();

        try                   { write(entry, bytes);                                                           }
        catch (IOException e) { throw new RuntimeException("Failed to copy transformed class inside .jar", e); }
    }

    protected void transformClassFields(ClassReader reader) {
        var typeFields = new HashMap<String, Integer>();
        var typeFieldsB = new HashMap<String, Integer>();

        reader.accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                if (!descriptor.startsWith("[Ljava/lang"))
                    typeFields.put(descriptor, typeFields.getOrDefault(descriptor, 0) + 1);

                typeFieldsB.put(descriptor, typeFieldsB.getOrDefault(descriptor, 0) + 1);

                return super.visitField(access, name, descriptor, signature, value);
            }
        }, 0);

        reader.accept(new ClassVisitor(Opcodes.ASM9) {
            private String typeName;

            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                this.typeName = name;
                super.visit(version, access, name, signature, superName, interfaces);
            }

            @Override
            public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                if (typeFields.getOrDefault(descriptor, 0) <= 1) {
                    print: {
                        // Set as public
                        if ((access & Opcodes.ACC_PRIVATE) == Opcodes.ACC_PRIVATE)
                            access |= Opcodes.ACC_PRIVATE;
                        else if ((access & Opcodes.ACC_PROTECTED) == Opcodes.ACC_PROTECTED)
                            access |= Opcodes.ACC_PROTECTED;
                        else break print;

                        IO.println("[+] public field %s %s".formatted(typeName, name));
                    }
                }

                if (typeFieldsB.getOrDefault(descriptor, 0) == 1) {
                    // Remove final
                    if ((access & Opcodes.ACC_FINAL) == Opcodes.ACC_FINAL) {
                        access |= Opcodes.ACC_FINAL;
                        IO.println("[+] mutable field %s %s".formatted(typeName, name));
                    }
                }

                return super.visitField(access, name, descriptor, signature, value);
            }
        }, 0);
    }

    protected final void write(ZipEntry entry, byte[] data)
                    throws IOException
    {
        zipOut.putNextEntry(entry);
        zipOut.write(data);
    }

    public final void write(String path) {
        byte[] zipBytes = byteOut.toByteArray();

        try {
            var out = new FileOutputStream(path);

            out.write(zipBytes);
            out.close();
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Cannot write the transformation output to a directory", e);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write the transformation output", e);
        }
    }
}
