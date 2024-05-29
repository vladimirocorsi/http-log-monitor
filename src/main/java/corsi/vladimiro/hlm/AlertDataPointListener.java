package corsi.vladimiro.hlm;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import corsi.vladimiro.hlm.parsing.DataPoint;
import corsi.vladimiro.hlm.parsing.DataPointListener;

import javax.annotation.Nonnull;
import java.util.TreeMap;

@Deprecated
public class AlertDataPointListener implements DataPointListener {

    private final TreeMap<Long, Long> timeWindow;
    private final int timeWindowInSecs;
    private final double hitsPerSecsThreshold;
    private final int granularityInSecs;

    private boolean alertActive = false;
    private long count;

    public AlertDataPointListener()
    {
        this(120, 10D, 1);
    }

    public AlertDataPointListener(int timeWindowInSecs,
                                  double hitsPerSecsThreshold,
                                  int granularityInSecs)
    {
        Preconditions.checkArgument(granularityInSecs > 0);
        Preconditions.checkArgument(timeWindowInSecs >= granularityInSecs);
        Preconditions.checkArgument(timeWindowInSecs % granularityInSecs == 0);
        Preconditions.checkArgument(hitsPerSecsThreshold > 0);
        this.timeWindowInSecs = timeWindowInSecs;
        this.hitsPerSecsThreshold = hitsPerSecsThreshold;
        this.granularityInSecs = granularityInSecs;
        this.timeWindow = new TreeMap<>();
    }

    @Override
    public void onDataPoint(@Nonnull DataPoint dataPoint)
    {
        handleAggregation(dataPoint.getUnixTimestamp());
    }

    private void handleAggregation(long timestamp)
    {
        var countKey = timestamp / granularityInSecs;
        var countValue = timeWindow.computeIfPresent(countKey, (k, v) -> v + 1);
        if (countValue == null)
        {
            timeWindow.put(countKey, 1L);
        }
        count ++;
        long endOfWindow = timeWindow.descendingMap().firstEntry().getKey() * granularityInSecs;
        var timeWindowIt = timeWindow.entrySet().iterator();
        while (timeWindowIt.hasNext())
        {
            var curEntry = timeWindowIt.next();
            var curTimestamp = curEntry.getKey() * granularityInSecs;
            if (endOfWindow - curTimestamp >= timeWindowInSecs)
            {
                count = count - curEntry.getValue();
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
