# AutoService Pro – Java Backend

## Prerequisites
- Java 17+
- Maven 3.8+
- MySQL 8.x with your autoservice database

## 1. Configure Database
Edit `src/main/java/com/autoservice/DB.java`:
```java
private static final String URL  = "jdbc:mysql://localhost:3306/YOUR_DB_NAME?...";
private static final String USER = "root";
private static final String PASS = "your_password";
```

## 2. Run Stored Procedures & Triggers
```bash
mysql -u root -p your_database < procedures_and_triggers.sql
```

## 3. Build the Fat JAR
```bash
mvn clean package -q
```
This creates `target/autoservice-1.0.jar` bundling MySQL connector + org.json.

## 4. Run the Server
```bash
java -jar target/autoservice-1.0.jar
```
The server starts on **http://localhost:8080**

The `static/` folder (containing `index.html`) must be in the **same directory** as where you run the JAR.

## 5. Open the UI
Open your browser: **http://localhost:8080**

---

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET/POST | `/api/customers` | List / Create customers |
| GET/PUT | `/api/customers/{id}` | Get / Update customer |
| GET/POST | `/api/vehicles` | List / Register vehicles |
| GET/PUT | `/api/vehicles/{id}` | Get / Update vehicle |
| GET/POST | `/api/mechanics` | List / Add mechanics |
| GET/PUT | `/api/mechanics/{id}` | Get / Update mechanic |
| GET/POST | `/api/services` | List / Add services |
| GET/PUT | `/api/services/{id}` | Get / Update service |
| GET/POST | `/api/parts` | List / Add parts |
| GET/PUT | `/api/parts/{id}` | Get / Update part |
| GET/POST | `/api/orders` | List / Create orders |
| GET | `/api/orders/{id}` | Get single order |
| POST | `/api/orders/{id}/assign` | Assign mechanic (calls Assign_Mechanic SP) |
| GET/POST | `/api/orders/{id}/services` | List / Add service to order |
| DELETE | `/api/orders/{id}/services/{detailId}` | Remove service |
| GET/POST | `/api/orders/{id}/parts` | List / Add part (calls Add_Part_Usage SP) |
| DELETE | `/api/orders/{id}/parts/{usageId}` | Remove part |
| POST | `/api/orders/{id}/complete` | Complete order (calls Complete_Service SP) |
| POST | `/api/orders/{id}/bill` | Generate bill (calls Generate_Bill SP) |
| GET | `/api/bills` | All bills |
| PUT | `/api/bills/{id}/pay` | Mark bill as paid |
| GET | `/api/reports/summary` | Dashboard stats |
| GET | `/api/reports/revenue` | Revenue by month |
| GET | `/api/reports/mechanic` | Mechanic performance |
| GET | `/api/reports/parts` | Parts usage |
| GET | `/api/reports/pending` | Pending/active orders |

## Project Structure
```
autoservice/
├── pom.xml
├── procedures_and_triggers.sql
├── static/
│   └── index.html          ← Frontend SPA
└── src/main/java/com/autoservice/
    ├── Server.java          ← HTTP server + routing
    ├── DB.java              ← MySQL connection
    ├── CustomerHandler.java
    ├── VehicleHandler.java
    ├── CatalogHandlers.java ← ServiceHandler, MechanicHandler, PartsHandler
    ├── ServiceOrderHandler.java
    ├── BillingHandlers.java ← BillHandler, ReportHandler
    └── StaticHandler.java
```
