package com.salaryflow.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.YearMonth
import java.util.Locale

class TimeTextTest {

    @Test
    fun `month and weekday follow the selected locale`() {
        val month = YearMonth.of(2026, 10)

        assertEquals("October 2026", TimeText.monthYear(month, Locale.ENGLISH))
        assertEquals("2026年10月", TimeText.monthYear(month, Locale.SIMPLIFIED_CHINESE))
        assertEquals("Sat", TimeText.weekday(DayOfWeek.SATURDAY, Locale.ENGLISH))
        assertEquals("周六", TimeText.weekday(DayOfWeek.SATURDAY, Locale.SIMPLIFIED_CHINESE))
    }

    @Test
    fun `compact amounts use locale-specific units`() {
        assertEquals("50.00K", Money.compactYuan(5_000_000L, Locale.ENGLISH))
        assertEquals("5.00万", Money.compactYuan(5_000_000L, Locale.SIMPLIFIED_CHINESE))
    }

    @Test
    fun `relative day labels cover the last three days only`() {
        val today = LocalDate.of(2026, 10, 27)

        assertEquals("今天", TimeText.relativeDayLabel(today, today, Locale.SIMPLIFIED_CHINESE))
        assertEquals(
            "昨天",
            TimeText.relativeDayLabel(today.minusDays(1), today, Locale.SIMPLIFIED_CHINESE),
        )
        assertEquals(
            "前天",
            TimeText.relativeDayLabel(today.minusDays(2), today, Locale.SIMPLIFIED_CHINESE),
        )
        assertNull(TimeText.relativeDayLabel(today.minusDays(3), today, Locale.SIMPLIFIED_CHINESE))
        assertEquals("Today", TimeText.relativeDayLabel(today, today, Locale.ENGLISH))
        assertEquals(
            "Yesterday",
            TimeText.relativeDayLabel(today.minusDays(1), today, Locale.ENGLISH),
        )
        assertEquals(
            "Day before yesterday",
            TimeText.relativeDayLabel(today.minusDays(2), today, Locale.ENGLISH),
        )
    }

    @Test
    fun `transaction timestamps switch from relative labels to dates`() {
        val zone = ZoneId.of("Asia/Shanghai")
        val today = LocalDate.of(2026, 10, 27)
        fun stamp(date: LocalDate, hour: Int, minute: Int): Long =
            LocalDateTime.of(date, java.time.LocalTime.of(hour, minute))
                .atZone(zone)
                .toInstant()
                .toEpochMilli()

        assertEquals(
            "今天 09:05",
            TimeText.transactionTimestamp(stamp(today, 9, 5), today, zone, Locale.SIMPLIFIED_CHINESE),
        )
        assertEquals(
            "昨天 23:59",
            TimeText.transactionTimestamp(
                stamp(today.minusDays(1), 23, 59),
                today,
                zone,
                Locale.SIMPLIFIED_CHINESE,
            ),
        )
        assertEquals(
            "前天 00:01",
            TimeText.transactionTimestamp(
                stamp(today.minusDays(2), 0, 1),
                today,
                zone,
                Locale.SIMPLIFIED_CHINESE,
            ),
        )
        assertEquals(
            "10月24日 12:00",
            TimeText.transactionTimestamp(
                stamp(today.minusDays(3), 12, 0),
                today,
                zone,
                Locale.SIMPLIFIED_CHINESE,
            ),
        )
    }

    @Test
    fun `older timestamps include the year only when it differs`() {
        val zone = ZoneId.of("Asia/Shanghai")
        val today = LocalDate.of(2027, 1, 2)
        val lastYear = LocalDateTime.of(2026, 12, 20, 8, 0).atZone(zone).toInstant().toEpochMilli()

        assertEquals(
            "2026年12月20日 08:00",
            TimeText.transactionTimestamp(lastYear, today, zone, Locale.SIMPLIFIED_CHINESE),
        )
        assertEquals(
            "Dec 20, 2026 08:00",
            TimeText.transactionTimestamp(lastYear, today, zone, Locale.ENGLISH),
        )
        assertEquals(
            "Today 09:05",
            TimeText.transactionTimestamp(
                LocalDateTime.of(2027, 1, 2, 9, 5).atZone(zone).toInstant().toEpochMilli(),
                today,
                zone,
                Locale.ENGLISH,
            ),
        )
    }
}
