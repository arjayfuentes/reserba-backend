package com.rjproj.reserba.config;

import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "itemconfig")
@RequiredArgsConstructor
@Data
public class ItemConfig {

    private String name;

    private String value;


}
