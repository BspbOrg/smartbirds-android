package org.bspb.smartbirds.pro.ui.views;

/**
 * Interface for views that support read-only mode.
 */
public interface SupportReadOnly {
    /**
     * Set whether this view is in read-only mode.
     * When read-only, the view should:
     * - Display its current value
     * - Prevent user editing/interaction
     *
     * @param readOnly true to make the view read-only, false to allow editing
     */
    void setReadOnly(boolean readOnly);

    /**
     * Check if this view is currently in read-only mode.
     *
     * @return true if read-only, false otherwise
     */
    boolean isReadOnly();
}
