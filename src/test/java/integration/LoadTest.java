package integration;

import org.junit.Before;
import org.junit.Test;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by wongb on 4/7/17.
 */
public class LoadTest {
    List<IntegrationTest> integrationTests = new ArrayList<>();

    @Before
    public void setup(){
        for (int i = 0; i < 10; i++){
            IntegrationTest test = new IntegrationTest();
            integrationTests.add(test);
            test.setup();
        }
    }

    @Before
    public void tearDown() throws InterruptedException, ConnectException {
        for (IntegrationTest test : integrationTests){
            test.cleanup();
        }
    }

    @Test
    public void integrationTest() throws Exception {
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < integrationTests.size(); i++){
            IntegrationTest test = integrationTests.get(i);

            int finalI = i;
            Thread t = new Thread(()->{
                Thread.currentThread().setName("Integration-Test-Thread-" + finalI);
                try {
                    test.integrationTest();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            t.start();
            threads.add(t);
        }

        for (Thread t : threads){
            t.join();
        }
    }
}
