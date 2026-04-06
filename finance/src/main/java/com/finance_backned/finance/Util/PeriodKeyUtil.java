package com.finance_backned.finance.Util;



import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;

public class PeriodKeyUtil {

    public static List<String[]> getPeriodBuckets(LocalDate date) {
        String year  = String.valueOf(date.getYear());
        String month = date.format(DateTimeFormatter.ofPattern("yyyy-MM"));

        int weekNum = date.get(WeekFields.ISO.weekOfWeekBasedYear());
        int weekYear = date.get(WeekFields.ISO.weekBasedYear());
        String week = String.format("%d-W%02d", weekYear, weekNum);

        return List.of(
                new String[]{"week",  week},
                new String[]{"month", month},
                new String[]{"year",  year}
        );
    }
}
