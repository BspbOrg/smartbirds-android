package org.bspb.smartbirds.pro.db;

import android.provider.BaseColumns;

import net.simonvt.schematic.annotation.AutoIncrement;
import net.simonvt.schematic.annotation.DataType;
import net.simonvt.schematic.annotation.NotNull;
import net.simonvt.schematic.annotation.PrimaryKey;
import net.simonvt.schematic.annotation.Unique;

import static net.simonvt.schematic.annotation.DataType.Type.BLOB;
import static net.simonvt.schematic.annotation.DataType.Type.INTEGER;
import static net.simonvt.schematic.annotation.DataType.Type.TEXT;

/**
 * Created by groupsky on 22.11.16.
 */

public interface MonitoringColumns {

    @DataType(INTEGER)
    @PrimaryKey
    @AutoIncrement
    String _ID = BaseColumns._ID;

    @DataType(TEXT)
    @Unique
    @NotNull
    String CODE = "code";

    /**
     * {@link org.bspb.smartbirds.pro.content.Monitoring.Status}
     */
    @DataType(TEXT)
    @NotNull
    String STATUS = "status";

    @DataType(BLOB)
    @NotNull
    String DATA = "data";

    String ENTRIES_COUNT = "entries_count";

}
