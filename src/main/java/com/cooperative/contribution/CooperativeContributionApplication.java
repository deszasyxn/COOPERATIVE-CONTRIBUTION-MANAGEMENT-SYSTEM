package com.cooperative.contribution;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
public class CooperativeContributionApplication {

    private static final Logger logger = LoggerFactory.getLogger(CooperativeContributionApplication.class);

    public static void main(String[] args) {
        logger.debug("Starting cooperative contribution application");
        SpringApplication.run(CooperativeContributionApplication.class, args);
    }
}
