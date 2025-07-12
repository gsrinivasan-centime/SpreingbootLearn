# 🚀 Quick Setup Guide - Spring Boot Microservices

Welcome! This guide provides multiple setup options for the Spring Boot microservices application. Choose the one that best fits your needs.

## 📋 Prerequisites

Before you begin, ensure you have the following installed:

- **Docker Desktop** (v20.10 or higher)
- **Docker Compose** (v2.0 or higher) 
- **Git** (for cloning the repository)
- **Java 17** (optional - for local development)
- **Maven 3.8+** (optional - for building locally)

### 🔍 Verify Prerequisites

```bash
# Check Docker version
docker --version
# Should show: Docker version 20.10.x or higher

# Check Docker Compose version  
docker-compose --version
# Should show: Docker Compose version 2.x.x or higher

# Verify Docker is running
docker ps
# Should show empty list or running containers (no errors)
```

## ⚡ Setup Options

### 🐳 Option 1: Complete Docker Setup (Fastest - 60 seconds)

**Best for**: First-time users, demos, complete isolation

```bash
# 1. Clone the repository
git clone <your-repo-url>
cd SpreingbootLearn

# 2. Run complete Docker setup
chmod +x docker-setup.sh
./docker-setup.sh
```

**What this does:**
- ✅ Builds Docker images for both microservices
- ✅ Starts all infrastructure (MySQL, Redis, Kafka)
- ✅ Deploys microservices in containers
- ✅ Creates Kafka topics automatically
- ✅ Runs Liquibase migrations
- ✅ Shows service URLs and health status

### 🏭 Option 2: Production-Ready Setup (Enhanced)

**Best for**: Testing production-like configurations, performance evaluation

```bash
# Clone and setup
git clone <your-repo-url>
cd SpreingbootLearn

# Run production setup with monitoring
chmod +x production-setup.sh
./production-setup.sh
```

**Additional features:**
- 🔧 Resource limits and optimization
- 📊 Enhanced monitoring and metrics
- 🔐 Security configurations
- 💾 Performance tuning
- 📈 Health checks and readiness probes

### 🛠️ Option 3: Hybrid Development Setup

**Best for**: Active development, debugging, faster code changes

```bash
# 1. Start infrastructure only
chmod +x quick-start.sh
./quick-start.sh

# 2. In separate terminals, run microservices locally:
cd book-service && mvn spring-boot:run
cd user-service && mvn spring-boot:run
```

**Advantages:**
- 🚀 Fast development cycle
- 🐛 Easy debugging with IDE
- 🔄 Hot reload on code changes
- 💻 Direct access to logs and breakpoints

# 2. Start all services
./start-microservices.sh

# 3. Wait for services to be ready (about 2-3 minutes)
# The script will show progress and notify when ready
```

## 🏗️ What Gets Started

The setup will start the following services:

### 🔧 Infrastructure Services
- **MySQL Database** - `localhost:3308` (Books & Users data)
- **Redis Cache** - `localhost:6379` (Caching layer)
- **Apache Kafka** - `localhost:9092` (Message broker)
- **Zookeeper** - Internal (Kafka coordination)

### 🎯 Microservices
- **Book Service** - `http://localhost:8081/api/v1`
- **User Service** - `http://localhost:8082/api/v1`

### 📊 Management UIs
- **Kafka UI** - `http://localhost:8080` (Monitor Kafka topics)
- **Redis Commander** - `http://localhost:8090` (Monitor Redis cache)

### 📖 API Documentation
- **Book Service Swagger** - `http://localhost:8081/api/v1/swagger-ui.html`
- **User Service Swagger** - `http://localhost:8082/api/v1/swagger-ui.html`

## 🌐 Service URLs (After Setup)

Once any setup option completes successfully, you can access:

### 📚 Application Services
- **Book Service API**: http://localhost:8081/api/v1
- **User Service API**: http://localhost:8082/api/v1

### 📖 API Documentation
- **Book Service Swagger**: http://localhost:8081/api/v1/swagger-ui/index.html
- **User Service Swagger**: http://localhost:8082/api/v1/swagger-ui/index.html

### 🔧 Management Tools
- **Kafka UI**: http://localhost:8080 (admin/admin123 for production setup)
- **Redis Commander**: http://localhost:8090 (admin/admin123 for production setup)

### 🔍 Health & Monitoring
- **Book Service Health**: http://localhost:8081/api/v1/actuator/health
- **User Service Health**: http://localhost:8082/api/v1/actuator/health
- **Book Service Metrics**: http://localhost:8081/api/v1/actuator/metrics
- **User Service Metrics**: http://localhost:8082/api/v1/actuator/metrics

