package com.example.demo.config;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

public class NativeWebSocketHandler extends TextWebSocketHandler {
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        WebSocketSessionManager.addSession(session);
        System.out.println("[WebSocket] 연결됨: " + session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) {
        WebSocketSessionManager.removeSession(session);
        System.out.println("[WebSocket] 연결 종료: " + session.getId());
    }
}