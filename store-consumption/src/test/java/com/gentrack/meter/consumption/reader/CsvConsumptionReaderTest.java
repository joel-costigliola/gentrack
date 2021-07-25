package com.gentrack.meter.consumption.reader;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.gentrack.meter.consumption.model.Consumption;

public class CsvConsumptionReaderTest {

  private CsvConsumptionReader csvConsumptionReader = new CsvConsumptionReader();

  @Test
  void should_read_consumption_and_sum_all_values_per_day() throws IOException {
    // GIVEN
    InputStream data = new FileInputStream(new File("src/test/resources/small-consumption.csv"));
    // WHEN
    Stream<Consumption> consumptions = csvConsumptionReader.readConsumption(data);
    // THEN
    assertThat(consumptions).containsExactly(new Consumption("EE00011", LocalDate.of(2019, 1, 1), 24.0),
                                             new Consumption("EE00011", LocalDate.of(2019, 1, 2), 47.0));
  }

}
