package me.test.coupon.api.service;

import me.test.coupon.api.repository.CouponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

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

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @BeforeEach
    void redis_init() {
        redisTemplate
                .getConnectionFactory()
                .getConnection()
                .flushAll();
    }

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


    //==> 결과
    // expected: 100L
    // but was: 119L
    //==> 문제점
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

        // 테스트 케이스가 실패하는 이유 -> 데이터 처리가 실시간이 아니기 때문
        // Producer 가 토픽에 메시지를 전송 -> 토픽 -> Consumer 는 데이터 수신 상태였다가
        // 토픽에 메시지가 전송되면 데이터를 처리 (쿠폰생성)
        // 그 사이에 테스트 케이스가 종료되기 때문에 테스트가 실패하는것
        // 예:
        // expected: 100L
        // but was: 32L

        // 임의로 Thread.sleep 을 주어서 실제로 쿠폰이 100개 생성되는지 확인
        Thread.sleep(10000);

        long couponCount = couponRepository.count();

        // then
        assertThat(couponCount).isEqualTo(100);
    }

    @DisplayName("User 당 오직 하나의 쿠폰만 발급되는 것을 확인한다.(추가 요구사항)")
    @Test
    void apply_one_coupon_per_user() throws InterruptedException {
        // given
        int threadCount = 1000;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch countDownLatch = new CountDownLatch(threadCount);

        // when
        // 한명의 유저(UserId: 1L)가 1000번의 쿠폰생성 요청을 보내도 오직 1개 쿠폰만 생성 확인
        for (int i = 0; i < threadCount; i++) {
            long userId = i;
            executorService.submit(() -> {
                try {
                    couponApplyService.apply(1L);
                } finally {
                    countDownLatch.countDown();
                }
            });
        }

        countDownLatch.await();

        Thread.sleep(10000);

        long couponCount = couponRepository.count();

        // then
        assertThat(couponCount).isEqualTo(1);
    }
}
