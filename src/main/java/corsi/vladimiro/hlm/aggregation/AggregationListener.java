package corsi.vladimiro.hlm.aggregation;

import javax.annotation.Nonnull;

/**
 * Receives {@link Aggregation} objects.
 */
public interface AggregationListener {

    void onAggregation(@Nonnull Aggregation aggregation);

}
