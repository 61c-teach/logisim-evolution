# 61C Logisim fork: repository guide

This guide is for course staff who maintain the CS 61C fork of Logisim-evolution. It covers four things:

1. what changed between our old build (3.8.0-61c) and the v5 build;
2. what every folder in the repository is for;
3. where the course-specific changes live;
4. step-by-step recipes for common changes.

Related documents in this folder:
- [`customizing-appearance.md`](customizing-appearance.md): icons, logo, theme, colors.
- [`v5-port-notes.md`](v5-port-notes.md): how each course commit was carried onto v5.

---

## 1. Old build vs. v5 build

| | Old build (`develop`) | New build (`v5-upgrade`) |
|---|---|---|
| Version string | `3.8.0-61c.dev` | `5.0.0-61c.dev` |
| Upstream base | Logisim-evolution `v3.7.2` + 316 commits (Aug 2022, before the 3.8.0 release) | Logisim-evolution `v5.0.0` (Sep 2026) |
| Java needed to **run** | 16+ (we use 17) | **21+** |
| Build tool | Gradle 7, Java 16 target | Gradle 9.7, Java 21 target |
| Jar size | ~14 MB (docs and boards stripped) | ~54 MB (shrink commit not redone yet) |
| Course features | all | all (see §3) |

The new build includes every upstream release in between: 3.8.0, 3.9.0, 4.0.0, 4.1.0 and 5.0.0. Upstream's full list is in `CHANGES.md`. The changes that matter for 61C are below.

### Changes students will notice
- **Dark mode works properly (5.0).** The canvas, wires, components and icons follow the FlatLaf Dark and Darcula themes, and you can switch theme without restarting.
- **Configurable hotkeys (3.9).** Most shortcuts can be changed under Preferences → Hotkeys. Our "S doesn't shrink gates" change is now a different default for one of these hotkeys.
- **Autosave (3.9),** on by default every 30 minutes. It can be turned off in Preferences.
- **"Find Action" search (5.0):** press Shift twice, or Ctrl+Shift+A, to find any menu action by name. The component tree also has a filter box.
- **Undo/Redo history menus (4.1),** limited to 64 actions.
- **Multi-line text labels (5.0):** Shift+Enter in the Text tool.
- **Pasting (5.0):** pastes at the mouse position.
- **Moving components (5.0)** keeps their state, such as register and RAM contents.
- **Zoom and scroll (5.0)** are remembered separately for each circuit.
- **Image component (5.0)** places a picture inside a circuit.
- **Bus width labels (5.0):** an optional "Show Bus Width" attribute on wires.
- **Timing diagram fixes (5.0):** exports, reset offsets, and clocks whose duty cycle isn't 50%. It still has no "run until halt" button; we use VCD export and Surfer instead (§3).
- **Opening files (5.0):** drag a `.circ` file onto the window.
- **Merging and exporting circuits (5.0):** merge selected circuits from another `.circ` file, or export one circuit with its dependencies.

### Changes in how circuits behave
These can change what a student's circuit does. Watch for them on Ed.

- **Floating subcircuit inputs stay floating (5.0).** An unconnected subcircuit input used to read as 0 in some places; it now carries the floating/undefined value (`U`) through the circuit. This is why 3 autograder tests print `U` instead of `0` for instructions the staff CPU doesn't implement. A correct CPU is not affected.
- **Pin attributes simplified (4.0).** The old "Three-state" and "Pull" attributes became a single "Behavior" attribute. An input pin that relied on pulling floating values to 0 now needs Behavior = Pull Down.
- **Simulation engine rewritten (4.0)** for speed and to fix race conditions. The output of every proj3 test was byte-identical except the 3 tests above.
- **RAM:**
  - unconnected outputs now default to undefined rather than error (3.9);
  - line-enable inputs only write when enabled (5.0).
- **Controlled buffer (3.9):** passes U and E values through while it is enabled.
- **Multiplier (5.0):** gains a third mode, signed × unsigned.
- **Random generator (5.0):** uses a different algorithm, so a given seed produces different values.
- **Built-in TTY table (5.0):** puts bit widths in headers. We keep the course table format (§3), so the reference files still match.

### Changes for staff and tooling
- **Command-line options:**
  - `--load <file> [label]` can load several RAM/ROM components;
  - new `--save <file>` and `--fpga-cable`;
  - invalid options now exit with a nonzero status;
  - help text follows the selected locale.
