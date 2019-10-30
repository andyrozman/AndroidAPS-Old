package info.nightscout.androidaps.database

import info.nightscout.androidaps.database.embedments.InterfaceIDs
import info.nightscout.androidaps.database.entities.*
import info.nightscout.androidaps.database.transactions.MergedBolus
import info.nightscout.androidaps.database.transactions.Transaction

@Deprecated(
        message = "This class only adds support for blocking calls while migrating to reactive application design. Avoid using it.",
        replaceWith = ReplaceWith("AppRepository", "info.nightscout.androidaps.database.AppRepository")
)
object BlockingAppRepository {

    fun getLastGlucoseValue(): GlucoseValue? = AppRepository.getLastGlucoseValue().blockingGet()

    fun getLastGlucoseValueIfRecent(): GlucoseValue? = AppRepository.getLastGlucoseValueIfRecent().blockingGet()

    fun getGlucoseValuesInTimeRange(start: Long, end: Long): List<GlucoseValue> = AppRepository.getGlucoseValuesInTimeRange(start, end).blockingGet()

    fun getGlucoseValuesInTimeRangeIf39OrHigher(start: Long, end: Long): List<GlucoseValue> = AppRepository.getGlucoseValuesInTimeRangeIf39OrHigher(start, end).blockingGet()

    fun <T> runTransaction(transaction: Transaction<T>) = AppRepository.runTransaction(transaction).blockingAwait()

    fun <T> runTransactionForResult(transaction: Transaction<T>): T = AppRepository.runTransactionForResult(transaction).blockingGet()

    fun getTemporaryBasalsInTimeRange(start: Long, end: Long): List<TemporaryBasal> = AppRepository.getTemporaryBasalsInTimeRange(start, end).blockingFirst()

    fun getExtendedBolusesInTimeRange(start: Long, end: Long): List<ExtendedBolus> = AppRepository.getExtendedBolusesInTimeRange(start, end).blockingFirst()

    fun getTemporaryTargetsInTimeRange(start: Long, end: Long): List<TemporaryTarget> = AppRepository.getTemporaryTargetsInTimeRange(start, end).blockingFirst()

    fun getTotalDailyDoses(amount: Int): List<TotalDailyDose> = AppRepository.getTotalDailyDoses(amount).blockingGet()

    fun getMergedBolusData(start: Long, end: Long): List<MergedBolus> = AppRepository.getMergedBolusData(start, end).blockingGet()

    fun getLastTherapyEventByType(type: TherapyEvent.Type): TherapyEvent? = AppRepository.getLastTherapyEventByType(type).blockingGet()

    fun getTherapyEventsInTimeRange(start: Long, end: Long): List<TherapyEvent> = AppRepository.getTherapyEventsInTimeRange(start, end).blockingFirst()

    fun getTherapyEventsInTimeRange(type: TherapyEvent.Type, start: Long, end: Long): List<TherapyEvent> = AppRepository.getTherapyEventsInTimeRange(type, start, end).blockingFirst()

    fun getAllTherapyEvents(): List<TherapyEvent> = AppRepository.getAllTherapyEvents().blockingFirst()

    fun getProfileSwitchesInTimeRange(start: Long, end: Long): List<ProfileSwitch> = AppRepository.getProfileSwitchesInTimeRange(start, end).blockingFirst()

    fun getAllProfileSwitches(): List<ProfileSwitch> = AppRepository.getAllProfileSwitches().blockingFirst()

    fun getTemporaryBasalActiveAtIncludingInvalid(timestamp: Long, pumpType: InterfaceIDs.PumpType): TemporaryBasal? = AppRepository.getTemporaryBasalActiveAtIncludingInvalidMaybe(timestamp, pumpType).blockingGet()
}