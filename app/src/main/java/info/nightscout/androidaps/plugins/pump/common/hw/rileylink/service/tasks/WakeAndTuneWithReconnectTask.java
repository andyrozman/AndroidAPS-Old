package info.nightscout.androidaps.plugins.pump.common.hw.rileylink.service.tasks;

import android.os.SystemClock;

import info.nightscout.androidaps.plugins.bus.RxBus;
import info.nightscout.androidaps.plugins.pump.common.hw.rileylink.RileyLinkConst;
import info.nightscout.androidaps.plugins.pump.common.hw.rileylink.RileyLinkUtil;
import info.nightscout.androidaps.plugins.pump.common.hw.rileylink.defs.RileyLinkError;
import info.nightscout.androidaps.plugins.pump.common.hw.rileylink.defs.RileyLinkServiceState;
import info.nightscout.androidaps.plugins.pump.common.hw.rileylink.service.data.ServiceTransport;
import info.nightscout.androidaps.plugins.pump.medtronic.MedtronicPumpPlugin;
import info.nightscout.androidaps.plugins.pump.medtronic.events.EventRefreshButtonState;
import info.nightscout.androidaps.plugins.pump.medtronic.service.RileyLinkMedtronicService;

import static info.nightscout.androidaps.plugins.pump.common.hw.rileylink.RileyLinkUtil.getRileyLinkCommunicationManager;

/**
 * Created by geoff on 7/16/16.
 */
public class WakeAndTuneWithReconnectTask extends PumpTask {

    private static final String TAG = "WakeAndTuneTask";


    public WakeAndTuneWithReconnectTask() {
    }


    public WakeAndTuneWithReconnectTask(ServiceTransport transport) {
        super(transport);
    }


    @Override
    public void run() {
        RileyLinkMedtronicService serviceInstance = RileyLinkMedtronicService.getInstance();
        RxBus.INSTANCE.send(new EventRefreshButtonState(false));
        MedtronicPumpPlugin.isBusy = true;
        double newFrequency = serviceInstance.doTuneUpDeviceNoStatusChange();
        MedtronicPumpPlugin.isBusy = false;

        if (newFrequency == 0.0d) {
            serviceInstance.disconnectRileyLink();

            SystemClock.sleep(10000);
        }

        RxBus.INSTANCE.send(new EventRefreshButtonState(true));

        //ServiceTaskExecutor.startTask(new WakeAndTuneTask());
        //RileyLinkUtil.sendBroadcastMessage(RileyLinkConst.Intents.RileyLinkDisconnect);

        //ServiceTask task = new InitializePumpManagerTask(RileyLinkUtil.getTargetDevice());
        //ServiceTaskExecutor.startTask(task);

    }
}
