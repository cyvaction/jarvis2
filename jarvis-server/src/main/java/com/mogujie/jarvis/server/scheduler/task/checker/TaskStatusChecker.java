/*
 * 蘑菇街 Inc.
 * Copyright (c) 2010-2015 All Rights Reserved.
 *
 * Author: guangming
 * Create Date: 2015年11月4日 下午7:14:59
 */

package com.mogujie.jarvis.server.scheduler.task.checker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.collect.Maps;
import com.mogujie.jarvis.core.expression.DefaultDependencyStrategyExpression;
import com.mogujie.jarvis.core.expression.DependencyExpression;
import com.mogujie.jarvis.core.expression.DependencyStrategyExpression;
import com.mogujie.jarvis.server.domain.CommonStrategy;
import com.mogujie.jarvis.server.domain.JobDependencyEntry;
import com.mogujie.jarvis.server.guice.Injectors;
import com.mogujie.jarvis.server.service.JobService;
import com.mogujie.jarvis.server.service.TaskDependService;
import com.mogujie.jarvis.server.service.TaskService;

/**
 * @author guangming
 *
 */
public class TaskStatusChecker {

    private JobService jobService = Injectors.getInjector().getInstance(JobService.class);
    private TaskService taskService = Injectors.getInjector().getInstance(TaskService.class);
    private TaskDependService taskDependService = Injectors.getInjector().getInstance(TaskDependService.class);
    // Map<jobId, TaskDependStatus>
    private Map<Long, TaskDependStatus> jobStatusMap = new ConcurrentHashMap<Long, TaskDependStatus>();
    private long myJobId;
    private long myTaskId;

    public TaskStatusChecker(long jobId, long taskId, long scheduleTime, Map<Long, List<Long>> dependTaskIdMap) {
        this.myJobId = jobId;
        this.myTaskId = taskId;
        if (dependTaskIdMap != null && !dependTaskIdMap.isEmpty()) {
            this.jobStatusMap = convertToJobStatus(scheduleTime, dependTaskIdMap);
            // store task dependency
            taskDependService.storeParent(taskId, dependTaskIdMap);
        }
    }

    public TaskStatusChecker(long jobId, long taskId, long scheduleTime) {
        this.myJobId = jobId;
        this.myTaskId = taskId;
        // load dependTaskIdMap from taskDependService
        Map<Long, List<Long>> dependTaskIdMap = taskDependService.loadParent(taskId);
        this.jobStatusMap = loadJobStatus(dependTaskIdMap, scheduleTime);
    }

    public long getMyJobId() {
        return myJobId;
    }

    public void setMyJobId(long myJobId) {
        this.myJobId = myJobId;
    }

    public long getMyTaskId() {
        return myTaskId;
    }

    public void setMyTaskId(long myTaskId) {
        this.myTaskId = myTaskId;
    }

    public boolean checkStatus() {
        boolean finishStatus = true;
        for (Entry<Long, TaskDependStatus> entry : jobStatusMap.entrySet()) {
            TaskDependStatus status = entry.getValue();
            if (!status.check()) {
                finishStatus = false;
                break;
            }
        }
        return finishStatus;
    }

    public List<Long> getDependTaskIds() {
        List<Long> dependTaskIds = new ArrayList<Long>();
        for (TaskDependStatus status : jobStatusMap.values()) {
            dependTaskIds.addAll(status.getDependTaskIds());
        }
        return dependTaskIds;
    }

    public void removeJob(long jobId) {
        jobStatusMap.remove(jobId);
        Map<Long, List<Long>> dependTaskIdMap = convert2DependTaskIdMap(jobStatusMap);
        // flush task dependency
        taskDependService.storeParent(myTaskId, dependTaskIdMap);
    }

