/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */
package cn.jmicro.api.utils;

import java.lang.ref.SoftReference;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import cn.jmicro.common.CommonException;

/**
* A utility class for parsing and formatting HTTP dates as used in cookies and
* other headers.  This class handles dates as defined by RFC 2616 section
* 3.3.1 as well as some other common non-standard formats.
*
* @since 4.3
*/
public final class DateUtils {
	
	 public static final long DAY_LONG = 24*60*59*1000;

	 public static final String PATTERN_YYYY_MM_DD_HHMMSSSSST = "yyyy-MM-dd HH:mm:ss.SSS'T'";
	 
	 public static final String PATTERN_YYYY_MM_DDTHHMMSSSSST = "yyyy-MM-dd'T'HH:mm:ss.SSS";
	
	 public static final String PATTERN_YYYY_MM_DD_HHMMSSZZZ = "yyyy-MM-dd HH:mm:ss SSS";
	 
	 public static final String PATTERN_YYYY_MM_DD_HHMMSS = "yyyy-MM-dd HH:mm:ss";
	 
	 public static final String PATTERN_YYYY_MM_DD_HHMM = "yyyy-MM-dd HH:mm";
	 
	 public static final String PATTERN_YYYY_MM_DD_HH = "yyyy-MM-dd HH";
	 
	 public static final String PATTERN_YYYY_MM_DD = "yyyy-MM-dd";
	 
	 public static final String PATTERN_HHMMSS = "HH:mm:ss";
	 
	 public static final String PATTERN_HHMMSSZZZ = "HH:mm:ss SSS";
	 
	 public static final String PATTERN_YYYYMMDD = "yyyyMMdd";
	 
   /**
    * Date format pattern used to parse HTTP date headers in RFC 1123 format.
    */
   public static final String PATTERN_RFC1123 = "EEE, dd MMM yyyy HH:mm:ss zzz";

   /**
    * Date format pattern used to parse HTTP date headers in RFC 1036 format.
    */
   public static final String PATTERN_RFC1036 = "EEE, dd-MMM-yy HH:mm:ss zzz";

   /**
    * Date format pattern used to parse HTTP date headers in ANSI C
    * {@code asctime()} format.
    */
   public static final String PATTERN_ASCTIME = "EEE MMM d HH:mm:ss yyyy";

   private static final String[] DEFAULT_PATTERNS = new String[] {
       PATTERN_RFC1123,
       PATTERN_RFC1036,
       PATTERN_ASCTIME
   };

   private static final Date DEFAULT_TWO_DIGIT_YEAR_START;

   public static final TimeZone GMT = TimeZone.getTimeZone("Asia/Shanghai");

   static {
       final Calendar calendar = Calendar.getInstance();
       calendar.setTimeZone(GMT);
       calendar.set(2000, Calendar.JANUARY, 1, 0, 0, 0);
       calendar.set(Calendar.MILLISECOND, 0);
       DEFAULT_TWO_DIGIT_YEAR_START = calendar.getTime();
   }

   /**
    * Parses a date value.  The formats used for parsing the date value are retrieved from
    * the default http params.
    *
    * @param dateValue the date value to parse
    *
    * @return the parsed date or null if input could not be parsed
    */
   public static Date parseDate(final String dateValue) {
       return parseDate(dateValue, null, null);
   }

   /**
    * Parses the date value using the given date formats.
    *
    * @param dateValue the date value to parse
    * @param dateFormats the date formats to use
    *
    * @return the parsed date or null if input could not be parsed
    */
   public static Date parseDate(final String dateValue, final String[] dateFormats) {
       return parseDate(dateValue, dateFormats, null);
   }
   
   public static Date parseDate(final String dateValue, final String dateFormats) {
       try {
		return dateFormat(dateFormats).parse(dateValue);
	} catch (ParseException e) {
		throw new CommonException(dateValue+" with pattern "+dateFormats);
	}
   }
   
   private static DateFormat dateFormat(String pattern) {
	   DateFormat dateFormat = new SimpleDateFormat(pattern);
	   dateFormat.setLenient(false);
	   return dateFormat;
   }
   
  

