package info.nightscout.androidaps.plugins.source;

import android.app.Activity;
import android.content.DialogInterface;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import info.nightscout.androidaps.Constants;
import info.nightscout.androidaps.MainApp;
import info.nightscout.androidaps.R;
import info.nightscout.androidaps.database.BlockingAppRepository;
import info.nightscout.androidaps.database.entities.GlucoseValue;
import info.nightscout.androidaps.database.transactions.InvalidateGlucoseValueTransaction;
import info.nightscout.androidaps.plugins.common.SubscriberFragment;
import info.nightscout.androidaps.plugins.configBuilder.ConfigBuilderPlugin;
import info.nightscout.androidaps.plugins.general.nsclient.NSUpload;
import info.nightscout.androidaps.plugins.iob.iobCobCalculator.events.EventAutosensCalculationFinished;
import info.nightscout.androidaps.utils.DateUtil;
import info.nightscout.androidaps.utils.FabricPrivacy;
import info.nightscout.androidaps.utils.GlucoseValueUtilsKt;
import info.nightscout.androidaps.utils.T;

/**
 * Created by mike on 16.10.2017.
 */

public class BGSourceFragment extends SubscriberFragment {
    RecyclerView recyclerView;

    String units = Constants.MGDL;

    final long MILLS_TO_THE_PAST = T.hours(12).msecs();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        try {
            View view = inflater.inflate(R.layout.bgsource_fragment, container, false);

            recyclerView = (RecyclerView) view.findViewById(R.id.bgsource_recyclerview);
            recyclerView.setHasFixedSize(true);
            LinearLayoutManager llm = new LinearLayoutManager(view.getContext());
            recyclerView.setLayoutManager(llm);

            long now = System.currentTimeMillis();
            List<GlucoseValue> glucoseValues = new ArrayList<>(BlockingAppRepository.INSTANCE.getGlucoseValuesInTimeRange(now - TimeUnit.DAYS.toMillis(1), Long.MAX_VALUE));
            Collections.reverse(glucoseValues);
            RecyclerViewAdapter adapter = new RecyclerViewAdapter(glucoseValues);
            recyclerView.setAdapter(adapter);

            if (ConfigBuilderPlugin.getPlugin().getActiveProfileInterface() != null && ConfigBuilderPlugin.getPlugin().getActiveProfileInterface().getProfile() != null && ConfigBuilderPlugin.getPlugin().getActiveProfileInterface().getProfile().getDefaultProfile() != null)
                units = ConfigBuilderPlugin.getPlugin().getActiveProfileInterface().getProfile().getDefaultProfile().getUnits();

            return view;
        } catch (Exception e) {
            FabricPrivacy.logException(e);
        }

        return null;
    }

    @Subscribe
    public void onStatusEvent(final EventAutosensCalculationFinished unused) {
        updateGUI();
    }

    @Override
    protected void updateGUI() {
        Activity activity = getActivity();
        if (activity != null)
            activity.runOnUiThread(() -> {
                long now = System.currentTimeMillis();
                List<GlucoseValue> glucoseValues = new ArrayList<>(BlockingAppRepository.INSTANCE.getGlucoseValuesInTimeRange(now - TimeUnit.DAYS.toMillis(1), Long.MAX_VALUE));
                Collections.reverse(glucoseValues);
                recyclerView.swapAdapter(new RecyclerViewAdapter(glucoseValues), true);
            });
    }

    public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.BgReadingsViewHolder> {

        List<GlucoseValue> bgReadings;

        RecyclerViewAdapter(List<GlucoseValue> bgReadings) {
            this.bgReadings = bgReadings;
        }

        @Override
        public BgReadingsViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.bgsource_item, viewGroup, false);
            return new BgReadingsViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull BgReadingsViewHolder holder, int position) {
            GlucoseValue bgReading = bgReadings.get(position);
            holder.ns.setVisibility(NSUpload.isIdValid(bgReading.getInterfaceIDs().getNightscoutId()) ? View.VISIBLE : View.GONE);
            holder.invalid.setVisibility(!bgReading.isValid() ? View.VISIBLE : View.GONE);
            holder.date.setText(DateUtil.dateAndTimeString(bgReading.getTimestamp()));
            holder.value.setText(GlucoseValueUtilsKt.valueToUnitsString(bgReading.getValue(), units));
            holder.direction.setText(GlucoseValueUtilsKt.toSymbol(bgReading.getTrendArrow()));
            holder.remove.setTag(bgReading);
        }

        @Override
        public int getItemCount() {
            return bgReadings.size();
        }

        class BgReadingsViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            TextView date;
            TextView value;
            TextView direction;
            TextView invalid;
            TextView ns;
            TextView remove;

            BgReadingsViewHolder(View itemView) {
                super(itemView);
                date = (TextView) itemView.findViewById(R.id.bgsource_date);
                value = (TextView) itemView.findViewById(R.id.bgsource_value);
                direction = (TextView) itemView.findViewById(R.id.bgsource_direction);
                invalid = (TextView) itemView.findViewById(R.id.invalid_sign);
                ns = (TextView) itemView.findViewById(R.id.ns_sign);
                remove = (TextView) itemView.findViewById(R.id.bgsource_remove);
                remove.setOnClickListener(this);
                remove.setPaintFlags(remove.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
            }

            @Override
            public void onClick(View v) {
                final GlucoseValue bgReading = (GlucoseValue) v.getTag();
                switch (v.getId()) {

                    case R.id.bgsource_remove:
                        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                        builder.setTitle(MainApp.gs(R.string.confirmation));
                        builder.setMessage(MainApp.gs(R.string.removerecord) + "\n" + DateUtil.dateAndTimeString(bgReading.getTimestamp()) + "\n" + GlucoseValueUtilsKt.valueToUnitsString(bgReading.getValue(), units));
                        builder.setPositiveButton(MainApp.gs(R.string.ok), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
/*                                final String _id = bgReading._id;
                                if (NSUpload.isIdValid(_id)) {
                                    NSUpload.removeFoodFromNS(_id);
                                } else {
                                    UploadQueue.removeID("dbAdd", _id);
                                }
*/
                                BlockingAppRepository.INSTANCE.runTransaction(new InvalidateGlucoseValueTransaction(bgReading.getId()));
                                updateGUI();
                            }
                        });
                        builder.setNegativeButton(MainApp.gs(R.string.cancel), null);
                        builder.show();
                        break;

                }
            }
        }
    }

}
