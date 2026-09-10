package com.middleware.platform.unit;

import com.middleware.platform.iam.security.IpAllowlist;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IpAllowlistTest {

    @Test
    void matchesPlainIpAndCidr() {
        String list = "82.114.181.167, 185.240.67.0/24";
        assertThat(IpAllowlist.matches("82.114.181.167", list)).isTrue();
        assertThat(IpAllowlist.matches("185.240.67.94", list)).isTrue();
        assertThat(IpAllowlist.matches("185.240.68.1", list)).isFalse();
        assertThat(IpAllowlist.matches("82.114.181.168", list)).isFalse();
        assertThat(IpAllowlist.matches(null, list)).isFalse();
        assertThat(IpAllowlist.matches("1.2.3.4", (String) null)).isFalse();
    }

    @Test
    void cidrEdgeCases() {
        assertThat(IpAllowlist.matches("10.20.30.40", "0.0.0.0/0")).isTrue();
        assertThat(IpAllowlist.matches("10.20.30.40", "10.20.30.40/32")).isTrue();
        assertThat(IpAllowlist.matches("10.20.30.41", "10.20.30.40/32")).isFalse();
        assertThat(IpAllowlist.matches("192.168.1.5", "192.168.0.0/16")).isTrue();
        // IPv6 callers never match an IPv4 CIDR; literal IPv6 entries compare verbatim.
        assertThat(IpAllowlist.matches("::1", "10.0.0.0/8")).isFalse();
        assertThat(IpAllowlist.matches("::1", "::1")).isTrue();
    }

    @Test
    void normaliseTrimsDedupesAndSplitsPastedLists() {
        List<String> out = IpAllowlist.normalise(List.of(
                " 82.114.181.167 ", "82.114.179.185\n82.114.183.85", "82.114.181.167", "", "185.240.67.94,82.114.171.50"));
        assertThat(out).containsExactly(
                "82.114.181.167", "82.114.179.185", "82.114.183.85", "185.240.67.94", "82.114.171.50");
        assertThat(IpAllowlist.join(out)).isEqualTo(
                "82.114.181.167,82.114.179.185,82.114.183.85,185.240.67.94,82.114.171.50");
        assertThat(IpAllowlist.parse(IpAllowlist.join(out))).isEqualTo(out);
        assertThat(IpAllowlist.join(List.of())).isNull();
    }

    @Test
    void rejectsInvalidEntries() {
        assertThatThrownBy(() -> IpAllowlist.normalise(List.of("300.1.1.1")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("300.1.1.1");
        assertThatThrownBy(() -> IpAllowlist.normalise(List.of("10.0.0.0/33")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("prefix");
        assertThatThrownBy(() -> IpAllowlist.normalise(List.of("bank.example.com")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(IpAllowlist.validationError("10.0.0.0/8")).isEmpty();
        assertThat(IpAllowlist.validationError("2001:db8::1")).isEmpty();
    }
}
