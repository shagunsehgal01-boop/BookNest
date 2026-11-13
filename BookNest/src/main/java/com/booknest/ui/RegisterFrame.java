

package com.booknest.ui;

import com.booknest.service.AuthService;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;

public class RegisterFrame extends JFrame {

    private JTextField nameField;
    private JTextField emailField;
    private JPasswordField passwordField;

    private AuthService authService;

    public RegisterFrame() {

        authService = new AuthService();

        setTitle("BookNest - Student Registration");
        setSize(450, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(6, 2, 10, 15));
        panel.setBorder(
                BorderFactory.createEmptyBorder(30, 30, 30, 30)
        );

        JLabel heading = new JLabel("Welcome to BookNest!");
        heading.setFont(new Font("Arial", Font.BOLD, 20));

        nameField = new JTextField();
        emailField = new JTextField();
        passwordField = new JPasswordField();

        JButton registerButton = new JButton("Register");
        JButton loginButton = new JButton("Already registered? Login");

        panel.add(heading);
        panel.add(new JLabel("Create your account"));

        panel.add(new JLabel("Full Name:"));
        panel.add(nameField);

        panel.add(new JLabel("Email:"));
        panel.add(emailField);

        panel.add(new JLabel("Password:"));
        panel.add(passwordField);

        panel.add(new JLabel(""));
        panel.add(registerButton);

        panel.add(new JLabel(""));
        panel.add(loginButton);

        add(panel);

        registerButton.addActionListener(e -> registerStudent());

        loginButton.addActionListener(e -> {
            new LoginFrame().setVisible(true);
            dispose();
        });
    }

    private void registerStudent() {

        String name = nameField.getText();
        String email = emailField.getText();
        String password = new String(passwordField.getPassword());

        try {

            boolean registered = authService.register(
                    name, email, password
            );

            if (registered) {
                JOptionPane.showMessageDialog(
                        this,
                        "Registration successful! Welcome to BookNest."
                );

                nameField.setText("");
                emailField.setText("");
                passwordField.setText("");
            }

        } catch (IllegalArgumentException e) {

            JOptionPane.showMessageDialog(
                    this,
                    e.getMessage(),
                    "Invalid Input",
                    JOptionPane.WARNING_MESSAGE
            );

        } catch (SQLException e) {

            if ("23000".equals(e.getSQLState())) {
                JOptionPane.showMessageDialog(
                        this,
                        "This email is already registered."
                );
            } else {
                JOptionPane.showMessageDialog(
                        this,
                        "Database error: " + e.getMessage()
                );
            }
        }
    }
}
