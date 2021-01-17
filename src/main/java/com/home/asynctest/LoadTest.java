package com.home.asynctest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StopWatch;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class LoadTest {

    static AtomicInteger counter = new AtomicInteger(0);

    public static void main(String[] args) throws InterruptedException, BrokenBarrierException {

        ExecutorService es = Executors.newFixedThreadPool(100);

        RestTemplate rt = new RestTemplate();
        String url = "http://localhost:8080/rest";

        //경계 생성
        CyclicBarrier barrier = new CyclicBarrier(101);

        for(int i = 0 ; i < 100 ; i++) {

            es.submit(() -> {
                int idx = counter.addAndGet(1);

                // Thread가 이 지점에 도달하면 blocking 됨. 위에서 설정한 101번째 까지.
                // 100번이 넘어가면 blocking이 한거번에 해제됨
                barrier.await();

                log.info("Thread {}", idx);

                StopWatch sw = new StopWatch();
                sw.start();

                rt.getForObject(url, String.class);

                sw.stop();
                log.info("Elapsed({}): {}", idx, sw.getTotalTimeSeconds());
                return null;
            });
        }

        barrier.await();
        StopWatch main = new StopWatch();
        main.start();

        // 이미 시작된 Task는 실행 하고, 새로운 작업은 받지 않음
        es.shutdown();

        // 설정한 시간까지 모든 작업이 종료되기를 기다린다.
        // 설정한 시간이 지나면 해당 스레드는 interrupted 된다.
        es.awaitTermination(100, TimeUnit.SECONDS);

        main.stop();
        log.info("Total: {}", main.getTotalTimeSeconds());
    }
}

