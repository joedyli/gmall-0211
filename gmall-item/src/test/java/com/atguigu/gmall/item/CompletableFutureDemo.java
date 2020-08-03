package com.atguigu.gmall.item;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class CompletableFutureDemo {

    public static void main(String[] args) throws IOException {

        CompletableFuture<String> afuture = CompletableFuture.supplyAsync(() -> {

            try {
                System.out.println("初始化了一个A任务1");
                Thread.sleep(3000);
                System.out.println("A任务执行完成2");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "A";
        });
        CompletableFuture<Void> bcompletableFuture = afuture.thenAcceptAsync(t -> {
            try {
                System.out.println("初始化了B任务！3");
                Thread.sleep(2000);
                System.out.println("B任务执行完成4");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        CompletableFuture<Void> ccompletableFuture = afuture.thenAcceptAsync(t -> {
            try {
                System.out.println("初始化了C任务！5");
                Thread.sleep(2500);
                System.out.println("C任务执行完成6");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        CompletableFuture<Void> dcompletableFuture = afuture.thenAcceptAsync(t -> {
            try {
                System.out.println("初始化了d任务！5");
                Thread.sleep(4000);
                System.out.println("d任务执行完成6");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        CompletableFuture<Void> ecompletableFuture = CompletableFuture.runAsync(() -> {
            try {
                System.out.println("初始化了e任务！5");
                Thread.sleep(5000);
                System.out.println("e任务执行完成6");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        CompletableFuture.anyOf(bcompletableFuture, ccompletableFuture, dcompletableFuture, ecompletableFuture).join();
        System.out.println("所有任务执行完成了。。。。。。");

        System.in.read();

//        CompletableFuture.supplyAsync(() -> {
//            System.out.println("处理子任务。。。。。supplyAsync方法");
//            //int i = 1/0;
//            return "hello CompletableFuture!";
//        }).thenApplyAsync(t -> {
//            System.out.println("串行化方法，可以获取上一个任务的返回结果：" + t);
//            return "hello thenApplyAsync";
//        }).thenAcceptAsync(t -> {
//            System.out.println("又串一个任务，上一个任务的返回结果：" + t);
//        }).thenRunAsync(() -> {
//            System.out.println("不获取上一个任务的返回结果，也没有自己的返回结果，只要上一个任务执行完成，就执行该方法。");
//        }).whenCompleteAsync((t, u) -> { // 处理正常及异常任务信息
//            System.out.println("上一个任务处理完成，开始处理新任务！");
//            System.out.println("上一个任务的返回结果t: " + t);
//            System.out.println("上一个任务的异常信息u: " + u);
//        });

//                .exceptionally(t -> {
//            System.out.println("上一个任务出现异常t: " + t);
//            return "hello exceptionally!";
//        });

//        CompletableFuture.runAsync(() -> {
//            System.out.println("偷偷的执行了一个任务，没有任何返回结果集。。");
//        }).whenCompleteAsync((t, u) -> {
//            System.out.println("..............");
//        });
    }
}
