package org.example.learningprojectserver.service;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.AttachmentDownload;
import org.example.learningprojectserver.dto.ReadReceiptDTO;
import org.example.learningprojectserver.dto.SentMessageSummaryDTO;
import org.example.learningprojectserver.entities.MessageAttachmentEntity;
import org.example.learningprojectserver.notification.NotificationType;
import org.example.learningprojectserver.notification.dto.NotificationDTO;
import org.example.learningprojectserver.dto.MessageDTO;
import org.example.learningprojectserver.entities.MessageEntity;
import org.example.learningprojectserver.entities.UserEntity;
import org.example.learningprojectserver.mappers.MessageEntityToMessageDTOMapper;
import org.example.learningprojectserver.notification.dto.SystemMessageDTO;
import org.example.learningprojectserver.notification.publisher.NotificationEventPublisher;
import org.example.learningprojectserver.repository.MessageAttachmentRepository;
import org.example.learningprojectserver.repository.MessageRepository;
import org.example.learningprojectserver.repository.UserRepository;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.response.PageResponse;
import org.example.learningprojectserver.strategy.message.MessageRecipientStrategy;
import org.example.learningprojectserver.strategy.message.MessageRecipientStrategyFactory;
import org.example.learningprojectserver.utils.LocalizedMessages;
import org.example.learningprojectserver.utils.SmsSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class MessageService {

    public static final long MAX_ATTACHMENT_BYTES = 5L * 1024 * 1024;
    private static final String RECEIVED_MESSAGES_CACHE = "receivedMessages";
    private static final DateTimeFormatter SCHEDULE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final MessageAttachmentRepository messageAttachmentRepository;
    private final MessageRecipientStrategyFactory strategyFactory;
    private final MessageEntityToMessageDTOMapper messageEntityToMessageDTOMapper;
    private final NotificationEventPublisher notificationEventPublisher;
    private final CacheManager cacheManager;
    private final SmsSender smsSender;
    private final LocalizedMessages messages;



    public BasicResponse getRecipientTypes(String userId) {
        UserEntity user = userRepository.findUserByUserId(userId);
        if (user == null) {
            return new BasicResponse(false, "משתמש לא נימצא");
        }

        List<String> recipientTypes;
        switch (user.getRole()) {
            case SYSTEM_ADMIN:
                recipientTypes = List.of("כל מנהלי בית ספר");
                break;
            case SCHOOLMANAGER:
//                recipientTypes = List.of("כיתה", "שכבה", "מורי-שכבה", "כל-המורים", "כל-התלמידים");
                recipientTypes = List.of("כל-המורים", "כל-התלמידים");

                break;
            case TEACHER:
                recipientTypes = List.of("כיתה","כל-התלמידים");
                break;
            case STUDENT:
                recipientTypes = List.of();
                break;
            default:
                recipientTypes = List.of();
                break;
        }

        BasicResponse basicResponse = new BasicResponse(true, null);
        basicResponse.setData(recipientTypes);
        return basicResponse;
    }

    public BasicResponse sendMessage(String senderId, String recipientType, String recipientValue, String title, String content) {
        return sendMessage(senderId, recipientType, recipientValue, title, content, null, null);
    }

    /**
     * Sends (or schedules) one message to every recipient of the strategy. All copies share a
     * batchId, which is also the threadId replies attach to. With scheduledFor in the future the
     * copies are stored as pending and ScheduledMessagesJob delivers them; otherwise they are
     * delivered now. An optional attachment (up to 5MB) is stored once and shared by the copies.
     */
    @Transactional
    public BasicResponse sendMessage(String senderId, String recipientType, String recipientValue, String title,
                                     String content, LocalDateTime scheduledFor, MultipartFile file) {
        UserEntity sender = userRepository.findUserByUserId(senderId);

        if (sender == null) {
            return new BasicResponse(false, "השולח לא קיים");
        }

        if (title == null || title.trim().isEmpty()) {
            return new BasicResponse(false, "הכותרת לא יכולה להיות ריקה");
        }

        if (content == null || content.trim().isEmpty()) {
            return new BasicResponse(false, "התוכן לא יכול להיות ריק");
        }

        if (file != null && !file.isEmpty() && file.getSize() > MAX_ATTACHMENT_BYTES) {
            return new BasicResponse(false, messages.get("message.attachmentTooLarge"));
        }

        MessageRecipientStrategy strategy = strategyFactory.getStrategy(recipientType, recipientValue);
        List<UserEntity> recipients = strategy.getRecipients(senderId);

        if (recipients.isEmpty()) {
            return new BasicResponse(false, "אין נמענים לשליחת ההודעה");
        }

        LocalDateTime now = LocalDateTime.now();
        boolean scheduled = scheduledFor != null && scheduledFor.isAfter(now.plusMinutes(1));

        Long attachmentId = storeAttachment(file, senderId);
        String batchId = UUID.randomUUID().toString();

        List<MessageEntity> messageEntities = new ArrayList<>();
        for (UserEntity receiver : recipients) {
            MessageEntity message = new MessageEntity();
            message.setSender(sender);
            message.setReceiver(receiver);
            message.setTitle(title);
            message.setContent(content);
            message.setSentAt(scheduled ? scheduledFor : now);
            message.setBatchId(batchId);
            message.setThreadId(batchId);
            message.setAttachmentId(attachmentId);
            message.setScheduledFor(scheduled ? scheduledFor : null);
            message.setPending(scheduled ? Boolean.TRUE : null);
            messageEntities.add(message);
        }

        messageRepository.saveAll(messageEntities);

        if (scheduled) {
            return new BasicResponse(true, messages.get("message.scheduled",
                    scheduledFor.format(SCHEDULE_FORMAT), messageEntities.size()));
        }

        deliver(sender, title, content, recipients, true);
        return new BasicResponse(true, "ההודעה נשלחה בהצלחה ל־" + messageEntities.size() + " נמענים");
    }

    /** Called every minute by ScheduledMessagesJob: delivers scheduled messages whose time came. */
    @Transactional
    public int deliverDueScheduledMessages() {
        List<MessageEntity> due = messageRepository.findDueScheduledMessages(LocalDateTime.now());
        if (due.isEmpty()) return 0;

        Map<String, List<MessageEntity>> byBatch = new LinkedHashMap<>();
        for (MessageEntity m : due) {
            byBatch.computeIfAbsent(m.getBatchId(), k -> new ArrayList<>()).add(m);
        }
        LocalDateTime now = LocalDateTime.now();
        for (List<MessageEntity> batch : byBatch.values()) {
            batch.forEach(m -> {
                m.setPending(false);
                m.setSentAt(now);
            });
            messageRepository.saveAll(batch);
            MessageEntity first = batch.get(0);
            List<UserEntity> recipients = batch.stream().map(MessageEntity::getReceiver).toList();
            deliver(first.getSender(), first.getTitle(), first.getContent(), recipients, true);
        }
        return due.size();
    }

    private void deliver(UserEntity sender, String title, String content, Collection<UserEntity> recipients, boolean sms) {
        MessageDTO messageDTO = new MessageDTO();
        messageDTO.setTitle(title);
        messageDTO.setContent(content);
        messageDTO.setSentAt(LocalDateTime.now());
        messageDTO.setSenderName(sender.getUsername());

        NotificationDTO<SystemMessageDTO> notificationDTO = new NotificationDTO<>(
                NotificationType.SYSTEM_MESSAGE,
                new SystemMessageDTO(messageDTO)
        );

        List<String> recipientsIds = recipients.stream()
                .map(UserEntity::getUserId)
                .toList();

        notificationEventPublisher.publish(recipientsIds, notificationDTO);

        if (sms) {
            List<String> recipientsPhoneNumber = recipients.stream()
                    .map(UserEntity::getPhoneNumber)
                    .toList();
            String smsMessage = "קיבלת הודעה חדשה במערכת. לצפייה היכנס: https://schoolmanagmentsystem-z0m5.onrender.com/login";
            smsSender.sendSms(smsMessage, recipientsPhoneNumber);
        }

        recipientsIds.forEach(this::evictReceived);
    }




   @Cacheable(value = "receivedMessages", key = "#userId")
    public BasicResponse getAllRecivedMessages(String userId) {
 BasicResponse basicResponse = new BasicResponse();

List<MessageDTO> messageDTOs = messageRepository.findMessagesByRecipientUserId(userId);

basicResponse.setSuccess(true);
basicResponse.setData(messageDTOs);
return basicResponse;
    }

    /** Server-paginated inbox, newest first (not cached: pages change with every new message). */
    public BasicResponse getReceivedMessagesPage(String userId, Integer page, Integer size) {
        BasicResponse response = new BasicResponse(true, null);
        response.setData(PageResponse.of(messageRepository.findMessagesPageByRecipientUserId(userId,
                PageRequest.of(PageResponse.clampPage(page), PageResponse.clampSize(size, 20))), m -> m));
        return response;
    }

    public BasicResponse getAllSentMessages(String userId) {
        BasicResponse basicResponse = new BasicResponse();
        UserEntity user = userRepository.findUserByUserId(userId);
        if (user == null) {
            basicResponse.setSuccess(false);
            return basicResponse;
        }


        List<MessageEntity> messages = user.getSentMessages();
        List<MessageDTO> messageDTOS = messages.stream()
                .map(messageEntityToMessageDTOMapper)
                .toList();
        basicResponse.setSuccess(true);
        basicResponse.setData(messageDTOS);
        return basicResponse;

    }

    /** What the sender sent, one row per send action, with how many recipients read it. */
    public BasicResponse getSentMessageSummaries(String userId, Integer page, Integer size) {
        if (userRepository.findUserByUserId(userId) == null) {
            return new BasicResponse(false, messages.get("common.userNotFound"));
        }
        List<Object[]> rows = messageRepository.findSentBatchesBySender(userId,
                PageRequest.of(PageResponse.clampPage(page), PageResponse.clampSize(size, 20)));
        List<SentMessageSummaryDTO> summaries = rows.stream().map(r -> new SentMessageSummaryDTO(
                (String) r[0],
                (String) r[1],
                (String) r[2],
                (String) r[3],
                (LocalDateTime) r[4],
                (LocalDateTime) r[5],
                ((Number) r[6]).intValue() == 1,
                ((Number) r[7]).longValue(),
                r[8] == null ? 0 : ((Number) r[8]).longValue(),
                (Long) r[9]
        )).toList();
        BasicResponse response = new BasicResponse(true, null);
        response.setData(summaries);
        return response;
    }

    /** Who read a message the user sent and when (unread recipients last). */
    public BasicResponse getReadReceipts(String userId, String batchId) {
        List<ReadReceiptDTO> receipts = new ArrayList<>(messageRepository.findReadReceipts(userId, batchId).stream()
                .map(r -> new ReadReceiptDTO((String) r[0], (LocalDateTime) r[1]))
                .toList());
        if (receipts.isEmpty()) {
            return new BasicResponse(false, messages.get("message.notFound"));
        }
        receipts.sort(Comparator.comparing(ReadReceiptDTO::getReadAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        BasicResponse response = new BasicResponse(true, null);
        response.setData(receipts);
        return response;
    }

    @Transactional
    public BasicResponse markAsRead(String userId, Long messageId) {
        MessageEntity message = messageRepository.findById(messageId).orElse(null);
        if (message == null) {
            return new BasicResponse(false, messages.get("message.notFound"));
        }
        if (!message.getReceiver().getUserId().equals(userId)) {
            return new BasicResponse(false, messages.get("message.forbidden"));
        }
        if (message.getReadAt() == null) {
            message.setReadAt(LocalDateTime.now());
            evictReceived(userId);
        }
        return new BasicResponse(true, messages.get("message.markedRead"));
    }

    /**
     * Replies inside the conversation of messageId. The reply goes to the other side of that
     * message: a recipient answers the sender, the sender answers that recipient.
     */
    @Transactional
    public BasicResponse reply(String userId, Long messageId, String content, MultipartFile file) {
        if (content == null || content.trim().isEmpty()) {
            return new BasicResponse(false, messages.get("message.replyEmpty"));
        }
        if (file != null && !file.isEmpty() && file.getSize() > MAX_ATTACHMENT_BYTES) {
            return new BasicResponse(false, messages.get("message.attachmentTooLarge"));
        }
        MessageEntity original = messageRepository.findById(messageId).orElse(null);
        if (original == null || Boolean.TRUE.equals(original.getPending())) {
            return new BasicResponse(false, messages.get("message.notFound"));
        }
        UserEntity me;
        UserEntity other;
        if (original.getReceiver().getUserId().equals(userId)) {
            me = original.getReceiver();
            other = original.getSender();
        } else if (original.getSender().getUserId().equals(userId)) {
            me = original.getSender();
            other = original.getReceiver();
        } else {
            return new BasicResponse(false, messages.get("message.forbidden"));
        }

        // Messages sent before threads existed get their ids now.
        if (original.getBatchId() == null) {
            original.setBatchId(UUID.randomUUID().toString());
        }
        if (original.getThreadId() == null) {
            original.setThreadId(original.getBatchId());
        }

        String prefix = messages.get("message.replyPrefix");
        String title = original.getTitle().startsWith(prefix) ? original.getTitle() : prefix + original.getTitle();

        MessageEntity reply = new MessageEntity();
        reply.setSender(me);
        reply.setReceiver(other);
        reply.setTitle(title.length() > 255 ? title.substring(0, 255) : title);
        reply.setContent(content);
        reply.setSentAt(LocalDateTime.now());
        reply.setBatchId(UUID.randomUUID().toString());
        reply.setThreadId(original.getThreadId());
        reply.setParentMessageId(original.getId());
        reply.setAttachmentId(storeAttachment(file, userId));
        messageRepository.save(reply);

        deliver(me, reply.getTitle(), content, List.of(other), false);
        evictReceived(userId);

        BasicResponse response = new BasicResponse(true, messages.get("message.replySent"));
        response.setData(reply.getThreadId());
        return response;
    }

    /** The conversation as this user sees it: their own copies only, a broadcast shown once. */
    public BasicResponse getThread(String userId, String threadId) {
        List<MessageDTO> rows = messageRepository.findThreadForUser(userId, threadId);
        if (rows.isEmpty()) {
            return new BasicResponse(false, messages.get("message.notFound"));
        }
        Set<Long> mine = new HashSet<>(messageRepository.findThreadMessageIdsSentBy(userId, threadId));
        Set<String> seenBatches = new HashSet<>();
        List<MessageDTO> thread = new ArrayList<>();
        for (MessageDTO m : rows) {
            if (m.getBatchId() != null && !seenBatches.add(m.getBatchId())) continue;
            m.setMine(mine.contains(m.getId()));
            thread.add(m);
        }
        BasicResponse response = new BasicResponse(true, null);
        response.setData(thread);
        return response;
    }

    /** The attachment, or null when it does not exist or the user is not part of a message carrying it. */
    @Transactional(readOnly = true)
    public AttachmentDownload getAttachment(String userId, Long attachmentId) {
        if (messageRepository.countAttachmentAccess(userId, attachmentId) == 0) {
            return null;
        }
        MessageAttachmentEntity a = messageAttachmentRepository.findById(attachmentId).orElse(null);
        if (a == null) return null;
        return new AttachmentDownload(a.getFileName(), a.getContentType(), a.getData());
    }

    private Long storeAttachment(MultipartFile file, String uploaderUserId) {
        if (file == null || file.isEmpty()) return null;
        try {
            MessageAttachmentEntity a = new MessageAttachmentEntity();
            String name = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
            a.setFileName(name.length() > 255 ? name.substring(name.length() - 255) : name);
            a.setContentType(file.getContentType() == null ? "application/octet-stream" : file.getContentType());
            a.setSizeBytes(file.getSize());
            a.setData(file.getBytes());
            a.setUploaderUserId(uploaderUserId);
            a.setCreatedAt(LocalDateTime.now());
            return messageAttachmentRepository.save(a).getId();
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Reading the uploaded file failed", e);
        }
    }

    // Evicts after the commit when inside a transaction, so a read cannot re-cache the old list.
    private void evictReceived(String userId) {
        Cache cache = cacheManager.getCache(RECEIVED_MESSAGES_CACHE);
        if (cache == null) return;
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    cache.evict(userId);
                }
            });
        } else {
            cache.evict(userId);
        }
    }

    }
