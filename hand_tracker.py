"""
Hand tracking sidecar for the Java Fruit Ninja game.

Opens the default webcam, runs MediaPipe's HandLandmarker on each frame, and
streams the dominant fingertip position to the Java game over a TCP socket
on 127.0.0.1:50007 (the original working architecture).

The Java game (HandTrackingClient) connects as a TCP client and reads one
ASCII line per webcam frame:

    <screen_width>,<screen_height>,<x>,<y>,<active_blade_count>

If no hand is visible the script sends:

    <screen_width>,<screen_height>,-1,-1,1

Self-test (no game needed):
    py -3.13 -u hand_tracker.py --diagnose
"""

import os
import socket
import sys
import time
import traceback
import urllib.request

try:
    sys.stdout.reconfigure(line_buffering=True)
except Exception:
    pass

try:
    import cv2
except Exception as exc:
    print(f"[hand_tracker] FATAL: could not import cv2 (opencv-python): {exc}",
          file=sys.stderr, flush=True)
    sys.exit(2)

try:
    import mediapipe as mp
    from mediapipe.tasks import python as mp_python
    from mediapipe.tasks.python import vision as mp_vision
except Exception as exc:
    print(f"[hand_tracker] FATAL: could not import mediapipe Tasks API: {exc}",
          file=sys.stderr, flush=True)
    sys.exit(2)


HOST = "127.0.0.1"
PORT = 50007

CAM_WIDTH = 640
CAM_HEIGHT = 480

MODEL_FILE = "hand_landmarker.task"
MODEL_URL = (
    "https://storage.googleapis.com/mediapipe-models/hand_landmarker/"
    "hand_landmarker/float16/1/hand_landmarker.task"
)

TIP_INDICES = [4, 8, 12, 16, 20]
PIP_INDICES = [3, 6, 10, 14, 18]


def ensure_model():
    if os.path.exists(MODEL_FILE) and os.path.getsize(MODEL_FILE) > 0:
        return
    print(f"[hand_tracker] downloading {MODEL_FILE}...", flush=True)
    try:
        urllib.request.urlretrieve(MODEL_URL, MODEL_FILE)
    except Exception as exc:
        print(f"[hand_tracker] FATAL: model download failed: {exc}",
              file=sys.stderr, flush=True)
        sys.exit(3)
    print("[hand_tracker] model downloaded", flush=True)


def count_extended_fingers(landmarks):
    wrist_x = landmarks[0].x
    count = 0
    if abs(landmarks[4].x - wrist_x) > abs(landmarks[3].x - wrist_x):
        count += 1
    for tip, pip in zip(TIP_INDICES[1:], PIP_INDICES[1:]):
        if landmarks[tip].y < landmarks[pip].y:
            count += 1
    return count


def _try_open(index, backend, backend_name):
    cap = cv2.VideoCapture(index, backend)
    if not cap.isOpened():
        return None
    cap.set(cv2.CAP_PROP_FRAME_WIDTH, CAM_WIDTH)
    cap.set(cv2.CAP_PROP_FRAME_HEIGHT, CAM_HEIGHT)
    ok, frame = cap.read()
    if not ok or frame is None:
        cap.release()
        return None
    print(f"[hand_tracker] webcam opened on index {index} via {backend_name}",
          flush=True)
    return cap


def open_webcam():
    print("[hand_tracker] opening webcam...", flush=True)
    backends = [
        (cv2.CAP_DSHOW, "DirectShow"),
        (cv2.CAP_MSMF,  "Media Foundation"),
        (cv2.CAP_ANY,   "auto"),
    ]
    for attempt in range(2):
        if attempt > 0:
            print("[hand_tracker] webcam busy, retrying in 1.5s...", flush=True)
            time.sleep(1.5)
        for index in (0, 1, 2):
            for backend, name in backends:
                cap = _try_open(index, backend, name)
                if cap is not None:
                    return cap
    print("[hand_tracker] FATAL: cannot open webcam on any backend or index",
          file=sys.stderr, flush=True)
    return None


def build_detector():
    try:
        base_options = mp_python.BaseOptions(model_asset_path=MODEL_FILE)
        options = mp_vision.HandLandmarkerOptions(
            base_options=base_options,
            running_mode=mp_vision.RunningMode.VIDEO,
            num_hands=1,
            min_hand_detection_confidence=0.5,
            min_hand_presence_confidence=0.5,
            min_tracking_confidence=0.5,
        )
        return mp_vision.HandLandmarker.create_from_options(options)
    except Exception as exc:
        print(f"[hand_tracker] FATAL: HandLandmarker init failed: {exc}",
              file=sys.stderr, flush=True)
        traceback.print_exc()
        return None


