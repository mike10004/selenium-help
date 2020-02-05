package com.github.mike10004.seleniumhelp;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class ReadmeExample {

    public static void main(String[] args) throws IOException {
        WebDriverManager.chromedriver().setup();
        ChromeWebDriverFactory factory = ChromeWebDriverFactory.builder()
                .configure(chromeOptions -> {
                    // ...
                })
                .build();
        Path scratchDir = java.nio.file.Files.createTempDirectory("selenium-help-example");
        try {
            TrafficCollector collector = TrafficCollector.builder(factory)
                    .collectHttps(new AutoCertificateAndKeySource(scratchDir))
                    .build();
            HarPlus<String> harPlus = collector.collect(new TrafficGenerator<String>() {
                @Override
                public String generate(WebDriver driver) {
                    driver.get("https://www.example.com/");
                    return driver.getTitle();
                }
            });
            System.out.println("collected page with title " + harPlus.result);
            File harFile = File.createTempFile("selenium-help-example", ".har");
            BrowserUpHars.writeHarTo(harPlus.har, harFile, StandardCharsets.UTF_8);
            System.out.format("wrote har to %s%n", harFile);
        } finally {
            FileUtils.forceDelete(scratchDir.toFile());
        }
    }

}
