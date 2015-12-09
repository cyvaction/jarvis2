/*
 * 蘑菇街 Inc.
 * Copyright (c) 2010-2015 All Rights Reserved.
 *
 * Author: guangming
 * Create Date: 2015年8月31日 上午10:50:42
 */

package com.mogujie.jarvis.server.scheduler.task;

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import com.mogujie.jarvis.core.domain.TaskDetail;
import com.mogujie.jarvis.core.domain.TaskStatus;
import com.mogujie.jarvis.core.util.IdUtils;
import com.mogujie.jarvis.core.util.JsonHelper;
import com.mogujie.jarvis.dto.generate.Job;
import com.mogujie.jarvis.dto.generate.Task;
import com.mogujie.jarvis.server.TaskManager;
import com.mogujie.jarvis.server.TaskQueue;
import com.mogujie.jarvis.server.domain.RetryType;
import com.mogujie.jarvis.server.scheduler.Scheduler;
import com.mogujie.jarvis.server.scheduler.TaskRetryScheduler;
import com.mogujie.jarvis.server.scheduler.event.AddTaskEvent;
import com.mogujie.jarvis.server.scheduler.event.FailedEvent;
import com.mogujie.jarvis.server.scheduler.event.KilledEvent;
import com.mogujie.jarvis.server.scheduler.event.ManualRerunTaskEvent;
import com.mogujie.jarvis.server.scheduler.event.RetryTaskEvent;
import com.mogujie.jarvis.server.scheduler.event.RunTaskEvent;
import com.mogujie.jarvis.server.scheduler.event.RunningEvent;
import com.mogujie.jarvis.server.scheduler.event.ScheduleEvent;
import com.mogujie.jarvis.server.scheduler.event.StartEvent;
import com.mogujie.jarvis.server.scheduler.event.StopEvent;
import com.mogujie.jarvis.server.scheduler.event.SuccessEvent;
import com.mogujie.jarvis.server.service.JobService;
import com.mogujie.jarvis.server.service.TaskService;
import com.mogujie.jarvis.server.util.SpringContext;

/**
 * Scheduler used to handle ready tasks.
 *
 * @author guangming
 *
 */
public class TaskScheduler extends Scheduler {
    private static TaskScheduler instance = new TaskScheduler();

    private TaskScheduler() {
    }

    public static TaskScheduler getInstance() {
        return instance;
    }

    private TaskGraph taskGraph = TaskGraph.INSTANCE;
    private JobService jobService = SpringContext.getBean(JobService.class);
    private TaskService taskService = SpringContext.getBean(TaskService.class);
    private TaskManager taskManager = SpringContext.getBean(TaskManager.class);
    private TaskQueue taskQueue = TaskQueue.INSTANCE;
    private TaskRetryScheduler retryScheduler = TaskRetryScheduler.INSTANCE;

    private static final Logger LOGGER = LogManager.getLogger();

    public void destroy() {
        taskGraph.clear();
    }

    @Override
    public void handleStartEvent(StartEvent event) {
    }

    @Override
    public void handleStopEvent(StopEvent event) {
    }

    @Subscribe
    @Transactional
    @AllowConcurrentEvents
    public void handleSuccessEvent(SuccessEvent e) {
        long taskId = e.getTaskId();
        LOGGER.info("start handleSuccessEvent, taskId={}", taskId);
        // update task status
        List<DAGTask> childTasks = taskGraph.getChildren(taskId);
        Map<Long, List<Long>> childTaskMap = TaskGraph.convert2TaskMap(childTasks);
        taskService.updateStatusWithEnd(taskId, TaskStatus.SUCCESS, childTaskMap);
        LOGGER.info("update {} with SUCCESS status", taskId);

        // remove from taskGraph
        taskGraph.removeTask(taskId);
        LOGGER.info("remove {} from taskGraph", taskId);

        // notify child tasks
        LOGGER.info("notify child tasks {}", childTasks);
        for (DAGTask childTask : childTasks) {
            if (childTask != null && childTask.checkStatus()) {
                LOGGER.info("child {} pass the status check", childTask);
                submitTask(childTask);
            }
        }

        // reduce task number
        reduceTaskNum(taskId);
    }

