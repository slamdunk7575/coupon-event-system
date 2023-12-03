package me.study.consumer.consumer;

import me.study.consumer.domain.Coupon;
import me.study.consumer.domain.FailedEvent;
import me.study.consumer.repository.CouponRepository;
import me.study.consumer.repository.FailedEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class CouponCreateConsumer {

    private final CouponRepository couponRepository;

    private final FailedEventRepository failedEventRepository;

    private final Logger logger = LoggerFactory.getLogger(CouponCreateConsumer.class);

    public CouponCreateConsumer(CouponRepository couponRepository, FailedEventRepository failedEventRepository) {
        this.couponRepository = couponRepository;
        this.failedEventRepository = failedEventRepository;
    }

    // (예외상황) Consumer 에서 토픽에 있는 데이터를 가져간 후 쿠폰을 발급하는 과정에서 에러가 발생한다면?
    // 쿠폰은 발급되지 않았는데 발급한 쿠폰 갯수만 올라가는 문제가 발생할 수 있음
    // 결과적으로 100개 보다 적은 수량의 쿠폰이 발급 될 수 있음

    // (해결)
    // 쿠폰 발급시 오류가 발생하면 백업 데이터(FailedEvent)와 로그를 남기고
    // 이후에 FailedEvent 에 쌓인 데이터들을 배치에서 주기적으로 읽어서 쿠폰을 재발급
    // api - Topic - Consumer - FailedEvent - 배치 프로그램 - Coupon
    @KafkaListener(topics = "coupon_create", groupId = "group_1")
    public void listener(Long userId) {
        try {
            couponRepository.save(new Coupon(userId));
        } catch (Exception e) {
            logger.error("failed to create coupon :: " + userId);
            failedEventRepository.save(new FailedEvent(userId));
        }
    }
}
