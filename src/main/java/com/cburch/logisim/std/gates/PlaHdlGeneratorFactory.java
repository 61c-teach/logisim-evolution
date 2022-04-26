/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.gates;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHdlGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.Hdl;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.util.LineBuffer;

import java.util.ArrayList;

public class PlaHdlGeneratorFactory extends AbstractHdlGeneratorFactory {

  public PlaHdlGeneratorFactory() {
    super();
    myPorts
        .add(Port.INPUT, "index", 0, Pla.IN_PORT, Pla.ATTR_IN_WIDTH)
        .add(Port.OUTPUT, "result", 0, Pla.OUT_PORT, Pla.ATTR_OUT_WIDTH);
  }

  private static String bits(char[] b) {
    final var s = new StringBuilder();
    for (final var c : b) s.insert(0, ((c == '0' || c == '1') ? c : Hdl.wildcard()));
    if (Hdl.isVerilog()) return s.toString();
    else {
      if (b.length == 1) return "'" + s + "'";
      else return "\"" + s + "\"";
    }
  }

  private static String zeros(int sz) {
    final var s = new StringBuilder();
    s.append("0".repeat(sz));
    if (sz == 1) return "'" + s + "'";
    else return "\"" + s + "\"";
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer().addVhdlKeywords().empty();
    final var tt = attrs.getValue(Pla.ATTR_TABLE);
    final var inSz = attrs.getValue(Pla.ATTR_IN_WIDTH).getWidth();
    final var outSz = attrs.getValue(Pla.ATTR_OUT_WIDTH).getWidth();
    if (Hdl.isVhdl()) {
      var leader = "result <= ";
      if (tt.rows().isEmpty()) {
        contents.add("{{1}}{{2}};", leader, zeros(outSz));
      } else {
        for (PlaTable.Row r : tt.rows()) {
          contents.add("{{1}}{{2}} {{when}} std_match(Index, {{3}}) {{else}}", leader, bits(r.outBits), bits(r.inBits));
          leader = " ".repeat(leader.length());
        }
        contents.add("{{1}}{{2}};", leader, zeros(outSz));
      }
    } else {
      contents.add("reg [{{1}}:0] temp;", outSz-1);
      contents.add("always @(*) begin");
      if (tt.rows().isEmpty()) {
        contents.add("   temp = {{1}};", Hdl.getZeroVector(outSz, true));
      } else {
        String leader = "   if ";
        for (PlaTable.Row r : tt.rows()) {
          final var ifCondition = new ArrayList<String>();
          for (int i = 0; i < r.inBits.length; i++) {
            if (r.inBits[i] != '0' && r.inBits[i] != '1') continue;
            ifCondition.add(String.format("index[%d] == 1'b%c", i, r.inBits[i]));
          }
          contents.add(leader + "({{1}}) begin", String.join(" && ", ifCondition));
          contents.add("      // index ==? " + bits(r.inBits));
          contents.add("      // Comment: " + r.comment);
          contents.add("      temp = {{1}}'b{{2}};", outSz, bits(r.outBits));
          contents.add("   end");
          leader = "   else if ";
        }
        contents.add("   else begin");
        contents.add("      temp = {{1}};", Hdl.getZeroVector(outSz, true));
        contents.add("   end");
      }
      contents.add("end");
      contents.add("assign result = temp;");
    }
    return contents.empty();
  }
}
