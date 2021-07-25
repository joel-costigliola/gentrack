package com.gentrack.meter.consumption.writer;

import java.util.List;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.gentrack.meter.consumption.model.Consumption;

public class DynamoDBConsumptionWriter implements ConsumptionWriter {

  private DynamoDBMapper dynamoDB;

  public DynamoDBConsumptionWriter(DynamoDBMapper dynamoDBMapper) {
    dynamoDB = dynamoDBMapper;
  }

  @Override
  public void writeConsumptionBatch(List<Consumption> consumptionBatch) {
    consumptionBatch.forEach(dynamoDB::batchSave);
  }

}
