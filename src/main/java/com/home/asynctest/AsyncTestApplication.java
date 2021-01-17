package com.home.asynctest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@Slf4j
public class AsyncTestApplication {

	@RestController
	public static class MyController {

		RestTemplate rt = new RestTemplate();

		@GetMapping("/rest")
		public String rest(String idx) {
			String res = rt.getForObject("http://localhost:8081/service?req={req}", String.class, "hello" + idx);
			return res;
		}
	}

	public static void main(String[] args) {
		SpringApplication.run(AsyncTestApplication.class, args);
	}

}

