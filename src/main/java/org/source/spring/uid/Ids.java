package org.source.spring.uid;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Ids {
    private static IdGenerator idGenerator;

    public static synchronized void setIdGenerator(IdGenerator idGenerator) {
        Ids.idGenerator = idGenerator;
    }

    public static long longId() {
        return idGenerator.nextId();
    }

    public static String stringId() {
        return String.valueOf(idGenerator.nextId());
    }

}
