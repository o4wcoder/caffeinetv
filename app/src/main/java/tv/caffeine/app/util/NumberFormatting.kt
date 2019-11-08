package tv.caffeine.app.util

import android.icu.text.CompactDecimalFormat
import android.os.Build
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.log10

fun compactThousandsNumberFormat(number: Int) = format(number, true)

fun longFormThousandsNumberFormat(number: Int) = format(number, false)

private fun format(number: Int, isCompactThousands: Boolean): String =
    when {
        number == 0 -> "$number"
        (log10(number.toDouble()).toInt() == 3 && !isCompactThousands) -> NumberFormat.getInstance().format(number)
        else -> compactNumberFormatWithDecimal(number)
    }

private fun compactNumberFormatWithDecimal(number: Int): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        val formatter = CompactDecimalFormat.getInstance(Locale.getDefault(), CompactDecimalFormat.CompactStyle.SHORT)
        formatter.roundingMode = RoundingMode.DOWN.ordinal
        formatter.maximumFractionDigits = 1
        formatter.format(number)
    } else {
        englishCompactNumberFormatWithDecimal(number)
    }
}

fun englishCompactNumberFormatWithDecimal(number: Int): String {
    return when (number) {
        0 -> "$number"
        else ->
            when (log10(number.toDouble()).toInt()) {
            in 0..2 -> NumberFormat.getInstance().format(number)
            in 3..5 -> "${doFormat(number / 1000.0)}K"
            in 6..8 -> "${doFormat(number / 1000000.0)}M"
            in 9..11 -> "${number / 1000000000}B"
            else -> "${number / 1000000000000}T"
        }
    }
}

private fun doFormat(number: Double): String {
    val formatter = NumberFormat.getInstance()
    formatter.maximumFractionDigits = 1
    formatter.roundingMode = RoundingMode.DOWN
    return formatter.format(number)
}