package com.github.mike10004.seleniumhelp;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import com.google.common.net.HttpHeaders;
import net.lightbody.bmp.core.har.*;
import org.junit.Test;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.mike10004.seleniumhelp.HarCleaner.HEADER_VALUE_BROTLI_ENCODED;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.*;

public class HarCleanerTest {
    @Test
    public void clean() throws Exception {
        Har har = new Har();
        HarLog log = new HarLog();
        har.setLog(log);
        String plainJs = new String(loadResource("/harcleaner/response.js"), UTF_8);
        String plainCss = new String(loadResource("/harcleaner/response.css"), UTF_8);
        byte[] brotliJs = loadResource("/harcleaner/response.js.br");
        byte[] brotliCss = loadResource("/harcleaner/response.css.br");
        HarResponse plainJsResponse = buildResponse("text/javascript", null, plainJs, null);
        HarResponse brotliJsResponse = buildResponse("text/javascript", HEADER_VALUE_BROTLI_ENCODED, brotliJs);
        HarResponse plainCssResponse = buildResponse("text/css", null, plainCss, null);
        HarResponse brotliCssResponse = buildResponse("text/css", HEADER_VALUE_BROTLI_ENCODED, brotliCss);
        List<HarEntry> entries = Stream.of(plainJsResponse, brotliCssResponse, plainCssResponse, brotliJsResponse)
                .map(rsp -> buildEntry(new HarRequest(), rsp)).collect(Collectors.toList());
        entries.forEach(log::addEntry);
        List<HarEntry> cleaned = new HarCleaner().clean(har);
        for (HarEntry entry : cleaned) {
            System.out.format("%s (%s) %s%n", entry.getResponse().getContent().getMimeType(), entry.getResponse().getContent().getEncoding(), entry.getRequest().getUrl());
        }
        System.out.format("%d entries cleaned%n", cleaned.size());
        assertEquals("num cleaned entries", 2, cleaned.size());
        assertEquals("brotli-decompressed js", plainJs, brotliJsResponse.getContent().getText());
        assertEquals("brotli-decompressed css", plainCss, brotliCssResponse.getContent().getText());
    }

    private HarEntry buildEntry(HarRequest request, HarResponse response) {
        HarEntry entry = new HarEntry();
        entry.setRequest(request);
        entry.setResponse(response);
        return entry;
    }

    private byte[] loadResource(String resourcePath) throws IOException {
        return Resources.toByteArray(getClass().getResource(resourcePath));
    }

    private HarResponse buildResponse(String contentTypeHeader, String contentEncodingHeader, byte[] harContentBytes) {
        return buildResponse(contentTypeHeader, contentEncodingHeader, Base64.getEncoder().encodeToString(harContentBytes), "base64");
    }

    private HarNameValuePair buildHeader(String name, String value) {
        HarNameValuePair header = new HarNameValuePair(name, value);
        return header;

    }

    private HarResponse buildResponse(String contentTypeHeader, String contentEncodingHeader, String harContentText, String harContentEncoding) {
        HarResponse response = new HarResponse(200, "OK", null);
        HarContent content = response.getContent();
        content.setMimeType(contentTypeHeader);
        content.setEncoding(harContentEncoding);
        content.setText(harContentText);
        List<HarNameValuePair> headers = ImmutableList.of(buildHeader(HttpHeaders.CONTENT_TYPE, contentTypeHeader), buildHeader(HttpHeaders.CONTENT_ENCODING, contentEncodingHeader));
        response.getHeaders().addAll(headers);
        return response;
    }
}