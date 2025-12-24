# Pragmite Docker Guide

This guide explains how to build and run Pragmite using Docker.

## Prerequisites

- Docker 20.10+
- Docker Compose 1.29+ (optional, for docker-compose)

## Quick Start

### Option 1: Using Docker directly

```bash
# Build the image
cd pragmite-core
docker build -t pragmite:latest .

# Run analysis on a project
docker run -v /path/to/your/project:/pragmite/projects \
           -v $(pwd)/reports:/pragmite/reports \
           pragmite:latest /pragmite/projects -o /pragmite/reports/report.json
```

### Option 2: Using Docker Compose

```bash
# Build and run default analysis (test-ecommerce)
docker-compose up pragmite

# Analyze a custom project
PROJECT_PATH=/path/to/your/project docker-compose --profile custom up pragmite-custom
```

## Docker Image Details

### Multi-Stage Build

The Dockerfile uses a multi-stage build for optimal image size:

1. **Builder Stage**: Uses `maven:3.9-eclipse-temurin-21-alpine` to compile the application
2. **Runtime Stage**: Uses `eclipse-temurin:21-jre-alpine` for a minimal runtime image

### Image Size

- Builder image: ~500 MB
- Final runtime image: ~250 MB

### Volumes

The container exposes four volumes:

| Volume | Purpose | Example |
|--------|---------|---------|
| `/pragmite/config` | Configuration files (.pragmite.yaml) | `-v $(pwd)/.pragmite.yaml:/pragmite/config/.pragmite.yaml:ro` |
| `/pragmite/projects` | Source code to analyze | `-v /path/to/project:/pragmite/projects:ro` |
| `/pragmite/reports` | Analysis output reports | `-v $(pwd)/reports:/pragmite/reports` |
| `/pragmite/logs` | Application logs | `-v $(pwd)/logs:/pragmite/logs` |

## Examples

### 1. Basic Analysis

```bash
docker run -v $(pwd)/test-ecommerce:/pragmite/projects \
           pragmite:latest /pragmite/projects
```

### 2. Generate JSON Report

```bash
docker run -v $(pwd)/test-ecommerce:/pragmite/projects \
           -v $(pwd)/reports:/pragmite/reports \
           pragmite:latest /pragmite/projects \
           -o /pragmite/reports/analysis.json \
           -f json
```

### 3. With Custom Configuration

```bash
docker run -v $(pwd)/test-ecommerce:/pragmite/projects \
           -v $(pwd)/reports:/pragmite/reports \
           -v $(pwd)/.pragmite.yaml:/pragmite/config/.pragmite.yaml:ro \
           pragmite:latest /pragmite/projects \
           -o /pragmite/reports/report.json \
           -f both
```

### 4. Verbose Output

```bash
docker run -v $(pwd)/test-ecommerce:/pragmite/projects \
           pragmite:latest /pragmite/projects --verbose
```

### 5. Custom Memory Settings

```bash
docker run -e JAVA_OPTS="-Xmx4g -Xms1g" \
           -v $(pwd)/my-project:/pragmite/projects \
           pragmite:latest /pragmite/projects
```

## Docker Compose Configuration

The `docker-compose.yml` includes two services:

### Service 1: pragmite (default)

Analyzes the `test-ecommerce` project with default settings.

```bash
docker-compose up pragmite
```

### Service 2: pragmite-custom

Analyzes a custom project specified via `PROJECT_PATH` environment variable.

```bash
PROJECT_PATH=/my/project docker-compose --profile custom up pragmite-custom
```

## Building for Production

### Optimizations

1. **Layer Caching**: The Dockerfile copies `pom.xml` first to cache dependencies
2. **Dependency Pre-download**: `mvn dependency:go-offline` speeds up subsequent builds
3. **Skip Tests**: Production builds skip tests with `-DskipTests`

### Build Command

```bash
docker build --no-cache -t pragmite:1.0.0 .
```

### Tagging

```bash
docker tag pragmite:1.0.0 myregistry/pragmite:1.0.0
docker tag pragmite:1.0.0 myregistry/pragmite:latest
```

### Push to Registry

```bash
docker push myregistry/pragmite:1.0.0
docker push myregistry/pragmite:latest
```

## Troubleshooting

### Issue: Container exits immediately

**Solution**: Check that you're providing a valid project path and command.

```bash
# Show help to verify container works
docker run pragmite:latest --help
```

### Issue: Permission denied on reports

**Solution**: Ensure the reports directory is writable by the container.

```bash
chmod 777 $(pwd)/reports
```

### Issue: Out of memory

**Solution**: Increase Java heap size.

```bash
docker run -e JAVA_OPTS="-Xmx8g -Xms2g" ...
```

### Issue: Build fails with "Connection refused"

**Solution**: Check Docker network and Maven repository access.

```bash
docker build --network=host -t pragmite:latest .
```

## CI/CD Integration

### GitHub Actions

```yaml
- name: Build Docker Image
  run: |
    cd pragmite-core
    docker build -t pragmite:${{ github.sha }} .

- name: Run Analysis
  run: |
    docker run -v $(pwd)/my-project:/pragmite/projects \
               -v $(pwd)/reports:/pragmite/reports \
               pragmite:${{ github.sha }} /pragmite/projects \
               -o /pragmite/reports/report.json
```

### Jenkins

```groovy
stage('Docker Build') {
    steps {
        sh 'cd pragmite-core && docker build -t pragmite:${BUILD_NUMBER} .'
    }
}

stage('Analyze') {
    steps {
        sh '''
            docker run -v ${WORKSPACE}/project:/pragmite/projects \
                       -v ${WORKSPACE}/reports:/pragmite/reports \
                       pragmite:${BUILD_NUMBER} /pragmite/projects
        '''
    }
}
```

## Best Practices

1. **Read-Only Mounts**: Mount source code as read-only (`:ro`) to prevent accidental modifications
2. **Resource Limits**: Set memory and CPU limits for large projects
3. **Volume Persistence**: Use named volumes for logs and caches
4. **Network Isolation**: Run in isolated networks for security
5. **Health Checks**: Implement health checks for production deployments

## Advanced Configuration

### Custom Entrypoint

```bash
docker run --entrypoint /bin/sh pragmite:latest -c "java -version && pragmite --help"
```

### Interactive Mode

```bash
docker run -it --entrypoint /bin/bash pragmite:latest
```

### Compose Override

Create `docker-compose.override.yml`:

```yaml
version: '3.8'

services:
  pragmite:
    environment:
      - JAVA_OPTS=-Xmx8g -Xms2g
    volumes:
      - ./my-custom-config.yaml:/pragmite/config/.pragmite.yaml:ro
```

## Support

For issues or questions about Docker deployment:
- GitHub Issues: https://github.com/pragmite/pragmite/issues
- Documentation: See main README.md
