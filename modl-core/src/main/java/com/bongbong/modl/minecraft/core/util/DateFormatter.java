package com.bongbong.modl.minecraft.core.util;

import com.bongbong.modl.minecraft.core.Constants;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class DateFormatter {
    public static String format(Date dateToFormat) {
        SimpleDateFormat date = new SimpleDateFormat(Constants.DATE_FORMAT);
        date.setTimeZone(TimeZone.getTimeZone("EST"));

        return date.format(dateToFormat) + " EST";
    }
}
