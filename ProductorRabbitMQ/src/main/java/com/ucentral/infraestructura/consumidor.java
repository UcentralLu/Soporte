package com.ucentral.infraestructura;

import com.rabbitmq.client.*;

public class consumidor implements Runnable {
    private static final String HOST = "localhost";
    private static final String VHOST = "/";
    private static final String USER = "guest";
    private static final String PASS = "guest";
    private static final String DIRECT_EX = "test.direct";
    private static final String TOPIC_EX = "test.topic";

    private String tipo;  // "severity" o "area"
    private String clave; // "baja", "media", "alta" o "software", "hardware", etc.
    private boolean activo = true;

    public consumidor(String tipo, String clave) {
        this.tipo = tipo;
        this.clave = clave;
    }

    public void detener() {
        this.activo = false;
    }

    @Override
    public void run() {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(HOST);
            factory.setVirtualHost(VHOST);
            factory.setUsername(USER);
            factory.setPassword(PASS);

            Connection conn = factory.newConnection();
            Channel ch = conn.createChannel();

            String queueName = "soporte." + clave;
            String exchange = tipo.equalsIgnoreCase("severity") ? DIRECT_EX : TOPIC_EX;
            String bindingKey = tipo.equalsIgnoreCase("severity") ? clave : clave + ".*";

            ch.queueDeclare(queueName, true, false, false, null);
            ch.queueBind(queueName, exchange, bindingKey);

            System.out.println("[AgenteConsumer] Escuchando en cola: " + queueName);

            DeliverCallback callback = (consumerTag, delivery) -> {
                if (!activo) return;
                String message = new String(delivery.getBody(), "UTF-8");
                String routingKey = delivery.getEnvelope().getRoutingKey();
                System.out.println("[AgenteConsumer] Mensaje recibido (" + routingKey + "): " + message);
            };

            ch.basicConsume(queueName, true, callback, consumerTag -> {});

            while (activo) {
                Thread.sleep(500); // Mantener hilo activo mientras escucha
            }

            ch.close();
            conn.close();
            System.out.println("[AgenteConsumer] Cerrada conexión para: " + queueName);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
