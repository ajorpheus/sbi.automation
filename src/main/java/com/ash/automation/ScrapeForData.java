package com.ash.automation;


import java.io.IOException;
import java.text.ParseException;
import java.util.logging.Level;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.ash.notifications.email.EmailService;
import com.ash.notifications.prowl.ProwlNotification;
import com.ash.notifications.slack.SlackNotification;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.ProxyConfig;
import com.gargoylesoftware.htmlunit.WebClient;
@Component
public class ScrapeForData {
    public static final Logger logger = LoggerFactory.getLogger(ScrapeForData.class);

    @Autowired
    EmailService emailService;
    @Autowired
    ProwlNotification prowlNotification;
    @Autowired
    SlackNotification slackNotification;
	@Autowired
    Scraper scraper;

    @Value("${proxy.enabled}")
    private boolean proxyEnabled;
    @Value("${http-proxy.hostname}")
    private String proxyHostName;
    @Value("${http-proxy.port}")
    private int proxyPort;


    @Value("${email.notifications.enabled}")
    private boolean emailNotificationsEnabled;

    @Value("${prowl.notifications.enabled}")
    private boolean prowlNotificationsEnabled;

    @Value("${slack.notifications.enabled}")
    private boolean slackNotificationsEnabled;

    private WebClient webClient;

    private void setupHtmlUnit() {
        java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF);

        webClient = new WebClient(BrowserVersion.FIREFOX_3_6);
        webClient.setCssEnabled(false);
        webClient.setTimeout(35000);
        webClient.setThrowExceptionOnScriptError(false);
        webClient.setJavaScriptEnabled(false);

        if (proxyEnabled) {
            logger.info("Proxy is enabled. Using ProxyHost:{} , ProxyPort: {}", proxyHostName, proxyPort);
            ProxyConfig proxyConfig = new ProxyConfig(proxyHostName, proxyPort);
            webClient.setProxyConfig(proxyConfig);
        }
    }

    public void performMagic() throws Exception {
        setupHtmlUnit();
        String data = scraper.scrapeForData(webClient);
        sendNotifications(data);
        tearDownHtmlUnit();
    }

    private void sendNotifications(String exchangeRate) throws ParseException {
        logger.debug(
                "\n\n********************************************************\n" +
                        "********************************************************\n" +
                        "GBP/INR Exchange Rate: {}" +
                        "********************************************************\n" +
                        "********************************************************"
                , new Object[]{exchangeRate});
        logger.info("\u0007");

        if(prowlNotificationsEnabled)
        {
            logger.info("Prowl notifications are enabled.");
            prowlNotification.send("Exchange Rate ", "GBP/INR: " + exchangeRate, "");
        }

        if(emailNotificationsEnabled)
        {
            logger.info("Email notifications are enabled.");
//            String emailBody = Helper.escape(exchangeRateDialogueText);
            emailService.sendEmail("", "Exchange Rate GBP/INR: " + exchangeRate);
        }


        if(slackNotificationsEnabled)
        {
            logger.info("Slack notifications are enabled.");
            try {
                slackNotification.send(exchangeRate);
                //slackNotification.send(exchangeRateDialogueText);
            } catch (IOException e) {
                logger.error("Couldn't send slack notification");
                // ignore
            }
        }
    }

    private void tearDownHtmlUnit() {
        webClient.closeAllWindows();
    }


}
