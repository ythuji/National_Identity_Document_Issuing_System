# National Identity Document Issuing System (NIDIS) 🇱🇰

An enterprise-grade e-Government web application for issuing and managing Sri Lankan national identity documents:
- **National Identity Cards (Smart NIC)** — Department for Registration of Persons (DRP)
- **Driving Licenses** — Department of Motor Traffic (DMT)
- **Sri Lankan Passports** — Department of Immigration & Emigration (DIE)

---

## 🚀 Key Features

1. **End-to-End Application Lifecycle**:
   - Online application submission for **New**, **Renewal**, and **Lost** documents.
   - Secure document upload and storage using Microsoft SQL Server binary (`VARBINARY(MAX)`).
   - Multi-role workflow: Citizen Applicant &rarr; Verification Officer Review &rarr; Payment &rarr; Payment Verification & Card Dispatch &rarr; Delivery Confirmation.

2. **Automatic Sri Lankan Format Document Number Generation**:
   - When the officer verifies payment and marks the document as shipped (**PAYMENT_VERIFY** &rarr; **SHIPPED**), the system automatically creates an authentic official document number:
     - **NIC**: Modern 12-digit format `YYYYDDD0NNNC` calculated from Date of Birth (`YYYY`), Day of the Year (`DDD`, with `+500` offset for females), followed by a 5-digit unique serial & check digit (e.g., `200113508883`).
     - **Driving License**: DMT format starting with prefix `B` followed by 7 numeric digits (e.g., `B8294105`).
     - **Passport**: DIE format starting with prefix `N` followed by 7 numeric digits (e.g., `N7482910`).
   - Automatically stored in the database (`issued_nic_number`, `issued_license_number`, `issued_passport_number`).
   - Lifetime document preservation: For renewal or lost applications with an existing number, that number is preserved.

3. **Citizen Email & In-Portal Notifications**:
   - Automated SMTP email dispatched to citizen's registered email with official document details, shipping status, and safety guidance for future renewals.
   - Real-time in-app dashboard notification and badge display.

4. **Instant Auto-Fill & Lookup for Renewals & Lost Documents**:
   - Citizens can enter their issued document number in Renewal or Lost forms and click **Lookup & Fill** to auto-populate bio-data and records.

---

## 🛠️ Prerequisites for Running on Any Laptop

Before running the project on a teammate's computer, ensure the following software is installed:

