# Lighthouse - AI Observability Platform

A comprehensive observability platform for monitoring, analyzing, and optimizing AI applications. Lighthouse provides real-time tracking of AI queries, hallucination detection, cost analytics, and email notifications to help developers build reliable, production-ready AI systems.

## 📋 Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Features](#features)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Configuration](#configuration)
- [Running the Application](#running-the-application)
- [API Documentation](#api-documentation)
- [SDK Integration](#sdk-integration)
- [Database Setup](#database-setup)
- [Authentication](#authentication)
- [Email Notifications](#email-notifications)
- [Development](#development)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)

## 🎯 Overview

Lighthouse is a full-stack AI observability platform that helps developers monitor, debug, and optimize their AI applications. It provides:

- **Real-time Monitoring**: Track latency, costs, and token usage across all AI API calls
- **Hallucination Detection**: Validate AI responses against database context with confidence scoring
- **Cost Analytics**: Monitor spending patterns and optimize usage
- **Email Notifications**: Get instant alerts when hallucinations are detected (confidence < 50%)
- **Multi-Project Support**: Organize AI operations into projects with isolated traces
- **Database Integration**: Connect PostgreSQL databases for RAG workflows and context-aware validation
- **Developer SDK**: Easy integration with minimal overhead

## 🏗️ Architecture

Lighthouse consists of two main components:

### Frontend (React + TypeScript + Vite)
- **Location**: `/Users/alan/Documents/coding/lighthouse-frontend`
- **Port**: `5173` (development)
- **Framework**: React 19 with TypeScript
- **Styling**: Tailwind CSS with custom architectural design system
- **State Management**: React Hooks (useState, useEffect, useContext)
- **Routing**: Hash-based navigation for landing, features, docs, and dashboard

### Backend (Spring Boot + Java)
- **Location**: Separate backend repository (Spring Boot application)
- **Port**: `8080` (default)
- **Framework**: Spring Boot 3.x
- **Database**: PostgreSQL
- **Authentication**: Supabase JWT (optional for development)
- **Email**: Spring Mail (SMTP)

### Data Flow

```
User Query → Frontend → Backend API → AI Service (Gemini/OpenAI/etc.)
                                    ↓
                            Hallucination Detector
                                    ↓
                            Database Context Validation
                                    ↓
                            Email Notification (if confidence < 50%)
                                    ↓
                            Trace Storage (PostgreSQL)
                                    ↓
                            Frontend Dashboard Display
```

## 🛠️ Tech Stack

### Frontend
- **React 19**: UI library
- **TypeScript**: Type safety
- **Vite**: Build tool and dev server
- **Tailwind CSS**: Utility-first CSS framework
- **Recharts**: Chart library for analytics
- **Supabase Client**: Authentication (optional)

### Backend
- **Spring Boot 3.x**: Java framework
- **Spring Data JPA**: Database ORM
- **PostgreSQL**: Primary database
- **Spring Security**: Authentication and authorization
- **Spring Mail**: Email notifications
- **Google Gemini API**: AI model for hallucination review
- **Gson**: JSON processing
- **Java HTTP Client**: External API calls

## ✨ Features

### Real-time Monitoring
- Track every AI query with detailed metrics
- Monitor latency, token usage, and cost in real-time
- View complete query history with searchable prompts and responses
- Performance metrics dashboard with averages and totals

### Hallucination Detection
- **Confidence Scoring**: Advanced algorithm analyzes AI responses against database context
    - **Supported (75-100%)**: High confidence, all claims well-supported
    - **Warning (50-75%)**: Moderate confidence, some claims need verification
    - **Hallucination Detected (0-50%)**: Low confidence, claims not well-supported
- **AI-Powered Review**: Automatic identification of unsupported claims with detailed explanations
- **Email Notifications**: Instant alerts when confidence drops below 50%

### Cost Analytics
- Track AI API costs over time with detailed charts
- Monitor spending per model, provider, and project
- Optimize usage to reduce expenses
- Support for multiple AI providers (OpenAI, Anthropic, Google Gemini, etc.)

### Database Integration
- **RAG Support**: Connect PostgreSQL databases for Retrieval Augmented Generation
- **Context-Aware Queries**: Automatically enrich AI prompts with relevant database context
- **Database Browser**: Browse tables, schemas, and query data directly from the dashboard
- **Connection Management**: Manage multiple database connections with secure credential storage

### Multi-Project Support
- Organize AI operations into separate projects
- Unique API keys for each project
- Isolated traces and team-based access controls
- Project-specific analytics and monitoring

### Developer SDK
- Lightweight integration with minimal overhead
- Works with any AI provider
- Automatic trace tracking
- Simple API key authentication

## 📦 Prerequisites

### Frontend
- **Node.js**: v18 or higher
- **npm** or **yarn**: Package manager
- **Modern Browser**: Chrome, Firefox, Safari, or Edge (latest versions)

### Backend
- **Java**: JDK 17 or higher
- **Maven**: 3.6 or higher
- **PostgreSQL**: 12 or higher
- **Google Gemini API Key**: For AI-powered hallucination review (optional)
- **SMTP Server**: For email notifications (Gmail, SendGrid, etc.) - optional

## 🚀 Installation

### Frontend Setup

1. **Clone the repository** (if not already done):
```bash
cd /Users/alan/Documents/coding/lighthouse-frontend
```

2. **Install dependencies**:
```bash
npm install
# or
yarn install
```

3. **Create environment file**:
```bash
cp .env.example .env
```

4. **Configure environment variables** (see [Configuration](#configuration) section)

### Backend Setup

1. **Navigate to backend directory**:
```bash
cd /path/to/lighthouse-backend
```

2. **Install dependencies** (Maven will download automatically):
```bash
mvn clean install
```

3. **Create database**:
```sql
CREATE DATABASE lighthouse;
```

4. **Configure application.properties** (see [Configuration](#configuration) section)

## ⚙️ Configuration

### Frontend Configuration (`.env`)

Create a `.env` file in the frontend root directory:

```env
# Supabase Configuration (Optional - for production authentication)
VITE_SUPABASE_URL=https://your-project.supabase.co
VITE_SUPABASE_ANON_KEY=your-anon-key

# Backend API URL (default: http://localhost:8080)
VITE_API_BASE_URL=http://localhost:8080
```

**Note**: If Supabase is not configured, the app will run in development mode without authentication.

### Backend Configuration (`application.properties`)

Configure the backend in `src/main/resources/application.properties`:

```properties
# Application
spring.application.name=Lighthouse

# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/lighthouse
spring.datasource.username=your_db_username
spring.datasource.password=your_db_password

# JPA Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# Google Gemini Configuration
gemini.api.key=your_gemini_api_key
gemini.api.url=https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent

# CORS Configuration
spring.web.cors.allowed-origins=http://localhost:5173
spring.web.cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS
spring.web.cors.allowed-headers=*

# Supabase Configuration (Optional)
supabase.url=https://your-project.supabase.co
supabase.jwt.secret=your-jwt-secret

# Email Configuration (Optional - for hallucination alerts)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
spring.mail.from=noreply@lighthouse.ai
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

**Environment Variables Alternative**:
You can also use environment variables instead of hardcoding values:

```properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}
gemini.api.key=${GEMINI_API_KEY}
supabase.url=${SUPABASE_URL}
supabase.jwt.secret=${SUPABASE_JWT_SECRET}
spring.mail.username=${EMAIL_USER}
spring.mail.password=${EMAIL_PASS}
```

### Gmail App Password Setup

For Gmail SMTP, you need to create an App Password:

1. Go to [Google Account Settings](https://myaccount.google.com/)
2. Navigate to **Security** → **2-Step Verification**
3. At the bottom, click **App passwords**
4. Generate a new app password for "Mail"
5. Use this password in `spring.mail.password`

## 🏃 Running the Application

### Development Mode

#### Start Backend (Terminal 1)
```bash
cd /path/to/lighthouse-backend
mvn spring-boot:run
```

The backend will start on `http://localhost:8080`

#### Start Frontend (Terminal 2)
```bash
cd /Users/alan/Documents/coding/lighthouse-frontend
npm run dev
# or
yarn dev
```

The frontend will start on `http://localhost:5173`

### Production Build

#### Frontend
```bash
npm run build
# or
yarn build
```

The built files will be in the `dist/` directory.

#### Backend
```bash
mvn clean package
java -jar target/lighthouse-0.0.1-SNAPSHOT.jar
```

## 📚 API Documentation

### Base URL
```
http://localhost:8080/api
```

### Authentication
Most endpoints require authentication via Supabase JWT token:
```
Authorization: Bearer <jwt-token>
```

For SDK endpoints, use API key:
```
X-API-Key: lh_<your-api-key>
```

### Endpoints

#### Traces

**GET `/api/traces`**
- Get all traces (optionally filtered by project)
- Query params: `?projectId=<project-id>` (optional)
- Returns: Array of Trace objects

**GET `/api/traces/{id}`**
- Get a specific trace by ID
- Returns: Trace object

**POST `/api/traces/query`**
- Execute AI query without database context
- Body: `{ "prompt": "your query" }`
- Returns: Trace object

**POST `/api/traces/query-with-db`**
- Execute AI query with database context for hallucination detection
- Body: `{ "prompt": "your query", "dbConnectionId": "connection-id" }`
- Returns: Trace object with hallucination data

**POST `/api/traces/validate-response`**
- Validate a pre-generated AI response against database
- Body: `{ "prompt": "...", "response": "...", "databaseConnectionId": "...", "tokensUsed": 100, "costUsd": 0.001, "latencyMs": 250, "provider": "gemini" }`
- Returns: Trace object with hallucination data

**POST `/api/traces/{traceId}/check-hallucinations`**
- Re-check hallucinations for an existing trace
- Body: `{ "dbConnectionId": "connection-id" }`
- Returns: Updated Trace object

**GET `/api/traces/stats`**
- Get statistics (optionally filtered by project)
- Query params: `?projectId=<project-id>` (optional)
- Returns: `{ "totalCost": 0.0, "totalRequests": 0, "averageLatency": 0.0 }`

**DELETE `/api/traces/clear`**
- Delete all traces (for testing)

#### Projects

**GET `/api/projects`**
- Get all projects for authenticated user
- Returns: Array of Project objects

**GET `/api/projects/{id}`**
- Get a specific project by ID
- Returns: Project object

**POST `/api/projects`**
- Create a new project
- Body: `{ "name": "Project Name", "description": "Optional description" }`
- Returns: Project object with generated API key

**DELETE `/api/projects/{id}`**
- Delete a project

#### Database Connections

**GET `/api/db-connections`**
- Get all database connections
- Returns: Array of DatabaseConnection objects

**POST `/api/db-connections`**
- Create a new database connection
- Body: `{ "name": "...", "host": "...", "port": 5432, "database": "...", "username": "...", "password": "..." }`
- Returns: DatabaseConnection object

**POST `/api/db-connections/{id}/test`**
- Test a database connection
- Returns: `{ "success": true, "message": "..." }`

**GET `/api/db-connections/{id}/tables`**
- Get all tables from a database connection
- Returns: Array of table names

**POST `/api/db-connections/{id}/search`**
- Search database with a query
- Body: `{ "query": "your search query" }`
- Returns: `{ "results": "..." }`

**DELETE `/api/db-connections/{id}`**
- Delete a database connection

#### SDK

**POST `/api/sdk/traces`**
- Send trace from external SDK
- Headers: `X-API-Key: lh_<your-api-key>`
- Body: `{ "prompt": "...", "response": "...", "tokensUsed": 100, "costUsd": 0.001, "latencyMs": 250, "provider": "openai" }`
- Returns: Created Trace object

#### User Preferences

**GET `/api/user/preferences`**
- Get user notification preferences
- Returns: `{ "emailNotifications": true }`

**PUT `/api/user/preferences`**
- Update user notification preferences
- Body: `{ "emailNotifications": true }`
- Returns: Updated preferences

## 🔌 SDK Integration

### JavaScript/TypeScript

```javascript
import { LighthouseSDK } from '@lighthouse-ai/sdk';

const lighthouse = new LighthouseSDK({
  apiKey: 'lh_your-api-key',
  baseUrl: 'http://localhost:8080'
});

// Wrap your AI API call
async function queryAI(prompt) {
  const startTime = Date.now();
  
  // Your AI API call
  const response = await yourAIService.call(prompt);
  
  // Send trace to Lighthouse
  await lighthouse.track({
    prompt: prompt,
    response: response.text,
    tokensUsed: response.usage.total_tokens,
    costUsd: calculateCost(response),
    latencyMs: Date.now() - startTime,
    provider: 'openai'
  });
  
  return response;
}
```

### Python

```python
from lighthouse_sdk import LighthouseSDK
import time

lighthouse = LighthouseSDK(
    api_key='lh_your-api-key',
    base_url='http://localhost:8080'
)

def query_ai(prompt):
    start_time = time.time()
    
    # Your AI API call
    response = your_ai_service.call(prompt)
    
    # Send trace to Lighthouse
    lighthouse.track(
        prompt=prompt,
        response=response.text,
        tokens_used=response.usage.total_tokens,
        cost_usd=calculate_cost(response),
        latency_ms=int((time.time() - start_time) * 1000),
        provider='openai'
    )
    
    return response
```

## 🗄️ Database Setup

### PostgreSQL Installation

**macOS**:
```bash
brew install postgresql
brew services start postgresql
```

**Linux**:
```bash
sudo apt-get install postgresql postgresql-contrib
sudo systemctl start postgresql
```

**Windows**:
Download from [PostgreSQL website](https://www.postgresql.org/download/windows/)

### Create Database

```sql
-- Connect to PostgreSQL
psql -U postgres

-- Create database
CREATE DATABASE lighthouse;

-- Create user (optional)
CREATE USER lighthouse_user WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE lighthouse TO lighthouse_user;
```

### Database Schema

The backend uses JPA/Hibernate with `ddl-auto=update`, so tables are created automatically. Main tables:

- `traces`: AI query traces
- `projects`: User projects
- `database_connections`: Database connection configurations
- `api_credentials`: API keys for AI providers

## 🔐 Authentication

### Development Mode (No Authentication)

If Supabase is not configured, the app runs in development mode:
- All API endpoints are accessible without authentication
- Projects are created with "anonymous" user ID
- Email notifications require manual user email configuration

### Production Mode (Supabase)

1. **Create Supabase Project**:
    - Go to [Supabase](https://supabase.com)
    - Create a new project
    - Get your project URL and anon key

2. **Get JWT Secret**:
    - In Supabase dashboard, go to Settings → API
    - Copy the JWT Secret

3. **Configure Backend**:
```properties
supabase.url=https://your-project.supabase.co
supabase.jwt.secret=your-jwt-secret
```

4. **Configure Frontend**:
```env
VITE_SUPABASE_URL=https://your-project.supabase.co
VITE_SUPABASE_ANON_KEY=your-anon-key
```

5. **Update Security Config**:
    - Ensure `SecurityConfig.java` applies JWT filter when Supabase is configured
    - Development mode should allow unauthenticated access when Supabase is not configured

## 📧 Email Notifications

### Configuration

Email notifications are sent when hallucinations are detected (confidence < 50%). Configure in `application.properties`:

```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
spring.mail.from=noreply@lighthouse.ai
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

### Email Content

When a hallucination is detected, users receive an email with:
- Confidence score
- Number of unsupported claims
- Query prompt (truncated)
- AI response (truncated)
- Trace ID with dashboard link

### Testing Email

Use the test endpoint:
```bash
curl -X GET http://localhost:8080/api/traces/test-email \
  -H "Authorization: Bearer <your-jwt-token>"
```

### User Preferences

Users can enable/disable email notifications via the dashboard:
- Navigate to Dashboard → Notifications
- Toggle email notifications on/off
- Preferences are stored per user

## 💻 Development

### Frontend Development

**Project Structure**:
```
src/
├── components/          # React components
│   ├── Dashboard.tsx
│   ├── TraceList.tsx
│   ├── HallucinationWarning.tsx
│   └── ...
├── services/           # API services
│   └── api.ts
├── contexts/           # React contexts
│   └── AuthContext.tsx
├── types/              # TypeScript types
│   └── Trace.ts
├── lib/                # Utilities
│   └── supabase.ts
└── App.tsx             # Main app component
```

**Adding a New Component**:
1. Create component in `src/components/`
2. Use TypeScript for type safety
3. Follow architectural design system (see `index.css`)
4. Use Tailwind CSS classes

**Styling Guidelines**:
- Use architectural color palette (defined in `index.css`)
- Follow minimalist design principles
- Use `font-light` for body text
- Use `uppercase tracking-wider` for labels
- Use `border-foreground/10` for subtle borders

### Backend Development

**Project Structure**:
```
src/main/java/com/example/lighthouse/
├── Controller/         # REST controllers
│   ├── TraceController.java
│   ├── ProjectController.java
│   └── ...
├── Model/              # JPA entities
│   ├── Trace.java
│   ├── Project.java
│   └── ...
├── repository/         # JPA repositories
│   ├── TraceRepository.java
│   └── ...
├── service/            # Business logic
│   ├── AIService.java
│   ├── HallucinationDetector.java
│   ├── EmailService.java
│   └── ...
└── config/             # Configuration
    ├── SecurityConfig.java
    └── SupabaseConfig.java
```

**Adding a New Endpoint**:
1. Create controller method in appropriate `*Controller.java`
2. Add `@CrossOrigin(origins = "http://localhost:5173")` for CORS
3. Use `@Autowired` for dependency injection
4. Handle authentication via `Authentication` parameter
5. Return appropriate HTTP status codes

**Database Changes**:
- JPA entities use `@Entity` and `@Table` annotations
- Repositories extend `JpaRepository<Entity, ID>`
- Use `@ManyToOne`, `@OneToMany` for relationships
- `ddl-auto=update` automatically creates/updates tables

## 🐛 Troubleshooting

### Frontend Issues

**Blank Page**:
- Check browser console for errors
- Verify Supabase configuration (or disable for dev mode)
- Check that backend is running on port 8080
- Verify CORS is enabled in backend

**API Calls Failing**:
- Check network tab in browser DevTools
- Verify backend is running
- Check authentication headers are being sent
- Verify CORS configuration

**Build Errors**:
```bash
# Clear cache and reinstall
rm -rf node_modules package-lock.json
npm install
```

### Backend Issues

**Database Connection Failed**:
- Verify PostgreSQL is running: `psql -U postgres -l`
- Check database credentials in `application.properties`
- Verify database exists: `CREATE DATABASE lighthouse;`

**JWT Validation Failed**:
- Check Supabase JWT secret matches
- Verify token is being sent in `Authorization` header
- Check token hasn't expired
- For development, ensure SecurityConfig allows unauthenticated access

**Email Not Sending**:
- Verify SMTP configuration
- Check Gmail app password (not regular password)
- Test with `/api/traces/test-email` endpoint
- Check backend logs for email service errors
- Verify user email is available in authentication

**Port Already in Use**:
```bash
# Find process using port 8080
lsof -i :8080
# Kill process
kill -9 <PID>
```

### Common Errors

**"Cannot resolve symbol"** (IntelliJ):
- File → Invalidate Caches / Restart
- Maven → Reload Project

**"Ambiguous mapping"**:
- Check for duplicate `@RequestMapping` paths
- Ensure method signatures are unique

**CORS Errors**:
- Verify `@CrossOrigin` annotation on controllers
- Check `SecurityConfig` allows CORS
- Verify frontend URL matches `allowed-origins`

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Code Style

- **Frontend**: Follow existing React/TypeScript patterns
- **Backend**: Follow Spring Boot conventions
- Use meaningful variable and method names
- Add comments for complex logic
- Write descriptive commit messages

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🙏 Acknowledgments

- Built with [React](https://react.dev/) and [Spring Boot](https://spring.io/projects/spring-boot)
- Uses [Tailwind CSS](https://tailwindcss.com/) for styling
- [Supabase](https://supabase.com/) for authentication
- [Google Gemini](https://ai.google.dev/) for AI-powered features

## 📞 Support

For issues, questions, or contributions:
- Open an issue on GitHub
- Check existing documentation
- Review troubleshooting section

---

**Built with ❤️ for reliable AI systems**
