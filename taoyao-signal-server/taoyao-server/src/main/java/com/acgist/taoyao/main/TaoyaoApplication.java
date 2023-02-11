package com.acgist.taoyao.main;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan(basePackages = "com.acgist.taoyao")
@SpringBootApplication
public class TaoyaoApplication {

	public static void main(String[] args) {
		System.getProperties().setProperty("jdk.internal.httpclient.disableHostnameVerification", Boolean.TRUE.toString());
		SpringApplication.run(TaoyaoApplication.class, args);
	}

}
