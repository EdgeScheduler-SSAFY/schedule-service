package com.edgescheduler.scheduleservice.client;

import com.edgescheduler.scheduleservice.dto.response.MemberInfoResponse;
import com.edgescheduler.scheduleservice.dto.response.UserInfoResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class UserServiceClient {

    private final WebClient webClient;

    public UserServiceClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("http://user-service").build();
    }

    public UserInfoResponse getUserName(Integer id) {
        return webClient.get()
            .uri("/members/{id}", id)
            .retrieve().bodyToMono(UserInfoResponse.class).block();
    }

    public MemberInfoResponse getMemberInfo(Integer id) {
        return webClient.get()
            .uri("/auth/me", id)
            .retrieve().bodyToMono(MemberInfoResponse.class).block();
    }
}
