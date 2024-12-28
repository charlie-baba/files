# Google Drive Integration Backend

A Spring Boot application that provides REST APIs for Google Drive integration with OAuth 2.0 authentication.

## Project Overview

This Spring Boot backend provides:
- Google OAuth 2.0 authentication
- JWT token-based session management
- Google Drive file operations:
    - File upload
    - File download
    - File deletion
    - File listing
- Database integration for user management

## Prerequisites

Before you begin, ensure you have:
- [Java JDK 17+](https://adoptium.net/)
- [Maven](https://maven.apache.org/download.cgi)
- [PostgreSQL](https://www.postgresql.org/download/) (or your preferred database)
- Google Cloud Platform account with:
    - Google Drive API enabled
    - OAuth 2.0 credentials configured

## Getting Started

1. Clone the repository:

```bash
git clone git@github.com:charlie-baba/files.git
```

2. Configure application.properties:

Update the following properties of `src/main/resources/application.properties` with your credential:

```properties
# Server Configuration
server.port=8080

# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/your_database
spring.datasource.username=your_username
spring.datasource.password=your_password

# Google OAuth2 Configuration
google.oauth.client.id=your_google_client_id
google.oauth.client.secret=your_google_client_secret


```

3. Install dependencies:

```bash
mvn clean install
```

## Running the Application

### Development Mode

The easiest approach is to run the application from an IDE like IntelliJ IDEA. 

- Open the project with an IDE 
- Navigate to `FilesApplication.java` file and click the play (run) button at the to right.

Alternatively,

```bash
mvn spring-boot:run
```

The application will start at `http://localhost:8080`

### Production Build

```bash
mvn clean package
java -jar target/files-0.0.1-SNAPSHOT.jar
```

## API Endpoints

### Authentication Endpoints

- `GET /api/auth/login/google` - Get Google OAuth URL
- `GET /api/auth/google/callback` - OAuth callback handler
- `GET /api/auth/check` - Validate JWT token
- `POST /api/auth/logout` - User logout

### Google Drive Endpoints

- `GET /api/google-drive/files` - List user's files
- `POST /api/google-drive/upload` - Upload file
- `GET /api/google-drive/download/{fileId}` - Download file
- `DELETE /api/google-drive/delete/{fileId}` - Delete file

### Test Endpoints

- `GET /api/test/db-connection` - Test database connectivity

## Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── com/strac/files/
│   │       ├── config/             # App Configuration files
│   │       ├── controllers/        # REST controllers
│   │       ├── exceptions/         # Custom Exception Handlers
│   │       ├── interceptors/       # HTTP request interceptors
│   │       ├── models/             # Entity classes
│   │           ├── dto/            # Data Transfer Objects
│   │           └── repositories/   # Data repositories
│   │       ├── services/           
│   │           └── impl/           # Business logic
│   │       ├── utils/              # Utility classes
│   │       └── FilesApplication.java              
│   └── resources/
│       └── application.properties
├── test/
```

## Security Configuration

The application implements:
- Google OAuth 2.0 for authentication
- JWT token-based session management
- Spring Security for endpoint protection
- CORS configuration for frontend integration

## Design Decisions

- **OAuth Flow**: Uses authorization code flow for secure authentication
- **Token Management**: JWT tokens for stateless authentication
- **File Operations**: Streaming for efficient file transfer
- **Error Handling**: Global exception handling with appropriate HTTP status codes
- **Database**: JPA/Hibernate for database operations
- **Logging**: SLF4J with Lombok for comprehensive logging

## Development Notes

1. Ensure all required Google OAuth2 credentials are configured
2. Update callback URLs in Google Cloud Console
3. Configure CORS settings for your frontend domain
4. Set appropriate file size limits in properties
5. Use environment variables for sensitive data in production
