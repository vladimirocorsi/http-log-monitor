package corsi.vladimiro.hlm.aggregation;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

import javax.annotation.Nonnull;

public class Labels {

    private final String section;

    public static Labels of(@Nonnull String section)
    {
        Preconditions.checkNotNull(section);
        return new Labels(section);
    }

    private Labels(String section)
    {
        this.section = section;
    }

    @Nonnull
    public String getSection()
    {
        return section;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        Labels labels = (Labels) o;
        return Objects.equal(section, labels.section);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(section);
    }
}
