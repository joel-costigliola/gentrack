package com.gentrack.meter.consumption.api.resource;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.internal.IteratorSupport;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;

@Path("/consumption")
public class ConsumptionResource {

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/average/{date}")
  public Response getAverageConsumptionOn(@PathParam("date") String date) {
    // validate date parameter is a proper date.
    try {
      LocalDate.parse(date);
    } catch (DateTimeParseException e) {
      return invalidDateParam(date);
    }
    Table consumptionTable = getDynamoDB().getTable("Consumption");
    QuerySpec comsumptionAtDate = consumptionAtDateQuery(date);
    IteratorSupport<Item, QueryOutcome> results = consumptionTable.query(comsumptionAtDate).iterator();
    double average = computeAverage(results);
    return averageResponse(average);
  }

  private static QuerySpec consumptionAtDateQuery(String date) {
    return new QuerySpec().withKeyConditionExpression("date = :v_date")
                          .withValueMap(new ValueMap().withString(":v_date", date));
  }

  private static double computeAverage(IteratorSupport<Item, QueryOutcome> results) {
    long count = 0;
    double sum = 0.0;
    while (results.hasNext()) {
      sum += results.next().getDouble("value");
      count++;
    }
    return sum / count;
  }

  private static Response invalidDateParam(String date) {
    return Response.status(BAD_REQUEST)
                   .entity("Invalid date, accepted format is YYYY-MM-DD, ex 2019-01-01 but given date was " + date).build();
  }

  private static Response averageResponse(double average) {
    Map<String, String> entity = new HashMap<>();
    entity.put("average", String.valueOf(average));
    return Response.status(200).entity(entity).build();
  }

  // ideally we would inject this as field in this class with a DI framework
  private DynamoDB getDynamoDB() {
    String awsRegion = System.getenv("AWS_REGION");
    String dynamoDBEndpoint = System.getenv("DYNAMODB_ENDPOINT");
    EndpointConfiguration endpointConfiguration = new EndpointConfiguration(dynamoDBEndpoint, awsRegion);
    AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                                                       .withEndpointConfiguration(endpointConfiguration)
                                                       .build();
    return new DynamoDB(client);
  }

}