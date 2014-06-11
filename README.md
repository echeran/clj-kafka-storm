# An Implementation of Kafka + Storm

## Components

Within the Lambda Architecture repository are the following directories:

* twitter-example - a Storm topology that pulls from a Kafka 0.7 server using KafkaSpout.  The KafkaSpout pulls tweets from the Kafka server that originally come from the Twitter dev stream via the twitter-kafka-producer.
* twitter-kafka-producer – a Kafka producer a.k.a. publisher that links
the Twitter dev testing stream to Kafka

## Notes

Even though Kafka v0.7 is not the most recent version of Kafka, it was
the only one that I was able to get to work at the time with Storm
with relatively little effort.  Others also have trouble with getting KafkaSpout from the current
version of storm-kafka (v 0.9x) to work with Kafka v0.8 (it only works
with Kafka v0.7) -- see this email group thread
https://groups.google.com/forum/#!topic/storm-user/OXQ0a9ppcYM 

Those with extra time should consider updating the latest version
KafkaSpout to work with the latest version of Kafka, which is said to
be much better than Kafka v0.7.

In order to target Storm version 0.8.2, use
storm-kafka version 0.8.0-wip4 -- see this email group thread https://groups.google.com/forum/#!topic/storm-user/DEqXfOSv_MA

## Installation

### Kafka v0.7

[Download a v 0.7.x Kafka release](http://kafka.apache.org/downloads.html)
and follow the
[Kafka v0.7 documentation's Quickstart instructions](http://kafka.apache.org/07/quickstart.html) for downloading,installing,
and running.

Documentation for Kafka v0.7
[can be found here](http://kafka.apache.org/07/documentation.html).

In order to install a multi-broker Kafka cluster, you can adapt the
[instructions from Quickstart of the latest Kafka documentation](http://kafka.apache.org/documentation.html#quickstart):

#### Configure the built-in Kafka Zookeeper instance

* ensure in `config/zookeeper.properties`
  * `clientPort=2181`

##### Configure the Kafka brokers

* `cp config/server.properties config/server-0.properties`
  * edit `config/server-0.properties`
    * `brokerid=0`
    * `port=9092`
    * `log.dir=/tmp/kfk0.7-logs-0`
* `cp config/server.properties config/server-1.properties`
  * edit `config/server-1.properties`
    * `brokerid=1`
    * `port=9093`
    * `log.dir=/tmp/kfk0.7-logs-1`
* `cp config/server.properties config/server-2.properties`
  * edit `config/server-2.properties`
    * `brokerid=2`
    * `port=9094`
    * `log.dir=/tmp/kfk0.7-logs-2`

### Configure the Twitter Dev Stream

* Configure the access credentials
  * Create a [https://dev.twitter.com/](Twitter Developer) account
  * Create a [https://spring.io/guides/gs/register-twitter-app/](Twitter
      Application)
  * In `./twitter-kafka-producer/src/twitter_kafka_producer/core.clj`:
    * Enter the Twitter API {key, secret} as the OAuth Consumer {key, secret}
    * Enter the Twitter access {token , token secret} as the OAuth access
        {token , token secret}
* Configure the terms used to filter Twitter stream
  * In `./twitter-kafka-producer/src/twitter_kafka_producer/core.clj`:
    * Edit the `track-terms` vector to contain the strings used to
      filter the Tweet stream

## Usage

* Run the Kafka Zookeeper instance
  * `bin/zookeeper-server-start.sh config/zookeeper.properties`
* Start the Kafka brokers, each with its own JMX port number
  * `JMX_PORT=2002 bin/kafka-server-start.sh config/server-0.properties`
  * `JMX_PORT=2003 bin/kafka-server-start.sh config/server-1.properties`
  * `JMX_PORT=2004 bin/kafka-server-start.sh config/server-2.properties`
* In Kafka v0.7, topics are created automatically when you write to
  them.  In v0.8, you must create the topic manually.  Nothing to do
  here for Kafka topic creation in this particular example implementation.
  * Debugging of topics can be done through running the console
    producer and consumer as:
    * console producer - `bin/kafka-console-producer.sh --zookeeper localhost:2181 --topic
    twitter-to-storm`
    * console consumer -`bin/kafka-console-consumer.sh --zookeeper
    localhost:2181 --topic twitter-to-storm --from-beginning`
* Run the Kafka producer
  * `cd ./twitter-kafka-producer`
  * `lein do clean, run` (this is `lein clean` and `lein run` combined
    as one call to `lein` - note the spacing around `do` and commas)
* Run the Storm topology (that is, a Storm instance with this topology) 
  * `cd ./twitter-example`
  * `lein do clean, run`
  
## Versions

For me, so far, the hardest part by far of getting Kafka, Storm, and KafkaSpout to work together has been a matter of getting the correct versions to match up.

The code currently uses the following versions for the components:

* Kafka v 0.7.2
* Kafka client API via clj-kafka v 0.0.7-0.7 for a Kafka producer
* Storm v 0.8.2
* storm-kafka v 0.8.0-wip4 for a KafkaSpout (that version will work with Storm
v 0.8.2, and it works with Kafka only for Kafka v 0.7.2)
* twitter4j-core and twitter4j-stream v 3.0.6 (the versions for both
  should be the same)

The version numbers are as used within Leiningen (therefore, you should be able to use them via Maven (either in Maven Central or Clojars))

## License

The license for all of the code in the repository (including twitter-example and
twitter-kafka-producer) is EPL, the same as Clojure.
