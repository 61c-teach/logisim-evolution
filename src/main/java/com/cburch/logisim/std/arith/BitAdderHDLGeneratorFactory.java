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

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.fpga.gui.FPGAReport;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.std.plexers.Plexers;

import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

import static com.cburch.logisim.std.Strings.S;

public class BitAdderHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

    @Override
    public String getComponentStringIdentifier() {
        return "BitAdder";
    }

    @Override
    public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
        SortedMap<String, Integer> Inputs = new TreeMap<String, Integer>();
        int num_inputs = attrs.getValue(BitAdder.NUM_INPUTS).intValue();
        for (int i = 0; i < num_inputs; i++) {
            Inputs.put(String.format("In%d", i), attrs.getValue(StdAttr.WIDTH).getWidth());
        }
        return Inputs;
    }

    @Override
    public ArrayList<String> GetModuleFunctionality(
            Netlist TheNetlist, AttributeSet attrs, FPGAReport Reporter, String HDLType) {
        ArrayList<String> Contents = new ArrayList<String>();
        int nrOfBits = attrs.getValue(StdAttr.WIDTH).getWidth();
        int num_inputs = attrs.getValue(BitAdder.NUM_INPUTS).intValue();
        int output_bits = computeOutputBits(nrOfBits, num_inputs);
        if (HDLType.equals(VHDL)) {
            Contents.add("");
        } else {
            Contents.add("");
            Contents.add("   reg [" + Integer.toString(output_bits-1) + ":0] temp;");
            Contents.add("   integer i;");
            Contents.add("");
            Contents.add("   always @(*) begin");
            Contents.add("      temp = 0;");
            Contents.add("      for (i = 0; i<" + Integer.toString(nrOfBits) + "; i = i + 1) begin");
            for (int i = 0; i < num_inputs; i++) {
                Contents.add(String.format("         temp = temp + In%s[i];", i));
            }
            Contents.add("      end");
            Contents.add("   end");
            Contents.add("   assign Result = temp;");
        }
        return Contents;
    }


    @Override
    public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
        SortedMap<String, Integer> Outputs = new TreeMap<String, Integer>();
        int inputbits = attrs.getValue(StdAttr.WIDTH).getWidth();
        int num_inputs = attrs.getValue(BitAdder.NUM_INPUTS).intValue();
        Outputs.put("Result", computeOutputBits(inputbits, num_inputs));
        return Outputs;
    }

    private int computeOutputBits(int width, int inputs) {
        int maxBits = width * inputs;
        int outWidth = 1;
        while ((1 << outWidth) <= maxBits) outWidth++;
        return outWidth;
    }

    @Override
    public SortedMap<Integer, String> GetParameterList(AttributeSet attrs) {
        SortedMap<Integer, String> Parameters = new TreeMap<Integer, String>();
        return Parameters;
    }

    @Override
    public SortedMap<String, Integer> GetParameterMap(
            Netlist Nets, NetlistComponent ComponentInfo, FPGAReport Reporter) {
        SortedMap<String, Integer> ParameterMap = new TreeMap<String, Integer>();
        return ParameterMap;
    }

    @Override
    public SortedMap<String, String> GetPortMap(
            Netlist Nets, Object MapInfo, FPGAReport Reporter, String HDLType) {
        SortedMap<String, String> PortMap = new TreeMap<String, String>();
        if (!(MapInfo instanceof NetlistComponent)) return PortMap;
        NetlistComponent ComponentInfo = (NetlistComponent) MapInfo;
        int num_inputs = ComponentInfo.GetComponent().getAttributeSet().getValue(BitAdder.NUM_INPUTS).intValue();
        PortMap.putAll(GetNetMap("Result", true, ComponentInfo, 0, Reporter, HDLType, Nets));
        for (int i = 0; i < num_inputs; i++) {
            PortMap.putAll(GetNetMap(String.format("In%d", i), true, ComponentInfo, i+1, Reporter, HDLType, Nets));
        }
        return PortMap;
    }

    @Override
    public String GetSubDir() {
        return "arithmetic";
    }

    @Override
    public SortedMap<String, Integer> GetWireList(AttributeSet attrs, Netlist Nets) {
        SortedMap<String, Integer> Wires = new TreeMap<String, Integer>();
        return Wires;
    }

    @Override
    public boolean HDLTargetSupported(String HDLType, AttributeSet attrs) {
        return HDLType.equals(VERILOG);
    }
}
