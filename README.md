# OWEE 💸

### Expense sharing that actually makes settling simple.

OWEE is a UPI-first Android app for friends and groups to **track shared expenses, calculate balances, and settle debts with fewer transactions**.

The interesting part: after making a payment through any UPI app, you can **share the payment receipt directly with OWEE**. OWEE reads the receipt and can turn the payment into a settlement or debt — without manually entering everything.

> **OWEE is not a payment app.** Payments happen through external UPI apps such as Google Pay, PhonePe, Paytm, or BHIM.

---

## ✨ Features

* 👥 **Friends** — Track direct expenses and debts between friends.
* 👨‍👩‍👧‍👦 **Groups** — Split expenses across multiple people and track everyone's balance.
* 🧮 **Smart Balances** — Balances are calculated from expenses and settlements instead of being stored separately.
* 🔄 **Smart Settlements** — Reduce multiple outstanding debts into fewer real-world transactions.
* 📷 **UPI Receipt Sharing** — Share a UPI payment receipt with OWEE and extract payment information.
* 💳 **UPI Payments** — Open installed UPI apps directly from OWEE to make payments.
* ⚡ **Realtime Updates** — Expenses, settlements, groups, friend requests, and notifications update through Supabase Realtime.

---

## 🧠 How OWEE Works

OWEE treats **expenses and settlements as the source of truth**.

```text
Expenses + Settlements
          ↓
   Local Calculation
          ↓
       Balances
          ↓
 Settlement Suggestions
```

No separate `friend_balance`, `group_balance`, or `home_balance` tables are required.

When something changes, OWEE recalculates the affected balances locally instead of synchronizing calculated totals.

---

## 💳 UPI Receipt → Settlement

A completed UPI payment can be shared directly with OWEE:

```text
UPI Payment
     ↓
Payment Receipt
     ↓
Share → OWEE
     ↓
Read Receipt
     ↓
Extract Payment Details
     ↓
Create Settlement / Debt
     ↓
Recalculate Balance
```

This connects the **actual payment** with the **expense relationship** being tracked in OWEE.

OWEE does not verify the payment with UPI. The receipt is user-provided information, and the user confirms the resulting settlement.

---

## 🏗️ Architecture

OWEE follows a clean Android architecture:

```text
Jetpack Compose
       ↓
   ViewModel
       ↓
 Repository / Use Case
       ↓
    Supabase
```

### Core principles

* Kotlin + Jetpack Compose
* MVVM
* StateFlow for reactive UI state
* Repository pattern
* Supabase Auth
* Supabase PostgreSQL
* Supabase Realtime
* Supabase Storage
* UPI deep links/intents
* Business logic kept outside Composables

---

## 📱 App Structure

OWEE has four main destinations:

```text
🏠 Home       → Financial overview
👥 Friends    → Direct expenses & settlements
👨‍👩‍👧 Groups   → Shared expenses & settlements
👤 Profile    → Account & UPI information
```

There is intentionally **no Settlement tab**. Settlements belong where the debt exists — inside Friends or Groups.

---

## 🔄 Realtime

Supabase Realtime is used for:

* Friend requests
* Group updates
* Expenses
* Settlements
* Notifications

Calculated values such as **balances, totals, and settlement plans are never synchronized**. They are derived locally from the source data.

---

## 🛠️ Tech Stack

**Kotlin** · **Jetpack Compose** · **MVVM** · **StateFlow** · **Navigation Compose** · **Supabase Auth** · **PostgreSQL** · **Supabase Realtime** · **Supabase Storage** · **UPI Intents**

---

## 📸 Screenshots

## 📸 Screenshots

<table>
  <tr>
    <td><img src="https://github.com/user-attachments/assets/e24603c0-fb91-4bef-add6-6eb862a8c19a" width="220"/></td>
    <td><img src="https://github.com/user-attachments/assets/bc1c8711-8d6b-4f4b-a1de-4a4da92cc44d" width="220"/></td>
    <td><img src="https://github.com/user-attachments/assets/b566582b-eaa6-4275-af9d-c4cad64060c7" width="220"/></td>
  </tr>
  <tr>
    <td><img src="https://github.com/user-attachments/assets/c2ce95bc-59aa-47e5-b427-22c0ad82bd16" width="220"/></td>
    <td><img src="https://github.com/user-attachments/assets/fa597c45-8131-490a-82f0-56382801cfb7" width="220"/></td>
  </tr>
</table>



---

## 🚀 Project

OWEE is built to solve a simple problem:

> **You shouldn't need a spreadsheet to figure out who owes whom.**

[⭐ View the project on GitHub](https://github.com/AnishRandhawa21/Owee)
