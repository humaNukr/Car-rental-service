package com.example.carrental.repository.spec;

import com.example.carrental.dto.car.CarSearchParameters;
import com.example.carrental.entity.Car;
import com.example.carrental.entity.Rental;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class CarSpecificationBuilder {
    public Specification<Car> build(CarSearchParameters params) {
        Specification<Car> spec = Specification.unrestricted();

        if (params.brands() != null && params.brands().length > 0) {
            spec = spec.and((root, query, cb)
                    -> root.get("brand").in(Arrays.asList(params.brands())));
        }

        if (params.models() != null && params.models().length > 0) {
            spec = spec.and((root, query, cb)
                    -> root.get("model").in(Arrays.asList(params.models())));
        }

        if (params.types() != null && params.types().length > 0) {
            spec = spec.and((root, query, cb)
                    -> root.get("type").in(Arrays.asList(params.types())));
        }

        if (params.colors() != null && params.colors().length > 0) {
            spec = spec.and((root, query, cb)
                    -> root.get("color").in(Arrays.asList(params.colors())));
        }

        if (params.status() != null) {
            spec = spec.and((root, query, cb)
                    -> cb.equal(root.get("status"), params.status()));
        }

        if (params.minDailyFee() != null) {
            spec = spec.and((root, query, cb)
                    -> cb.ge(root.get("dailyFee"), params.minDailyFee()));
        }

        if (params.maxDailyFee() != null) {
            spec = spec.and((root, query, cb)
                    -> cb.le(root.get("dailyFee"), params.maxDailyFee()));
        }

        if (params.startDate() != null && params.endDate() != null) {
            spec = spec.and((root, query, cb) -> {
                assert query != null;
                Subquery<Long> subquery = query.subquery(Long.class);
                Root<Rental> rentalRoot = subquery.from(Rental.class);

                subquery.select(rentalRoot.get("id"));
                subquery.where(
                        cb.equal(rentalRoot.get("car"), root),
                        cb.isNull(rentalRoot.get("actualReturnDate")),
                        cb.lessThanOrEqualTo(rentalRoot.get("rentalDate"), params.endDate()),
                        cb.greaterThanOrEqualTo(rentalRoot.get("returnDate"), params.startDate())
                );

                return cb.not(cb.exists(subquery));
            });

        }

        return spec;
    }
}