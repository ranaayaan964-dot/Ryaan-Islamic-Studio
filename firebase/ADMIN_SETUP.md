# Admin Setup aur Login Guide

Yeh guide Roman English mein hai. Is file mein password, PIN, OTP, API key, service-account key, ya `.env` ki koi value nahi hai. Admin password har account owner khud Firebase Console mein set ya reset karega.

## 1. Designated admin email accounts

Yeh addresses admin accounts ke liye designated hain, lekin email match khud admin permission nahi deta:

- `ayianrana20133@gmail.com`
- `ranasalar964@gmail.com`

Har email ka apna Firebase Authentication account hona chahiye. Admin access sirf us account ke Firebase UID par trusted server se set kiye gaye `admin: true` custom claim se milta hai. Apna account hi use karein; kisi aur ka password ya verification code na maangein.

## 2. Firebase project ko select karein

1. Firebase Console (`https://console.firebase.google.com/`) par apne authorized Google account se sign in karein.
2. Ayaan Islamic Studio ka sahi Firebase project select karein. Production aur test project ko mix na karein.
3. `Authentication` > `Sign-in method` mein `Email/Password` enabled hona chahiye.
4. `Firestore Database` active ho aur app ke liye sahi project configure ho.

Firebase project ID ko apne actual project se verify karein; neeche kisi project ID ya credential ko assume nahi kiya gaya hai.

## 3. Admin Firebase account banana ya check karna

Har authorized email ke liye:

1. Firebase Console mein `Authentication` > `Users` kholein.
2. Email list mein maujood ho to account ka status check karein. Nahi ho to `Add user` se email aur naya, unique password set karein.
3. Password owner ko secure password manager ya private channel se dein. Password ko is repository, issue, chat, source code, ya `.env` mein save na karein.
4. Agar account pehle se hai aur password maloom nahi, us account ke owner se Firebase Auth ka password reset karwayein. Admin ka purana password read ya recover nahi kiya ja sakta.
5. Account email verify karwayein agar project ki Firebase Auth settings verification enforce karti hain.
6. Har admin ke liye alag, lamba, unique password set karein. Password ko kisi aur account ke saath reuse na karein.

Firebase Console mein password dekhne ka option nahi hota. Password reset karna hota hai.

## 4. Admin profile aur custom claim tayyar karna

App admin ko sirf Firebase Authentication ke active session aur refreshed ID token ke `admin: true` custom claim se pehchanta hai. Email, local role, username, ya Firestore profile ka `role` field akelay admin access nahi dete. Admin claim ko trusted Admin SDK environment se set karein; Android client se claim set karna mumkin ya safe nahi hona chahiye.

1. `Authentication` > `Users` mein admin account khol kar uska Firebase UID copy karein. UID email address ya username nahi hota.
2. Trusted server, Cloud Function, ya apne secured admin workstation par Firebase Admin SDK ka istemal karein. Service-account key ko Android project, GitHub, ya `.env` mein mat rakhein.
3. UID aur email dono ko manually verify karke ensure karein ke account authorized admin email se match karta hai. Existing claims preserve karte hue sirf `admin: true` add karein. Yeh code sirf trusted server/admin environment par chalayein:

```js
const account = await getAuth().getUser(adminUid);
await getAuth().setCustomUserClaims(adminUid, {
  ...account.customClaims,
  admin: true,
});
```

`adminUid` ko sirf Firebase Console se verify kiye hue account UID se set karein. Yeh code Android client mein nahi chalana.

4. Firestore `users/{uid}` profile verify karein. Kam az kam `uid`, `email`, `username`, `fullName`, `authProvider`, `createdAt`, `lastLoginAt`, `activitySummary`, aur `isRemoved: false` fields consistent hon. Profile ka `role` field admin authority ka source nahi hai.
5. App se sign out karke dobara login karein, taake refreshed Firebase ID token mein custom claim aa jaye.

Firebase custom claim add karna account ke password ko nahi badalta.

## 5. Firestore rules deploy aur verify karna

Repository ke root folder se Firebase CLI ko apne sahi project par point karke rules deploy karein:

```powershell
firebase login
firebase use <your-firebase-project-id>
firebase deploy --only firestore:rules
```

`<your-firebase-project-id>` ko actual ID se replace karein. Deploy se pehle rules ko Firebase Emulator ya staging project mein test karein. Admin access ko sirf app ke button/Compose UI par depend na karein; Firestore rules hi remote data ka asal access control hain.

## 6. App mein admin login ke steps

1. App open karein aur splash/loading screen khatam hone dein.
2. Login screen par `LOG IN` select karein.
3. Apna authorized admin email aur us email ke Firebase Authentication account ka current password enter karein.
4. `Log In` press karein.
5. Agar Firebase SMS multi-factor verification ka prompt aaye, enrolled phone par aaya hua current code enter karein. Code kisi ke saath share na karein.
6. Login ke baad Terms screen aaye to parh kar `Agree` select karein.
7. Admin account ko Admin Dashboard par jana chahiye. Agar normal user screen ya plan selection dikhe, sign out karke Firebase UID/profile, account removal status, refreshed custom claim, aur Firestore rules check karein. Sirf email match se admin access enable nahi hota.

## 7. Password bhoolne par

1. Login screen mein `Forgot password?` select karein.
2. Usi admin Firebase email ko enter karein jiska password reset karna hai.
3. Firebase ke password reset link ko privately open karke naya password set karein.
4. App mein naye password se sign in karein. Password ya reset link kisi helper ko bhejne ki zaroorat nahi.
5. Agar email na aaye, Firebase Authentication user, email template/settings, spam folder, aur app logs check karein.

## 8. Zaroori security status

Android client mein local PIN/username admin bypass remove kar diya gaya hai. Admin authorization ab Firebase custom claim par depend karta hai. Isliye production admin accounts ko claim assign karne ke liye trusted Admin SDK backend/admin environment configure karna zaroori hai.

Public GitHub, public APK, ya real users ke liye release se pehle maintainer ko:

1. Custom claim sirf trusted server/Admin SDK se assign karein aur admin email/UID pairing ko server-side verify karein.
2. Naye `firestore.rules` deploy karne se pehle Firebase Emulator ya staging project mein profile, Pro redemption, aur message-recipient access tests chalayein.
3. Email/password accounts ke liye Firebase mein supported MFA enable karein; email reset link ko MFA ka substitute na samjhein.
4. Gemini/Groq jaise API keys ko Android APK mein confidential na samjhein. Public production ke liye provider calls trusted backend ke peeche rakhein aur keys ko provider console mein restrict/rotate karein.

## 9. Secrets aur GitHub

- `.env` ko commit ya push na karein; repository `.gitignore` mein yeh file ignored hai.
- `git add .` ki bajaye sirf intended file paths stage karein.
- Commit se pehle yeh checks run karein:

```powershell
git status --short
git check-ignore .env
git ls-files --error-unmatch .env
```

`git check-ignore .env` ignore rule dikhaye. `git ls-files --error-unmatch .env` fail hona chahiye, matlab file tracked nahi hai. Agar `.env` ya koi password/service-account key pehle kabhi Git mein push hua ho, file delete karna kaafi nahi: us credential ko foran rotate/revoke karein.
