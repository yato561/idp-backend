# Prometheus & Grafana Integration Guide

## Overview

Your IDP Backend application now supports integration with **Prometheus** and **Grafana** for metrics collection and visualization. Instead of manually ingesting metrics, you can now automatically pull metrics from Prometheus.

## Architecture

```
Services → Prometheus (scrapes metrics)
                ↓
    IDP Backend (pulls via PrometheusClient)
                ↓
    Database (stores metrics)
                ↓
    Grafana (visualizes from DB)
```

## Components Added

### 1. PrometheusClient (`util/PrometheusClient.java`)
- Connects to Prometheus server
- Executes PromQL queries
- Extracts metrics for CPU, Memory, and service health
- Supports both instant and range queries

### 2. HttpClientConfig (`config/HttpClientConfig.java`)
- Provides RestClient bean for HTTP communication

### 3. Updated MetricController Endpoints
- **`POST /api/metrics/prometheus/sync/{serviceId}`** - Sync metrics for one service
  ```bash
  curl -X POST http://localhost:8081/api/metrics/prometheus/sync/{serviceId}?serviceName=myservice \
    -H "Authorization: Bearer <admin_token>"
  ```

- **`POST /api/metrics/prometheus/pull-all`** - Bulk sync all services
  ```bash
  curl -X POST http://localhost:8081/api/metrics/prometheus/pull-all \
    -H "Authorization: Bearer <admin_token>"
  ```

- **`GET /api/metrics/prometheus/query`** - Execute custom PromQL
  ```bash
  curl "http://localhost:8081/api/metrics/prometheus/query?query=up{job='myservice'}" \
    -H "Authorization: Bearer <admin_token>"
  ```

### 4. Exposed Prometheus Metrics Endpoint
- **`GET /actuator/prometheus`** - Application metrics in Prometheus format
  ```bash
  curl http://localhost:8081/actuator/prometheus
  ```

## Setup Instructions

### Step 1: Configure Prometheus Connection

Update `application.properties`:

```properties
# Prometheus Server URL
prometheus.url=http://localhost:9090
prometheus.query-interval=30s
```

### Step 2: Install & Configure Prometheus

**Using Docker Compose:**

```yaml
version: '3.8'
services:
  prometheus:
    image: prom/prometheus:latest
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus_data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'

volumes:
  prometheus_data:
```

**prometheus.yml Configuration:**

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'idp-backend'
    static_configs:
      - targets: ['localhost:8081']
    metrics_path: '/actuator/prometheus'

  - job_name: 'kubernetes-pods'  # If running in K8s
    kubernetes_sd_configs:
      - role: pod
    relabel_configs:
      - source_labels: [__meta_kubernetes_pod_label_app]
        action: keep
        regex: myservice.*
```

### Step 3: Install & Configure Grafana

**Using Docker Compose:**

```yaml
services:
  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
      - GF_INSTALL_PLUGINS=grafana-piechart-panel
    volumes:
      - grafana_data:/var/lib/grafana
      - ./grafana/dashboards:/etc/grafana/provisioning/dashboards
      - ./grafana/datasources:/etc/grafana/provisioning/datasources
    depends_on:
      - prometheus

volumes:
  grafana_data:
```

**Grafana Datasource Configuration (datasources.yml):**

```yaml
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
    editable: true
```

### Step 4: Start Services

```bash
# Start Prometheus and Grafana
docker-compose up -d prometheus grafana

# Access URLs
# Prometheus: http://localhost:9090
# Grafana: http://localhost:3000 (admin/admin)
```

## Usage Workflows

### Workflow 1: Manual Sync
Sync metrics from Prometheus to your database on demand:

```bash
curl -X POST http://localhost:8081/api/metrics/prometheus/sync/{serviceId}?serviceName=myservice \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### Workflow 2: Scheduled Sync
Use a cron job or scheduler to periodically pull from Prometheus:

