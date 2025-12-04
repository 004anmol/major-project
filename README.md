# AI Mentoring System

A comprehensive college mentoring system with role-based access control for Students and Teachers (Mentors).

## Features

### Student Features
- **Role-based Login**: Secure authentication for students
- **AI-Generated Quizzes**: Generate quizzes using Grok API based on topic and difficulty
- **Quiz Management**: Take quizzes and view detailed results with strength/weakness analysis
- **Document Upload**: Upload important documents on teacher's request
- **Assignment Submission**: Submit assignments with file upload before deadlines
- **Course Enrollment**: Enroll in courses linked to YouTube playlists
- **Notifications**: Receive and view notifications from mentor
- **One-to-One Mentorship**: Each student is assigned to one mentor/teacher

### Teacher Features
- **Role-based Login**: Secure authentication for teachers
- **Document Management**: Request documents from students, review and provide remarks
- **Notifications**: Send important notifications to students
- **Assignment Management**: Create assignments with deadlines for students
- **Quiz Creation**: Generate quizzes manually for students
- **Course Management**: Create courses linked to YouTube playlists
- **Student Management**: View and manage assigned students

## Technology Stack

- **Backend**: Spring Boot 4.0.0
- **Database**: PostgreSQL
- **Security**: Spring Security with role-based access control
- **Frontend**: Thymeleaf templates with Bootstrap 5
- **AI Integration**: Grok API for quiz generation and analysis
- **File Upload**: Spring Multipart for document and assignment submissions

## Prerequisites

- Java 21
- PostgreSQL database
- Maven 3.6+
- Grok API key

## Setup Instructions

### 1. Database Setup

Create a PostgreSQL database:
```sql
CREATE DATABASE ai_mentoring_db;
```

### 2. Configuration

Update `src/main/resources/application.properties`:

```properties
# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/ai_mentoring_db
spring.datasource.username=your_postgres_username
spring.datasource.password=your_postgres_password

# Grok API Configuration
grok.api.url=https://api.x.ai/v1/chat/completions
grok.api.key=YOUR_GROK_API_KEY_HERE
```

### 3. Build and Run

```bash
# Build the project
mvn clean install

# Run the application
mvn spring-boot:run
```

The application will be available at `http://localhost:8080`

### 4. Initial Setup

1. Register a Teacher account:
   - Go to `/register`
   - Select "TEACHER" role
   - Complete registration

2. Register a Student account:
   - Go to `/register`
   - Select "STUDENT" role
   - Complete registration

3. Assign Student to Teacher:
   - This can be done through the database or by adding an admin interface
   - Update the `students` table to set the `mentor_id` (teacher_id)

## Project Structure

```
src/main/java/com/it/ai_mentoring_system/
├── config/              # Configuration classes
├── controller/          # REST and MVC controllers
├── model/               # Entity models
├── repository/          # JPA repositories
├── security/            # Security configuration
└── service/             # Business logic services
```

## API Endpoints

### Authentication
- `GET /login` - Login page
- `POST /login` - Process login
- `GET /register` - Registration page
- `POST /register` - Process registration
- `GET /dashboard` - Redirect based on role

### Student Endpoints
- `GET /student/dashboard` - Student dashboard
- `GET /student/quizzes` - View all quizzes
- `GET /student/quiz/generate` - Generate AI quiz form
- `POST /student/quiz/generate` - Generate AI quiz
- `GET /student/quiz/{id}/take` - Take quiz
- `POST /student/quiz/{id}/submit` - Submit quiz
- `GET /student/quiz-results` - View quiz results
- `GET /student/assignments` - View assignments
- `GET /student/assignment/{id}/submit` - Submit assignment form
- `POST /student/assignment/{id}/submit` - Submit assignment
- `GET /student/documents` - View documents
- `GET /student/document/upload` - Upload document form
- `POST /student/document/upload` - Upload document
- `GET /student/courses` - View enrolled courses
- `GET /student/notifications` - View notifications

### Teacher Endpoints
- `GET /teacher/dashboard` - Teacher dashboard
- `GET /teacher/students` - View students
- `GET /teacher/quizzes` - View quizzes
- `GET /teacher/quiz/create` - Create quiz form
- `POST /teacher/quiz/create` - Create quiz
- `GET /teacher/assignments` - View assignments
- `GET /teacher/assignment/create` - Create assignment form
- `POST /teacher/assignment/create` - Create assignment
- `GET /teacher/documents` - View documents
- `GET /teacher/document/request` - Request document form
- `POST /teacher/document/request` - Request document
- `GET /teacher/document/{id}/review` - Review document form
- `POST /teacher/document/{id}/review` - Review document
- `GET /teacher/notification/create` - Create notification form
- `POST /teacher/notification/create` - Create notification
- `GET /teacher/courses` - View courses
- `GET /teacher/course/create` - Create course form
- `POST /teacher/course/create` - Create course

## Database Schema

The system uses JPA with automatic schema generation. Key entities:

- **User**: Base user information with roles
- **Student**: Student profile linked to a mentor
- **Teacher**: Teacher profile
- **Course**: Courses with YouTube playlist links
- **Quiz**: Quizzes (AI-generated or manual)
- **QuizResult**: Quiz results with analysis
- **Assignment**: Assignments with deadlines
- **AssignmentSubmission**: Student submissions
- **Document**: Document uploads and reviews
- **Notification**: Notifications from teachers

## File Upload

Uploaded files are stored in the `uploads/` directory (configurable in `application.properties`). The directory is automatically created on startup.

## Security

- Password encryption using BCrypt
- Role-based access control (STUDENT/TEACHER)
- Session-based authentication
- CSRF protection (can be enabled)

## Notes

- Make sure to set your Grok API key in `application.properties`
- The upload directory will be created automatically
- Roles (STUDENT, TEACHER) are automatically initialized on first run
- Each student must be assigned to a teacher (mentor) to use the system fully

## Troubleshooting

1. **Database Connection Issues**: Verify PostgreSQL is running and credentials are correct
2. **Grok API Errors**: Check API key and network connectivity
3. **File Upload Issues**: Ensure the uploads directory has write permissions
4. **Role Assignment**: Students need to be assigned to a teacher via database or admin interface

## Future Enhancements

- Admin interface for user management
- Email notifications
- Real-time chat between student and teacher
- Advanced analytics and reporting
- Mobile app support

## License

This project is for educational purposes.




