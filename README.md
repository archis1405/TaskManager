## Features

| Category | Features |
|---|---|
| **Auth** | JWT register/login/refresh, BCrypt hashing, role-based access |
| **Tasks** | CRUD, status/priority management, due dates, filtering, sorting, full-text search |
| **Projects** | Create teams, invite/remove members, leave projects |
| **Collaboration** | Comments on tasks, file attachments (up to 10MB), paginated history |
| **Notifications** | Real-time WebSocket push + persistent notification inbox |
| **AI** | Optional AI task description generation via Claude/OpenAI API |
| **API Docs** | Swagger UI auto-generated at `/swagger-ui.html` |

---

##  Architecture

```
src/main/java/com/taskflow/
├── config/          # Spring Security, WebSocket, CORS, OpenAPI
├── controller/      # REST controllers (Auth, Task, Project, Collaboration, AI)
├── dto/
│   ├── request/     # RegisterRequest, TaskRequest, ProjectRequest, ...
│   └── response/    # ApiResponse<T> envelope, TaskResponse, UserSummaryResponse, ...
├── entity/          # JPA entities: User, Task, Project, Comment, Attachment, Notification
├── exception/       # Custom exceptions + GlobalExceptionHandler
├── repository/      # Spring Data JPA repos + TaskSpecification (dynamic filters)
├── security/        # JwtUtils, JwtAuthenticationFilter, CustomUserDetailsService
└── service/         # AuthService, TaskService, ProjectService, CommentService,
                     #   AttachmentService, NotificationService, AiTaskService
```

---

## Quick Start

### 1. Clone the Repository
```bash
git clone https://github.com/YOUR_USERNAME/taskflow-backend.git
cd taskflow-backend
```

### 2. Start with Docker Compose (Recommended)
```bash
docker-compose up -d
```
The API will be available at `http://localhost:8080`.

### 3. Manual Setup

**Create the database:**
```sql
CREATE DATABASE taskflow;
```

**Configure `application.properties`** (or use environment variables):
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/taskflow
spring.datasource.username=postgres
spring.datasource.password=your_password
app.jwt.secret=your-base64-encoded-secret-key-at-least-32-chars
```

**Run the application:**
```bash
mvn spring-boot:run
```

---

##  API Reference

Interactive docs: **`http://localhost:8080/swagger-ui.html`**

### Authentication

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/auth/register` | Register new account |
| `POST` | `/api/auth/login` | Login → returns JWT |
| `POST` | `/api/auth/refresh` | Refresh access token |
| `GET` | `/api/auth/me` | Get current user profile |
| `PUT` | `/api/auth/me` | Update profile |
| `POST` | `/api/auth/logout` | Logout (client discards token) |

### Tasks

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/tasks` | Create task |
| `GET` | `/api/tasks` | List tasks (with filters) |
| `GET` | `/api/tasks/my` | My assigned tasks |
| `GET` | `/api/tasks/{id}` | Get task by ID |
| `PUT` | `/api/tasks/{id}` | Update task |
| `PATCH` | `/api/tasks/{id}/status` | Update status |
| `PATCH` | `/api/tasks/{id}/complete` | Mark completed |
| `PATCH` | `/api/tasks/{id}/assign` | Assign to user |
| `DELETE` | `/api/tasks/{id}` | Delete task |

**Query Parameters for `GET /api/tasks`:**

| Param | Type | Description |
|-------|------|-------------|
| `projectId` | Long | Filter by project |
| `assigneeId` | Long | Filter by assignee |
| `status` | Enum | `OPEN`, `IN_PROGRESS`, `IN_REVIEW`, `COMPLETED`, `CANCELLED` |
| `priority` | Enum | `LOW`, `MEDIUM`, `HIGH`, `URGENT` |
| `dueDateFrom` | Date | `YYYY-MM-DD` |
| `dueDateTo` | Date | `YYYY-MM-DD` |
| `search` | String | Full-text search in title & description |
| `page` | int | Page number (0-based) |
| `size` | int | Page size |
| `sort` | String | e.g. `createdAt,desc` |

### Projects

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/projects` | Create project/team |
| `GET` | `/api/projects` | My projects |
| `GET` | `/api/projects/{id}` | Get project |
| `PUT` | `/api/projects/{id}` | Update project |
| `DELETE` | `/api/projects/{id}` | Delete project |
| `POST` | `/api/projects/{id}/members/{userId}` | Add member |
| `DELETE` | `/api/projects/{id}/members/{userId}` | Remove member |
| `POST` | `/api/projects/{id}/leave` | Leave project |

### Collaboration

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/tasks/{id}/comments` | Add comment |
| `GET` | `/api/tasks/{id}/comments` | Get comments |
| `PUT` | `/api/comments/{id}` | Edit comment |
| `DELETE` | `/api/comments/{id}` | Delete comment |
| `POST` | `/api/tasks/{id}/attachments` | Upload file |
| `GET` | `/api/tasks/{id}/attachments` | List attachments |
| `GET` | `/api/attachments/{id}/download` | Download file |
| `DELETE` | `/api/attachments/{id}` | Delete attachment |

### Notifications

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/notifications` | Get notifications |
| `GET` | `/api/notifications/unread-count` | Unread count |
| `PATCH` | `/api/notifications/{id}/read` | Mark read |
| `PATCH` | `/api/notifications/read-all` | Mark all read |

### AI (Optional)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/ai/generate-task` | Generate task description from brief input |

---

## Authentication Flow

```
1. POST /api/auth/register  →  { accessToken, refreshToken, user }
2. Include header:          →  Authorization: Bearer <accessToken>
3. Token expires (24h)      →  POST /api/auth/refresh?refreshToken=<token>
```

---

## Real-Time Notifications (WebSocket)

Connect using STOMP over SockJS:

```javascript
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({ Authorization: 'Bearer ' + token }, () => {
  stompClient.subscribe('/user/queue/notifications', (message) => {
    const notification = JSON.parse(message.body);
    console.log('New notification:', notification);
  });
});
```

Notifications are pushed when:
- A task is assigned to you
- Someone comments on your task

---

##  AI Task Generation (Optional Extension)

Enable in `application.properties`:
```properties
app.ai.enabled=true
app.ai.api-key=your-anthropic-api-key
app.ai.model=claude-3-haiku-20240307
```

**Usage:**
```bash
POST /api/ai/generate-task
{
  "userInput": "Set up CI/CD pipeline for the backend",
  "context": "DevOps project using GitHub Actions"
}
```

**Response:**
```json
{
  "title": "Configure GitHub Actions CI/CD Pipeline",
  "description": "Set up automated CI/CD pipeline using GitHub Actions...",
  "priority": "HIGH",
  "summary": "Automate build, test, and deployment workflow"
}
```

## 🗂️ Data Model

```
User ──< Project (owner)
User >──< Project (members M:N)
Project ──< Task
User ──< Task (creator)
User ──< Task (assignee)
Task ──< Comment
Task ──< Attachment
User ──< Notification
```
