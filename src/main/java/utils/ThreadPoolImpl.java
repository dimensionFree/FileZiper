package utils;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public class ThreadPoolImpl implements IThreadPool {
    //thread num
    public static Integer WORKER_NUMBER =10;
    //finished thread num;thread safe
    static volatile int sumCount=0;
    //task list;no thread safe;todo BlockingQueue
    static List<Runnable> taskQueue=new LinkedList<>();

    //线程工作组
    WorkerThread[] workThreads;
    //原子性
    static AtomicLong threadNum = new AtomicLong();

    static ThreadPoolImpl threadPool;

    //构造方法
    public ThreadPoolImpl() {
        this(WORKER_NUMBER);
    }

    public ThreadPoolImpl(int workerNum) {
        this.WORKER_NUMBER = workerNum;
        //开辟工作线程空间
        workThreads = new WorkerThread[WORKER_NUMBER];
        //开始创建工作线程
        for (int i = 0; i < WORKER_NUMBER; i++) {
            workThreads[i] = new WorkerThread();
            Thread thread = new Thread(workThreads[i], "ThreadPool-worker" + threadNum.incrementAndGet());
            System.out.println("初始化线程数" + (i + 1) + "---------当前线程名称:" + thread.getName());
            thread.start();
        }
    }

    @Override
    public String toString() {
        return "工作线程数量为" + WORKER_NUMBER
                + "已完成的任务数" + sumCount +
                "等待任务数量" + taskQueue.size();
    }


    //获取线程池
    public static IThreadPool getThreadPool() {
        return getThreadPool(WORKER_NUMBER);
    }

    public static IThreadPool getThreadPool(int workerNum) {
        //容错性，如果小于等于0就默认线程数
        if (workerNum <= 0) {
            workerNum = WORKER_NUMBER;
        }
        if (threadPool == null) {
            threadPool = new ThreadPoolImpl(workerNum);
        }
        return threadPool;
    }


    @Override
    public void execute(Runnable task) {
        synchronized (taskQueue) {
            taskQueue.add(task);
            taskQueue.notifyAll();
        }
    }

    @Override
    public void execute(Runnable[] tasks) {
        synchronized (taskQueue) {
            for (Runnable task : tasks) {
                taskQueue.add(task);
            }
            taskQueue.notifyAll();
        }
    }

    @Override
    public void execute(List<Runnable> tasks) {
        synchronized (taskQueue) {
            for (Runnable task : tasks) {
                taskQueue.add(task);
            }
            taskQueue.notifyAll();
        }
    }

    @Override
    public void destroy() {
        //循环是否还存在任务，如果存在等待20毫秒处理时间
        while (!taskQueue.isEmpty()) {
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        //如果任务队列已处理完成，销毁线程，清空任务
        for (int i = 0; i < WORKER_NUMBER; i++) {
            WorkerThread workThread = workThreads[i];
            if (Objects.nonNull( workThread)){
                workThread.setWorkerFlag();
                workThread = null;
            }

        }
        threadPool = null;
        taskQueue.clear();
    }

    //创建工作线程池
    class WorkerThread extends Thread {
        //用来标识当前线程属于活动可用状态
        private boolean isRunning = true;

        @Override
        public void run() {
            Runnable runnable = null;
            //死循环
            while (isRunning) {
                //非线程安全，所以采用同步锁
                synchronized (taskQueue) {
                    while (isRunning && taskQueue.isEmpty()) {
                        try {
                            //如果任务队列为空，等待20毫秒 监听任务到达
                            taskQueue.wait(100);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    //任务队列不为空
                    if (!taskQueue.isEmpty()) {
                        runnable = taskQueue.remove(0);//获取第一个任务
                    }
                } try{
                if (runnable != null) {
                    runnable.run();
                }}catch (Exception e){
                    e.printStackTrace();
                    System.out.println("e caught in running");
                }finally {
                    sumCount++;
                    runnable = null;
                }

            }
        }

        //销毁线程
        public void setWorkerFlag() {
            isRunning = false;
        }
    }
}