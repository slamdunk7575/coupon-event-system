package me.test.coupon.api.service;

import me.test.coupon.api.domain.Coupon;
import me.test.coupon.api.repository.CouponCountRepository;
import me.test.coupon.api.repository.CouponRepository;
import org.springframework.stereotype.Service;

@Service
public class CouponApplyService {

    private final CouponRepository couponRepository;

    private final CouponCountRepository couponCountRepository;

    public CouponApplyService(CouponRepository couponRepository,
                              CouponCountRepository couponCountRepository) {
        this.couponRepository = couponRepository;
        this.couponCountRepository = couponCountRepository;
    }

    // Race Condition 해결 방법
    // 1. synchronized 활용
    // 서버가 여러대인 경우 다시 Race Condition 발생함

    // 2. MySQL 락
    // 발급된 쿠폰을 가져오는 것부터 ~ 쿠폰 발급까지 Lock 을 걸어야함 -> Lock 을 거는 구간이 길어져서 성능이 안좋을 수 있음
    // 예:
    // 10:00 lock 획득
    // 10:01 쿠폰개수 획득
    // 10:02 쿠폰 생성
    // 10:03 lock 해제
    // 위처럼 A 라는 유저가 10시에 lock 을 획득하였고 10:03 에 lock 을 해제할 때,
    // 다른 유저들은 10:00 ~ 10:03 까지 쿠폰생성 메소드에 진입을 하지 못하고 lock 을 획득할 때까지 대기

    // 3. Redis 활용
    // Single-Thread 로 동작하여 어떤 시점에 쿠폰에 접근할 수 있는 Thread 를 1개로 제한하여
    // Race Condition 해결 할 수 있을뿐만 아니라
    // incr 명령어는 성능도 빠름 (incr: 숫자를 1 증가시키고 증가된 값을 리턴하는 명령어)
    // 참고: flushall 명령어 -? Redis 키 모두 지우기

    public void apply(Long userId) {
        // long couponCount = couponRepository.count();
        long couponCount = couponCountRepository.increment();

        if (couponCount > 100) {
            return;
        }

        couponRepository.save(new Coupon(userId));
    }
}
