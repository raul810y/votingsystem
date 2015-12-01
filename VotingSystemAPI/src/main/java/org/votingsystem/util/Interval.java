package org.votingsystem.util;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.text.ParseException;
import java.util.Date;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Interval {

    public enum Lapse {YEAR, MONTH, WEEK, DAY, HOUR, MINUTE, SECOND}

    private Date dateFrom;
    private Date dateTo;
    private Boolean currentWeekPeriod;

    public Interval(){}

    public Interval(Date dateFrom, Date dateTo) {
        this.dateFrom = new Date(dateFrom.getTime());
        this.dateTo = new Date(dateTo.getTime());
    }

    public Date getDateFrom() {
        return dateFrom;
    }

    public Date getDateTo() {
        return dateTo;
    }

    public static Interval parse(Map dataMap) throws ParseException {
        Date dateFrom = DateUtils.getDateFromString((String) dataMap.get("dateFrom"));
        Date dateTo = DateUtils.getDateFromString((String) dataMap.get("dateTo"));
        return new Interval(dateFrom, dateTo);
    }

    public Boolean isCurrentWeekPeriod() {
        if(currentWeekPeriod == null) {
            Interval period = DateUtils.getCurrentWeekPeriod();
            currentWeekPeriod = (dateFrom.compareTo(period.getDateFrom()) >=0 &&
                    dateTo.compareTo(period.getDateTo()) <= 0);
        }
        return currentWeekPeriod;
    }

    public void setCurrentWeekPeriod(Boolean currentWeekPeriod) {
        this.currentWeekPeriod = currentWeekPeriod;
    }

    public boolean inRange(Date dateToCheck) {
        return dateToCheck.compareTo(dateFrom) >= 0 && dateToCheck.compareTo(dateTo) <= 0;
    }

    @Override public String toString() {
        return "Period from [" + DateUtils.getDateStr(dateFrom) + " - " + DateUtils.getDateStr(dateTo) + "]";
    }
}
