package corsi.vladimiro.hlm.aggregation;

import com.google.common.base.Preconditions;
import org.checkerframework.checker.units.qual.A;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a counter of multiple {@link corsi.vladimiro.hlm.parsing.DataPoint}s.
 * Has associated an interval in time related to the timestamp of the data points counted.
 * Also contains a breakdown of the count by {@link Labels} where a {@link Labels} object represents
 * a point in the label space of the data point, e.g: HTTP status code + request section + remote host...
 */
public class Aggregation {

    private final long beginTimestamp;
    private final long endTimestamp;
    private long totalCount;

    private final Map<Labels, Long> labelCounts;

    Aggregation(long beginTimestamp, long endTimestamp) {
        Preconditions.checkArgument(beginTimestamp >= 0);
        Preconditions.checkArgument(endTimestamp > beginTimestamp);
        this.beginTimestamp = beginTimestamp;
        this.endTimestamp = endTimestamp;
        this.totalCount = 0;
        this.labelCounts = new HashMap<>();
    }

    void increment(@Nonnull Labels labels) {
        increment();
        long labelCount = labelCounts.getOrDefault(labels, 0L);
        labelCounts.put(labels, labelCount + 1);
    }

    void increment()
    {
        totalCount = totalCount + 1;
    }

    /**
     * @return the count of this {@link Aggregation}.
     */
    public long getTotalCount() {
        return totalCount;
    }

    /**
     * @return the breakdown of the count by {@link Labels}.
     */
    @Nonnull
    public Map<Labels, Long> getLabelCounts() {
        return Collections.unmodifiableMap(labelCounts);
    }

    public long getBeginTimestamp() {
        return beginTimestamp;
    }

    public long getEndTimestamp() {
        return endTimestamp;
    }

    /**
     * @param aggregation the {@link Aggregation} to add to this {@link Aggregation}.
     * @return a new {@link Aggregation} having the same time interval as this {@link Aggregation}
     * and having as count (both total and breakdown) the sum of the counts of the two {@link Aggregation}.
     * Although this is not enforced you must probably make sure that you are not adding aggregations with
     * different time intervals.
     */
    @Nonnull
    public Aggregation sum(Aggregation aggregation)
    {
        var result = new Aggregation(this.beginTimestamp, this.endTimestamp);
        result.totalCount = this.totalCount + aggregation.totalCount;
        for (var e: this.labelCounts.entrySet())
        {
            result.labelCounts.put(e.getKey(),
                    e.getValue() + aggregation.labelCounts.getOrDefault(e.getKey(), 0L));
        }
        for (var e: aggregation.labelCounts.entrySet())
        {
            if (!this.labelCounts.containsKey(e.getKey()))
            {
                result.labelCounts.put(e.getKey(),e.getValue());
            }
        }
        return result;
    }

    public Aggregation withNewTimestamps(long newBeginTimestamp, long newEndTimestamp) {
        var result = new Aggregation(newBeginTimestamp, newEndTimestamp);
        result.totalCount = this.totalCount;
        result.labelCounts.putAll(this.labelCounts);
        return result;
    }

    public static Aggregation single(long beginTimestamp, long endTimestamp) {
        var aggregation = new Aggregation(beginTimestamp, endTimestamp);
        aggregation.increment();
        return aggregation;
    }
}
