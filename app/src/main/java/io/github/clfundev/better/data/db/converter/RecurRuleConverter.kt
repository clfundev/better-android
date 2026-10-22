package io.github.clfundev.better.data.db.converter

import androidx.room.TypeConverter
import io.github.clfundev.better.occur.RecurRule

class RecurRuleConverter {
    @TypeConverter
    fun stringToRecurRule(string: String?): RecurRule? {
        return if (string != null) RecurRule.ofPattern(string) else null
    }

    @TypeConverter
    fun recurRuleToString(recurRule: RecurRule?): String? {
        return recurRule?.toPattern()
    }
}
