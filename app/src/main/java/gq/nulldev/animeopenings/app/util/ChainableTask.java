package gq.nulldev.animeopenings.app.util;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Project: NullConcurrency
 * Created: 21/10/15
 * Author: nulldev
 */

/**
 * A chainable task for use in ChainableTaskList
 */
public abstract class ChainableTask implements Runnable {
    boolean executed;
    boolean optional;
    ArrayList<ChainableTask> dependentTasks = new ArrayList<>();
    ArrayList<String> requiredDataEntries = new ArrayList<>();
    ChainableTaskList parentList;

    public ChainableTask(){}

    public ChainableTask(ArrayList<ChainableTask> dependentTasks) {
        this.dependentTasks = dependentTasks;
    }

    public ArrayList<ChainableTask> getDependentTasks() {
        return dependentTasks;
    }

    public void setDependentTasks(ArrayList<ChainableTask> dependentTasks) {
        this.dependentTasks = dependentTasks;
    }

    public ArrayList<String> getRequiredDataEntries() {
        return requiredDataEntries;
    }

    public void setRequiredDataEntries(ArrayList<String> requiredDataEntries) {
        this.requiredDataEntries = requiredDataEntries;
    }

    public boolean isExecuted() {
        return executed;
    }

    public void setExecuted(boolean executed) {
        this.executed = executed;
    }

    public boolean isOptional() {
        return optional;
    }

    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    public ChainableTaskList getParentList() {
        return parentList;
    }

    public HashMap<String, Object> getDataStorage() {
        return getParentList().getDataStorage();
    }

    @Override
    public String toString() {
        return "ChainableTask{" +
                "executed=" + executed +
                ", optional=" + optional +
                ", dependentTasks=" + dependentTasks +
                ", requiredDataEntries=" + requiredDataEntries +
                ", parentList=" + parentList +
                '}';
    }
}
