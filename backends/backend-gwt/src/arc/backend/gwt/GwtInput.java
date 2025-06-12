
package arc.backend.gwt;

import arc.Input;

public abstract class GwtInput extends Input {

	/** Resets all Input events (called on main loop after rendering) */
    abstract void reset();
}
