package gov.cdc.prime.router.resender;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FailedReportResenderApplication {

	public static void main(String[] args) {
		SpringApplication.run(FailedReportResenderApplication.class, args).close();
	}

}
