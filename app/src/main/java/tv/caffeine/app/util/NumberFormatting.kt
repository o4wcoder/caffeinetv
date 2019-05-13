package tv.caffeine.app.util

import android.icu.text.CompactDecimalFormat
import android.os.Build
import java.util.Locale
import kotlin.math.log10

fun compactNumberFormat(number: Int) = englishCompactNumberFormat(number)

// TODO: for i18n/L10n, we might want to use this version
fun compactNumberFormatWithDecimal(number: Int): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        CompactDecimalFormat.getInstance(Locale.getDefault(), CompactDecimalFormat.CompactStyle.SHORT).format(number)
    } else {
        englishCompactNumberFormat(number)
    }
}

private fun englishCompactNumberFormat(number: Int): String {
    return when (log10(number.toDouble()).toInt()) {
        in 0..2 -> "$number"
        in 3..5 -> "${number / 1000}k"
        in 6..8 -> "${number / 1000000}M"
        in 9..11 -> "${number / 1000000000}B"
        else -> "${number / 1000000000000}T"
    }
}
