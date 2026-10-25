# Deploying Baaki on Render (with Free Managed PostgreSQL)

This guide explains how to deploy **Baaki** (Frontend, Backend, and Database) to [Render](https://render.com) using their **100% Free** tier.

---

## Architecture Overview

- **Database**: Render Managed PostgreSQL (Free)
- **Backend**: Render Web Service running Spring Boot 3 + Java 21 with `SPRING_PROFILES_ACTIVE=postgres` (Free)
- **Frontend**: Render Static Site running Vite + React SPA (Free)

*(Note: Local development with `docker-compose` continues to run with MySQL as default, while Render runs with PostgreSQL via the `postgres` Spring profile!)*

---

## Method 1: 1-Click Blueprint Deploy (Recommended)

Render Blueprints automatically creates the PostgreSQL database, backend service, and frontend static site all together.

1. **Push your code to GitHub**:
   ```bash
   git add .
   git commit -m "Add PostgreSQL profile and Render blueprint"
   git push origin main
   ```

2. **Open Render**:
   - Go to [dashboard.render.com](https://dashboard.render.com/).
   - Click **New +** -> **Blueprint**.
   - Select your `baaki` repository.

3. **Render will auto-configure**:
   - Database: `baaki-db` (PostgreSQL)
   - Backend: `baaki-backend` (Docker container with `postgres` profile)
   - Frontend: `baaki-frontend` (Static Site with SPA rewrites)

4. **Variables to fill in**:
   - `AI_API_KEY`: Your Google Gemini API key (for invoice scanning).
   - `VITE_API_BASE_URL`: `https://baaki-backend.onrender.com/api` (replace with your actual backend name).

5. Click **Apply**! Render will deploy all 3 services automatically.

---

## Method 2: Manual Setup (Step-by-Step)

If you prefer setting up services individually:

### Step 1: Create PostgreSQL Database
1. In Render, click **New +** -> **PostgreSQL**.
2. Name: `baaki-db`, Database: `baaki`, User: `baaki`.
3. Instance Type: **Free**.
4. Click **Create Database**.
5. Once created, copy the **Internal Database URL** (e.g., `postgres://baaki:...@dpg-...:5432/baaki`).

### Step 2: Deploy Backend Web Service
1. Click **New +** -> **Web Service**.
2. Select your repository.
3. Configure:
   - **Name**: `baaki-backend`
   - **Root Directory**: `backend`
   - **Runtime**: `Docker`
   - **Instance Type**: `Free`
4. In **Environment Variables**:
   - `SPRING_PROFILES_ACTIVE`: `postgres`
   - `DATABASE_URL`: *(paste the Internal Database URL from Step 1)*
   - `AI_API_KEY`: *(your Gemini key)*
   - `MAIL_PROVIDER`: `none`
   - `CORS_ALLOWED_ORIGINS`: `https://baaki-frontend.onrender.com`
   - `PUBLIC_BASE_URL`: `https://baaki-frontend.onrender.com`

### Step 3: Deploy Frontend Static Site
1. Click **New +** -> **Static Site**.
2. Select your repository.
3. Configure:
   - **Name**: `baaki-frontend`
   - **Root Directory**: `frontend`
   - **Build Command**: `npm install && npm run build`
   - **Publish Directory**: `dist`
4. Under **Redirects / Rewrites**, click **Add Rule**:
   - **Type**: `Rewrite`
   - **Source**: `/*`
   - **Destination**: `/index.html`
5. In **Environment Variables**:
   - `VITE_API_BASE_URL`: `https://baaki-backend.onrender.com/api`
