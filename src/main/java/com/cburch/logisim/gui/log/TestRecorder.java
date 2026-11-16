/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.log;

import com.cburch.logisim.circuit.Analyze;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.std.wiring.Pin;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Records the output table of a halting test circuit exactly the way {@code --tty table,binary}
 * does: the output pins of the top-level circuit (except {@code halt}), named by {@link
 * Analyze#getPinLabels}, with a new row recorded only when some output value changes.
 */
public class TestRecorder {
  public static final String HALT_PIN = "halt";

  private final List<Instance> pins = new ArrayList<>();
  private final List<String> names = new ArrayList<>();
  private final Instance haltPin;
  private final List<String[]> rows = new ArrayList<>();
  private String[] prev;

  public TestRecorder(Circuit circuit) {
    Instance halt = null;
    for (final var entry : Analyze.getPinLabels(circuit).entrySet()) {
      final var pin = entry.getKey();
      if (Pin.FACTORY.isInputPin(pin)) continue;
      if (halt == null && HALT_PIN.equals(entry.getValue())) {
        halt = pin;
        continue;
      }
      pins.add(pin);
      names.add(entry.getValue());
    }
    haltPin = halt;
  }

  public boolean hasHaltPin() {
    return haltPin != null;
  }

  public List<String> getNames() {
    return Collections.unmodifiableList(names);
  }

  /** Column index of a top-level pin component, or -1 if it is not a recorded output. */
  public int indexOf(Component comp) {
    for (var i = 0; i < pins.size(); i++) {
      if (pins.get(i).getComponent() == comp) return i;
    }
    return -1;
  }

  public List<String[]> getRows() {
    return Collections.unmodifiableList(rows);
  }

  public int getRowCount() {
    return rows.size();
  }

  public boolean isHalted(CircuitState state) {
    if (haltPin == null) return false;
    return Pin.FACTORY.getValue(state.getInstanceState(haltPin)).equals(com.cburch.logisim.data.Value.TRUE);
  }

  /**
   * Samples the outputs. Returns the index of the new row if any output changed since the last
   * sample (or this is the first sample), otherwise -1.
   */
  public int sample(CircuitState state) {
    final var cur = new String[pins.size()];
    for (var i = 0; i < pins.size(); i++) {
      cur[i] = Pin.FACTORY.getValue(state.getInstanceState(pins.get(i))).toBinaryString();
    }
    if (prev != null && Arrays.equals(prev, cur)) return -1;
    prev = cur;
    rows.add(cur);
    return rows.size() - 1;
  }
}
