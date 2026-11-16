/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.log;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.generated.BuildInfo;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.std.wiring.Tunnel;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Writes a Value Change Dump (VCD) of a headless simulation, for viewing in waveform viewers such
 * as Surfer or GTKWave. Every named signal in the circuit hierarchy is dumped: pins, probes,
 * tunnels, registers and other loggable components, with one VCD scope per subcircuit instance.
 *
 * <p>When a {@link TestReference} is given, the expected value of each output column is dumped
 * next to it as {@code <name>_expected}, together with a 1-bit {@code MISMATCH} signal that is high
 * for every row that differs from the reference.
 */
public class VcdWriter implements Closeable {
  /** Simulated time per clock toggle (half a clock cycle), in VCD time units (ns). */
  public static final long HALF_CYCLE = 5;

  private static class Var {
    final String id;
    final int width;
    final Supplier<Value> fetch; // null for synthetic signals
    String last;

    Var(String id, int width, Supplier<Value> fetch) {
      this.id = id;
      this.width = width;
      this.fetch = fetch;
    }
  }

  private final BufferedWriter out;
  private final List<Var> vars = new ArrayList<>();
  private final TestRecorder recorder;
  private final TestReference ref;
  private final Var[] expectedVars; // per recorded column, may contain nulls
  private Var mismatchVar;
  private int nextId = 0;
  private long lastTime = -1;
  private boolean anyMismatch = false;
  private long firstMismatchTime = -1;
  private int firstMismatchRow = -1;

  public VcdWriter(File file, CircuitState root, TestRecorder recorder, TestReference ref)
      throws IOException {
    this.recorder = recorder;
    this.ref = ref;
    this.out =
        new BufferedWriter(
            new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8));
    expectedVars = new Var[recorder == null ? 0 : recorder.getNames().size()];

