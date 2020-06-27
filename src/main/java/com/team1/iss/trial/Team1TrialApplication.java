package com.team1.iss.trial;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.team1.iss.trial.domain.Manager;

@SpringBootApplication
public class Team1TrialApplication {

	public static void main(String[] args) {
		SpringApplication.run(Team1TrialApplication.class, args);
		Manager manager=new Manager();
	}

}
