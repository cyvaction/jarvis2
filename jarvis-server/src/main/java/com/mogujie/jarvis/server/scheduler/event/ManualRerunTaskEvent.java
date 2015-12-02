/*
 * 蘑菇街 Inc.
 * Copyright (c) 2010-2015 All Rights Reserved.
 *
 * Author: guangming
 * Create Date: 2015年12月2日 下午7:24:02
 */

package com.mogujie.jarvis.server.scheduler.event;

import java.util.List;

import com.mogujie.jarvis.core.observer.Event;

/**
 * @author guangming
 *
 */
public class ManualRerunTaskEvent implements Event {
    private List<Long> taskIdList;

    /**
     * @param taskIdList
     */
    public ManualRerunTaskEvent(List<Long> taskIdList) {
        this.taskIdList = taskIdList;
    }

    public List<Long> getTaskIdList() {
        return taskIdList;
    }

    public void setTaskIdList(List<Long> taskIdList) {
        this.taskIdList = taskIdList;
    }
}
