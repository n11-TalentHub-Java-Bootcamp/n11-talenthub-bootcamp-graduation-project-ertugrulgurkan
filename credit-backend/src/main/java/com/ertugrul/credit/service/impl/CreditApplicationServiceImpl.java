package com.ertugrul.credit.service.impl;

import com.ertugrul.credit.dto.CreditApplicationRequestDto;
import com.ertugrul.credit.dto.CreditApplicationResultDto;
import com.ertugrul.credit.entity.CreditApplication;
import com.ertugrul.credit.enums.CreditApplicationResult;
import com.ertugrul.credit.mapper.CreditApplicationMapper;
import com.ertugrul.credit.rule.CreditAmountCalculator;
import com.ertugrul.credit.service.CreditApplicationService;
import com.ertugrul.credit.service.CreditScoreService;
import com.ertugrul.credit.service.ValidationService;
import com.ertugrul.credit.service.entityservice.CreditApplicationEntityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreditApplicationServiceImpl implements CreditApplicationService {
    private final UserServiceImpl userService;
    private final CreditApplicationEntityService creditApplicationEntityService;
    private final ValidationService validationService;
    private final CreditAmountCalculator creditAmountCalculator;
    private final CreditScoreService creditScoreService;

    @Transactional
    @Override
    public CreditApplicationResultDto saveCreditApplication(CreditApplicationRequestDto creditApplicationRequestDto) {
        CreditApplication creditApplication = CreditApplicationMapper.INSTANCE.convertCreditApplicationRequestDtoToCreditApplication(creditApplicationRequestDto);
        userService.saveUserToEntity(creditApplication.getUser());
        fillCreditApplicationEntity(creditApplication);
        CreditApplication savedApplication = creditApplicationEntityService.save(creditApplication);
        return CreditApplicationMapper.INSTANCE.convertCreditApplicationToCreditApplicationResultDto(savedApplication);
    }

    private void fillCreditApplicationEntity(CreditApplication creditApplication) {
        Long creditScore = creditScoreService.calculateCreditScore(creditApplication.getMonthlyIncome(), creditApplication.getUser().getNationalIdNumber());
        creditApplication.setApplicationDate(LocalDateTime.now());
        creditApplication.setCreditScore(creditScore);
        double creditAmount = creditAmountCalculator.getCreditLimitAmount(creditApplication);
        CreditApplicationResult creditApplicationResult = creditAmount > 0 ? CreditApplicationResult.APPROVED : CreditApplicationResult.REJECTED;
        creditApplication.setCreditApplicationResult(creditApplicationResult);
        creditApplication.setCreditLimitAmount(creditAmount);
        log.info("Credit Application Result: " + creditApplicationResult);
        log.info("Credit Limit Amount: " + creditAmount);
    }

    @Override
    public CreditApplicationResultDto findCreditApplicationByNationalIdNumberAndBirthDate(String nationalIdNumber, LocalDate birthDate) {
        Optional<CreditApplication> creditApplicationByNationalIdNumberAndBirthDate = creditApplicationEntityService.findCreditApplicationByNationalIdNumberAndBirthDate(nationalIdNumber, birthDate);
        CreditApplication creditApplication = validationService.validateCreditApplication(creditApplicationByNationalIdNumberAndBirthDate);
        return CreditApplicationMapper.INSTANCE.convertCreditApplicationToCreditApplicationResultDto(creditApplication);
    }
}