#!/bin/bash

# Kubernetes Deployment Script for Bookstore Microservices
# This script demonstrates how to deploy the containerized microservices to Kubernetes

set -e

NAMESPACE="bookstore"
K8S_DIR="k8s"

echo "🚀 Deploying Bookstore Microservices to Kubernetes"
echo "=================================================="

# Check if kubectl is available
if ! command -v kubectl &> /dev/null; then
    echo "❌ kubectl is not installed or not in PATH"
    echo "Please install kubectl to deploy to Kubernetes"
    exit 1
fi

# Check if we can connect to a Kubernetes cluster
if ! kubectl cluster-info &> /dev/null; then
    echo "❌ Cannot connect to Kubernetes cluster"
    echo "Please ensure you have access to a Kubernetes cluster"
    echo "For local development, consider using:"
    echo "  - minikube start"
    echo "  - kind create cluster"
    echo "  - Docker Desktop Kubernetes"
    exit 1
fi

echo "✅ Connected to Kubernetes cluster"
kubectl cluster-info

# Create namespace if it doesn't exist
echo ""
echo "📦 Creating namespace: $NAMESPACE"
kubectl create namespace $NAMESPACE --dry-run=client -o yaml | kubectl apply -f -

# Deploy infrastructure services first
echo ""
echo "🏗️  Deploying infrastructure services..."
kubectl apply -f $K8S_DIR/infrastructure.yaml -n $NAMESPACE

# Wait for infrastructure to be ready
echo ""
echo "⏳ Waiting for infrastructure services to be ready..."
echo "Waiting for MySQL..."
kubectl wait --for=condition=ready pod -l app=mysql -n $NAMESPACE --timeout=300s

echo "Waiting for Redis..."
kubectl wait --for=condition=ready pod -l app=redis -n $NAMESPACE --timeout=300s

echo "Waiting for Zookeeper..."
kubectl wait --for=condition=ready pod -l app=zookeeper -n $NAMESPACE --timeout=300s

echo "Waiting for Kafka..."
kubectl wait --for=condition=ready pod -l app=kafka -n $NAMESPACE --timeout=300s

# Deploy microservices
echo ""
echo "🔧 Deploying microservices..."
kubectl apply -f $K8S_DIR/bookstore-deployment.yaml -n $NAMESPACE

# Wait for microservices to be ready
echo ""
echo "⏳ Waiting for microservices to be ready..."
kubectl wait --for=condition=ready pod -l app=bookstore -n $NAMESPACE --timeout=300s

# Show deployment status
echo ""
echo "📊 Deployment Status"
echo "==================="
kubectl get pods -n $NAMESPACE
echo ""
kubectl get services -n $NAMESPACE

# Show application URLs
echo ""
echo "🌐 Application Access"
echo "===================="
echo "To access the services, you can use port-forwarding:"
echo ""
echo "Book Service:"
echo "  kubectl port-forward -n $NAMESPACE service/bookstore-service 8081:8081"
echo "  Then visit: http://localhost:8081/api/v1/actuator/health"
echo ""
echo "User Service:"
echo "  kubectl port-forward -n $NAMESPACE service/bookstore-service 8082:8082"
echo "  Then visit: http://localhost:8082/api/v1/actuator/health"
echo ""

# Show logs command
echo "📋 Useful Commands"
echo "=================="
echo "View pod logs:"
echo "  kubectl logs -n $NAMESPACE -l app=bookstore -c book-service"
echo "  kubectl logs -n $NAMESPACE -l app=bookstore -c user-service"
echo ""
echo "Scale deployment:"
echo "  kubectl scale deployment bookstore-microservices --replicas=3 -n $NAMESPACE"
echo ""
echo "Delete deployment:"
echo "  kubectl delete namespace $NAMESPACE"
echo ""

echo "✅ Deployment completed successfully!"
echo ""
echo "💡 Benefits of this Pod-based deployment:"
echo "   - Both services share the same network namespace"
echo "   - They can communicate via localhost (http://localhost:8081, http://localhost:8082)"
echo "   - Shared lifecycle management"
echo "   - Resource efficiency"
echo "   - Simplified service discovery within the pod"
