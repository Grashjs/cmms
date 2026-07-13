package com.grash.service;

import com.grash.advancedsearch.SearchCriteria;
import com.grash.advancedsearch.SpecificationBuilder;
import com.grash.dto.NotificationPatchDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.NotificationMapper;
import com.grash.model.Notification;
import com.grash.model.OwnUser;
import com.grash.model.PushNotificationToken;
import com.grash.repository.NotificationRepository;
import io.github.jav.exposerversdk.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final PushNotificationTokenService pushNotificationTokenService;
    private final SimpMessageSendingOperations messagingTemplate;

    @Async
    public Notification create(Notification notification) {
        Notification savedNotification = notificationRepository.save(notification);
        messagingTemplate.convertAndSend("/notifications/" + notification.getUser().getId(), savedNotification);
        return savedNotification;
    }

    @Async
    public void createMultiple(List<Notification> notifications, boolean mobile, String title) {
        List<Notification> savedNotifications = notificationRepository.saveAll(notifications);
        savedNotifications.forEach(notification ->
                messagingTemplate.convertAndSend("/notifications/" + notification.getUser().getId(), notification));
        if (mobile && !notifications.isEmpty())
            try {
                sendPushNotifications(notifications.stream().map(Notification::getUser).collect(Collectors.toList()),
                        title, notifications.get(0).getMessage(), new HashMap<String, Object>() {{
                            put("type", notifications.get(0).getNotificationType());
                            put("id", notifications.get(0).getResourceId());
                        }});
            } catch (PushClientException | InterruptedException e) {
                e.printStackTrace();
            }
    }

    public Notification update(Long id, NotificationPatchDTO notificationsPatchDTO) {
        if (notificationRepository.existsById(id)) {
            Notification savedNotification = notificationRepository.findById(id).get();
            return notificationRepository.save(notificationMapper.updateNotification(savedNotification,
                    notificationsPatchDTO));
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    public Collection<Notification> getAll() {
        return notificationRepository.findAll();
    }

    public void delete(Long id) {
        notificationRepository.deleteById(id);
    }

    public Optional<Notification> findById(Long id) {
        return notificationRepository.findById(id);
    }

    public Collection<Notification> findByUser(Long id) {
        return notificationRepository.findByUser_Id(id);
    }

    public Page<Notification> findBySearchCriteria(SearchCriteria searchCriteria) {
        SpecificationBuilder<Notification> builder = new SpecificationBuilder<>();
        searchCriteria.getFilterFields().forEach(builder::with);
        Pageable page = PageRequest.of(searchCriteria.getPageNum(), searchCriteria.getPageSize(),
                searchCriteria.getDirection(), searchCriteria.getSortField());
        return notificationRepository.findAll(builder.build(), page);
    }

    public void sendPushNotifications(Collection<OwnUser> users, String title, String message,
                                      Map<String, Object> data) throws PushClientException, InterruptedException {

        List<String> tokens = new ArrayList<>();
        users.forEach(user -> {
            Optional<PushNotificationToken> optionalPushNotificationToken =
                    pushNotificationTokenService.findByUser(user.getId());
            if (optionalPushNotificationToken.isPresent()) {
                String token = optionalPushNotificationToken.get().getToken();
                if (PushClient.isExponentPushToken(token))
                    tokens.add(token);
            }
        });

        ExpoPushMessage expoPushMessage = new ExpoPushMessage();
        expoPushMessage.getTo().addAll(tokens);
        expoPushMessage.setTitle(title);
        expoPushMessage.setBody(message);
        expoPushMessage.setData(data);

        List<ExpoPushMessage> expoPushMessages = new ArrayList<>();
        expoPushMessages.add(expoPushMessage);

        PushClient client = new PushClient();
        List<List<ExpoPushMessage>> chunks = client.chunkPushNotifications(expoPushMessages);

        List<CompletableFuture<List<ExpoPushTicket>>> messageRepliesFutures = new ArrayList<>();

        for (List<ExpoPushMessage> chunk : chunks) {
            messageRepliesFutures.add(client.sendPushNotificationsAsync(chunk));
        }

        // Wait for each completable future to finish
        List<ExpoPushTicket> allTickets = new ArrayList<>();
        for (CompletableFuture<List<ExpoPushTicket>> messageReplyFuture : messageRepliesFutures) {
            try {
                allTickets.addAll(messageReplyFuture.get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        List<ExpoPushMessageTicketPair<ExpoPushMessage>> zippedMessagesTickets =
                client.zipMessagesTickets(expoPushMessages, allTickets);

        List<ExpoPushMessageTicketPair<ExpoPushMessage>> okTicketMessages =
                client.filterAllSuccessfulMessages(zippedMessagesTickets);
        String okTicketMessagesString = okTicketMessages.stream().map(
                p -> "Title: " + p.message.getTitle() + ", Id:" + p.ticket.getId()
        ).collect(Collectors.joining(","));
        System.out.println(
                "Recieved OK ticket for " +
                        okTicketMessages.size() +
                        " messages: " + okTicketMessagesString
        );

        List<ExpoPushMessageTicketPair<ExpoPushMessage>> errorTicketMessages =
                client.filterAllMessagesWithError(zippedMessagesTickets);
        String errorTicketMessagesString = errorTicketMessages.stream().map(
                p -> "Title: " + p.message.getTitle() + ", Error: " + p.ticket.getDetails().getError()
        ).collect(Collectors.joining(","));
        System.out.println(
                "Recieved ERROR ticket for " +
                        errorTicketMessages.size() +
                        " messages: " +
                        errorTicketMessagesString
        );


        // TODO
        // Countdown 30s
//            int wait = 30;
//            for (int i = wait; i >= 0; i--) {
//                System.out.print("Waiting for " + wait + " seconds. " + i + "s\r");
//                Thread.sleep(1000);
//            }
//        System.out.println("Fetching receipts...");
//
//        List<String> ticketIds = (client.getTicketIdsFromPairs(okTicketMessages));
//        CompletableFuture<List<ExpoPushReceipt>> receiptFutures = client.getPushNotificationReceiptsAsync(ticketIds);
//
//        List<ExpoPushReceipt> receipts = new ArrayList<>();
//        try {
//            receipts = receiptFutures.get();
//        } catch (ExecutionException | InterruptedException e) {
//            e.printStackTrace();
//        }
//
//        System.out.println(
//                "Received " + receipts.size() + " receipts:");
//
//        for (ExpoPushReceipt receipt : receipts) {
//            System.out.println(
//                    "Receipt for id: " +
//                            receipt.getId() +
//                            " had status: " +
//                            receipt.getStatus());
//
//        }

    }

    public void readAll(Long userId) {
        notificationRepository.readAll(userId);
    }
}
