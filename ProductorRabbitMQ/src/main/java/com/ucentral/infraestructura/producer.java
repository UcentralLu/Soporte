package com.ucentral.infraestructura;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;

/**
 * Producer.java
 *
 * Envía tickets de soporte a RabbitMQ utilizando:
 * - test.direct (direct) para severidad: "baja", "media", "alta"
 * - test.topic  (topic)  para área: "hardware.*", "software.*", "network.*"
 */
public class producer {
    private static final String HOST      = "localhost";
    private static final String VHOST     = "/";
    private static final String USER      = "guest";
    private static final String PASS      = "guest";
    private static final String DIRECT_EX = "test.direct";
    private static final String TOPIC_EX  = "test.topic";

    private ConnectionFactory factory;

    public producer() {
        factory = new ConnectionFactory();
        factory.setHost(HOST);
        factory.setVirtualHost(VHOST);
        factory.setUsername(USER);
        factory.setPassword(PASS);
    }

    /**
     * Envía un ticket al exchange directo "test.direct".
     * @param ticketJson  Mensaje JSON del ticket
     * @param severity    Severidad: "baja", "media" o "alta"
     */
    public void sendSeverityTicket(String ticketJson, String severity) throws Exception {
        try (Connection conn = factory.newConnection();
             Channel ch   = conn.createChannel()) {
            ch.basicPublish(DIRECT_EX, severity, MessageProperties.PERSISTENT_TEXT_PLAIN, ticketJson.getBytes("UTF-8"));
            System.out.println("[Producer] Enviado a severidad '" + severity + "': " + ticketJson);
        }
    }

    /**
     * Envía un ticket al exchange topic "test.topic".
     * Si la clave es "hardware", "software" o "network", se ajusta a "hardware.issue", etc.
     * @param ticketJson  Mensaje JSON del ticket
     * @param areaKey     Clave base: e.g. "hardware", "software", "network"
     */
    public void sendAreaTicket(String ticketJson, String areaKey) throws Exception {
        // Asegurar que la clave cumpla el patrón esperado por el consumidor
        String routingKey;
        if (areaKey.matches("hardware|software|network")) {
            routingKey = areaKey + ".issue"; // default .issue
        } else {
            routingKey = areaKey; // ya es una clave tipo "network.alert", por ejemplo
        }

        try (Connection conn = factory.newConnection();
             Channel ch   = conn.createChannel()) {
            ch.basicPublish(TOPIC_EX, routingKey, MessageProperties.PERSISTENT_TEXT_PLAIN, ticketJson.getBytes("UTF-8"));
            System.out.println("[Producer] Enviado a área '" + routingKey + "': " + ticketJson);
        }
    }
}
