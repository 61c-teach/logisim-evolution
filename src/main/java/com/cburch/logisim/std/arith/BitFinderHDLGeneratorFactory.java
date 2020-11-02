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

import com.cburch.logisim.analyze.model.Var;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.fpga.gui.FPGAReport;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.instance.StdAttr;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

public class BitFinderHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  private static final String BitFinderModeStr = "BitFinderMode";
  private static final int BitFinderModeId = -1;

  @Override
  public String getComponentStringIdentifier() {
    return "BitFinder";
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> Inputs = new TreeMap<String, Integer>();
    Inputs.put("DataA", attrs.getValue(StdAttr.WIDTH).getWidth());
    return Inputs;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(
      Netlist TheNetlist, AttributeSet attrs, FPGAReport Reporter, String HDLType) {
    ArrayList<String> Contents = new ArrayList<String>();
    int nrOfBits = attrs.getValue(StdAttr.WIDTH).getWidth();
    int outputbits = computeOutputBits(nrOfBits-1);
    Object finderMode = attrs.getValue(BitFinder.TYPE);
    if (HDLType.equals(VHDL)) {
      Contents.add("");
    } else {
      Contents.add("");
      Contents.add("");
      Contents.add("   reg ["+ Integer.toString(outputbits-1) + ":0] temp;");
      Contents.add("");
      Contents.add("   always @(*) begin");
      Contents.add("      temp = 0;");
      Contents.add(String.format("      if (%s == 0) begin", BitFinderModeStr));
      for (int i = nrOfBits-1; i >= 0; i--) {
        Contents.add(String.format("         temp = DataA[%d] == %d ? %d : temp;", i, 1, i));
      }
      Contents.add("      end");
      Contents.add(String.format("      else if (%s == 1) begin", BitFinderModeStr));
      for (int i = 0; i < nrOfBits; i++) {
        Contents.add(String.format("         temp = DataA[%d] == %d ? %d : temp;", i, 1, i));
      }
      Contents.add("      end");
      Contents.add(String.format("      else if (%s == 2) begin", BitFinderModeStr));
      for (int i = nrOfBits-1; i >= 0; i--) {
        Contents.add(String.format("         temp = DataA[%d] == %d ? %d : temp;", i, 0, i));
      }
      Contents.add("      end");
      Contents.add("      else begin");
      for (int i = 0; i < nrOfBits; i++) {
        Contents.add(String.format("         temp = DataA[%d] == %d ? %d : temp;", i, 0, i));
      }
      Contents.add("      end");
      Contents.add("   end");
      Contents.add(String.format("   assign Present = (%s < 2) ? |DataA : |(~DataA);", BitFinderModeStr));
      Contents.add("   assign Result = temp;");
    }
    return Contents;
  }

  private int computeOutputBits(int maxBits) {
    int outWidth = 1;
    while ((1 << outWidth) <= maxBits) outWidth++;
    return outWidth;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> Outputs = new TreeMap<String, Integer>();
    int inputbits = attrs.getValue(StdAttr.WIDTH).getWidth();
    Outputs.put("Result", computeOutputBits(inputbits-1));
    Outputs.put("Present", 1);
    return Outputs;
  }

  @Override
  public SortedMap<Integer, String> GetParameterList(AttributeSet attrs) {
    SortedMap<Integer, String> Parameters = new TreeMap<Integer, String>();
    Parameters.put(BitFinderModeId, BitFinderModeStr);
    return Parameters;
  }

  @Override
  public SortedMap<String, Integer> GetParameterMap(
      Netlist Nets, NetlistComponent ComponentInfo, FPGAReport Reporter) {
    SortedMap<String, Integer> ParameterMap = new TreeMap<String, Integer>();
    Object finderMode = ComponentInfo.GetComponent().getAttributeSet().getValue(BitFinder.TYPE);
    if (finderMode == BitFinder.LOW_ONE) ParameterMap.put(BitFinderModeStr, 0);
    else if (finderMode == BitFinder.HIGH_ONE) ParameterMap.put(BitFinderModeStr, 1);
    else if (finderMode == BitFinder.LOW_ZERO) ParameterMap.put(BitFinderModeStr, 2);
    else ParameterMap.put(BitFinderModeStr, 4);
    return ParameterMap;
  }

  @Override
  public SortedMap<String, String> GetPortMap(
	      Netlist Nets, Object MapInfo, FPGAReport Reporter, String HDLType) {
    SortedMap<String, String> PortMap = new TreeMap<String, String>();
    if (!(MapInfo instanceof NetlistComponent)) return PortMap;
    NetlistComponent ComponentInfo = (NetlistComponent) MapInfo;
    PortMap.putAll(GetNetMap("Present", true, ComponentInfo, 0, Reporter, HDLType, Nets));
    PortMap.putAll(GetNetMap("Result", true, ComponentInfo, 1, Reporter, HDLType, Nets));
    PortMap.putAll(GetNetMap("DataA", true, ComponentInfo, 2, Reporter, HDLType, Nets));
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
    return true;
  }
}
