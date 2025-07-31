package io.github.bitfist.jcef.spring.browser;

import org.jspecify.annotations.Nullable;

public interface CefQueryHandler {

	@Nullable
	String handleQuery(@Nullable String query);

}
