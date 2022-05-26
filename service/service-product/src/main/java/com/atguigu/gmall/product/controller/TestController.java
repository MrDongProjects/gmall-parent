package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.product.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

@RestController
@RequestMapping("/admin/product/test")
public class TestController {
    @Autowired
    private TestService testService;

    @GetMapping("/testLock")
    public synchronized Result testLock() {
        testService.testLock();
        return Result.ok();
    }

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(new Supplier<Integer>() {
                    @Override
                    public Integer get() {
                        System.out.println(Thread.currentThread().getName() + "\t CompletableFuture");
//                int i = 10/0;
                        return 1024;
                    }
                })
//                .whenComplete(new BiConsumer<Object, Throwable>() {
//            @Override
//            public void accept(Object o, Throwable throwable) {
//                System.out.println("--------o:"+o.toString());
//                System.out.println("--------throwable:"+throwable);
//            }
//        }).exceptionally(new Function<Throwable, Object>() {
//            @Override
//            public Object apply(Throwable throwable) {
//                System.out.println("throwable = " + throwable);
//                return 666;
//            }
//        });

                .thenApply(new Function<Integer, Integer>() {
                    @Override
                    public Integer apply(Integer o) {
                        System.out.println("thenApply方法，上次返回结果：" + o);
                        return o * 2;

                    }
                }).whenComplete(new BiConsumer<Integer, Throwable>() {
                    @Override
                    public void accept(Integer o, Throwable throwable) {
                        System.out.println("-------o=" + o);
                        System.out.println("-------throwable=" + throwable);

                    }
                }).exceptionally(new Function<Throwable, Integer>() {
                    @Override
                    public Integer apply(Throwable throwable) {
                        System.out.println("throwable=" + throwable);
                        return 6666;

                    }

                });

        System.out.println(future.get());
    }

    ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(50, 500, 30, TimeUnit.SECONDS, new ArrayBlockingQueue<>(10000));
    // 线程1执行返回的结果：hello
    CompletableFuture<String> futureA = CompletableFuture.supplyAsync(() -> "hello");

    // 线程2 获取到线程1执行的结果
    CompletableFuture<Void> futureB = futureA.thenAcceptAsync((s) -> {
        delaySec(1);
        printCurrTime(s+" 第一个线程");
    }, threadPoolExecutor);

    CompletableFuture<Void> futureC = futureA.thenAcceptAsync((s) -> {
        delaySec(3);
        printCurrTime(s+" 第二个线程");
    }, threadPoolExecutor);


    private static void printCurrTime(String str) {
        System.out.println(str);
    }

    private static void delaySec(int i) {
        try {
            Thread.sleep(i * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


}
