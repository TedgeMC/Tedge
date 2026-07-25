package pl.olafcio.tedge.jargen;

import org.jspecify.annotations.NullMarked;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import pl.olafcio.tedge.jargen.transformers.ClassPublicizer;

import java.io.*;
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

        reader.accept(visitor, 0);
        bytes = writer.toByteArray();

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
