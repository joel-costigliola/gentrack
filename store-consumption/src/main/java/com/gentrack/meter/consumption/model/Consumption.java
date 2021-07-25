package com.gentrack.meter.consumption.model;

import java.time.LocalDate;
import java.util.Objects;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

/**
 * Use a composite primary key as meter  + local date identifies uniquely a meter daily consumption.
 * <p>
 * Use the localDate as partition key because we will query all values for a given date thus having all the data in the same partition will be advantageous. 
 */
@DynamoDBTable(tableName = "Consumption")
public class Consumption {

  private String meter;
  private LocalDate localDate;
  private double value;

  @DynamoDBHashKey(attributeName = "date")
  public LocalDate getLocalDate() {
    return localDate;
  }

  @DynamoDBRangeKey(attributeName = "meter")
  public String getMeter() {
    return meter;
  }

  public double getValue() {
    return value;
  }

  // setter needed for dynamo

  public void setMeter(String meter) {
    this.meter = meter;
  }

  public void setLocalDate(LocalDate localDate) {
    this.localDate = localDate;
  }

  public void setValue(double value) {
    this.value = value;
  }

  public Consumption(String meter, LocalDate localDate, double value) {
    this.meter = meter;
    this.localDate = localDate;
    this.value = value;
  }

  public Consumption() {}

  @Override
  public String toString() {
    return String.format("Consumption [meter=%s, localDate=%s, value=%s]", meter, localDate, value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(localDate, meter, value);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    Consumption other = (Consumption) obj;
    return Objects.equals(localDate, other.localDate) &&
           Objects.equals(meter, other.meter) &&
           Double.doubleToLongBits(value) == Double.doubleToLongBits(other.value);
  }

}
