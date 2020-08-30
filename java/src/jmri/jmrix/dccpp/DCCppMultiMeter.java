package jmri.jmrix.dccpp;

import jmri.*;
import jmri.implementation.DefaultMeter;
import jmri.implementation.MeterUpdateTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provide access to current meter from the DCC++ Base Station
 *
 * @author Mark Underwood (C) 2015
 */
public class DCCppMultiMeter extends jmri.implementation.DefaultMeterGroup implements DCCppListener {

    private DCCppTrafficController tc = null;
    private final MeterUpdateTask updateTask;
    private final Meter currentMeter;

    public DCCppMultiMeter(DCCppSystemConnectionMemo memo) {
        super("DVBaseStation");
//        super(DCCppConstants.METER_INTERVAL_MS);
        
        tc = memo.getDCCppTrafficController();

        updateTask = new MeterUpdateTask(10000, 0) {
            @Override
            public void requestUpdateFromLayout() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        };
        
        currentMeter = new DefaultMeter("DVBaseStationCurrent", Meter.Unit.Percent, 0, 100.0, 1.0, updateTask);
        
        InstanceManager.getDefault(MeterManager.class).register(currentMeter);
        
        addMeter(MeterGroup.CurrentMeter, MeterGroup.CurrentMeterDescr, currentMeter);

        // TODO: For now this is OK since the traffic controller
        // ignores filters and sends out all updates, but
        // at some point this will have to be customized.
        tc.addDCCppListener(DCCppInterface.THROTTLE, this);

        //is_enabled = false;
        updateTask.initTimer();

        log.debug("DCCppMultiMeter constructor called");
    }

    public void setDCCppTrafficController(DCCppTrafficController controller) {
        tc = controller;
    }

    @Override
    public void message(DCCppReply r) {
        log.debug("DCCppMultiMeter received reply: {}", r.toString());
        if (r.isCurrentReply()) {
            try {
                setCurrent(((r.getCurrentInt() * 1.0f) / (DCCppConstants.MAX_CURRENT * 1.0f)) * 100.0f );  // return as percentage.
            } catch (JmriException e) {
                log.error("exception thrown by setCurrent", e);
            }
        }
    }

    @Override
    public void message(DCCppMessage m) {
    }

    @Override
    public void requestUpdateFromLayout() {
        tc.sendDCCppMessage(DCCppMessage.makeReadTrackCurrentMsg(), this);
    }
/*
    @Override
    // Handle a timeout notification
    public String getHardwareMeterName() {
        return ("DCC++");
    }

    @Override
    public boolean hasCurrent() {
        return true;
    }

    @Override
    public boolean hasVoltage() {
        return false;
    }

    @Override
    public CurrentUnits getCurrentUnits() {
        return  CurrentUnits.CURRENT_UNITS_PERCENTAGE;
    }
*/
    // Handle a timeout notification
    @Override
    public void notifyTimeout(DCCppMessage msg) {
        log.debug("Notified of timeout on message {}, {} retries available.", msg.toString(), msg.getRetries());
    }

    private final static Logger log = LoggerFactory.getLogger(DCCppMultiMeter.class);

}
