package com.harsha.minichat.realtime;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

	private final RealtimeHandler handler;

	public WebSocketConfig(RealtimeHandler handler) {
		this.handler = handler;
	}

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		// Browsers may only connect from pages served by this backend (same origin), which is Spring's default.
		registry.addHandler(handler, "/api/v4/websocket");
	}
}
