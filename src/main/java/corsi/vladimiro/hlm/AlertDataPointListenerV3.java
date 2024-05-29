package corsi.vladimiro.hlm;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import corsi.vladimiro.hlm.aggregation.Aggregation;
import corsi.vladimiro.hlm.aggregation.AggregationListener;
import corsi.vladimiro.hlm.aggregation.TimeWindow;
import corsi.vladimiro.hlm.parsing.DataPoint;
import corsi.vladimiro.hlm.parsing.DataPointListener;

import javax.annotation.Nonnull;
//! In progress, UNUSED, UNTESTED !
public class AlertDataPointListenerV3 implements DataPointListener {

    private final TimeWindow timeWindow;
    private final double hitsPerSecsThreshold;

    private boolean alertActive = false;

    public AlertDataPointListenerV3()
    {
        this(120, 10D, 1);
    }

    public AlertDataPointListenerV3(int timeWindowInSecs,
                                    double hitsPerSecsThreshold,
                                    int granularityInSecs)
    {
        Preconditions.checkArgument(granularityInSecs > 0);
        Preconditions.checkArgument(hitsPerSecsThreshold > 0);
        this.hitsPerSecsThreshold = hitsPerSecsThreshold;
        this.timeWindow = new TimeWindow(timeWindowInSecs, granularityInSecs, new AggregationListener[]{});
    }

    @Override
    public void onDataPoint(@Nonnull DataPoint dataPoint)
    {
        handleAggregation(Aggregation.single(
                dataPoint.getUnixTimestamp(),
                dataPoint.getUnixTimestamp() + 1));
    }

    private void handleAggregation(Aggregation aggregation)
    {
        timeWindow.submit(aggregation);
        double hitPerSecondAverage = (double)timeWindow.getCount() / timeWindow.getTimeWindowInSecs();
        if (hitPerSecondAverage > hitsPerSecsThreshold && !alertActive)
        {
            alertActive = true;
            alertActive(timeWindow.getEndOfWindow());
        }
        else if (hitPerSecondAverage <= hitsPerSecsThreshold && alertActive)
        {
            alertActive = false;
            alertInactive(timeWindow.getEndOfWindow());
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
