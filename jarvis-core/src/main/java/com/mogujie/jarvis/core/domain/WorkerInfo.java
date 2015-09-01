/*
 * 蘑菇街 Inc.
 * Copyright (c) 2010-2015 All Rights Reserved.
 *
 * Author: wuya
 * Create Date: 2015年6月14日 下午8:13:35
 */
package com.mogujie.jarvis.core.domain;

import java.util.Objects;

/**
 * @author wuya
 *
 */
public class WorkerInfo {

    private String ip;
    private int port;
    private String clientKey;
    private String akkaPath;

    public WorkerInfo(String ip, int port, String clientKey) {
        this.ip = ip;
        this.port = port;
        this.clientKey = clientKey;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getClientKey() {
        return clientKey;
    }

    public void setClientKey(String clientKey) {
        this.clientKey = clientKey;
    }

    public String getAkkaPath() {
        if (akkaPath == null) {
            StringBuilder sb = new StringBuilder();
            sb.append("akka.tcp://client@");
            sb.append(ip);
            sb.append(":");
            sb.append(port);
            akkaPath = sb.toString();
        }

        return akkaPath;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip, port);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        WorkerInfo other = (WorkerInfo) obj;
        return Objects.equals(ip, other.ip) && Objects.equals(port, other.port) && Objects.equals(clientKey, other.clientKey);
    }

    @Override
    public String toString() {
        return getAkkaPath();
    }

}