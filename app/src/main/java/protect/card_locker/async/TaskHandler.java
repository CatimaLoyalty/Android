package protect.card_locker.async;

import android.os.Handler;
import android.os.Looper;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * AsyncTask has been deprecated so this provides very rudimentary compatibility without
 * needing to redo too many Parts.
 * <p>
 * However this is a much, much more cooperative Behaviour than before so
 * the callers need to ensure we do NOT rely on forced cancellation and feed less into the
 * ThreadPools so we don't OOM/Overload the Users device
 * <p>
 * This assumes single-threaded callers.
 */
public class TaskHandler {

    public enum TYPE {
        BARCODE,
        IMPORT,
        EXPORT
    }

    HashMap<TYPE, ThreadPoolExecutor> executors = generateExecutors();

    final private HashMap<TYPE, LinkedList<Future<?>>> taskList = new HashMap<>();

    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    private HashMap<TYPE, ThreadPoolExecutor> generateExecutors() {
        HashMap<TYPE, ThreadPoolExecutor> initExecutors = new HashMap<>();
        for (TYPE type : TYPE.values()) {
            replaceExecutor(initExecutors, type, false, false);
        }
        return initExecutors;
    }

    /**
     * Replaces (or initializes) an Executor with a clean (new) one
     *
     * @param executors Map Reference
     * @param type      Which Queue
     * @param flushOld  attempt shutdown
     * @param waitOnOld wait for Termination
     */
    private void replaceExecutor(HashMap<TYPE, ThreadPoolExecutor> executors, TYPE type, Boolean flushOld, Boolean waitOnOld) {
        ThreadPoolExecutor oldExecutor = executors.get(type);
        if (oldExecutor != null) {
            if (flushOld) {
                oldExecutor.shutdownNow();
            }
            if (waitOnOld) {
                try {
                    //noinspection ResultOfMethodCallIgnored
                    oldExecutor.awaitTermination(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        executors.put(type, (ThreadPoolExecutor) Executors.newCachedThreadPool());
    }

    /**
     * Queue a Pseudo-AsyncTask for execution
     *
     * @param type     Queue
     * @param callable PseudoAsyncTask
     */
    public void executeTask(TYPE type, CompatCallable<?> callable) {
        Runnable runner = () -> {
            try {
                // Run on the UI Thread
                uiHandler.post(callable::onPreExecute);

                // Background
                final Object result = callable.call();

                // Post results on UI Thread so we can show them
                uiHandler.post(() -> {
                    callable.onPostExecute(result);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        LinkedList<Future<?>> list = taskList.get(type);

        if (list == null) {
            list = new LinkedList<>();
        }

        ThreadPoolExecutor executor = executors.get(type);

        if (executor != null) {
            Future<?> task = executor.submit(runner);
            // Test Queue Cancellation:
            // task.cancel(true);
            list.push(task);
            taskList.put(type, list);
        }
    }

    /**
     * This will attempt to cancel a currently running list of Tasks
     * Useful to ignore scheduled tasks - but not able to hard-stop tasks that are running
     *
     * @param type                    Which Queue to target
     * @param forceCancel             attempt to close the Queue and force-replace it after
     * @param waitForFinish           wait and return after the old executor finished. Times out after 5s
     * @param waitForCurrentlyRunning wait before cancelling tasks. Useful for tests.
     */
    public void flushTaskList(TYPE type, Boolean forceCancel, Boolean waitForFinish, Boolean waitForCurrentlyRunning) {
        // Only used for Testing
        if (waitForCurrentlyRunning) {
            ThreadPoolExecutor oldExecutor = executors.get(type);

            if (oldExecutor != null) {
                try {
                    //noinspection ResultOfMethodCallIgnored
                    oldExecutor.awaitTermination(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        // Attempt to cancel known Tasks and clean the List
        LinkedList<Future<?>> tasks = taskList.get(type);
        if (tasks != null) {
            for (Future<?> task : tasks) {
                if (!task.isDone() || !task.isCancelled()) {
                    // Interrupt any Task we can
                    task.cancel(true);
                }
            }
        }
        tasks = new LinkedList<>();
        taskList.put(type, tasks);

        if (forceCancel || waitForFinish) {
            ThreadPoolExecutor oldExecutor = executors.get(type);

            if (oldExecutor != null) {
                if (forceCancel) {
                    if (waitForFinish) {
                        try {
                            //noinspection ResultOfMethodCallIgnored
                            oldExecutor.awaitTermination(5, TimeUnit.SECONDS);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    oldExecutor.shutdownNow();
                    replaceExecutor(executors, type, true, false);
                } else {
                    try {
                        //noinspection ResultOfMethodCallIgnored
                        oldExecutor.awaitTermination(5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}