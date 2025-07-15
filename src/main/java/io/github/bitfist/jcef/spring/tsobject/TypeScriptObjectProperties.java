package io.github.bitfist.jcef.spring.tsobject;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jcef.tsobject")
@RequiredArgsConstructor
public class TypeScriptObjectProperties {
    private final boolean enableWebCommunication;
}