    out.write("$date " + new java.util.Date() + " $end\n");
    out.write("$version Logisim-evolution " + BuildInfo.version + " $end\n");
    out.write("$timescale 1ns $end\n");
    final var rootName = sanitize(root.getCircuit().getName(), "top");
    out.write("$scope module " + rootName + " $end\n");
    declareScope(root, true);
    if (ref != null && recorder != null) {
      final var names = recorder.getNames();
      for (var c = 0; c < names.size(); c++) {
        final var refCol = ref.indexOf(names.get(c));
        if (refCol < 0) continue;
        final var width = Math.max(1, ref.getWidth(refCol));
        expectedVars[c] = declare(sanitize(names.get(c), "out") + "_expected", width, null);
      }
      mismatchVar = declare("MISMATCH", 1, null);
    }
    out.write("$upscope $end\n");
    out.write("$enddefinitions $end\n");
  }

  private void declareScope(CircuitState state, boolean isRoot) throws IOException {
    final Set<String> used = new HashSet<>();
    final var subcircuits = new ArrayList<Component>();
    for (final var comp : state.getCircuit().getNonWires()) {
      if (comp.getFactory() instanceof SubcircuitFactory) {
        subcircuits.add(comp);
        continue;
      }
      if (comp.getFactory() instanceof Tunnel) {
        final var label = comp.getAttributeSet().getValue(StdAttr.LABEL);
        if (label == null || label.isEmpty()) continue;
        final var name = sanitize(label, "tunnel");
        if (!used.add(name)) continue; // all tunnels with one label are the same net
        final var loc = comp.getLocation();
        final var width = comp.getAttributeSet().getValue(StdAttr.WIDTH).getWidth();
        if (width <= 0) continue;
        declare(name, width, () -> state.getValue(loc));
        continue;
      }
      final var log = (LoggableContract) comp.getFeature(LoggableContract.class);
      if (log == null) continue;
      final var opts = log.getLogOptions();
      if (opts != null && opts.length > 0) continue; // e.g. RAM contents: too much to dump
      String name = null;
      if (isRoot && recorder != null) {
        final var col = recorder.indexOf(comp);
        if (col >= 0) name = recorder.getNames().get(col);
      }
      if (name == null) name = componentName(comp);
      name = unique(sanitize(name, "signal"), used);
      final var bw = log.getBitWidth(null);
      final var width = bw == null ? 0 : bw.getWidth();
      if (width <= 0) continue;
      declare(name, width, () -> log.getLogValue(state, null));
    }
    for (final var comp : subcircuits) {
      final var factory = (SubcircuitFactory) comp.getFactory();
      final var sub = factory.getSubstate(state, comp);
      if (sub == null) continue;
      var name = comp.getAttributeSet().getValue(StdAttr.LABEL);
      if (name == null || name.isEmpty()) name = componentName(comp);
      out.write("$scope module " + unique(sanitize(name, "sub"), used) + " $end\n");
      declareScope(sub, false);
      out.write("$upscope $end\n");
    }
  }

  private static String componentName(Component comp) {
    final var label = comp.getAttributeSet().getValue(StdAttr.LABEL);
    if (label != null && !label.isEmpty()) return label;
    final var loc = comp.getLocation();
    final var base =
        comp.getFactory() instanceof SubcircuitFactory sub
            ? sub.getSubcircuit().getName()
            : comp.getFactory().getName();
    return base + "_" + loc.getX() + "_" + loc.getY();
  }

  private static String unique(String name, Set<String> used) {
    var ret = name;
    var i = 2;
    while (!used.add(ret)) ret = name + "_" + i++;
    return ret;
  }

  private static String sanitize(String s, String fallback) {
    if (s == null || s.isEmpty()) return fallback;
    final var b = new StringBuilder();
    for (final var c : s.toCharArray()) {
      b.append(Character.isLetterOrDigit(c) || c == '_' ? c : '_');
    }
    return b.toString();
  }

  private Var declare(String name, int width, Supplier<Value> fetch) throws IOException {
    final var v = new Var(makeId(nextId++), width, fetch);
    vars.add(v);
    out.write("$var wire " + width + " " + v.id + " " + name + " $end\n");
    return v;
  }

  private static String makeId(int n) {
    final var b = new StringBuilder();
    do {
      b.append((char) ('!' + (n % 94)));
      n /= 94;
    } while (n > 0);
    return b.toString();
  }

  private static String encode(Value v, int width) {
    final var b = new StringBuilder(width);
    for (var i = width - 1; i >= 0; i--) {
      final var bit = v.get(i);
      if (bit == Value.TRUE) b.append('1');
      else if (bit == Value.FALSE) b.append('0');
      else if (bit == Value.UNKNOWN) b.append('z'); // floating
      else b.append('x'); // error / conflict
    }
    return b.toString();
  }

  private void write(long time, Var v, String bits) throws IOException {
    if (bits.equals(v.last)) return;
    if (time != lastTime) {
      out.write("#" + time + "\n");
      lastTime = time;
    }
    v.last = bits;
    if (v.width == 1) out.write(bits + v.id + "\n");
    else out.write("b" + bits + " " + v.id + "\n");
  }

  /** Dumps the current value of every circuit signal at the given time. */
  public void sample(long time) throws IOException {
    for (final var v : vars) {
      if (v.fetch == null) continue;
      write(time, v, encode(v.fetch.get(), v.width));
    }
  }

  /**
   * Records that the output table gained a row at the given time, and dumps the expected values
   * and mismatch flag for that row.
   */
  public void row(long time, int rowIdx) throws IOException {
    if (ref == null || recorder == null) return;
    final var actual = recorder.getRows().get(rowIdx);
    final var names = recorder.getNames();
    var bad = false;
    for (var c = 0; c < names.size(); c++) {
      final var ev = expectedVars[c];
      if (ev == null) continue;
      final var expected = ref.get(rowIdx, ref.indexOf(names.get(c)));
      if (expected == null) {
        write(time, ev, "x".repeat(ev.width));
        continue;
      }
      write(time, ev, encode(TestReference.parseBinary(expected), ev.width));
      if (!expected.equals(actual[c])) bad = true;
    }
    write(time, mismatchVar, bad ? "1" : "0");
    if (bad && !anyMismatch) {
      anyMismatch = true;
      firstMismatchTime = time;
      firstMismatchRow = rowIdx;
    }
  }

  /** Closes the dump at the given end time and returns a one-line summary of the comparison. */
  public String finish(long time) throws IOException {
    if (time > lastTime) out.write("#" + time + "\n");
    out.close();
    if (ref == null || recorder == null) return null;
    if (anyMismatch) {
      return "first mismatch at row " + firstMismatchRow + " (t=" + firstMismatchTime + "ns)";
    }
    if (recorder.getRowCount() < ref.getRowCount()) {
      return "circuit halted after " + recorder.getRowCount() + " rows but the reference has "
          + ref.getRowCount();
    }
    return "no mismatches";
  }

  @Override
  public void close() throws IOException {
    out.close();
  }
}
