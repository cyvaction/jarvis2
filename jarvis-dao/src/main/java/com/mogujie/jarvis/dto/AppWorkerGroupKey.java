package com.mogujie.jarvis.dto;

import java.io.Serializable;

public class AppWorkerGroupKey implements Serializable {
    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column app_worker_group.appId
     *
     * @mbggenerated Thu Nov 05 15:09:39 CST 2015
     */
    private Integer appId;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column app_worker_group.workerGroupId
     *
     * @mbggenerated Thu Nov 05 15:09:39 CST 2015
     */
    private Integer workerGroupId;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database table app_worker_group
     *
     * @mbggenerated Thu Nov 05 15:09:39 CST 2015
     */
    private static final long serialVersionUID = 1L;

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column app_worker_group.appId
     *
     * @return the value of app_worker_group.appId
     *
     * @mbggenerated Thu Nov 05 15:09:39 CST 2015
     */
    public Integer getAppId() {
        return appId;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column app_worker_group.appId
     *
     * @param appId the value for app_worker_group.appId
     *
     * @mbggenerated Thu Nov 05 15:09:39 CST 2015
     */
    public void setAppId(Integer appId) {
        this.appId = appId;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column app_worker_group.workerGroupId
     *
     * @return the value of app_worker_group.workerGroupId
     *
     * @mbggenerated Thu Nov 05 15:09:39 CST 2015
     */
    public Integer getWorkerGroupId() {
        return workerGroupId;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column app_worker_group.workerGroupId
     *
     * @param workerGroupId the value for app_worker_group.workerGroupId
     *
     * @mbggenerated Thu Nov 05 15:09:39 CST 2015
     */
    public void setWorkerGroupId(Integer workerGroupId) {
        this.workerGroupId = workerGroupId;
    }
}