package me.test.coupon.api.service;

import me.test.coupon.api.domain.Coupon;
import me.test.coupon.api.repository.CouponRepository;
import org.springframework.stereotype.Service;

@Service
public class CouponApplyService {

    private final CouponRepository couponRepository;

    public CouponApplyService(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    public void apply(Long userId) {
        long couponCount = couponRepository.count();

        if (couponCount > 100) {
            return;
        }

        couponRepository.save(new Coupon(userId));
    }
}