   /**
    * Parses the date value using the given date formats.
    *
    * @param dateValue the date value to parse
    * @param dateFormats the date formats to use
    * @param startDate During parsing, two digit years will be placed in the range
    * {@code startDate} to {@code startDate + 100 years}. This value may
    * be {@code null}. When {@code null} is given as a parameter, year
    * {@code 2000} will be used.
    *
    * @return the parsed date or null if input could not be parsed
    */
   public static Date parseDate(
           final String dateValue,
           final String[] dateFormats,
           final Date startDate) {
       //Asserts.notNull(dateValue, "Date value");
       final String[] localDateFormats = dateFormats != null ? dateFormats : DEFAULT_PATTERNS;
       final Date localStartDate = startDate != null ? startDate : DEFAULT_TWO_DIGIT_YEAR_START;
       String v = dateValue;
       // trim single quotes around date if present
       // see issue #5279
       if (v.length() > 1 && v.startsWith("'") && v.endsWith("'")) {
           v = v.substring (1, v.length() - 1);
       }

       for (final String dateFormat : localDateFormats) {
           final SimpleDateFormat dateParser = DateFormatHolder.formatFor(dateFormat);
           dateParser.set2DigitYearStart(localStartDate);
           final ParsePosition pos = new ParsePosition(0);
           final Date result = dateParser.parse(v, pos);
           if (pos.getIndex() != 0) {
               return result;
           }
       }
       return null;
   }

   /**
    * Formats the given date according to the RFC 1123 pattern.
    *
    * @param date The date to format.
    * @return An RFC 1123 formatted date string.
    *
    * @see #PATTERN_RFC1123
    */
   public static String formatDate(final Date date) {
       return formatDate(date, PATTERN_RFC1123);
   }

   /**
    * Formats the given date according to the specified pattern.  The pattern
    * must conform to that used by the {@link SimpleDateFormat simple date
    * format} class.
    *
    * @param date The date to format.
    * @param pattern The pattern to use for formatting the date.
    * @return A formatted date string.
    *
    * @throws IllegalArgumentException If the given date pattern is invalid.
    *
    * @see SimpleDateFormat
    */
   public static String formatDate(final Date date, final String pattern) {
       //Args.notNull(date, "Date");
       //Args.notNull(pattern, "Pattern");
       final SimpleDateFormat formatter = DateFormatHolder.formatFor(pattern);
       return formatter.format(date);
   }
   
   public static String formatDate(final long date, final String pattern) {
       return formatDate(new Date(date),pattern);
   }

   /**
    * Clears thread-local variable containing {@link java.text.DateFormat} cache.
    *
    * @since 4.3
    */
   public static void clearThreadLocal() {
       DateFormatHolder.clearThreadLocal();
   }

   /** This class should not be instantiated. */
   private DateUtils() {
   }

   /**
    * A factory for {@link SimpleDateFormat}s. The instances are stored in a
    * threadlocal way because SimpleDateFormat is not threadsafe as noted in
    * {@link SimpleDateFormat its javadoc}.
    *
    */
   final static class DateFormatHolder {

       private static final ThreadLocal<SoftReference<Map<String, SimpleDateFormat>>>
           THREADLOCAL_FORMATS = new ThreadLocal<SoftReference<Map<String, SimpleDateFormat>>>() {

           @Override
           protected SoftReference<Map<String, SimpleDateFormat>> initialValue() {
               return new SoftReference<Map<String, SimpleDateFormat>>(
                       new HashMap<String, SimpleDateFormat>());
           }

       };

       /**
        * creates a {@link SimpleDateFormat} for the requested format string.
        *
        * @param pattern
        *            a non-{@code null} format String according to
        *            {@link SimpleDateFormat}. The format is not checked against
        *            {@code null} since all paths go through
        *            {@link DateUtils}.
        * @return the requested format. This simple dateformat should not be used
        *         to {@link SimpleDateFormat#applyPattern(String) apply} to a
        *         different pattern.
        */
       public static SimpleDateFormat formatFor(final String pattern) {
           final SoftReference<Map<String, SimpleDateFormat>> ref = THREADLOCAL_FORMATS.get();
           Map<String, SimpleDateFormat> formats = ref.get();
           if (formats == null) {
               formats = new HashMap<String, SimpleDateFormat>();
               THREADLOCAL_FORMATS.set(
                       new SoftReference<Map<String, SimpleDateFormat>>(formats));
           }

           SimpleDateFormat format = formats.get(pattern);
           if (format == null) {
               format = new SimpleDateFormat(pattern, Locale.CHINA);
               format.setTimeZone(GMT);
               formats.put(pattern, format);
           }

           return format;
       }

       public static void clearThreadLocal() {
           THREADLOCAL_FORMATS.remove();
       }

   }

}

