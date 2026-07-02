# CI/CD them rieng, khong sua code hien co

Ban nay chi them cac file CI/CD moi. Cac file chuc nang san co cua project duoc giu nguyen, bao gom controller, service, template, database config, dang nhap, dat hang, dashboard va AI phan loai anh.

## 1. Cac file moi duoc them

```text
.github/maven-settings.xml
.github/workflows/ci.yml
.github/workflows/docker-image.yml
.github/workflows/deploy-render.yml
.github/workflows/deploy-vps.yml
Dockerfile
.dockerignore
docker-compose.prod.yml
.env.cicd.example
scripts/deploy-vps.sh
scripts/vps-install-docker.sh
CICD_ONLY_SETUP.md
```

Khong sua `application.properties`, `pom.xml`, controller, service, template hoac file nghiep vu nao cua bai.

## 2. CI build tu dong tren GitHub

Day code len GitHub:

```bash
git init
git add .
git commit -m "Add CI/CD files only"
git branch -M main
git remote add origin https://github.com/TEN_GITHUB/WebBanDoGiaDung.git
git push -u origin main
```

Vao tab `Actions` tren GitHub. Workflow `CI - Build Spring Boot` se tu chay va build Maven.

Project dang dung Spring Boot milestone nen CI dung file `.github/maven-settings.xml` de Maven lay dependency tu Spring Milestone Repository ma khong can sua `pom.xml`.

## 3. Build Docker image tu dong

Workflow `CI - Docker Image` se build Docker image va push len GitHub Container Registry:

```text
ghcr.io/TEN_GITHUB/WebBanDoGiaDung:latest
```

Neu repository private, vao GitHub package settings va cap quyen pull cho VPS hoac tao token `GHCR_TOKEN`.

## 4. CD len Render

Cach nay phu hop neu muon demo free.

### Buoc A: Tao Web Service tren Render

1. Vao Render.
2. `New` -> `Web Service`.
3. Chon repository GitHub cua project.
4. Chon Dockerfile neu Render hoi cach build.
5. Them Environment Variables can thiet.

Vi project goc dang bat Redis session, neu Render free khong co Redis, co the them ENV tren Render de demo nhe hon:

```text
SPRING_CACHE_TYPE=simple
SPRING_SESSION_STORE_TYPE=none
```

Cac bien hay can them:

```text
JWT_SECRET
JWT_EXPIRATION_MS
JWT_REFRESH_EXPIRATION_MS
APP_BASE_URL
GEMINI_API_KEY neu dung AI
MAIL_USERNAME / MAIL_PASSWORD neu dung gui mail
GOOGLE_CLIENT_ID / GOOGLE_CLIENT_SECRET neu dung Google login
```

### Buoc B: Lay Deploy Hook URL

Trong Render service:

```text
Settings -> Deploy Hook -> Copy URL
```

Trong GitHub repo:

```text
Settings -> Secrets and variables -> Actions -> New repository secret
```

Them secret:

```text
RENDER_DEPLOY_HOOK_URL=URL_DEPLOY_HOOK_CUA_RENDER
```

Sau do moi lan push len `main`, workflow `CD - Trigger Render Deploy` se goi Render deploy hook.

## 5. CD len VPS Ubuntu

Dung cach nay neu muon deploy dung kieu production hon.

### Buoc A: Cai Docker tren VPS

Copy script hoac chay thu cong tren VPS:

```bash
bash scripts/vps-install-docker.sh
```

### Buoc B: Tao thu muc app tren VPS

```bash
sudo mkdir -p /opt/webbandogiadung
sudo chown -R $USER:$USER /opt/webbandogiadung
cd /opt/webbandogiadung
```

Tao file `.env` tren VPS dua theo `.env.cicd.example`:

```bash
nano .env
```

Bat buoc nen co:

```text
APP_IMAGE=ghcr.io/TEN_GITHUB/WebBanDoGiaDung:latest
APP_PORT=8080
APP_BASE_URL=http://IP_VPS:8080
JWT_SECRET=chuoi_bi_mat_dai
JWT_EXPIRATION_MS=86400000
JWT_REFRESH_EXPIRATION_MS=604800000
```

Neu muon dung database rieng tren server/cloud thi them:

```text
SPRING_DATASOURCE_URL=jdbc:mysql://host:3306/database
SPRING_DATASOURCE_USERNAME=username
SPRING_DATASOURCE_PASSWORD=password
```

Neu de trong, app se dung cau hinh database goc trong `application.properties`.

### Buoc C: Them GitHub Secrets cho VPS

Trong GitHub repo:

```text
Settings -> Secrets and variables -> Actions -> New repository secret
```

Them:

```text
SERVER_HOST=IP_VPS
SERVER_USER=root
SERVER_PORT=22
SERVER_SSH_KEY=private_key_ssh
SERVER_APP_DIR=/opt/webbandogiadung
```

Neu image GHCR dang private, them:

```text
GHCR_USERNAME=ten_github
GHCR_TOKEN=personal_access_token_co_quyen_read:packages
```

Sau do push len `main`, workflow `CD - Deploy to VPS` se build image, copy file deploy va restart app bang Docker Compose.

## 6. Test CI/CD

### Test CI

Sua code bat ky, roi push:

```bash
git add .
git commit -m "test ci"
git push
```

Vao GitHub `Actions`, workflow CI phai mau xanh.

### Test CD Render

Them secret `RENDER_DEPLOY_HOOK_URL`, push len `main`, vao Render xem service dang redeploy.

### Test CD VPS

Them cac secret VPS, tao file `.env` tren VPS, push len `main`, sau do kiem tra:

```bash
cd /opt/webbandogiadung
docker compose -f docker-compose.prod.yml ps
docker compose -f docker-compose.prod.yml logs -f app
```

Mo web:

```text
http://IP_VPS:8080
```
