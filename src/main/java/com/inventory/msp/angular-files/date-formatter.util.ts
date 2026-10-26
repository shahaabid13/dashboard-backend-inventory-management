/**
 * DateFormatterUtil - Robust date parsing and formatting utility
 *
 * Handles all common date serialization formats from the backend:
 *  - JSON array:        [2025, 6, 17, 14, 28, 46, 956170000]
 *  - Comma string:      "2025,6,17,14,28,46,956170000"
 *  - ISO string:        "2025-06-17T14:28:46.956170000Z"
 *  - Numeric epoch:     1718626126956 (ms, seconds, or nanoseconds)
 *  - Firestore object:  { seconds: 1234567890, nanos: 123456789 }
 *  - JS Date object:    new Date()
 *
 * Usage:
 *   import { DateFormatterUtil } from './date-formatter.util';
 *
 *   // Parse and format in one call
 *   const formatted = DateFormatterUtil.formatDate(request.createdAt);
 *
 *   // Or parse to Date object then use your own formatting
 *   const dateObj = DateFormatterUtil.parseDate(request.createdAt);
 *   if (dateObj) {
 *     console.log('Date:', dateObj.toISOString());
 *   }
 *
 *   // Resolve date from multiple possible field names
 *   const value = DateFormatterUtil.resolveDateField(request, [
 *     'createdAt', 'created_at', 'created', 'timestamp'
 *   ]);
 *   const formatted = DateFormatterUtil.formatDate(value);
 */
export class DateFormatterUtil {
  /**
   * Parse any date-like value into a JS Date object
   * Returns null if parsing fails
   */
  static parseDate(input: any): Date | null {
    if (input === null || input === undefined || input === '') return null;

    // Already a Date
    if (input instanceof Date) return isNaN(input.getTime()) ? null : input;

    // ── JSON ARRAY from Spring Boot LocalDateTime ──────────────────────────────
    // e.g. [2025, 6, 17, 14, 28, 46, 956170000]
    //       yr   mo  day hr  min sec  nanoseconds
    if (Array.isArray(input) && input.length >= 6) {
      const [yr, mo, day, hr = 0, min = 0, sec = 0, nano = 0] = input.map(Number);
      const ms = Math.floor(nano / 1_000_000); // nanoseconds → milliseconds
      // month is 1-based from Java, JS Date expects 0-based
      const d = new Date(yr, mo - 1, day, hr, min, sec, ms);
      return isNaN(d.getTime()) ? null : d;
    }

    // Firestore-like { seconds, nanos } object
    if (typeof input === 'object') {
      const secs = Number(input.seconds ?? input._seconds ?? NaN);
      if (!isNaN(secs)) {
        const nanos = Number(input.nanos ?? input._nanoseconds ?? 0);
        return new Date(secs * 1000 + Math.floor(nanos / 1_000_000));
      }
      return null;
    }

    let s = String(input).trim();
    if (!s) return null;

    // Pure numeric string — epoch seconds, ms, or ns
    if (/^\d+$/.test(s)) {
      const n = BigInt(s);
      let ms: number;
      if (n > 1_000_000_000_000_000_000n) ms = Number(n / 1_000_000n);       // nanoseconds
      else if (n > 1_000_000_000_000n)    ms = Number(n);                     // milliseconds
      else                                ms = Number(n) * 1000;              // seconds
      const d = new Date(ms);
      return isNaN(d.getTime()) ? null : d;
    }

    // Comma-separated string "2025,6,17,14,28,46,956170000"
    if (s.includes(',') && /^\d/.test(s)) {
      const parts = s.split(',').map(p => Number(p.trim()));
      if (parts.length >= 6) {
        const [yr, mo, day, hr = 0, min = 0, sec = 0, nano = 0] = parts;
        const ms = Math.floor(nano / 1_000_000);
        const d = new Date(yr, mo - 1, day, hr, min, sec, ms);
        return isNaN(d.getTime()) ? null : d;
      }
    }

    // ISO / date string — truncate fractional seconds to 3 digits first
    s = s.replace(/(\d{2}:\d{2}:\d{2})\.(\d{3})\d+/, '$1.$2');
    const d = new Date(s);
    return isNaN(d.getTime()) ? null : d;
  }

  /**
   * Format a raw date value to a human-readable string
   * Returns 'N/A' if parsing fails
   *
   * Example output: "Jun 17, 2025 02:28 PM"
   */
  static formatDate(input: any): string {
    const d = this.parseDate(input);
    if (!d) return 'N/A';

    return d.toLocaleDateString('en-US', {
      year: 'numeric', month: 'short', day: 'numeric'
    }) + ' ' + d.toLocaleTimeString('en-US', {
      hour: '2-digit', minute: '2-digit', hour12: true
    });
  }

  /**
   * Format a date to ISO string (e.g., "2025-06-17T14:28:46.956Z")
   */
  static formatDateISO(input: any): string {
    const d = this.parseDate(input);
    return d ? d.toISOString() : 'N/A';
  }

  /**
   * Format a date to local ISO string without Z suffix (e.g., "2025-06-17T14:28:46.956")
   */
  static formatDateLocalISO(input: any): string {
    const d = this.parseDate(input);
    if (!d) return 'N/A';
    return d.toISOString().replace('Z', '');
  }

  /**
   * Format a date to just the date part (e.g., "Jun 17, 2025")
   */
  static formatDateOnly(input: any): string {
    const d = this.parseDate(input);
    if (!d) return 'N/A';
    return d.toLocaleDateString('en-US', {
      year: 'numeric', month: 'short', day: 'numeric'
    });
  }

  /**
   * Format a date to just the time part (e.g., "02:28 PM")
   */
  static formatTimeOnly(input: any): string {
    const d = this.parseDate(input);
    if (!d) return 'N/A';
    return d.toLocaleTimeString('en-US', {
      hour: '2-digit', minute: '2-digit', hour12: true
    });
  }

  /**
   * Resolve a date field from an object by trying multiple candidate keys
   * Also checks one-level nested objects
   *
   * Example:
   *   resolveDateField(request, ['createdAt', 'created_at', 'created'])
   *   → returns the value of the first key that exists
   */
  static resolveDateField(obj: any, keys: string[]): any {
    if (!obj) return null;

    // Check direct keys
    for (const k of keys) {
      if (obj[k] !== undefined && obj[k] !== null) return obj[k];
    }

    // Check one-level nested objects
    for (const prop of Object.keys(obj)) {
      const val = obj[prop];
      if (val && typeof val === 'object' && !Array.isArray(val)) {
        for (const k of keys) {
          if (val[k] !== undefined && val[k] !== null) return val[k];
        }
      }
    }

    return null;
  }

  /**
   * Enrich an object with formatted date/time fields
   * Useful for components that need pre-formatted display values
   *
   * Example:
   *   requests = requests.map(r =>
   *     DateFormatterUtil.enrichWithFormattedDates(r, ['createdAt', 'updatedAt'])
   *   );
   *
   *   // Now r has: r.formattedCreatedAt, r.formattedUpdatedAt
   */
  static enrichWithFormattedDates<T extends Record<string, any>>(
    obj: T,
    dateFields: string[]
  ): T & Record<string, any> {
    const enriched = { ...obj };
    for (const field of dateFields) {
      const value = this.resolveDateField(obj, [field]);
      enriched[`formatted${field.charAt(0).toUpperCase()}${field.slice(1)}`] = this.formatDate(value);
    }
    return enriched;
  }
}

