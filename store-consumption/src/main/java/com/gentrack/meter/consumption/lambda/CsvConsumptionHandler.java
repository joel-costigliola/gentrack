package com.gentrack.meter.consumption.lambda;

import static java.lang.String.format;

import java.io.InputStream;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.event.S3EventNotification.S3EventNotificationRecord;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.gentrack.meter.consumption.ingestor.CsvConsumptionIngestor;
import com.gentrack.meter.consumption.reader.ConsumptionReader;
import com.gentrack.meter.consumption.reader.CsvConsumptionReader;
import com.gentrack.meter.consumption.writer.ConsumptionWriter;
import com.gentrack.meter.consumption.writer.DynamoDBConsumptionWriter;

public class CsvConsumptionHandler implements RequestHandler<S3Event, String> {

  private static final Logger LOGGER = LoggerFactory.getLogger(CsvConsumptionHandler.class);

  @Override
  public String handleRequest(S3Event s3event, Context context) {
    
    S3EventNotificationRecord record = s3event.getRecords().get(0);
    String bucket = record.getS3().getBucket().getName();
    // Object key may have spaces or unicode non-ASCII characters.
    String csvKey = record.getS3().getObject().getUrlDecodedKey();
    CsvConsumptionIngestor csvConsumptionIngestor = initCsvConsumptionIngestor();
    try {
      LOGGER.info(format("Reading %s/%s", bucket, csvKey));
      InputStream consumptionData = getS3Object(bucket, csvKey).getObjectContent();
      csvConsumptionIngestor.ingestConsumptionData(consumptionData);
      return "Ok";
    } catch (Exception e) {
      // use AWS Cloudwatch to get metrics of failed processed files. 
      LOGGER.error(format("Failed to process%s/%s", bucket, csvKey));
      throw e;
    }
  }

  private CsvConsumptionIngestor initCsvConsumptionIngestor() {
    ConsumptionReader consumptionReader = initConsumptionReader();
    ConsumptionWriter consumptionWriter = initConsumptionWriter();
    int batchSize = Integer.valueOf(Optional.ofNullable(System.getenv("WRITE_BATCH_SIZE")).orElse("100"));
    return new CsvConsumptionIngestor(consumptionReader, consumptionWriter, batchSize);
  }

  private ConsumptionWriter initConsumptionWriter() {
    DynamoDBMapper dynamoDBMapper = initDynamoDBMapper();
    return new DynamoDBConsumptionWriter(dynamoDBMapper);
  }

  private ConsumptionReader initConsumptionReader() {
    return new CsvConsumptionReader();
  }

  private S3Object getS3Object(String srcBucket, String srcKey) {
    AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();
    return s3Client.getObject(new GetObjectRequest(srcBucket, srcKey));
  }
  
  private DynamoDBMapper initDynamoDBMapper() {
    String awsRegion = System.getenv("AWS_REGION");
    String dynamoDBEndpoint = System.getenv("DYNAMODB_ENDPOINT");
    EndpointConfiguration endpointConfiguration = new EndpointConfiguration(dynamoDBEndpoint, awsRegion);
    AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                                                       .withEndpointConfiguration(endpointConfiguration)
                                                       .build();
    return new DynamoDBMapper(client);
  }
}