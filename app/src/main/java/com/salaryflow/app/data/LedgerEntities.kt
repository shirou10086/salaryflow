package com.salaryflow.app.data

import androidx.room.TypeConverter
import com.salaryflow.app.R

const val AUTO_SALARY_NOTE = "note:auto_salary_settlement"
const val BALANCE_UP_NOTE = "note:balance_up"
const val BALANCE_DOWN_NOTE = "note:balance_down"

enum class TxType {
    EXPENSE,
    INCOME,
    SAVING_IN,
    SAVING_OUT,
}

data class Category(
    val key: String,
    val nameRes: Int,
)

data class CategoryTotal(
    val category: String,
    val totalCents: Long,
)

class Converters {
    @TypeConverter
    fun fromTxType(type: TxType): String = type.name

    @TypeConverter
    fun toTxType(value: String): TxType = TxType.valueOf(value)
}

object Categories {
    val expenses = listOf(
        Category("food", R.string.category_food),
        Category("transport", R.string.category_transport),
        Category("shopping", R.string.category_shopping),
        Category("housing", R.string.category_housing),
        Category("fun", R.string.category_fun),
        Category("health", R.string.category_health),
        Category("study", R.string.category_study),
        Category("social", R.string.category_social),
        Category("other", R.string.category_other),
    )

    val incomes = listOf(
        Category("salary", R.string.category_salary),
        Category("bonus", R.string.category_bonus),
        Category("invest", R.string.category_invest),
        Category("side", R.string.category_side),
        Category("other_income", R.string.category_other_income),
    )

    val savings = listOf(
        Category("auto_saving", R.string.category_auto_saving),
        Category("manual_saving", R.string.category_manual_saving),
        Category("saving_out", R.string.category_saving_out),
        Category(BALANCE_CALIBRATION_CATEGORY, R.string.category_balance_calibration),
    )

    private val all = expenses + incomes + savings

    fun find(key: String): Category =
        all.firstOrNull { it.key == key } ?: Category(key, R.string.category_other)
}

fun TransactionEntity.balanceDeltaCents(): Long = when {
    type == TxType.INCOME -> amountCents
    type == TxType.EXPENSE -> -amountCents
    type == TxType.SAVING_IN && category == BALANCE_CALIBRATION_CATEGORY -> amountCents
    type == TxType.SAVING_OUT && category == BALANCE_CALIBRATION_CATEGORY -> -amountCents
    else -> 0L
}
