package no.simula.corona.data

import java.text.SimpleDateFormat
import java.util.*

class AgeVerifier {
    private val mToday = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    private var mSelected: Calendar? = null

    fun select(year: Int, month: Int, day: Int) {
        mSelected = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(year, month, day)
        }
    }

    fun hasSelection(): Boolean = mSelected != null
    fun year(): Int = mSelected?.get(Calendar.YEAR) ?: 0
    fun month(): Int = mSelected?.get(Calendar.MONTH) ?: 0
    fun day(): Int = mSelected?.get(Calendar.DAY_OF_MONTH) ?: 0

    fun isOldEnough(): Boolean {
        val diff = mToday.timeInMillis - mSelected!!.timeInMillis.toDouble()
        val years = diff / (1000 * 60 * 60) / (24 * 365)

        return years >= 16
    }

    fun formatDate(): String {
        val df = SimpleDateFormat("dd.MM.yyyy", Locale.US)
        return df.format(mSelected!!.time)
    }

    companion object {
        fun parse(date: String) {

        }
    }
}