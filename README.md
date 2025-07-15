
# 🔐 Cipher Shield

**Cipher Shield** is a privacy-first Android application that enables secure encryption and decryption of files using hybrid cryptography (RSA + AES). It offers seamless file selection, encrypted storage, private key management, and user-defined save paths — all wrapped in a clean, user-friendly interface.

---

## 📱 Features

- 🔑 **Hybrid Encryption** – RSA-secured AES key + AES-encrypted file
- 📂 **Custom File Picker** – Select any file from local storage for encryption
- 🧾 **Private Key Management** – Generate, view, copy, and save RSA private keys locally
- 💾 **Custom Save Paths** – Choose where to store encrypted files and keys
- 📤 **Secure Sharing** – Share `.cyps` files safely via Android’s file provider
- 📥 **Decryption with Original Extension** – Restore original files with correct extensions
- 🧭 **User Manual & UI Guide** – In-app searchable documentation
- 🏠 **Modern UI** – Polished screens with home/exit navigation and animations

---

## 🔧 Technologies Used

| Stack | Tools |
|-------|-------|
| Language | Java |
| Framework | Android SDK |
| UI | XML (Material UI elements) |
| Encryption | AES + RSA (Java Security APIs) |
| Storage | Internal storage, SQLite (for optional key saving) |
| File Handling | Content Resolver, FileProvider |
| Compatibility | Android 6.0+ (API 23+) |

---

## 🚀 Getting Started

1. Clone this repo:
   ```bash
   https://github.com/wolverine9039/Cipher-Shield.git
   ```
2. Open in **Android Studio**
3. Build the project and run it on an emulator or real device

> ✅ Make sure you grant **file read** and **storage** permissions when prompted.

---

## 🧪 How It Works

- When you select a file, the app:
  - Reads file as bytes
  - Generates an AES key and encrypts the file
  - Encrypts the AES key using RSA
  - Stores the AES+RSA encrypted payload as `.cyps`

- During decryption:
  - Reads `.cyps` file
  - Extracts and decrypts AES key using private RSA key
  - Decrypts the file and restores original extension

---

## 📂 File Structure

```plaintext
CipherShield/
├── MainActivity.java         # Splash screen
├── front.java                # Main dashboard
├── Encryption.java           # File picker + intent
├── Encryption_Algos.java     # RSA + AES encryption logic
├── Decryption.java           # Decryption with key parsing
├── UserManual.java           # In-app searchable user guide
├── res/
│   └── layout/               # All XML layouts
│   └── drawable/             # Custom backgrounds/icons
└── AndroidManifest.xml
```

---

## 🧠 Author

**Mayank Bisht**  
📧 mayankbisht8532@gmail.com  
📍 Dehradun, India  
[LinkedIn](https://github.com/wolverine9039/Cipher-Shield.git)

---

## 📜 License

This project is licensed under the [MIT License](LICENSE).  
You are free to use, modify, and distribute this app.
