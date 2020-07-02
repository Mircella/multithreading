package kz.mircella.multithreading.abandonedlock;

import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class AbandonedLockDemo {
    public static Random random = new Random(System.currentTimeMillis());
    public static Queue<DatabaseRecord> databaseRecords = new LinkedList<>();

    public static void main(String[] args) {
        var records = IntStream.range(0, 20).boxed().map(it -> new DatabaseRecord(random.nextInt(10000) + "-database-record")).collect(Collectors.toSet());
        databaseRecords.addAll(records);

        Lock databaseConnection_1 = new ReentrantLock();
        Lock databaseConnection_2 = new ReentrantLock();
        Lock databaseConnection_3 = new ReentrantLock();

        // to prevent deadlock - acquire locks in some prioritized order
        new SoftwareEngineer("Jane", databaseConnection_1, databaseConnection_2, databaseRecords).start();
        new SoftwareEngineer("Mike", databaseConnection_2, databaseConnection_3, databaseRecords).start();
        new SoftwareEngineer("Anna", databaseConnection_1, databaseConnection_3, databaseRecords).start();
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
}

class SoftwareEngineer extends Thread {

    private String name;
    private Random random;
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
        while (!databaseRecords.isEmpty()) {
            try {
                databaseConnection1.lock();
                databaseConnection2.lock();

                if (!databaseRecords.isEmpty()) {
                    int seconds = 0;
                    try {
                        seconds = random.nextInt(3);
                        Thread.sleep(1000 * seconds);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    DatabaseRecord databaseRecord = databaseRecords.poll();
                    System.out.println(this.name + " processed 1 database record " + databaseRecord.id + " for " + seconds + " seconds, records remained: " + databaseRecords.size());
                    if (databaseRecord.id.contains("2")) {
                        throw new NullPointerException();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                databaseConnection1.unlock();
                databaseConnection2.unlock();
            }
        }
    }
}
