package com.tekhive.spring.security.postgresql.chat.repository;

import com.tekhive.spring.security.postgresql.chat.entity.ChatMessage;
import com.tekhive.spring.security.postgresql.chat.entity.MessageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    long countBySenderIdAndRecipientIdAndStatus(Long senderId, Long recipientId, MessageStatus status);

    List<ChatMessage> findByChatId(String chatId);
    List<ChatMessage> findChatMessageBySenderIdAndRecipientId(Long senderId, Long recipientId);

    @Transactional
    Integer deleteAllByRecipientIdAndSenderId(Long recipientId, Long senderId);
}
