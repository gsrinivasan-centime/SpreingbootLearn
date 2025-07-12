# 🎯 Getting Started - Spring Boot Microservices

**One-minute setup for a complete microservices environment!**

## 🚀 Instant Setup

```bash
# 1. Clone the repository
git clone <your-repo-url>
cd SpreingbootLearn

# 2. Choose your setup method
./setup-menu.sh
```

That's it! The interactive menu will guide you through the setup process.

## 📋 Prerequisites

- **Docker Desktop** (running)
- **Git**
- **4GB+ RAM** recommended

## ⚡ Quick Options

If you prefer direct commands:

### 🐳 Complete Docker Setup (Recommended)
```bash
./docker-setup.sh
```
**Result**: Everything running in containers in ~60 seconds

### 🏭 Production Setup  
```bash
./production-setup.sh
```
**Result**: Production-ready environment with monitoring

### 🛠️ Development Setup
```bash
./quick-start.sh  # Infrastructure only
# Then run microservices locally with Java/Maven
```

## 🌐 After Setup

| Service | URL | Description |
|---------|-----|-------------|
| 📚 Book Service | http://localhost:8081/api/v1 | Books, inventory, categories |
| 👥 User Service | http://localhost:8082/api/v1 | Users, auth, orders |
| 📊 Kafka UI | http://localhost:8080 | Message monitoring |
| 🗄️ Redis Commander | http://localhost:8090 | Cache management |

## 🔧 Management

```bash
# Check status
./status-check.sh

# View logs
docker-compose logs -f [service-name]

# Stop everything
docker-compose down

# Complete cleanup
docker-compose down -v
```

## 📚 Learn More

- **[QUICK_SETUP.md](QUICK_SETUP.md)** - Detailed setup guide
- **[README.md](README.md)** - Full project documentation  
- **[docs/](docs/)** - Architecture and learning guides

## 🆘 Troubleshooting

1. **Services not starting?** → `docker-compose logs -f`
2. **Port conflicts?** → Change ports in `docker-compose.yml`
3. **Need fresh start?** → `docker-compose down -v && ./docker-setup.sh`

---

🎉 **Ready to explore microservices architecture!** Use the Swagger UIs to test the APIs and dive into the code.
