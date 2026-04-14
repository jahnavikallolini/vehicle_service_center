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

### Customers
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/customers | Retrieve all customers |
| POST | /api/customers | Create a new customer |
| GET | /api/customers/{id} | Retrieve a specific customer |
| PUT | /api/customers/{id} | Update a customer |

---

### Vehicles
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/vehicles | Retrieve all vehicles |
| POST | /api/vehicles | Register a vehicle |
| GET | /api/vehicles/{id} | Retrieve a specific vehicle |
| PUT | /api/vehicles/{id} | Update a vehicle |

---

### Mechanics
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/mechanics | Retrieve all mechanics |
| POST | /api/mechanics | Add a mechanic |
| GET | /api/mechanics/{id} | Retrieve a specific mechanic |
| PUT | /api/mechanics/{id} | Update a mechanic |

---

### Services
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/services | Retrieve all services |
| POST | /api/services | Add a service |
| GET | /api/services/{id} | Retrieve a specific service |
| PUT | /api/services/{id} | Update a service |

---

### Parts
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/parts | Retrieve all parts |
| POST | /api/parts | Add a part |
| GET | /api/parts/{id} | Retrieve a specific part |
| PUT | /api/parts/{id} | Update a part |

---

### Orders
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/orders | Retrieve all orders |
| POST | /api/orders | Create a new order |
| GET | /api/orders/{id} | Retrieve a specific order |
| POST | /api/orders/{id}/assign | Assign a mechanic |
| GET / POST | /api/orders/{id}/services | List / Add services to order |
| DELETE | /api/orders/{id}/services/{detailId} | Remove a service from order |
| GET / POST | /api/orders/{id}/parts | List / Add parts to order |
| DELETE | /api/orders/{id}/parts/{usageId} | Remove a part from order |
| POST | /api/orders/{id}/complete | Mark order as completed |
| POST | /api/orders/{id}/bill | Generate bill for order |

---

### Billing and Reports

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/bills | Retrieve all bills |
| PUT | /api/bills/{id}/pay | Mark bill as paid |
| GET | /api/reports/summary | Dashboard statistics |
| GET | /api/reports/revenue | Revenue analysis |
| GET | /api/reports/mechanic | Mechanic performance |
| GET | /api/reports/parts | Parts usage analysis |
| GET | /api/reports/pending | Pending and active orders |

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
