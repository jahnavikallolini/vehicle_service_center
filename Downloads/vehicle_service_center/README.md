# Vehicle Service Center Management System

This project is a full-stack vehicle service center management system designed to handle day-to-day operations such as customer management, service tracking, billing, and reporting. The application integrates a Java-based backend, a MySQL database, and a simple HTML frontend.

---

## Overview

The system provides a structured way to manage service workflows, including registering customers and vehicles, assigning mechanics, tracking service orders, and generating bills. It uses a modular backend architecture with REST-like APIs to enable communication between components.

---

## Tech Stack

- Backend: Java (custom HTTP server)
- Database: MySQL
- Frontend: HTML, CSS
- Build Tool: Maven

---

## Prerequisites

- Java 17 or higher  
- Maven 3.8 or higher  
- MySQL 8.x  

---

## Database Configuration

Update the database connection details in:


src/main/java/com/autoservice/DB.java


```java
private static final String URL  = "jdbc:mysql://localhost:3306/YOUR_DB_NAME?...";
private static final String USER = "root";
private static final String PASS = "your_password";
```
---
## Running Stored Procedures and Triggers

Execute the SQL file to set up stored procedures and triggers:
```
mysql -u root -p your_database < procedures_and_triggers.sql
```
---
## Build Instructions

Use Maven to build the project:
```
mvn clean package
```
This generates a JAR file in the target/ directory.

---
## Running the Application

Run the generated JAR file:
```
java -jar target/autoservice-1.0.jar
```
The server will start at:

http://localhost:8080

Ensure that the static/ folder (containing index.html) is present in the same directory from which the JAR is executed.

---
## API Endpoints
Customers
    GET /api/customers – Retrieve all customers
    POST /api/customers – Create a new customer
    GET /api/customers/{id} – Retrieve a customer
    PUT /api/customers/{id} – Update a customer
Vehicles
    GET /api/vehicles – Retrieve all vehicles
    POST /api/vehicles – Register a vehicle
    GET /api/vehicles/{id} – Retrieve a vehicle
    PUT /api/vehicles/{id} – Update a vehicle
Mechanics
    GET /api/mechanics – Retrieve all mechanics
    POST /api/mechanics – Add a mechanic
    GET /api/mechanics/{id} – Retrieve a mechanic
    PUT /api/mechanics/{id} – Update a mechanic
Services
    GET /api/services – Retrieve all services
    POST /api/services – Add a service
    GET /api/services/{id} – Retrieve a service
    PUT /api/services/{id} – Update a service
Parts
    GET /api/parts – Retrieve all parts
    POST /api/parts – Add a part
    GET /api/parts/{id} – Retrieve a part
    PUT /api/parts/{id} – Update a part
Orders
    GET /api/orders – Retrieve all orders
    POST /api/orders – Create an order
    GET /api/orders/{id} – Retrieve an order
    POST /api/orders/{id}/assign – Assign a mechanic
    POST /api/orders/{id}/services – Add service to order
    DELETE /api/orders/{id}/services/{detailId} – Remove service
    POST /api/orders/{id}/parts – Add part to order
    DELETE /api/orders/{id}/parts/{usageId} – Remove part
    POST /api/orders/{id}/complete – Complete order
    POST /api/orders/{id}/bill – Generate bill
Billing and Reports
    GET /api/bills – Retrieve all bills
    PUT /api/bills/{id}/pay – Mark bill as paid
    GET /api/reports/summary – Dashboard statistics
    GET /api/reports/revenue – Revenue reports
    GET /api/reports/mechanic – Mechanic performance
    GET /api/reports/parts – Parts usage
    GET /api/reports/pending – Active and pending orders

---

## Project Structure
```
autoservice/
├── pom.xml
├── procedures_and_triggers.sql
├── static/
│   └── index.html
└── src/main/java/com/autoservice/
    ├── Server.java
    ├── DB.java
    ├── CustomerHandler.java
    ├── VehicleHandler.java
    ├── CatalogHandlers.java
    ├── ServiceOrderHandler.java
    ├── BillingHandlers.java
    └── StaticHandler.java
```
