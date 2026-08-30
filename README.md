# Sans Finance 🏦

Sans Finance is a premium, AI-powered personal finance and wealth management application. It provides a comprehensive dashboard to track your entire financial life—from daily expenses to long-term net worth, including investments and portfolio tracking.

<img width="1376" height="3058" alt="Gemini_Generated_Image_mbod72mbod72mbod" src="https://github.com/user-attachments/assets/3fabaac4-d548-4f75-a6b2-9857826bda7a" />

## 📥 Download

[![Download Latest APK](https://img.shields.io/badge/Download-Release%20v1.3-brightgreen?style=for-the-badge&logo=android)](https://github.com/nichsedge/sansfinance/releases/tag/v1.3)

Get the latest release directly from [GitHub Releases](https://github.com/nichsedge/sansfinance/releases/latest).

## ✨ Features

- **Daily Financial Operations Dashboard:** Monitor your Net Worth, Monthly Cash Flow, Pacing & Velocity, Upcoming Bills, and Recent Transactions in an uncluttered, high-velocity view.
- **Home Screen Widgets (Jetpack Glance):** Modern, reactive Material 3 widgets for quick transaction entry and real-time financial health summary with pacing alerts.
- **Monte Carlo FIRE Simulation:** 1,000-run stochastic geometric Brownian motion simulator modeling market volatility, inflation, and sequence-of-returns risk with 10th/50th/90th percentile fan charts and FIRE probability scores.
- **Dividend & Cash Yield Tracker:** Dedicated portfolio yield analysis tracking annual passive run-rate, weighted yield-on-cost %, coupon schedules, and lifestyle expense coverage.
- **Smart Cash-Injection Rebalancing:** Calculate optimal buy allocations for fresh deposits across underweight asset classes to achieve target allocations without triggering taxable sales.
- **Safe-to-Spend Runway Pacing:** Real-time calculation of daily discretionary allowances after accounting for upcoming committed bills and billing cycle days.
- **Installment Horizon Roadmap:** Timeline matrix projecting future monthly debt obligation reductions and freed cash flow milestones.
- **Database Health & Maintenance Utility:** One-tap SQLite `VACUUM` defragmentation, `PRAGMA optimize`, `ANALYZE`, and orphaned tag reference cleanup.
- **Emergency Fund Stress Testing:** Liquid safety buffer calculator with interactive shock scenarios (job loss, 50% pay cut, +25% cost-of-living shock, -30% portfolio drawdown).
- **Savings Rate & Net Worth Velocity:** Track real-time savings rate acceleration, 3/6-month velocity averages, and momentum trends with interactive haptic scrubbing.
- **Adaptive Wide-Screen Layouts:** Responsive multi-pane Material 3 design for foldables, tablets, and landscape orientations.
- **Wealth & Strategy Hub:** Centralized capital allocation (Asset Class, Category, Currency), Financial Independence (FIRE) suite with interactive simulation, and modern Bento Grid navigation.
- **AI-Powered Insights**: Advanced monthly reviews and portfolio health analysis via **OpenAI** or **OpenRouter** (Cloud AI).
- **Privacy First:** Core data processing is local with global privacy mode toggle, ensuring your financial privacy.

## 🛠 Tech Stack

- **Architecture:** Clean Architecture
- **Mobile UI:** [Jetpack Compose](https://developer.android.com/compose)
- **Dependency Injection:** [Hilt](https://dagger.dev/hilt/) (Android)
- **Local Database:** [Room](https://developer.android.com/training/data-storage/room) (SQLite) - Version 38
- **AI:** OpenAI & OpenAI-compatible APIs (OpenRouter)
- **Data Persistence:** DataStore (Preferences)

## 🚀 Project Structure

- `:app` — Android Application (including Domain & Data logic)

## 🚀 Getting Started

### Prerequisites

- Android device/emulator with **API level 36+**.
- [Android Studio Ladybug](https://developer.android.com/studio) or newer.
- JDK 17.

### Build and Install

1. Clone the repository:
   ```bash
   git clone https://github.com/nichsedge/sansfinance.git
   ```
2. Open the project in Android Studio.
3. Build and run the `app` module.

Alternatively, use the Makefile or build script:
```bash
make release
# or
./scripts/build_release.sh
```
The release APK will be available in the `release/` folder.

## 📄 License

This project is licensed under the MIT License.

---
Built with ❤️ by [nichsedge](https://github.com/nichsedge)
