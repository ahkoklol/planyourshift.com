package com.planyourshift;

import org.springframework.boot.SpringApplication;

public class TestPlanyourshiftApplication {

	public static void main(String[] args) {
		SpringApplication.from(PlanyourshiftApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
