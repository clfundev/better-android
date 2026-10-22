package io.github.clfundev.better.data.db.converter

import androidx.room.TypeConverter

class BooleanConverter {
    @TypeConverter
    fun booleanToInt(b: Boolean): Int {
        return if (b) 1 else 0
    }

    @TypeConverter
    fun intToBoolean(i: Int): Boolean {
        return i != 0
    }
}
