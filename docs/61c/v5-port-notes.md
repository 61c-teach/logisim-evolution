# v5.0.0 port notes (61C)

The course commits from `develop` (3.8.0-61c) were replayed onto upstream `v5.0.0` in order. This file records how each conflict was resolved. Commit hashes refer to `develop`.

## Not carried over
- `dc3d56a78` Shrink distribution jar, and `f3a640bf3` Re-add stripped EN docs. Not replayed, so the v5 jar is about 54 MB instead of about 14 MB. Redo the shrink as one new commit if size matters. `DUMMYBOARD.xml`, which the shrink commit added, was kept because the autograder needs it.
- `7acc0c4c5`, `8b969037c`, `d9188526d`, `e8200bc6f`: upstream v5 already fixes these (Xilinx script, Verilog BitFinder, relative save paths, decoder enable port). Verified by the component tests below.

## Re-done by hand for v5
- `3da0fb809` Verilog emission from the command line (`--fpga-hdl`, `--fpga-workspace`, `ANNOTATE` and `FORCEDOWNLOAD` for `--test-fpga`), merged with v5's `--fpga-cable` and `configurationValid`.
- `8faceb029` TTY table format: `runSimulation` takes the pin-label map and prints the course header row.
- `c880849aa` "S" no longer shrinks gates. v5 hotkeys are preferences, so this is now a change to the default in `AppPreferences`.
- `674e1e93c` Multiplier Verilog sign. v5 has three modes (`SIGNED_MAP`: 0 = unsigned, 1 = signed×unsigned, 2 = signed), and upstream's Verilog still mixed them up.
- `c469269c4` The priority encoder keeps its own `select_enc` attribute so course `.circ` files still load. Upstream raised `select` to 8 bits.
- Locking (`e646ffdf9`, `1b64b78b9`, `934f12bd7`): `StdAttr.LOCKED` is added to v5's three pin attribute lists and to `Wire`, with a lock check before v5's hotkey handling.
- `95c389b47` `--no-sync-prefs`, merged with v5's preference monitors and autosave preferences.
- `1c9d00146` / `36b0a5ad2` CI: `build.yml` rewritten for JDK 21 and actions v4. The other v5 workflows (nightly, codeql, …) are still present.
- `9b946714b` The jar has no `-all` suffix (`archiveClassifier.set("")` for the Shadow plugin on Gradle 9).
- `58ee9eb0b` Subtractor Verilog. First dropped as "fixed upstream", but upstream's `dataA + ~(dataB) + n_bIn` widens `dataB` before inverting, so borrow-out is wrong. Restored the course `dataA - dataB - borrowIn`.
- `93604626c` + `9b249c7ed` PLA Verilog. First dropped as "fixed upstream", but upstream emits a bare `casez` outside any `always` block and the invalid literal `3'0`, which Verilator rejects. Restored the course `always @(*)` if/else-if version (first matching row wins, like the simulator).
- Multiplier signed×unsigned mode (new in v5, not used by the course): `$signed(a) * $unsigned(b)` is an unsigned product in Verilog. Changed it to `$signed(a) * $signed({1'b0, b})`.
- `c880849aa` also removed "S" from Preferences → reset hotkeys, which still put it back.
- `--fpga-hdl` / `--fpga-workspace` help text keys fixed (`argFpgaHdl`, `argFpgaWorkspace`). Added the missing `argNoScreenMenuBarOption` string, which 3.8 never had either.
- Uncommitted 3.8 work, `--vcd`/`--vcd-ref` waveform export (`gui/log/VcdWriter`, `TestRecorder`, `TestReference`), also ported.

## Tests changed to match course behavior
- `LogisimVersionTest`: suffixes may start with a digit (`61c`).
- `PriorityEncoderHdlGeneratorFactoryTest`: uses `ATTR_SELECT_ENC`; `s_selectVector0` is 65 bits.
- `CircuitHdlGeneratorFactoryTest`: stubs `getNetHdlName` (course tunnel-named nets).
- `pref-cmdline.html` (en, zh) documents the course command-line flags.

## Verification (staff CPU from proj3)
- `./gradlew test`: 638/638 pass. `PrefMonitorKeyStrokeTest` (upstream) failed once out of four runs; it uses the real preference store.
- Component Verilog tests: 41 one-component circuits (gates, mux, decoder, 8- and 64-input priority encoder, BitSelector, subtractor, multiplier in all modes, shifter in every mode at 8 and 2 bits, BitAdder, BitFinder, bit extender, PLA, ROM, ROM through splitters, tunnels, bit extender with no output). Each one is checked in Logisim (`-w` test vectors), then exported with the autograder flags and run in Verilator against the same vectors. 41/41 pass on v5 and 40/40 on 3.8 (signed×unsigned is v5-only). Before the fixes above, v5 failed subtractor, PLA and signed×unsigned.
- Parallel exports overwrite each other's `--fpga-workspace` through preference sync, on both 3.8 and v5. With `--no-sync-prefs` it works (0/24 missing vs 4/24). The autograder runs one export at a time, but adding `--no-sync-prefs` to its Makefile would be safer.
- proj3 `test.sh part_a/part_b --pipelined` with the staff CPU: 41/41 on both.
- CLI: `-S` library substitution, `--vcd`/`--vcd-ref`, `--no-sync-prefs`, `--no-screen-menu-bar`, DRC width-mismatch reporting, ROM with no connections, and no Dock icon in TTY mode (`UIElement`) all behave the same as 3.8.
- VCD: same as 3.8 while the program runs. After the CPU fetches past the end of the program, v5 shows the undefined control signals as `z`, where 3.8 showed 0 (the same floating-value change as below).
- `--tty table,binary,csv` on 122 proj3 + autograder test circuits: 119 byte-identical to 3.8. The 3 that differ (`unit-divu`, `unit-remu`, `everything-prime`) fail on both builds, because the staff CPU doesn't implement those instructions. v5 shows those outputs as undefined (`U`) where 3.8 showed 0.
- Autograder Verilog flow (`emit` + Verilator, 37 asm tests): 37/37 pass, the same as 3.8.
- Requires Java 21. The autograder `setup.sh` (openjdk-17) and student setup instructions must be updated.
