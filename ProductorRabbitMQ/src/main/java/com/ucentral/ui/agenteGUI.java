package com.ucentral.ui;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import javax.swing.*;
import java.awt.*;

/**
 * AgenteGUI.java
 *
 * Interfaz gráfica para agentes de soporte que consumen tickets desde RabbitMQ.
 */
public class agenteGUI extends JFrame {
    private static final String HOST      = "localhost";
    private static final String VHOST     = "/";
    private static final String USER      = "guest";
    private static final String PASS      = "guest";
    private static final String DIRECT_EX = "test.direct";
    private static final String TOPIC_EX  = "test.topic";

    private JRadioButton rbSeverity;
    private JRadioButton rbArea;
    private JComboBox<String> comboKeys;
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
        rbSeverity = new JRadioButton("Por Severidad", true);
        rbArea     = new JRadioButton("Por Área");
        ButtonGroup group = new ButtonGroup();
        group.add(rbSeverity);
        group.add(rbArea);

        topPanel.add(rbSeverity);
        topPanel.add(rbArea);

        comboKeys = new JComboBox<>(new String[]{"baja", "media", "alta"});
        topPanel.add(new JLabel("Clave:"));
        topPanel.add(comboKeys);

        btnListen = new JButton("Iniciar Escucha");
        topPanel.add(btnListen);

        textArea = new JTextArea();
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        rbSeverity.addActionListener(e -> updateCombo());
        rbArea.addActionListener(e -> updateCombo());
        btnListen.addActionListener(e -> startListening());
    }

    private void updateCombo() {
        comboKeys.removeAllItems();
        if (rbSeverity.isSelected()) {
            comboKeys.addItem("baja");
            comboKeys.addItem("media");
            comboKeys.addItem("alta");
        } else {
            comboKeys.addItem("hardware");
            comboKeys.addItem("software");
            comboKeys.addItem("network");
        }
    }

    private void startListening() {
        String type = rbSeverity.isSelected() ? "severity" : "area";
        String key  = (String) comboKeys.getSelectedItem();

        // Permitir múltiples escuchas sin deshabilitar el botón\ n            // btnListen.setEnabled(false);
        textArea.append("[INFO] Iniciando escucha en " + type + ": " + key + "\n");

        new Thread(() -> {
            try (Connection conn = factory.newConnection();
                 Channel ch   = conn.createChannel()) {

                String queueName = "soporte." + key;
                if (type.equals("severity")) {
                    ch.queueDeclare(queueName, true, false, false, null);
                    ch.queueBind(queueName, DIRECT_EX, key);
                } else {
                    ch.queueDeclare(queueName, true, false, false, null);
                    ch.queueBind(queueName, TOPIC_EX, key + ".*");
                }

                DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                    String msg        = new String(delivery.getBody(), "UTF-8");
                    String routingKey = delivery.getEnvelope().getRoutingKey();
                    SwingUtilities.invokeLater(() -> textArea.append(
                        "[" + type + ", " + routingKey + "] " + msg + "\n"));
                };
                ch.basicConsume(queueName, true, deliverCallback, consumerTag -> {});

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> textArea.append(
                    "[ERROR] " + ex.getMessage() + "\n"));
            }
        }).start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new agenteGUI().setVisible(true));
    }
}
