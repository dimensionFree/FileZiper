import utils.IThreadPool;
import utils.ThreadPoolImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class threadTestMain {
    public static void main(String[] args) {
//        //获取线程池
//        IThreadPool t = ThreadPoolImpl.getThreadPool(20);
//
//        List<Runnable> taskList = new ArrayList<Runnable>();
//        for (int i = 0; i < 100; i++) {
//            taskList.add(new Task());
//        }
//        //执行任务
//        t.execute(taskList);
//        System.out.println(t);
//        //销毁线程
//        t.destroy();
//        System.out.println(t);

        String a="aaa111aaa";
        String replace =a.replaceFirst("aaa", "bbb");
        System.out.println(replace);
    }

    static class Task implements Runnable {

        private static volatile int i = 1;

        @Override
        public void run() {
            Object o = new Object();
            System.out.println("当前处理的线程:" + Thread.currentThread().getName() + " 执行任务" + (i++) + " 完成");
        }
    }
}
