# Simple Chat Android Project

A private, peer-to-peer encrypted chat app for local Wi-Fi communication with no servers in between.  

*net.forestany.simplechat*  
*must use JDK 21 for compilation*

---

## Description

This app is a simple and secure chat tool that allows two users to communicate directly over a local Wi-Fi network. All messages are end-to-end encrypted and exchanged without any central server - ensuring full privacy and keeping your data within your network.

You can connect to another user in two ways:
- Automatic discovery: Find available chat rooms automatically using UDP multicast over local wifi.
- Manual connection: Enter the known IP address and port of another device in the local wifi to connect directly.

The app is built with Android, using Java 21 and Kotlin, and relies on the developer's own framework forestJ for reliable, encrypted peer-to-peer data exchange.

Everything happens locally - fast, simple, and private.

---

## Tech Stack

- Android: Tested Android 36, Minimum Android 29
- Language: Kotlin, Java 21
- Framework: [forestJ](https://github.com/ReneArentz/forestJ)  
- IDE: Android Studio Narwhal for Linux | 2025.1.1 Patch 1

---

## License

This project is open source under the GNU GPL v3 license — feel free to host, modify, and improve it while maintaining attribution.