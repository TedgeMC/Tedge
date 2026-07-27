package pl.olafcio.tedge;

/**
 * To load an initializer class to Tedge, add an entry<br/>
 * being its classname to your tedge.mod.json {@code load} section.
 * <br/><br/>
 * For example:
 * <pre>
 * {@code
 *  name: "TestMod"
 *
 *  load:
 *   - pl.olafcio.testmod.Main
 * }
 * </pre>
 */
public interface IInitializer {
    void init();
}
