package achlaq.co.transactionreconengine.service;

import achlaq.co.transactionreconengine.repository.TransactionRepository;
import achlaq.co.transactionreconengine.repository.projection.HighValueUserProjection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticService {

    private final TransactionRepository transactionRepo;

    @Cacheable(value = "highValueUsers", unless = "#result.isEmpty()")
    public List<HighValueUserProjection> getHighValueUsers() {
        return transactionRepo.findHighValueUsersAboveAverage();
    }
}