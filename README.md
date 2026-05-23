# Fruit Ninja Java

A Java 8 port of my Fruit Ninja web game, written for Professor Murphy's CMP 717
Video Game Programming course. The game runs in Eclipse / standalone Java and
plays with either a mouse or real hand tracking through the webcam.

## Architecture

```
src/
  game/        Main, GameWindow, GamePanel (loop), GameMode, GameState
  math/        Vector2, Collision (line vs circle intersection)
  entities/    Fruit, Bomb, SpecialFruit, FruitType, FruitRenderer
  effects/     Particle, JuiceSplatter, SliceTrail, ScorePopup, EffectsManager
  input/       InputManager, SliceLine, HandTrackingClient
  ui/          MainMenu, Hud, GameOverScreen

hand_tracker.py     Python MediaPipe sidecar (streams fingertip x,y over a socket)
requirements.txt    Python dependencies for the sidecar
```

The Java code is 100% of the game (loop, physics, collision, rendering,
scoring, modes, effects, UI). The Python sidecar exists for one reason only:
MediaPipe doesn't have a clean Java desktop binding, so we run it as a tiny
external process that streams `x, y, blade_count` over TCP. The Java game
treats those numbers exactly like a mouse position. If the sidecar isn't
running, the game falls back to mouse input automatically.

## Concepts from class shown in the code

  * `math/Collision.segmentIntersectsCircle` is the canonical "line versus
    circle" test from class. Every slice is a line segment from the previous
    fingertip position to the current one, tested against each fruit's circle.
  * Delta time game loop in `game/GamePanel.run()` uses `System.nanoTime`
    and a sleep-based 60 FPS target.
  * Per-frame physics in each entity's `update(dtScale, gravity)` applies
    `vy += gravity * dt; x += vx * dt; y += vy * dt;` so motion stays
    frame-rate independent.
  * `Fruit.slice(dx, dy)` produces two halves with launch vectors derived
    from the slice direction, perpendicular to the cut.
  * `entities/FruitRenderer` builds each fruit from layered Graphics2D paint
    passes (radial gradient body, ambient occlusion ring, diffuse + specular
    highlights, stem and leaf overlays) instead of using sprites.

## Running

### Without hand tracking (mouse only)

```
build.bat
run-game.bat
```

Slice with the mouse. Press 1, 2, or 3 in the menu to start Classic, Arcade,
or Zen.

### With hand tracking (webcam)

Install Python dependencies once:

```
pip install -r requirements.txt
```

Then start the tracker in one terminal and the game in another:

```
run-tracker.bat
run-game.bat
```

The tracker prints `client connected` once the game attaches. Slice by moving
your index finger in front of the webcam.

## Running from VS Code (recommended)

  1. Install the **Extension Pack for Java** (Microsoft) from the VS Code
     marketplace if you don't have it already.
  2. Open this folder in VS Code (`File > Open Folder` and pick
     `fruit-ninja-java`).
  3. Press **F5** to run. A `Run Fruit Ninja` configuration is already
     wired up in `.vscode/launch.json`, and `tasks.json` builds before launch.
  4. For hand tracking, open the command palette
     (`Ctrl + Shift + P`) and pick
     `Tasks: Run Task > Run hand tracker (Python sidecar)`.

## Importing into Eclipse

  1. File -> Import -> Existing Projects into Workspace
  2. Pick this folder
  3. Right click `src/game/Main.java` and choose Run As -> Java Application

## Controls

  * Mouse drag or finger swipe to slice fruit
  * 1 / 2 / 3 to pick a game mode in the menu
  * ENTER on the game over screen to play again, ESC to return to the menu
  * ESC during a game to return to the menu
