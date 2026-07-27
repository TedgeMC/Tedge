package pl.olafcio.tedge;

import java.util.Map;
import java.util.jar.JarFile;

/**
 * An internal mod container.
 * @param yml The mod's `tedge.mod.json` file, serialized.
 * @param jarFile A JarFile instance of the mod's jar.
 */
public record Mod(Map<String, Object> yml, JarFile jarFile) {}
