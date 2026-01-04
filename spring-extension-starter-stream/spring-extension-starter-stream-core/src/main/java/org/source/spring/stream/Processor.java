package org.source.spring.stream;

public interface Processor {
    default Status status() {
        return Status.NEW;
    }

    enum Status {
        NEW,
        PROCESSED
    }
}
