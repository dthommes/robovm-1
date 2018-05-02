package org.robovm.idea.idevlog;

import com.intellij.diagnostic.logging.LogFilter;
import com.intellij.diagnostic.logging.LogFilterListener;
import com.intellij.diagnostic.logging.LogFilterModel;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class DeviceLogFilterModel extends LogFilterModel {
    private final List<LogFilterListener> myListeners = ContainerUtil.createLockFreeCopyOnWriteList();

    @Nullable
    private Pattern myCustomPattern;

    /**
     * This is called to enable regular expression filtering of log messages.
     * Replaces the customFilter mechanism.
     */
    void updateCustomPattern(@Nullable Pattern pattern) {
        myCustomPattern = pattern;
        fireTextFilterChange();
    }

    @Override
    public String getCustomFilter() {
        return "";
    }

    @Override
    public final void addFilterListener(LogFilterListener listener) {
        myListeners.add(listener);
    }

    @Override
    public final void removeFilterListener(LogFilterListener listener) {
        myListeners.remove(listener);
    }


    @Override
    public List<? extends LogFilter> getLogFilters() {
        return Collections.emptyList();
    }

    @Override
    public boolean isFilterSelected(LogFilter logFilter) {
        return false;
    }

    @Override
    public void selectFilter(LogFilter logFilter) {
    }

    @Override
    @NotNull
    public final MyProcessingResult processLine(String line) {
        return new MyProcessingResult(ProcessOutputTypes.STDOUT, isApplicable(line), null);
    }

    private void fireTextFilterChange() {
        for (LogFilterListener listener : myListeners) {
            listener.onTextFilterChange();
        }
    }

    @Override
    public final boolean isApplicable(String line) {
        // Not calling the super class version, it does not do what we want with regular expression matching
        return (myCustomPattern == null || myCustomPattern.matcher(line).find());
    }
}
