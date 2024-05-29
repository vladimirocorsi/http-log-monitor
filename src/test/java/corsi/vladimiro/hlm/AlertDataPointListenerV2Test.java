package corsi.vladimiro.hlm;

import corsi.vladimiro.hlm.parsing.DataPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AlertDataPointListenerV2Test {

    @Mock
    private DataPoint dataPoint;

    @Test
    void onDataPoint()
    {
        var listener =
                Mockito.spy(new AlertDataPointListenerV2(10, 2D, 1));

        Mockito.doReturn(11L).when(dataPoint).getUnixTimestamp();
        listener.onDataPoint(dataPoint);
        Mockito.doReturn(8L).when(dataPoint).getUnixTimestamp();
        listener.onDataPoint(dataPoint);
        Mockito.doReturn(9L).when(dataPoint).getUnixTimestamp();
        listener.onDataPoint(dataPoint);

        //no alert
        Mockito.verify(listener, Mockito.never()).alertActive(ArgumentMatchers.anyLong());
        Mockito.verify(listener, Mockito.never()).alertInactive(ArgumentMatchers.anyLong());

        //let's go to 20 + 1 points in 10 seconds
        Mockito.doReturn(2L).when(dataPoint).getUnixTimestamp();
        for (int i = 0; i < 18; i++)
        {
            listener.onDataPoint(dataPoint);
        }
        //we add another point at a different timestamp to complete the aggregation
        Mockito.doReturn(3L).when(dataPoint).getUnixTimestamp();
        listener.onDataPoint(dataPoint);
        //verify alert
        Mockito.verify(listener, Mockito.times(1)).alertActive(11L);
        //add more points and check no additional alert
        for (int i = 0; i < 18; i++)
        {
            listener.onDataPoint(dataPoint);
        }
        Mockito.verify(listener, Mockito.times(1)).alertActive(11L);

        //no let's go forward with time
        Mockito.doReturn(30L).when(dataPoint).getUnixTimestamp();
        listener.onDataPoint(dataPoint);
        //we add another point at a different timestamp to complete the aggregation
        Mockito.doReturn(29L).when(dataPoint).getUnixTimestamp();
        listener.onDataPoint(dataPoint);
        //check alert deactivated
        Mockito.verify(listener, Mockito.times(1)).alertInactive(30L);

        //reach 20 points in window and check no additional deactivation or activation
        for (int i = 0; i < 18; i++)
        {
            listener.onDataPoint(dataPoint);
        }
        //we add another one at a different timestamp to complete the aggregation
        Mockito.doReturn(31L).when(dataPoint).getUnixTimestamp();
        listener.onDataPoint(dataPoint);
        Mockito.verify(listener, Mockito.times(1)).alertInactive(ArgumentMatchers.anyLong());
        Mockito.verify(listener, Mockito.times(1)).alertActive(ArgumentMatchers.anyLong());

        //add another point to complete previous aggregation and check for additional alert
        Mockito.doReturn(35L).when(dataPoint).getUnixTimestamp();
        listener.onDataPoint(dataPoint);
        Mockito.verify(listener, Mockito.times(1)).alertInactive(30L);
        Mockito.verify(listener, Mockito.times(1)).alertActive(11L);
        Mockito.verify(listener, Mockito.times(1)).alertActive(31L);

    }
}