package ru.moysklad.intern;

import org.springframework.boot.SpringApplication;

public class TestInternApplication {

	public static void main(String[] args) {
		SpringApplication.from(InternApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
