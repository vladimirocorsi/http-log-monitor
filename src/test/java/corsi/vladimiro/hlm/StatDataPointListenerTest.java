package corsi.vladimiro.hlm;

import corsi.vladimiro.hlm.parsing.DataPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class StatDataPointListenerTest {

    @Mock
    private DataPoint dataPoint;

    @Test
    void onDataPoint() {
        var listener = Mockito.spy(new StatDataPointListener(2, 10));

        Mockito.doReturn(1L).when(dataPoint).getUnixTimestamp();
        Mockito.doReturn("section1").when(dataPoint).getSection();
        listener.onDataPoint(dataPoint);
        Mockito.verify(listener, Mockito.never()).printStat(
                ArgumentMatchers.anyLong(),
                ArgumentMatchers.anyLong(),
                ArgumentMatchers.anyLong(),
                ArgumentMatchers.anyList());

        Mockito.doReturn(1L).when(dataPoint).getUnixTimestamp();
        Mockito.doReturn("section2").when(dataPoint).getSection();
        listener.onDataPoint(dataPoint);
        Mockito.verify(listener, Mockito.never()).printStat(
                ArgumentMatchers.anyLong(),
                ArgumentMatchers.anyLong(),
                ArgumentMatchers.anyLong(),
                ArgumentMatchers.anyList());

        Mockito.doReturn(1L).when(dataPoint).getUnixTimestamp();
        Mockito.doReturn("section3").when(dataPoint).getSection();
        listener.onDataPoint(dataPoint);
        Mockito.verify(listener, Mockito.never()).printStat(
                ArgumentMatchers.anyLong(),
                ArgumentMatchers.anyLong(),
                ArgumentMatchers.anyLong(),
                ArgumentMatchers.anyList());

        Mockito.doReturn(4L).when(dataPoint).getUnixTimestamp();
        Mockito.doReturn("section2").when(dataPoint).getSection();
        listener.onDataPoint(dataPoint);
        Mockito.verify(listener, Mockito.never()).printStat(
                ArgumentMatchers.anyLong(),
                ArgumentMatchers.anyLong(),
                ArgumentMatchers.anyLong(),
                ArgumentMatchers.anyList());

        Mockito.doReturn(3L).when(dataPoint).getUnixTimestamp();
        Mockito.doReturn("section1").when(dataPoint).getSection();
        listener.onDataPoint(dataPoint);
        Mockito.verify(listener, Mockito.never()).printStat(
                ArgumentMatchers.anyLong(),
                ArgumentMatchers.anyLong(),
                ArgumentMatchers.anyLong(),
                ArgumentMatchers.anyList());

        Mockito.doReturn(9L).when(dataPoint).getUnixTimestamp();
        Mockito.doReturn("section1").when(dataPoint).getSection();
        listener.onDataPoint(dataPoint);
        Mockito.verify(listener, Mockito.never()).printStat(
                ArgumentMatchers.anyLong(),
                ArgumentMatchers.anyLong(),
                ArgumentMatchers.anyLong(),
                ArgumentMatchers.anyList());

        //this point won't be part of the printed statistics, it is in a new interval.
        Mockito.doReturn(10L).when(dataPoint).getUnixTimestamp();
        Mockito.doReturn("section1").when(dataPoint).getSection();
        listener.onDataPoint(dataPoint);

        var expectedLabelMap = List.of(
                Map.entry("section1", 3L),
                Map.entry("section2", 2L));

        Mockito.verify(listener).printStat(
                ArgumentMatchers.eq(0L),
                ArgumentMatchers.eq(10L),
                ArgumentMatchers.eq(6L),
                ArgumentMatchers.eq(expectedLabelMap));
    }
}