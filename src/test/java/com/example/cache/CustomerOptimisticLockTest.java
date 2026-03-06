package com.example.cache;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class CustomerOptimisticLockTest {

    @Autowired
    private CustomerRepository repository;

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
            latch.await();

            Customer c = repository.findById(id).orElseThrow();
            c.setName("Updated");

            repository.saveAndFlush(c); // IMPORTANT
            return null;
        };

        Future<Void> future1 = executor.submit(task);
        Future<Void> future2 = executor.submit(task);

        // start both threads
        latch.countDown();

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

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

