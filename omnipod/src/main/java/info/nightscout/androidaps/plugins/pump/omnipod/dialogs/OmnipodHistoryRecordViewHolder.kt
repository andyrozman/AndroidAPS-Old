package info.nightscout.androidaps.plugins.pump.omnipod.dialogs

import android.graphics.Color
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import info.nightscout.androidaps.data.Profile
import info.nightscout.androidaps.db.OmnipodHistoryRecord
import info.nightscout.androidaps.logging.AAPSLogger
import info.nightscout.androidaps.logging.LTag
import info.nightscout.androidaps.plugins.pump.common.data.TempBasalPair
import info.nightscout.androidaps.plugins.pump.common.defs.PumpType
import info.nightscout.androidaps.plugins.pump.common.utils.ProfileUtil
import info.nightscout.androidaps.plugins.pump.omnipod.R
import info.nightscout.androidaps.plugins.pump.omnipod.driver.db.PodHistoryEntryType
import info.nightscout.androidaps.plugins.pump.omnipod.util.OmnipodUtil
import info.nightscout.androidaps.utils.resources.ResourceHelper

class OmnipodHistoryRecordViewHolder(view: View, val aapsLogger: AAPSLogger, val omnipodUtil: OmnipodUtil, val resourceHelper: ResourceHelper) : RecyclerView.ViewHolder(view) {

    private val time: TextView = view.findViewById(R.id.omnipod_history_time)
    private val type: TextView = view.findViewById(R.id.omnipod_history_source)
    private val value: TextView = view.findViewById(R.id.omnipod_history_description)
    private val layout: LinearLayout = view.findViewById(R.id.omnipod_history_item_layout)
    private val standardBackgroundColorFilter = view.background.colorFilter

    private var record: OmnipodHistoryRecord? = null

    fun bind(record: OmnipodHistoryRecord?) {
        this.record = record
        record?.let {
            time.text = record.dateTimeString
            type.setText(PodHistoryEntryType.getByCode(record.podEntryTypeCode).resourceId)
            value.text = description(record)

            if (PodHistoryEntryType.getByCode(record.podEntryTypeCode) == PodHistoryEntryType.PairAndPrime) {
                layout.background.setColorFilter(Color.parseColor("#8000ff00"), PorterDuff.Mode.OVERLAY)
            } else {
                layout.background.colorFilter = standardBackgroundColorFilter
            }
        }
    }

    private fun description(record: OmnipodHistoryRecord) : String {
        if (record.isSuccess) {
            return when (PodHistoryEntryType.getByCode(record.podEntryTypeCode)) {
                PodHistoryEntryType.SetTemporaryBasal -> {
                    val tempBasalPair: TempBasalPair = omnipodUtil.gsonInstance.fromJson(record.data, TempBasalPair::class.java)
                    resourceHelper.gs(R.string.omnipod_cmd_tbr_value, tempBasalPair.insulinRate, tempBasalPair.durationMinutes)
                }

                PodHistoryEntryType.FillCannulaSetBasalProfile,
                PodHistoryEntryType.SetBasalSchedule  -> profileValue(record.data)

                PodHistoryEntryType.SetBolus          -> {
                    if (record.data.contains(";")) {
                        val splitVal = record.data.split(";".toRegex()).toTypedArray()
                        resourceHelper.gs(R.string.omnipod_cmd_bolus_value_with_carbs, java.lang.Double.valueOf(splitVal[0]), java.lang.Double.valueOf(splitVal[1]))
                    } else {
                        resourceHelper.gs(R.string.omnipod_cmd_bolus_value, java.lang.Double.valueOf(record.data))
                    }
                }

                PodHistoryEntryType.PairAndPrime -> {
                    resourceHelper.gs(R.string.omnipod_history_new_pod_serial,record.podSerial)
                }

                PodHistoryEntryType.GetPodStatus,
                PodHistoryEntryType.GetPodInfo,
                PodHistoryEntryType.SetTime,
                PodHistoryEntryType.CancelTemporaryBasal,
                PodHistoryEntryType.CancelTemporaryBasalForce,
                PodHistoryEntryType.ConfigureAlerts,
                PodHistoryEntryType.CancelBolus,
                PodHistoryEntryType.DeactivatePod,
                PodHistoryEntryType.ResetPodState,
                PodHistoryEntryType.AcknowledgeAlerts,
                PodHistoryEntryType.SuspendDelivery,
                PodHistoryEntryType.ResumeDelivery,
                PodHistoryEntryType.UnknownEntryType  -> ""
                else -> ""
            }
        } else {
            return record.data
        }
    }

    private fun profileValue(data: String?) : String {
        if (data != null) {
            aapsLogger.debug(LTag.PUMP, "Profile json:\n$data")
            try {
                val profileValuesArray: Array<Profile.ProfileValue> = omnipodUtil.getGsonInstance().fromJson(data, Array<Profile.ProfileValue>::class.java)
                return ProfileUtil.getBasalProfilesDisplayable(profileValuesArray, PumpType.Insulet_Omnipod)
            } catch (e: Exception) {
                aapsLogger.error(LTag.PUMP, "Problem parsing Profile json. Ex: {}, Data:\n{}", e.message, data)
                return ""
            }
        } else {
            return ""
        }
    }

    companion object {

        fun create(parent: ViewGroup, aapsLogger: AAPSLogger, omnipodUtil: OmnipodUtil, resourceHelper: ResourceHelper) : OmnipodHistoryRecordViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.omnipod_pod_history_item, parent, false)
            return OmnipodHistoryRecordViewHolder(view, aapsLogger, omnipodUtil, resourceHelper)
        }
    }

}