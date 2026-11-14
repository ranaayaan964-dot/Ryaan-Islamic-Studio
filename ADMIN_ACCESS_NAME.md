# Admin Panel Access Guide

Yeh guide Roman English mein hai. Is mein koi password, PIN, OTP, API key, ya `.env` value nahi di gayi hai.

## Admin account

App mein admin access in authorized email accounts ke liye hai:

- `ayianrana20133@gmail.com`
- `ranasalar964@gmail.com`

In mein se apna account use karein. Username ya display name password nahi hota. Password wohi Firebase Authentication password hai jo account owner ne apne account ke liye set kiya hai. Password kisi README, source file, chat, ya GitHub par share na karein.

## Login ke steps

1. Ayaan Islamic Studio app open karein.
2. `LOG IN` select karein.
3. Apna authorized admin email aur us account ka Firebase password enter karein.
4. `Log In` press karein.
5. Agar extra verification dialog aaye, apne admin email par bheja gaya current verification code use karein. Code kisi aur ke saath share na karein.
6. Verification ke baad Terms screen aaye to `Agree` karein. Admin account ko Admin Dashboard par jana chahiye.
7. Agar app dashboard ki bajaye plan selection ya normal user screen khole, sign out karke sahi authorized Firebase account se login karein. Phir bhi masla ho to project maintainer se Firebase Auth account aur admin profile check karwayein.

## Password bhool gaye?

Login screen par `Forgot password?` select karke apna account email enter karein. Sirf us email par aaye reset code ka istemal karein. Agar code nahi aata, maintainer se Firebase Authentication aur mail delivery configuration check karwayein. Apna password ya verification code maintainer ko bhejne ki zaroorat nahi hoti.

## Security warning before public release

Current app source mein legacy admin sign-in fallback logic maujood hai. Is wajah se documented Firebase login steps ko production-grade security ka saboot na samjhein. Repository ko public rakhne ya naya APK distribute karne se pehle maintainer ko fallback login aur local-only admin authorization hata kar Firebase-backed authentication/authorization enforce karni chahiye. Koi embedded PIN/password use, publish, ya document na karein; agar pehle se share hua ho to usay compromised samajh kar rotate karein.

Admin access ko sirf app UI se nahi, Firestore Security Rules aur Firebase Authentication se bhi protect karein. Admin Firebase accounts par strong unique passwords aur supported multi-factor authentication enable rakhein. Service-account keys ya Admin SDK credentials Android app mein kabhi na rakhein.

## `.env` ko GitHub se bahar rakhna

Project `.gitignore` mein `.env` ignore hota hai. Commit ya push se pehle PowerShell mein yeh checks chala sakte hain:

```powershell
git check-ignore .env
git ls-files --error-unmatch .env
```

Pehla command `.env` ko ignored dikhana chahiye. Doosra command fail hona chahiye, kyun ke `.env` tracked nahi hona chahiye. `git add .` ke bajaye specific files stage karein, aur `git status --short` dekh kar confirm karein ke `.env` list mein nahi hai. Agar `.env` kabhi Git mein commit ya remote par push hua ho, sirf file delete karna kaafi nahi: us ke tamam secrets rotate karein.
