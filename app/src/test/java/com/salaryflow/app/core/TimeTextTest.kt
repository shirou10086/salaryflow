package com.salaryflow.app.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
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
}
