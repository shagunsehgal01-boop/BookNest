

package com.booknest.dao;

import com.booknest.database.DBConnection;
import com.booknest.model.Student;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class StudentDAO {

    public boolean registerStudent(Student student) throws SQLException {

        String query = "INSERT INTO students (name, email, password) VALUES (?, ?, ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {

            ps.setString(1, student.getName());
            ps.setString(2, student.getEmail());
            ps.setString(3, student.getPassword());

            return ps.executeUpdate() > 0;
        }
    }

    public String getPasswordByEmail(String email) throws SQLException {

        String query = "SELECT password FROM students WHERE email = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {

            ps.setString(1, email);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("password");
                }
            }
        }

        return null;
    }

    public String getNameByEmail(String email) throws SQLException {

        String query = "SELECT name FROM students WHERE email = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {

            ps.setString(1, email);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("name");
                }
            }
        }

        return null;
    }
}

