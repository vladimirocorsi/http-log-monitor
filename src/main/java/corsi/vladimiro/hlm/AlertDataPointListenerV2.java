package corsi.vladimiro.hlm;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import corsi.vladimiro.hlm.aggregation.Aggregation;
import corsi.vladimiro.hlm.aggregation.Aggregator;
import corsi.vladimiro.hlm.parsing.DataPoint;
import corsi.vladimiro.hlm.parsing.DataPointListener;

import javax.annotation.Nonnull;
import java.util.TreeMap;

/**
 * Receives data points, aggregates them and checks hits per second rate over a
 * configurable time window. Alerts are activated if hit rate passes a given threshold
 * and are deactivated when the rate decreases below the threshold.
 */
public class AlertDataPointListenerV2 implements DataPointListener {

    private final TreeMap<Long, Aggregation> timeWindow = new TreeMap<>();
    private final Aggregator aggregator;
    private final int timeWindowInSecs;
    private final double hitsPerSecsThreshold;

    private boolean alertActive = false;
    private long count;

    public AlertDataPointListenerV2()
    {
        this(120, 10D, 1);
    }

    /**
     * Constructs a new {@link AlertDataPointListenerV2}.
     * @param timeWindowInSecs width of the sliding window, positive integer.
     * @param hitsPerSecsThreshold threshold for the hit rate, positive.
     * @param granularityInSecs duration of the integration interval, must be less or equal timeWindowInSecs.
     */
    public AlertDataPointListenerV2(int timeWindowInSecs,
                                  double hitsPerSecsThreshold,
                                  int granularityInSecs)
    {
        Preconditions.checkArgument(granularityInSecs > 0);
        aggregator =  new Aggregator(granularityInSecs);
        Preconditions.checkArgument(timeWindowInSecs >= granularityInSecs);
        this.timeWindowInSecs = timeWindowInSecs;
        Preconditions.checkArgument(hitsPerSecsThreshold > 0);
        this.hitsPerSecsThreshold = hitsPerSecsThreshold;
    }

    /**
     * Submits a data point.
     * @param dataPoint the {@link DataPoint} to submit.
     */
    @Override
    public void onDataPoint(@Nonnull DataPoint dataPoint)
    {
        var aggregation = aggregator.submit(dataPoint);
        aggregation.ifPresent(this::handleAggregation);
    }

    private void handleAggregation(Aggregation aggregation)
    {
        this.count += aggregation.getTotalCount();
        var existing = timeWindow.get(aggregation.getBeginTimestamp());
        if (existing != null)
        {
            aggregation = aggregation.sum(existing);
        }
        timeWindow.put(aggregation.getBeginTimestamp(), aggregation);
        long endOfWindow = timeWindow.descendingMap().firstEntry().getValue().getBeginTimestamp();
        var timeWindowIt = timeWindow.entrySet().iterator();
        while (timeWindowIt.hasNext())
        {
            var curEntry = timeWindowIt.next();
            var curTimestamp = curEntry.getValue().getBeginTimestamp();
            if (endOfWindow - curTimestamp >= timeWindowInSecs)
            {
                count = count - curEntry.getValue().getTotalCount();
                timeWindowIt.remove();
            }
        }
        double hitPerSecondAverage = (double)count / timeWindowInSecs;
        if (hitPerSecondAverage > hitsPerSecsThreshold && !alertActive)
        {
            alertActive = true;
            alertActive(endOfWindow);
        }
        else if (hitPerSecondAverage <= hitsPerSecsThreshold && alertActive)
        {
            alertActive = false;
            alertInactive(endOfWindow);
        }
    }

    @VisibleForTesting
    void alertInactive(long timestamp)
    {
        System.out.println("Alert deactivated at time " + timestamp);
    }

    @VisibleForTesting
    void alertActive(long timestamp)
    {
        System.out.println("Alert activated at time " + timestamp);
    }

}
