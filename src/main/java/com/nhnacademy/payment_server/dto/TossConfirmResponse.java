package com.nhnacademy.payment_server.dto;


import com.nhnacademy.payment_server.entity.PaymentStatus;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TossConfirmResponse {
    // Toss가 주는 JSON 응답을 받기위한 DTO
    private String status;
    private OffsetDateTime requestedAt; // LocalDateTime 으로 했다가 오류남 - 한국 시간 기준 이라는 정확한 타임존 정보가 포함된 포맷사용
    private OffsetDateTime approvedAt;
    private String orderId; // DB에 있는 Long 주문 번호랑 다름
    private String paymentKey;
    private Long totalAmount;
    private String method;

    // String으로 받은 status를 미리 정의된 enum 으로 바꿔야함
    public PaymentStatus getStatusEnum() {
        if (this.status == null) {
            return null;
        }
        try {
            return PaymentStatus.valueOf(this.status);
        } catch (IllegalArgumentException e) {
            // Enum에 없는 status 즉 "PARTIAL_CANCELED" 같이 정의하지 않은 값을 줘도 에러가 나지 않고 null을 반환
            return null;
        }
    }
}


/* 실제 토스가 주는 Json Response
{
  "mId": "tosspayments",
  "lastTransactionKey": "9C62B18EEF0DE3EB7F4422EB6D14BC6E",
  "paymentKey": "5EnNZRJGvaBX7zk2yd8ydw26XvwXkLrx9POLqKQjmAw4b0e1",
  "orderId": "a4CWyWY5m89PNh7xJwhk1",
  "orderName": "토스 티셔츠 외 2건",
  "taxExemptionAmount": 0,
  "status": "DONE",
  "requestedAt": "2024-02-13T12:17:57+09:00",
  "approvedAt": "2024-02-13T12:18:14+09:00",
  "useEscrow": false,
  "cultureExpense": false,
  "card": {
    "issuerCode": "71",
    "acquirerCode": "71",
    "number": "12345678****000*",
    "installmentPlanMonths": 0,
    "isInterestFree": false,
    "interestPayer": null,
    "approveNo": "00000000",
    "useCardPoint": false,
    "cardType": "신용",
    "ownerType": "개인",
    "acquireStatus": "READY",
    "amount": 1000
  },
  "virtualAccount": null,
  "transfer": null,
  "mobilePhone": null,
  "giftCertificate": null,
  "cashReceipt": null,
  "cashReceipts": null,
  "discount": null,
  "cancels": null,
  "secret": null,
  "type": "NORMAL",
  "easyPay": {
    "provider": "토스페이",
    "amount": 0,
    "discountAmount": 0
  },
  "country": "KR",
  "failure": null,
  "isPartialCancelable": true,
  "receipt": {
    "url": "https://dashboard.tosspayments.com/receipt/redirection?transactionId=tviva20240213121757MvuS8&ref=PX"
  },
  "checkout": {
    "url": "https://api.tosspayments.com/v1/payments/5EnNZRJGvaBX7zk2yd8ydw26XvwXkLrx9POLqKQjmAw4b0e1/checkout"
  },
  "currency": "KRW",
  "totalAmount": 1000,
  "balanceAmount": 1000,
  "suppliedAmount": 909,
  "vat": 91,
  "taxFreeAmount": 0,
  "metadata": null,
  "method": "카드",
  "version": "2022-11-16"
}
*/