package info.nightscout.androidaps.database.transactions

import androidx.room.Embedded
import info.nightscout.androidaps.database.Block
import info.nightscout.androidaps.database.TargetBlock
import info.nightscout.androidaps.database.embedments.InsulinConfiguration
import info.nightscout.androidaps.database.entities.ProfileSwitch
import java.util.*

/**
 * Inserts a ProfileSwitch to the database
 */
class InsertProfileSwitchTransaction(
        var timestamp: Long,
        var profileName: String,
        var glucoseUnit: ProfileSwitch.GlucoseUnit,
        var basalBlocks: List<Block>,
        var isfBlocks: List<Block>,
        var icBlocks: List<Block>,
        var targetBlocks: List<TargetBlock>,
        @Embedded
        var insulinConfiguration: InsulinConfiguration,
        var timeshift: Int,
        var percentage: Int,
        var duration: Long
) : Transaction<Unit>() {
    override fun run() {
        database.profileSwitchDao.insertNewEntry(ProfileSwitch(
                timestamp = timestamp,
                utcOffset = TimeZone.getDefault().getOffset(timestamp).toLong(),
                glucoseUnit = glucoseUnit,
                basalBlocks = basalBlocks,
                isfBlocks = isfBlocks,
                icBlocks = icBlocks,
                targetBlocks = targetBlocks,
                insulinConfiguration = insulinConfiguration,
                timeshift = timeshift,
                percentage = percentage,
                duration = duration,
                profileName = profileName
        ))
    }
}