package com.ssafy.alpaca.config;

import com.ssafy.alpaca.common.jwt.StompHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@RequiredArgsConstructor
@EnableWebSocketMessageBroker
public class WebSocketBrokerConfig implements WebSocketMessageBrokerConfigurer {

    private final StompHandler stompHandler;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 클라이언트에서 연결할 부분을 설정
        registry.addEndpoint("/ch")
                // 허용 도메인
                .setAllowedOriginPatterns("*")
                // fallback
                .withSockJS();
    }

    // 클라이언트가 메시지를 구독할 endpoint를 정의합니다.
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 스프링 내장 브로커를 사용
        // "/pub" 으로 시작하는 메시지를 브로커로 라우팅
        config.enableSimpleBroker("/sub");

        // "/sub"으로 시작하는 stomp message는 @Controller 내부에 @MessageMapping 메소드로 라우팅된다.
        // 추가적인 전처리가 필요할때 사용
        config.setApplicationDestinationPrefixes("/pub");

    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompHandler);
    }
}
