package kz.mircella.multithreading;

import java.util.Collections;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class BarrierDemo {
    public static void main(String[] args) throws Exception {
        String[] names = {"Jane", "Daniel"};
        var customers = IntStream.range(0, 10).boxed().map(it -> new Shopper(names[it % 3] + "_" + it)).collect(Collectors.toList());
        Collections.shuffle(customers);
        for (var it : customers) {
            it.start();
        }
        for (var customer : customers) {
            customer.join();
        }
    }
}

class Shopper extends Thread {

    public static int items = 0;
    private static Lock pocketBook = new ReentrantLock();
    // cyclic as it can be reset to the initial state/ reuse barrier 
    private static CyclicBarrier cyclicBarrier = new CyclicBarrier(10);

    public Shopper(String name) {
        this.setName(name);
    }

    @Override
    public void run() {
        pocketBook.lock();
        try {
            int boughtItems = buy();
            System.out.println(this.getName() + " bought " + boughtItems + " items");
            System.out.println("There is " + items + " items in general");
        } finally {
            pocketBook.unlock();
        }
    }

    public int buy() {
        if (getName().contains("Jane")) {
            items += 2;
            return 2;
        } else {
            items += 1;
            return 1;
        }
    }
}