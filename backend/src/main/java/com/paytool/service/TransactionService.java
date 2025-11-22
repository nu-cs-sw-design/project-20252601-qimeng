package com.paytool.service;

import com.paytool.dto.CreateTransactionInput;
import com.paytool.exception.CustomException;
import com.paytool.model.Transaction;
import com.paytool.model.TransactionStatus;
import com.paytool.model.User;
import com.paytool.repository.TransactionRepository;
import com.paytool.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TransactionService {
    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);
    
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Transactional
    public Transaction createTransaction(CreateTransactionInput input) {
        logger.debug("Creating transaction from sender {} to receiver {}", 
            input.getSenderId(), input.getReceiverId());
        
        Long senderId = Long.parseLong(input.getSenderId());
        Long receiverId = Long.parseLong(input.getReceiverId());
        
        User sender = userRepository.findById(senderId)
            .orElseThrow(() -> new CustomException("Sender not found"));
        
        User receiver = userRepository.findById(receiverId)
            .orElseThrow(() -> new CustomException("Receiver not found"));
        
        // Business rule: sender and receiver cannot be the same
        if (senderId.equals(receiverId)) {
            throw new CustomException("Sender and receiver cannot be the same");
        }

        Transaction transaction = new Transaction();
        transaction.setSender(sender);
        transaction.setReceiver(receiver);
        transaction.setAmount(input.getAmount());
        transaction.setDescription(input.getDescription());
        transaction.setStatus(TransactionStatus.PENDING);

        Transaction savedTransaction = transactionRepository.save(transaction);
        logger.info("Transaction created successfully with ID: {}", savedTransaction.getId());
        return savedTransaction;
    }

    @Transactional
    public Transaction updateTransactionStatus(Long id, TransactionStatus status) {
        logger.debug("Updating transaction {} status to {}", id, status);
        
        Transaction transaction = transactionRepository.findById(id)
            .orElseThrow(() -> new CustomException("Transaction not found"));

        // Business rule: validate status transition
        validateStatusTransition(transaction.getStatus(), status);

        transaction.setStatus(status);
        Transaction updatedTransaction = transactionRepository.save(transaction);
        logger.info("Transaction {} status updated to {}", id, status);
        return updatedTransaction;
    }

    @Transactional(readOnly = true)
    public Optional<Transaction> findById(Long id) {
        return transactionRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Transaction findByIdOrThrow(Long id) {
        return transactionRepository.findById(id)
            .orElseThrow(() -> new CustomException("Transaction not found"));
    }

    @Transactional(readOnly = true)
    public List<Transaction> findAll() {
        return transactionRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Transaction> findByUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new CustomException("User not found"));
        return transactionRepository.findBySenderOrReceiver(user, user);
    }

    @Transactional(readOnly = true)
    public List<Transaction> findBySender(Long userId) {
        User sender = userRepository.findById(userId)
            .orElseThrow(() -> new CustomException("Sender not found"));
        return transactionRepository.findBySender(sender);
    }

    @Transactional(readOnly = true)
    public List<Transaction> findByReceiver(Long userId) {
        User receiver = userRepository.findById(userId)
            .orElseThrow(() -> new CustomException("Receiver not found"));
        return transactionRepository.findByReceiver(receiver);
    }

    private void validateStatusTransition(TransactionStatus currentStatus, TransactionStatus newStatus) {
        // Define valid status transitions
        switch (currentStatus) {
            case PENDING:
                // Can transition to COMPLETED, FAILED, or CANCELLED
                if (newStatus == TransactionStatus.PENDING) {
                    throw new CustomException("Transaction is already in PENDING status");
                }
                break;
            case COMPLETED:
                throw new CustomException("Cannot change status of a completed transaction");
            case FAILED:
                throw new CustomException("Cannot change status of a failed transaction");
            case CANCELLED:
                throw new CustomException("Cannot change status of a cancelled transaction");
        }
    }
}

