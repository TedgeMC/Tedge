package pl.olafcio.tedge.jargen;

import org.jspecify.annotations.NullMarked;
import pl.olafcio.tedge.jargen.classtransformer.ClassTransformer;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.NoSuchElementException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static java.lang.classfile.ClassFile.*;

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

        var reader = new ClassTransformer(bytes);

        reader.expect("CA FE BA BE");
        reader.expect("00 00 00");

        // Major Version
        int major = reader.consume();
        if (major > 69)
            throw new RuntimeException("Unsupported class file major version '" + major + "'");

        // Minor Version
        int minor = reader.consume();
        if (major == 69 && minor != 0)
            throw new RuntimeException("Unsupported class file minor version '" + minor + "'");

        String Def = "";

        var typeMod = reader.consume();

        if ((typeMod & 0x32) == 0x32) {
            // Final
            Def += " record";
        } else if ((typeMod & 0x10) == 0x10) {
            // Class
            Def += " class";
        } else if ((typeMod & 0x09) == 0x09) {
            // Annotation
            Def += " @interface";
        } else if ((typeMod & 0x2A) == 0x2A) {
            // Enum
            Def += " enum";
        } else if ((typeMod & 0x07) == 0x07) {
            // Interface
            Def += " interface";
        } else {
            throw new RuntimeException("Unknown class object type");
        }

        Def = Def.trim();

        // class     => 0A | 00 02 00 03 07
        // interface => 07 | 00 02 01 00 + length

        String Name = "?";

        if (reader.now("0A")) {
            reader.consume(); // not sure either; probably 2-byte integer?
            reader.consume(); // super class (constant index)
            reader.consume(); // not sure either; probably 2-byte integer?
            reader.consume(); // this class (constant index)

            if (reader.now("07 00 04 0C 00 05 00 06 01 00")) {
                var Extends = reader.parseText();
                if (Extends.equals("java/lang/Record")) {
                    // Type = "record";

                    while (!reader.now("07 00 14 01 00"))
                        reader.consume();
                } else {
                    // Type = "class";

                    while (!reader.now("07 00 08 01 00"))
                        reader.consume();
                }

                // System.out.println("Class/record '" + entry.getName() + "' extends '" + Extends + "'");
            } else {
                throw new RuntimeException("Invalid class/record '" + entry.getName() + "'");
            }


            Name = reader.parseText();
        } else {
            reader.expect("07 00 02 01 00");

            Name = reader.parseText();

            if (reader.now("07 00 04 01 00")) {
                var Extends = reader.parseText();

                // Type = "[@]interface";

                // System.out.println("[@]Interface '" + entry.getName() + "' is '" + Name + "' which extends '" + Extends + "'");
            } else {
                // Type = "enum";

                // System.out.println("   Enum      '" + entry.getName() + "' is '" + Name + "'");
            }
        }

                             // �SourceFile�
        while (!reader.now("01 00 0A 53 6F 75 72 63 65 46 69 6C 65 01 00"))
            reader.consume();

        var sourceFile = reader.parseText();

        var bb = ByteBuffer.allocateDirect(2);
        bb.order(ByteOrder.BIG_ENDIAN);
        bb.put(reader.consume());
        bb.put(reader.consume());
        bb.flip();
        var mod = bb.getShort();

        if ((mod & ACC_PROTECTED) == ACC_PROTECTED) {
            mod |= ACC_PROTECTED;
            mod |= ACC_PUBLIC;

            System.out.println("[+] protected %s %s".formatted(Def, Name));
        } else if ((mod & ACC_PRIVATE) == ACC_PRIVATE) {
            mod |= ACC_PRIVATE;
            mod |= ACC_PUBLIC;

            System.out.println("[+] private %s %s".formatted(Def, Name));
        } else if ((mod & ACC_PUBLIC) != ACC_PUBLIC) {
            mod |= ACC_PUBLIC;

            System.out.println("[+] %s %s".formatted(Def, Name));
        }

        reader.back(2);
        reader.buf(2).putShort(mod);

        try                   { write(entry, bytes);                                                           }
        catch (IOException e) { throw new RuntimeException("Failed to copy transformed class inside .jar", e); }
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
