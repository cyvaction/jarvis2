/*
 * 蘑菇街 Inc.
 * Copyright (c) 2010-2015 All Rights Reserved.
 *
 * Author: guangming
 * Create Date: 2015年9月9日 下午2:43:18
 */

package com.mogujie.jarvis.server.scheduler.event;

import java.util.Set;


/**
 * @author guangming
 *
 */
public class ModifyJobEvent extends DAGJobEvent {
    public enum MODIFY_TYPE {
        ADD,
        DEL,
        MODIFY
    }

    private Set<Long> dependencies;
    private MODIFY_TYPE modifyType;
    private boolean hasCron;
    private boolean hasCycle;

    public ModifyJobEvent(long jobId, Set<Long> dependencies, MODIFY_TYPE type,
            boolean hasCron, boolean hasCycle) {
       super(jobId);
       this.dependencies = dependencies;
       this.modifyType = type;
       this.hasCron = hasCron;
       this.hasCycle = hasCycle;
    }

    public Set<Long> getDependencies() {
        return dependencies;
    }

    public void setDependencies(Set<Long> dependencies) {
        this.dependencies = dependencies;
    }

    public MODIFY_TYPE getModifyType() {
        return modifyType;
    }

    public void setModifyType(MODIFY_TYPE modifyType) {
        this.modifyType = modifyType;
    }

    public boolean isHasCron() {
        return hasCron;
    }

    public void setHasCron(boolean hasCron) {
        this.hasCron = hasCron;
    }

    public boolean isHasCycle() {
        return hasCycle;
    }

    public void setHasCycle(boolean hasCycle) {
        this.hasCycle = hasCycle;
    }
}