def process_frame(frame, detector, start_time):
    try:
        frame = cv2.flip(frame, 1)
        h, w = frame.shape[:2]
        rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
        mp_image = mp.Image(image_format=mp.ImageFormat.SRGB, data=rgb)
        ts_ms = int((time.time() - start_time) * 1000)
        result = detector.detect_for_video(mp_image, ts_ms)
        if result.hand_landmarks:
            lm = result.hand_landmarks[0]
            tip = lm[8]
            px = tip.x * w
            py = tip.y * h
            blades = count_extended_fingers(lm)
            blade_count = 1 if blades <= 1 else 5
            return f"{w},{h},{px:.2f},{py:.2f},{blade_count}\n"
        return f"{w},{h},-1,-1,1\n"
    except Exception as exc:
        print(f"[hand_tracker] frame error (skipping): {exc}",
              file=sys.stderr, flush=True)
        return None


def serve_one_client(conn, cap, detector):
    """Stream frames over `conn` until the client disconnects or an error
    forces us to close it. Returns when the client is gone."""
    conn.settimeout(5.0)
    start_time = time.time()
    consecutive_read_failures = 0
    try:
        while True:
            ok, frame = cap.read()
            if not ok:
                consecutive_read_failures += 1
                if consecutive_read_failures >= 60:
                    print("[hand_tracker] webcam read failed repeatedly, dropping client.",
                          file=sys.stderr, flush=True)
                    return
                time.sleep(0.016)
                continue
            consecutive_read_failures = 0

            line = process_frame(frame, detector, start_time)
            if line is None:
                continue

            try:
                conn.sendall(line.encode("ascii"))
            except (BrokenPipeError, ConnectionResetError, ConnectionAbortedError,
                    OSError):
                print("[hand_tracker] client disconnected.", flush=True)
                return
            except socket.timeout:
                print("[hand_tracker] send timed out, dropping client.",
                      flush=True)
                return

            time.sleep(0.016)
    except KeyboardInterrupt:
        raise


def serve_forever(cap, detector):
    """Bind once, then accept() repeatedly so the tracker survives multiple
    game restarts. Returns only on a fatal error or Ctrl+C."""
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    try:
        server.bind((HOST, PORT))
    except OSError as exc:
        print(f"[hand_tracker] FATAL: cannot bind {HOST}:{PORT}: {exc}",
              file=sys.stderr, flush=True)
        print("[hand_tracker] Is another tracker already running?",
              file=sys.stderr, flush=True)
        return 4
    server.listen(1)
    print(f"[hand_tracker] waiting for Java client on {HOST}:{PORT}...",
          flush=True)

    try:
        while True:
            try:
                conn, addr = server.accept()
            except KeyboardInterrupt:
                raise
            print(f"[hand_tracker] client connected from {addr}", flush=True)
            try:
                serve_one_client(conn, cap, detector)
            finally:
                try: conn.close()
                except Exception: pass
            print(f"[hand_tracker] waiting for Java client on {HOST}:{PORT}...",
                  flush=True)
    except KeyboardInterrupt:
        print("[hand_tracker] Ctrl+C, shutting down.", flush=True)
        return 0
    finally:
        try: server.close()
        except Exception: pass


def run_diagnose():
    """5-second self-test."""
    print("[diagnose] Starting 5-second self-test...", flush=True)
    status = {"webcam": False, "detector": False, "detection": False, "port": False}
    ensure_model()

    cap = open_webcam()
    if cap is None:
        print_diagnose_summary(status); return 1
    status["webcam"] = True

    detector = build_detector()
    if detector is None:
        cap.release()
        print_diagnose_summary(status); return 1
    status["detector"] = True

    start_time = time.time()
    deadline = time.time() + 5.0
    while time.time() < deadline and not status["detection"]:
        ok, frame = cap.read()
        if not ok:
            time.sleep(0.05); continue
        line = process_frame(frame, detector, start_time)
        if line is not None:
            status["detection"] = True
            print(f"[diagnose] sample frame line: {line.strip()}", flush=True)
            break

    test_server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    test_server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    try:
        test_server.bind((HOST, PORT))
        status["port"] = True
    except OSError as exc:
        print(f"[diagnose] port {PORT} busy: {exc}", file=sys.stderr, flush=True)
    finally:
        test_server.close()

    cap.release()
    try: detector.close()
    except Exception: pass
    print_diagnose_summary(status)
    return 0 if all(status.values()) else 1


def print_diagnose_summary(status):
    print("", flush=True)
    print("=" * 50, flush=True)
    print("[diagnose] SUMMARY", flush=True)
    print("=" * 50, flush=True)
    for key, label in [
        ("webcam", "Webcam opens"),
        ("detector", "HandLandmarker initializes"),
        ("detection", "Sample frame detected"),
        ("port", f"TCP port {PORT} bindable"),
    ]:
        mark = "OK  " if status.get(key) else "FAIL"
        print(f"  [{mark}] {label}", flush=True)
    print("=" * 50, flush=True)


def main():
    if "--diagnose" in sys.argv[1:]:
        sys.exit(run_diagnose())

    ensure_model()
    cap = open_webcam()
    if cap is None:
        sys.exit(2)
    detector = build_detector()
    if detector is None:
        cap.release()
        sys.exit(1)

    try:
        rc = serve_forever(cap, detector)
        sys.exit(rc or 0)
    finally:
        cap.release()
        try: detector.close()
        except Exception: pass


if __name__ == "__main__":
    main()
