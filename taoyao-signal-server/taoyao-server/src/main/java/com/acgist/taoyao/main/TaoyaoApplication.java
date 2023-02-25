package com.acgist.taoyao.main;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan(basePackages = "com.acgist.taoyao")
@SpringBootApplication
public class TaoyaoApplication {

	public static void main(String[] args) {
		SpringApplication.run(TaoyaoApplication.class, args);
//      System.exit(SpringApplication.exit(SpringApplication.run(TaoyaoApplication.class, args)));
	}

}
