
package com.booknest.ui;

import com.booknest.service.AuthService;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;

public class LoginFrame extends JFrame {

    private JTextField emailField;
    private JPasswordField passwordField;
    private AuthService authService;

    public LoginFrame() {

        authService = new AuthService();

        setTitle("BookNest - Login");
        setSize(420, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(4, 2, 10, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        JLabel heading = new JLabel("Welcome Back to BookNest!");
        heading.setFont(new Font("Arial", Font.BOLD, 18));

        emailField = new JTextField();
        passwordField = new JPasswordField();

        JButton loginButton = new JButton("Login");
        JButton registerButton = new JButton("Create Account");

        panel.add(heading);
        panel.add(new JLabel(""));
        panel.add(new JLabel("Email:"));
        panel.add(emailField);
        panel.add(new JLabel("Password:"));
        panel.add(passwordField);
        panel.add(registerButton);
        panel.add(loginButton);

        add(panel);

        loginButton.addActionListener(e -> loginStudent());

        registerButton.addActionListener(e -> {
            new RegisterFrame().setVisible(true);
            dispose();
        });
    }

    private void loginStudent() {

        String email = emailField.getText();
        String password = new String(passwordField.getPassword());

        try {

            String studentName = authService.login(email, password);

            if (studentName != null) {
                JOptionPane.showMessageDialog(this, "Login successful!");

                new DashboardFrame(studentName, email).setVisible(true);
                dispose();

            } else {
                JOptionPane.showMessageDialog(
                        this,
                        "Incorrect email or password."
                );
            }

        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage());

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Database error: " + e.getMessage()
            );
        }
    }
}
