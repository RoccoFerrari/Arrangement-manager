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

### Backend (server.py)

The backend is a RESTful API developed in Python using the Flask framework and SQLAlchemy for database management. The application uses an SQLite database (arrangement_manager.db) for data persistence.

The server.py file defines database models for User, Table, MenuItem, and OrderEntry, as well as a set of API endpoints to manage CRUD (Create, Read, Update, Delete) operations on these entities.

Here's an overview of the available endpoints:

- **User Management**: `/users/register` and `/users/login` for registration and authentication.

- **Table Management**: `/users/<userId>/tables` for adding, editing, deleting, and restoring a user's tables.

- **Menu Management**: `/users/<userId>/menu` for managing menu items.

- **Order Management**: `/users/<userId>/orders` for adding and updating order entries.
    

### Technologies Used

- **Language**: Kotlin

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

To start the project, follow these steps:

1. Clone the repository:
    
    ```
    git clone https://github.com/RoccoFerrari/Arrangement-manager.git
    
    ```
    
2. Open the project in Android Studio.

3. Make sure all dependencies are installed (Android Studio will download them automatically).

4. Run the application on an emulator or a physical device.

### How to contribute

Contributions are welcome! If you want to improve the project, submit a pull request.
