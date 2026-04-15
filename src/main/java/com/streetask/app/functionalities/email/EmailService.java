package com.streetask.app.functionalities.email;

import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Value("${sendgrid.api-key}")
    private String sendgridApiKey;

    public void sendAccountDeletionEmail(String toEmail) {
        Email from = new Email("streetask0@gmail.com", "Streetask");
        Email to   = new Email(toEmail);

        String subject = "Your account has been removed";
        Content content = new Content("text/plain",
            "Hello,\n\n" +
            "We are informing you that your Streetask account has been removed by the moderation team " +
            "due to a violation of our usage guidelines.\n\n" +
            "If you believe this is a mistake, you can contact us by replying to this email.\n\n" +
            "Best regards,\n" +
            "The Streetask Team"
        );

        Mail mail = new Mail(from, subject, to, content);

        SendGrid sg = new SendGrid(sendgridApiKey);
        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);
            logger.info("[EmailService] Deletion email sent to {}. Status: {}", toEmail, response.getStatusCode());
        } catch (Exception e) {
            // No bloqueamos el flujo principal si el email falla
            logger.error("[EmailService] Failed to send deletion email to {}: {}", toEmail, e.getMessage());
        }
    }
}
