package com.tekhive.spring.security.postgresql.chat.service;

import com.tekhive.spring.security.postgresql.chat.entity.ChatMessage;
import com.tekhive.spring.security.postgresql.chat.entity.MessageStatus;
import com.tekhive.spring.security.postgresql.chat.exception.ResourceNotFoundException;
import com.tekhive.spring.security.postgresql.chat.repository.ChatMessageRepository;
import lombok.var;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomService chatRoomService;

    public ChatMessageService(ChatMessageRepository chatMessageRepository, ChatRoomService chatRoomService) {
        this.chatMessageRepository = chatMessageRepository;
        this.chatRoomService = chatRoomService;
    }

    public ChatMessage save(ChatMessage chatMessage) {
        chatMessage.setStatus(MessageStatus.RECEIVED);
        chatMessageRepository.save(chatMessage);
        return chatMessage;
    }

    public long countNewMessages(Long senderId, Long recipientId) {
        return chatMessageRepository.countBySenderIdAndRecipientIdAndStatus(
                senderId, recipientId, MessageStatus.RECEIVED);
    }

    public List<ChatMessage> findChatMessages(Long senderId, Long recipientId) {
        var chatId = chatRoomService.getChatId(senderId, recipientId, false);

        var messages =
                chatId.map(cId -> chatMessageRepository.findByChatId(cId)).orElse(new ArrayList<>());

        if(messages.size() > 0) {
            updateStatuses(senderId, recipientId, MessageStatus.DELIVERED);
        }

        return messages;
    }

    public ChatMessage findById(Long id) {
        return chatMessageRepository
                .findById(id)
                .map(chatMessage -> {
                    chatMessage.setStatus(MessageStatus.DELIVERED);
                    return chatMessageRepository.save(chatMessage);
                })
                .orElseThrow(() ->
                        new ResourceNotFoundException("Can not find message (" + id + ")"));
    }

    public void updateStatuses(Long senderId, Long recipientId, MessageStatus status) {
        List<ChatMessage> foundMessages = chatMessageRepository.findChatMessageBySenderIdAndRecipientId(senderId, recipientId);

        for (ChatMessage chatMessage : foundMessages) {
            chatMessage.setStatus(status);
            chatMessageRepository.save(chatMessage);
        }
    }

    public boolean deleteMessages(Long senderId, Long recipientId) {
        Integer first = chatMessageRepository.deleteAllByRecipientIdAndSenderId(recipientId, senderId);
        Integer second = chatMessageRepository.deleteAllByRecipientIdAndSenderId(senderId, recipientId);
        return first != null && second != null;
    }

}