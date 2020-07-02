package kz.mircella.multithreading.livelock;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class LivelockDemo {

    public static Random random = new Random(System.currentTimeMillis());
    public static Queue<DatabaseRecord> databaseRecords = new LinkedList<>();

    public static void main(String[] args) {
        var records = IntStream.range(0, 10000).boxed().map(it -> new DatabaseRecord("id_" + random.nextInt(10000))).collect(Collectors.toSet());
        databaseRecords.addAll(records);

        Lock databaseConnection_1 = new ReentrantLock();
        Lock databaseConnection_2 = new ReentrantLock();

        // potential deadlock - locks are obtained not in the priority order
        new SoftwareEngineer("Jane", databaseConnection_1, databaseConnection_2, databaseRecords).start();
        new SoftwareEngineer("Mike", databaseConnection_2, databaseConnection_1, databaseRecords).start();
    }
}

class DatabaseRecord {
    String id;

    public DatabaseRecord(String id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DatabaseRecord)) return false;
        DatabaseRecord databaseRecord = (DatabaseRecord) o;
        return id.equals(databaseRecord.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return id;
    }
}

class SoftwareEngineer extends Thread {

    private Random random;
    private String name;
    private Lock databaseConnection1;
    private Lock databaseConnection2;
    private Queue<DatabaseRecord> databaseRecords;

    public SoftwareEngineer(String name, Lock databaseConnection1, Lock databaseConnection2, Queue<DatabaseRecord> databaseRecords) {
        this.databaseConnection1 = databaseConnection1;
        this.databaseConnection2 = databaseConnection2;
        this.databaseRecords = databaseRecords;
        this.name = name;
        this.random = new Random(System.currentTimeMillis());
    }

    @Override
    public void run() {
        List<DatabaseRecord> processed = new ArrayList<>();
        while (!databaseRecords.isEmpty()) {
            try {
                databaseConnection1.lock();
                // to prevent deadlock without carrying of priority of locks - tits possible to use tryLock() instead of lock() on the second lock
                // tryLock() -> boolean (true - if lock was occupied and failed to acquire the lock) -> if its busy - release the first lock
                if (!databaseConnection2.tryLock()) {
                    databaseConnection1.unlock(); // release first lock for some other thread
                    System.out.println(this.name + " failed to acquire lock 2, its already acquired ");
                    // potential live lock - thread is constantly trying to acquire the lock, then fails and tries again
                    // to prevent live lock - use random waiting time before acquiring the lock net time
                    Thread.sleep(1000 * random.nextInt(30));
                } else {
                    if (!databaseRecords.isEmpty()) {
                        DatabaseRecord databaseRecord = databaseRecords.poll();
                        processed.add(databaseRecord);
                        System.out.println(this.name + " processed 1 database record " + databaseRecord.id + ", records remained: " + databaseRecords.size());
                    }
                    System.out.println(this.name + " processed " + processed.size() + " records: " + processed);
                }
            } catch (Exception e) {
                // always unlock locks in the same order they were locked!
                databaseConnection2.unlock();
                databaseConnection1.unlock();
            }
        }
    }
}