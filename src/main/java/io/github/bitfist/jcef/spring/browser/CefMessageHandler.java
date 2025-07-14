package io.github.bitfist.jcef.spring.browser;

import org.springframework.lang.Nullable;

public interface CefMessageHandler {

    @Nullable String handleQuery(@Nullable String query);

}
