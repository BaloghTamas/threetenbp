/*
 * Copyright (c) 2010-2011, Stephen Colebourne & Michael Nascimento Santos
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 *  * Neither the name of JSR-310 nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package javax.time.i18n;

import java.io.Serializable;

import javax.time.Duration;
import javax.time.calendar.Calendrical;
import javax.time.calendar.CalendricalMerger;
import javax.time.calendar.Chronology;
import javax.time.calendar.DateTimeField;
import javax.time.calendar.DateTimeFieldRule;
import javax.time.calendar.ISOChronology;
import javax.time.calendar.ISOPeriodUnit;
import javax.time.calendar.InvalidCalendarFieldException;
import javax.time.calendar.LocalDate;
import javax.time.calendar.MonthOfYear;
import javax.time.calendar.PeriodUnit;

/**
 * The Historic calendar system.
 * <p>
 * HistoricChronology defines the rules of the Historic calendar system.
 * The Historic calendar has twelve months of 30 days followed by an additional
 * period of 5 or 6 days, modelled as the thirteenth month in this implementation.
 * <p>
 * Years are measured in the 'Era of the Martyrs'.
 * 0001-01-01 (Historic) equals 0284-08-29 (ISO).
 * The supported range is from Historic year 1 to year 9999 (inclusive).
 * <p>
 * HistoricChronology is immutable and thread-safe.
 *
 * @author Stephen Colebourne
 */
public final class HistoricChronology extends Chronology implements Serializable {

    /**
     * A serialization identifier for this class.
     */
    private static final long serialVersionUID = 1L;
    /**
     * The start of months in a standard year.
     */
    private static final int[] STANDARD_MONTH_START = new int[] {0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334};
    /**
     * The start of months in a leap year.
     */
    private static final int[] LEAP_MONTH_START = new int[] {0, 31, 60, 91, 121, 152, 182, 213, 244, 274, 305, 335};

    /**
     * The cutover from Julian to Gregorian.
     */
    private final LocalDate cutover;

    //-----------------------------------------------------------------------
    /**
     * Obtains an instance of <code>HistoricChronology</code> specifying the
     * cutover date when the Gregorian/ISO calendar system was first used.
     *
     * @param cutover  the cutover date, not null
     * @return a {@code HistoricChronology}, never null
     */
    public static HistoricChronology cutoverAt(final LocalDate cutover) {
        checkNotNull(cutover, "Cutover date must not be null");
        return new HistoricChronology(cutover);
    }

    /**
     * Obtains an instance of <code>HistoricChronology</code> using the standard
     * cutover date of 1582-10-15.
     *
     * @return a {@code HistoricChronology}, never null
     */
    public static HistoricChronology standardCutover() {
        return new HistoricChronology(HistoricDate.STANDARD_CUTOVER);
    }

    //-----------------------------------------------------------------------
    /**
     * Restrictive constructor.
     */
    private HistoricChronology(final LocalDate cutover) {
        this.cutover = cutover;
    }

    //-----------------------------------------------------------------------
    /**
     * Validates that the input value is not null.
     *
     * @param object  the object to check
     * @param errorMessage  the error to throw
     * @throws NullPointerException if the object is null
     */
    static void checkNotNull(Object object, String errorMessage) {
        if (object == null) {
            throw new NullPointerException(errorMessage);
        }
    }

    /**
     * Calculates the day-of-year from a date.
     *
     * @param date  the date to use, not null
     * @return the day-of-year
     */
    int getDayOfYear(HistoricDate date) {
        int moy0 = date.getMonthOfYear().ordinal();
        int dom = date.getDayOfMonth();
        if (isLeapYear(date.getYear())) {
            return LEAP_MONTH_START[moy0] + dom;
        } else {
            return STANDARD_MONTH_START[moy0] + dom;
        }
    }

