package com.gentrack.meter.consumption.api;

import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.BDDAssertions.then;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Response;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.amazonaws.serverless.proxy.internal.LambdaContainerHandler;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.internal.testutils.MockLambdaContext;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.services.lambda.runtime.Context;

// when application support injecting services, we can inject a mock dynamo db and test the behavior of the lambda  
@Ignore
public class StreamLambdaHandlerTest {

  private static StreamLambdaHandler lambdaHandler;
  private static Context lambdaContext;

  @BeforeClass
  public static void setUp() {
    lambdaHandler = new StreamLambdaHandler();
    lambdaContext = new MockLambdaContext();
  }

  @Test
  public void should_return() throws IOException {
    // GIVEN
    AwsProxyRequestBuilder awsProxyRequestBuilder = new AwsProxyRequestBuilder("/consumption/average/2019-01-01", HttpMethod.GET);
    InputStream requestStream = awsProxyRequestBuilder.header(ACCEPT, APPLICATION_JSON).buildStream();
    ByteArrayOutputStream responseStream = new ByteArrayOutputStream();
    // WHEN
    handle(requestStream, responseStream);
    // THEN
    AwsProxyResponse response = readResponse(responseStream);
    then(response).isNotNull();
    then(response.getStatusCode()).isEqualTo(Response.Status.OK.getStatusCode());
    then(response.isBase64Encoded()).isFalse();
    then(response.getBody()).contains("date", "Hello, World!");
    then(response.getHeaders()).containsKey(CONTENT_TYPE);
    then(response.getHeaders().get(CONTENT_TYPE)).startsWith(APPLICATION_JSON);
  }

  @Test
  public void invalidResource_streamRequest_responds404() throws IOException {
    // GIVEN
    AwsProxyRequestBuilder awsProxyRequestBuilder = new AwsProxyRequestBuilder("/consumption/foo", HttpMethod.GET);
    InputStream requestStream = awsProxyRequestBuilder.header(ACCEPT, APPLICATION_JSON).buildStream();
    ByteArrayOutputStream responseStream = new ByteArrayOutputStream();
    // WHEN
    handle(requestStream, responseStream);
    // THEN
    AwsProxyResponse response = readResponse(responseStream);
    then(response).isNotNull();
    then(response.getStatusCode()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
  }

  private void handle(InputStream is, ByteArrayOutputStream os) throws IOException {
    lambdaHandler.handleRequest(is, os, lambdaContext);
  }

  private AwsProxyResponse readResponse(ByteArrayOutputStream responseStream) throws IOException {
    return LambdaContainerHandler.getObjectMapper().readValue(responseStream.toByteArray(), AwsProxyResponse.class);
  }
}
