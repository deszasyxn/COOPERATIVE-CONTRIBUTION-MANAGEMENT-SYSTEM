# Cooperative Contribution Management System

A Java web application scaffold using Spring Boot and Maven. The existing HTML,
CSS, and JavaScript pages are served as static resources by Spring Boot.

## Requirements

- Java 17 or later
- Maven 3.6.3 or later

## Run the application

From the project directory:

```sh
mvn spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080) for the landing page.
The login, signup, and admin pages are available at:

- `/login&signup/login.html`
- `/login&signup/signup.html`
- `/admin/adminDash.html`

The login and signup forms are currently wired to `/api/...` endpoints in the
frontend, but those backend endpoints have not been implemented yet.
