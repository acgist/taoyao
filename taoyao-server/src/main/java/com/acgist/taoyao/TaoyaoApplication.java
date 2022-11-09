package com.acgist.taoyao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@ComponentScan(basePackages = "com.acgist.taoyao")
@SpringBootApplication
@EnableAspectJAutoProxy(exposeProxy = true)
public class TaoyaoApplication {

	public static void main(String[] args) {
		SpringApplication.run(TaoyaoApplication.class, args);
	}

}
