package org.deliverygo.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;


@Configuration
public class DemoDataLoader {

    @Bean CommandLineRunner demo(){
    return args-> {
        System.out.println("Hello World");
    };
    }
}
