import Wrappers.CalendarWrapper;
import org.joda.time.DateTime;
import org.junit.Test;

/**
 * Created by Sebastian on 19/08/2016.
 */
public class TestTime {

    @Test
    public void testTIme() {

        DateTime date = new DateTime();
        String pattern = "yyyyMM";

        System.out.println(date.getMonthOfYear());

        System.out.println(CalendarWrapper.nextTrimester(201610));

    }

}
