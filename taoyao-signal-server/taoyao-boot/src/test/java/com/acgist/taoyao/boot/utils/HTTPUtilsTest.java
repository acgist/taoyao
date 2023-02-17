package com.acgist.taoyao.boot.utils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HTTPUtilsTest {

    @Test
    void test() throws IOException, InterruptedException {
        HTTPUtils.init(5000, Executors.newCachedThreadPool());
        final HttpClient client = HTTPUtils.newClient();
        final HttpResponse<String> response = client.send(
            HttpRequest.newBuilder(URI.create("https://www.acgist.com")).GET().build(),
            BodyHandlers.ofString()
        );
        log.info("{}", response.body());
    }
    
}
