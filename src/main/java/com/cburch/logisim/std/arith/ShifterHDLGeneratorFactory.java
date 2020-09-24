/*
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
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.fpga.gui.FPGAReport;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.instance.StdAttr;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

public class ShifterHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  private static final String ShiftModeStr = "ShifterMode";
  private static final int ShiftModeId = -1;

  @Override
  public String getComponentStringIdentifier() {
    return "Shifter";
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> Inputs = new TreeMap<>();
    Inputs.put("DataA", attrs.getValue(StdAttr.WIDTH).getWidth());
    Inputs.put("ShiftAmount", getNrofShiftBits(attrs));
    return Inputs;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(
      Netlist TheNetlist, AttributeSet attrs, FPGAReport Reporter, String HDLType) {
    ArrayList<String> Contents = new ArrayList<>();
    int nrOfBits = attrs.getValue(StdAttr.WIDTH).getWidth();
    if (HDLType.equals(VHDL)) {
      Contents.add(
          "   -----------------------------------------------------------------------------");
      Contents.add(
          "   --- ShifterMode represents when:                                          ---");
      Contents.add(
          "   --- 0 : Logical Shift Left                                                ---");
      Contents.add(
          "   --- 1 : Rotate Left                                                       ---");
      Contents.add(
          "   --- 2 : Logical Shift Right                                               ---");
      Contents.add(
          "   --- 3 : Arithmetic Shift Right                                            ---");
      Contents.add(
          "   --- 4 : Rotate Right                                                      ---");
      Contents.add(
          "   -----------------------------------------------------------------------------");
      Contents.add("");
      Contents.add("");
      if (nrOfBits == 1) {
        Contents.add("   Result <= DataA WHEN " + ShiftModeStr + " = 1 OR");
        Contents.add("                        " + ShiftModeStr + " = 3 OR");
        Contents.add(
            "                        " + ShiftModeStr + " = 4 ELSE DataA AND NOT(ShiftAmount);");
      } else {
        int stage;
        for (stage = 0; stage < getNrofShiftBits(attrs); stage++) {
          Contents.addAll(GetStageFunctionalityVHDL(stage, nrOfBits));
        }
        Contents.add(
            "   -----------------------------------------------------------------------------");
        Contents.add(
            "   --- Here we assign the result                                             ---");
        Contents.add(
            "   -----------------------------------------------------------------------------");
        Contents.add("");
        Contents.add(
            "   Result <= s_stage_" + (getNrofShiftBits(attrs) - 1) + "_result;");
        Contents.add("");
      }
    } else {
      Contents.add(
          "   /***************************************************************************");
      Contents.add(
          "    ** ShifterMode represents when:                                          **");
      Contents.add(
          "    ** 0 : Logical Shift Left                                                **");
      Contents.add(
          "    ** 1 : Rotate Left                                                       **");
      Contents.add(
          "    ** 2 : Logical Shift Right                                               **");
      Contents.add(
          "    ** 3 : Arithmetic Shift Right                                            **");
      Contents.add(
          "    ** 4 : Rotate Right                                                      **");
      Contents.add(
          "    ***************************************************************************/");
      Contents.add("");
      Contents.add("");
      Contents.add("   wire [" + Integer.toString(2*nrOfBits) + ":0] left_rotate = {DataA, DataA} << ShiftAmount;");
      Contents.add("   wire [" + Integer.toString(2*nrOfBits) + ":0] right_rotate = {DataA, DataA} >> ShiftAmount;");
      Contents.add("");
      Contents.add("");
      Contents.add("   assign Result = (ShifterMode == 0) ? DataA << ShiftAmount :");
      Contents.add("                   (ShifterMode == 1) ? left_rotate[" + Integer.toString(2*nrOfBits-1) + ":" + Integer.toString(nrOfBits) + "] :");
      Contents.add("                   (ShifterMode == 2) ? DataA >> ShiftAmount :");
      Contents.add("                   (ShifterMode == 3) ? DataA >>> ShiftAmount :");
      Contents.add("                   (ShifterMode == 4) ? right_rotate[" + Integer.toString(nrOfBits-1) + ":0] : DataA;");
    }
    return Contents;
  }

  private int getNrofShiftBits(AttributeSet attrs) {
    int inputbits = attrs.getValue(StdAttr.WIDTH).getWidth();
    int shift = 1;
    while ((1 << shift) < inputbits) shift++;
    return shift;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> Outputs = new TreeMap<>();
    int inputbits = attrs.getValue(StdAttr.WIDTH).getWidth();
    Outputs.put("Result", inputbits);
    return Outputs;
  }

  @Override
  public SortedMap<Integer, String> GetParameterList(AttributeSet attrs) {
    SortedMap<Integer, String> Parameters = new TreeMap<>();
    Parameters.put(ShiftModeId, ShiftModeStr);
    return Parameters;
  }

  @Override
  public SortedMap<String, Integer> GetParameterMap(
      Netlist Nets, NetlistComponent ComponentInfo, FPGAReport Reporter) {
    SortedMap<String, Integer> ParameterMap = new TreeMap<>();
    Object shift = ComponentInfo.GetComponent().getAttributeSet().getValue(Shifter.ATTR_SHIFT);
    if (shift == Shifter.SHIFT_LOGICAL_LEFT) ParameterMap.put(ShiftModeStr, 0);
    else if (shift == Shifter.SHIFT_ROLL_LEFT) ParameterMap.put(ShiftModeStr, 1);
    else if (shift == Shifter.SHIFT_LOGICAL_RIGHT) ParameterMap.put(ShiftModeStr, 2);
    else if (shift == Shifter.SHIFT_ARITHMETIC_RIGHT) ParameterMap.put(ShiftModeStr, 3);
    else ParameterMap.put(ShiftModeStr, 4);
    return ParameterMap;
  }

  @Override
  public SortedMap<String, String> GetPortMap(
	      Netlist Nets, Object MapInfo, FPGAReport Reporter, String HDLType) {
    SortedMap<String, String> PortMap = new TreeMap<>();
    if (!(MapInfo instanceof NetlistComponent)) return PortMap;
    NetlistComponent ComponentInfo = (NetlistComponent) MapInfo;
    PortMap.putAll(GetNetMap("DataA", true, ComponentInfo, Shifter.IN0, Reporter, HDLType, Nets));
    PortMap.putAll(
        GetNetMap("ShiftAmount", true, ComponentInfo, Shifter.IN1, Reporter, HDLType, Nets));
    PortMap.putAll(GetNetMap("Result", true, ComponentInfo, Shifter.OUT, Reporter, HDLType, Nets));
    return PortMap;
  }

  private ArrayList<String> GetStageFunctionalityVHDL(int StageNumber, int NrOfBits) {
    ArrayList<String> Contents = new ArrayList<>();
    int nr_of_bits_to_shift = (1 << StageNumber);
    Contents.add(
        "   -----------------------------------------------------------------------------");
    Contents.add(
        "   --- Here stage "
            + StageNumber
            + " of the binary shift tree is defined                     ---");
    Contents.add(
        "   -----------------------------------------------------------------------------");
    Contents.add("");
    if (StageNumber == 0) {
      Contents.add(
          "   s_stage_0_shiftin <= DataA("
              + (NrOfBits - 1)
              + ") WHEN "
              + ShiftModeStr
              + " = 1 OR "
              + ShiftModeStr
              + " = 3 ELSE");
      Contents.add("                        DataA(0) WHEN " + ShiftModeStr + " = 4 ELSE '0';");
      Contents.add("");
      Contents.add("   s_stage_0_result  <= DataA");
      if (NrOfBits == 2) Contents.add("                           WHEN ShiftAmount = '0' ELSE");
      else Contents.add("                           WHEN ShiftAmount(0) = '0' ELSE");
      Contents.add(
          "                        DataA("
              + (NrOfBits - 2)
              + " DOWNTO 0)&s_stage_0_shiftin");
      Contents.add(
          "                           WHEN "
              + ShiftModeStr
              + " = 0 OR "
              + ShiftModeStr
              + " = 1 ELSE");
      Contents.add(
          "                        s_stage_0_shiftin&DataA("
              + (NrOfBits - 1)
              + " DOWNTO 1);");
      Contents.add("");
    } else {
      Contents.add(
          "   s_stage_"
              + StageNumber
              + "_shiftin <= s_stage_"
              + (StageNumber - 1)
              + "_result( "
              + (NrOfBits - 1)
              + " DOWNTO "
              + (NrOfBits - nr_of_bits_to_shift)
              + " ) WHEN "
              + ShiftModeStr
              + " = 1 ELSE");
      Contents.add(
          "                        (OTHERS => s_stage_"
              + (StageNumber - 1)
              + "_result("
              + (NrOfBits - 1)
              + ")) WHEN "
              + ShiftModeStr
              + " = 3 ELSE");
      Contents.add(
          "                        s_stage_"
              + (StageNumber - 1)
              + "_result( "
              + (nr_of_bits_to_shift - 1)
              + " DOWNTO 0 ) WHEN "
              + ShiftModeStr
              + " = 4 ELSE");
      Contents.add("                        (OTHERS => '0');");
      Contents.add("");
      Contents.add(
          "   s_stage_"
              + StageNumber
              + "_result  <= s_stage_"
              + (StageNumber - 1)
              + "_result");
      Contents.add("                           WHEN ShiftAmount(" + StageNumber + ") = '0' ELSE");
      Contents.add(
          "                        s_stage_"
              + (StageNumber - 1)
              + "_result( "
              + (NrOfBits - nr_of_bits_to_shift - 1)
              + " DOWNTO 0 )&s_stage_"
              + StageNumber
              + "_shiftin");
      Contents.add(
          "                           WHEN "
              + ShiftModeStr
              + " = 0 OR "
              + ShiftModeStr
              + " = 1 ELSE");
      Contents.add(
          "                        s_stage_"
              + StageNumber
              + "_shiftin&s_stage_"
              + (StageNumber - 1)
              + "_result( "
              + (NrOfBits - 1)
              + " DOWNTO "
              + nr_of_bits_to_shift
              + " );");
      Contents.add("");
    }
    return Contents;
  }

  @Override
  public String GetSubDir() {
    return "arithmetic";
  }

  @Override
  public SortedMap<String, Integer> GetWireList(AttributeSet attrs, Netlist Nets) {
    SortedMap<String, Integer> Wires = new TreeMap<>();
    int shift = getNrofShiftBits(attrs);
    int loop;
    for (loop = 0; loop < shift; loop++) {
      Wires.put("s_stage_" + loop + "_result", attrs.getValue(StdAttr.WIDTH).getWidth());
      Wires.put("s_stage_" + loop + "_shiftin", 1 << loop);
    }
    return Wires;
  }

  @Override
  public boolean HDLTargetSupported(String HDLType, AttributeSet attrs) {
    return true;
  }
}
