package info.nightscout.androidaps.database.transactions.medtronic

import org.joda.time.LocalDateTime
import org.joda.time.Minutes
import org.slf4j.LoggerFactory
import java.util.*

class MedtronicUtilKotlin {

    val LOG = LoggerFactory.getLogger("PUMP_COMM")

    fun isSameDayATDAndMillis(atechDateTime: Long, timeInMillis: Long): Boolean {

        val dt = GregorianCalendar()
        dt.timeInMillis = timeInMillis

        val entryDate = toATechDate(dt)

        return isSameDay(atechDateTime, entryDate)
    }

    fun toATechDate(gc: GregorianCalendar): Long {
        var atechDateTime = 0L

        atechDateTime += gc.get(Calendar.YEAR) * 10000000000L
        atechDateTime += (gc.get(Calendar.MONTH) + 1) * 100000000L
        atechDateTime += gc.get(Calendar.DAY_OF_MONTH) * 1000000L
        atechDateTime += gc.get(Calendar.HOUR_OF_DAY) * 10000L
        atechDateTime += gc.get(Calendar.MINUTE) * 100L
        atechDateTime += gc.get(Calendar.SECOND).toLong()

        return atechDateTime
    }


    fun isSameDay(ldt1: Long, ldt2: Long): Boolean {

        val day1 = ldt1 / 10000L
        val day2 = ldt2 / 10000L

        return day1 == day2
    }

    fun toMillisFromATD(atechDateTime: Long): Long {

        val gc = toGregorianCalendar(atechDateTime)

        return gc.getTimeInMillis()
    }


    /**
     * DateTime is packed as long: yyyymmddHHMMss
     *
     * @param atechDateTime
     * @return
     */
    fun toGregorianCalendar(atechDateTime: Long): GregorianCalendar {
        var atechDateTime = atechDateTime
        val year = (atechDateTime / 10000000000L).toInt()
        atechDateTime -= year * 10000000000L

        val month = (atechDateTime / 100000000L).toInt()
        atechDateTime -= month * 100000000L

        val dayOfMonth = (atechDateTime / 1000000L).toInt()
        atechDateTime -= dayOfMonth * 1000000L

        val hourOfDay = (atechDateTime / 10000L).toInt()
        atechDateTime -= hourOfDay * 10000L

        val minute = (atechDateTime / 100L).toInt()
        atechDateTime -= minute * 100L

        val second = atechDateTime.toInt()

        try {
            return GregorianCalendar(year, month - 1, dayOfMonth, hourOfDay, minute, second)
        } catch (ex: Exception) {
            LOG.error("DateTimeUtil", String.format("Error creating GregorianCalendar from values [atechDateTime=%d, year=%d, month=%d, day=%d, hour=%d, minute=%d, second=%d]", atechDateTime, year, month, dayOfMonth, hourOfDay, minute, second))
            throw ex
        }

    }


    fun getATechDateDiferenceAsMinutes(date1: Long?, date2: Long?): Int {

        val minutes = Minutes.minutesBetween(toLocalDateTime(date1!!), toLocalDateTime(date2!!))

        return minutes.getMinutes()
    }


    fun isSame(d1: Double?, d2: Double?): Boolean {
        val diff = d1!! - d2!!

        return Math.abs(diff) <= 0.000001
    }


    fun toLocalDateTime(atechDateTime: Long): LocalDateTime {
        var atechDateTime = atechDateTime
        val year = (atechDateTime / 10000000000L).toInt()
        atechDateTime -= year * 10000000000L

        val month = (atechDateTime / 100000000L).toInt()
        atechDateTime -= month * 100000000L

        val dayOfMonth = (atechDateTime / 1000000L).toInt()
        atechDateTime -= dayOfMonth * 1000000L

        val hourOfDay = (atechDateTime / 10000L).toInt()
        atechDateTime -= hourOfDay * 10000L

        val minute = (atechDateTime / 100L).toInt()
        atechDateTime -= minute * 100L

        val second = atechDateTime.toInt()

        try {
            return LocalDateTime(year, month, dayOfMonth, hourOfDay, minute, second)
        } catch (ex: Exception) {
            LOG.error("Error creating LocalDateTime from values [atechDateTime={}, year={}, month={}, day={}, hour={}, minute={}, second={}]. Exception: {}", atechDateTime, year, month, dayOfMonth, hourOfDay, minute, second, ex.message)
            //return null;
            throw ex
        }




    }


}