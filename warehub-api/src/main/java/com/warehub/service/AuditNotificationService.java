package com.warehub.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 审核待办实时推送（SSE）。
 * 维护当前在线的 SSE 连接，审核通过/驳回后向所有连接推送最新待办数量，
 * 前端据此刷新待办角标，无需频繁轮询。
 */
@Service
public class AuditNotificationService {

    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    /** 订阅：返回一个长连接 SseEmitter（无超时，由前端关闭或心跳维持） */
    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError((e) -> emitters.remove(emitter));
        emitters.add(emitter);
        return emitter;
    }

    /** 推送最新待办数量给所有在线连接 */
    public void pushTodoCount(long count) {
        for (SseEmitter em : emitters) {
            try {
                em.send(SseEmitter.event().name("todoCount").data(count));
            } catch (IOException e) {
                emitters.remove(em);
            }
        }
    }

    public int clientCount() {
        return emitters.size();
    }
}
