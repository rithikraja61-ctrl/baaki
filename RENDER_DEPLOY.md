# Deploying Baaki on Render (Single All-in-One Docker Container)

Baaki is configured to run as a **single, unified Docker container** that bundles both the React frontend and Spring Boot backend together, connected to Render's **Free Managed PostgreSQL** database.

---

## Why Single Container on Render?
- **100% Free**: Fits perfectly within Render's free tier (1 free Web Service + 1 free PostgreSQL database).
- **Same Origin / No CORS**: Frontend and API run on the exact same domain (`https://your-app.onrender.com`), eliminating CORS headaches and network latency.
- **Single URL**: One clean URL for your dashboard, customer invoice views, pay links, and API.

---

## Method 1: 1-Click Blueprint Deploy (Recommended)

1. **Commit and push** to your GitHub repository:
   ```bash
   git add .
   git commit -m "Configure all-in-one Docker deployment for Render"
   git push origin main
   ```

2. **Open Render**:
   - Go to [dashboard.render.com](https://dashboard.render.com/).
   - Click **New +** -> **Blueprint**.
   - Connect your **`baaki`** repository.

3. **Render will auto-detect `render.yaml`**:
   - Creates the **`baaki-db`** (PostgreSQL)
   - Creates the **`baaki`** Web Service (Unified Docker Container)
   - Automatically connects them via `DATABASE_URL`.

4. **Variables to enter**:
   - `AI_API_KEY`: Your Google Gemini API key (for invoice photo/PDF scanning).

5. Click **Apply**. Render will build the React frontend, package the Spring Boot backend, run Flyway migrations, and launch your application!

---

## Method 2: Manual Web Service Setup (Alternative)

If setting up without blueprints:

### 1. Create Free PostgreSQL Database
1. In Render, click **New +** -> **PostgreSQL**.
2. Name: `baaki-db`, Database: `baaki`, User: `baaki`.
3. Plan: **Free**.
4. Click **Create Database**, then copy the **Internal Database URL**.

### 2. Create All-in-One Web Service
1. Click **New +** -> **Web Service**.
2. Select your repository.
3. Configure:
   - **Name**: `baaki`
   - **Runtime**: `Docker`
   - **Dockerfile Path**: `./Dockerfile`
   - **Plan**: `Free`
4. Environment Variables:
   - `SPRING_PROFILES_ACTIVE`: `postgres`
   - `DATABASE_URL`: *(paste the Internal Database URL from Step 1)*
   - `AI_API_KEY`: *(your Gemini API key)*
   - `MAIL_PROVIDER`: `none`
   - `PUBLIC_BASE_URL`: `https://baaki.onrender.com` (replace with your Render web service URL)
