package me.test.coupon.api.repository;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AppliedUserRepository {

    private final RedisTemplate<String, String> redisTemplate;

    public AppliedUserRepository(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // (추가 요구사항) 발급 가능한 쿠폰 수를 1인당 1개로 제한하기
    // Redis 에서 지원하는 Set 자료구조를 활용해서 쿠폰발급 갯수를 제한하도록 처리
    public Long add(Long userId) {
        return redisTemplate
                .opsForSet()
                .add("applied_user", userId.toString());
    }
}
