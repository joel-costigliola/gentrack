package com.gentrack.meter.consumption.reader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.ArrayUtils;

import com.gentrack.meter.consumption.model.Consumption;

public class CsvConsumptionReader implements ConsumptionReader {

  private static final String[] NON_TIME_HEADERS = { "Meter", "Date" };

  private static final String[] TIMES_HEADERS = { "0:00", "0:30", "1:00", "1:30", "2:00", "2:30", "3:00", "3:30",
      "4:00", "4:30", "5:00", "5:30", "6:00", "6:30", "7:00", "7:30", "8:00", "8:30", "9:00", "9:30", "10:00", "10:30", "11:00",
      "11:30", "12:00", "12:30", "13:00", "13:30", "14:00", "14:30", "15:00", "15:30", "16:00", "16:30", "17:00", "17:30",
      "18:00", "18:30", "19:00", "19:30", "20:00", "20:30", "21:00", "21:30", "22:00", "22:30", "23:00", "23:30" };

  private static final String[] HEADERS = ArrayUtils.addAll(NON_TIME_HEADERS, TIMES_HEADERS);

  @Override
  public Stream<Consumption> readConsumption(InputStream consumptionData) {
    Iterable<CSVRecord> records;
    try {
      records = CSVFormat.DEFAULT.withHeader(HEADERS)
                                 .withFirstRecordAsHeader()
                                 .parse(new InputStreamReader(consumptionData));
    } catch (IOException e) {
      // let the caller deal with the error.
      throw new RuntimeException(e);
    }

    return StreamSupport.stream(records.spliterator(), false).map(CsvConsumptionReader::toConsumption);
  }

  private static Consumption toConsumption(CSVRecord record) {
    double values = sumValues(record);
    return new Consumption(record.get("Meter"), LocalDate.parse(record.get("Date")), values);
  }

  private static double sumValues(CSVRecord record) {
    return Stream.of(TIMES_HEADERS)
                 .map(record::get)
                 .mapToDouble(Double::valueOf)
                 .sum();
  }

}