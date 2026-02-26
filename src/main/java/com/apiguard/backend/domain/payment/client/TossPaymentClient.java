package com.apiguard.backend.domain.payment.client;

import com.apiguard.backend.global.exception.PaymentException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Base64;
import java.util.Map;

@Component
public class TossPaymentClient {

    private final RestClient restClient;

    public TossPaymentClient(
        @Value("${toss.payments.secret-key}") String secretKey
    ) {
        String encodedKey = Base64.getEncoder()
            .encodeToString((secretKey + ":").getBytes());

        this.restClient = RestClient.builder()
            .baseUrl("https://api.tosspayments.com")
            .defaultHeader("Authorization", "Basic " + encodedKey)
            .build();
    }

    public TossConfirmResponse confirmPayment(String paymentKey, String orderId, Long amount) {
        try {
            return restClient.post()
                .uri("/v1/payments/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                    "paymentKey", paymentKey,
                    "orderId", orderId,
                    "amount", amount
                ))
                .retrieve()
                .body(TossConfirmResponse.class);
        } catch (RestClientResponseException e) {
            throw new PaymentException("토스 결제 승인 실패: " + e.getResponseBodyAsString());
        }
    }
}
