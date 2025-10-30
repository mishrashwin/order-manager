# Order Manager (Spring Boot Starter)

A starter project to manage and track client orders from placement to delivery.

## Features
- RESTful CRUD API for Orders
- H2 in-memory database
- Flyway for DB versioning
- Maven build
- Spring Boot 3

## Run Locally
```bash
mvn spring-boot:run
```
Visit: [http://localhost:8080/api/orders](http://localhost:8080/api/orders)

## Build Executable JAR
```bash
mvn clean package
java -jar target/order-manager-0.0.1-SNAPSHOT.jar
```

## Example API Calls
POST `/api/orders`
```json
{
  "clientName": "Acme Corp",
  "vendor": "Vendor A",
  "product": "Widget X",
  "status": "PROCESSING",
  "orderDate": "2025-10-30",
  "deliveryDate": "2025-11-10"
}
```
