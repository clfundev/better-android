package io.github.clfundev.better.widget

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import io.github.clfundev.better.R
import io.github.clfundev.better.occur.RecurRule

class FreqWheelPicker(context: Context, attrs: AttributeSet?) :
    WheelPicker<String>(context, attrs) {

    private val dataSet = listOf(
        RecurRule.DAILY,
        RecurRule.WEEKLY,
        RecurRule.MONTHLY,
        RecurRule.YEARLY
    )

    private var quantity = 0

    init {
        setMaxWidthText("WWWWW")
        setItemFormatter {
            when (it) {
                RecurRule.DAILY -> resources.getQuantityString(R.plurals.unit_day, quantity)
                RecurRule.WEEKLY -> resources.getQuantityString(R.plurals.unit_week, quantity)
                RecurRule.MONTHLY -> resources.getQuantityString(R.plurals.unit_month, quantity)
                RecurRule.YEARLY -> resources.getQuantityString(R.plurals.unit_year, quantity)
                else -> ""
            }
        }
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

    override fun getItem(index: Int): String {
        if (index < 0 || index >= getItemCount()) {
            throw IndexOutOfBoundsException("Index out of bounds: index = $index, size = ${getItemCount()}")
        }
        return dataSet[index]
    }

    override fun indexOf(item: String): Int {
        return dataSet.indexOf(item)
    }

    fun getQuantity(): Int {
        return quantity
    }

    fun setQuantity(quantity: Int) {
        this.quantity = quantity
        invalidate()
    }

    private class SavedState : BaseSavedState {
        var quantity: Int = 0

        constructor(superState: Parcelable?) : super(superState)

        private constructor(source: Parcel) : super(source) {
            quantity = source.readInt()
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeInt(quantity)
        }

        companion object {
            @JvmField
            val CREATOR = object : Parcelable.Creator<SavedState> {
                override fun createFromParcel(source: Parcel): SavedState {
                    return SavedState(source)
                }

                override fun newArray(size: Int): Array<SavedState?> {
                    return arrayOfNulls(size)
                }
            }
        }
    }

    override fun onSaveInstanceState(): Parcelable {
        val savedState = SavedState(super.onSaveInstanceState())
        savedState.quantity = quantity
        return savedState
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is SavedState) {
            super.onRestoreInstanceState(state.superState)
            setQuantity(state.quantity)
        } else {
            super.onRestoreInstanceState(state)
        }
    }
}
