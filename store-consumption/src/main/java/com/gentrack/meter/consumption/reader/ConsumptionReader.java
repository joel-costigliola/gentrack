package com.gentrack.meter.consumption.reader;

import java.io.InputStream;
import java.util.stream.Stream;

import com.gentrack.meter.consumption.model.Consumption;

public interface ConsumptionReader {
  
  Stream<Consumption> readConsumption(InputStream consumptionData);
  
}