    public void removeTask(long jobId, long taskId) {
        if (jobStatusMap.containsKey(jobId)) {
            TaskDependStatus taskStatus = jobStatusMap.get(jobId);
            taskStatus.removeTask(taskId);
        }
        Map<Long, List<Long>> dependTaskIdMap = convert2DependTaskIdMap(jobStatusMap);
        // flush task dependency
        taskDependService.storeParent(myTaskId, dependTaskIdMap);
    }

    private Map<Long, TaskDependStatus> loadJobStatus(Map<Long, List<Long>> dependTaskIdMap, long scheduleTime) {
        Map<Long, TaskDependStatus> jobStatusMap = new ConcurrentHashMap<Long, TaskDependStatus>();
        Map<Long, JobDependencyEntry> dependencyMap = jobService.get(myJobId).getDependencies();
        if (dependencyMap != null) {
            for (Entry<Long, JobDependencyEntry> entry : dependencyMap.entrySet()) {
                long preJobId = entry.getKey();
                JobDependencyEntry dependencyEntry = entry.getValue();
                DependencyStrategyExpression commonStrategy = dependencyEntry.getDependencyStrategyExpression();
                DependencyExpression dependencyExpression = dependencyEntry.getDependencyExpression();
                List<Long> dependTaskIds = null;
                if (dependTaskIdMap != null && dependTaskIdMap.containsKey(preJobId)) {
                    dependTaskIds = dependTaskIdMap.get(preJobId);
                } else {
                    dependTaskIds = taskService.getDependTaskIds(myJobId, preJobId, scheduleTime, dependencyExpression);
                }
                TaskDependStatus taskStatus = new TaskDependStatus(dependTaskIds, commonStrategy);
                jobStatusMap.put(preJobId, taskStatus);
            }
        }
        return jobStatusMap;
    }

    private Map<Long, TaskDependStatus> convertToJobStatus(long scheduleTime, Map<Long, List<Long>> dependTaskIdMap) {
        Map<Long, TaskDependStatus> jobStatusMap = new ConcurrentHashMap<Long, TaskDependStatus>();
        for (Entry<Long, List<Long>> entry : dependTaskIdMap.entrySet()) {
            long preJobId = entry.getKey();
            List<Long> dependTaskIds = entry.getValue();
            Map<Long, JobDependencyEntry> dependencyMap = jobService.get(myJobId).getDependencies();
            if (dependencyMap != null && dependencyMap.containsKey(preJobId)) {
                JobDependencyEntry dependencyEntry = dependencyMap.get(preJobId);
                DependencyStrategyExpression commonStrategy = dependencyEntry.getDependencyStrategyExpression();
                if (dependTaskIds == null || dependTaskIds.isEmpty()) {
                    DependencyExpression dependencyExpression = dependencyEntry.getDependencyExpression();
                    dependTaskIds = taskService.getDependTaskIds(myJobId, preJobId, scheduleTime, dependencyExpression);
                }
                TaskDependStatus taskStatus = new TaskDependStatus(dependTaskIds, commonStrategy);
                jobStatusMap.put(preJobId, taskStatus);
            } else {
                // for testing
                if (dependTaskIds != null && !dependTaskIds.isEmpty()) {
                    DependencyStrategyExpression commonStrategy = new DefaultDependencyStrategyExpression(CommonStrategy.ALL.getExpression());
                    TaskDependStatus taskStatus = new TaskDependStatus(dependTaskIds, commonStrategy);
                    jobStatusMap.put(preJobId, taskStatus);
                }
            }
        }
        return jobStatusMap;
    }

    private Map<Long, List<Long>> convert2DependTaskIdMap(Map<Long, TaskDependStatus> jobStatusMap) {
        Map<Long, List<Long>> dependTaskIdMap = Maps.newHashMap();
        for (Entry<Long, TaskDependStatus> entry : jobStatusMap.entrySet()) {
            long jobId = entry.getKey();
            TaskDependStatus taskStatus = entry.getValue();
            List<Long> dependTaskIds = taskStatus.getDependTaskIds();
            dependTaskIdMap.put(jobId, dependTaskIds);
        }
        return dependTaskIdMap;
    }
}
