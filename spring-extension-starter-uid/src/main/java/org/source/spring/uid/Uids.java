package org.source.spring.uid;

import com.github.yitter.idgen.YitIdHelper;
import lombok.experimental.UtilityClass;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.util.Objects;

@UtilityClass
public class Uids {
    public static Long longId() {
        return YitIdHelper.nextId();
    }

    public static String stringId(@Nullable UidPrefix prefix) {
        if (Objects.isNull(prefix)) {
            return stringId();
        }
        return stringId(prefix.getPrefix());
    }

    public static String stringId() {
        return stringId("");
    }

    private static String stringId(@Nullable String prefix) {
        Long id = longId();
        if (StringUtils.hasText(prefix)) {
            return prefix + id;
        }
        return "" + id;
    }

}