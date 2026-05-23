package effects;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;

/**
 * Tiny SFX manager. Pre-loads WAV files into Clip objects keyed by name and
 * plays them on demand. Missing files are silently ignored so the game runs
 * cleanly even with an empty assets/sounds/ folder.
 *
 * Drop your WAVs into assets/sounds/ with these names to bring them in:
 *
 *   slice.wav       - slicing a fruit (short swoosh)
 *   splat.wav       - juice splatter follow-up
 *   bomb.wav        - bomb explosion
 *   combo.wav       - combo chime
 *   menu_hover.wav  - menu hover ping
 *   start.wav       - game start gong
 *   game_over.wav   - game over gong
 *
 * Free sources: freesound.org, kenney.nl, opengameart.org (filter by CC0).
 */
public final class SoundManager {

    public static final String SLICE      = "slice";
    public static final String SPLAT      = "splat";
    public static final String BOMB       = "bomb";
    public static final String COMBO      = "combo";
    public static final String MENU_HOVER = "menu_hover";
    public static final String START      = "start";
    public static final String GAME_OVER  = "game_over";

    private static final Map<String, Clip> CLIPS = new HashMap<>();
    private static boolean loaded;

    private SoundManager() {}

    public static synchronized void preload() {
        if (loaded) return;
        loaded = true;
        load(SLICE,      "assets/sounds/slice.wav");
        load(SPLAT,      "assets/sounds/splat.wav");
        load(BOMB,       "assets/sounds/bomb.wav");
        load(COMBO,      "assets/sounds/combo.wav");
        load(MENU_HOVER, "assets/sounds/menu_hover.wav");
        load(START,      "assets/sounds/start.wav");
        load(GAME_OVER,  "assets/sounds/game_over.wav");
    }

    public static void play(String key) {
        play(key, 1.0f);
    }

    /**
     * @param volume linear gain in [0.0, 1.0]. Internally translated to dB
     *               with -80 dB at silence.
     */
    public static void play(String key, float volume) {
        if (!loaded) preload();
        Clip clip = CLIPS.get(key);
        if (clip == null) return;
        try {
            clip.stop();
            clip.flush();
            clip.setFramePosition(0);
            applyGain(clip, volume);
            clip.start();
        } catch (Exception ignore) {
            // Audio subsystem can be flaky on Windows. Just drop the cue.
        }
    }

    private static void load(String key, String path) {
        try {
            File file = new File(path);
            if (!file.exists()) return;
            AudioInputStream in = AudioSystem.getAudioInputStream(file);
            Clip clip = AudioSystem.getClip();
            clip.open(in);
            CLIPS.put(key, clip);
        } catch (Exception ignore) {
            // bad file or unsupported format — just skip it.
        }
    }

    private static void applyGain(Clip clip, float volume) {
        if (volume < 0) volume = 0;
        if (volume > 1) volume = 1;
        if (!clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) return;
        FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
        float dB = volume == 0 ? -80f : (float) (Math.log10(volume) * 20);
        gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), dB)));
    }
}
