package corsi.vladimiro.hlm.parsing;

import com.google.common.base.Preconditions;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

/**
 * Represents a log line.
 */
@Immutable
public class DataPoint {

    private final long unixTimestamp;
    private final String section;
    private final String status;
    private final long bytes;
    private final String remoteHost;

    DataPoint(long timestamp,
                     @Nonnull  String section,
                     @Nonnull String status,
                     long bytes,
                     @Nonnull String remoteHost) {
        Preconditions.checkArgument(timestamp >= 0);
        this.unixTimestamp = timestamp;
        this.section = Preconditions.checkNotNull(section);
        this.status = Preconditions.checkNotNull(status);
        Preconditions.checkArgument(bytes >= 0);
        this.bytes = bytes;
        this.remoteHost = Preconditions.checkNotNull(remoteHost);
    }

    public long getUnixTimestamp() {
        return unixTimestamp;
    }

    @Nonnull
    public String getSection() {
        return section;
    }

    @Nonnull
    public String getStatus() {
        return status;
    }

    public long getBytes() {
        return bytes;
    }

    @Nonnull
    public String getRemoteHost() {
        return remoteHost;
    }
}
