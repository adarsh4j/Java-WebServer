# Feedback Server in Java

This project is a simple Java-based HTTP server that serves a feedback form and stores submitted data in a MySQL database.

## Features

- Serves an HTML form and CSS styling.
- Accepts user feedback via POST request.
- Parses and stores form data in a MySQL database.
- Returns a thank you message after submission.

## Requirements

- Java 8 or above
- MySQL
- MySQL JDBC Driver (`mysql-connector-java`)
- HTML form (`form.html`)
- CSS file (`form.css`)

## Setup

1. **Create MySQL Database and Table:**

```sql
CREATE DATABASE feedbackdb;
USE feedbackdb;

CREATE TABLE feedback (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100),
    message TEXT
);
