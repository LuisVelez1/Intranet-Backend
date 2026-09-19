package com.backendintranet.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "absence.mail.enabled", havingValue = "true")
public class AbsenceMailConfig {
    @Bean("absenceMailSender")
    public JavaMailSenderImpl absenceMailSender(
            @Value("${absence.mail.host}") String host,
            @Value("${absence.mail.port:587}") int port,
            @Value("${absence.mail.username:}") String username,
            @Value("${absence.mail.password:}") String password,
            @Value("${absence.mail.starttls:true}") boolean tls,
            @Value("${absence.mail.auth:true}") boolean auth) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        sender.setUsername(username);
        sender.setPassword(password);
        sender.setDefaultEncoding("UTF-8");
        var props = sender.getJavaMailProperties();
        props.setProperty("mail.smtp.auth", String.valueOf(auth));
        props.setProperty("mail.smtp.starttls.enable", String.valueOf(tls));
        props.setProperty("mail.smtp.starttls.required", String.valueOf(tls));
        props.setProperty("mail.smtp.connectiontimeout", "5000");
        props.setProperty("mail.smtp.timeout", "5000");
        props.setProperty("mail.smtp.writetimeout", "5000");
        return sender;
    }
}
