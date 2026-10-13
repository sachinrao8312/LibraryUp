# LibraryUp: task list

The working backlog. `CLAUDE.md` says *why* (research, evidence, principles); this file
says *what's next*. Keep it current: tick items in the PR that finishes them, add new
ones as they come up.

Legend: `[x]` done · `[ ]` to do · **P0** must-have · **P1** differentiator · **P2** later.
Each item names where the work goes so a new session can start without asking.

---

## Milestone 0: v0.1 kickoff ✅ (PR #1)
- [x] Fork from RentUp; package `ai.sach.libraryup`, name, CI build + unit tests
- [x] `domain` module: billing cycles, dues, seat/shift conflicts, receipts, UPI links,
      templates, CSV, backup crypto (tested on the JVM)
- [x] Room schema v1 + `LibraryRepository` with activity log on every money/student change
- [x] Setup wizard, dashboard, seat map with shift filter, students, admission, student
      detail (collect, renew, change seat/shift, late fee, freeze, left), payments,
      reminders queue, attendance, expenses + P&L, library settings, CSV import/export,
      activity log
- [x] Backup of every table + photos/ID proofs; optional password encryption with recovery code
- [x] English + Hindi strings for all library screens

## Milestone 1: ready for pilot (closed testing with 10–20 libraries)
Must be done before any real owner uses it.

### Setup you must do by hand
- [ ] Firebase: add Android app `ai.sach.libraryup`, SHA-1/256 (debug, release, Play
      signing), download `google-services.json` into `app/`, add it as the
      `GOOGLE_SERVICES_JSON` repo secret for CI
- [ ] Replace `google_web_client_id` / `default_web_client_id` in `res/values/strings.xml`
      with the LibraryUp project's Web client ID (they still point at RentUp's project)
- [ ] Google Cloud: enable Drive API for the new project; OAuth consent screen name "LibraryUp"
- [ ] Release signing key; Play Console app; internal testing track
- [ ] Check "LibraryUp" on Play Store, domain, IP India trademark

### Quality and correctness (P0)
- [ ] Test on a real low-end phone (3 GB RAM, Android 8–10): setup, 200 seats × 3 shifts,
      seat map scroll, receipt PDF, WhatsApp intents, Drive backup
- [x] Repository tests (Robolectric + in-memory Room, `app/src/test/.../data/`): admission
      conflict, renew, change seat, freeze/unfreeze date extension, mark left frees seat,
      receipt sequence, payment edit/delete, late fee, student delete, attendance undo
- [x] Backup round-trip test: export → clear → restore → equal (all tables, plain and
      encrypted with password or recovery code). Attachments not covered yet
- [x] Release build with R8 runs in CI (`assembleRelease`); still verify a real restore on
      a release build on a phone
- [x] Room schema v1 committed (`app/schemas/`); CI republishes it on the `screenshots` branch.
      Re-copy it after any schema change until the first release, then never edit v1 again
- [x] Translate the main inherited settings/auth screens (sign-in, splash, lock, settings,
      backup, security). Help & support and privacy policy text still English
- [x] Remove dead RentUp code: Firestore guest-migration of `tenants`/`properties`
      (`AuthRepository.migrateFirestoreReferences`), "Additional Settings" empty card and
      `showProperties` pref (`AppPreferences*`), `WHATSAPP_INTEGRATION` route, unused
      Retrofit/OkHttp wiring in `AppModule`
- [x] `firestore.rules`: keep only `users/{uid}` profile docs; drop tenants/properties/guests paths
- [x] New icon (indigo open book + chevron), adaptive + legacy PNGs, notification icon.
      Splash/sign-in still use the green brand circle; final icon from a designer before launch
- [x] App lock covers resume from background, not just cold start

### Small P0 gaps found while building v0.1
- [x] Edit / delete a payment's amount (today: delete + re-collect)
- [x] Delete a student entirely (today: mark left)
- [x] Seat label conflicts: warn on duplicate labels in one block
- [x] Attendance: undo a wrong "In" tap (`clearAttendance` only clears Out)
- [x] Plans tied to a shift: picker in plan dialog (`Plan.shiftId` exists, no UI)
- [x] Message templates editable per library (`MessageTemplates` defaults only)
- [x] Holiday broadcast: one message to all active students (template exists)
- [x] Late fee auto-rule (e.g. ₹10/day after grace) instead of manual entry