- **Verilog/VHDL export:**
  - upstream now fixes some HDL bugs we had patched ourselves (BitFinder, PLA, subtractor, decoder);
  - it still has the multiplier sign bug, which we fix.

  The autograder's Verilator flow passes 37/37 tests on both builds.
- **Unit tests:** upstream now has a JUnit suite (`src/test`, 638 tests); run it with `./gradlew test`.
- **CI:** upstream added many GitHub workflows (CodeQL, nightly builds, changelog and ticket checks). Only `build.yml` is ours.

---

## 2. Repository map

### Top level

| Path | What it is |
|---|---|
| `build.gradle.kts` | The whole build: version handling, the shadow (fat) jar, `jpackage` installers, checkstyle, and code generation of `BuildInfo`. Course changes: `appVersion` keeps the `-61c` suffix; the jar name has no `-all`. |
| `gradle.properties` | **Version number** (`version = 5.0.0-61c.dev`) and project URL. Change the version here when releasing. |
| `settings.gradle.kts`, `gradlew`, `gradlew.bat`, `gradle/wrapper/` | Gradle wrapper. Always build with `./gradlew`; it downloads the right Gradle. |
| `src/main/java/` | All application source code (see below). |
| `src/main/resources/` | Everything bundled into the jar besides code: strings, icons, built-in help, boards, templates. |
| `src/test/java/` | JUnit tests; mirrors the `src/main/java` package layout. |
| `docs/` | Developer documentation. `docs/61c/` is ours. `docs/img/` holds images for the README. |
| `artwork/` | Source SVGs for the logo and app icon, the fonts they use, and `update_assets.sh`, which regenerates the PNG, ICNS and ICO files. |
| `support/jpackage/` | Per-OS installer resources (icons, file associations) used by `./gradlew createApp/createDmg/createMsi`. |
| `support/Flatpak/`, `snap/` | Linux packaging. The course doesn't use them. |
| `boards_model/` | Board photos and models for upstream's FPGA board editor. The course doesn't use them. |
| `.github/workflows/` | CI. `build.yml` is the course workflow: it builds the jar on push and PR and uploads it as an artifact. |
| `CHANGES.md` | Upstream changelog. Read it before every upstream merge. |
| `checkstyle-suppressions.xml` | Style-check exceptions. The course CI skips checkstyle (`-x checkstyleMain`). |

### `src/main/java/com/cburch/`
Logisim's original author was Carl Burch, hence `cburch`. Almost everything is under `logisim/`.

