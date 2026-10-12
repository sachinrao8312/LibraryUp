# LibraryUp: library manager for self-study libraries

> Context file for anyone (human or AI) starting work in this repo. Read it first.
> Keep it up to date when decisions change.

## What this is

An Android app for **owners of self-study libraries / reading rooms in India**:
places where students (mostly competitive-exam aspirants: UPSC, SSC, banking,
NEET/JEE, state PSCs) pay a **monthly fee for a seat**, often in fixed time
**shifts**. Hubs: Delhi (Mukherjee Nagar, Laxmi Nagar), Kota, Patna, Jaipur,
Prayagraj, Indore, Lucknow, plus thousands of small-town libraries.

Today most owners run on a register notebook, Excel and WhatsApp. They lose
money to forgotten dues, double-booked seats and manual follow-ups.

**This is not a book-lending library app.** No catalog, ISBN or issue/return
of books.

## Relationship to RentUp

- Started as a **separate repo built from the RentUp codebase**
  (`sach-ai/RentUp`, Play Store: `ai.sach.rentup`), because the core model
  matches: property → library, unit/room → seat, tenant → student,
  rent → monthly fee, plus due dates, payments, reminders, documents and backup.
- Package / applicationId: **`ai.sach.libraryup`** (confirm before first upload;
  it can never change after publishing).
- This must be a **clearly distinct product** on Play Store (own name, icon,
  screenshots, description, library-specific features). Google's
  *Repetitive Content* spam policy can reject the app, or strike the developer
  account that also owns RentUp, if the two look like copies.
- Generic fixes (backup, auth, crash fixes) may be ported between the two repos
  by hand. Note the RentUp commit/PR when porting.

## Tech stack (inherited from RentUp)

Kotlin, Jetpack Compose (Material 3), Navigation Compose, Room (local DB),
DataStore, WorkManager, Firebase (Auth, Firestore, Messaging, Analytics,
Crashlytics), Credential Manager Google sign-in, Google Drive backup
(`drive.file` scope, zip of a JSON envelope + attachments), Coil, AndroidX
Security Crypto, Biometric lock. minSdk 24, targetSdk 35.

Firebase: register `ai.sach.libraryup` as a **new Android app** (own
`google-services.json`, own SHA-1/SHA-256 for debug, release and Play signing).

## Domain model (target)

| Entity | Key fields |
|---|---|
| Library (branch) | name, address, owner, opening hours; an owner may have several branches |
| Shift | name (Morning/Evening/Full day/Night), start–end time, fee |
| Seat | number/label, zone (AC/non-AC, cabin/open), optional locker |
| Student | name, phone, photo, ID proof, exam preparing for, guardian phone, join date |
| Allocation | student ↔ seat ↔ shift, from/to dates. A seat can host different students in non-overlapping shifts |
| Fee / Payment | month, amount, discount, mode (cash/UPI), receipt no., due date, late fee |
| Attendance | student, date, check-in/out time, method (QR/manual) |
| Expense | rent, electricity, internet, staff salary (for profit view) |
| Plan | name, duration (days or calendar months), price, shift |
| Locker | number, fee, assigned student |
| Enquiry | name, phone, wanted shift, follow-up date, status, converted student |
| ActivityLog | who, what entity, action (add/edit/delete), before/after, time |
| Library settings | logo, colors, UPI VPA, fee cycle mode, reminder templates, roles/PINs |

## Rename map from RentUp

`Property → Library`, `Unit/Room → Seat`, `Tenant → Student`,
`Rent → Fee`, `Lease/Agreement → Admission`, `Landlord → Owner`.
All user-facing text goes in `strings.xml` (no hardcoded strings), with Hindi
(`values-hi`) from day one.

## Features

Picked from the descriptions and every review of 12 competitor apps
(Play Store data, 2026-10-04). The *Why* column cites the evidence.
**P0** = needed to be taken seriously, **P1** = how we win,
**P2** = later, **No** = deliberately skipped.

