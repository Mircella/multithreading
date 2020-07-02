package kz.mircella.multithreading.racecondition;

import java.util.Collections;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class RaceConditionDemo {

    public static void main(String[] args) throws Exception {
        Lock entrance = new ReentrantLock();
        String[] names = {"Jane", "Peter", "Daniel"};
        var customers = IntStream.range(0, 400).boxed().map(it -> new Customer(names[it % 3] + "_" + it, entrance)).collect(Collectors.toList());
        Collections.shuffle(customers);
        for (Customer it : customers) {
            it.start();
        }
        for (Customer customer : customers) {
            customer.join();
        }
    }
}

class Customer extends Thread {

    private String name;
    private Lock entrance;

    public Customer(String name, Lock entrance) {
        this.name = name;
        this.entrance = entrance;
    }

    @Override
    public void run() {
        entrance.lock();
        try {
            Shop.getInstance().enter();
            buy();
            System.out.println(this.name + " added items:" + Shop.items);
            Shop.getInstance().leave();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            entrance.unlock();
        }
    }

    public void buy() {
        if (this.name.contains("Jane")) {
            Shop.items += 2;
        } else if (this.name.contains("Peter")) {
            Shop.items += 5;
        } else {
            Shop.items += 1;
        }
    }

}

class Shop {

    public static int items = 0;
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