| Requirement | Minimum Version | Download Link / Notes |
| :--- | :--- | :--- |
| **Java Development Kit (JDK)** | **JDK 21** | [Oracle JDK 21](https://www.oracle.com/java/technologies/downloads/#java21) or [Eclipse Temurin 21](https://adoptium.net/) |
| **Apache Maven** | **3.9+** | [Maven Download](https://maven.apache.org/download.cgi) (or use your IDE's bundled Maven) |
| **Microsoft SQL Server** | **2019 / 2022 / Express** | [SQL Server Downloads](https://www.microsoft.com/en-us/sql-server/sql-server-downloads) (Express or Developer Edition) |
| **SQL Server Management Studio (SSMS)** | **19+ or Azure Data Studio** | [SSMS Download](https://learn.microsoft.com/en-us/sql/ssms/download-sql-server-management-studio-ssms) |

---

## 🗄️ Database Setup (Run Once on Teammate's Laptop)

You have **two simple options** to set up the database:

### Option A: Complete Automatic Setup via SQL Script (Recommended)
1. Open **SQL Server Management Studio (SSMS)** or **Azure Data Studio**.
2. Connect to your local SQL Server instance (e.g., `localhost` or `.\SQLEXPRESS`).
3. Open the file **`setup_database.sql`** located in the root folder of this project.
4. Click **Execute** (or press `F5`).
5. **Done!** This single script will:
   - Create the `NIDIS_DB` database if it doesn't exist.
   - Create the SQL Server login and user `nidis_admin` with password `Nidis@2026Secure`.
   - Create all 12 tables with constraints and foreign keys.
   - Seed default roles, users, and realistic sample applications with Sri Lankan document numbers.

### Option B: Spring Boot Auto-Initialization
If you prefer Spring Boot to create tables and seed data automatically:
1. In SSMS or `sqlcmd`, simply execute:
   ```sql
   CREATE DATABASE NIDIS_DB;
   ```
2. When you start the Spring Boot application, **Hibernate** (`ddl-auto=update`) and **Spring SQL Init** (`src/main/resources/data.sql`) will automatically create all missing tables and seed initial demo accounts and applications!

---

## ⚙️ What Teammates Need to Change to Run on Their Laptop

Open **`src/main/resources/application.properties`** and check these 3 areas:

### 1. SQL Server Connection URL (`spring.datasource.url`)
Check how SQL Server is installed on your laptop:

- **If you installed SQL Server Developer or Standard (Default Instance on port 1433):**
  ```properties
  spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=NIDIS_DB;encrypt=true;trustServerCertificate=true
  ```

- **If you installed SQL Server Express (`SQLEXPRESS` named instance):**
  Change the URL to use your instance name:
  ```properties
  spring.datasource.url=jdbc:sqlserver://localhost;instanceName=SQLEXPRESS;databaseName=NIDIS_DB;encrypt=true;trustServerCertificate=true
  ```

### 2. SQL Server Username & Password
- **Default project credentials (created by `setup_database.sql`):**
  ```properties
  spring.datasource.username=nidis_admin
  spring.datasource.password=Nidis@2026Secure
  ```
- **If you prefer using your own SQL Server `sa` account:**
  ```properties
  spring.datasource.username=sa
  spring.datasource.password=YourSaPasswordHere
  ```

> [!IMPORTANT]
> **SQL Server Configuration Checklist:**
> 1. In **SQL Server Configuration Manager**, ensure **TCP/IP** is set to **Enabled** under *SQL Server Network Configuration &rarr; Protocols*.
> 2. Ensure **SQL Server and Windows Authentication mode** (Mixed Mode) is enabled in SQL Server Properties.
> 3. Restart the **SQL Server** service after enabling TCP/IP.

### 3. Email SMTP Configuration (Optional)
The project comes pre-configured with a working Gmail SMTP app password.
If your teammate wants to dispatch emails from their own Gmail:
```properties
spring.mail.username=your_email@gmail.com
spring.mail.password=your_16_digit_google_app_password
```
*(Note: If SMTP credentials fail or are offline, the application gracefully logs all emails and OTP codes directly to the console without interrupting any user actions).*

---

## 🏃 Building and Running the Application

Open a terminal or command prompt inside the project root:

```bash
# 1. Compile the project
mvn clean compile

# 2. Run the Spring Boot application
mvn spring-boot:run
```

Or open the project in **IntelliJ IDEA**, **VS Code**, or **Eclipse** and run:
`NationalIdentityDocumentIssuingSystemApplication.java`

Once running, access the web portal in your browser:
👉 **`http://localhost:8080`**

---

## 🔑 Pre-Seeded Test Accounts

All pre-seeded test accounts use password: **`Password123`**

| Role | Email Address | Password | Description / Use Case |
| :--- | :--- | :--- | :--- |
| **Super Administrator** | `admin@nidis.gov.lk` | `Password123` | System oversight, user status management, global audit logs. |
| **Verification Officer** | `officer@nidis.gov.lk` | `Password123` | Application verification queue, document review, approval, and **Verify Payment & Dispatch** (generates official Sri Lankan numbers). |
| **Citizen (Applicant 1)** | `citizen@test.lk` | `Password123` | Has shipped NIC (`200012345678`), shipped License (`B8294105`), shipped Passport (`N7482910`). |
| **Citizen (Applicant 2)** | `kamal@test.lk` | `Password123` | Has approved & paid Driving License ready for officer dispatch testing, and an NIC under review. |

---

## 🧪 Testing the Feature on Another Laptop

1. Sign in as **Verification Officer**: `officer@nidis.gov.lk` / `Password123`.
2. Open the **Verification Queue** (`/verification/queue`).
3. Click **Review** on an approved application that has `Payment: PAID` (e.g. `LIC-2026-DEMO02`).
4. Click **Verify Payment & Dispatch**.
5. Observe:
   - The status updates to **SHIPPED**.
   - An authentic Sri Lankan format document number is generated automatically.
   - The official document number is saved in the database.
   - An email dispatch notice with the document number is sent and printed in the terminal console.
6. Sign out and sign in as **Citizen** (`kamal@test.lk` / `Password123`).
7. Observe the new official document number displayed on the dashboard and application detail page!
8. Click **Apply for Renewal** or **Apply for Lost** on that service, type your newly issued document number, and click **Lookup & Fill** to watch your bio-data automatically populate from the database.

