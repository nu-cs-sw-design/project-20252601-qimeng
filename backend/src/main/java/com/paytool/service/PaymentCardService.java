package com.paytool.service;

import com.paytool.exception.CustomException;
import com.paytool.model.Group;
import com.paytool.model.GroupMember;
import com.paytool.model.GroupStatus;
import com.paytool.model.MemberStatus;
import com.paytool.model.PaymentCard;
import com.paytool.model.PaymentCardStatus;
import com.paytool.repository.GroupMemberRepository;
import com.paytool.repository.GroupRepository;
import com.paytool.repository.PaymentCardRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentCardService {
    private static final Logger logger = LoggerFactory.getLogger(PaymentCardService.class);
    
    private final PaymentCardRepository paymentCardRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupPublisher groupPublisher;

    @Transactional
    public PaymentCard generatePaymentCard(Long groupId) {
        logger.debug("Generating payment card for group: {}", groupId);
        
        Group group = groupRepository.findById(groupId)
            .orElseThrow(() -> new CustomException("Group not found"));

        List<GroupMember> members = groupMemberRepository.findByGroup(group);

        // Business rule 1: Check if member count matches expected
        if (members.size() != group.getTotalPeople()) {
            throw new CustomException(
                String.format("Not enough members to generate payment card. Expected %d, but got %d",
                    group.getTotalPeople(), members.size())
            );
        }

        // Business rule 2: Check if all members have agreed
        boolean allAgreed = members.stream()
            .allMatch(member -> member.getStatus() == MemberStatus.AGREED);

        if (!allAgreed) {
            throw new CustomException("Not all members have agreed to the payment");
        }

        // Business rule 3: Check if group is in valid status
        if (group.getStatus() != GroupStatus.ACTIVE && group.getStatus() != GroupStatus.PENDING) {
            throw new CustomException("Cannot generate payment card for group in status: " + group.getStatus());
        }

        // Generate payment card
        PaymentCard card = new PaymentCard();
        card.setGroup(group);
        card.setCardNumber(generateCardNumber());
        card.setAmount(group.getTotalAmount());
        card.setStatus(PaymentCardStatus.ACTIVE);

        PaymentCard savedCard = paymentCardRepository.save(card);
        logger.info("Payment card generated successfully with ID: {} for group: {}", 
            savedCard.getId(), groupId);

        // Update group status to COMPLETED
        group.setStatus(GroupStatus.COMPLETED);
        Group updatedGroup = groupRepository.save(group);
        
        // Publish group status update event
        groupPublisher.publishGroupStatus(groupId.toString(), updatedGroup);
        
        logger.info("Group {} status updated to COMPLETED", groupId);
        return savedCard;
    }

    @Transactional(readOnly = true)
    public PaymentCard findById(Long id) {
        return paymentCardRepository.findById(id)
            .orElseThrow(() -> new CustomException("Payment card not found"));
    }

    @Transactional(readOnly = true)
    public List<PaymentCard> findByGroup(Long groupId) {
        Group group = groupRepository.findById(groupId)
            .orElseThrow(() -> new CustomException("Group not found"));
        return paymentCardRepository.findByGroup(group);
    }

    private String generateCardNumber() {
        // Generate a 16-digit card number using current time in nanoseconds
        return String.format("%016d", System.nanoTime() % 10000000000000000L);
    }
}

