# AppBlocker - Device Owner Provisioning Guide for Parents

To secure your child's Android device, AppBlocker must be set up as a **Device Owner**. A normal application cannot prevent itself from being uninstalled or block new app installations. Device Owner status provides these capabilities.

Below are the two setup pathways for activating Device Owner protection.

---

## Method 1: Production Setup (Requires Factory Reset)

This is the recommended setup for securing a child's device. It ensures AppBlocker is enrolled before any other user accounts (e.g. Google Accounts) are added.

### Steps:
1. **Back up any important data** on the child's device.
2. **Factory Reset** the device from Android settings (`Settings` -> `System` -> `Reset options` -> `Erase all data`).
3. Turn on the device. On the very first setup welcome screen (where you select your language), **rapidly tap 6 times on any blank space**.
4. The device will launch a hidden camera scanner for QR code setup.
5. Connect to a Wi-Fi network if prompted.
6. **Scan the QR Code** generated inside the AppBlocker Onboarding screen (Slide 3) or create a QR code containing these exact properties:
   ```properties
   android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME=com.appblocker.core/com.appblocker.core.admin.AppDeviceAdminReceiver
   android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION=https://github.com/aakashakash933-hue/appblocker/raw/main/app-debug.apk
   ```
7. Android Setup Wizard will download AppBlocker, set it as the Device Owner, and launch it automatically to finish onboarding.

---

## Method 2: Testing / Developer Fallback (Via ADB)

If you are a developer testing the features locally, you can set the Device Owner status using ADB command-line tools without performing a factory reset.

### Prerequisites:
- USB Debugging enabled on the device.
- Device connected to your computer via USB with ADB tools installed.

### Steps:
1. Open settings on the target phone and **remove all Google and other local user accounts** under `Settings` -> `Passwords & accounts`. If you do not do this, the system command will fail.
2. Compile and install the AppBlocker APK on the device.
3. Open a terminal on your computer and execute:
   ```bash
   adb shell dpm set-device-owner com.appblocker.core/com.appblocker.core.admin.AppDeviceAdminReceiver
   ```
4. You should see a success message: `Success: Device owner set to package com.appblocker.core`.
5. Open the app. The warning banner will disappear, and you can now toggle uninstall locks and app blocking features.
6. You can now re-add your Google accounts.

---

## Security Alert: Private DNS Bypass
If the child sets a custom resolver in Android's **Private DNS** settings (e.g., DNS over TLS hostnames), local UDP/TCP DNS filters are bypassed. 
To prevent this, go to:
`Settings` -> `Network & Internet` -> `Private DNS` and select **Off** or **Automatic** (ensure no custom hostname is set). If active, AppBlocker will post a high-priority persistent warning notification to alerts.
