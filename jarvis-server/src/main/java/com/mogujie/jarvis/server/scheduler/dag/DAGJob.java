/*
 * 蘑菇街 Inc.
 * Copyright (c) 2010-2015 All Rights Reserved.
 *
 * Author: guangming
 * Create Date: 2015年8月31日 上午11:38:28
 */

package com.mogujie.jarvis.server.scheduler.dag;

import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mogujie.jarvis.server.scheduler.dag.checker.DAGDependChecker;
import com.mogujie.jarvis.server.scheduler.dag.checker.DAGDependCheckerFactory;

/**
 * @author guangming
 *
 */
public class DAGJob extends AbstractDAGJob {

    private long jobId;
    private DAGDependChecker dependChecker;
    private DAGJobType type;
    private boolean timeReadyFlag = false;
    private static final Logger LOGGER = LogManager.getLogger();

    public DAGJob() {
        this.dependChecker = DAGDependCheckerFactory.create();
    }

    public DAGJob(long jobId, DAGJobType type) {
        this.jobId = jobId;
        this.type = type;
        this.dependChecker = DAGDependCheckerFactory.create();
    }

    @Override
    public boolean dependCheck(Set<Long> needJobs) {
        boolean passCheck = true;
        if (type.implies(DAGJobType.DEPEND)) {
            boolean dependCheck = dependChecker.check(needJobs);
            if (!dependCheck) {
                LOGGER.debug("dependChecker failed, job {}, needJobs {}", jobId, needJobs);
            }
            passCheck = passCheck && dependChecker.check(needJobs);
        }

        if (type.implies(DAGJobType.TIME)) {
            if (!timeReadyFlag) {
                LOGGER.debug("Job {} is not time ready", jobId);
            }
            passCheck = passCheck && timeReadyFlag;
        }

        return passCheck;
    }

    @Override
    public String toString() {
        return "{[jobid is" + jobId + "]," +
                "[DAG type is" + type + "]," +
                "[depend check instance is" + dependChecker.getClass().getSimpleName() + "]}";
    }

    public long getJobId() {
        return jobId;
    }

    public void setJobId(long jobId) {
        this.jobId = jobId;
        this.dependChecker.setMyJobId(jobId);
    }

    public void setDependStatus(long jobId, long taskId) {
        dependChecker.setDependStatus(jobId, taskId);
    }

    public void resetDependStatus(long jobId, long taskId) {
        dependChecker.resetDependStatus(jobId, taskId);
    }

    public void resetDependStatus() {
        dependChecker.resetAllStatus();
        if (type.implies(DAGJobType.TIME)) {
            resetTimeReadyFlag();
        }
    }

    public void setTimeReadyFlag() {
        timeReadyFlag = true;
    }

    public void resetTimeReadyFlag() {
        timeReadyFlag = false;
    }

    public DAGDependChecker getDependChecker() {
        return dependChecker;
    }

    public void setDependChecker(DAGDependChecker dependChecker) {
        this.dependChecker = dependChecker;
    }

    public DAGJobType getType() {
        return type;
    }

    public void setType(DAGJobType type) {
        this.type = type;
    }

    public void updateJobTypeByTimeFlag(boolean timeFlag) {
        updateJobType(timeFlag, DAGJobType.TIME);
    }

    public void updateJobTypeByDependFlag(boolean dependFlag) {
        updateJobType(dependFlag, DAGJobType.DEPEND);
    }

    public void updateJobTypeByCycleFlag(boolean cycleFlag) {
        updateJobType(cycleFlag, DAGJobType.CYCLE);
    }

    private void updateJobType(boolean isAdd, DAGJobType that) {
        if (isAdd) {
            type = type.or(that);
        } else {
            type = type.remove(that);
        }
    }
}
