package com.mogujie.jarvis.dao.generate;

import com.mogujie.jarvis.dto.generate.JobDepend;
import com.mogujie.jarvis.dto.generate.JobDependExample;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface JobDependMapper {
    int countByExample(JobDependExample example);

    int deleteByExample(JobDependExample example);

    int deleteByPrimaryKey(@Param("jobId") Long jobId, @Param("preJobId") Long preJobId);

    int insert(JobDepend record);

    int insertSelective(JobDepend record);

    java.util.List<com.mogujie.jarvis.dto.generate.JobDepend> selectByExample(JobDependExample example);

    JobDepend selectByPrimaryKey(@Param("jobId") Long jobId, @Param("preJobId") Long preJobId);

    int updateByExampleSelective(@Param("record") JobDepend record, @Param("example") JobDependExample example);

    int updateByExample(@Param("record") JobDepend record, @Param("example") JobDependExample example);

    int updateByPrimaryKeySelective(JobDepend record);

    int updateByPrimaryKey(JobDepend record);
}