## ✅ Verify Setup

### 1. Check All Services Are Running
```bash
./status-check.sh
```

### 2. Test API Endpoints
```bash
# Check service health
curl http://localhost:8081/api/v1/actuator/health
curl http://localhost:8082/api/v1/actuator/health

# Test APIs (will return 401 - authentication required, which is expected)
curl http://localhost:8081/api/v1/books
curl http://localhost:8082/api/v1/users
```

### 3. Access Management UIs
- Open `http://localhost:8080` for Kafka UI
- Open `http://localhost:8090` for Redis Commander

## 🛠️ Management Commands

### Status Checking
```bash
# Complete status report
./status-check.sh

# Health checks only
./status-check.sh health

# Container status
docker-compose ps
```

### Service Management
```bash
# View logs for all services
docker-compose logs -f

# View logs for specific service
docker-compose logs -f book-service

# Restart a service
docker-compose restart book-service

# Stop all services
docker-compose down

# Full cleanup (removes all data)
docker-compose down -v
```

### Development Workflow
```bash
# Rebuild service after code changes
docker-compose build book-service
docker-compose up -d book-service

# Scale services (production setup)
docker-compose up -d --scale book-service=2

# Monitor resource usage
docker stats
```

## 🔧 Troubleshooting

### Common Issues

#### ❌ Port Already in Use
```bash
# Check what's using the port
lsof -i :8081
lsof -i :8082

# Kill the process or change ports in docker-compose.yml
```

#### ❌ Services Not Starting
```bash
# Check service logs
docker-compose logs book-service

# Check health status
curl http://localhost:8081/api/v1/actuator/health

# Restart with fresh data
docker-compose down -v
./docker-setup.sh
```

#### ❌ Database Connection Issues
```bash
# Check MySQL status
docker exec bookstore-mysql mysqladmin ping

# Verify database exists
docker exec bookstore-mysql mysql -u bookstore_user -pbookstore_pass -e "SHOW DATABASES;"

# Check Liquibase migrations
docker exec bookstore-mysql mysql -u bookstore_user -pbookstore_pass bookstore_users -e "SELECT * FROM DATABASECHANGELOG;"
```

#### ❌ Kafka Connection Issues
```bash
# Check Kafka topics
docker exec bookstore-kafka kafka-topics --bootstrap-server localhost:9092 --list

# Check if Kafka is responding
docker exec bookstore-kafka kafka-broker-api-versions --bootstrap-server localhost:9092
```

### Performance Issues

#### 🐌 Slow Startup
- Increase Docker memory to 4GB+
- Close other applications
- Use SSD for Docker storage

#### 💾 Memory Issues
```bash
# Check memory usage
docker stats

# Restart with memory limits
docker-compose down
docker-compose up -d
```

### Getting Help

1. **Check logs first**: `docker-compose logs -f [service-name]`
2. **Verify health**: `./status-check.sh health`
3. **Fresh start**: `docker-compose down -v && ./docker-setup.sh`
4. **Check documentation**: See `docs/` folder for detailed guides

## 🗂️ Project Structure

```
SpreingbootLearn/
├── book-service/          # Book management microservice
├── user-service/          # User management microservice
├── scripts/              # Database initialization scripts
├── config/               # Configuration files
├── k8s/                  # Kubernetes deployment files
├── docs/                 # Documentation
├── docker-compose.yml    # Main Docker Compose file
├── quick-setup.sh        # Automated setup script
├── start-microservices.sh # Start all services
├── status-check.sh       # Check service status
└── README.md            # Main project documentation
```

## 📚 Next Steps

1. **Explore APIs**: Use Swagger UI to test endpoints
2. **Check Data**: Use Redis Commander and database tools
3. **Monitor Messages**: Use Kafka UI to see message flow
4. **Read Documentation**: Check the `docs/` folder for detailed guides
5. **Development**: See individual service README files for development setup

## 🎯 Success Indicators

You'll know everything is working when:

- ✅ All containers show as "healthy" in `docker-compose ps`
- ✅ Both service health endpoints return HTTP 200
- ✅ Swagger UIs are accessible
- ✅ Kafka UI shows topics
- ✅ Redis Commander shows connections
- ✅ No error logs in service outputs

## 📚 Learning Resources

- **Architecture Guide**: [docs/01-SpringBoot.md](docs/01-SpringBoot.md)
- **Database Design**: [docs/03-Database.md](docs/03-Database.md)
- **Containerization**: [docs/11-Containerization.md](docs/11-Containerization.md)
- **Testing Strategies**: [docs/09-Testing.md](docs/09-Testing.md)

---

🎉 **Congratulations!** Your Spring Boot microservices environment is ready for development and learning!
