/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.memory;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.ConnectionEnd;
import com.cburch.logisim.fpga.designrulecheck.Net;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.netlistComponent;
import com.cburch.logisim.fpga.hdlgenerator.Hdl;
import com.cburch.logisim.fpga.hdlgenerator.InlinedHdlGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.WithSelectHdlGenerator;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.LineBuffer;

import java.util.HashMap;
import java.util.Map;

public class RomHdlGeneratorFactory extends InlinedHdlGeneratorFactory {

  private String atIndex(String netName, int index) {
    return netName + "[" + index + "]";
  }

  private String getNetHdlName(Netlist nets, Net net) {
    return (net.isBus() ? BUS_NAME : NET_NAME) + nets.getNetHdlName(net);
  }

  private void addExtraWires(LineBuffer contents, String newNetName, Netlist nets, ConnectionEnd end) {
    // guaranteed to be a bus here, not a net
    contents.add("wire [{{1}}-1:0] {{2}};", end.getNrOfBits(), newNetName);
    Map<String, String> wires = new HashMap<>();
    for (int i = 0; i < end.getNrOfBits(); i++) {
      final var conn = end.get((byte) i);
      wires.put(atIndex(newNetName, i), atIndex(getNetHdlName(nets, conn.getParentNet()), conn.getParentNetBitIndex()));
    }
    Hdl.addAllWiresSorted(contents, wires);
  }

  @Override
  public LineBuffer getInlinedCode(
      Netlist nets, Long componentId, netlistComponent componentInfo, String circuitName) {
    AttributeSet attrs = componentInfo.getComponent().getAttributeSet();
    final var addressWidth = attrs.getValue(Mem.ADDR_ATTR).getWidth();
    final var dataWidth = attrs.getValue(Mem.DATA_ATTR).getWidth();
    final var romContents = attrs.getValue(Rom.CONTENTS_ATTR);
    String sourceNetName = Hdl.getBusName(componentInfo, RamAppearance.getAddrIndex(0, attrs), nets);
    String destNetName = Hdl.getBusName(componentInfo, RamAppearance.getDataOutIndex(0, attrs), nets);

    Map<String, String> extraWires = new HashMap<>();
    final var busPrefix = BUS_NAME + "_" + componentInfo.getComponent().getAttributeSet().getValue(StdAttr.LABEL);

    final var wiresHdl = LineBuffer.getHdlBuffer();
    if (sourceNetName == null) {
      sourceNetName = busPrefix + "_in";
      final var sourceEnd = componentInfo.getEnd(RamAppearance.getAddrIndex(0, attrs));
      addExtraWires(wiresHdl, sourceNetName, nets, sourceEnd);
    }
    if (destNetName == null) {
      destNetName = busPrefix + "_out";
      final var sourceEnd = componentInfo.getEnd(RamAppearance.getDataOutIndex(0, attrs));
      addExtraWires(wiresHdl, destNetName, nets, sourceEnd);
    }
    if (wiresHdl.size() > 0) wiresHdl.empty();

    final var generator = (new WithSelectHdlGenerator(componentInfo.getComponent().getAttributeSet().getValue(StdAttr.LABEL),
            sourceNetName, addressWidth, destNetName, dataWidth)).setDefault(0L);
    for (var addr = 0L; addr < (1L << addressWidth); addr++) {
      final var romValue = romContents.get(addr);
      if (romValue != 0L) generator.add(addr, romValue);
    }
    return LineBuffer.getBuffer().add(wiresHdl).add(generator.getHdlCode());
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    if (attrs == null) return false;
    if (attrs.getValue(Mem.LINE_ATTR) == null) return false;
    return attrs.getValue(Mem.LINE_ATTR).equals(Mem.SINGLE);
  }
}
