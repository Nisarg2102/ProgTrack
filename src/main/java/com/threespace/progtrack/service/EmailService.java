package com.threespace.progtrack.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendApprovalEmail(String toEmail, String fullName) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("ProgTrack: Account Approved!");
        message.setText("Hello " + fullName + ",\n\n"
                + "Great news! Your ProgTrack account has been approved by the Administrator.\n"
                + "You can now log in using the credentials you created during registration.\n\n"
                + "Login here: http://localhost:8080/login\n\n"
                + "Welcome to the team!\n- 3Space Technologies");
        
        mailSender.send(message);
    }

    public void sendRejectionEmail(String toEmail, String fullName) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("ProgTrack: Account Registration Update");
        message.setText("Hello " + fullName + ",\n\n"
                + "We are writing to inform you that your request for a ProgTrack account has been declined at this time.\n"
                + "If you believe this is an error, please contact your systems administrator.\n\n"
                + "- 3Space Technologies");
        
        mailSender.send(message);
    }
}