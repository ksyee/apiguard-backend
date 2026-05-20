package com.apiguard.backend.global.security;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OutboundUrlGuard {

    private static final String HTTP = "http";
    private static final String HTTPS = "https";

    private final OutboundUrlProperties properties;

    public URI validateHttpUrl(String rawUrl, String label) {
        URI uri = parseUri(rawUrl, label);
        String scheme = uri.getScheme();
        if (scheme == null || (!HTTP.equalsIgnoreCase(scheme) && !HTTPS.equalsIgnoreCase(scheme))) {
            throw new IllegalArgumentException(label + "은 http/https URL이어야 합니다.");
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException(label + "의 host가 올바르지 않습니다.");
        }

        if (!properties.isAllowPrivateNetwork()) {
            validatePublicHost(host, label);
        }
        return uri;
    }

    private URI parseUri(String rawUrl, String label) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new IllegalArgumentException(label + "이 비어 있습니다.");
        }
        try {
            return new URI(rawUrl.trim());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(label + " 형식이 올바르지 않습니다.");
        }
    }

    private void validatePublicHost(String host, String label) {
        String normalizedHost = host.toLowerCase(Locale.ROOT);
        if (isLocalHostname(normalizedHost)) {
            throw new IllegalArgumentException(label + "은 localhost/private network로 요청할 수 없습니다.");
        }

        try {
            for (InetAddress address : InetAddress.getAllByName(host)) {
                if (isPrivateAddress(address)) {
                    throw new IllegalArgumentException(label + "은 localhost/private network로 요청할 수 없습니다.");
                }
            }
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException(label + "의 host를 확인할 수 없습니다.");
        }
    }

    private boolean isLocalHostname(String host) {
        return "localhost".equals(host) || host.endsWith(".localhost") || "localtest.me".equals(host);
    }

    private boolean isPrivateAddress(InetAddress address) {
        byte[] raw = address.getAddress();
        return address.isAnyLocalAddress()
            || address.isLoopbackAddress()
            || address.isLinkLocalAddress()
            || address.isSiteLocalAddress()
            || address.isMulticastAddress()
            || isAwsMetadataAddress(raw)
            || isIpv6UniqueLocalAddress(raw);
    }

    private boolean isAwsMetadataAddress(byte[] raw) {
        return raw.length == 4
            && (raw[0] & 0xff) == 169
            && (raw[1] & 0xff) == 254
            && (raw[2] & 0xff) == 169
            && (raw[3] & 0xff) == 254;
    }

    private boolean isIpv6UniqueLocalAddress(byte[] raw) {
        return raw.length == 16 && (raw[0] & 0xfe) == 0xfc;
    }
}
