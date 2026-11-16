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

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHdlGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.Hdl;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.LineBuffer;

public class BitAdderHdlGeneratorFactory extends AbstractHdlGeneratorFactory {

    public BitAdderHdlGeneratorFactory() {
        super();
        getWiresPortsDuringHDLWriting = true;
    }

    @Override
    public void getGenerationTimeWiresPorts(Netlist theNetlist, AttributeSet attrs) {
        final var nrOfInputs = attrs.getValue(BitAdder.NUM_INPUTS);
        final var bitWidth = attrs.getValue(StdAttr.WIDTH).getWidth();
        for (var input = 1; input <= nrOfInputs; input++) {
            myPorts.add(Port.INPUT, String.format("input%d", input), bitWidth, input, StdAttr.WIDTH);
        }
        myPorts.add(Port.OUTPUT, "result", BitAdder.computeOutputBits(bitWidth, nrOfInputs), BitAdder.OUT, StdAttr.WIDTH);
    }

    @Override
    public LineBuffer getModuleFunctionality(Netlist theNetList, AttributeSet attrs) {
        final var nrOfBits = attrs.getValue(StdAttr.WIDTH).getWidth();
        final var nrOfInputs = attrs.getValue(BitAdder.NUM_INPUTS);
        int output_bits = BitAdder.computeOutputBits(nrOfBits, nrOfInputs);
        final var contents = LineBuffer.getBuffer()
                .pair("nrOfBits", nrOfBits)
                .pair("nrOfOutputBits1", output_bits - 1);
        if (Hdl.isVerilog()) {
            contents.add("reg [{{nrOfOutputBits1}}:0] temp;");
            contents.add("integer i;");
            contents.add("");
            contents.add("always @(*) begin");
            contents.add("   temp = 0;");
            contents.add("   for (i = 0; i<{{nrOfBits}}; i = i + 1) begin");
            for (int i = 1; i <= nrOfInputs; i++) {
                contents.add("      temp = temp + input{{1}}[i];", i);
            }
            contents.add("   end");
            contents.add("end");
            contents.add("assign result = temp;");
        }
        return contents.empty();
    }

    @Override
    public boolean isHdlSupportedTarget(AttributeSet attrs) {
        return Hdl.isVerilog();
    }
}
