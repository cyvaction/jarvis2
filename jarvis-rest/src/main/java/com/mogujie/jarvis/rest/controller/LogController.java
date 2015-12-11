/*
 * 蘑菇街 Inc.
 * Copyright (c) 2010-2015 All Rights Reserved.
 *
 * Author: muming
 * Create Date: 2015年10月08日 下午3:19:28
 */
package com.mogujie.jarvis.rest.controller;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.mogujie.jarvis.core.domain.AkkaType;
import com.mogujie.jarvis.core.domain.StreamType;
import com.mogujie.jarvis.protocol.AppAuthProtos;
import com.mogujie.jarvis.protocol.ReadLogProtos.LogStorageReadLogResponse;
import com.mogujie.jarvis.protocol.ReadLogProtos.RestReadLogRequest;
import com.mogujie.jarvis.rest.RestResult;
import com.mogujie.jarvis.rest.utils.JsonParameters;
import com.mogujie.jarvis.rest.vo.LogResultVo;

/**
 * @author muming
 */
@Path("api/log")
public class LogController extends AbstractController {

    private final static int DEFAULT_LINE = 1000;

    /**
     * 获取执行日志
     *
     * @param parameters (taskId、offset：日志内容的字节偏移量、lines：日志读取的行数)
     */
    @POST
    @Path("readExecuteLog")
    @Produces(MediaType.APPLICATION_JSON)
    public RestResult readExecuteLog(@FormParam("appToken") String appToken,
                                        @FormParam("appName") String appName,
                                        @FormParam("user") String user,
                                        @FormParam("parameters") String parameters) {

        return queryLog(StreamType.STD_ERR, appToken, appName, user, parameters);
    }

    /**
     * 获取结果数据
     *
     * @param parameters (taskId、offset：日志内容的字节偏移量、lines：日志读取的行数)
     */
    @POST
    @Path("readResult")
    @Produces(MediaType.APPLICATION_JSON)
    public RestResult readResult(@FormParam("appToken") String appToken,
                                    @FormParam("appName") String appName,
                                    @FormParam("user") String user,
                                    @FormParam("parameters") String parameters) throws Exception {
        return queryLog(StreamType.STD_OUT, appToken, appName, user, parameters);
    }

    /**
     * 获取数据_内部函数
     */
    private RestResult queryLog(StreamType type, String appToken, String appName, String user, String parameters) {

        try {
            AppAuthProtos.AppAuth appAuth = AppAuthProtos.AppAuth.newBuilder().setName(appName).setToken(appToken).build();

            JsonParameters para = new JsonParameters(parameters);
            String fullId = para.getStringNotEmpty("fullId");
            Long offset = para.getLong("offset", 0L);
            Integer lines = para.getInteger("lines", DEFAULT_LINE);

            RestReadLogRequest request = RestReadLogRequest.newBuilder()
                    .setAppAuth(appAuth)
                    .setFullId(fullId)
                    .setType(type.getValue())
                    .setOffset(offset)
                    .setLines(lines)
                    .build();

            LogStorageReadLogResponse response = (LogStorageReadLogResponse) callActor(AkkaType.LOGSTORAGE, request);

            if (response.getSuccess()) {
                LogResultVo logVo = new LogResultVo()
                        .setLog(response.getLog())
                        .setOffset(response.getOffset())
                        .setIsEnd(response.getIsEnd());
                return successResult(logVo);
            } else {
                return errorResult(response.getMessage());
            }
        } catch (Exception e) {
            return errorResult(e.getMessage());
        }
    }

}
