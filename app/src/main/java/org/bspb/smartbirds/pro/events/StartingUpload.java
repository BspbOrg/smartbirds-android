package org.bspb.smartbirds.pro.events;

/**
 * Created by groupsky on 14-10-21.
 */
public class StartingUpload {
    public final String monitoringPath;
    public StartingUpload(String monitoringPath) {
        this.monitoringPath = monitoringPath;
    }
}
