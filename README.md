# Energy Meter solution

## Problem Statement

Given a csv file containing consumption of various energy meters.
Provide a cloud native solution using AWS that consumes (via batch processing rather than real time streaming) a large number of these files into a data store and an API to sum total consumption per day.

## Expected Solution

The solution needs to be provided as an open GitHub/BitBucket/GitLab repo containing 4 elements:

* The actual solution for the problem
* The automated tests verifying the code works and uses cases are met
* The Infrastructure as Code deploying the solution into AWS
* A readme file containing: 1) what was done, 2) what wasn't done, 3) what would be done with more time

## Architecture

The solution is composed of two applications:
- Consumption Ingestion application (in ./store-consumption)
- Consumption reporting API (in ./consumption-api)


## Consumption Ingestion application

This is based on https://github.com/awsdocs/aws-lambda-developer-guide/tree/main/sample-apps/s3-java modified to write to DynamoDB.

### Architecture

S3 -> Lambda -> DynamoDB

A lambda is triggered for any files put in the specified S3 bucket, the lambda reads the file, compute the sum of values for each day and save the result in DynamoDB.

Assumption: files can be processed within 15 min, if that's not the case then an alternative architecture must be used (see alternatives section).

Provided the above assumption is true, a lambda is a good fit for this application because:
- the processing is stateless (each file can be processed independently of the other ones)
- no cost waste of AWS resources, we pay only for the invoked lambdas


#### Lambda

Read the csv as an input stream (not loading the whole file in memory) and delegates to CsvConsumptionIngestor the processing of the data.
CsvConsumptionIngestor use ConsumptionReader to map the data to a stream of Consumption objects which is passed to ConsumptionWriter.
ConsumptionWriter batches the stream and write each batch until stream is exhausted.

CsvConsumptionIngestor is not tied to the lambda and could be reused in a different context (like an application running on EC2).

ConsumptionReader could check the data is in within reasonable range, for example it should not read a negative value or a value so big it can't happen, it could raise an exception to discard the file processing (ideally an alert would be raised too)

I loosely followed the Hexagonal architecture pattern, a bit overkill for this simple example but that's a pattern I find quite clean to separate the domain from the tech plumbing.
https://blog.octo.com/en/hexagonal-architecture-three-principles-and-an-implementation-example/

#### DynamoDB

Schema is: Meter,Date,value
Ex:
EE00011,2019-01-01,123
EE00011,2019-01-02,456


### Assumptions

CSV structure is stable and won't evolve much. 
Date in csv always follow the YYYY-MM-DD pattern and the timezone is consistent that is other csv are in the same TZ (as a note, it would be interesting to see how consecutive files look like when DST applies).
Consumption values can be floating point numbers. 
Meter and date constitutes a unique identifier for a row.

### Security

Since the data is not sensitive (no Private Personal Information, credit card or health data), it has been decided not to encrypt the data in S3.

### Alternative solutions

#### Server application running on EC2

The application repeats these steps in an infinite loop
- reads the s3 bucket 
- update DynamoDB
- move the file to "processed" bucket to avoid processing the same files over and over

This works fine with a single instance of the application but is tricky when multiple instances are running as they would compete for the files.
This can be solved by having a store where the application sets a logical lock on the file they want to process.

#### Replacing DynamoDB with RDS

Pros:

The main advantage would to get on the API side to leverage "group by" clause to compute the average.

```sql
SELECT date, AVG(value) AS average_consumption
FROM consumption 
GROUP BY date
WHERE date = '2019-01-01';
```

Cons:

Not sure that RDS can scale well in term of writing.

#### EMR Spark application

Spark can read directly csv files stored in S3 without size limitations.
To write the consumption data to DynamoDB, use the EMR DynamoDB connector https://github.com/awslabs/emr-dynamodb-connector

#### Timestream

Seems a good fit for time series https://docs.aws.amazon.com/timestream/index.html but I did not have the time to look into it in details.
 
#### CSV to dynamo 

Fast but no control over the business logic of the meter ingestion (what if we want to exclude some files if they have obvious bad values?)

https://aws.amazon.com/blogs/database/implementing-bulk-csv-ingestion-to-amazon-dynamodb/ can't be applied directly as the first column is not uuid.
https://github.com/aws-samples/csv-to-dynamodb 

## Consumption reporting API

API Gateway -> Lambda -> DynamoDB

This is based on https://aws.amazon.com/blogs/opensource/java-apis-aws-lambda/ 

### API Schema

To get the average for a given date, simply perfrom a GET on URL: `<server>/consumption/average/{date}`

## To be done with more time

DynamoDB cloud formation definition
Fine grained access policy to the S3 bucket with a proper IAM role (priniciple of least privileges)
Code doc (javadoc)
Error handling (metrics)
Put each application in its own git repository.
Setup CI to build and test the applications with code quality reports.
Setup Deployment pipelines to deploy the applications in different environment, this would require parameterizing the cloud formation stack for the solution to inject
- the environment name
- the input s3 bucket
- the dynamo db table

### End to end / smoke tests

I did not have the time and without an AWS account it does not make much sense.
TODO: explain the approach 

### Documentation

Have a proper documentation space for the application that includes:
* Application purpose (why have built it?)
* Architecture - High Level Design 
* Support - How to monitor the application, where to get insights of the application behavior (logs, dashboard), BCP Business Continuation Process / recovery scenarios when the application fails


### AWS Cost estimates 

Depending on the volumetry of csv consumption files and the expected API usage.

### API schema 

Define the API schema in a swagger/open api format (or whatever standard is in use at Gentrack)

### Alerting and monitoring

This includes
- metrics to see the use of the ingestion and consumption API and if it meets the agreed SLAs.
- dashboard for the support roster to monitor application 
- alerts notifiying the support roster when the application behaves badly (ex: ingestion not keeping up with the amount of input files, consumption API failing to respond)

### Security

Enforce credentials (API keys per client applications) 
Make sure throtlling is in place to avoid Denial Of Service attacks on the API.

### Performance tests

This needs to be done to make sure the application can cope with the expected amount of input data and that the consumption API behaves within the expected SLAs.
This might be optional if we know the input data is relatively small and the consumption API is not a critical application.