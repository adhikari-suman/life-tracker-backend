package com.lifetracker.life_tracker_backend;

import org.springframework.boot.SpringApplication;

public class TestLifeTrackerBackendApplication {

	public static void main(String[] args) {
		SpringApplication.from(LifeTrackerBackendApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
