package kz.mircella.multithreading.semaphore;

import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;

public class SemaphoreDemo {

    public static void main(String[] args) {

        for (int i = 0; i < 20; i++) {
            new Customer(i).start();
        }
    }
}

class Customer extends Thread {

    private String id;

    public Customer(int id) {
        this.id = "user_" + id;
    }

    @Override
    public void run() {
        try {
            Shop.getInstance().enter();
            System.out.println(this.id + " entered the shop");
            int timeForShopping = ThreadLocalRandom.current().nextInt(1000, 5000);
            Thread.sleep(timeForShopping);
            Shop.getInstance().leave();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println(this.id + " finished shopping ");
        }
    }

}

class Shop {

    private static Shop instance = new Shop();
    // semaphore allows limited number of thread to have access to the object, thread MUST release semaphore after finishing its work!
    // if permits = 1 -> semaphore is binary -> acts as mutex
    // if permits > 1 -> counting semaphore
    private static Semaphore allowedCustomerNumber = new Semaphore(5);

    private Shop() {
    }

    public static Shop getInstance() {
        return instance;
    }

    public void enter() {
        try {
            allowedCustomerNumber.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void leave() {
        allowedCustomerNumber.release();
    }
}