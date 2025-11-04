package org.source.spring.uid;

import com.github.yitter.idgen.YitIdHelper;
import lombok.experimental.UtilityClass;
import org.springframework.util.StringUtils;

@UtilityClass
public class Uids {
    public static Long longId() {
        return YitIdHelper.nextId();
    }

    public static String stringId() {
        return stringId("");
    }

    public static String stringId(String prefix) {
        Long id = longId();
        if (StringUtils.hasText(prefix)) {
            return prefix + id;
        }
        return "" + id;
    }

}