/**
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along 
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
 */

package com.cburch.logisim.std.arith;

import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHdlGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.Hdl;
import com.cburch.logisim.fpga.hdlgenerator.HdlParameters;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.LineBuffer;
import java.util.HashMap;

public class BitFinderHdlGeneratorFactory extends AbstractHdlGeneratorFactory {

  private static final String BIT_FINDER_MODE_STRING = "bitFinderMode";
  private static final int BIT_FINDER_MODE_ID = -1;

  public BitFinderHdlGeneratorFactory() {
    super();
    myParametersList
        .add(BIT_FINDER_MODE_STRING, BIT_FINDER_MODE_ID, HdlParameters.MAP_ATTRIBUTE_OPTION, BitFinder.TYPE,
            new HashMap<AttributeOption, Integer>() {{
              put(BitFinder.LOW_ONE, 0);
              put(BitFinder.HIGH_ONE, 1);
              put(BitFinder.LOW_ZERO, 2);
              put(BitFinder.HIGH_ZERO, 3);
            }});
    getWiresPortsDuringHDLWriting = true;
  }

  @Override
  public void getGenerationTimeWiresPorts(Netlist theNetlist, AttributeSet attrs) {
    final var nrOfBits = attrs.getValue(StdAttr.WIDTH).getWidth();
    myPorts.add(Port.INPUT, "dataA", nrOfBits, BitFinder.IN, StdAttr.WIDTH);
    myPorts.add(Port.OUTPUT, "result", BitFinder.computeOutputBits(nrOfBits-1), BitFinder.OUT);
    myPorts.add(Port.OUTPUT, "present", 1, BitFinder.PRESENT);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist theNetList, AttributeSet attrs) {
    final var nrOfBits = attrs.getValue(StdAttr.WIDTH).getWidth();
    final var nrOfOutputBits = BitFinder.computeOutputBits(nrOfBits - 1);
    final var contents = LineBuffer.getBuffer()
            .pair("finderMode", BIT_FINDER_MODE_STRING)
            .pair("nrOfOutputBits1", nrOfOutputBits - 1);
    contents.addRemarkBlock("""
          BitFinderMode represents when:
          0 : Lowest-order 1
          1 : Highest-order 1
          2 : Lowest-order 0
          3 : Highest-order 0
          """);
    if (Hdl.isVerilog()) {
      contents.add("reg [{{nrOfOutputBits1}}:0] temp;");
      contents.add("");
      contents.add("always @(*) begin");
      contents.add("   temp = 0;");
      contents.add("   if ({{finderMode}} == 0) begin");
      for (int i = nrOfBits-1; i >= 0; i--) {
        contents.add("      temp = dataA[{{1}}] == {{2}} ? {{3}} : temp;", i, 1, i);
      }
      contents.add("   end");
      contents.add("   else if ({{finderMode}} == 1) begin");
      for (int i = 0; i < nrOfBits; i++) {
        contents.add("      temp = dataA[{{1}}] == {{2}} ? {{3}} : temp;", i, 1, i);
      }
      contents.add("   end");
      contents.add("   else if ({{finderMode}} == 2) begin");
      for (int i = nrOfBits-1; i >= 0; i--) {
        contents.add("      temp = dataA[{{1}}] == {{2}} ? {{3}} : temp;", i, 0, i);
      }
      contents.add("   end");
      contents.add("   else begin");
      for (int i = 0; i < nrOfBits; i++) {
        contents.add("      temp = dataA[{{1}}] == {{2}} ? {{3}} : temp;", i, 0, i);
      }
      contents.add("   end");
      contents.add("end");
      contents.add("assign present = ({{finderMode}} < 2) ? |dataA : |(~dataA);");
      contents.add("assign result = temp;");
    }
    return contents.empty();
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    return Hdl.isVerilog();
  }
}
