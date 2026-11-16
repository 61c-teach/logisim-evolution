# Customizing Logisim's appearance (61C fork, v5.x)

This guide is for 61C staff maintaining our fork of Logisim-evolution. It covers how to change Logisim's icons, logos, theme, colors and fonts. Everything here was checked against upstream tag `v5.0.0`.

All paths are relative to the repository root.

Contents:
- [1. Build and preview](#1-build-and-preview)
- [2. The three kinds of icons](#2-the-three-kinds-of-icons)
- [3. App icon, logo, About box and splash screen](#3-app-icon-logo-about-box-and-splash-screen)
- [4. Theme (look and feel)](#4-theme-look-and-feel)
- [5. Canvas, wire and component colors](#5-canvas-wire-and-component-colors)
- [6. Fonts](#6-fonts)
- [7. Keeping changes merge-friendly](#7-keeping-changes-merge-friendly)

---

## 1. Build and preview

v5 requires **Java 21**: `build.gradle.kts` sets `sourceCompatibility = JavaVersion.VERSION_21`.

```bash
./gradlew run          # build and launch the app (application plugin, main class com.cburch.logisim.Main)
./gradlew shadowJar    # build a self-contained jar in build/libs/
java -jar build/libs/logisim-evolution-*.jar
```

Upstream names the jar `logisim-evolution-<version>-all.jar`. Our course commit `9b946714b` removes the `-all` suffix.

**Saved preferences.** Your own saved preferences (theme, colors, zoom) can hide a change you just made to a default. They are stored with `java.util.prefs` under `Preferences.userNodeForPackage(Main.class)` (`AppPreferences.java`). On macOS they live in `~/Library/Preferences/com.apple.java.util.prefs.plist`.

There is no general "reset preferences" button; the Preferences window only has one for hotkeys. To test a change to a default, do one of the following:
- delete that preferences node;
- use our fork's `--no-sync-prefs` flag (course commit `95c389b47`, once it has been carried over to v5).

---

## 2. The three kinds of icons

Logisim draws icons in the toolbox (the component tree), the toolbars, and the attribute and menu areas. Each icon comes from one of three sources. Before changing an icon, find out which kind it is: search the component's class for `setIcon(`, `setIconName(` or `paintIcon(`.

### 2a. Image files: `src/main/resources/resources/logisim/icons/`

There are 27 small images, mostly 16×16 GIFs; `por.png` is a PNG. Examples are `clock.gif`, `probe.gif`, `tunnel.gif`, `splitter.gif`, `subcirc.gif`, `ok.gif` and `cancel.gif`.

They are loaded by `com.cburch.logisim.util.IconsUtil.getIcon(String name)`. That method looks the file up on the classpath at `resources/logisim/icons/<name>` and scales it to `AppPreferences.getScaled(AppPreferences.IconSize)`. `IconSize` is 16, multiplied by the user's zoom factor.

Code reaches these images in three ways:
- **Components:** `InstanceFactory.setIconName("pio.gif")`, used for example in `std/io/PortIo.java`, `std/io/Telnet.java` and `std/io/RealTimeClock.java`.
- **Hard-coded static fields:**
  - `std/wiring/Clock.java`: `IconsUtil.getIcon("clock.gif")`;
  - `comp/AbstractComponentFactory.java`: `subcirc.gif`, the fallback icon for any factory;
  - `util/JHotkeyInput.java`: `ok.gif` and `cancel.gif`.
- **Toolbar buttons:** `gui/main/LogisimToolbarItem` has constructor and `setIcon(String)` overloads that take a file name.

If a file is missing, there is no error. `InstanceFactory.paintIcon(...)` falls back to `AbstractComponentFactory.paintIcon`, which draws `subcirc.gif` or, failing that, a generic box. In v5.0.0 these referenced files already don't exist: `pla.gif`, `ttl.gif`, `displaydecoder.gif` and `dropdown.png` (used in `gui/log/SelectionList.java`).

**Format tips.** Any format `javax.swing.ImageIcon` reads works: PNG, GIF or JPEG. Images are scaled with `Image.SCALE_SMOOTH`, so a 32×32 or 64×64 PNG with transparency looks sharper than a 16×16 GIF on HiDPI screens and when the zoom factor is above 1. Images are **not** recolored for dark themes. Only drawn icons (2b and 2c) follow the theme colors.

### 2b. Java-drawn icon classes: `com.cburch.logisim.gui.icons.*`

These are about 45 classes in `src/main/java/com/cburch/logisim/gui/icons/`, for example `ArithmeticIcon`, `PlexerIcon`, `SimulationIcon`, `SelectIcon`, `ZoomIcon`, `TreeIcon` and `ProjectAddIcon`. A few drawing-editor icons are in `com.cburch.draw.icons`.

All of them extend `BaseIcon`, which does the shared setup:
- implements `javax.swing.Icon` with width and height equal to `AppPreferences.getIconSize()`;
- turns on antialiasing when `UI_ANTIALIASING` is set;
- sets the pen color to `AppPreferences.COMPONENT_ICON_COLOR`, which is black in light themes and white in dark ones (section 5);
- sets a stroke of `getScaled(1)`;
- calls `paintIcon(Graphics2D)` on the subclass.

Subclasses draw on a 16×16 grid and wrap every coordinate in `scale(...)` or `AppPreferences.getScaled(...)`.

These icons are used in two places:
- **Components:** `InstanceFactory.setIcon(new XxxIcon(...))`. There are about 100 calls, for example `std/arith/Adder.java` uses `setIcon(new ArithmeticIcon("+"))`.
- **Toolbars:**
  - `gui/main/SimulationToolbarModel.java`: run/step/tick buttons drawn by `SimulationIcon`;
  - `gui/main/ToolboxToolbarModel.java`: `ProjectAddIcon`, `AppearEditIcon`, etc.;
  - the toolbox clear button, `new ClearIcon()` in `gui/main/Toolbox.java`;
  - tools such as `tools/SelectTool.java`, which uses `new SelectIcon()`.

### 2c. Icons painted by the component itself

Some factories override `paintIcon(InstancePainter)` and draw the icon with the component's own drawing code. Examples:
- all gates, through `std/gates/AbstractGate.paintIcon`, which is `final` and calls `paintIconANSI` or `paintIconIEC` depending on the gate-shape preference;
- `Pin`, `Clock`, `Text`, `Image`, `Buffer`, `ControlledBuffer`, `NotGate` and `SubcircuitFactory`.

A few tools paint their own icon in `paintIcon(ComponentDrawContext, int, int)`: `PokeTool` (hand), `WiringTool`, `TextTool` and `EditTool`.

When a factory's `paintIcon(InstancePainter)` is overridden, `setIcon` and `setIconName` are ignored for that factory. The base `paintIcon` clears the painter's factory to signal that it was not overridden, and the fallback code checks for that.

### Worked example: change the Tunnel tool icon

`Tunnel` loads `tunnel.gif` (see 2a). There are two ways to change it.

1. **Replace the resource. This is the merge-friendly way.** Draw a new icon and save it over `src/main/resources/resources/logisim/icons/tunnel.gif`. Keep the file name. If you would rather use PNG, change the single `"tunnel.gif"` string in `std/wiring/Tunnel.java`. Then run `./gradlew run` and look at **Wiring → Tunnel** in the toolbox at several zoom factors (Preferences → Window).

2. **Draw it in Java,** so it scales cleanly and follows dark mode. Add `gui/icons/TunnelIcon.java`:

   ```java
   package com.cburch.logisim.gui.icons;

   import java.awt.Graphics2D;

   public class TunnelIcon extends BaseIcon {
     @Override
     protected void paintIcon(Graphics2D g2) {
       // 16x16 design grid; scale() applies the user's zoom factor
       final int[] xs = {scale(1), scale(5), scale(15), scale(15), scale(5)};
       final int[] ys = {scale(8), scale(3), scale(3), scale(13), scale(13)};
       g2.drawPolygon(xs, ys, xs.length);
     }
   }
   ```

   Then, in the `Tunnel` constructor, replace `setIconName("tunnel.gif")` with `setIcon(new TunnelIcon())`. The pen color is already `COMPONENT_ICON_COLOR`, so the icon flips correctly between light and dark themes.

Changing a toolbar button follows the same pattern. Edit the icon class it uses: for example, `SimulationIcon` draws the step and tick buttons, and `SimulationToolbarModel` creates them.

---

## 3. App icon, logo, About box and splash screen

| What | File(s) | Used by |
|---|---|---|
| Window and taskbar icon (Windows/Linux) | `src/main/resources/resources/logisim/img/logisim-icon-{16,32,48,128,256}.png` | `gui/generic/LFrame.java`. It tries sizes `{16, 32, 48, 64, 128}` and passes them to `setIconImages`; 48 is the default. There is no 64 file and 256 is never loaded. |
| About box logo, also shown on the splash screen | `src/main/resources/resources/logisim/img/logisim-evolution-logo.png` | `gui/start/About.java` (`LOGO_IMG`, `LOGO_HEIGHT = 200`, `PANEL_WIDTH = 600`). `gui/start/SplashScreen` reuses `About.getImagePanel()`. |
| Credits logo | `src/main/resources/resources/logisim/hendrix.png` | `gui/start/AboutCredits.java` |
| Installer icons (jpackage builds only) | `support/jpackage/macos/Logisim-evolution.icns`, `Logisim-evolution-circ.icns` (`.circ` file-type icon), `support/jpackage/windows/Logisim-evolution.ico`, `support/jpackage/linux/logisim-icon-128.png` | `build.gradle.kts` (`--icon ...`) |
| Unused copy | `src/main/resources/resources/logisim/img/Logisim-evolution.icns` | Nothing in `src/main/java` loads it. |
| Source artwork | `artwork/logisim-evolution-logo.svg`, `artwork/logisim-evolution-icon.svg` | Turned into PNGs by `artwork/update_assets.sh` |

**About the Dock icon on macOS.** v5 never calls `java.awt.Taskbar`. When students run a plain `java -jar`, macOS ignores `LFrame`'s icon images and the Dock shows the generic Java icon. Only jpackage builds show `Logisim-evolution.icns`. To brand the Dock icon for `java -jar`, add a `Taskbar.getTaskbar().setIconImage(...)` call in `Main`, guarded by `Taskbar.isTaskbarSupported()`.

**Course logo.** Course commit `fa630715e` ("un-bloodify logo") replaced `artwork/logisim-evolution-logo.svg` and `src/main/resources/resources/logisim/img/logisim-evolution-logo.png` (553×201). The v5 port includes this commit, so the v5 build already shows the course logo.

**Regenerating all assets from SVG.** Run `artwork/update_assets.sh` from inside `artwork/`. It needs Inkscape, icoutils and icnsutils, and the logo fonts from `artwork/fonts/` must be installed first or the logo comes out wrong. The script:
- renders the logo at 550 px wide;
- renders icons at 16/32/48/128/256;
- copies them into `img/` and `support/jpackage/`;
- builds the `.icns` and `.ico` files.

It has a bug: the `.icns` loop writes `Logisim-evolution-.icns`, with an extra hyphen, instead of `Logisim-evolution.icns`. Rename the result by hand.

If you change the logo's height, keep it at 200 px tall and under 600 px wide, or update the constants in `About.java` to match (see `artwork/README.md`).

**About box text.** Our extra note in the About panel comes from course commit `c617c0975`, which adds 3 lines to `AboutCredits.java`. The About box's white background and black border are hard-coded in `About.java` (`Color.WHITE` and `Color.BLACK`). They do not follow the dark theme.

---

## 4. Theme (look and feel)

v5 uses **FlatLaf** (`com.formdev:flatlaf:3.7.2` in `build.gradle.kts`).

**Setup at startup.** In `Main.main`, when not running headless, Logisim:
1. installs the FlatLaf themes FlatLight, FlatDark, FlatIntelliJ, FlatDarcula, FlatMacLight and FlatMacDark (`installLafInfo()`);
2. calls `UIManager.setLookAndFeel(AppPreferences.LookAndFeel.get())`;
3. calls `AppPreferences.applyThemeColors()`.

**Default theme.** It is set in `prefs/AppPreferences.java`:

```java
public static final PrefMonitor<String> LookAndFeel =
    create(new PrefMonitorString("LookAndFeel", FlatIntelliJLaf.class.getName()));
```

To change the course default, change that class name, for example to `FlatMacLightLaf.class.getName()`. The change only affects users who have never saved a theme preference.

**Switching themes at runtime.** **Preferences → Window → Look and feel** (`gui/prefs/WindowOptions.java`) lists everything in `UIManager.getInstalledLookAndFeels()`. Choosing one:
- saves the preference;
- calls `applyThemeColors()`;
- re-applies the theme to every open window (`applyLookAndFeelGlobally`).

Some changes still show a "restart required" warning.

**How dark mode is detected.** `AppPreferences.isDarkTheme(className)` returns true when the class name contains `"Dark"` or `"Darcula"`. Canvas colors, K-maps, FPGA boards and the HDL editor theme (`gui/generic/EditorTheme.java`, which picks `LIGHT_EDITOR_THEME` or `DARK_EDITOR_THEME`) all key off this check. A custom theme class must follow that naming, or `isDarkTheme` must be changed.

**Adding a course theme.** v5 doesn't do this today, but FlatLaf supports it in two ways:
- tweak FlatLaf defaults with `UIManager.put("Component.arc", 8)`-style calls in `Main` right after `setLookAndFeel`;
- subclass a FlatLaf theme, register it with `installLafInfo` next to the others in `Main`, and make it the default `LookAndFeel` value.

Keep these changes inside `Main.java` and `AppPreferences.java`.

---

## 5. Canvas, wire and component colors

All of these are `PrefMonitorInt` preferences in `prefs/AppPreferences.java`, each with a light default (`DEFAULT_*`) and a dark default (`DARK_*`):

| Preference | Light default | Dark default |
|---|---|---|
| `CANVAS_BG_COLOR` | `0xFFFFFFFF` | `0xFF2B2B2B` |
| `GRID_BG_COLOR` | `0xFFFFFFFF` | `0xFF2B2B2B` |
| `GRID_DOT_COLOR` | `0xFF777777` | `0xFFAAAAAA` |
| `GRID_ZOOMED_DOT_COLOR` | `0xFFCCCCCC` | `0xFF666666` |
| `COMPONENT_COLOR` | `0x00000000` | `0xFFFFFFFF` |
| `COMPONENT_ICON_COLOR` | `0x00000000` | `0xFFFFFFFF` |
| `TRUE_COLOR` | `0x0000D200` | `0xFF00FF00` |
| `FALSE_COLOR` | `0x00006400` | `0xFF339933` |
| `UNKNOWN_COLOR` | `0x002828FF` | `0xFF3399FF` |
| `ERROR_COLOR` | `0x00C00000` | `0xFFFF4444` |
| `NIL_COLOR` | `0x00808080` | `0xFFBBBBBB` |
| `BUS_COLOR` | `0x00000000` | `0xFFFFFFFF` |
| `STROKE_COLOR` | `0x00FF00FF` | `0xFFFF88FF` |

`COMPONENT_SECONDARY_COLOR`, `COMPONENT_GHOST_COLOR`, the K-map and table colors, the FPGA colors, the width-error colors and the clock-frequency color follow the same pattern.

**Important: colors are reset on every launch.** `AppPreferences.applyThemeColors()` runs from `Main` at every startup and whenever the theme changes. It overwrites the canvas, grid, component, table and signal color preferences with the `DEFAULT_*` or `DARK_*` constant for the current theme. It then copies the signal colors into the static fields `Value.trueColor`, `Value.falseColor`, etc., which is what wire drawing actually reads.

So color changes a user makes in Preferences (`gui/prefs/WindowOptions.java` and `SimOptions.java`) only last until the next restart. To change the course colors, **edit the `DEFAULT_*` and `DARK_*` constants**. Changing the preference's initial value is not enough.

Not everything follows these preferences:
- some drawing code uses hard-coded `Color.BLACK`, `Color.WHITE` and other literal colors, for example the About and splash panels, and the fallback icon in `AbstractComponentFactory.paintIcon`;
- image icons (2a) are not recolored.

Check both kinds in a dark theme after any change.

---

## 6. Fonts

- **Whole UI:** the `APP_FONT` preference (`"AppFont"`, empty by default). When it is set, `Main.updateGlobalFont` calls `FlatLaf.setPreferredFontFamily` and replaces every `FontUIResource` in `UIManager`. When it is empty, `Main` only sets tooltip and tree fonts to bold SansSerif at `getScaled(12)`.
- **Scaled component fonts:** `AppPreferences.FONT_SIZE = 14`, applied through `getScaledFont(...)`.
- **Bold or plain:** `AppPreferences.getPreferredFontStyle(name)` returns `PLAIN` for font names containing "medium", "light", "thin", "regular", "semibold" or "extrabold", to avoid fake bold.
- **Logo fonts:** the fonts in `artwork/fonts/` are only needed to render the logo SVG. The app never loads them.

---

## 7. Keeping changes merge-friendly

Upstream changes quickly: there were more than 2,700 commits between our old base and v5.0.0. To keep future upgrades cheap:

1. **Prefer replacing resources over changing code.** Overwrite files in `resources/logisim/icons/` and `resources/logisim/img/` and keep their names. Git merges a binary replacement cleanly unless upstream changes the same file.
2. **Keep code changes in a few places:**
   - `prefs/AppPreferences.java` for the default theme and color constants;
   - `Main.java` for theme registration, FlatLaf tweaks and the Dock icon;
   - new files under `gui/icons/` for drawn icons.

   Avoid scattering `Color` literals across component classes.
3. **Add new files rather than editing upstream ones.** Write a new `XxxIcon extends BaseIcon` and change one `setIcon(...)` line, rather than rewriting an existing icon class.
4. **Use one commit per visual change,** with a message prefix like `[61c/theme]`, so `git log --grep 61c/` lists everything to carry forward.
5. **Record every course appearance change** in the list below.

### Course appearance changes (keep up to date)

| Change | Files | Commit |
|---|---|---|
| Replaced "bloody" logo with clean logo | `artwork/logisim-evolution-logo.svg`, `src/main/resources/resources/logisim/img/logisim-evolution-logo.png` | `fa630715e` (ported to v5) |
| Course note in About panel | `src/main/java/com/cburch/logisim/gui/start/AboutCredits.java` | `c617c0975` (ported to v5) |
