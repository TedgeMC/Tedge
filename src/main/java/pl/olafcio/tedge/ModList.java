package pl.olafcio.tedge;

import java.nio.file.Path;
import java.util.HashMap;

public final class ModList {
    private ModList() {}

    static HashMap<Path, Mod> mods;

    @SuppressWarnings("unchecked")
    public static HashMap<Path, Mod> getMods() {
        return (HashMap<Path, Mod>) mods.clone();
    }

    /**
     * @apiNote <b>Experimental!</b>
     */
    public static void setMods(HashMap<Path, Mod> mods) {
        ModList.mods = mods;
    }
}