## Milestone 2: public launch (P1 differentiators)
- [x] **Student ID card**: photo, seat, shift, validity, father's name, QR (PNG share)
- [x] **QR attendance**: scan the ID-card QR to mark In/Out (ZXing scanner; repeat scan within 5 min ignored)
- [x] **Own branding**: library logo on receipts and ID cards (posters when they exist); logo included in backups
- [x] **Vacancy poster**: live free seats per shift, logo, phone (Seats tab → megaphone)
- [ ] Later: more posters (admission open, holiday, festival wishes, results)
- [x] **Enquiry list**: walk-ins with follow-up date (in the morning digest), admit to student
      (`Enquiry` entity exists)
- [x] **Staff mode**: owner PIN; staff can't edit/delete payments, delete students, or open expenses, settings, export, backup, log
- [x] **Lockers**: number, fee, assignment; freed when a student leaves or is deleted
- [x] **Change admin phone/email**: library phone is in Library settings; the account email is the Google sign-in (sign in with another account to change it)
- [x] Reminder language per student (Hindi/English, default = library setting)
- [x] Monthly report PDF: collections by mode, expenses by category, profit, dues, admissions (Expenses → PDF icon)
- [x] Play listing text in English + Hindi (`docs/play-listing.md`); final phone screenshots still to capture

## Milestone 3: monetization (see CLAUDE.md → Monetization, App Factory MONETIZATION.md)
- [x] Pro gate (`ProRules` in domain, `ProManager`): Pro = purchase, 30-day trial from setup, or 24h ad pass
- [x] Free tier: stops adding seats past 50 (Remote Config `free_seat_limit`, `trial_days`); Pro extras open
      the Pro screen. Nothing entered is ever locked; export and backup always free
- [x] Play Billing: `pro_yearly` (subscription) and `pro_lifetime` (one-time), restore purchases, offline cache
- [x] AdMob: banner on the dashboard (free plan only); rewarded ad gives 24h Pro. Test IDs until
      `admobAppId`, `admobBannerId`, `admobRewardedId` are set in `gradle.properties`
- [x] Data Safety draft updated for ads and billing (`docs/data-safety.md`)
- [ ] Manual: create `pro_yearly` (₹999/yr) and `pro_lifetime` (₹2,499) in Play Console; AdMob app + units;
      Remote Config keys; `app-ads.txt`; "Contains ads" in Play Console
- [ ] Later: referral codes, "LibraryUp saved you" card, sponsor cards / affiliates

## Milestone 4: growth (P2)
- [ ] Multi-device sync (owner + manager phones), end-to-end encrypted
- [ ] Student self-admission via QR link (form on student's phone) + student page
      (rules, offers, dues, validity). Needs a small backend
- [ ] Free library web page / Google presence
- [ ] Multi-branch support (`libraryId` on rows; migration)
- [ ] Online fee collection (UPI intent with callback / Razorpay) with receipts
- [ ] Web dashboard
- [ ] More languages: Marathi, Bengali, Tamil, Telugu

## App Factory (shared code with RentUp and future apps)
- [ ] Port generic improvements back to RentUp: backup encryption (`BackupCrypto`),
      backup `app` id check, CI workflow
- [ ] At 3+ apps: extract `sach-core` modules (see App Factory `CORE_ARCHITECTURE.md`):
      `core-billing` (from `domain`), `core-backup`, `core-messaging`, `core-docs`, `core-ui`
- [ ] Next vertical: HostelUp (PG/hostel): research step per App Factory `PLAYBOOK.md`

## Known issues / tech debt
- Backups are built in memory (fine for hundreds of students; stream for very large photo sets)
- `LibraryState` recomputes on every table change; fine at library scale, profile if > 2,000 students
- Seat map day boundary uses the device date at composition; reopens correct after midnight
- Fixed font scale (inherited); revisit for accessibility
