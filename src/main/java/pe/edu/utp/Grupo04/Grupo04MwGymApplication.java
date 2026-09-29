package pe.edu.utp.Grupo04;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = { org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class })
public class Grupo04MwGymApplication {

	public static void main(String[] args) {
		SpringApplication.run(Grupo04MwGymApplication.class, args);
	}

}
