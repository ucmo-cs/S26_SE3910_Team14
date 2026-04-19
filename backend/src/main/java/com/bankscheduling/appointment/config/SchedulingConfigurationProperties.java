package com.bankscheduling.appointment.config;

import com.bankscheduling.appointment.config.properties.PiiCryptoProperties;
import com.bankscheduling.appointment.security.CorsApplicationProperties;
import com.bankscheduling.appointment.security.JwtSecurityProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        JwtSecurityProperties.class,
        CorsApplicationProperties.class,
        PiiCryptoProperties.class
})
public class SchedulingConfigurationProperties {
}
