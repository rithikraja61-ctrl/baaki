# Deploying Baaki on Render

This guide explains how to deploy **Baaki** (Frontend + Backend) to [Render](https://render.com) using their free tiers.

---

## 1. Setup a Free MySQL Database

Render natively provides PostgreSQL, but Baaki uses MySQL. You can use any free MySQL cloud provider:

### Option A: Aiven (Recommended)
1. Sign up at [Aiven.io](https://aiven.io/).
2. Create a new service -> select **MySQL** (Free tier available).
3. Copy the **Service URI** or the host, port, user, and password.
   - Example JDBC format: `jdbc:mysql://<host>:<port>/defaultdb?sslmode=require`

### Option B: TiDB Cloud (Free Serverless)
1. Sign up at [TiDB Cloud](https://tidbcloud.com/).
2. Create a free **Serverless Cluster** (25 GB free storage, 100% MySQL compatible).
3. In the connection window, select **Java / JDBC** and copy the connection string.

---

## 2. Deploy Using Render Blueprint (Fastest)

1. Push your repository to **GitHub** or **GitLab**.
2. Go to the [Render Dashboard](https://dashboard.render.com/).
3. Click **New +** -> **Blueprint**.
4. Connect your Baaki repository.
5. Render will automatically detect `render.yaml` and configure both services:
   - `baaki-backend` (Docker Web Service)
   - `baaki-frontend` (Static Site)
6. Fill in the environment variables prompted by Render:
   - `SPRING_DATASOURCE_URL`: Your cloud MySQL JDBC URL
   - `SPRING_DATASOURCE_USERNAME`: Database username
   - `SPRING_DATASOURCE_PASSWORD`: Database password
   - `AI_API_KEY`: Your Gemini API Key
   - `VITE_API_BASE_URL`: `https://baaki-backend.onrender.com/api` (replace with your actual backend service name)
7. Click **Apply**.

---

## 3. Manual Deployment (Alternative)

If you prefer to configure services manually without blueprints:

### Backend (Web Service)
1. Click **New +** -> **Web Service**.
2. Select your repository.
3. Configure:
   - **Name**: `baaki-backend`
   - **Root Directory**: `backend`
   - **Runtime**: `Docker`
   - **Instance Type**: `Free`
4. Environment Variables:
   - `SPRING_DATASOURCE_URL`: `jdbc:mysql://<host>:<port>/<db>?useSSL=true`
   - `SPRING_DATASOURCE_USERNAME`: `<username>`
   - `SPRING_DATASOURCE_PASSWORD`: `<password>`
   - `AI_API_KEY`: `<your-gemini-key>`
   - `MAIL_PROVIDER`: `none`
   - `CORS_ALLOWED_ORIGINS`: `https://your-frontend.onrender.com`
   - `PUBLIC_BASE_URL`: `https://your-frontend.onrender.com`

### Frontend (Static Site)
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
5. Environment Variables:
   - `VITE_API_BASE_URL`: `https://baaki-backend.onrender.com/api`
