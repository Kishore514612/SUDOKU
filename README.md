# ✦ SUDOKU — Deep Space Edition ✦

A full-stack Sudoku game with a cosmic space theme, built with **React + TypeScript** (frontend) and **Spring Boot + Java** (backend).

---

## Prerequisites

| Tool       | Version  |
|------------|----------|
| Java JDK   | 21+      |
| Maven      | 3.9+     |
| Node.js    | 20+      |
| npm        | 10+      |

---

## Project Structure

```
WP/
├── backend/          # Spring Boot REST API + H2 database
│   ├── src/
│   └── pom.xml
├── frontend/         # React + Vite + Tailwind CSS
│   ├── src/
│   └── package.json
└── README.md
```

---

## Quick Start

### 1. Start the Backend (Spring Boot)

```bash
cd backend
mvn spring-boot:run
```

The backend starts at **http://localhost:8080**.

> H2 Console is available at http://localhost:8080/h2-console  
> JDBC URL: `jdbc:h2:file:./data/sudokudb` | Username: `sa` | Password: *(empty)*

### 2. Start the Frontend (React + Vite)

Open a **second terminal**:

```bash
cd frontend
npm install
npm run dev
```

The frontend starts at **http://localhost:5173**.

---

## Play the Game

Open **http://localhost:5173** in your browser.

1. Select a difficulty (Easy / Medium / Hard)
2. Click **START NEW MISSION**
3. Click cells and use the number pad or keyboard (`1`–`9`) to fill in numbers
4. Use `Backspace` / `Delete` to erase, arrow keys to navigate
5. Undo (`Ctrl+Z`) and Redo (`Ctrl+Y`) are supported
6. Pause/Resume via the header icon
7. Click **Submit Solution** when finished

If you leave and come back, click **Resume** on the landing page to continue where you left off.

---

## REST API Endpoints

| Method | Endpoint                        | Description                  |
|--------|---------------------------------|------------------------------|
| POST   | `/api/games`                    | Create a new game            |
| GET    | `/api/games/{id}`               | Get game state               |
| GET    | `/api/games/active`             | Get latest unfinished game   |
| POST   | `/api/games/{id}/move`          | Submit a move                |
| POST   | `/api/games/{id}/undo`          | Undo last move               |
| POST   | `/api/games/{id}/redo`          | Redo undone move             |
| POST   | `/api/games/{id}/pause`         | Pause the game               |
| POST   | `/api/games/{id}/resume`        | Resume the game              |
| POST   | `/api/games/{id}/submit`        | Validate completed board     |

---

## Build for Production

### Frontend

```bash
cd frontend
npm run build
```

Output goes to `frontend/dist/`.

### Backend

```bash
cd backend
mvn clean package -DskipTests
java -jar target/sudoku-game-engine-1.0.0.jar
```

---

## Tech Stack

- **Frontend**: React 19, TypeScript, Vite, Tailwind CSS v4, Lucide Icons
- **Backend**: Java 21, Spring Boot 3.3, Spring Data JPA, H2 Database
- **Puzzle Generation**: Randomized backtracking solver with difficulty-based cell removal
