package utils;

import java.util.List;

public interface IThreadPool {
    public void execute(Runnable task);

    public void execute(List<Runnable> tasks);

    public void execute(Runnable[] tasks);

    //destroy task
    void destroy();
}
