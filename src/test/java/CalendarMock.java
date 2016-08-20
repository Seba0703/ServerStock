import Wrappers.CalendarWrapper;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * fecha mockeada 20161231 y 201612
 */
public class CalendarMock extends CalendarWrapper{

    private int day;
    private int month;
    private int year;

    public void set(int day, int month, int year) {

        this.day = day;
        this.month = month;
        this.year = year;
    }

    public int getYYYYMMDD() {
        return 20161231;
    }

    public int getYYYYMM() {
        return 201612;
    }

}
