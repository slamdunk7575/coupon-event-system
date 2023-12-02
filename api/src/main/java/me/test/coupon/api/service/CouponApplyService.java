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
    // 참고: flushall 명령어 -> Redis 키 모두 지우기

    // 문제점
    // Redis 를 활용해서 쿠폰 발급 갯수를 가져오고 -> 발급 가능한지 확인 -> RDB 에 저장하는 방식
    // 발급하는 쿠폰 갯수가 많아질수록 RDB 에 부하를 주게됨
    // RDB 가 쿠폰전용 DB 가 아니라 다양한 곳에서 사용하고 있다면 다른 서비스에까지 장애가 발생할 수 있음

    // 예:
    // MySQL 에 1분에 100개의 insert 만 가능하다고 가정
    // 10:00 쿠폰생성 10000개 요청
    // 10:01 주문생성 요청
    // 10:02 회원가입 요청

    // 10:00 에 10000개의 쿠폰생성 요청이 들어오면 1분에 100개씩 10000개를 생성하기 위해선 100분이 걸림
    // 이후에 주문생성, 회원가입 요청은 100분 이후에 처리됨
    // timeout 설정이 없다면 느리게라도 처리가 되겠지만
    // 대부분의 서비스는 timeout 설정이 되어있기 때문에 주문생성, 회원가입 요청뿐만 아니라 일부 쿠폰생성도 되지않는 오류발생
    public void apply(Long userId) {
        // long couponCount = couponRepository.count();
        long couponCount = couponCountRepository.increment();

        if (couponCount > 100) {
            return;
        }

        couponRepository.save(new Coupon(userId));
    }
}
