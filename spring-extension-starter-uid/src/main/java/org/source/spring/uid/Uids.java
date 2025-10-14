package org.source.spring.uid;

import lombok.experimental.UtilityClass;

import java.util.Objects;

@UtilityClass
public class Uids {
    private static UidGenerator uidGenerator;

    public static synchronized void setUidGenerator(UidGenerator uidGenerator) {
        Uids.uidGenerator = uidGenerator;
    }

    public static Long longId() {
        if (Objects.isNull(uidGenerator)) {
            return null;
        }
        return uidGenerator.nextId();
    }

    public static String stringId() {
        Long id = longId();
        if (Objects.nonNull(id)) {
            return String.valueOf(id);
        }
        return null;
    }

}
