package com.gentrack.meter.consumption.ingestor;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentrack.meter.consumption.model.Consumption;
import com.gentrack.meter.consumption.reader.ConsumptionReader;
import com.gentrack.meter.consumption.writer.ConsumptionWriter;

// no direct dependency on any AWS services 
public class CsvConsumptionIngestor {

  private static final Logger LOGGER = LoggerFactory.getLogger(CsvConsumptionIngestor.class);
  private ConsumptionReader consumptionReader;
  private ConsumptionWriter consumptionWriter;
  private int batchSize;

  public CsvConsumptionIngestor(ConsumptionReader consumptionReader, ConsumptionWriter consumptionWriter, int batchSize) {
    this.consumptionReader = consumptionReader;
    this.consumptionWriter = consumptionWriter;
    this.batchSize = batchSize;
  }

  public void ingestConsumptionData(InputStream consumptionData) {
      LOGGER.info("Start ingestion");
      Stream<Consumption> consumptionStream = consumptionReader.readConsumption(consumptionData);
      writeConsumptionStream(consumptionWriter, consumptionStream);
      LOGGER.info("End ingestion");
  }

  private void writeConsumptionStream(ConsumptionWriter consumptionWriter, Stream<Consumption> consumptionStream) {
    List<Consumption> consumptionBatch = new ArrayList<>(batchSize);
    consumptionStream.forEach(consumption -> {
      consumptionBatch.add(consumption);
      if (consumptionBatch.size() == batchSize) {
        consumptionWriter.writeConsumptionBatch(consumptionBatch);
        consumptionBatch.clear();
      }
    });
    if (!consumptionBatch.isEmpty()) {
      consumptionWriter.writeConsumptionBatch(consumptionBatch);
    }
  }
}