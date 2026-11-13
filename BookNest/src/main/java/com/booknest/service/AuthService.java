
package com.booknest.service;

import com.booknest.dao.StudentDAO;
import com.booknest.model.Student;

import org.mindrot.jbcrypt.BCrypt;

import java.sql.SQLException;

public class AuthService {

    private StudentDAO studentDAO = new StudentDAO();

    public boolean register(String name, String email, String password)
            throws SQLException {

        name = name.trim();
        email = email.trim().toLowerCase();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            throw new IllegalArgumentException("All fields are required.");
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            throw new IllegalArgumentException("Enter a valid email address.");
        }

        if (password.length() < 8) {
            throw new IllegalArgumentException(
                    "Password must contain at least 8 characters.");
        }

        String hashedPassword = BCrypt.hashpw(
                password, BCrypt.gensalt());

        Student student = new Student(name, email, hashedPassword);

        return studentDAO.registerStudent(student);
    }

public String login(String email, String password) throws SQLException {

    email = email.trim().toLowerCase();

    if (email.isEmpty() || password.isEmpty()) {
        throw new IllegalArgumentException("Enter email and password.");
    }

    String storedHash = studentDAO.getPasswordByEmail(email);

    if (storedHash == null ||
            !BCrypt.checkpw(password, storedHash)) {
        return null;
    }

    return studentDAO.getNameByEmail(email);
}

}

