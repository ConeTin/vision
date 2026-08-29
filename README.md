# Vision

Spatial windows for Minecraft, in the spirit of visionOS.

Vision is a client-side Fabric mod that puts free-floating frosted-glass panels into the world.
You place a window in front of you, drag it around by a handle under it, resize it by its corners,
and — if MCEF is installed — load a real web page into it, complete with mouse and keyboard input.

Everything is aimed by looking: the crosshair is the cursor, and it morphs from the vanilla cross
into a ring whenever it is over a window.

## Features

- **Spatial panels.** Windows are real quads in world space with a full 3D transform. They face you
  when spawned and turn to follow you while dragged.
- **Frosted glass.** The backdrop is blurred with a Dual Kawase pyramid (7 levels, downsample →
  tent upsample). Shape, rounded corners and rim highlights are done in an SDF fragment shader.
- **Mutual blur.** Each window's backdrop is composed separately from "the world plus every *other*
  window", so panels blur each other symmetrically and the blur does not pop when they swap depth.
- **Direct manipulation.** A grab bar below the window for dragging, scroll to push it away or pull
  it closer, corner handles that animate into an arc for resizing, and a close button that unfolds
  from a dot into a cross on hover.
- **Web windows.** An embedded Chromium page rendered straight onto the panel via
  [MCEF](https://github.com/CinemaMod/mcef), with click, scroll and keyboard forwarding.
- **Pixel-perfect chrome.** Pixel density is fixed per window, so resizing reflows the content
  instead of stretching it and rounded corners never turn into ellipses.

## Requirements

| | |
|---|---|
| Minecraft | 1.21.4 |
| Fabric Loader | 0.19.3 or newer |
| Fabric API | 0.119.4+1.21.4 |
| Java | 21 |
| [MCEF](https://github.com/CinemaMod/mcef) | 2.1.6-1.21.4 |

MCEF is a separate mod and is **not** bundled into the jar — install it alongside Vision.
On first launch it downloads the CEF native binaries itself.

## Controls

| Input | Action |
|---|---|
| `V` | Open an empty demo window |
| `B` | Open a web window |
| Left click on the bar below a window | Grab and drag the window |
| Scroll while dragging | Move the window closer / further away |
| Left click on a bottom corner handle | Resize the window |
| Left click on the dot left of the bar | Close the window |
| Left click on the page | Send the click to the page and take keyboard focus |
| `Esc` | Release keyboard focus back to the game |

While a window holds keyboard focus, key presses go to it and not to the game.

## Building

```sh
./gradlew build
```

The jar lands in `build/libs/`. To run a development client:

```sh
./gradlew runClient
```

## Project layout

```
client/os/          Window model, window manager, input routing
client/os/apps/     Window contents: DemoApp, WebContent (MCEF browser)
client/render/      World rendering, Dual Kawase blur, crosshair
client/ui/          UiCanvas — the 2D drawing API window contents see
mixin/              Crosshair, mouse and keyboard interception
resources/assets/vision/shaders/core/   GLSL for glass, web, bar, handle, close, blur
```

## Writing your own window

A window's content is anything that implements `WindowContent`. It draws in its own pixel space
through `UiCanvas` and receives mouse and keyboard events in the same coordinates:

```java
public class ClockApp implements WindowContent {
    @Override
    public String title() {
        return "Clock";
    }

    @Override
    public void render(UiCanvas g) {
        g.textCentered(LocalTime.now().toString(), g.width() / 2f, g.height() / 2f, 0xFFFFFFFF);
    }
}
```

Open it in front of the player at 2.5 blocks:

```java
Window window = new Window(new ClockApp(), 480, 320, 3.0f, 2.0f);
WindowManager.INSTANCE.open(window, 2.5f);
```

The last four arguments are the internal resolution in pixels and the physical size in blocks.
Their ratio fixes the pixel density for the lifetime of the window.

## License

[MIT](LICENSE).

MCEF is licensed separately under LGPL 2.1 and is used as an external dependency.

---

Russian version: [README.ru.md](README.ru.md)
