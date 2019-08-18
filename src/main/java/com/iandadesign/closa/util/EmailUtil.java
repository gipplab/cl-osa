package com.iandadesign.closa.util;

import com.iandadesign.closa.util.wikidata.WikidataDumpUtil;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class EmailUtil {

    private static String username;
    private static String password;
    private static String recipient;

    static {
        InputStream inputStream = null;

        try {
            Properties properties = new Properties();;
            String propFileLocalName = "config-local.properties";

            // switch to config-local if it exists
            if (EmailUtil.class.getClassLoader().getResource(propFileLocalName) != null) {
                inputStream = EmailUtil.class.getClassLoader().getResourceAsStream(propFileLocalName);

                if (inputStream != null) {
                    properties.load(inputStream);
                } else {
                    throw new FileNotFoundException("Property file '" + propFileLocalName + "' not found in the classpath");
                }
            }
            username = properties.getProperty("email_username");
            password = properties.getProperty("email_password");
            recipient = properties.getProperty("email_recipient");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void sendMail(String subject, String body) {
        String from = username;
        String pass = password;
        String[] to = {recipient}; // list of recipient email addresses

        sendFromGMail(from, pass, to, subject, body);
    }

    private static void sendFromGMail(String from, String pass, String[] to, String subject, String body) {
        Properties props = System.getProperties();
        String host = "smtp.gmail.com";
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.user", from);
        props.put("mail.smtp.password", pass);
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");

        Session session = Session.getDefaultInstance(props);
        MimeMessage message = new MimeMessage(session);

        try {
            message.setFrom(new InternetAddress(from));
            InternetAddress[] toAddress = new InternetAddress[to.length];

            // To get the array of addresses
            for (int i = 0; i < to.length; i++) {
                toAddress[i] = new InternetAddress(to[i]);
            }

            for (int i = 0; i < toAddress.length; i++) {
                message.addRecipient(Message.RecipientType.TO, toAddress[i]);
            }

            message.setSubject(subject);
            message.setText(body);
            Transport transport = session.getTransport("smtp");
            transport.connect(host, from, pass);
            transport.sendMessage(message, message.getAllRecipients());
            transport.close();
        } catch (MessagingException ae) {
            ae.printStackTrace();
        }
    }

}
