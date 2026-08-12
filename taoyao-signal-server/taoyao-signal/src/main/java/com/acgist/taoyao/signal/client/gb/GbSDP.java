package com.acgist.taoyao.signal.client.gb;

import java.util.stream.Stream;

import org.apache.commons.lang3.math.NumberUtils;

public class GbSDP {

    public static final Long getSsrc(String content) {
        return Stream.of(content.split("\n"))
            .map(x -> x.strip())
            .filter(x -> x.startsWith("y="))
            .map(x -> x.substring(2).strip())
            .filter(NumberUtils::isDigits)
            .map(Long::valueOf)
            .findFirst()
            .orElse(0L);
    }

    public static final String getHost(String content) {
        return Stream.of(content.split("\n"))
            .map(x -> x.strip())
            .filter(x -> x.startsWith("c=IN IP4 "))
            .map(x -> x.substring(9).strip())
            .findFirst()
            .orElse(null);
    }

    public static final Integer getPort(String content) {
        return Stream.of(content.split("\n"))
            .map(x -> x.strip())
            .filter(x -> x.startsWith("m=video"))
            .map(x -> {
                final String[] array = x.split(" ");
                if (array.length >= 2) {
                    return array[1].strip();
                }
                return null;
            })
            .filter(NumberUtils::isDigits)
            .map(Integer::valueOf)
            .findFirst()
            .orElse(null);
    }

    public static final String recvSDP(String deviceId, String host, int port) {
        return String.format(
            """
            v=0
            o=%s 0 0 IN IP4 %s
            s=Play
            c=IN IP4 %s
            t=0 0
            m=video %d RTP/AVP 96
            a=recvonly
            a=rtpmap:96 PS/90000
            """,
            deviceId,
            host,
            host,
            port
        );
    }

    public static final String sendSDP(String deviceId, String host, int port) {
        return String.format(
            """
            v=0
            o=%s 0 0 IN IP4 %s
            s=Play
            c=IN IP4 %s
            t=0 0
            m=video %d RTP/AVP 96
            a=recvonly
            a=rtpmap:96 PS/90000
            """,
            deviceId,
            host,
            host,
            port
        );
    }

}
