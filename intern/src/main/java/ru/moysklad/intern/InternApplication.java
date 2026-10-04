package ru.moysklad.intern;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class InternApplication {
	public static void main(String[] args) {
		ApplicationContext context = SpringApplication.run(InternApplication.class, args);
	}
}