| Package | What it does | When you'd touch it |
|---|---|---|
| `logisim/Main.java` | Entry point. Parses arguments via `Startup`, installs the FlatLaf themes, then starts the GUI or TTY mode. | Startup-wide behavior, the theme, the macOS Dock icon. |
| `logisim/gui/start/` | **Startup and the command line.** `Startup.java` defines every CLI flag. `TtyInterface.java` is headless mode (`--tty`), used by `test.sh` and the autograder. Also `About*`, `SplashScreen` and `MacOsAdapter`. | Adding or changing a CLI flag; changing the test table format; the About box. |
| `logisim/gui/main/` | The main window: `Frame`, the drawing `Canvas`, `Toolbox`, selection and clipboard (`Selection*`, `Clipboard`), the attribute table (`AttrTable*`), the simulation tree. | Editing behavior, what's allowed on locked components, the layout of the main window. |
| `logisim/gui/menu/` | Menu bar and menu items (`Menu*.java`, `LogisimMenuBar`). | Adding, hiding or disabling menu items. The course disables some Help items in `MenuHelp`. |
| `logisim/gui/log/` | **Timing diagram backend and logging.** `Model`/`Signal` store signal history; `LogFrame` is the window. Course code here: **`VcdWriter`, `TestRecorder`, `TestReference`** (VCD export for Surfer). | Waveforms. |
| `logisim/gui/chrono/` | Drawing code for the timing diagram (`ChronoPanel`, `LeftPanel`, `RightPanel`). | Timing diagram appearance and zoom. |
| `logisim/gui/prefs/`, `logisim/prefs/` | The Preferences window (`gui/prefs`) and the stored preference values (`prefs/AppPreferences.java`, `PrefMonitor*`). **`AppPreferences` holds the default values** for every setting: theme, colors, hotkeys, autosave. | Changing a default (theme, colors, hotkeys); `--no-sync-prefs`. |
| `logisim/gui/icons/` | Icons drawn in Java code (`BaseIcon` subclasses). | See `customizing-appearance.md`. |
| `logisim/gui/generic/`, `opts/`, `hex/`, `appear/`, `search/`, `test/` | Shared widgets (`OptionPane`, `LFrame`); the project options dialog; the hex/memory editor; the custom-appearance editor; Find Action; the test-vector window. | Rarely. |
| `logisim/circuit/` | **The circuit model and simulator core.** `Circuit` (components and wires), `Wire`, `CircuitState` (values during simulation), `Propagator` (event-driven simulation), `CircuitWires` (nets, tunnels, buses), `SubcircuitFactory`, `Analyze` (pin labels used by the TTY table), `RadixOption` (value display formats, including the course "RISC-V instruction" radix). | Simulation semantics, display radixes. |
| `logisim/comp/`, `logisim/instance/` | The component framework. `Component`/`ComponentFactory` are the old API; `InstanceFactory`/`Instance`/`InstanceState` are the newer API used by most components. **`StdAttr`** defines shared attributes (`FACING`, `WIDTH`, `LABEL`, and the course's `LOCKED`). | Adding a component, or an attribute shared by all components. |
| `logisim/data/` | Value types: `Value` (multi-bit signal with 0/1/U/E bits), `BitWidth`, `Location`, `Bounds`, `Attribute`/`Attributes`. | Rarely. |
| `logisim/std/` | **Built-in component libraries,** one folder per library in the explorer pane: `wiring` (Pin, Probe, Tunnel, Splitter, Clock, Constant, Bit Extender…), `gates`, `plexers` (Mux, Decoder, Priority Encoder…), `arith` (Adder, Multiplier, Shifter…), `memory` (Register, RAM, ROM…), `io`, `base` (Text, Label), `ttl`, `tcl`, `hdl`, `bfh`. Each component is usually two files: `Xxx.java` (behavior and drawing) and `XxxHdlGeneratorFactory.java` (Verilog/VHDL export). | Changing how a component simulates or exports to Verilog (most course HDL fixes live here). |
| `logisim/tools/` | Mouse and keyboard tools: `EditTool`, `SelectTool`, `AddTool`, `PokeTool`, `WiringTool`, `MenuTool`, and `SetAttributeAction`. | Locking (the course blocks edits to locked components here). |
| `logisim/file/` | Reading and writing `.circ` XML (`XmlReader`, `XmlWriter`), loading libraries (`Loader`, `LibraryManager`). | `.circ` compatibility; the course `--lib-substitute`. |
| `logisim/proj/` | `Project` (an open file plus its undo history), `Action`, `ProjectActions` (open, new, save). | Rarely. |
| `logisim/fpga/` | **FPGA and HDL export pipeline** used by the autograder's `--test-fpga … HDLONLY`: `designrulecheck/Netlist` (builds the net list; the course names nets after tunnels), `hdlgenerator/` (shared Verilog/VHDL helpers in `Hdl`), `download/Download` (the `--test-fpga` driver; the course `ANNOTATE`/`FORCEDOWNLOAD` options), `gui/Reporter` (the log lines the autograder parses), `file/` (board XML files). | Anything Verilog-related in the autograder. |
| `logisim/analyze/` | Combinational Analysis window (truth tables, K-maps). | Rarely. |
| `logisim/vhdl/`, `logisim/soc/` | VHDL entity component and simulation; System-on-Chip library (Nios2/RV32IM CPUs, buses). | Not used by the course. |
| `logisim/util/` | Shared helpers: `LineBuffer` (templated HDL text), `LocaleManager`, `GraphicsUtil`, `MacCompatibility` (course: `--no-screen-menu-bar`)… | Rarely. |
| `draw/`, `hex/`, `gray/`, `hdl/`, `contracts/` | Drawing library for the appearance editor; hex editor widget; example "gray code" library; HDL text editor; interfaces. | Rarely. |

### `src/main/resources/`

| Path | What it is |
|---|---|
| `resources/logisim/strings/<area>/<area>.properties` | **All UI text,** in English. Translations are in `<area>_de.properties` and similar files. Code reads strings with `S.get("key")`, through each package's `Strings.java`. Add new keys to the English file; missing translations fall back to English. |
| `resources/logisim/icons/` | Image-file icons (GIF/PNG, 16 px). |
| `resources/logisim/img/` | App icons and the About/splash logo. |
| `resources/logisim/boards/` | FPGA board definitions. **`DUMMYBOARD.xml` is required by the autograder** (`--test-fpga … DUMMYBOARD`). |
| `resources/logisim/hdl/` | HDL fragments copied into exports. |
| `resources/logisim/default.templ` | Default template for a new project. |
| `resources/logisim/settings.properties` | List of supported locales. |
| `doc/` | Built-in help (Help → User's Guide). The course disables some Help menu items. `doc/en/html/guide/prefs/pref-cmdline.html` documents the CLI flags, and a unit test checks that every flag appears there. |
| `logback.xml` | Logging configuration. The autograder parses `[main] ERROR com.cburch.logisim.fpga.gui.Reporter - …` lines, so don't change the log pattern. |

---

## 3. Where the course changes live

Search the code for `61C` to find the newer course changes; older course commits don't always carry that comment. `git log develop` lists every course commit.

| Feature | Files |
|---|---|
| **TTY table format** (header row, binary/CSV, one row per output change, stop on `halt`). proj3 `.ref`/`.piperef` files depend on this exact format. | `gui/start/TtyInterface.java` (`runSimulation`, `displayTableRow`, `valueFormat`) |
| **VCD waveform export** (`--vcd`, `--vcd-ref`) for Surfer | `gui/log/VcdWriter.java`, `TestRecorder.java`, `TestReference.java`; option parsing in `Startup.java`; sampling in `TtyInterface.runSimulation` |
| **Verilog export from the command line** (`--fpga-hdl`, `--fpga-workspace`, `--test-fpga … ANNOTATE HDLONLY FORCEDOWNLOAD`) | `gui/start/Startup.java`, `fpga/download/Download.java`, `fpga/gui/Reporter.java` |
| **Verilog generator fixes** (gates, mux, priority encoder up to 64 inputs, shifter, BitSelector, BitAdder, BitExtender, ROM, multiplier sign) | `std/**/*HdlGeneratorFactory.java`, `std/gates/AbstractGateHdlGenerator.java`, `fpga/hdlgenerator/Hdl.java` |
| **Tunnel names in HDL nets** (readable Verilog) | `fpga/designrulecheck/Netlist.java` (`getNetHdlName`), `circuit/CircuitHdlGeneratorFactory.java` |
| **Component locking** (staff lock harness parts; students can't move, delete or edit them) | `instance/StdAttr.java` (`LOCKED`), `std/wiring/PinAttributes.java`, `ProbeAttributes.java`, `circuit/Wire.java`, `tools/{EditTool,SelectTool,AddTool,MenuTool,SetAttributeAction}.java`, `gui/main/{Selection,SelectionActions,LayoutEditHandler}.java`, `comp/Component.java`, `instance/InstanceComponent.java` |
| **RISC-V instruction radix** (probes show `addi x1, x0, 5`) | `circuit/RadixOption.java` |
| **Default radix, "S" key no longer shrinks gates** | `prefs/AppPreferences.java` |
| **`--lib-substitute`** (swap one library for another; used for the staff ALU/regfile) | `gui/start/Startup.java`, `file/Loader.java` |
| **`--no-sync-prefs`, `--no-screen-menu-bar`, macOS fixes** (no Dock icon in TTY mode, Sonoma brew-OpenJDK crash) | `prefs/*PrefMonitor*.java`, `prefs/AppPreferences.java`, `gui/start/Startup.java`, `util/MacCompatibility.java` |
| **Fewer GUI calls in TTY mode** (so the autograder runs headless) | `Main.java`, `file/Loader.java`, `gui/start/Startup.java` |
| **Help menu items disabled; About note; course logo** | `gui/menu/MenuHelp.java`, `gui/start/AboutCredits.java`, `resources/logisim/img/logisim-evolution-logo.png` |
| **Version suffix `-61c.dev`; jar name** | `gradle.properties`, `LogisimVersion.java`, `build.gradle.kts` |

---

## 4. How-to recipes

### Build and run
```bash
./gradlew shadowJar                       # -> build/libs/logisim-evolution-<version>.jar
./gradlew run                             # build and launch the GUI
./gradlew test                            # unit tests (all must pass)
java -jar build/libs/logisim-evolution-5.0.0-61c.dev.jar [file.circ]
```
You need JDK 21 (`brew install --cask temurin@21`). If several JDKs are installed, set `JAVA_HOME=$(/usr/libexec/java_home -v 21)`.

### Check a change against proj3 (do this before every release)
1. Copy the jar to `proj3/<dev>/tools/logisim-evolution.jar`.
2. Run the whole suite, with and without `--pipelined`, and with `--wave` once.
3. Run the old and new jars with `-tty table,binary,csv` on every circuit in `tests/` and the autograder tests, and `diff` the output. It should be identical.
4. Run the autograder Verilog flow (`autograder/proj3b/verilog_components`, `make runtest IS_LOCAL=true`, needs Verilator). The 37 assembly tests should still pass.
5. If the jar's md5 is checked anywhere, regenerate the hashes with `gen_hashes.sh`.

### Add a command-line flag
1. In `gui/start/Startup.java`:
   - add an `ARG_…_LONG` constant;
   - add an `addOption(opts, "argXxxOption", ARG_…, nArgs)` call in `parseArgs`;
   - add a `case ARG_… -> handleArgXxx(startup, opt)`;
   - add the handler, which returns `RC.OK`, `RC.WARN` or `RC.ERROR`;
   - add a getter if `TtyInterface` needs the value.
2. Add `argXxxOption` and `argXxxOptionArgName` to `strings/gui/gui.properties`.
3. Document the flag in `doc/en/…/pref-cmdline.html` and `doc/zh/…/pref-cmdline.html`, or `CommandLineDocumentationTest` will fail.

### Change how a component simulates
Edit `std/<library>/<Component>.java`, usually `propagate(InstanceState)`. Then run the proj3 table diff: any change here can change grading output.

### Fix a Verilog export bug
1. Edit `std/<library>/<Component>HdlGeneratorFactory.java`.
2. The Verilog branch is the `else` of `if (Hdl.isVhdl())`. Template strings use `{{name}}` placeholders, filled in through `LineBuffer.pair(...)`.
3. Test it:
   ```bash
   java -jar <jar> --fpga-hdl Verilog --fpga-workspace /tmp/out --test-fpga cpu.circ cpu DUMMYBOARD ANNOTATE HDLONLY FORCEDOWNLOAD
   ```
   then run the autograder Verilator flow.
4. Many generators have tests in `src/test/java/.../std/`; update or add one.

### Add or change UI text
Edit `strings/<area>/<area>.properties` in English. In code, use `S.get("key")`, or `S.getter("key")` for text that updates when the language changes.

### Change a default setting (theme, colors, hotkeys, autosave)
Edit the matching `create(new PrefMonitor…("name", default))` in `prefs/AppPreferences.java`. A student who already changed the setting keeps their own value. Colors are reapplied on every launch; see `customizing-appearance.md`.

### Merge a future upstream release
1. Add the upstream remote:
   ```bash
   git remote add upstream https://github.com/logisim-evolution/logisim-evolution.git
   git fetch upstream --tags
   ```
2. Read `CHANGES.md` for the new version and note anything in the "Changes in how circuits behave" category (§1).
3. Pick a strategy:
   - **Merge** `vX.Y.Z` into the course branch; or
   - **Replay** the course commits on the new tag, as the v5 port did (see `v5-port-notes.md`). Replaying is easier when upstream has changed a lot.
4. Conflicts concentrate in `Startup.java`, `TtyInterface.java`, `AppPreferences.java`, the HDL generators and `gui.properties`. If upstream fixed a bug we had patched, take upstream's version and drop ours.
5. Run `./gradlew test`, then go through the proj3 checklist above.
6. Check the Java version needed: student setup docs and the autograder image (`autograder/…/setup.sh`) must install it.

### Release to students
1. Set `version` in `gradle.properties`, for example `5.0.0-61c.fa26`.
2. Push to `develop` on `61c-teach/logisim-evolution`. CI uploads the jar as an artifact.
3. Update the course download links (`download_tools.sh` pins a release) and the proj3 `tools/` jar.