### P0: table stakes (every serious rival has these)
| Feature | Details | Why |
|---|---|---|
| Student profile | Photo, phone, guardian phone, **father's name, address**, exam, ID proof (Aadhaar photo), join date, notes | Everyone has it; GoLibrary user asked for father's name and address on the ID card |
| Membership plans | Daily, monthly, **quarterly, half-yearly, yearly**, custom days; price per shift | GoLibrary user asked for quarterly/six-monthly; TrackMyLibrary and LibraryChief offer them |
| Fee cycle setting | **Calendar month** (5 Mar → 5 Apr) or fixed N days, per library | SeatMitra review: "fee is 30 days by default, should be fixed" |
| Visual seat map | Grid with a **custom layout: blocks/floors/rows**, rename seats, gaps | GoLibrary ask "seat customised option, block wise"; LibraryChief's "movie theatre" designer; SeatWise floor-wise |
| **Shift-aware seat colors** | Green free, yellow some shifts taken, red full day, orange fee overdue; **filter the map by shift** | GoLibrary 4★: "can't tell which seat is morning, evening or full day; finding evening seats takes a lot of effort" |
| Assign / **change seat** / **change shift** | One tap, history kept | AVS 1★: "how to change seat? change batch? assign batch?" |
| Payments | Cash/UPI, **partial payments and installments**, discount, late fee, edit/delete | MML offers installments; SeatMitra complaint: delete transaction broke after an update |
| **One receipt per payment** | Numbered PDF/image receipt shared to WhatsApp | AVS 1★: all past payments merged into one receipt, "accounting chaos" |
| Dues dashboard | Due today, overdue, **expiring in 7 days**, expired/left | SeatWise, LibraryChief ("overdue alerts on main screen") |
| WhatsApp reminders | Templates (Hindi/English) for fee due, expiry, holiday, welcome; **choose WhatsApp or WhatsApp Business**; bulk "send next" queue | SeatMitra ask: send through the WhatsApp Business app; everyone has reminders |
| Attendance | Manual plus QR; record **both in and out time** | SeatMitra 3★ ask; GoLibrary has entry/exit logs |
| Expenses and P&L | Rent, electricity, Wi-Fi, salary; monthly profit | AVS, SeatWise, TrackMyLibrary ("I can see profit and loss") |
| Renewals | Renew in one tap with the same seat and plan | SeatWise, GoLibrary |
| Hindi + English UI | Plus Marathi/Bengali later | SeatWise reviews praise Hindi; Librify has 9 languages |
| Backup | Automatic Drive backup **including attendance**, encrypted | AVS 1★: backups fail and skip attendance; GoLibrary 1★: crash wiped all data |

