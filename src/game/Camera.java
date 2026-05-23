package game;

/**
 * Global 2D / 3D camera, modeled directly on the Camera class Professor Murphy
 * built on March 10 and March 24.
 *
 * Fields are static so any background layer / 3D object / parallax helper can
 * reference them without being handed a Camera instance ("camera is at the
 * center of the universe; the universe comes to it" was the visceral example
 * he used in class).
 *
 * The 2D portion is used to drive parallax background motion: closer layers
 * (small z) translate by a large fraction of camera.x, far layers (large z)
 * translate by very little. The classic z-divided parallax formula:
 *
 *     drawX = layerWorldX - Camera.x / layer.z
 *
 * The 3D portion (with angle, cosA, sinA) is set up here so the cube-boss
 * lesson at the end of the semester can place itself relative to the camera
 * without rewriting state.
 */
public final class Camera {

    public static double x;
    public static double y;
    public static double z;

    /** View-rotation angle in degrees for the 3D camera. */
    public static double angle;
    public static double cosA = 1;
    public static double sinA = 0;

    private Camera() {}

    public static void moveLeft(double d)    { x -= d; }
    public static void moveRight(double d)   { x += d; }
    public static void moveUp(double d)      { y -= d; }
    public static void moveDown(double d)    { y += d; }
    public static void moveForward(double d) { z += d; }
    public static void moveBackward(double d){ z -= d; }

    public static void setAngle(double degrees) {
        angle = degrees;
        double r = Math.PI * degrees / 180.0;
        cosA = Math.cos(r);
        sinA = Math.sin(r);
    }

    public static void turnLeft(double deltaDegrees)  { setAngle(angle - deltaDegrees); }
    public static void turnRight(double deltaDegrees) { setAngle(angle + deltaDegrees); }

    public static void reset() {
        x = 0; y = 0; z = 0; setAngle(0);
    }
}
