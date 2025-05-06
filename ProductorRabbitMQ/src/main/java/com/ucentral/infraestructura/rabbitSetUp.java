package com.ucentral.infraestructura;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

/**
 * RabbitSetup.java
 *
 * Configura automáticamente los exchanges, colas y bindings de RabbitMQ necesarios
 * para el sistema de soporte técnico.
 */
public class rabbitSetUp {
    private static final String DIRECT_EX = "test.direct";
    private static final String TOPIC_EX  = "test.topic";

    public static void main(String[] args) throws Exception {
        // Configuración de la conexión
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");      // Host de RabbitMQ
        factory.setVirtualHost("/");       // Virtual host
        factory.setUsername("guest");
        factory.setPassword("guest");

        try (Connection conn = factory.newConnection();
             Channel ch   = conn.createChannel()) {

            // 1) Declarar exchanges
            ch.exchangeDeclare(DIRECT_EX, BuiltinExchangeType.DIRECT, true);
            ch.exchangeDeclare(TOPIC_EX,  BuiltinExchangeType.TOPIC,  true);

            // 2) Declarar colas para severidad (direct)
            String[] severidades = {"baja", "media", "alta"};
            for (String sev : severidades) {
                String queueName = "soporte." + sev;
                ch.queueDeclare(queueName, true, false, false, null);
                ch.queueBind(queueName, DIRECT_EX, sev);
            }

            // 3) Declarar colas para área (topic)
            String[] areas = {"hardware", "software", "network"};
            for (String area : areas) {
                String queueName = "soporte." + area;
                ch.queueDeclare(queueName, true, false, false, null);
                ch.queueBind(queueName, TOPIC_EX, area + ".*");
            }

            System.out.println("[RabbitSetup] Exchanges, colas y bindings creados exitosamente.");
        }
    }
}
