import java.util.concurrent.atomic.AtomicLong;

public class testMain {
    static class Counter {
        private AtomicLong  c = new AtomicLong(0);

        public void increment() {
            c.getAndIncrement();
        }

        public long value() {
            return c.get();
        }
//更多请阅读：https://www.yiibai.com/java_concurrency/concurrency_atomiclong.html


        public static void main(String[] args) throws InterruptedException {
            final Counter counter = new Counter();
            //1000 threads
            for(int i = 0; i < 1000 ; i++) {
                new Thread(new Runnable() {
                    public void run() {
                        counter.increment();
                    }
                }).start();
            }
            Thread.sleep(6000);

            System.out.println("Final number (should be 1000): " + counter.value());
//更多请阅读：https://www.yiibai.com/java_concurrency/concurrency_atomiclong.html


        }

    }
}
