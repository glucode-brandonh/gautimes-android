# Gautimes Android App

## Local Sandbox API Testing

To run this app against a local instance of the Train Times API (e.g., a local Ruby server), follow these steps:

### 1. Configure the Base URL
The app is configured to use `http://localhost:9292/` by default. If you need to change this, add the following line to your `local.properties` file:

```properties
TRAIN_TIMES_BASE_URL=http://localhost:9292/
```

### 2. Physical Device Setup (ADB Port Reversal)
If you are running the app on a **physical Android device** via USB, you must "reverse" the port so the phone can see the server running on your laptop.

Run the provided script from the project root:
```bash
./setup-adb-port.sh
```

Alternatively, run the command manually:
```bash
adb reverse tcp:9292 tcp:9292
```

> **Note**: If you have multiple devices connected, specify the serial number:
> `adb -s <SERIAL_NUMBER> reverse tcp:9292 tcp:9292`

### 3. Cleartext Traffic
The app is configured to allow HTTP traffic (`android:usesCleartextTraffic="true"`) for local testing. In a production environment, ensure all traffic is migrated to HTTPS.

### 4. Persistence & Caching
The app uses **Room** to cache stations and journey results.
- **Stations**: Cached indefinitely and refreshed on demand.
- **Journeys**: Cached per route (`from` to `to`) with a **15-minute expiration**.

To force a refresh and ignore the cache during development, you can tap the **Journeys** test chip in the debug section of the Home screen.
