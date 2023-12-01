package me.test.coupon.api.service;

import me.test.coupon.api.repository.CouponRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CouponApplyServiceTest {

    @Autowired
    private CouponApplyService couponApplyService;

    @Autowired
    private CouponRepository couponRepository;


    @DisplayName("한명이 이벤트에 응모하고 쿠폰이 발급되는 것을 확인한다.")
    @Test
    void apply_one_coupon() {
        // given
        Long userId = 1L;

        // when
        couponApplyService.apply(userId);
        long count = couponRepository.count();

        // then
        assertThat(count).isEqualTo(1);
    }


    // 문제점
    // Race Condition: 두개 이상의 쓰레드(여러명)가 공유자원(쿠폰)에 access 하고 동시에 작업을 수행하려고 할때 발생함
    @DisplayName("여러명이 이벤트에 응모하고 쿠폰이 여러장 발급되는 것을 확인한다.")
    @Test
    void apply_multiple_coupon() throws InterruptedException {
        // given
        int threadCount = 1000;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch countDownLatch = new CountDownLatch(threadCount);

        // when
        for (int i = 0; i < threadCount; i++) {
            long userId = i;
            executorService.submit(() -> {
                try {
                    couponApplyService.apply(userId);
                } finally {
                    countDownLatch.countDown();
                }
            });
        }

        countDownLatch.await();
        long couponCount = couponRepository.count();

        // then
        assertThat(couponCount).isEqualTo(100);
    }
}