### P1: differentiators (the reasons to switch)
| Feature | Details | Why |
|---|---|---|
| **"Your data stays with you"** | No company server; data on the phone plus the owner's own Drive (encrypted). Say it on the first screen and in the listing | #1 fear across reviews: "takes our data" (5 apps), "data can be misused", "students got fraud calls", "data copying automatically" |
| **Unlimited free WhatsApp** | Reminders go through the owner's own WhatsApp, so no message credits or SMS packs | GoLibrary 4★: "please increase number of messages"; "improve message facility" |
| **UPI pay link in every reminder** | `upi://pay?pa=<owner VPA>&am=<due>&tn=<name-month>` plus a UPI QR on receipts. No gateway, no fees | GoLibrary review: "no option to take payment in the app" |
| **Activity log** | Who added, edited or deleted which fee or student, and when; deleted items go to a recoverable bin | MML 1★: "unclear when fees or entries are deleted, no record" |
| **Staff/operator login** | Owner + operator roles; the operator can't delete payments or see the P&L. Works on the same phone with a PIN in v1 | AVS ask: "operator login option"; Librify 1★: "how to add second admin, what permissions" |
| **Own branding** | Library logo, name and colors on receipts, ID cards and posters | MML 1★: "can't put our own logo" |
| **Student ID card** | Printable/shareable card with photo, seat, shift, validity, QR (for attendance), father's name | GoLibrary ask; SeatWise QR ID cards |
| **Fill-the-seats tools** | Auto-branded posters: vacancy ("6 evening seats free"), admission open, holiday, festival wishes, results. Shared to WhatsApp status | AVS reviews: "social media option very very useful"; nobody connects this to live vacancy data |
| **Enquiry list** | Walk-ins and phone enquiries with follow-up reminders; convert to a student in one tap | No rival has it; empty seats are lost revenue |
| **Freeze membership** | Pause for exam or home leave; validity extends; seat optionally held | SeatWise has "frozen" members |
| **Import and export** | Import from Excel or CSV; export students, payments and attendance to Excel/PDF at any time, **free** | SeatMitra ask: "save all members in Excel or PDF and send it"; LibraryChief offers data migration |
| **Fair pricing** | Full app free up to ~50 seats; Pro ≤ ₹999/yr. No demo timer, no activation code, data never locked behind payment | Activation-code 1★ reviews (AVS); GoLibrary "₹999 please"; "2–3 day demo"; "pay to recover data"; SeatMitra loved at ₹1000/yr |
| Lockers | Locker number, fee, assignment | SeatMitra, Libly |
| Account basics | Change the admin phone/email; proper back navigation; calm color theme | SeatMitra asks and 1★ ("no back button"); Appu 2★ "colours hurt my eyes" |

### P2: later
- **Multi-device sync** (owner plus manager phones) via Firestore with end-to-end encryption.
  Owners want it (AVS question about opening on another phone; GoLibrary praised for web),
  but rivals' broken sync is a top complaint. Ship only when it's solid. v1 = single
  device + Drive restore.
- **Student self-admission by QR** (student fills the form on their phone) and a student
  page showing rules, offers, dues and validity (SeatWise, Librify, MML; SeatMitra
  ask for a rules tab and offer banner). Needs a small backend.
- **Free library web page / Google presence** (SeatMitra website, Librainian "footprint on Google").
- Multi-branch (Appu, MML, LibraryTrack); web dashboard (GoLibrary users love web).

### No: deliberately skipped
- Biometric hardware, GPS attendance: costly, overkill for most libraries.
- Student study tracker, test series, education news (Appu): off-mission bloat.
- Book catalog, ISBN, OPAC (MML, Handy Library): a different product.
- Paid SMS gateway: WhatsApp share covers it for free.
- iOS in v1 (a Librify 2★ mentions iPhone, but owners are overwhelmingly on Android).

### Quality bar (most 1★ reviews are about this, not features)
- No crashes or hangs; fast on a ₹8k phone; works offline.
- Every destructive action is undoable or logged.
- Updates never break existing flows (SeatMitra regression).
- Reply to every Play review within 48h. Rivals win 5★ reviews on support alone.

## Competitors (Play Store data scraped 2026-10-04)

The market is **crowded**. Seat map, multiple shifts, WhatsApp reminders, QR
attendance and fee tracking are **table stakes**.

| App | Installs | Model | Notes |
|---|---|---|---|
| GoLibrary (`com.threeclick.golibrary`) | **~104k** (since 2019) | Subscription | Market leader. Biometric access control. Complaints: yearly plan too costly ("reduce to ₹999"), app hangs, lost data then asked to buy premium to recover |
| Appu Library (`com.app.appulibrary`) | ~7.7k | Cloud, 2–3 day demo | Multi-branch, staff, expenses, student study tracker (timer, goals). Many 5★ reviews look promotional. Complaints: slow, "took our data, demo only 2–3 days" |
| SeatMitra (`com.toothclick.library`) | ~6.1k | ~₹1000/yr, no ads | Lockers, holiday reminders, free library website. Liked for price. Asks: Excel/PDF export, change admin phone, fees use fixed 30 days instead of calendar month, back button missing |
| AVS Library Manager (`com.avsvishalmedia.librarymanagement`) | ~4.2k | ~₹2000 one-time, activation code | Offline. Complaints: multi-device sync broken, manual backups fail, attendance missing from backup, activation-code friction, no batch/seat change |
| LibraryChief, Librify (TWA), MML, TrackMyLibrary, SeatWise, My Library Manager, Librainian (TWA), Base Library | 50–2.6k each | Mixed | Small. Librify praised for seat map UI; MML crashes, can't add own logo; SeatWise has Hindi support, launched Mar 2026 |

