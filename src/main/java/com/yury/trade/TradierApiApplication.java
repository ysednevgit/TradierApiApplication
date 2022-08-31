package com.yury.trade;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class TradierApiApplication {

	public static void main(String[] args) {
		//SpringApplication.run(TradierApiApplication.class, args);
		SpringApplicationBuilder builder = new SpringApplicationBuilder(TradierApiApplication.class);

		builder.headless(false);

		ConfigurableApplicationContext context = builder.run(args);
	}

}
