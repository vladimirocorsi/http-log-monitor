package corsi.vladimiro.hlm.parsing;

import javax.annotation.Nonnull;

/**
 * Receives {@link DataPoint} objects from {@link CSVLogParser}.
 */
public interface DataPointListener {

    void onDataPoint(@Nonnull DataPoint dataPoint);

}
