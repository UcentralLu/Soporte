package com.ucentral.ui;

import com.ucentral.infraestructura.*;
import com.ucentral.dominio.*;
import javax.swing.*;
import java.awt.*;
import java.util.UUID;

/**
 * ClienteGUI.java
 *
 * Interfaz gráfica para el cliente que envía tickets de soporte a RabbitMQ.
 */
public class clienteGUI extends JFrame {
    private JTextField userIdField;
    private JTextArea descriptionArea;
    private JComboBox<String> severityBox;
    private JComboBox<String> areaBox;
    private producer producer;

    public clienteGUI() {
        super("Cliente - Enviar Ticket de Soporte");
        producer = new producer();
        initUI();
    }

    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 500);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0; gbc.gridy = 0;

        panel.add(new JLabel("ID Usuario:"), gbc);
        gbc.gridx = 1;
        userIdField = new JTextField(20);
        panel.add(userIdField, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        panel.add(new JLabel("Descripción:"), gbc);
        gbc.gridx = 1;
        descriptionArea = new JTextArea(5, 20);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        panel.add(new JScrollPane(descriptionArea), gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        panel.add(new JLabel("Severidad:"), gbc);
        gbc.gridx = 1;
        severityBox = new JComboBox<>(new String[]{"baja", "media", "alta"});
        panel.add(severityBox, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        panel.add(new JLabel("Área:"), gbc);
        gbc.gridx = 1;
        areaBox = new JComboBox<>(new String[]{"hardware", "software", "network"});
        panel.add(areaBox, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JButton sendBtn = new JButton("Enviar Ticket");
        panel.add(sendBtn, gbc);

        sendBtn.addActionListener(e -> sendTicket());

        add(panel);
    }

    private void sendTicket() {
        String userId = userIdField.getText().trim();
        String description = descriptionArea.getText().trim();
        String severity = (String) severityBox.getSelectedItem();
        String area = (String) areaBox.getSelectedItem();

        if (userId.isEmpty() || description.length() < 10) {
            JOptionPane.showMessageDialog(this,
                    "El ID de usuario no puede estar vacío y la descripción debe tener al menos 10 caracteres.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            String ticketId = UUID.randomUUID().toString();
            mensajeTicket ticket = new mensajeTicket(ticketId, userId, description, severity, area);
            String ticketJson = ticket.toJson();

            producer.sendSeverityTicket(ticketJson, severity);
            producer.sendAreaTicket(ticketJson, area);


            JOptionPane.showMessageDialog(this,
                    "Ticket enviado correctamente.\nID: " + ticketId,
                    "Éxito", JOptionPane.INFORMATION_MESSAGE);

            userIdField.setText("");
            descriptionArea.setText("");
            severityBox.setSelectedIndex(0);
            areaBox.setSelectedIndex(0);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error al enviar el ticket:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new clienteGUI().setVisible(true);
        });
    }
}
