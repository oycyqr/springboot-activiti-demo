package com.oyc.activiti;

import org.activiti.spring.boot.SecurityAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author oyc
 */
@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
public class HolidayApplication {

    public static void main(String[] args) {
        SpringApplication.run(HolidayApplication.class, args);
    }

}
