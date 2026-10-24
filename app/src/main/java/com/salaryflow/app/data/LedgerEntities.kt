package com.salaryflow.app.data

import androidx.room.TypeConverter

enum class TxType {
    EXPENSE,
    INCOME,
    SAVING_IN,
    SAVING_OUT,
}

data class Category(
    val key: String,
    val name: String,
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
        Category("food", "餐饮"),
        Category("transport", "交通"),
        Category("shopping", "购物"),
        Category("housing", "居住"),
        Category("fun", "娱乐"),
        Category("health", "医疗"),
        Category("study", "学习"),
        Category("social", "人情"),
        Category("other", "其他"),
    )

    val incomes = listOf(
        Category("salary", "工资"),
        Category("bonus", "奖金"),
        Category("invest", "理财"),
        Category("side", "副业"),
        Category("other_income", "其他"),
    )

    val savings = listOf(
        Category("auto_saving", "自动寄存"),
        Category("manual_saving", "手动存入"),
        Category("saving_out", "取出"),
    )

    private val all = expenses + incomes + savings

    fun find(key: String): Category = all.firstOrNull { it.key == key } ?: Category(key, key)
}
