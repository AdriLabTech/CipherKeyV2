<p align="center">
  <img src="./assets/logo.png" width="350"/>
</p>
<p align="center">
  <strong>Robusto • Simple • Seguro • 100% Offline</strong><br>
  Gestor de contraseñas cifrado desarrollado en Java
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-JDK%2025-orange?style=for-the-badge"/>
  <img src="https://img.shields.io/badge/Build-Maven-blue?style=for-the-badge"/>
  <img src="https://img.shields.io/badge/Tests-JUnit%206-red?style=for-the-badge"/>
  <img src="https://img.shields.io/badge/License-MIT-green?style=for-the-badge"/>
  <img src="https://img.shields.io/badge/Latest_Version-3.0-yellow?style=for-the-badge"/>
</p>

---

# 📚 Índice

- [📌 Descripción](#-descripción)
- [✨ Características](#-características)
- [🛡️ Seguridad](#-seguridad)
- [🏗️ Arquitectura](#️-arquitectura)
- [📂 Estructura del Proyecto](#-estructura-del-proyecto)
- [🖥️ Manual de Usuario](#️-manual-de-usuario)
- [🚀 Instalación Paso a Paso](#-instalación-paso-a-paso)
- [🧪 Tests](#-tests)
- [📊 Diagramas](#-diagramas)
- [🔄 Versionado](#-versionado)
- [🤝 Contribuir](#-contribuir)
- [📄 Licencia](#-licencia)

---

# 📌 Descripción

**CipherKey** es un gestor de contraseñas seguro, completamente offline, desarrollado en **Java (JDK 25)** utilizando **Java Swing** para la interfaz gráfica.

Su objetivo es proporcionar almacenamiento cifrado local sin depender nada ajeno al ordenador local.

### 🔒 *__Tu seguridad no debería depender de terceros.__*

---

# ✨ Características

- 🔐 Bóveda cifrada en un único archivo `.ckey` protegida con clave maestra
- 🗂️ Entradas con usuario, contraseña, sitio web, notas, **categorías** y **favoritas**
- 🔎 Búsqueda instantánea por nombre, usuario, sitio, categoría o notas
- 🔑 Generador de contraseñas (8-64 caracteres, sin caracteres ambiguos opcional) y medidor de fuerza
- 🌗 Tema claro y oscuro (se recuerda), interfaz moderna con FlatLaf y soporte HiDPI
- ⌨️ Atajos de teclado y menú contextual
- 🧹 Portapapeles que se borra solo a los 30 s y bloqueo automático por inactividad (configurable)
- 🌐 100% offline

---

# 🛡️ Seguridad

| Característica | Implementación |
|---------------|---------------|
| Derivación de clave | Argon2id (64 MiB, 3 iteraciones, sal de 32 bytes); parámetros guardados en la cabecera |
| Cifrado | AES-256-GCM, nonce aleatorio nuevo en cada guardado |
| Integridad | La cabecera del fichero se autentica como AAD: cualquier alteración invalida la bóveda |
| Guardado | Atómico (fichero temporal + `fsync` + renombrado) con copia `.bak` de la versión anterior |
| Memoria | La contraseña maestra no se conserva tras abrir; solo la clave derivada, que se pone a cero al bloquear |
| Portapapeles | Se limpia a los 30 s si sigue conteniendo lo copiado |
| Bloqueo | Por inactividad (5 min) |

### 🔐 Decisiones de Seguridad

- No se almacenan contraseñas en texto plano y la clave maestra nunca se guarda.
- Sin conexión a red.
- Formato `.ckey` v2: `"CKEY" | versión | KDF | memoria | iteraciones | paralelismo | sal | nonce | cifrado+tag`.
- No es compatible con los `.ckey` de la v3.0 (eran bóvedas de prueba).
- El límite de intentos del login es solo de usabilidad: quien tenga el `.ckey` puede atacarlo offline, por eso la protección real es Argon2id + una clave maestra larga.
- Doble factor: pendiente. La v3.0 lo hacía por email, pero no protegía el fichero y se ha eliminado (ver "Versionado").

---

# 🏗️ Arquitectura

CipherKey sigue una arquitectura inspirada en el patrón **MVC (Model-View-Controller)** para separar responsabilidades.

| Capa | Responsabilidad |
|------|----------------|
| Model | Representación de datos (Credenciales, Usuario) |
| View | Interfaz Swing con FlatLaf (tema claro/oscuro, iconos SVG) |
| Controller | Lógica de la gestion de Contraseñas y bovedas |
| Security | Cifrado, persistencia y seguridad |
| Persistence | Formato `.ckey` (`Vault`) y preferencias de la aplicación |

---

# 📂 Estructura del Proyecto

```
src/
 └── main/
     └── java/dev/
                 ├── controller/
                 ├── model/
                 ├── persistence/
                 ├── security/
                 └── view/
```

### Justificación

- Separación clara de responsabilidades.
- Facilita escalabilidad.
- Permite testear la lógica sin depender de la UI.
- Mejora mantenibilidad.

---

# 🖥️ Manual de Usuario

## 1️⃣ Acceso

- Elige tu bóveda (las recientes aparecen en el desplegable) o pulsa **Examinar…**, escribe la clave maestra y **Desbloquear**.
- **Crear una bóveda nueva** pide una clave de al menos 8 caracteres con medidor de fuerza. Ojo: si la olvidas, no hay recuperación.

<p align="center">
  <img src="./assets/login-light.png" width="330"/>
  <img src="./assets/login-dark.png" width="330"/>
</p>

## 2️⃣ Ventana principal

- **Barra lateral:** todas las entradas, favoritas y categorías (con recuento).
- **Lista:** ordenable por columna; doble clic o `Enter` para editar.
- **Panel de detalle:** contraseña oculta con mostrar/copiar, abrir el sitio web, notas y fechas.

<p align="center">
  <img src="./assets/dashboard-light.png" width="650"/>
</p>
<p align="center">
  <img src="./assets/dashboard-dark.png" width="650"/>
</p>

| Atajo | Acción |
|-------|--------|
| `Ctrl+N` | Nueva entrada |
| `Ctrl+F` | Buscar |
| `Ctrl+B` / `Ctrl+U` | Copiar contraseña / usuario |
| `Ctrl+E` | Editar |
| `Supr` | Eliminar (con la lista enfocada) |
| `Ctrl+L` | Bloquear |

## 3️⃣ Entradas y generador

<p align="center">
  <img src="./assets/entry.png" width="300"/>
  <img src="./assets/generator.png" width="300"/>
</p>

---

# 🚀 Instalación Paso a Paso

## 📋 Requisitos

- Java JDK 25
- Maven 3.9+
- Sistema operativo: Windows / Linux / macOS (.jar)
  
---

## 1️⃣ Clonar repositorio

```bash
git clone https://github.com/AdriLabTech/CipherKeyV2.git
cd CipherKeyV2
```

## 2️⃣ Compilar proyecto

```bash
mvn clean package
```

## 3️⃣ Ejecutar aplicación

```bash
java -jar target/cipherkey-<VERSION>.jar
```

## 4️⃣ Primera ejecución

- Pulsa **Crear una bóveda nueva**, elige dónde guardar el `.ckey` y define tu clave maestra.

---

# 🧪 Tests

```bash
mvn test
```

Cubren el formato y la criptografía de la bóveda (contraseña incorrecta, manipulación de cabecera y datos, cambio de clave, guardado atómico, parámetros KDF absurdos) y el generador de contraseñas.

---

# 📊 Diagramas

## 🔷 Apertura de bóveda

```mermaid
flowchart TD
    A[Usuario elige .ckey + clave maestra] --> B[Leer cabecera y validar parámetros]
    B --> C[Argon2id → clave de 256 bits]
    C --> D{AES-GCM con cabecera como AAD}
    D -->|Tag válido| E[Entradas en memoria]
    D -->|Tag inválido| F[Contraseña incorrecta o bóveda manipulada]
```

---

# 🔄 Versionado

El proyecto sigue.

## V[Beta]

### Añadido
- Sistema de autenticación con clave maestra
- Persistencia cifrada `.ckey`
- Interfaz gráfica Swing

## V[1.0]

### Añadido
- Sistema de autenticación con clave maestra
- Generador de contraseñas
- Persistencia cifrada `.ckey`
- Interfaz gráfica Swing

  ## V[2.0]

### Añadido
- Sistema de autenticación con clave maestra
- Generador de contraseñas
- Persistencia cifrada `.ckey`
- Interfaz gráfica Swing
- Gestion de Bovedas
- Modo oscuro

## V[3.0]

### Añadido
- Sistema de autenticación con clave maestra
- Generador de contraseñas
- Persistencia cifrada `.ckey`
- Interfaz gráfica Swing
- Gestion de Bovedas
- Modo oscuro
- Validación por doble factor de autentificacion via email

---

## V[4.0] - EN DESARROLLO

### Añadido
- Interfaz rediseñada con FlatLaf: login único con bóvedas recientes, barra lateral, buscador, panel de detalle, avisos (toasts) y atajos
- Categorías, favoritas, medidor de fuerza y generador en vivo (con opción sin caracteres ambiguos)
- Tema claro/oscuro persistente y bloqueo automático configurable

### Cambiado
- Formato `.ckey` v2: Argon2id, cabecera autenticada, guardado atómico con `.bak`
- Guardar ya no repite la derivación de clave (de ~300k iteraciones PBKDF2 a ~2 ms)
- Derivación de clave fuera del hilo de la interfaz

### Eliminado
- Verificación por email (no protegía el fichero y exigía credenciales SMTP)
- Código sin uso (`CryptoUtils`, `MasterPasswordService`, `StorageManager`) y las dependencias Jackson y jakarta.mail

---

# 🤝 Contribuir

1. Fork del proyecto
2. Crear rama `feature/nueva-funcionalidad-o-fix-bug`
3. Commit descriptivo
4. Pull Request

Convenciones:
- Commits claros y descriptivos
- Seguir estructura MVC
- Añadir tests a nuevas funcionalidades

---

# 📄 Licencia

Distribuido bajo licencia MIT.

---

<p align="center">
  Hecho con ☕ y Java
</p>