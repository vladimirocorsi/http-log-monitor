package corsi.vladimiro.hlm.parsing;

import com.opencsv.exceptions.CsvValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class CSVLogParserTest {

    @Mock
    private DataPointListener listener;

    @Test
    void parse() throws CsvValidationException, IOException {

        //given
        String content = String.join("\n"
                , "\"remotehost\",\"rfc931\",\"authuser\",\"date\",\"request\",\"status\",\"bytes\""
                , "\"10.0.0.1\",\"-\",\"apache\",1549574332,\"GET /api/user HTTP/1.0\",200,1234"
                , "\"10.0.0.4\",\"-\",\"apache\",1549574333,\"GET /report HTTP/1.0\",200,1136");

        //when
        CSVLogParser.builder(new BufferedReader(new StringReader(content)))
                .withListeners(listener).build().parse();

        //then
        ArgumentCaptor<DataPoint> captor = ArgumentCaptor.forClass(DataPoint.class);
        Mockito.verify(listener, Mockito.times(2)).onDataPoint(captor.capture());

        assertEquals(1549574332, captor.getAllValues().get(0).getUnixTimestamp());
        assertEquals("/api", captor.getAllValues().get(0).getSection());
        assertEquals(1234, captor.getAllValues().get(0).getBytes());
        assertEquals("10.0.0.1", captor.getAllValues().get(0).getRemoteHost());
        assertEquals("200", captor.getAllValues().get(0).getStatus());

        assertEquals(1549574333, captor.getAllValues().get(1).getUnixTimestamp());
        assertEquals("/report", captor.getAllValues().get(1).getSection());
    }

    @Test
    void parse_with_invalid_line() throws CsvValidationException, IOException {

        //given
        String content = String.join("\n"
                , "\"remotehost\",\"rfc931\",\"authuser\",\"date\",\"request\",\"status\",\"bytes\""
                , "\"10.0.0.1\",\"-\",\"apache\",\"not a timestamp\",\"GET /api/user HTTP/1.0\",200,1234"
                , "\"10.0.0.4\",\"-\",\"apache\",1549574333,\"GET /report HTTP/1.0\",200,1136");

        //when
        CSVLogParser.builder(new BufferedReader(new StringReader(content)))
                .withListeners(listener).build().parse();

        //then
        ArgumentCaptor<DataPoint> captor = ArgumentCaptor.forClass(DataPoint.class);
        Mockito.verify(listener, Mockito.times(1)).onDataPoint(captor.capture());
        assertEquals(1549574333, captor.getAllValues().get(0).getUnixTimestamp());
    }

    @Test
    void parseSection() {
        assertEquals("/api", CSVLogParser.parseSection("/api/request/ HTTP/1.0"));
        assertEquals("/apiv2", CSVLogParser.parseSection("/apiv2 HTTP/1.0"));
        assertEquals("/apiv3", CSVLogParser.parseSection("POST /apiv3 HTTP"));
    }
}