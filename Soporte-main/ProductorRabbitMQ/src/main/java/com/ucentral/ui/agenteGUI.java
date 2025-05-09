package com.ucentral.ui;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import javax.swing.*;
import java.awt.*;
import java.util.HashSet;
import java.util.Set;

import org.json.JSONObject;

/**
 * AgenteGUI.java
 *
 * Interfaz gráfica para agentes de soporte que consumen tickets desde RabbitMQ.
 */
public class agenteGUI extends JFrame {
    private static final String HOST = "localhost";
    private static final String VHOST = "/";
    private static final String USER = "guest";
    private static final String PASS = "guest";
    private static final String DIRECT_EX = "test.direct";
    private static final String TOPIC_EX = "test.topic";

    private JCheckBox cbHardware;
    private JCheckBox cbSoftware;
    private JCheckBox cbNetwork;
    private JCheckBox cbBaja;
    private JCheckBox cbMedia;
    private JCheckBox cbAlta;
    private JButton btnListen;
    private JTextArea textArea;

    private ConnectionFactory factory;

    public agenteGUI() {
        super("Agente - Tickets de Soporte");
        initRabbit();
        initUI();
    }

    private void initRabbit() {
        factory = new ConnectionFactory();
        factory.setHost(HOST);
        factory.setVirtualHost(VHOST);
        factory.setUsername(USER);
        factory.setPassword(PASS);
    }

    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 400);
        setLocationRelativeTo(null);

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

        topPanel.add(new JLabel("Filtrar por Área:"));
        cbHardware = new JCheckBox("Hardware");
        cbSoftware = new JCheckBox("Software");
        cbNetwork = new JCheckBox("Network");
        topPanel.add(cbHardware);
        topPanel.add(cbSoftware);
        topPanel.add(cbNetwork);

        topPanel.add(new JLabel("Filtrar por Severidad:"));
        cbBaja = new JCheckBox("Baja");
        cbMedia = new JCheckBox("Media");
        cbAlta = new JCheckBox("Alta");
        topPanel.add(cbBaja);
        topPanel.add(cbMedia);
        topPanel.add(cbAlta);

        btnListen = new JButton("Iniciar Escucha");
        topPanel.add(btnListen);

        textArea = new JTextArea();
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        btnListen.addActionListener(e -> startListening());
    }

    private void startListening() {
        Set<String> areasSeleccionadas = new HashSet<>();
        if (cbHardware.isSelected())
            areasSeleccionadas.add("hardware");
        if (cbSoftware.isSelected())
            areasSeleccionadas.add("software");
        if (cbNetwork.isSelected())
            areasSeleccionadas.add("network");

        Set<String> severidadesSeleccionadas = new HashSet<>();
        if (cbBaja.isSelected())
            severidadesSeleccionadas.add("baja");
        if (cbMedia.isSelected())
            severidadesSeleccionadas.add("media");
        if (cbAlta.isSelected())
            severidadesSeleccionadas.add("alta");

        if (areasSeleccionadas.isEmpty() && severidadesSeleccionadas.isEmpty()) {
            textArea.append("[INFO] Por favor, selecciona al menos un filtro de área o severidad.\n");
            // No retornamos aquí, permitimos la suscripción global de todos modos
        }

        textArea.append("[INFO] Iniciando escucha con los filtros seleccionados (y global).\n");

        new Thread(() -> {
            try (Connection conn = factory.newConnection();
                    Channel ch = conn.createChannel()) {

                // Siempre suscribirse a la cola global
                String globalQueueName = "soporte.global";
                try {
                    startConsumer(ch, globalQueueName);
                    SwingUtilities.invokeLater(() -> textArea
                            .append("[INFO] Escuchando todos los tickets (cola global: " + globalQueueName + ")\n"));
                } catch (java.io.IOException e) {
                    SwingUtilities
                            .invokeLater(() -> textArea.append("[ADVERTENCIA] No se pudo suscribir a la cola global: "
                                    + globalQueueName + " - " + e.getMessage() + "\n"));
                }

                // Suscribirse a colas por severidad (DIRECT_EX)
                if (!severidadesSeleccionadas.isEmpty()) {
                    for (String severidad : severidadesSeleccionadas) {
                        String queueName = "soporte." + severidad.toLowerCase();
                        try {
                            startConsumer(ch, queueName);
                            SwingUtilities.invokeLater(() -> textArea.append(
                                    "[INFO] Escuchando severidad: " + severidad + " (cola: " + queueName + ")\n"));
                        } catch (java.io.IOException e) {
                            SwingUtilities.invokeLater(
                                    () -> textArea.append("[ADVERTENCIA] No se pudo suscribir a la cola de severidad: "
                                            + queueName + " - " + e.getMessage() + "\n"));
                        }
                    }
                }

                // Suscribirse a colas por área (TOPIC_EX)
                if (!areasSeleccionadas.isEmpty()) {
                    for (String area : areasSeleccionadas) {
                        String queueName = "soporte." + area.toLowerCase();
                        try {
                            startConsumer(ch, queueName);
                            SwingUtilities.invokeLater(() -> textArea
                                    .append("[INFO] Escuchando área: " + area + " (cola: " + queueName + ")\n"));
                        } catch (java.io.IOException e) {
                            SwingUtilities.invokeLater(
                                    () -> textArea.append("[ADVERTENCIA] No se pudo suscribir a la cola de área: "
                                            + queueName + " - " + e.getMessage() + "\n"));
                        }
                    }
                }

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> textArea.append("[ERROR] " + ex.getMessage() + "\n"));
            }
        }).start();
    }

    private void startConsumer(Channel channel, String queueName) throws java.io.IOException {
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String msg = new String(delivery.getBody(), "UTF-8");
            String routingKey = delivery.getEnvelope().getRoutingKey();

            SwingUtilities.invokeLater(() -> {
                try {
                    JSONObject json = new JSONObject(msg);
                    String formatted = String.format(
                            "Área: %s\nTicket ID: %s\nUsuario: %s\nDescripción: %s\nSeveridad: %s\nFecha: \n\n",
                            json.optString("area"),
                            json.optString("ticketId"),
                            json.optString("userId"),
                            json.optString("description"),
                            json.optString("severity"),
                            json.optString("timestamp"),
                            queueName,
                            routingKey);
                    textArea.append(formatted);
                } catch (Exception e) {
                    textArea.append(
                            "[Cola: " + queueName + ", Routing Key: " + routingKey + "] Mensaje: " + msg + "\n\n");
                }
            });
        };
        channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new agenteGUI().setVisible(true));
    }
}