package io.devforge.platform.core.security;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile("local & !wso2")
public class LocalSecurityConfiguration {

    @Bean
    @Order(0)
    SecurityFilterChain localDevelopmentSecurity(
            HttpSecurity http,
            @Value("${server.address:127.0.0.1}") String serverAddress) throws Exception {
        if (!isLoopbackBinding(serverAddress)) {
            throw new IllegalStateException(
                    "The local security profile permits unauthenticated access and must bind only to loopback. "
                            + "Use the WSO2 profile for network-accessible deployments.");
        }
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .build();
    }

    static boolean isLoopbackBinding(String address) {
        if (address == null || address.isBlank()) {
            return false;
        }
        try {
            InetAddress[] resolved = InetAddress.getAllByName(address);
            return resolved.length > 0 && Arrays.stream(resolved).allMatch(InetAddress::isLoopbackAddress);
        } catch (UnknownHostException exception) {
            return false;
        }
    }
}
