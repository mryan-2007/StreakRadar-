# ⚡ StreakRadar - Aviator Screen Multiplier Streak Radar

A high-performance, real-time Android screen analyzer and streak tracker built for monitoring top score strips (e.g. Aviator in Google Chrome). Runs **100% on your Android device without root or superuser permissions**.

---

## 🚀 Key Features

1. **Precision Screen Area Cropping (Interactive HUD)**:
   - Full-screen transparent overlay with touch-and-drag corner handles.
   - Position and resize the glowing crop box directly over the horizontal score ribbon in Chrome.
   - Quick "Top Ribbon" preset and live relative dimension preview.

2. **Continuous Multiplier Detection (10 checks/second)**:
   - Powered by **Google ML Kit Optical Character Recognition (OCR)**.
   - Automatically detects numbers, decimals, and multiplier indicators (e.g., `1.34x`, `15.00x`).
   - Identifies the **leftmost** score (the newly updated round in Aviator).
   - Smart debounce filter prevents duplicate counts while the score ribbon is stationary.

3. **Consecutive Streak Counter & Alerts**:
   - Configurable threshold (Default: `< 2.00x`).
   - Consecutive target streak (Default: `7` consecutive rounds).
   - If a new round multiplier is `< 2.00x`, the streak counter increments (1/7, 2/7, ...).
   - If a round multiplier is `>= 2.00x`, the streak counter automatically restarts at 0.
   - When the streak reaches 7 consecutive rounds, triggers **strong vibration patterns** and optional **sound chimes**!

4. **Floating In-Game HUD & Notification Center**:
   - Draggable mini widget (`⚡ 4/7 | 1.85x`) that stays on screen over Chrome.
   - Expandable HUD with quick controls: `[▶ Resume / ⏸ Pause]`, `[✂ Crop Area]`, `[↺ Reset Streak]`.
   - Ongoing system notification with action buttons to control the radar without switching apps.

5. **In-App Simulator**:
   - Test detection, threshold evaluation, and vibration alerts directly inside the app with simulated rounds.

---

## 📱 How to Use on Your Android Phone (No PC Needed)

1. **Launch StreakRadar**:
   - Open the app and grant **Display over other apps** (Overlay) and **Notification** permissions.
2. **Start Radar**:
   - Tap **"Start Radar"**. Accept the Android system screen capture prompt.
   - You will see the floating badge `⚡ 0/7 | --` appear on your screen.
3. **Switch to Chrome & Aviator**:
   - Open Google Chrome and launch the game.
4. **Crop the Score Bar**:
   - Tap the floating widget or pull down the notification drawer and tap **"✂ Crop Area"**.
   - Drag and resize the neon cyan box to match the top multiplier strip.
   - Tap **"✓ Save Area"**.
5. **Start Monitoring**:
   - Tap **"▶ Start"** on the floating widget or notification.
   - StreakRadar reads the leftmost multiplier 10 times per second.
   - Keep playing or watching. Once 7 consecutive scores under 2.00x are detected, your phone will strongly vibrate!

---

## 💻 How to Build in GitHub Codespaces

You can build the APK entirely in the cloud using GitHub Codespaces from your phone or tablet browser:

1. **Create Codespace**:
   - Push this repository to your GitHub account (`mryan-2007`).
   - Click **Code** -> **Codespaces** -> **Create codespace on main**.
2. **Make Gradle Wrapper Executable**:
   ```bash
   chmod +x gradlew
   ```
3. **Build the Debug APK**:
   ```bash
   ./gradlew assembleDebug
   ```
4. **Download the APK to your phone**:
   - In the Codespaces file explorer, navigate to:
     `app/build/outputs/apk/debug/app-debug.apk`
   - Right-click (or long press) `app-debug.apk` and select **Download**.
   - Install the APK directly on your Android phone!

---

## 🔒 Security & Privacy

- **No Root Required**: Uses standard Android `MediaProjectionManager` and `SYSTEM_ALERT_WINDOW`.
- **Local Processing**: All frame analysis and OCR are executed entirely on-device using local ML Kit models. No screenshots or data are uploaded anywhere.
