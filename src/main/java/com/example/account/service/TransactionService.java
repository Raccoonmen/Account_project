package com.example.account.service;

import com.example.account.domain.Account;
import com.example.account.domain.AccountUser;
import com.example.account.domain.Transaction;
import com.example.account.dto.TransactionDto;
import com.example.account.exception.AccountException;
import com.example.account.repository.AccountRepository;
import com.example.account.repository.AccountUserRepository;
import com.example.account.repository.TransactionRepository;
import com.example.account.type.AccountStatus;
import com.example.account.type.ErrorCode;
import com.example.account.type.TransactionResultType;
import com.example.account.type.TransactionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final AccountUserRepository accountUserRepository;
    private final AccountRepository accountRepository;

    /**
     *사용자가 없는경우, 계좌가 없는 경우, 아이디 계좌소유주가 다른경우,
     * 계좌가 해지상태인경우, 거래금액이 더 큰 경우, 거래금액이 너무 작은경우
     */

    @Transactional
    public TransactionDto useBalance(Long userId, String accountNumber,
                                     Long amount){

        AccountUser user = accountUserRepository.findById(userId)
                .orElseThrow(() -> new AccountException(ErrorCode.USER_NOT_FOUND));
        Account account = accountRepository.findByAccountNumber(accountNumber)
                        .orElseThrow(() -> new AccountException(ErrorCode.ACCOUNT_NOT_FOUND))


        validateUseBalance(user, account, amount);

        account.useBalance(amount);

        return TransactionDto.fromEntity(transactionRepository.save(
                Transaction.builder()
                        .transactionType(TransactionType.USE)
                        .transactionResultType(TransactionResultType.S)
                        .account(account)
                        .amount(amount)
                        .balanceSnapshot(account.getBalance())
                        .transactionId(UUID.randomUUID().toString().replace("-", ""))
                        .transactedAt(LocalDateTime.now())
                        .build()
                )
        );
    }

    private void validateUseBalance(AccountUser user, Account account, Long amount) {
        if (!Objects.equals(user.getId(), account.getAccountUser().getId())){
            throw new AccountException(ErrorCode.USER_ACCOUNT_UN_MATCH);
        }
        if(account.getAccountStatus() != AccountStatus.IN_USE){
            throw new AccountException(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED);
        }
        if (account.getBalance() < amount) {
            throw new AccountException(ErrorCode.AMOUNT_EXCEED_BALANCE);
        }
    }


}
