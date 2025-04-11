package time;

public interface CountdownTimer extends Timer {

    boolean hasExpired();

    long getRemainingMillis();

    long getTimeoutMillis();

}
