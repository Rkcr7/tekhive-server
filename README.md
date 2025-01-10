# TekHive Server

## Overview
TekHive Server is the backend component of the TekHive application, a social networking platform designed for tech professionals. This Spring Boot-based server provides RESTful APIs for user management, authentication, real-time chat, and social networking features.

## Related Repositories
- Frontend: [TekHive](https://github.com/Rkcr7/tekhive-client)
## Technology Stack
- **Framework**: Spring Boot
- **Language**: Java
- **Database**: PostgreSQL (Hosted on Neon.tech)
- **Security**: JWT (JSON Web Tokens)
- **Real-time Communication**: WebSocket
- **Build Tool**: Maven

## Key Features
- User Authentication and Authorization
- Real-time Chat System
- Friend Management System
- User Profile Management
- Secure Password Handling
- WebSocket Integration for Real-time Updates

## Project Structure
```
tekhive-server/
├── src/main/java/com/tekhive/
│   ├── controllers/       # REST API endpoints
│   ├── models/           # Data models/entities
│   ├── repositories/     # Database repositories
│   ├── security/        # Security configurations and JWT
│   ├── services/        # Business logic
│   └── chat/            # WebSocket and chat functionality
```

## Prerequisites
- Java JDK 11 or higher
- Maven
- PostgreSQL (Remote connection to Neon.tech)

## Configuration
The application uses the following key configurations in `application.properties`:
- Database connection (PostgreSQL on Neon.tech)
- JWT Security settings
- Server port configuration
- WebSocket endpoints

## API Endpoints
- `/api/auth/*` - Authentication endpoints
- `/api/users/*` - User management
- `/api/chat/*` - Chat functionality
- `/api/friends/*` - Friend management

## Security
- JWT-based authentication
- Password encryption
- Secure WebSocket connections
- CORS configuration

## Database Schema
- Users table
- Chat messages
- Friend relationships
- User profiles

## Getting Started

### Installation
1. Clone the repository:
```bash
git clone [repository-url]
```

2. Navigate to project directory:
```bash
cd tekhive-server
```

3. Build the project:
```bash
mvn clean install
```

4. Run the application:
```bash
mvn spring-boot:run
```

The server will start on port 8085 by default.

## Environment Variables
- `SPRING_DATASOURCE_URL`: Database URL
- `SPRING_DATASOURCE_USERNAME`: Database username
- `SPRING_DATASOURCE_PASSWORD`: Database password
- `JWT_SECRET`: Secret key for JWT tokens

## Contributing
1. Fork the repository
2. Create your feature branch
3. Commit your changes
4. Push to the branch
5. Create a new Pull Request