    @Subscribe
    @Transactional
    @AllowConcurrentEvents
    public void handleRunningEvent(RunningEvent e) {
        long taskId = e.getTaskId();
        LOGGER.info("start handleRunningEvent, taskId={}", taskId);
        int workerId = e.getWorkerId();
        taskService.updateStatusWithStart(taskId, TaskStatus.RUNNING, workerId);
        LOGGER.info("update {} with RUNNING status", taskId);
    }

    @Subscribe
    @Transactional
    @AllowConcurrentEvents
    public void handleKilledEvent(KilledEvent e) {
        long taskId = e.getTaskId();
        LOGGER.info("start handleKilledEvent, taskId={}", taskId);
        taskService.updateStatusWithEnd(taskId, TaskStatus.KILLED);
        LOGGER.info("update {} with KILLED status", taskId);
        reduceTaskNum(taskId);
    }

    @Subscribe
    @Transactional
    @AllowConcurrentEvents
    public void handleFailedEvent(FailedEvent e) {
        long taskId = e.getTaskId();
        LOGGER.info("start handleFailedEvent, taskId={}", taskId);
        DAGTask dagTask = taskGraph.getTask(taskId);
        if (dagTask != null) {
            long jobId = dagTask.getJobId();
            Job job = jobService.get(jobId).getJob();
            int failedRetries = job.getFailedAttempts();
            int failedInterval = job.getFailedInterval();

            int attemptId = dagTask.getAttemptId();
            LOGGER.info("attemptId={}, failedRetries={}", attemptId, failedRetries);
            if (attemptId <= failedRetries) {
                attemptId++;
                dagTask.setAttemptId(attemptId);
                Task task = new Task();
                task.setTaskId(taskId);
                task.setAttemptId(attemptId);
                task.setUpdateTime(DateTime.now().toDate());
                task.setStatus(TaskStatus.READY.getValue());
                taskService.updateSelective(task);
                LOGGER.info("update task {}, attemptId={}", taskId, attemptId);
                retryScheduler.addTask(getTaskInfo(dagTask), failedRetries, failedInterval, RetryType.FAILED_RETRY);
                LOGGER.info("add to retryScheduler");
            } else {
                taskService.updateStatusWithEnd(taskId, TaskStatus.FAILED);
                LOGGER.info("update {} with FAILED status", taskId);
                String key = jobId + "_" + taskId;
                retryScheduler.remove(key, RetryType.FAILED_RETRY);
                LOGGER.info("remove from retryScheduler, key={}", key);
            }
        }

        reduceTaskNum(taskId);
    }

    @Subscribe
    @Transactional
    public void handleAddTaskEvent(AddTaskEvent e) {
        long jobId = e.getJobId();
        long scheduleTime = e.getScheduleTime();
        LOGGER.info("start handleAddTaskEvent, jobId={}, scheduleTime={}", jobId, scheduleTime);
        Map<Long, List<Long>> dependTaskIdMap = e.getDependTaskIdMap();

        // create new task
        long taskId = taskService.createTaskByJobId(jobId, scheduleTime);
        LOGGER.info("add new task[{}] to DB", taskId);

        // add to taskGraph
        DAGTask dagTask = new DAGTask(jobId, taskId, scheduleTime, dependTaskIdMap);
        taskGraph.addTask(taskId, dagTask);
        LOGGER.info("add {} to taskGraph", dagTask);

        // add task dependency
        if (dependTaskIdMap != null) {
            for (Long preJobId : dependTaskIdMap.keySet()) {
                List<Long> preTasks = dependTaskIdMap.get(preJobId);
                for (Long parentId : preTasks) {
                    taskGraph.addDependency(parentId, taskId);
                }
            }
        }

        // 如果通过依赖检查，提交给任务执行器
        if (dagTask.checkStatus()) {
            submitTask(dagTask);
        }

        // send ScheduleEvent
        ScheduleEvent event = new ScheduleEvent(jobId, taskId, scheduleTime);
        getSchedulerController().notify(event);
    }

