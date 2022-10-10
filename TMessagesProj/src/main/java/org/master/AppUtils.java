package org.master;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class AppUtils {

    public static long getTimeInMill(String dateStr){
        try{
            String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'";
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date serverDate = simpleDateFormat.parse(dateStr);
            return  serverDate.toInstant().toEpochMilli();
        }catch (Exception ignore){

        }
        return 0;
    }
}
