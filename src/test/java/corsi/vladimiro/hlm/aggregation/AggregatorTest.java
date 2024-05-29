package corsi.vladimiro.hlm.aggregation;

import corsi.vladimiro.hlm.parsing.DataPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class AggregatorTest {

    @Mock
    private DataPoint dataPoint;

    @Test
    void submit_with_no_labels() {
        var aggregator = new Aggregator(10);
        Mockito.doReturn(1L).when(dataPoint).getUnixTimestamp();
        assertTrue(aggregator.submit(dataPoint).isEmpty());

        Mockito.doReturn(2L).when(dataPoint).getUnixTimestamp();
        assertTrue(aggregator.submit(dataPoint).isEmpty());

        Mockito.doReturn(10L).when(dataPoint).getUnixTimestamp();
        var aggregation = aggregator.submit(dataPoint);

        assertEquals(2, aggregation.get().getTotalCount());
    }

    @Test
    void submit_with_labels() {
        var aggregator = new Aggregator(10,
                dataPoint1 -> Optional.of(Labels.of(dataPoint1.getSection())));
        Mockito.doReturn(1L).when(dataPoint).getUnixTimestamp();
        Mockito.doReturn("section1").when(dataPoint).getSection();
        assertTrue(aggregator.submit(dataPoint).isEmpty());

        Mockito.doReturn(2L).when(dataPoint).getUnixTimestamp();
        Mockito.doReturn("section2").when(dataPoint).getSection();
        assertTrue(aggregator.submit(dataPoint).isEmpty());

        Mockito.doReturn(10L).when(dataPoint).getUnixTimestamp();
        Mockito.doReturn("section1").when(dataPoint).getSection();
        var aggregation = aggregator.submit(dataPoint);

        assertEquals(2, aggregation.get().getTotalCount());
        assertEquals(1, aggregation.get().getLabelCounts().get(Labels.of("section1")));
        assertEquals(1, aggregation.get().getLabelCounts().get(Labels.of("section2")));
    }
}