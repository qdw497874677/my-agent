package io.github.pi_java.agent.adapter.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(scanBasePackages = "io.github.pi_java.agent")
public class PiCloudServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(PiCloudServerApplication.class, args);
    }
}
