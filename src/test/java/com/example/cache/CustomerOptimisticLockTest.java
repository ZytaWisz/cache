package com.example.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
public class CustomerOptimisticLockTest {

    @Autowired
    private CustomerRepository repository;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    @Test
    void shouldThrowOptimisticLockException() throws Exception {
        // given
        Customer customer = new Customer("John", "john@test.com");
        Customer saved = repository.save(customer);

        Long id = saved.getId();

        // Simulate two concurrent transactions
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(1);

        Callable<Void> task = () -> {
            latch.await();//this does not guarantee: both read before write

            Customer c = repository.findById(id).orElseThrow();
            c.setName("Updated");
            //the problem would occur if latch.await(); appeared here.
            // Here read is not synchronized,
            // which means that read will be executed at a different time
            repository.saveAndFlush(c); // IMPORTANT
            return null;
        };

        Future<Void> future1 = executor.submit(task);
        Future<Void> future2 = executor.submit(task);

       // threads can reach latch.await() at different times,
        // which means the synchronization moment is not always identical,
        // so using CountDownLatch may not be enough.
        // The problem would occur if latch.await(); appeared after findById() in line 34

        // start both threads
        latch.countDown();

        executor.shutdown();
        boolean finished = executor.awaitTermination(5, TimeUnit.SECONDS);

        assertThat(finished).isTrue();

        // One of them should fail
        boolean exceptionThrown = false;

        //check that one of the futures failed
        try {
            future1.get();
            future2.get();
        } catch (ExecutionException e) {
            exceptionThrown = true;
        }

        assertThat(exceptionThrown).isTrue();

        // One of the saves should throw OptimisticLockException
        // Final state should reflect only one update
        Customer result = repository.findById(id).orElseThrow();

        assertThat(result.getVersion()).isEqualTo(1L);
    }

    @Test
    void shouldThrowOptimisticLockExceptionWithCyclicBarrier() throws Exception {
        // given
        Customer customer = new Customer("John", "john@test.com");
        Customer saved = repository.save(customer);

        Long id = saved.getId();

        // Simulate two concurrent transactions
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CyclicBarrier barrier = new CyclicBarrier(2);

        Callable<Void> task = () -> {

            Customer c = repository.findById(id).orElseThrow();
            // both threads wait here
            barrier.await();

            c.setName("Updated");

            repository.saveAndFlush(c); // IMPORTANT
            return null;
        };

        Future<Void> future1 = executor.submit(task);
        Future<Void> future2 = executor.submit(task);

        executor.shutdown();
        boolean finished = executor.awaitTermination(5, TimeUnit.SECONDS);

        assertThat(finished).isTrue();

        // One of them should fail
        boolean exceptionThrown = false;

        //check that one of the futures failed
        try {
            future1.get();
            future2.get();
        } catch (ExecutionException e) {
            exceptionThrown = true;
        }

        assertThat(exceptionThrown).isTrue();

        // One of the saves should throw OptimisticLockException
        // Final state should reflect only one update
        Customer result = repository.findById(id).orElseThrow();

        assertThat(result.getVersion()).isEqualTo(1L);
    }

    @Test
    void shouldThrowOptimisticLockExceptionWithTwoCyclicBarrier() throws Exception {
        // given
        Customer customer = new Customer("John", "john@test.com");
        Customer saved = repository.save(customer);

        Long id = saved.getId();

        // Simulate two concurrent transactions
        ExecutorService executor = Executors.newFixedThreadPool(2);

        CyclicBarrier readBarrier = new CyclicBarrier(2);
        CyclicBarrier writeBarrier = new CyclicBarrier(2);

        //Two barriers ensure that:
        //both threads:
        //1. have read the same version
        //2. have modified the entity
        //3. are writing simultaneously

        //This is the ideal optimistic locking scenario.

        Callable<Void> task = () -> {

            Customer c = repository.findById(id).orElseThrow();
            // both threads wait here
            readBarrier.await();

            c.setName("Updated");

            writeBarrier.await();

            repository.saveAndFlush(c); // IMPORTANT
            return null;
        };

        Future<Void> future1 = executor.submit(task);
        Future<Void> future2 = executor.submit(task);

        executor.shutdown();
        boolean finished = executor.awaitTermination(5, TimeUnit.SECONDS);

        assertThat(finished).isTrue();

        // One of them should fail
        boolean exceptionThrown = false;

        //check that one of the futures failed
        try {
            future1.get();
            future2.get();
        } catch (ExecutionException e) {
            exceptionThrown = true;
        }

        assertThat(exceptionThrown).isTrue();

        // One of the saves should throw OptimisticLockException
        // Final state should reflect only one update
        Customer result = repository.findById(id).orElseThrow();

        assertThat(result.getVersion()).isEqualTo(1L);
    }
}

