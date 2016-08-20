package Wrappers;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.Calendar;

/**
 * Created by Sebastian on 15/08/2016.
 */
public class CalendarWrapper {

    private DateTime date;
    private static final String yyyyMMdd = "yyyyMMdd";
    private static final String yyyyMM = "yyyyMM";

    public CalendarWrapper() {
        date = new DateTime();
    }

    public int getYYYYMMDD() {
        return Integer.parseInt(date.toString(yyyyMMdd));
    }

    public int getYYYYMM() {
        return Integer.parseInt(date.toString(yyyyMM));
    }

    public static int nextTrimester(int lastUpdate) {
        DateTimeFormatter dft = DateTimeFormat.forPattern(yyyyMM);
        DateTime lastUpdateDate = dft.parseDateTime(String.valueOf(lastUpdate));

        return Integer.parseInt(lastUpdateDate.plusMonths(3).toString(yyyyMM));

    }

    public static int beforeTwo(int lastUpdate) {
        DateTimeFormatter dft = DateTimeFormat.forPattern(yyyyMM);
        DateTime lastUpdateDate = dft.parseDateTime(String.valueOf(lastUpdate));

        return Integer.parseInt(lastUpdateDate.minusMonths(2).toString(yyyyMM));

    }



}
