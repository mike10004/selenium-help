package com.github.mike10004.seleniumhelp;

import com.github.mike10004.xvfbtesting.XvfbRule;
import io.github.bonigarcia.wdm.ChromeDriverManager;
import io.github.bonigarcia.wdm.FirefoxDriverManager;
import net.lightbody.bmp.core.har.Har;
import org.apache.http.cookie.MalformedCookieException;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CookieStorageTest {

    @Rule
    public XvfbRule xvfb = XvfbRule.builder().disabledOnWindows().build();

    @Test
    public void testFirefox() throws Exception {
        FirefoxDriverManager.getInstance().setup(UnitTests.REQUIRED_GECKODRIVER_VERSION);
        testWithDriverFactory(FirefoxWebDriverFactory.builder().environment(xvfb.getController().newEnvironment()).build());
    }

    @Test
    public void testChrome() throws Exception {
        ChromeDriverManager.getInstance().setup();
        testWithDriverFactory(ChromeWebDriverFactory.builder().environment(xvfb.getController()::newEnvironment).build());
    }

    private void testWithDriverFactory(WebDriverFactory factory) throws IOException, MalformedCookieException {
        HarPlus<Void> collection = HttpsTestTrafficCollector.build(factory).collect(driver -> {
            driver.get("https://httprequestecho.appspot.com/cookies/set");
            return (Void) null;
        });
        Har har = collection.har;
        final FlexibleCookieSpec cookieSpec = FlexibleCookieSpec.getDefault();
        List<DeserializableCookie> harCookies = HarAnalysis.of(har).findCookies(cookieSpec).makeUltimateCookieList();
        if (harCookies.size() > 1) {
            System.out.println("multiple cookies found");
            for (DeserializableCookie cookie : harCookies) {
                System.out.format("%s%n", cookie);
            }
        }
        assertEquals("num cookies", 1, harCookies.size());
        checkCookies(harCookies);
    }

    private void checkCookies(Iterable<DeserializableCookie> cookies) {
        cookies.forEach(c -> {
            System.out.format("cookie: %s%n", c);
            assertNotNull("name", c.getName());
            assertNotNull("value", c.getValue());
            assertNotNull("path", c.getPath());
            assertNotNull("domain", c.getDomain());
            assertNotNull("domain attribute", c.getDomainAttribute());
            assertNotNull("expiry", c.getExpiryDate());
        });
    }

}