Not relevant: `com.handylibrary.main` (personal book catalog, 1M installs),
`com.academic.master` (now "LibraryTrack", but reviews are from an older study-notes
app), `com.spysoft.bima` (insurance agents app).

### What owners complain about (from 1–2★ reviews)
1. **Data trust (the #1 fear).** "Takes our data", "data can be misused",
   "a library software took data and students started getting fraud calls",
   "data copying automatically". The same 1★ appears on 5 different apps.
2. **Data loss and sync.** Crashes wiping data, broken multi-device sync,
   failed backups, attendance missing from backups.
3. **Pricing tricks.** Short demos, activation codes, surprise paywalls,
   "pay to recover your data". Accepted price point: **~₹1000/year**.
4. **Bugs and slow apps.** Hangs, crashes, signup failures, missing back button.
5. **Missing basics:**
   - Operator/second admin with permissions
   - Changing seat or batch
   - Block-wise seat layout
   - Own logo on ID cards and receipts
   - Father's name and address on the ID card
   - Excel/PDF export
   - Calendar-month fee cycle
   - Changing the admin phone number
   - Rules and offers screen for students

### Where we can win
1. **"Your data never leaves your phone."** This answers the market's biggest fear
   head-on. Make it the headline of the Play listing. Offline-first native app;
   several rivals are web wrappers (TWA) or cloud-only.
2. **No lock-in, privacy, reliable backup.** Data stays on the device plus an **encrypted backup in
   the owner's own Google Drive**. No vendor server holds students' ID proofs.
   This is a selling point under the DPDP Act.
3. **Honest price.** A real free tier (no demo timer or activation code, and data
   is never held hostage) plus Pro at **≤ ₹999/year**. Rivals charge ₹59–₹499 a month.
4. **Filling empty seats, not just managing them.** Most tools stop at
   managing current students. Add:
   - A shareable vacancy poster or link ("6 seats free in the evening shift") for WhatsApp status and groups
   - An enquiry/lead list with follow-up reminders
   - A referral discount ("bring a friend")
5. **Fast setup.** Import students from Excel or a photo of the register, and be live in 5 minutes.

## Monetization

How competitors charge (Play data plus reviews): almost all charge **library owners
a subscription or license**, usually paid **outside Play Billing** (web/UPI after a
sales call). No ads except AVS.
- Trial, then a yearly or monthly plan: GoLibrary, SeatWise (14-day trial),
  LibraryChief and MML (7-day), Librify (₹59–149/mo), Libraryly (₹159–499/mo).
- One-time license via activation code: AVS (~₹2000; Play IAP $20.99).
- Cheap Play IAP: SeatMitra (₹1000/yr), TrackMyLibrary ($5.49–44.99).
- Free to start, paid later (owners resent it: "bhaiya aapne bhi paid kar diya").
- Side income: WhatsApp/SMS message packs (GoLibrary), biometric hardware
  (GoLibrary), branding/design services and referral bonuses (Appu), online fee
  collection via Razorpay (LibraryTrack, My Abhyasika), student marketplace /
  "nearby library" discovery (LibraryTrack).

**Our model:**
1. **Free:** all P0 features up to ~50 seats, forever. Export and backup always free.
2. **Pro:** ₹799–999/yr (or ~₹99/mo) via Play Billing. Unlimited seats, staff login,
   branding, posters, ID cards, enquiries, P&L reports. A 30-day full trial with no
   activation code.
3. **Lifetime option:** ~₹2499 one-time for owners who hate subscriptions.
4. **Later:** a multi-device sync / multi-branch tier; an optional paid "done-for-you"
   setup (we import their register); printed QR standees and ID cards.
5. **Ads (decided Oct 2026):** AdMob in the free tier. Rewarded ads unlock extras
   (bulk reminder run, export, poster, 24h Pro); banner/native on dashboard and lists
   only, **never** on payment or receipt screens. Pro removes ads.
6. **Never:** selling or sharing student data (DPDP Act risk; one "fraud calls" review
   kills the app in owner groups), crypto/tokens, message credits.

## Product principles
- **Offline-first.** Owners are on cheap phones with patchy internet.
- **Hindi + English**, simple words, big touch targets.
- **WhatsApp is the channel.** Everything shareable goes there.
- Student data (ID proofs, phone numbers) is personal data under India's
  DPDP Act. Keep it on device or the owner's own Drive, encrypt backups, and
  keep the Play Data Safety form accurate.

## Go-to-market (summary)
- Play Store ASO keywords: *library management, study library, reading room,
  seat booking, library fee, self study centre, library attendance*; listing in
  Hindi and English.
- Direct outreach: visit or call libraries in one hub city first, onboard
  10–20 owners free, collect feedback and reviews.
- Library-owner Facebook/WhatsApp groups, Hindi YouTube demo videos and Shorts,
  Instagram Reels.
- Launch posts: Product Hunt, Indie Hackers, r/developersIndia, r/SideProject,
  and Indian startup directories.
- Success metrics: installs/week, owners with at least 10 students added (activation),
  D30 retention, Play rating ≥ 4.5.

## Roadmap
See `TASKS.md` for the live task list. v0.1 (PR #1) covers the P0 list above.

## Code map
| Path | What |
|---|---|
| `domain/` | Pure Kotlin rules, JVM-tested: `Billing.kt` (cycles, plans, freeze), `Dues.kt` (status), `Seats.kt` (shift windows, conflicts, seat map), `Messaging.kt` (receipt no., UPI link, en/hi templates), `Csv.kt` (export/import), `BackupCrypto.kt`, `Reports.kt` |
| `app/.../data/local/entity/Entities.kt` | Room entities (schema v1) |
| `app/.../data/local/dao/LibraryDao.kt` | All queries |
| `app/.../data/repository/LibraryRepository.kt` | Every write; transactions + activity log; builds `LibraryState` |
| `app/.../data/backup/` | Backup zip, encryption secrets, Google Drive backup |
| `app/.../ui/library/` | All library screens + `LibraryViewModel` (activity-scoped) |
| `app/.../MainActivity.kt` | Auth flow → setup wizard → bottom-nav shell and routes |
| `app/.../ui/screens/` | Inherited RentUp screens: sign-in, app lock, settings |
| `app/.../notifications/` | Owner's morning digest (WorkManager) |
| `docs/` | Data safety draft, Play listing draft |

## How to build and run
- Android Studio (Ladybug or newer), JDK 17. Put the Firebase `google-services.json` for
  `ai.sach.libraryup` in `app/` (CI uses `.github/ci/google-services.dummy.json`).
- `./gradlew :domain:test` runs the business-rule tests (no Android needed).
- `./gradlew :app:assembleDebug :app:testDebugUnitTest` builds the app; CI runs both on every PR.
- Strings: every user-facing string in `res/values/*.xml` and `res/values-hi/strings.xml`
  (library screens: edit `tools/strings_gen.py`, then run `python3 tools/strings_gen.py app/src/main/res`).
- Schema changes need a Room `Migration`; never destructive migration once real owners exist.

## Open decisions
- Name decided: **LibraryUp**, Play title `LibraryUp: Library Manager`. Check Play, domain and trademark before upload.
- Exact free-tier limit and Pro price (Remote Config so it can be tuned).
- Whether attendance needs a student-side app or desk-QR only.
