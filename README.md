# Current kafkaAuditor Architecture:

### Desciption

* Whenever application pushes/consumes the messages to/from kafka topics, it calls audit function of kafka Auditor to audit the message. 
* To use the auditing facilities, producer/consumer should pass ("unixepoch","hostname","topic","tier") to audit function of kafka Auditor class.
* Auditor library is backed by 5 threads to calculate audit data and 1 scheduled thread to timely reporting the audit data to kafka. Thread pool is used to unblock the producer as soon as possible  and  to create the auditor threads in more controlled way else more threads will be forked that will waste the time in cpu context switching rather than spending the time on actual work.
* ("unixepoch","hostname","topic","tier") , these four parameters will decide if counter of message consumed/produced should be incremented or initialized.
* These parameters are important to make the auditor more generalized ,This design decision will help the library to transform in "Auditing as a service" , which can  be scaled independently and consumed by anyone in organization. But if we have high velocity of data than network latency , bandwidth may hit the performance and time to invoke the service to audit the data might be little more. 

### Auditor Library In Short contains 4  classes :

* 1. Audit Message :  It define the fields that we need to send in Audit message.
* 2. Audit Reporter : It maintain the Audit producer instance and produce function to push the audit data to kafka.
* 3. Kafka Auditor :  It is basically most important class where most of the stuff happens. This class provide audit function to accept the audit request , process the audit data and with the use of scheduled thread make a call to audit reporter to publish the audit data timely to kafka topic
* 4. MapKey :  This class defines the key for Audit Map.  basically "unixepoch","hostname","topic" & "tier" is considered as a Map key to increment the message counter. This class override the default hashcode and equals function of object class.  

### Simple Producer and Simple Consumer

* 1. Simple Producer :  This class produce records , basically this class generates and produces the data to kafka topic in sync way and at the same time request to audit the produce.
* 2. Simple Consumer : This class consume the data from the topic configured and request to audit the consume.

#### Important Auditor parameters

* audit.auditdata.flush.frequency=30000 , this parameter tell the kafkaAuditor how frequently audit data has to be published to kafka. parameter values shows time in milliseconds
* audit.timebucket.size=1 tell about "how many minutes of data" should be aggregated and sent to kafka.(N floored minute)

# Alternate Design for KafkaAuditor: 

## KafkaAuditing as service
* It will be much better, if we can design kafka auditor as a service where to audit any produce or consume  is just need to invoke a service with specific inputs. 
("tier","topic","hostname","unix time epoch") for auditing the record. Such service can be centrally hosted and dynamically scaled as per requirement & load.
# KafkaAuditor : Approach 2:

# where to audit the data ?

There are 2 places where we can send the audit data 
1. To kafka
2. To Database

According to me kafka auditor should directly insert the audit data in some highly available cassandra like database rathen than relying on same infrastructure(kafka) which we want to audit ,  

# Audit Checker

## Approach 1:

* Step1: After pushing all the logs to Kafka Audit Topic , a consumer service can read these audit data and can put in some database like Apache Cassandra, oracle or sql. where a view can be created to read the aggregated data from database.
* Step 2: That view can be invoked by rest service and data can be sent to front end applications in json format. 

* Query for database view can look like this.


### SQl Query
select  timestampBucket, dataTopic, server , 
sum(case when tier='consume' then recordCount else 0 end) as message_consumed,
sum(case when tier='produce' then recordCount else 0 end) as message_produced from 
audit_mysql_table  group by timestampBucket, dataTopic, server;

### Oracle Query
select  timestampBucket, dataTopic, server , 
cast (sum(decode(tier,'consume',recordCount)) as LONG) as message_consumed,
cast (sum(decode(tier,'produce',recordCount)) as LONG) as message_produced 
from audit_Oracle_table  
group by timestampBucket, dataTopic, server;

### ksql
Even before pushing the data to some database , data can be aggregated in kafka topic using ksql (streams & table)

#### Stream
create stream audit_stream (dataTopic varchar, server varchar , tier varchar, timestampBucket BIGINT  , recordCount BIGINT ) with (kafka_topic='Audit_Topic',value_format='json');

#### table
create table audit_table as select timestampBucket, tier, dataTopic, server, sum(recordCount) from audit_stream  group by timestampBucket, tier, dataTopic, server;

#### More sophisticated ksql query (not able to create)
create table audit_table as
select  timestampBucket, dataTopic, server , 
sum(case when tier='consume' then recordCount else 0 end) as message_consumed,
sum(case when tier='produce' then recordCount else 0 end) as message_produced from 
audit_stream  group by timestampBucket, dataTopic, server;

* Error: In Above ksql query (May be a bug in kafka) Need to check further
* No SUM aggregate function with null  argument type exists!

### Response to front end can be like this , Multiple end point can be created to retrieve the detailed view of auditing the data.

* Sample Response
{ "timestampBucket":45676765,
"dataTopic":"topicname",
"server":"xyz",
"message_consumed":200,
"message_produced":200
}




### Enhancement

* KafkaAuditor can be designed as a "KafkaAuditor as a service"
* If database approach has to be chosen to push the audit data , then kafka audit topics can be kept as fall back. It means if database went down we don't lose auditing , this will give us more flexibility. 


#Deployment instructions for Current solution

* Please create "test" and "audit" topic or make auto create topic property to true.
* bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic audit

* tar -xvzf KafkaAuditor.tar.gz
* cd KafkaAuditor/consumer
* Open config properties to adapt the below parameters.
* data.consumer.bootstrap.servers=localhost:9092
* audit.producer.bootstrap.servers=localhost:9092
* mvn clean test -DfileName=./config.properties
* mvn clean install -DfileName=./config.properties -DskipTests
* java -jar target/consumer-0.0.1-SNAPSHOT.jar ./config.properties


* cd ../producer
* Open config properties to adapt the below parameters.
* data.producer.bootstrap.servers=localhost:9092
* audit.producer.bootstrap.servers=localhost:9092
* mvn clean test -DfileName=./config.properties
* mvn clean install -DfileName=./config.properties -DskipTests
* java -jar target/producer-0.0.1-SNAPSHOT.jar ./config.properties


## Example : Producer produced 1000 record in synchronous fashion and consumer consumed 1000 records and audit job were running with 30 seconds flush frequency
Aggregation should be done based on timestampBucket, dataTopic, server then we can have correct results.

### Sample Output (This should be aggregated)
{"dataTopic":"test","server":"siftworkstation","tier":"consume","timestampBucket":25608325,"recordCount":217}
{"dataTopic":"test","server":"siftworkstation","tier":"produce","timestampBucket":25608325,"recordCount":287}
{"dataTopic":"test","server":"siftworkstation","tier":"consume","timestampBucket":25608325,"recordCount":288}
{"dataTopic":"test","server":"siftworkstation","tier":"consume","timestampBucket":25608326,"recordCount":4}
{"dataTopic":"test","server":"siftworkstation","tier":"produce","timestampBucket":25608326,"recordCount":75}
{"dataTopic":"test","server":"siftworkstation","tier":"produce","timestampBucket":25608325,"recordCount":218}
{"dataTopic":"test","server":"siftworkstation","tier":"consume","timestampBucket":25608326,"recordCount":293}
{"dataTopic":"test","server":"siftworkstation","tier":"produce","timestampBucket":25608326,"recordCount":294}
{"dataTopic":"test","server":"siftworkstation","tier":"produce","timestampBucket":25608326,"recordCount":126}
{"dataTopic":"test","server":"siftworkstation","tier":"consume","timestampBucket":25608326,"recordCount":198}




## Author

* Manish Jain (manishmbm2010@gmail.com)
