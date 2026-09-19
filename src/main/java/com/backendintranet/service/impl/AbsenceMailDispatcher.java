package com.backendintranet.service.impl;

import com.backendintranet.repository.AbsenceMailRepository;

import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@ConditionalOnProperty(name = "absence.mail.enabled", havingValue = "true")
public class AbsenceMailDispatcher {
    private final AbsenceMailRepository repository;
    private final JavaMailSender sender;
    private final String from;

    public AbsenceMailDispatcher(
            AbsenceMailRepository repository,
            @Qualifier("absenceMailSender") JavaMailSender sender,
            @Value("${absence.mail.from}") String from) {
        this.repository = repository;
        this.sender = sender;
        this.from = from;
    }

    @Scheduled(
            fixedDelayString = "${absence.mail.poll-ms:5000}",
            initialDelayString = "${absence.mail.poll-ms:5000}")
    @Transactional
    public void dispatch() {
        for (var item :
                repository.findTop20BySentAtIsNullAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                        LocalDateTime.now())) {
            item.setAttempts(item.getAttempts() + 1);
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(from);
                message.setTo(item.getRecipient());
                message.setSubject(item.getSubject());
                message.setText(item.getBody());
                sender.send(message);
                item.setSentAt(LocalDateTime.now());
                item.setLastError(null);
            } catch (MailException e) {
                item.setLastError(e.getClass().getSimpleName());
                item.setNextAttemptAt(
                        LocalDateTime.now().plusMinutes(Math.min(item.getAttempts(), 60)));
            }
            repository.save(item);
        }
    }
}
