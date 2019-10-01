package com.github.mike10004.seleniumhelp;

import com.github.mike10004.xvfbtesting.XvfbRule;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.github.bonigarcia.wdm.DriverManagerType;
import io.github.bonigarcia.wdm.WebDriverManager;

import java.net.URI;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface WebDriverTestParameter {

    WebDriverFactory createWebDriverFactory(XvfbRule xvfb);

    /**
     * Checks whether the test should check that the page text is what was expected.
     */
    boolean isBrotliSupported(String url);

    DriverManagerType getDriverManagerType();

    static List<WebDriverTestParameter> all() {
        return createList(x -> true);
    }

    static List<WebDriverTestParameter> createList(Predicate<? super DriverManagerType> typePredicate) {
        return Stream.of(new FirefoxTestParameter(), new ChromeTestParameter())
                .filter(p -> typePredicate.test(p.getDriverManagerType()))
                .collect(Collectors.toList());
    }

    class ChromeTestParameter implements WebDriverTestParameter {
        @Override
        public WebDriverFactory createWebDriverFactory(XvfbRule xvfb) {
            return ChromeWebDriverFactory.builder()
                    .chromeOptions(UnitTests.createChromeOptions())
                    .environment(xvfb.getController().newEnvironment())
                    .build();
        }

        @Override
        public boolean isBrotliSupported(String url) {
            return true;
        }

        @Override
        public DriverManagerType getDriverManagerType() {
            return DriverManagerType.CHROME;
        }
    }

    class FirefoxTestParameter implements WebDriverTestParameter {
        @Override
        public WebDriverFactory createWebDriverFactory(XvfbRule xvfb) {
            return FirefoxWebDriverFactory.builder()
                    .putPreferences(UnitTests.createFirefoxPreferences())
                    .environment(xvfb.getController().newEnvironment())
                    .build();
        }

        /**
         * Firefox does not send 'br' in the accept-encoding request header for http requests,
         * and (I suppose) doesn't decode responses in encodings it didn't expect.
         * See: https://bugzilla.mozilla.org/show_bug.cgi?id=1218924
         * @return false if URL scheme is http
         */
        @Override
        public boolean isBrotliSupported(String url) {
            return !"http".equals(URI.create(url).getScheme());
        }

        @Override
        public DriverManagerType getDriverManagerType() {
            return DriverManagerType.FIREFOX;
        }

    }

    static void doDriverManagerSetup(WebDriverTestParameter testParameter) {
        DriverManagerSetupCache.doSetup(testParameter.getDriverManagerType());
    }

    class DriverManagerSetupCache {

        private static final LoadingCache<DriverManagerType, ?> driverManagerSetupCache = CacheBuilder.newBuilder()
                .build(new CacheLoader<DriverManagerType, Object>() {
                    @Override
                    public Object load(@SuppressWarnings("NullableProblems") DriverManagerType key) {
                        WebDriverManager.getInstance(key).setup();
                        return new Object();
                    }
                });

        private DriverManagerSetupCache() {}

        private static void doSetup(DriverManagerType driverManagerType) {
            driverManagerSetupCache.getUnchecked(driverManagerType);
        }
    }
}

