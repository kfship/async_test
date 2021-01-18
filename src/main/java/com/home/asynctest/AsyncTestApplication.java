package com.home.asynctest;

import io.netty.channel.nio.NioEventLoopGroup;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.Netty4ClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.context.request.async.DeferredResult;

@SpringBootApplication
@Slf4j
@EnableAsync
public class AsyncTestApplication {

    @RestController
    public static class MyController {

        //사용하는 thread 개수를 1개로 지정
        AsyncRestTemplate rt = new AsyncRestTemplate(new Netty4ClientHttpRequestFactory(new NioEventLoopGroup(1)));

        @Autowired
        MyService myService;

        @GetMapping("/rest")
        public DeferredResult<String> rest(String idx) {

            final String URL1 = "http://localhost:8081/service?req={req}";
            final String URL2 = "http://localhost:8081/service2?req={req}";

            DeferredResult<String> dr = new DeferredResult<>();

            ListenableFuture<ResponseEntity<String>> f1 = rt.getForEntity(URL1, String.class, "hello" + idx);
            //f1.get() 이렇게 값을 가져오면 blocking 되기 때문에 의미 없다

            f1.addCallback(s->{
                ListenableFuture<ResponseEntity<String>> f2 = rt.getForEntity(URL2, String.class, s.getBody());
                f2.addCallback(s2->{
                    ListenableFuture<String> f3 = myService.work(s2.getBody());
                    f3.addCallback(s3->{
                        dr.setResult(s3);
                    }, e-> {
                        dr.setErrorResult(e.getMessage());
                    });
                }, e->{
                    dr.setErrorResult(e.getMessage());
                });
            }, e-> {
                dr.setErrorResult(e.getMessage());
            });

            return dr;
        }
    }

    @Service
    public static class MyService {
        @Async
        public ListenableFuture<String> work(String req) {
            return new AsyncResult<>(req + "/asyncwork");
        }
    }

    @Bean
    public ThreadPoolTaskExecutor myThreadPool() {
        ThreadPoolTaskExecutor te = new ThreadPoolTaskExecutor();
        te.setCorePoolSize(1);
        te.setMaxPoolSize(10);
        te.initialize();
        return te;
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

