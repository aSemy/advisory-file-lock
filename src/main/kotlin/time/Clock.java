package time;

public interface Clock {

    /**
     * The current time in milliseconds since 1970-01-01T00:00:00Z (UTC).
     */
    long getCurrentTime();
}
