package engine;

/**
 * States the application caries out in order of appearance. After the
 * last state the application exports the image and closes itself.
 * 
 * @author Jello
 */

public enum ApplicationState {
	PICK_HUE_STYLE,
	PICK_HUES,
	PICK_VALUE_COUNT,
	ADJUST_SATURATION,
	ADJUST_BRIGHTNESS,
	ADJUST_TINTS,
	PICK_RENDER_STYLE,
}
