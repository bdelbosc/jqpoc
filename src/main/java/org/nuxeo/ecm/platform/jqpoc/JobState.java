package org.nuxeo.ecm.platform.jqpoc;

public enum JobState {
    READY, // Ready to be processed
    PROCESSING, // Already processed by another worker
    TIMEDOUT, // Not processed within the expected time
    NONE // nothing to do
}
