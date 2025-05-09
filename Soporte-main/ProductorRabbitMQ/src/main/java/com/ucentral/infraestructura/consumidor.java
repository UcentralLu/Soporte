package com.ucentral.infraestructura;

import com.rabbitmq.client.*;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public class consumidor implements Runnable {
    private static final String HOST = "localhost";
    private static final String VHOST = "/";
    private static final String USER = "guest";
    private static final String PASS = "guest";
    private static final String TOPIC_EX = "test.topic"; // Usamos el Topic Exchange

    private String area;       // "hardware", "software", "network", etc.
    private String severity;   // "baja", "media", "alta"
    private JTextArea textArea; // Para mostrar los mensajes en la interfaz gráfica
    private boolean activo = true;

    public consumidor(String area, String severity, JTextArea textArea) {
        this.area = area;
        this.severity = severity;
        this.textArea = textArea;
    }

    public void detener() {
        this.activo = false;
    }

    @Override
    public void run() {
        try {
            // Configuración de la conexión con RabbitMQ
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(HOST);
            factory.setVirtualHost(VHOST);
            factory.setUsername(USER);
            factory.setPassword(PASS);

            Connection conn = factory.newConnection();
            Channel ch = conn.createChannel();

            // Declarar la cola y el binding
            String queueName = "soporte." + area; // El nombre de la cola basado en el área
            ch.queueDeclare(queueName, true, false, false, null);
            
            // En el binding necesitamos vincular las severidades directamente al área.
            // La severidad debe coincidir exactamente con lo que se espera.
            ch.queueBind(queueName, TOPIC_EX, area + "." + severity); // Usar topic exchange para el área y severidad

            System.out.println("[Consumidor] Escuchando en cola: " + queueName);

            // Callback para manejar los mensajes entrantes
            DeliverCallback callback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), "UTF-8");
                String routingKey = delivery.getEnvelope().getRoutingKey();

                // Filtrar los mensajes por la severidad seleccionada
                if (routingKey.equals(area + "." + severity)) {
                    // Si el mensaje tiene la severidad correcta, mostrarlo en la interfaz gráfica
                    SwingUtilities.invokeLater(() -> {
                        textArea.append("[" + area + ", " + routingKey + "] " + message + "\n");
                    });
                }
            };

            // Consumir los mensajes de la cola
            ch.basicConsume(queueName, true, callback, consumerTag -> {});

            // Mantener el hilo activo mientras el consumidor esté activo
            while (activo) {
                Thread.sleep(500); // No hacer nada, solo esperar que el callback maneje los mensajes
            }

            // Cerrar la conexión y el canal cuando se detenga el consumidor
            ch.close();
            conn.close();
            System.out.println("[Consumidor] Cerrada conexión para: " + queueName);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
