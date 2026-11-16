/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.log;

import com.cburch.logisim.data.Value;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Expected output of a halting test circuit, as produced by {@code --tty table,binary,csv}: a
 * header line of output pin names followed by one line of binary values per row. A new row is
 * printed every time any output changes, so row {@code k} of a reference lines up with row {@code
 * k} recorded by {@link TestRecorder}.
 */
public class TestReference {
  private final File file;
  private final List<String> names;
  private final List<String[]> rows;

  private TestReference(File file, List<String> names, List<String[]> rows) {
    this.file = file;
    this.names = names;
    this.rows = rows;
  }

  public static TestReference load(File file) throws IOException {
    final var lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
    final var names = new ArrayList<String>();
    final var rows = new ArrayList<String[]>();
    for (final var raw : lines) {
      final var line = raw.strip();
      if (line.isEmpty()) continue;
      final var cells = line.split(",", -1);
      for (var i = 0; i < cells.length; i++) cells[i] = cells[i].strip();
      if (names.isEmpty()) {
        Collections.addAll(names, cells);
      } else {
        rows.add(cells);
      }
    }
    if (names.isEmpty()) throw new IOException("empty reference file: " + file);
    return new TestReference(file, Collections.unmodifiableList(names), rows);
  }

  public File getFile() {
    return file;
  }

  public List<String> getNames() {
    return names;
  }

  public int getRowCount() {
    return rows.size();
  }

  public int indexOf(String name) {
    return names.indexOf(name);
  }

  /** Raw cell text, or null if the row or column does not exist. */
  public String get(int row, int col) {
    if (row < 0 || row >= rows.size() || col < 0) return null;
    final var cells = rows.get(row);
    return col < cells.length ? cells[col] : null;
  }

  /** Width (number of bits) of a column, judged from its first row. */
  public int getWidth(int col) {
    final var s = get(0, col);
    return s == null ? 0 : s.length();
  }

  /** Converts a binary cell (as printed by Value.toBinaryString) back into a Value. */
  public static Value parseBinary(String bits) {
    if (bits == null || bits.isEmpty()) return Value.NIL;
    final var n = bits.length();
    final var vals = new Value[n];
    for (var i = 0; i < n; i++) {
      final var c = bits.charAt(n - 1 - i); // vals[0] is the least significant bit
      if (c == '1') vals[i] = Value.TRUE;
      else if (c == '0') vals[i] = Value.FALSE;
      else if (c == Value.ERRORCHAR || c == 'E') vals[i] = Value.ERROR;
      else vals[i] = Value.UNKNOWN;
    }
    return Value.create(vals);
  }
}
