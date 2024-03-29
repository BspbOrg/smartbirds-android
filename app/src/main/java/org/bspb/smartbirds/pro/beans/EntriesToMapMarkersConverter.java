package org.bspb.smartbirds.pro.beans;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.res.StringRes;
import org.bspb.smartbirds.pro.R;
import org.bspb.smartbirds.pro.collections.Converter;
import org.bspb.smartbirds.pro.content.MonitoringEntry;
import org.bspb.smartbirds.pro.enums.EntryType;
import org.bspb.smartbirds.pro.ui.map.EntryMapMarker;
import org.bspb.smartbirds.pro.ui.utils.FormsConfig;

/**
 * Created by groupsky on 20.03.17.
 */
@EBean(scope = EBean.Scope.Singleton)
public class EntriesToMapMarkersConverter implements Converter<MonitoringEntry, EntryMapMarker> {

    @StringRes(R.string.tag_lat)
    protected String tagLatitude;

    @StringRes(R.string.tag_lon)
    protected String tagLongitude;

    @StringRes(R.string.entry_type_ciconia)
    protected String entryTypeCiconia;

    @StringRes(R.string.tag_species_scientific_name)
    protected String tagSpecies1;

    @StringRes(R.string.tag_observed_bird)
    protected String tagSpecies2;

    @StringRes(R.string.tag_primary_type)
    protected String tagThreatsPrimaryType;

    @StringRes(R.string.tag_category)
    protected String tagThreatsCategory;

    @StringRes(R.string.monitoring_threats_poisoned)
    protected String poisonedString;

    @StringRes(R.string.tag_pylons_pylon_type)
    protected String tagPylonType;

    @Override
    public EntryMapMarker convert(MonitoringEntry item) {
        double lat = Double.valueOf(item.data.get(tagLatitude));
        double lon = Double.valueOf(item.data.get(tagLongitude));
        String name = null;

        if (EntryType.CICONIA.equals(item.type)) {
            name = entryTypeCiconia;
        } else if (EntryType.THREATS.equals(item.type)) {
            String primaryType = item.data.get(tagThreatsPrimaryType);
            if (FormsConfig.ThreatsPrimaryType.threat.isSame(primaryType)) {
                name = item.data.get(tagThreatsCategory);
            } else {
                name = poisonedString;
            }

        } else if (EntryType.PYLONS.equals(item.type)) {
            name = item.data.get(tagPylonType);
        } else {
            if (item.data.containsKey(tagSpecies1)) {
                name = item.data.get(tagSpecies1);
            } else if (item.data.containsKey(tagSpecies2)) {
                name = item.data.get(tagSpecies2);
            }
        }
        return new EntryMapMarker(name, lat, lon, item.id, item.type);
    }
}
