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

        String subject = "Tu cuenta ha sido eliminada";
        Content content = new Content("text/plain",
            "Hola,\n\n" +
            "Te informamos de que tu cuenta en Streetask ha sido eliminada por el equipo de moderación " +
            "debido al incumplimiento de nuestras normas de uso.\n\n" +
            "Si crees que esto es un error, puedes ponerte en contacto con nosotros respondiendo a este correo.\n\n" +
            "Atentamente,\n" +
            "El Equipo de Streetask"
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
