package com.mogujie.jarvis.dto;

import java.io.Serializable;

public class TaskDependKey implements Serializable {
    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column task_depend.taskId
     *
     * @mbggenerated Thu Nov 05 15:09:39 CST 2015
     */
    private Long taskId;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column task_depend.preJobId
     *
     * @mbggenerated Thu Nov 05 15:09:39 CST 2015
     */
    private Long preJobId;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column task_depend.preTaskId
     *
     * @mbggenerated Thu Nov 05 15:09:39 CST 2015
     */
    private Long preTaskId;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database table task_depend
     *
     * @mbggenerated Thu Nov 05 15:09:39 CST 2015
     */
    private static final long serialVersionUID = 1L;

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column task_depend.taskId
     *
     * @return the value of task_depend.taskId
     *
     * @mbggenerated Thu Nov 05 15:09:39 CST 2015
     */
    public Long getTaskId() {
        return taskId;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column task_depend.taskId
     *
     * @param taskId the value for task_depend.taskId
     *
     * @mbggenerated Thu Nov 05 15:09:39 CST 2015
     */
    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column task_depend.preJobId
     *
     * @return the value of task_depend.preJobId
     *
     * @mbggenerated Thu Nov 05 15:09:39 CST 2015
     */
    public Long getPreJobId() {
        return preJobId;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column task_depend.preJobId
     *
     * @param preJobId the value for task_depend.preJobId
     *
     * @mbggenerated Thu Nov 05 15:09:39 CST 2015
     */
    public void setPreJobId(Long preJobId) {
        this.preJobId = preJobId;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column task_depend.preTaskId
     *
     * @return the value of task_depend.preTaskId
     *
     * @mbggenerated Thu Nov 05 15:09:39 CST 2015
     */
    public Long getPreTaskId() {
        return preTaskId;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column task_depend.preTaskId
     *
     * @param preTaskId the value for task_depend.preTaskId
     *
     * @mbggenerated Thu Nov 05 15:09:39 CST 2015
     */
    public void setPreTaskId(Long preTaskId) {
        this.preTaskId = preTaskId;
    }
}