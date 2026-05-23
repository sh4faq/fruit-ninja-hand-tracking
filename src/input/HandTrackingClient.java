package input;

import java.awt.Component;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * TCP client that reads fingertip positions from the Python tracker sidecar
 * running on 127.0.0.1:50007. This is the original architecture that
 * actually worked end-to-end. The user runs run-tracker.bat in a separate
 * terminal, the game connects, the pill flips green.
 *
 * Protocol (one ASCII line per webcam frame):
 *     <screen_w>,<screen_h>,<x>,<y>,<blade_count>
 *
 * x or y == -1 means "no hand visible".
 *
 * Runs on its own daemon thread. Reconnects every 2 seconds while the
 * sidecar isn't running yet.
 */
public class HandTrackingClient {

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 50007;

    private static final int CONNECT_TIMEOUT_MS = 3000;
    private static final int READ_TIMEOUT_MS = 4000;
    private static final int RECONNECT_DELAY_MS = 2000;

    private final InputManager input;
    private final Component host;
    private volatile boolean running;
    private volatile boolean connected;
    private volatile int activeBladeCount = 5;

    private double smoothedX = Double.NaN;
    private double smoothedY = Double.NaN;
    private long lastSeenMs;

    public HandTrackingClient(InputManager input, Component host) {
        this.input = input;
        this.host = host;
    }

    public void start() {
        if (running) return;
        running = true;
        Thread t = new Thread(this::safeLoop, "HandTrackingClient");
        t.setDaemon(true);
        t.setUncaughtExceptionHandler((thread, ex) -> {
            connected = false;
            System.err.println("[Hand] Reader thread died: " + ex);
            ex.printStackTrace();
        });
        t.start();
    }

    public void stop() { running = false; }
    public int getActiveBladeCount() { return activeBladeCount; }
    public boolean isConnected() { return connected; }

    private void safeLoop() {
        try {
            loop();
        } catch (Throwable t) {
            connected = false;
            System.err.println("[Hand] Fatal in reader loop: " + t);
            t.printStackTrace();
        }
    }

    private void loop() {
        boolean announced = false;
        while (running) {
            if (!announced) {
                System.out.println("[Hand] Trying to connect to " + HOST + ":" + PORT + "...");
                announced = true;
            }

            Socket socket = null;
            BufferedReader in = null;
            boolean wasConnected = false;
            try {
                socket = new Socket();
                socket.connect(new InetSocketAddress(HOST, PORT), CONNECT_TIMEOUT_MS);
                socket.setSoTimeout(READ_TIMEOUT_MS);
                socket.setTcpNoDelay(true);
                in = new BufferedReader(new InputStreamReader(
                    socket.getInputStream(), StandardCharsets.US_ASCII));

                connected = true;
                wasConnected = true;
                announced = false;
                System.out.println("[Hand] Connected to sidecar.");

                String line;
                while (running && (line = in.readLine()) != null) {
                    parseAndSubmit(line);
                }
                System.out.println("[Hand] Sidecar closed (EOF).");
            } catch (java.net.ConnectException ce) {
                // Sidecar isn't running yet; quiet retry.
            } catch (java.net.SocketTimeoutException ste) {
                if (wasConnected) {
                    System.out.println("[Hand] Read timed out, reconnecting.");
                }
            } catch (Exception ex) {
                if (wasConnected) {
                    System.out.println("[Hand] Read loop error: "
                        + ex.getClass().getSimpleName() + ": " + ex.getMessage());
                }
            } finally {
                if (wasConnected) {
                    connected = false;
                    input.clearPointer();
                    smoothedX = Double.NaN;
                    smoothedY = Double.NaN;
                }
                if (in != null) try { in.close(); } catch (Exception ignore) {}
                if (socket != null) try { socket.close(); } catch (Exception ignore) {}
            }

            if (!running) break;
            try { Thread.sleep(RECONNECT_DELAY_MS); }
            catch (InterruptedException ie) {
                Thread.currentThread().interrupt(); return;
            }
        }
    }

    private void parseAndSubmit(String line) {
        String[] parts = line.split(",");
        if (parts.length < 5) return;
        try {
            double srcW = Double.parseDouble(parts[0]);
            double srcH = Double.parseDouble(parts[1]);
            double x    = Double.parseDouble(parts[2]);
            double y    = Double.parseDouble(parts[3]);
            int blades  = Integer.parseInt(parts[4].trim());
            activeBladeCount = blades;

            if (x < 0 || y < 0) {
                if (System.currentTimeMillis() - lastSeenMs > 250) {
                    input.clearPointer();
                    smoothedX = Double.NaN;
                    smoothedY = Double.NaN;
                }
                return;
            }
            lastSeenMs = System.currentTimeMillis();

            int dstW = host.getWidth();
            int dstH = host.getHeight();
            if (dstW <= 0 || dstH <= 0 || srcW <= 0 || srcH <= 0) return;

            double scale = Math.max(dstW / srcW, dstH / srcH);
            double offX = (dstW - srcW * scale) / 2.0;
            double offY = (dstH - srcH * scale) / 2.0;
            double px = x * scale + offX;
            double py = y * scale + offY;

            // Apply sensitivity around screen center so the player can crank
            // up how much their hand motion is amplified into pointer motion.
            double sensitivity = economy.Settings.sensitivity();
            double cx = dstW * 0.5;
            double cy = dstH * 0.5;
            px = cx + (px - cx) * sensitivity;
            py = cy + (py - cy) * sensitivity;

            // EMA smoothing alpha is now player-configurable.
            // Higher alpha = more responsive (less lag), lower = smoother.
            double alpha = economy.Settings.smoothing();

            if (Double.isNaN(smoothedX)) {
                smoothedX = px;
                smoothedY = py;
            } else {
                smoothedX += (px - smoothedX) * alpha;
                smoothedY += (py - smoothedY) * alpha;
            }

            input.submitPointer(smoothedX, smoothedY);
        } catch (NumberFormatException nfe) {
            // bad line, skip
        }
    }
}
