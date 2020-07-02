package kz.mircella.multithreading.starvation;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class StarvationDemo {

    public static Random random = new Random(System.currentTimeMillis());
    public static Queue<DatabaseRecord> databaseRecords = new LinkedList<>();

    public static void main(String[] args) {
        var records = IntStream.range(0, 10000).boxed().map(it -> new DatabaseRecord("id_" + random.nextInt(10000))).collect(Collectors.toSet());
        databaseRecords.addAll(records);

        Lock databaseConnection_1 = new ReentrantLock();
        Lock databaseConnection_2 = new ReentrantLock();

        // to prevent starvation and evenly assign work among threads - use the same locks for all threads + random waiting time
        new SoftwareEngineer("Jane", databaseConnection_1, databaseConnection_2, databaseRecords).start();
        // thread that has to struggle the least for locks will obtain more resources to execute i ts work
        new SoftwareEngineer("Mike", databaseConnection_1, databaseConnection_2, databaseRecords).start();
        new SoftwareEngineer("Anna", databaseConnection_1, databaseConnection_2, databaseRecords).start();
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
        List<DatabaseRecord> processed = new ArrayList<>();
        while (!databaseRecords.isEmpty()) {
            try {
                databaseConnection1.lock();
                databaseConnection2.lock();

                int seconds;
                try {
                    seconds = random.nextInt(3);
                    Thread.sleep(seconds);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (!databaseRecords.isEmpty()) {
                    DatabaseRecord databaseRecord = databaseRecords.poll();
                    processed.add(databaseRecord);
                    System.out.println(this.name + " processed 1 database record " + databaseRecord.id + ", records remained: " + databaseRecords.size());
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                databaseConnection1.unlock();
                databaseConnection2.unlock();
            }
        }
        System.out.println(this.name + " processed " + processed.size() + " records: " + processed);
    }
}