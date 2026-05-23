package economy;

import java.util.prefs.Preferences;

/**
 * Player-tweakable settings persisted across runs.
 *
 * Currently exposes hand-tracker sensitivity (how reactive the cursor is to
 * raw hand movement) and slice threshold (minimum pixel motion that counts
 * as a slice). Defaults aim to feel snappy without flickering.
 */
public final class Settings {

    private static final Preferences PREFS =
        Preferences.userRoot().node("/fruit_ninja_java");

    private static final String KEY_SENSITIVITY    = "settings.sensitivity";
    private static final String KEY_SMOOTHING      = "settings.smoothing";
    private static final String KEY_SLICE_THRESH   = "settings.sliceThresh";

    // Sensitivity multiplier on pointer motion delta. 1.0 = native MediaPipe.
    public static final double DEFAULT_SENSITIVITY = 1.4;
    // EMA alpha. Higher = more responsive (less smoothing). Range 0.30..0.95.
    public static final double DEFAULT_SMOOTHING   = 0.85;
    // Pixels of pointer motion needed to emit a slice line.
    public static final int    DEFAULT_SLICE_THRESH = 1;

    private Settings() {}

    public static double sensitivity() {
        return PREFS.getDouble(KEY_SENSITIVITY, DEFAULT_SENSITIVITY);
    }

    public static double smoothing() {
        return PREFS.getDouble(KEY_SMOOTHING, DEFAULT_SMOOTHING);
    }

    public static int sliceThreshold() {
        return PREFS.getInt(KEY_SLICE_THRESH, DEFAULT_SLICE_THRESH);
    }

    public static void setSensitivity(double v) {
        v = Math.max(0.5, Math.min(2.5, v));
        PREFS.putDouble(KEY_SENSITIVITY, v);
    }

    public static void setSmoothing(double v) {
        v = Math.max(0.30, Math.min(0.95, v));
        PREFS.putDouble(KEY_SMOOTHING, v);
    }

    public static void setSliceThreshold(int v) {
        v = Math.max(1, Math.min(8, v));
        PREFS.putInt(KEY_SLICE_THRESH, v);
    }

    public static void resetAll() {
        setSensitivity(DEFAULT_SENSITIVITY);
        setSmoothing(DEFAULT_SMOOTHING);
        setSliceThreshold(DEFAULT_SLICE_THRESH);
    }
}
