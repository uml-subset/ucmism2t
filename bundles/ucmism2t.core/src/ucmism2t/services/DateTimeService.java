package ucmism2t.services;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import org.eclipse.emf.ecore.EObject;

/**
 * Date and Time Service for Acceleo Templates
 * 
 * Provides ISO 8601 formatted date/time strings for use in generated
 * code, documentation headers, timestamps, etc.
 * 
 * ISO 8601 Format: YYYY-MM-DDThh:mm:ss
 * Example: 2025-02-13T14:30:45
 * 
 * Usage in Acceleo templates:
 *   when registered in Java code
 *   [aModel.getCurrentDateTime/]
 * 
 *   with [import ucmism2t::services::DateTimeService /] im mtl files
 *   [aModel.getCurrentDateTime()/]
 * 
 * @author ucmism2t generator
 * @version 1.0.0
 * @since Java 21
 */
public class DateTimeService {
    
    /**
     * ISO 8601 date-time formatter without timezone.
     * Format: YYYY-MM-DDThh:mm:ss
     * Example: 2025-02-13T14:30:45
     */
    private static final DateTimeFormatter ISO_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    
    /**
     * ISO 8601 date-time formatter with timezone.
     * Format: YYYY-MM-DDThh:mm:ss+HH:MM
     * Example: 2025-02-13T14:30:45+01:00
     */
    private static final DateTimeFormatter ISO_FORMATTER_WITH_ZONE = 
        DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    
    /**
     * Returns the current date and time in ISO 8601 format (local time).
     * 
     * This uses the system's default timezone.
     * 
     * Example in Acceleo template:
     *   // Generated on [model.getCurrentDateTime/]
     * 
     * Output example:
     *   // Generated on 2025-02-13T14:30:45
     * 
     * @param context The model element (required by Acceleo, but not used)
     * @return Current date/time as ISO 8601 string (local timezone)
     */
//    public String getCurrentDateTime(Object object) {
      public String getCurrentDateTime(EObject context) {
        return LocalDateTime.now().format(ISO_FORMATTER);
    }
    
    /**
     * Returns the current date and time in ISO 8601 format (UTC).
     * 
     * This always returns UTC time, regardless of system timezone.
     * Useful for timestamps that need to be timezone-independent.
     * 
     * Example in Acceleo template:
     *   // Generated on [model.getCurrentDateTimeUTC/] UTC
     * 
     * Output example:
     *   // Generated on 2025-02-13T13:30:45 UTC
     * 
     * @param context The model element (required by Acceleo, but not used)
     * @return Current date/time as ISO 8601 string in UTC
     */
    public String getCurrentDateTimeUTC(EObject context) {
        return ZonedDateTime.now(ZoneId.of("UTC")).format(ISO_FORMATTER);
    }
    
    /**
     * Returns the current date and time with timezone offset.
     * 
     * Format: YYYY-MM-DDThh:mm:ss+HH:MM
     * Example: 2025-02-13T14:30:45+01:00
     * 
     * Useful when timezone information needs to be preserved.
     * 
     * @param context The model element (required by Acceleo, but not used)
     * @return Current date/time as ISO 8601 string with timezone
     */
    public String getCurrentDateTimeWithZone(EObject context) {
        return ZonedDateTime.now().format(ISO_FORMATTER_WITH_ZONE);
    }
    
    /**
     * Returns the current date in ISO 8601 format (YYYY-MM-DD).
     * 
     * Example output: 2025-02-13
     * 
     * @param context The model element (required by Acceleo, but not used)
     * @return Current date as ISO 8601 string
     */
    public String getCurrentDate(EObject context) {
        return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
    }
    
    /**
     * Returns the current time in ISO 8601 format (HH:mm:ss).
     * 
     * Example output: 14:30:45
     * 
     * @param context The model element (required by Acceleo, but not used)
     * @return Current time as ISO 8601 string
     */
    public String getCurrentTime(EObject context) {
        return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME);
    }
    
    /**
     * Returns the current year as a string.
     * 
     * Useful for copyright notices: © 2025 Company Name
     * 
     * Example in Acceleo template:
     *   // Copyright (c) [model.getCurrentYear/] Example Corp
     * 
     * @param context The model element (required by Acceleo, but not used)
     * @return Current year as string (e.g., "2025")
     */
    public String getCurrentYear(EObject context) {
        return String.valueOf(LocalDateTime.now().getYear());
    }
    
    /**
     * Formats a custom date-time string with specified pattern.
     * 
     * Pattern examples:
     *   "yyyy-MM-dd" -> 2025-02-13
     *   "dd/MM/yyyy HH:mm" -> 13/02/2025 14:30
     *   "EEEE, MMMM d, yyyy" -> Thursday, February 13, 2025
     * 
     * Example in Acceleo template:
     *   [model.formatCurrentDateTime('yyyy-MM-dd')/]
     * 
     * @param context The model element (required by Acceleo, but not used)
     * @param pattern DateTimeFormatter pattern
     * @return Formatted current date/time
     * @throws IllegalArgumentException If pattern is invalid
     */
    public String formatCurrentDateTime(EObject context, String pattern) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            return LocalDateTime.now().format(formatter);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid date/time pattern: " + pattern, e);
        }
    }
}
