package com.pediuber.pediuber.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class LoggingService {

    private static final Logger logger =
            LoggerFactory.getLogger(LoggingService.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void info(LogEvent event) {
        log("INFO", event);
    }

    public void warn(LogEvent event) {
        log("WARN", event);
    }

    public void error(LogEvent event) {
        log("ERROR", event);
    }

    private void log(String level, LogEvent event) {

        try {

            event.setLevel(level);

            String json = objectMapper.writeValueAsString(event);

            switch (level) {

                case "INFO":
                    logger.info(json);
                    break;

                case "WARN":
                    logger.warn(json);
                    break;

                case "ERROR":
                    logger.error(json);
                    break;
            }

        } catch (Exception e) {

            logger.error("Erro ao gerar log estruturado");
        }
    }

}
