/*
 * 蘑菇街 Inc.
 * Copyright (c) 2010-2015 All Rights Reserved.
 *
 * Author: wuya Create Date: 2015年9月21日 下午4:13:54
 */

package com.mogujie.jarvis.server.actor;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.routing.SmallestMailboxPool;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.protobuf.GeneratedMessage;
import com.mogujie.jarvis.core.domain.AppStatus;
import com.mogujie.jarvis.core.domain.MessageType;
import com.mogujie.jarvis.core.domain.Pair;
import com.mogujie.jarvis.core.util.ConfigUtils;
import com.mogujie.jarvis.dto.generate.App;
import com.mogujie.jarvis.protocol.AppAuthProtos.AppAuth;
import com.mogujie.jarvis.protocol.HeartBeatProtos.HeartBeatRequest;
import com.mogujie.jarvis.server.ServerConigKeys;
import com.mogujie.jarvis.server.domain.ActorEntry;
import com.mogujie.jarvis.server.guice.Injectors;
import com.mogujie.jarvis.server.service.AppService;
import com.mogujie.jarvis.server.util.AppTokenUtils;

public class ServerActor extends UntypedActor {

    private AppService appService = Injectors.getInjector().getInstance(AppService.class);

    private static Configuration serverConfig = ConfigUtils.getServerConfig();
    private static boolean appTokenVerifyEnable = serverConfig.getBoolean(ServerConigKeys.APP_TOKEN_VERIFY_ENABLE, true);
    private static Map<Class<?>, Pair<ActorRef, ActorEntry>> map = Maps.newConcurrentMap();
    private static List<Pair<ActorRef, List<ActorEntry>>> actorRefs = Lists.newArrayList();

    private static Logger LOGGER = LogManager.getLogger();

    public static Props props() {
        return Props.create(ServerActor.class);
    }

    private void addActor(ActorRef actorRef, List<ActorEntry> handledMessages) {
        actorRefs.add(new Pair<>(actorRef, handledMessages));
    }

    private void addActors() {
        int taskMetricsRoutingActorNum = serverConfig.getInt(ServerConigKeys.TASK_METRICS_ACTOR_NUM, 50);
        actorRefs.add(new Pair<ActorRef, List<ActorEntry>>(getContext().actorOf(TaskMetricsRoutingActor.props(taskMetricsRoutingActorNum)),
                TaskMetricsRoutingActor.handledMessages()));

        int taskActorNum = serverConfig.getInt(ServerConigKeys.TASK_ACTOR_NUM, 10);
        addActor(getContext().actorOf(TaskActor.props().withRouter(new SmallestMailboxPool(taskActorNum))), TaskActor.handledMessages());
        addActor(getContext().actorOf(AppActor.props()), AppActor.handledMessages());
        addActor(getContext().actorOf(JobActor.props()), JobActor.handledMessages());
        addActor(getContext().actorOf(HeartBeatActor.props()), HeartBeatActor.handledMessages());
        addActor(getContext().actorOf(WorkerGroupActor.props()), WorkerGroupActor.handledMessages());
        addActor(getContext().actorOf(WorkerModifyStatusActor.props()), WorkerModifyStatusActor.handledMessages());
        addActor(getContext().actorOf(WorkerRegistryActor.props()), WorkerRegistryActor.handledMessages());
        addActor(getContext().actorOf(SystemActor.props()), SystemActor.handledMessages());
        addActor(getContext().actorOf(AlarmActor.props()), AlarmActor.handledMessages());
        addActor(getContext().actorOf(BizGroupActor.props()), BizGroupActor.handledMessages());
    }

    private Object generateResponse(Class<? extends GeneratedMessage> clazz, boolean success, String msg) {
        try {
            Method method = clazz.getMethod("getDefaultInstance", new Class[] {});
            Object object = method.invoke(null, new Object[] {});
            for (Field field : object.getClass().getDeclaredFields()) {
                if ("success_".equals(field.getName())) {
                    field.setAccessible(true);
                    field.set(object, success);
                }

                if ("message_".equals(field.getName())) {
                    field.setAccessible(true);
                    field.set(object, msg);
                }
            }

            return object;
        } catch (NoSuchMethodException | SecurityException | IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
            Throwables.propagate(e);
        }

        return null;
    }

    @Override
    public void preStart() throws Exception {
        synchronized (actorRefs) {
            if (actorRefs.size() == 0) {
                addActors();
                for (Pair<ActorRef, List<ActorEntry>> pair : actorRefs) {
                    ActorRef actorRef = pair.getFirst();
                    for (ActorEntry handledMessage : pair.getSecond()) {
                        map.put(handledMessage.getRequestClass(), new Pair<ActorRef, ActorEntry>(actorRef, handledMessage));
                    }
                }
            }
        }
    }

    @Override
    public void onReceive(Object obj) throws Exception {
        Class<?> clazz = obj.getClass();

        if (!clazz.equals(HeartBeatRequest.class)) {
            LOGGER.info("receive {}", clazz.getSimpleName());
        }

        Pair<ActorRef, ActorEntry> pair = map.get(clazz);
        if (pair == null) {
            LOGGER.warn("couldn't found handle actor for {}", clazz.getSimpleName());
            unhandled(obj);
            return;
        }

        ActorEntry actorEntry = pair.getSecond();
        try {
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                if (field.getType() == AppAuth.class) {
                    field.setAccessible(true);
                    AppAuth appAuth = (AppAuth) field.get(obj);
                    String appName = appAuth.getName();
                    App app = appService.getAppByName(appName);
                    if (app.getStatus() != AppStatus.ENABLE.getValue()) {
                        throw new IllegalArgumentException("App is not enable. appName:" + appName);
                    }

                    if (appTokenVerifyEnable) {// 验证授权
                        AppTokenUtils.verifyToken(app.getAppKey(), appAuth.getToken());
                        if (actorEntry.getMessageType() == MessageType.SYSTEM && app.getAppType() != MessageType.SYSTEM.getValue()) {
                            throw new IllegalArgumentException("该app没有管理权限. appName:" + appName);
                        }
                    }
                    break;
                }
            }
        } catch (Exception e) {
            Object msg = generateResponse(actorEntry.getResponseClass(), false, e.getMessage() != null ? e.getMessage() : e.toString());
            getSender().tell(msg, getSelf());
            return;
        }
        LOGGER.info("forward message of {} from ServerActor to {}", clazz.getSimpleName(), pair.getFirst().getClass().getSimpleName());
        pair.getFirst().forward(obj, getContext());
    }

}