    /**
     * Calculates the date from a year and day-of-year.
     *
     * @param year  the year, valid
     * @param dayOfYear  the day-of-year, valid
     * @return the date, never null
     */
    HistoricDate getDateFromDayOfYear(int year, int dayOfYear) {
        boolean leap = isLeapYear(year);
        if (dayOfYear == 366 && leap == false) {
            throw new InvalidCalendarFieldException("DayOfYear 366 is invalid for year " + year, dayOfYearRule());
        }
        int doy0 = dayOfYear - 1;
        int[] array = (leap ? LEAP_MONTH_START : STANDARD_MONTH_START);
        int month = 1;
        for ( ; month < 12; month++) {
            if (doy0 < array[month]) {
                break;
            }
        }
        MonthOfYear moy = MonthOfYear.of(month);
        int dom = dayOfYear - array[month - 1];
        return new HistoricDate(this, year, moy, dom);
    }

    //-----------------------------------------------------------------------
    /**
     * Checks if the specified year is a leap year.
     * <p>
     * A year is leap if the remainder after division by four equals three.
     * This method does not validate the year passed in, and only has a
     * well-defined result for years in the supported range.
     *
     * @param year  the year to check, not validated for range
     * @return true if the year is a leap year
     */
    public boolean isLeapYear(int year) {
        if (year < cutover.getYear()) {
            return JulianChronology.isLeapYear(year);
        } else if (year > cutover.getYear()) {
            return ISOChronology.isLeapYear(year);
        } else {
            if (cutover.getMonthOfYear().compareTo(MonthOfYear.FEBRUARY) < 0) {
                return false;  // TODO
            }
            return false;  // TODO
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the cutover date of the chronology.
     *
     * @return the cutover date of the chronology, never null
     */
    public LocalDate getCutover() {
        return cutover;
    }

    /**
     * Gets the name of the chronology.
     *
     * @return the name of the chronology, never null
     */
    @Override
    public String getName() {
        return "Historic " + cutover;
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the rule for the year field in the Historic chronology.
     *
     * @return the rule for the year field, never null
     */
    public DateTimeFieldRule eraRule() {
        return new EraRule(this);
    }

    /**
     * Gets the rule for the year field in the Historic chronology.
     *
     * @return the rule for the year field, never null
     */
    public DateTimeFieldRule yearOfEraRule() {
        return new YearRule(this);
    }

    /**
     * Gets the rule for the year field in the Historic chronology.
     *
     * @return the rule for the year field, never null
     */
    public DateTimeFieldRule yearRule() {
        return new YearRule(this);
    }

    /**
     * Gets the rule for the month-of-year field in the Historic chronology.
     *
     * @return the rule for the month-of-year field, never null
     */
    public DateTimeFieldRule monthOfYearRule() {
        return new MonthOfYearRule(this);
    }

    /**
     * Gets the rule for the day-of-month field in the Historic chronology.
     *
     * @return the rule for the day-of-month field, never null
     */
    public DateTimeFieldRule dayOfMonthRule() {
        return new DayOfMonthRule(this);
    }

    /**
     * Gets the rule for the day-of-year field in the Historic chronology.
     *
     * @return the rule for the day-of-year field, never null
     */
    public DateTimeFieldRule dayOfYearRule() {
        return new DayOfYearRule(this);
    }

    /**
     * Gets the rule for the day-of-week field in the Historic chronology.
     *
     * @return the rule for the day-of-week field, never null
     */
    public DateTimeFieldRule dayOfWeekRule() {
        return new DayOfWeekRule(this);
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the period unit for eras.
     * <p>
     * The period unit defines the concept of a period of an era.
     * <p>
     * This is a basic unit and has no equivalent period.
     * The estimated duration is equal to 2,000,000,000 years.
     * This is equivalent to the ISO era period unit.
     * <p>
     * See {@link #eraRule()} for the main date-time field.
     *
     * @return the period unit for years, never null
     */
    public static PeriodUnit periodEras() {
        return ISOPeriodUnit.ERAS;
    }

    /**
     * Gets the period unit for years.
     * <p>
     * The period unit defines the concept of a period of a year.
     * This has an estimated duration equal to 365.25 days.
     * <p>
     * See {@link #yearRule()} for the main date-time field.
     *
     * @return the period unit for years, never null
     */
    public static PeriodUnit periodYears() {
        return YEARS;
    }

    /**
     * Gets the period unit for months.
     * <p>
     * The period unit defines the concept of a period of a month.
     * Historic months are typically 30 days long, except for the 13th month which is
     * 5 or 6 days long. The rule uses an estimated duration of 29.5 days.
     * <p>
     * See {@link #monthOfYearRule()} for the main date-time field.
     *
     * @return the period unit for months, never null
     */
    public static PeriodUnit periodMonths() {
        return MONTHS;
    }

    /**
     * Gets the period unit for weeks.
     * <p>
     * The period unit defines the concept of a period of a week.
     * This is equivalent to the ISO weeks period unit.
     *
     * @return the period unit for weeks, never null
     */
    public static PeriodUnit periodWeeks() {
        return ISOPeriodUnit.WEEKS;
    }

    /**
     * Gets the period unit for days.
     * <p>
     * The period unit defines the concept of a period of a day.
     * This is equivalent to the ISO days period unit.
     * <p>
     * See {@link #dayOfMonthRule()} for the main date-time field.
     *
     * @return the period unit for days, never null
     */
    public static PeriodUnit periodDays() {
        return ISOPeriodUnit.DAYS;
    }

    //-----------------------------------------------------------------------
    /**
     * Merges the fields.
     * 
     * @param merger  the merge context
     */
    void merge(CalendricalMerger merger) {
        // TODO: era
        DateTimeField year = merger.getValue(yearRule());
        if (year != null) {
            // year-month-day
            DateTimeField moy = merger.getValue(monthOfYearRule());
            DateTimeField dom = merger.getValue(dayOfMonthRule());
            if (moy != null && dom != null) {
                HistoricDate date = HistoricDate.of(year.getValidIntValue(), MonthOfYear.of(moy.getValidIntValue()), dom.getValidIntValue());
                merger.storeMerged(HistoricDate.rule(), date);
                merger.removeProcessed(yearRule());
                merger.removeProcessed(monthOfYearRule());
                merger.removeProcessed(dayOfMonthRule());
            }
            // year-day
            DateTimeField doy = merger.getValue(dayOfYearRule());
            if (doy != null) {
                HistoricDate date = HistoricDate.of(year.getValidIntValue(), MonthOfYear.JANUARY, 1).withDayOfYear(doy.getValidIntValue());
                merger.storeMerged(HistoricDate.rule(), date);
                merger.removeProcessed(yearRule());
                merger.removeProcessed(dayOfYearRule());
            }
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Rule implementation.
     */
    private static final class EraRule extends DateTimeFieldRule implements Serializable {
        /** A serialization identifier for this class. */
        private static final long serialVersionUID = 1L;
        /** Constructor. */
        private EraRule(HistoricChronology chrono) {
            super(chrono, "Era", periodEras(), null, 0, 1);
        }
        @Override
        protected DateTimeField derive(Calendrical calendrical) {
            HistoricDate cd = calendrical.get(HistoricDate.rule());
            return cd != null ? field(cd.getEra().getValue()) : null;
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Rule implementation.
     */
    private static final class YearRule extends DateTimeFieldRule implements Serializable {
        /** The chronology. */
        private final HistoricChronology chrono;
        /** A serialization identifier for this class. */
        private static final long serialVersionUID = 1L;
        /** Constructor. */
        private YearRule(HistoricChronology chrono) {
            super(chrono, "Year", YEARS, null, -(HistoricDate.MAX_YEAR - 1), HistoricDate.MAX_YEAR);
            this.chrono = chrono;
        }
        @Override
        protected DateTimeField derive(Calendrical calendrical) {
            HistoricDate cd = calendrical.get(HistoricDate.rule());
            return cd != null ? field(cd.getYear()) : null;
        }
        @Override
        protected void merge(CalendricalMerger merger) {
            chrono.merge(merger);
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Rule implementation.
     */
    private static final class MonthOfYearRule extends DateTimeFieldRule implements Serializable {
        /** A serialization identifier for this class. */
        private static final long serialVersionUID = 1L;
        /** Constructor. */
        private MonthOfYearRule(HistoricChronology chrono) {
            super(chrono, "MonthOfYear", MONTHS, YEARS, 1, 13);
        }
        @Override
        protected DateTimeField derive(Calendrical calendrical) {
            HistoricDate cd = calendrical.get(HistoricDate.rule());
            return cd != null ? field(cd.getMonthOfYear().getValue()) : null;
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Rule implementation.
     */
    private static final class DayOfMonthRule extends DateTimeFieldRule implements Serializable {
        /** The chronology. */
        private final HistoricChronology chrono;
        /** A serialization identifier for this class. */
        private static final long serialVersionUID = 1L;
        /** Constructor. */
        private DayOfMonthRule(HistoricChronology chrono) {
            super(chrono, "DayOfMonth", periodDays(), MONTHS, 1, 30);
            this.chrono = chrono;
        }
        @Override
        public long getSmallestMaximumValue() {
            return 28;
        }
        @Override
        public long getMaximumValue(Calendrical calendrical) {
            DateTimeField year = calendrical.get(chrono.yearRule());
            DateTimeField moy = calendrical.get(chrono.monthOfYearRule());
            if (moy != null) {
                if (year != null) {
                    return MonthOfYear.of(moy.getValidIntValue()).lengthInDays(chrono.isLeapYear(year.getValidIntValue()));
                } else {
                    return MonthOfYear.of(moy.getValidIntValue()).maxLengthInDays();
                }
            }
            return getMaximumValue();
        }
        @Override
        protected DateTimeField derive(Calendrical calendrical) {
            HistoricDate cd = calendrical.get(HistoricDate.rule());
            return cd != null ? field(cd.getDayOfMonth()) : null;
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Rule implementation.
     */
    private static final class DayOfYearRule extends DateTimeFieldRule implements Serializable {
        /** The chronology. */
        private final HistoricChronology chrono;
        /** A serialization identifier for this class. */
        private static final long serialVersionUID = 1L;
        /** Constructor. */
        private DayOfYearRule(HistoricChronology chrono) {
            super(chrono, "DayOfYear", periodDays(), YEARS, 1, 366);
            this.chrono = chrono;
        }
        @Override
        public long getSmallestMaximumValue() {
            return 365;
        }
        @Override
        public long getMaximumValue(Calendrical calendrical) {
            DateTimeField year = calendrical.get(chrono.yearRule());
            if (year != null) {
                return chrono.isLeapYear(year.getValidIntValue()) ? 366 : 365;
            }
            return getMaximumValue();
        }
        @Override
        protected DateTimeField derive(Calendrical calendrical) {
            HistoricDate cd = calendrical.get(HistoricDate.rule());
            return cd != null ? field(cd.getDayOfYear()) : null;
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Rule implementation.
     */
    private static final class DayOfWeekRule extends DateTimeFieldRule implements Serializable {
        /** A serialization identifier for this class. */
        private static final long serialVersionUID = 1L;
        /** Constructor. */
        private DayOfWeekRule(HistoricChronology chrono) {
            super(chrono, "DayOfWeek", periodDays(), periodWeeks(), 1, 7);
        }
        @Override
        protected DateTimeField derive(Calendrical calendrical) {
            HistoricDate cd = calendrical.get(HistoricDate.rule());
            return cd != null ? field(cd.getDayOfWeek().getValue()) : null;
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Period unit for years.
     */
    private static final PeriodUnit YEARS = new Years();
    /**
     * Unit class for years.
     */
    private static final class Years extends PeriodUnit {
        private static final long serialVersionUID = 1L;
        private Years() {
            super("JulianYears", Duration.ofSeconds(31557600L));  // 365.25 days
        }
        private Object readResolve() {
            return YEARS;
        }
    }
    /**
     * Period unit for months.
     */
    private static final PeriodUnit MONTHS = new Months();
    /**
     * Unit class for months.
     */
    private static final class Months extends PeriodUnit {
        private static final long serialVersionUID = 1L;
        private Months() {
            super("JulianMonths", Duration.ofStandardHours(31557600L / 12L));  // 365.25 days / 12
        }
        private Object readResolve() {
            return MONTHS;
        }
    }

}
