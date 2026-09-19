package com.backendintranet.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.backendintranet.entity.AbsenceMail;
import com.backendintranet.repository.AbsenceMailRepository;
import com.backendintranet.service.impl.AbsenceMailDispatcher;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.*;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDateTime;
import java.util.List;

class AbsenceMailDispatcherTest {
    @Test
    void sendsQueuedMailAndMarksItDelivered() {
        var repository = mock(AbsenceMailRepository.class);
        var sender = mock(JavaMailSender.class);
        var mail = new AbsenceMail();
        mail.setRecipient("employee@example.test");
        mail.setSubject("Decision");
        mail.setBody("Approved");
        when(repository.findTop20BySentAtIsNullAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                        any()))
                .thenReturn(List.of(mail));
        new AbsenceMailDispatcher(repository, sender, "intranet@example.test").dispatch();
        var message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(message.capture());
        assertThat(message.getValue().getTo()).containsExactly("employee@example.test");
        assertThat(message.getValue().getFrom()).isEqualTo("intranet@example.test");
        assertThat(mail.getSentAt()).isNotNull();
        assertThat(mail.getAttempts()).isEqualTo(1);
        verify(repository).save(mail);
    }

    @Test
    void keepsFailedMailForRetryAndDoesNotExposeSmtpDetails() {
        var repository = mock(AbsenceMailRepository.class);
        var sender = mock(JavaMailSender.class);
        var mail = new AbsenceMail();
        mail.setRecipient("approver@example.test");
        when(repository.findTop20BySentAtIsNullAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                        any()))
                .thenReturn(List.of(mail));
        doThrow(new MailSendException("Sensitive server details"))
                .when(sender)
                .send(any(SimpleMailMessage.class));
        new AbsenceMailDispatcher(repository, sender, "intranet@example.test").dispatch();
        assertThat(mail.getSentAt()).isNull();
        assertThat(mail.getNextAttemptAt()).isAfter(LocalDateTime.now());
        assertThat(mail.getLastError()).isEqualTo("MailSendException");
        verify(repository).save(mail);
    }
}
