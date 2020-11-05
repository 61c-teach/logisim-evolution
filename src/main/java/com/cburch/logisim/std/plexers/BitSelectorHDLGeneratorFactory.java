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

package com.cburch.logisim.std.plexers;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.fpga.gui.FPGAReport;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.instance.StdAttr;

import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

public class BitSelectorHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  private static final String InputBitsStr = "NrOfInputBits";
  private static final int InputBitsId = -1;
  private static final String OutputsBitsStr = "NrOfOutputBits";
  private static final int OutputsBitsId = -2;
  private static final String SelectBitsStr = "NrOfSelBits";
  private static final int SelectBitsId = -3;
  private static final String ExtendedBitsStr = "NrOfExtendedBits";
  private static final int ExtendedBitsId = -4;

  private int calculateSelectBits(int input_bits, int output_bits) {
  int groups = (input_bits + output_bits - 1) / output_bits - 1;
  int selectBits = 1;
    if(groups >0) {
      while (groups != 1) {
        groups >>= 1;
        selectBits++;
      }
    }
    return selectBits;
  }

  private int calculateExtendedBits(int sel_bits, int output_bits) {
    int nr_of_slices = 1;
    for (int i = 0; i < sel_bits; i++) {
      nr_of_slices <<= 1;
    }
    return nr_of_slices * output_bits;
  }

  @Override
  public String getComponentStringIdentifier() {
    return "BITSELECTOR";
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> Inputs = new TreeMap<String, Integer>();
    int output_bits = attrs.getValue(BitSelector.GROUP_ATTR).getWidth();
    int input_bits = attrs.getValue(StdAttr.WIDTH).getWidth();
    int select_bits = calculateSelectBits(input_bits, output_bits);
    Inputs.put("DataIn", input_bits);
    Inputs.put("Sel", select_bits);
    return Inputs;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(
    Netlist TheNetlist, AttributeSet attrs, FPGAReport Reporter, String HDLType) {
    ArrayList<String> Contents = new ArrayList<String>();
    int output_bits = attrs.getValue(BitSelector.GROUP_ATTR).getWidth();
    int input_bits = attrs.getValue(StdAttr.WIDTH).getWidth();
    int select_bits = calculateSelectBits(input_bits, output_bits);
    int extended_bits = calculateExtendedBits(select_bits, output_bits);
    if (HDLType.equals(VHDL)) {
      Contents.add("");
    } else {
      if (output_bits > 1) {
        Contents.add("   reg["+ (output_bits-1) +":0] temp;");
        if (extended_bits > input_bits) {
          Contents.add("   assign s_extended_vector[" + (extended_bits - 1) + ":" + input_bits + "] = 0;");
        }
        Contents.add("   assign s_extended_vector[" + (input_bits - 1) + ":0] = DataIn;");
        Contents.add("   always @(*) begin");
        Contents.add("      case (Sel)");
        for (int i = 0; i < Math.pow(2, select_bits); i++) {
          Contents.add("         " + select_bits + "'d" + i + ": temp = s_extended_vector[" + ((i + 1) * output_bits - 1) + ":" + (i * output_bits) + "];");
        }
        Contents.add("         default: temp = s_extended_vector[" + (output_bits - 1) + ":0];");
        Contents.add("      endcase");
        Contents.add("   end");
        Contents.add("   assign DataOut = temp;");
      } else {
        Contents.add("   reg temp;");
        Contents.add("   always @(*) begin");
        Contents.add("      case (Sel)");
        for (int i = 0; i < Math.pow(2, select_bits); i++) {
          Contents.add("         " + select_bits + "'d" + i + ": temp = DataIn[" + i + "];");
        }
        Contents.add("         default: temp = DataIn[0];");
        Contents.add("      endcase");
        Contents.add("   end");
        Contents.add("   assign DataOut = temp;");
      }
    }
    return Contents;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> Outputs = new TreeMap<String, Integer>();
    int output_bits = attrs.getValue(BitSelector.GROUP_ATTR).getWidth();
    Outputs.put("DataOut", output_bits);
    return Outputs;
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
    PortMap.putAll(GetNetMap("DataIn", true, ComponentInfo, 1, Reporter, HDLType, Nets));
    PortMap.putAll(GetNetMap("Sel", true, ComponentInfo, 2, Reporter, HDLType, Nets));
    PortMap.putAll(GetNetMap("DataOut", true, ComponentInfo, 0, Reporter, HDLType, Nets));
    return PortMap;
  }

  @Override
  public String GetSubDir() {
    return "plexers";
  }

  @Override
  public SortedMap<String, Integer> GetWireList(AttributeSet attrs, Netlist Nets) {
    SortedMap<String, Integer> Wires = new TreeMap<String, Integer>();
    int output_bits = attrs.getValue(BitSelector.GROUP_ATTR).getWidth();
    int input_bits = attrs.getValue(StdAttr.WIDTH).getWidth();
    int select_bits = calculateSelectBits(input_bits, output_bits);
    int extended_bits = calculateExtendedBits(select_bits, output_bits);
    Wires.put("s_extended_vector", extended_bits);
    return Wires;
  }

  @Override
  public boolean HDLTargetSupported(String HDLType, AttributeSet attrs) {
    return HDLType.equals(VERILOG);
  }
}
