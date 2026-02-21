package com.example.demo.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendSimpleEmail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("iris.nadiia125@gmail.com");
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    public void sendVerificationEmail(String toEmail, String username, String code) {
        sendSimpleEmail(
                toEmail,
                "Votre code de verification ELMOQEF",
                "Bonjour " + username + ",\n\n" +
                        "Merci pour votre inscription en tant que prestataire.\n" +
                        "Voici votre code de verification a transmettre ou saisir pour finaliser votre inscription :\n\n" +
                        "CODE : " + code + "\n\n" +
                        "L'equipe ELMOQEF"
        );
    }

    //  Approbation provider
    public void sendApprovalEmail(String toEmail, String username) {
        sendSimpleEmail(
                toEmail,
                "Votre compte ELMOQEF a ete approuve !",
                "Bonjour " + username + ",\n\n" +
                        "Votre demande en tant que prestataire a ete approuvee.\n" +
                        "Vous pouvez maintenant vous connecter sur ELMOQEF.\n\n" +
                        "L'equipe ELMOQEF"
        );
    }

    //  Rejet provider 
    public void sendRejectionEmail(String toEmail, String username) {
        sendSimpleEmail(
                toEmail,
                "Votre demande ELMOQEF n'a pas ete acceptee",
                "Bonjour " + username + ",\n\n" +
                        "Votre demande d'inscription en tant que prestataire n'a pas ete acceptee.\n" +
                        "Contactez notre support pour plus d'informations.\n\n" +
                        "L'equipe ELMOQEF"
        );
    }
}