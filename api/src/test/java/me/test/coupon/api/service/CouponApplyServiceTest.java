package me.test.coupon.api.service;

import me.test.coupon.api.repository.CouponRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CouponApplyServiceTest {

    @Autowired
    private CouponApplyService couponApplyService;

    @Autowired
    private CouponRepository couponRepository;


    @DisplayName("이벤트에 한번 응모하고 쿠폰이 발급되는 것을 확인한다.")
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
}
