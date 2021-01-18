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

import java.util.function.Consumer;
import java.util.function.Function;

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


//            ListenableFuture<ResponseEntity<String>> f1 = rt.getForEntity(URL1, String.class, "hello" + idx);
//            //f1.get() 이렇게 값을 가져오면 blocking 되기 때문에 의미 없다
//
//            f1.addCallback(s->{
//                ListenableFuture<ResponseEntity<String>> f2 = rt.getForEntity(URL2, String.class, s.getBody());
//                f2.addCallback(s2->{
//                    ListenableFuture<String> f3 = myService.work(s2.getBody());
//                    f3.addCallback(s3->{
//                        dr.setResult(s3);
//                    }, e-> {
//                        dr.setErrorResult(e.getMessage());
//                    });
//                }, e->{
//                    dr.setErrorResult(e.getMessage());
//                });
//            }, e-> {
//                dr.setErrorResult(e.getMessage());
//            });

            DeferredResult<String> dr = new DeferredResult<>();

            Completion
                    .from(rt.getForEntity(URL1, String.class, "hello" + idx))
                    .andApply(s->rt.getForEntity(URL2, String.class, s.getBody()))
                    .andApply(s->myService.work(s.getBody()))
                    .andError(e->dr.setErrorResult(e.toString()))
                    .andAccept(s->dr.setResult(s));

            return dr;
        }
    }

    public static class AcceptCompletion<S> extends Completion<S, Void> {

        Consumer<S> con;
        public AcceptCompletion(Consumer<S> con) {
            this.con = con;
        }

        @Override
        void run(S value) {
            con.accept(value);
        }
    }

    public static class ErrorCompletion<T> extends Completion<T, T> {

        Consumer<Throwable> econ;
        public ErrorCompletion(Consumer<Throwable> econ) {
            this.econ = econ;
        }

        @Override
        void run(T value) {
            if(next != null) next.run(value);
        }

        @Override
        void error(Throwable e) {
            econ.accept(e);
        }
    }

    public static class ApplyCompletion<S, T> extends Completion<S, T> {

        Function<S, ListenableFuture<T>> fn;
        public ApplyCompletion(Function<S, ListenableFuture<T>> fn) {
            this.fn = fn;
        }

        @Override
        void run(S value) {
            ListenableFuture<T> lf = fn.apply(value);
            lf.addCallback(s->complete(s), e->error(e));
        }
    }

    @Service
    public static class Completion<S, T> {

        Completion next;

        public void andAccept(Consumer<T> con) {

            Completion<T, Void> c = new AcceptCompletion<>(con);
            this.next = c;
        }

        public Completion<T, T> andError(Consumer<Throwable> econ) {

            Completion<T, T> c = new ErrorCompletion<>(econ);
            this.next = c;

            return c;
        }

        public <V> Completion<T, V> andApply(Function<T, ListenableFuture<V>> fn) {

            Completion<T, V> c = new ApplyCompletion<>(fn);
            this.next = c;

            return c;
        }

        public static <S, T> Completion<S, T> from(ListenableFuture<T> lf) {

            Completion<S, T> c = new Completion<>();

            lf.addCallback(s->{
                c.complete(s);
            }, e->{
               c.error(e);
            });

            return c;
        }

        void error(Throwable e) {

            if(next != null) next.error(e);
        }

        void complete(T s) {
            if(next != null) next.run(s);
        }

        void run(S value) {

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

