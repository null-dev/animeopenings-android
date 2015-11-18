package gq.nulldev.animeopenings.app.util;

import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Project: NullConcurrency
 * Created: 21/10/15
 * Author: nulldev
 */

/**
 * Fully chainable task lists with persistent data storage, task dependencies and auto dependency-resolving sorting!
 */
public class ChainableTaskList {
    boolean executed = false;
    ArrayList<ChainableTask> completedTasks = new ArrayList<>();
    HashMap<String, Object> taskDataStorage = new HashMap<>();
    Queue<ChainableTask> queuedTasks = new LinkedBlockingDeque<>();
    ArrayList<ChainableTask> chainQueue = new ArrayList<>();

    /**
     * Create a new chainable task list from the specified tasks
     *
     * The tasks will be queued in order unless dependencies are required to be satisfied in which it will prioritize satisfying dependencies first.
     * @param tasks The tasks to add to the list
     */
    public ChainableTaskList(ChainableTask... tasks) {
        //Chain the tasks
        try {
            chainTaskList(Arrays.asList(tasks));
        } catch(IllegalArgumentException ignore) {}
    }

    /**
     * Chain a task list onto the current queue
     * @param taskList The task list to chain
     */
    void chainTaskList(List<ChainableTask> taskList) {
        ArrayList<ChainableTask> unchainedTasks = new ArrayList<>();
        unchainedTasks.addAll(taskList);
        //Make another queue in case we need to revert it
        Queue<ChainableTask> queueSnapshot = new LinkedBlockingDeque<>();
        for(ChainableTask item : queuedTasks) {
            queueSnapshot.add(item);
        }

        int iters = 0;
        int maxIters = unchainedTasks.size()+2;
        while(true) {
            //Detect circular dependencies
            if (iters > maxIters) {
                //Revert
                queuedTasks = queueSnapshot;
                throw new IllegalArgumentException("Circular or impossible dependency chain detected! Changes reverted! Aborting...");
            } else {
                iters++;
            }
            //Break when we have no tasks left to queue
            if(unchainedTasks.size() < 1) {
                break;
            }
            Iterator<ChainableTask> taskIterator = unchainedTasks.iterator();
            while (taskIterator.hasNext()) {
                ChainableTask task = taskIterator.next();
                //Queue this tasks if it has no dependencies or all it's dependencies have been satisfied
                if (task.getDependentTasks().size() < 1
                        || queuedTasks.containsAll(task.getDependentTasks())) {
                    task.parentList = this;
                    queuedTasks.add(task);
                    taskIterator.remove();
                }
            }
        }
    }

    /**
     * Chain a new task preserving the previous task order.
     *
     * NOTE: This function does not support dependencies (unless the dependencies have already been chained) and will fail if it detects dependencies.
     *
     * @param task The task to chain
     * @return This instance for chaining
     */
    public ChainableTaskList chain(ChainableTask task) {
        if(task.getDependentTasks().size() > 0) {
            if(!queuedTasks.containsAll(task.getDependentTasks())) {
                //I can't find all required dependencies!
                throw new IllegalArgumentException("Dependencies not satisfied. chain() does not support chaining tasks with unsatisfied dependencies to be satisfied in the future!");
            }
        }
        task.parentList = this;
        queuedTasks.add(task);
        return this;
    }

    /**
     * Chain a task, if the task's dependencies cannot be satisfied, it will be chained it later.
     *
     * NOTE: Executing this function is slow and can scramble the task order (while still following dependencies). Only used it when you have dependency trees to satisfy!
     *
     * @param task The task to chain
     * @return This instance for chaining
     */
    public ChainableTaskList chainLater(ChainableTask task) {
        //First try chaining it now
        try {
            chain(task);
        } catch(IllegalArgumentException e) {
            //Nope, we can't do this, chain it later
            chainQueue.add(task);
        }
        //Try chaining the chain queue
        try {
            chainTaskList(chainQueue);
        } catch(IllegalArgumentException e) {
            //We can't this right now, maybe later :)
        }
        return this;
    }

    /**
     * Run little boy run. Just kidding, we just run this task list.
     * @return This instance for chaining
     */
    public ChainableTaskList run() {
        if(executed) {
            throw new IllegalStateException("Cannot execute task list twice!");
        }
        if(chainQueue.size() > 0) {
            try {
                chainTaskList(chainQueue);
            } catch(IllegalArgumentException e) {
                throw new IllegalStateException("Chain queue could not be chained!");
            }
        }
        Iterator<ChainableTask> taskIterator = queuedTasks.iterator();
        while(taskIterator.hasNext()) {
            ChainableTask task = taskIterator.next();
            //Is all required data present?
            if(!taskDataStorage.keySet().containsAll(task.getRequiredDataEntries())
                    && !task.isOptional()) {
                throw new IllegalStateException("Cannot execute tasks, not all required data entries are present!");
            } else {
                //Run the task
                try {
                    task.run();

                    //Done!
                    task.setExecuted(true);
                    //Remove!
                    completedTasks.add(task);
                } catch(Throwable t) {
                    if(!task.isOptional()) {
                        throw new RuntimeException("Failed to execute non-optional task!", t);
                    } else {
                        System.out.println("Task threw error:");
                        t.printStackTrace();
                    }
                }
            }

            taskIterator.remove();
        }
        executed = true;
        return this;
    }

    public HashMap<String, Object> getDataStorage() {
        return taskDataStorage;
    }

    @Override
    public String toString() {
        return "ChainableTaskList{" +
                "executed=" + executed +
                ", completedTasks=" + completedTasks +
                ", taskDataStorage=" + taskDataStorage +
                ", queuedTasks=" + queuedTasks +
                ", chainQueue=" + chainQueue +
                '}';
    }
}