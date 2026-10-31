package com.atlas.supplychain.auth.infrastructure.config.security;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "atlas.cors")
public class CorsProperties {

    private List<String> allowedOrigins = new ArrayList<>();
}
