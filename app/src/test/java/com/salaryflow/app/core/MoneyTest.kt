package com.salaryflow.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {

    @Test
    fun `yuan input is converted without binary floating point drift`() {
        assertEquals(3_580L, Money.yuanToCents("35.80"))
        assertEquals(3_581L, Money.yuanToCents("35.81"))
        assertEquals(1L, Money.yuanToCents("0.01"))
        assertEquals(10_000L, Money.yuanToCents("100"))
        assertEquals(123_456L, Money.yuanToCents("1,234.56"))
        assertEquals(10_000L, Money.yuanToCents("¥100"))
    }

    @Test
    fun `fractional cents round half up`() {
        assertEquals(101L, Money.yuanToCents("1.005"))
        assertEquals(100L, Money.yuanToCents("1.0049"))
        assertEquals(100L, Money.yuanToCents("1.0044"))
    }

    @Test
    fun `invalid and overflowing yuan inputs are rejected`() {
        assertNull(Money.yuanToCents(""))
        assertNull(Money.yuanToCents("abc"))
        assertNull(Money.yuanToCents("-0.01"))
        assertNull(Money.yuanToCents("1e100"))
    }
}
