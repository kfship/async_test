package com.home.asynctest;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

@Slf4j
public class CFuture {

    public static void main(String[] args) throws ExecutionException, InterruptedException {

        ExecutorService es = Executors.newFixedThreadPool(10);


        CompletableFuture
                //.runAsync(() -> log.info("runAsync"))  //runnable을 구현하기 때문에 return값이 없다. 결과를 받아서 처리하는게 불가하다
                .supplyAsync(() -> {
                    log.info("runAsync");
                    //if(1 == 1) throw new RuntimeException();
                    return 1;
                }, es)  //각 각 다른 쓰레드에서 실행1
                .thenCompose(s -> {
                    log.info("thenApply {}", s);
                    return CompletableFuture.completedFuture(s + 1);  //값이 아니라 CompletableFuture 형태로 리턴
                })
                .thenApplyAsync(s2 -> {
                    log.info("thenApply {}", s2);
                    return s2 * 3;
                }, es) //각 각 다른 쓰레드에서 실행2
                .exceptionally(e -> -10) //어디서든 Exception이 발생하면 -10으로 리턴한다
                .thenAcceptAsync(s3 -> log.info("thenAccept {}", s3), es); //각 각 다른 쓰레드에서 실행3

//        CompletableFuture
//                //.runAsync(() -> log.info("runAsync"))  //runnable을 구현하기 때문에 return값이 없다. 결과를 받아서 처리하는게 불가하다
//                .supplyAsync(() -> {
//                    log.info("runAsync");
//                    //if(1 == 1) throw new RuntimeException();
//                    return 1;
//                })
//                .thenCompose(s -> {
//                    log.info("thenApply {}", s);
//                    return CompletableFuture.completedFuture(s + 1);  //값이 아니라 CompletableFuture 형태로 리턴
//                })
//                .thenApply(s2 -> {
//                    log.info("thenApply {}", s2);
//                    return s2 * 3;
//                })
//                .exceptionally(e -> -10) //어디서든 Exception이 발생하면 -10으로 리턴한다
//                .thenAccept(s3 -> log.info("thenAccept {}", s3));

        log.info("exit");

        ForkJoinPool.commonPool().shutdown();
        ForkJoinPool.commonPool().awaitTermination(10, TimeUnit.SECONDS);
    }
}
