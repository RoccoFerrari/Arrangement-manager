# Arrangement Manager

A native Android application designed to streamline order management in a restaurant. The app is divided into two main modules: one for waiters and one for the kitchen, which communicate with each other via a local Wi-Fi network.

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![GitHub stars](https://img.shields.io/github/stars/RoccoFerrari/Arrangement-manager.svg)](https://github.com/RoccoFerrari/Arrangement-manager/stargazers)
[Download the latest version](Arrangement-Manager.apk)

### Main features

- **Wireless Order Management**: Waiters can send orders directly to the kitchen via the local network, eliminating the need for paper orders.

- **Menu Management**: Users can add, edit, and manage the restaurant menu via the app.

- **Real-Time Notifications**: The kitchen receives orders instantly and can notify waiters when a dish or an entire order is ready.

- **Intuitive User Interface**: A clear and simple interface designed for use in a fast-paced work environment.

- **Login/Registration System**: Users can authenticate and manage their sessions.

### Project Architecture

The project follows the **MVVM (Model-View-ViewModel)** pattern for a clear separation of responsibilities and easy maintainability.

- **View**: Composed of `Fragments` and `Activities`, which are responsible for displaying the user interface.

- **ViewModel**: The `ViewModel` classes contain the UI's business logic, managing state and data.

- **Model**: The data layer, which includes:
    - **Data Classes**: (`User`, `Order`, `MenuItem`, etc.) which define the data structure.
    - **Repository / API Service**: The `apiService` interface manages network calls to communicate with the project's backend.

Communication between the “Waiter” and “Kitchen” modules occurs via **Network Service Discovery (NSD)** and **TCP Sockets**, allowing the devices to find each other and exchange data without manual configuration.

### Backend Services

The application relies on a RESTful API developed in Python (Flask) to manage data synchronization. The system handles data persistence differently depending on the environment:

#### 🚀 1. Public Web Service (Recommended / Production)
**The application is connected to a live, production-ready web service.**
This is the standard architecture for the deployed app.

- **Infrastructure**: Docker Containers orchestrated via Docker Compose.
- **Router**: Traefik (handles HTTPS and Routing).
- **Database**: **PostgreSQL**
    - Running in an isolated private network container.
    - Ensures high concurrency and robust data integrity.
- **Base URL**: `https://arrangement-manager.roccoferrari.com`

---

#### ⚠️ 2. Local Server (Legacy / Development)
> **Note:** This method is deprecated for standard use. Use it only if you need to modify the backend source code.

The legacy local version runs the `server.py` script directly on a machine.

- **Infrastructure**: Python script running locally.
- **Database**: **SQLite**
    - Uses a local file (`arrangement_manager.db`) for data persistence.
    - Simple setup for quick testing but not suitable for production.

**API Endpoints Overview (Valid for both environments):**
- **User Management**: `/users/register` and `/users/login` for registration and authentication.
- **Table Management**: `/users/<userId>/tables` for adding, editing, deleting, and restoring a user's tables.
- **Menu Management**: `/users/<userId>/menu` for managing menu items.
- **Order Management**: `/users/<userId>/orders` for adding and updating order entries.

### Technologies Used

- **Language**: Kotlin
- **Backend Infrastructure**: Docker, Traefik, Python (Flask)
- **Databases**: PostgreSQL (Production), SQLite (Local Dev)
- **Architecture**: MVVM

- **Android Libraries**:
    - `Jetpack ViewModel` and `LiveData` / `StateFlow` for state management.
    - `Jetpack Navigation` for navigation between `Fragments`.
    - `Retrofit` for API calls and communication with the backend.
    - `Gson` for serializing/deserializing JSON data.
    - `RecyclerView` and `CardView` for displaying lists.
    - `Network Service Discovery (NSD)` for discovering services on the local network.
    - `Coroutines` for managing asynchronous and network operations.

### Installation and Startup

There are two ways to run the application, depending on your needs.

#### 📱 Option 1: Quick Start (End Users)
If you intend to use the application with the **Public Web Service**, you do not need to setup Android Studio or build the project from source. The app comes pre-configured to connect to the live server.

👉 **[Download the latest APK here](Arrangement-Manager.apk)**

Simply download and install the APK on your Android device.

---

#### 💻 Option 2: Development Build (Developers)
To modify the code or run the app via Android Studio, follow these steps:

1. Clone the repository:
    ```bash
    git clone https://github.com/RoccoFerrari/Arrangement-manager.git
    ```

2. Open the project in **Android Studio**.

3. Sync the project with Gradle files (Android Studio will download dependencies automatically).

4. **Configuration**: Verify that the `BASE_URL` in your Retrofit configuration points to the public web service:
   - `https://arrangement-manager.roccoferrari.com`

5. Run the application on an emulator or a physical device.

### How to contribute

Contributions are welcome! If you want to improve the project, submit a pull request.