```bash
# Every 5 minutes
*/5 * * * * curl -X POST http://localhost:8081/api/metrics/prometheus/pull-all \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### Workflow 3: Continuous Integration
Integrate with your deployment pipeline:

```bash
#!/bin/bash
SERVICE_ID=$(curl -s http://localhost:8081/api/services | jq -r '.[0].id')
curl -X POST http://localhost:8081/api/metrics/prometheus/sync/$SERVICE_ID?serviceName=myservice \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

## Prometheus PromQL Queries

### CPU Usage
```promql
# Average CPU usage percentage for a service (5-minute window)
avg(rate(container_cpu_usage_seconds_total{pod=~"myservice.*"}[5m])) * 100
```

### Memory Usage
```promql
# Memory usage in MB
avg(container_memory_usage_bytes{pod=~"myservice.*"}) / 1024 / 1024
```

### Service Health
```promql
# Check if service is up (1 = up, 0 = down)
up{job="myservice"}
```

### Custom Metrics
```promql
# HTTP request rate
rate(http_requests_total[5m])

# Error rate
rate(http_requests_total{status=~"5.."}[5m])

# Response time
histogram_quantile(0.95, http_request_duration_seconds)
```

## Creating Grafana Dashboards

### Step 1: Add Prometheus Data Source
1. Go to Configuration → Data Sources
2. Click "Add data source"
3. Select "Prometheus"
4. Set URL: `http://prometheus:9090`

### Step 2: Create Dashboard
1. Create new dashboard
2. Add panels with PromQL queries
3. Examples:

**CPU Usage Panel:**
```promql
avg(rate(container_cpu_usage_seconds_total{pod=~"$service.*"}[5m])) * 100
```

**Memory Usage Panel:**
```promql
avg(container_memory_usage_bytes{pod=~"$service.*"}) / 1024 / 1024
```

**Alert Status Panel:**
```promql
count(alerts{status="OPEN"})
```

## Alert Integration

The alert system automatically evaluates metrics:
- **CPU > 80%** → `CPU_HIGH` alert
- **Memory > 1024MB** → `MEMORY_HIGH` alert  
- **Service Down** → `SERVICE_DOWN` alert

When you pull metrics from Prometheus, the alert engine will automatically trigger based on these thresholds.

## Troubleshooting

### Prometheus Connection Failed
```bash
# Test connectivity
curl -s http://localhost:9090/api/v1/query?query=up | jq

# Check logs
docker logs prometheus
```

### No Metrics in Database
- Verify Prometheus is scraping: `http://localhost:9090/targets`
- Check service name matches in PromQL queries
- Review application logs: `docker logs backend`

### Alert Not Triggering
- Verify metrics are being ingested
- Check alert thresholds in `AlertRuleEngine.java`
- Run manual evaluation: `POST /api/alerts/evaluate/{serviceId}`

## Performance Considerations

1. **Query Frequency**: Adjust `prometheus.query-interval` in properties
2. **Retention**: Configure Prometheus retention period (default: 15d)
3. **Scrape Interval**: Balance between accuracy and load
4. **Database Cleanup**: Archive old metrics regularly

## Next Steps

1. ✅ Set up Prometheus with service targets
2. ✅ Configure Prometheus scrape endpoints
3. ✅ Deploy Grafana with dashboards
4. ✅ Set up scheduled metric pulls
5. ✅ Create alerting rules in Grafana
6. ✅ Monitor alert notifications

## API Reference

| Endpoint | Method | Role | Purpose |
|----------|--------|------|---------|
| `/api/metrics/prometheus/sync/{serviceId}` | POST | ADMIN | Sync one service |
| `/api/metrics/prometheus/pull-all` | POST | ADMIN | Sync all services |
| `/api/metrics/prometheus/query` | GET | ADMIN | Custom PromQL |
| `/actuator/prometheus` | GET | PUBLIC | App metrics |

---

**Note**: Replace `localhost` with your actual server addresses in production environments.
