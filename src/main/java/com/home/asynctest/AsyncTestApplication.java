package com.home.asynctest;

import io.netty.channel.nio.NioEventLoopGroup;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.Netty4ClientHttpRequestFactory;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.async.DeferredResult;

@SpringBootApplication
@Slf4j
public class AsyncTestApplication {

    @RestController
    public static class MyController {

        //사용하는 thread 개수를 1개로 지정
        AsyncRestTemplate rt = new AsyncRestTemplate(new Netty4ClientHttpRequestFactory(new NioEventLoopGroup(1)));

        @GetMapping("/rest")
        public DeferredResult<String> rest(String idx) {

            DeferredResult<String> dr = new DeferredResult<>();

            //콜백을 작성할 필요가 없다.
            ListenableFuture<ResponseEntity<String>> f1 = rt.getForEntity("http://localhost:8081/service?req={req}", String.class, "hello" + idx);
            //f1.get() 이렇게 값을 가져오면 blocking 되기 때문에 의미 없다

            f1.addCallback(s->{

                dr.setResult(s.getBody() + "/work");

            }, e-> {

                dr.setErrorResult(e.getMessage());
            });

            return dr;
        }
    }

    /*
	//백그라운드에 쓰레드를 100개 만들어서 처리함. 바람직하지 않다.
	@RestController
	public static class MyController {

		AsyncRestTemplate rt = new AsyncRestTemplate();

		@GetMapping("/rest")
		public ListenableFuture<ResponseEntity<String>> rest(String idx) {

			//콜백을 작성할 필요가 없다.
	 		return rt.getForEntity("http://localhost:8081/service?req={req}", String.class, "hello" + idx);
		}
	}
	*/


	/*
	//최초 동기식 버전. RemoteService를 100번 호출하지만 각각 2초씩 필요하기 때문에 시간이 너무 오래걸림
	@RestController
	public static class MyController {

		RestTemplate rt = new RestTemplate();

		@GetMapping("/rest")
		public String rest(String idx) {
			String res = rt.getForObject("http://localhost:8081/service?req={req}", String.class, "hello" + idx);
			return res;
		}
	}
	*/

	public static void main(String[] args) {
		SpringApplication.run(AsyncTestApplication.class, args);
	}

}

