package corsi.vladimiro.hlm.aggregation;

import com.google.common.base.Preconditions;

import javax.annotation.Nonnull;
import java.util.TreeMap;

//! In progress, UNTESTED !
public class TimeWindow {

    private final TreeMap<Long, Aggregation> timeWindow = new TreeMap<>();
    private final int timeWindowInSecs;
    private final int granularityInSecs;
    private final AggregationListener[] listeners;
    private long count;

    public TimeWindow(int timeWindowInSecs,
                      int granularityInSecs,
                      @Nonnull AggregationListener... listeners)
    {
        Preconditions.checkArgument(granularityInSecs > 0);
        Preconditions.checkArgument(timeWindowInSecs >= granularityInSecs);
        for (var listener : listeners)
        {
            Preconditions.checkNotNull(listener);
        }
        this.granularityInSecs = granularityInSecs;
        this.timeWindowInSecs = timeWindowInSecs;
        this.listeners = listeners;
    }

    public void submit(Aggregation aggregation)
    {
        this.count += aggregation.getTotalCount();
        var key = aggregation.getBeginTimestamp() / granularityInSecs;
        var newBeginTimestamp = key * granularityInSecs;
        var newEndTimestamp = newBeginTimestamp + granularityInSecs;
        var existing = timeWindow.get(key);
        if (existing != null)
        {
            aggregation = existing.sum(aggregation);
        } else
        {
            aggregation = aggregation.withNewTimestamps(newBeginTimestamp, newEndTimestamp);
        }
        timeWindow.put(key, aggregation);

        long endOfWindow = getEndOfWindow();
        var timeWindowIt = timeWindow.entrySet().iterator();
        while (timeWindowIt.hasNext())
        {
            var curEntry = timeWindowIt.next();
            var curTimestamp = curEntry.getValue().getBeginTimestamp();
            if (endOfWindow - curTimestamp > timeWindowInSecs)
            {
                count = count - curEntry.getValue().getTotalCount();
                notifyListeners(curEntry.getValue());
                timeWindowIt.remove();
            }
        }
    }

    private void notifyListeners(Aggregation aggregation)
    {
        for (var listener: listeners)
        {
            listener.onAggregation(aggregation);
        }
    }

    public long getCount()
    {
        return count;
    }

    public int getTimeWindowInSecs()
    {
        return timeWindowInSecs;
    }

    public long getEndOfWindow()
    {
        return timeWindow.descendingMap().firstEntry().getValue().getEndTimestamp();
    }
}
