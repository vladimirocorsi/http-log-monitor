# HTTP Log Monitor
Take-home assignment for ***

## Effort
Time spent: about **15** hours.

## Building

Requires
* Maven 3;
* JDK 14.

Compile, test and package with:

        mvn clean package

## Running

Run the example file with (Windows syntax here):

        java -jar .\target\http-log-monitor-jar-with-dependencies.jar .\Log_File.txt

Launching with input expected from stdin with:

        java -jar .\target\http-log-monitor-jar-with-dependencies.jar

## Program Structure

### Parsing
Parsing logic is implemented in class _CSVLogParser_ which takes a _BufferedReader_ as input
and emits parsed records in form of _DataPoint_ objects towards a collection of _DataPointListener_.

### Statistics
The logic printing count statistics for each 10-second interval is implemented in
_StatDataPointListener_. This component receives ALL data points and aggregates them in intervals.
An aggregation here is an object storing, for a given time interval, 

* A cumulative count and
* A breakdown of the same count by _Labels_.

A _Labels_ object represents a point in the label space of our data points. Here for
simplicity Labels has a single dimension: the _section_ but more can be added like _remote host_, _request size_, etc.
The aggregation logic is a bit simplistic:

* When a data point is received it is assigned an interval [datapointTimestamp/intervalDuration, datapointTimestamp/intervalDuration + intervalDuration).
* We keep only one interval in memory so if the datapoint belongs to an interval in the past the point is DISCARDED.
* If the point belongs to the current interval then it is aggregated in the same Aggregation object.
* If the point belongs to an interval in the future then the current interval is completed, statistics are printed and the new interval becomes the current interval.

Total count is printed as well as the sections having the most hits.

### Alerting
Alerting logic is implemented in AlertDataPointListenerV2.
This components receives ALL data points. It performs aggregation in 1-second intervals and stores such
aggregations in a "sliding window" of 2 minutes.
For each Aggregation received by the window the hits per second ratio is calculated and alerts
are activated or deactivated accordingly.

## Criticalities
Some ideas for better design and performance.

### Many Allocations
While parsing we can potentially create a big amount of DataPoint objects, we could think
about using object pooling like done for example in Elastic APM agent (see https://github.com/elastic/apm-agent-java/blob/main/apm-agent-core/README.md#lifecycle).

### Duplicate Aggregations
Both Stats and Alerting components receive ALL the data points and then perform aggregation on their own.
We could think about introducing a single component performing aggregation in 1-second intervals and then chaining or run in parallel:
* The statistics logic, which would perform another aggregation in 10-seconds intervals;
* The alerting logic, which would hold a sliding window of 1-second intervals.

### No Memory when Calculating Statistics
We have already said that when we calculate statistics we only keep memory of one interval.
That means that we are obliged to discard Aggregation objects related to an interval which is before the current one. That can
happen because log entries are not strictly ordered.
We could introduce a sliding window (windowing logic has been extracted already in a separate class _TimeWindow_) of a tunable size to keep memory of more than one interval and print statistics only
when that interval quits the window. That of course is paid with potential delay in stats printing but allows for more accuracy.

### Enrich Aggregation API
Currently, the only form of aggregation is a counter. Think about introducing other types like histograms (See https://prometheus.io/docs/concepts/metric_types/).

### Poor Parallelism
Parsing, aggregation, stat and alert calculation (almost) all run in the same thread.
We could introduce separate threads running in executors defined in the single components with data points
(or Aggregation objects) flowing through a data structure like LinkedBlockingQueue or other (LMAX) for message passing.

### Introduce More Components
Logic for formatting and printing stats and alerts could be extracted in separate components with specific responsibility.

### Benchmarking Needed
Once optimizations done it would be interesting to benchmark the whole system and also gathering metrics for both the
whole process and the single components.

## Imagining a Distributed System
If we had to make a distributed system out of this we could think about the following components.

### Lightweight Log Shipper
Log Shipper would just contain the parsing logic and forward data points to a distributed broker like Kafka.
We could route data points to different queues/partitions based on some label values, for example the _section_.

### Aggregator
Aggregator would be responsible for aggregating data points in small intervals (1 second) and forward those aggregation to
other message queues/partitions. 

### Stat Calculator
Several Stat Calculator components could be used to print statistics for distinct subsets of Aggregation objects.
Each component would consume from a specific subsets of queues/partitions.

### Alert Manager
Alert Manager components would listen to the same queues as Stat Calculator.