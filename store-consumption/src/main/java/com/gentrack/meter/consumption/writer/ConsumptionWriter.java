package com.gentrack.meter.consumption.writer;

import java.util.List;

import com.gentrack.meter.consumption.model.Consumption;

public interface ConsumptionWriter {
  
  void writeConsumptionBatch(List<Consumption> consumptionBatch);
  
}