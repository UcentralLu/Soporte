package com.ucentral.dominio;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * MensajeTicket.java
 *
 * Objeto POJO que representa un ticket de soporte técnico.
 * Incluye método para serializar a JSON.
 */
public class mensajeTicket {
    private String ticketId;
    private String userId;
    private String description;
    private String severity;
    private String area;
    private String timestamp;

    public mensajeTicket(String ticketId, String userId, String description,
                         String severity, String area) {
        this.ticketId = ticketId;
        this.userId = userId;
        this.description = description;
        this.severity = severity;
        this.area = area;
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    public String getTicketId() {
        return ticketId;
    }

    public String getUserId() {
        return userId;
    }

    public String getDescription() {
        return description;
    }

    public String getSeverity() {
        return severity;
    }

    public String getArea() {
        return area;
    }

    public String getTimestamp() {
        return timestamp;
    }

    /**
     * Serializa el ticket a una cadena JSON simple.
     */
    public String toJson() {
        String descEscaped = description.replace("\"", "\\\"");
        return String.format(
            "{\"ticketId\":\"%s\",\"userId\":\"%s\",\"description\":\"%s\","
          + "\"severity\":\"%s\",\"area\":\"%s\",\"timestamp\":\"%s\"}",
            ticketId, userId, descEscaped, severity, area, timestamp
        );
    }
}