    @Subscribe
    @Transactional
    public void handleRetryTaskEvent(RetryTaskEvent e) {
        long taskId = e.getTaskId();
        LOGGER.info("start handleRetryTaskEvent, taskId={}", taskId);
        Task task = taskService.get(taskId);
        if (task != null) {
            taskService.updateStatus(taskId, TaskStatus.WAITING);
            LOGGER.info("update {} with WAITING status", taskId);

            DAGTask dagTask = taskGraph.getTask(taskId);
            if (dagTask == null) {
                dagTask = new DAGTask(task.getJobId(), taskId, task.getAttemptId(), task.getScheduleTime().getTime());
                taskGraph.addTask(taskId, dagTask);
                LOGGER.info("add {} to taskGraph", dagTask);
            }
            if (dagTask != null && dagTask.checkStatus()) {
                LOGGER.info("{} pass status check", dagTask);
                int attemptId = dagTask.getAttemptId();
                attemptId++;
                dagTask.setAttemptId(attemptId);
                Task updateTask = new Task();
                updateTask.setTaskId(taskId);
                updateTask.setAttemptId(attemptId);
                updateTask.setUpdateTime(DateTime.now().toDate());
                taskService.updateSelective(updateTask);
                LOGGER.info("update task {}, attemptId={}", taskId, attemptId);
                submitTask(dagTask);
            }
        }
    }

    @Subscribe
    public void handleManulRerunTaskEvent(ManualRerunTaskEvent e) {
        List<Long> taskIdList = e.getTaskIdList();
        LOGGER.info("start handleRetryTaskEvent, taskIdList={}", taskIdList);
        for (Long taskId : taskIdList) {
            DAGTask dagTask = taskGraph.getTask(taskId);
            if (dagTask != null && dagTask.checkStatus()) {
                LOGGER.info("{} pass status check", dagTask);
                submitTask(dagTask);
            }
        }
    }

    @Subscribe
    public void handleRunTaskEvent(RunTaskEvent e) {
        long taskId = e.getTaskId();
        LOGGER.info("start handleRunTaskEvent, taskId={}", taskId);
        DAGTask dagTask = taskGraph.getTask(taskId);
        if (dagTask != null && dagTask.checkStatus()) {
            LOGGER.info("{} pass status check", dagTask);
            submitTask(dagTask);
        }
    }

    @VisibleForTesting
    public TaskQueue getTaskQueue() {
        return taskQueue;
    }

    public void submitTask(DAGTask dagTask) {
        // update status to ready
        taskService.updateStatus(dagTask.getTaskId(), TaskStatus.READY);
        LOGGER.info("update {} with READY status", dagTask.getTaskId());

        // submit to TaskQueue
        TaskDetail taskDetail = getTaskInfo(dagTask);
        if (taskDetail != null) {
            taskQueue.put(taskDetail);
        }
    }

    private TaskDetail getTaskInfo(DAGTask dagTask) {
        String fullId = IdUtils.getFullId(dagTask.getJobId(), dagTask.getTaskId(), dagTask.getAttemptId());
        TaskDetail taskDetail = null;
        long jobId = dagTask.getJobId();
        Job job = jobService.get(jobId).getJob();
        taskDetail = TaskDetail.newTaskDetailBuilder()
                .setFullId(fullId)
                .setTaskName(job.getJobName())
                .setAppName(jobService.getAppName(jobId))
                .setUser(job.getSubmitUser())
                .setPriority(job.getPriority())
                .setContent(job.getContent())
                .setTaskType(job.getJobType())
                .setParameters(JsonHelper.fromJson2JobParams(job.getParams()))
                .setSchedulingTime(dagTask.getScheduleTime())
                .setGroupId(job.getWorkerGroupId())
                .setFailedRetries(job.getFailedAttempts())
                .setFailedInterval(job.getFailedInterval())
                .setRejectRetries(job.getRejectAttempts())
                .setRejectInterval(job.getRejectInterval())
                .build();
        return taskDetail;
    }

    private void reduceTaskNum(long taskId) {
        Task task = taskService.get(taskId);
        if (task != null) {
            taskManager.appCounterDecrement(task.getAppId());
        }
    }

}
