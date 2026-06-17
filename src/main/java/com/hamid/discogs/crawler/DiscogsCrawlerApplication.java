package com.hamid.discogs.crawler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DiscogsCrawlerApplication {

	public static void main(String[] args) {
		SpringApplication.run(DiscogsCrawlerApplication.class, args);
	}

}
