package corsi.vladimiro.hlm.aggregation;

import com.google.common.base.Preconditions;
import corsi.vladimiro.hlm.parsing.DataPoint;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.function.Function;

/**
 * Aggregates {@link DataPoint} objects lying in the same time interval. Interval duration
 * is configurable. When a new point is received which lies in a different
 * interval then this point is assigned to a new {@link Aggregation} and the previous {@link Aggregation} is returned
 * on the {@link Aggregator#submit(DataPoint)} method.
 * Time is divided in interval starting from zero: the Epoch. So [0,duration), [duration, 2duration),...
 */
public class Aggregator {

    private final int intervalInSecs;
    private final Function<DataPoint, Optional<Labels>> labelsExtractor;
    private Aggregation currentAggregation;

    /**
     * @param intervalInSecs length of the time intervals in which {@link DataPoint} are aggregated.
     */
    public Aggregator(int intervalInSecs)
    {
        this(intervalInSecs, dataPoint -> Optional.empty());
    }

    /**
     * @param intervalInSecs length of the time intervals in which {@link DataPoint} are aggregated.
     * @param labelsExtractor allows mapping a {@link DataPoint} to a set of labels to enrich the {@link Aggregation}
     *                        object.
     */
    public Aggregator(int intervalInSecs, @Nonnull Function<DataPoint, Optional<Labels>> labelsExtractor)
    {
        Preconditions.checkArgument(intervalInSecs > 0);
        Preconditions.checkNotNull(labelsExtractor);
        this.intervalInSecs = intervalInSecs;
        this.labelsExtractor = labelsExtractor;
    }

    /**
     * Allows to submit a {@link DataPoint} for aggregation. If the point lies in a different time
     * interval than the current aggregation, then the aggregation is considered completed and is returned.
     * The {@link Aggregator} assigns the new {@link DataPoint} then a new {@link Aggregation}.
     * @param dataPoint the {@link DataPoint} to submit for aggregation.
     * @return the {@link Aggregation} which is completed, if any.
     */
    @Nonnull
    public Optional<Aggregation> submit(@Nonnull DataPoint dataPoint)
    {
        final long beginTimestamp = dataPoint.getUnixTimestamp() / intervalInSecs * intervalInSecs;
        final long endTimestamp = beginTimestamp + intervalInSecs;

        if (currentAggregation == null)
        {
            currentAggregation = new Aggregation(beginTimestamp, endTimestamp);
        }

        Aggregation completedAggregation = null;
        if (currentAggregation.getBeginTimestamp() != beginTimestamp)
        {
            completedAggregation = currentAggregation;
            currentAggregation = new Aggregation(beginTimestamp, endTimestamp);
        }
        var labels = labelsExtractor.apply(dataPoint);
        if (labels.isEmpty())
        {
            currentAggregation.increment();
        } else
        {
            currentAggregation.increment(labels.get());
        }
        return Optional.ofNullable(completedAggregation);
    }

}
