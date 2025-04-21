# Solution - Nikolay Bankin

## How to run

### Native
#### Dependencies

- Redis Server
- Python 3.9
- Java 21

#### Steps

1. Start your Redis server
2. Install redis package for python
    ```
    python -m pip install redis
   ```
3. In a separate terminal window run the producer script
    ```
   python producer/producer.py
   ```
4. Run the consumer with
    ```
    mvn clean package && java -jar main/target/main-1.0-SNAPSHOT-all.jar 
   ```

### Docker
#### Dependencies

- Docker
- Docker compose

#### Steps

1. Run the whole solution with default configuration with
    ```
    docker-compose up
   ```

### Configuration

There are environment variables controlling each component. You can edit the
docker-compose file or provide environment variables with the following names.
There is a `.env` file which you can customise. All envs are present with 
their defaults in `default.env` for easier setup

- `REDIS_HOST` - host of redis server
  - default: `localhost`
- `REDIS_PORT` - port of redis server
  - default: `6379`

- `SOLUTION_PUB_SUB_KEY` - the pub/sub key where messages arrive
  - default: `messages:published'
- `SOLUTION_PROCESSED_MESSAGES_STREAM_KEY` - the stream where successfully
  processed messages are posted
    - default: `messages:processed`
- `SOLUTION_CONSUMER_COUNT` - how many consumers to be started
  - default: `2`
- `SOLUTION_ACTIVE_CONSUMERS_LIST_KEY` - list where all active consumer ids
are kept
  - default: `consumer:ids`
- `SOLUTION_MONITORING_TIMEOUT_IN_SECONDS` - how often should the monitoring
report about amount of processed messages per second
  - default: `3`

- `BANKIN_MESSAGE_BACKLOG_STREAM_KEY` - where messages from the pub/sub are
stored for processing
  - default: `messages:backlog`
- `BANKIN_CONSUMER_GROUP_NAME` - the name of the consumer group each consumer
is added to
  - default: `main-consumers`

## Description

The solution relies on Redis Streams and consumer groups. There is a process
that transfer all messaged from the Pub/Sub topic into a stream. Another
process registers the consumer group and the consumers inside of it. The 
consumer group ensures only one consumer will process each message.

By using Streams we're changing the way of communication a bit because we gain 
the ability to process messages that were published before our consumer was
started which would not be possible in a pure pub/sub.

## Notes

- I forgot to create the git repository when I started working so I created
the first commits from IntelliJ local history + memory so the different major 
iterations are visible
- Registering each consumer as listener to the Pub/Sub was great as the
interface incorporates the life-cycle of the consumer (sub/unsub) but using
keys to synchronise was actually hurting the throughput rather than 
increasing it
- Switching to sets for the consumer sync I was expecting to perform a lot
better than string keys but it was only slightly better as still best with 1
consumer
- The list was promising on paper but having the check for elements to read
constantly kept me searching for another solution. Also spawning multiple
infinite Flux streams produced a ton a errors above 10-20 consumers
- Streams and Consumer Groups looked like the best approach of "push"-ing
messages and registering multiple consumers relatively easy passing the sync
task back to Redis. I had tons of issues (mostly due to my lack
of knowledge on how to use it properly) with reading the stream, staying
alive for new "events" and general errors overall

## Improvements

- Actually processing ~2x messages with 2x the consumers
- Testing
- Error handling
- Dangling Redis connections