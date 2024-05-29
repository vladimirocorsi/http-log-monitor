package corsi.vladimiro.hlm;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import corsi.vladimiro.hlm.aggregation.Aggregation;
import corsi.vladimiro.hlm.aggregation.Aggregator;
import corsi.vladimiro.hlm.aggregation.Labels;
import corsi.vladimiro.hlm.parsing.DataPoint;
import corsi.vladimiro.hlm.parsing.DataPointListener;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Receives {@link DataPoint} and prints the count of received requests for every 10 second interval.
 * The time interval of a data point is identified by the integer division of its timestamp by 10.
 * This object has no memory of past intervals, so only one interval is considered at a time.
 * When a data point arrives which lies in a subsequent interval,
 * then the current interval is completed and stats printed.
 * When a data point arrives which lies in a past interval it is discarded and not aggregated.
 */
public class StatDataPointListener implements DataPointListener {

    private final Aggregator aggregator;

    private final int maxNumOfSections;

    private long lastHandledEndTimestamp;

    /**
     * Constructs a {@link StatDataPointListener} 10 as the maximum number of printed sections and
     * an interval duration of 10 seconds.
     */
    public StatDataPointListener()
    {
        this(10, 10);
    }

    /**
     * @param maxNumOfSections maximum number of sections for which we print request counts.
     * @param intervalInSeconds duration of the time interval used for aggregation.
     */
    public StatDataPointListener(int maxNumOfSections, int intervalInSeconds)
    {
        Preconditions.checkArgument(maxNumOfSections > 0);
        Preconditions.checkArgument(intervalInSeconds > 0);
        this.maxNumOfSections = maxNumOfSections;
        aggregator = new Aggregator(intervalInSeconds,
                dataPoint -> Optional.of(Labels.of(dataPoint.getSection())));
    }

    /**
     * Receives a {@link DataPoint} and performs aggregation and stat printing if it is the case.
     * The whole process is synchronous. We could optimize by enqueuing data points internally (blocking queue,
     * LMAX disruptor...) and have an asynchronous process handling aggregation and printing.
     *
     * @param dataPoint {@link DataPoint} to aggregate. If it lies in an interval which is before
     *                                   the current interval the point is discarded.
     */
    @Override
    public void onDataPoint(@Nonnull DataPoint dataPoint) {
        if (lastHandledEndTimestamp >= dataPoint.getUnixTimestamp())
        {
            return;
        }
        var aggregation = aggregator.submit(dataPoint);
        aggregation.ifPresent(this::handleAggregation);
    }

    private void handleAggregation(Aggregation aggregation) {
        lastHandledEndTimestamp = aggregation.getEndTimestamp();
        var sectionToCount = aggregation.getLabelCounts().entrySet().parallelStream()
                .collect(
                        //here we want to distribute the aggregation between cores
                        //but we have no control over which section goes to which core.
                        //We should refactor this to map each section to the same thread
                        //and then reaggregate to obtain a single map. It would be necessary to benchmark
                        //to assess the benefits.
                        Collectors.groupingByConcurrent(
                                labelCount -> labelCount.getKey().getSection(),
                                Collectors.summingLong(Map.Entry::getValue)
                        )
                );
        var sectionToCountSorted = sectionToCount.entrySet().stream()
                .sorted(java.util.Map.Entry.<String,Long>comparingByValue().reversed())
                .limit(maxNumOfSections)
                .collect(Collectors.toList());

        printStat(
                aggregation.getBeginTimestamp(),
                aggregation.getEndTimestamp(),
                aggregation.getTotalCount(),
                sectionToCountSorted
        );
    }

    @VisibleForTesting
    void printStat(long beginTimestamp,
                   long endTimestamp,
                   long totalCount,
                   List<Map.Entry<String, Long>> sectionToCountSorted)
    {
        StringBuilder sb = new StringBuilder("Statistics from ")
                .append(beginTimestamp)
                .append(" to ")
                .append(endTimestamp)
                .append(": Total hits=")
                .append(totalCount)
                .append(".\n");
        for (var sectionToCount : sectionToCountSorted)
        {
            sb.append(sectionToCount.getKey())
                    .append("= ")
                    .append(sectionToCount.getValue())
                    .append("\n");
        }
        System.out.println(sb);
    }
}
