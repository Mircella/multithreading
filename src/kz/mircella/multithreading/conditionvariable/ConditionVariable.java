package kz.mircella.multithreading.conditionvariable;

import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ConditionVariable {

    public static void main(String[] args) {
        Lock databaseConnection = new ReentrantLock();
        Condition databaseRecordProcessed = databaseConnection.newCondition();
        IntStream.range(0, 5).boxed().map(it -> {
            return new SoftwareEngineer(it, databaseConnection, databaseRecordProcessed);
        }).forEach(Thread::start);
    }
}

class Database {

    private static Database instance = new Database();
    private Queue<DatabaseRecord> databaseRecords;
    private Random random;

    private Database() {
        random = new Random(System.currentTimeMillis());
        databaseRecords = new LinkedList<>();
        var records = IntStream.range(0, 30).boxed().map(it -> new DatabaseRecord("id_" + random.nextInt(10000))).collect(Collectors.toSet());
        databaseRecords.addAll(records);
    }

    public static Database getInstance() {
        return instance;
    }

    public DatabaseRecord process() {
        return databaseRecords.poll();
    }

    public int count() {
        return databaseRecords.size();
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

    private int id;
    private Lock databaseConnection;
    // to prevent situation when thread is waiting all the time for lock to be released
    // mutex.lock() -> while condition is false -> !!!condition.await for another while loop iteration!!! -> else execute critical section -> !!!signal to condition that your work is done!!! -> release mutex
    // condition - not event or condition that should happen that thread can execute its code, its part of logic of while loop whether its turn of thread to execute its logic
    // condition.await() - waiting until ANOTHER thread signal that its turn of current thread
    private Condition databaseRecordProcessed; // this condition should be the same object for all waiting threads

    public SoftwareEngineer(int id, Lock databaseConnection, Condition databaseRecordProcessed) {
        this.id = id;
        this.databaseConnection = databaseConnection;
        this.databaseRecordProcessed = databaseRecordProcessed;
    }

    @Override
    public void run() {
        while (Database.getInstance().count() > 0) {

            // acquire lock on database
            databaseConnection.lock();
            try {
                // its not current thread's turn
                while (id != Database.getInstance().count() % 2 && Database.getInstance().count() > 0) {
                    System.out.println(this.id + " " + " is waiting for his turn");
                    // await until database record processed, so that then thread can check if its turn
                    databaseRecordProcessed.await();
                }
                // after successful waiting - process the database record
                if (Database.getInstance().count() > 0) {
                    DatabaseRecord databaseRecord = Database.getInstance().process();
                    System.out.println(this.id + " " + " processed 1 database record " + databaseRecord.id + ", records remained: " + Database.getInstance().count());

                    // wakes up sleeping thread
                    databaseRecordProcessed.signalAll();
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                // always unlock locks in the finally block
                databaseConnection.unlock();
            }
        }
    }
}