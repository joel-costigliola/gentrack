package com.gentrack.meter.consumption.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

import com.amazonaws.serverless.proxy.jersey.JerseyLambdaContainerHandler;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

public class StreamLambdaHandler implements RequestStreamHandler {

  private static final ResourceConfig JERSEY_APP = new ResourceConfig().packages("com.gentrack.meter.consumption.api.resource")
                                                                       .register(JacksonFeature.class);

  private static final JerseyLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> HANDLER = JerseyLambdaContainerHandler.getAwsProxyHandler(JERSEY_APP);

  @Override
  public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
    HANDLER.proxyStream(inputStream, outputStream, context);
    // just in case it wasn't closed by the mapper
    outputStream.close();
  }
}