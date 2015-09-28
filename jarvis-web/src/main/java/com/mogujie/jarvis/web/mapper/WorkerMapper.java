package com.mogujie.jarvis.web.mapper;

import com.mogujie.jarvis.web.entity.vo.WorkerGroupSearchVo;
import com.mogujie.jarvis.web.entity.vo.WorkerGroupVo;
import com.mogujie.jarvis.web.entity.vo.WorkerSearchVo;
import com.mogujie.jarvis.web.entity.vo.WorkerVo;

import java.util.List;

/**
 * Created by hejian on 15/9/28.
 */
public interface WorkerMapper {
    //worker
    public WorkerVo getWorkerById(Integer id);
    public Integer getWorkerCount(WorkerSearchVo workerSearchVo);
    public List<WorkerVo> getWorkerList(WorkerSearchVo workerSearchVo);
    public List<String> getAllWorkerIp();
    public List<Integer> getAllWorkerPort();


    //WorkerGroup
    public List<WorkerGroupVo> getAllWorkerGroup();
    public WorkerGroupVo getWorkerGroupById(Integer id);
    public Integer getWorkerGroupCount(WorkerGroupSearchVo workerGroupSearchVo);
    public List<WorkerGroupVo> getWorkerGroupList(WorkerGroupSearchVo workerGroupSearchVo);
    public List<String> getAllWorkerGroupCreator();

}
