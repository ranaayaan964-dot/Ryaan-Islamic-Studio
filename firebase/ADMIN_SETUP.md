# Admin Setup aur Login Guide

Yeh guide Roman English mein hai. Is file mein password, PIN, OTP, API key, service-account key, ya `.env` ki koi value nahi hai. Admin password har account owner khud Firebase Console mein set ya reset karega.

## 1. Authorized admin email accounts

App code mein yeh do email addresses admin ke liye listed hain:

- `ayianrana20133@gmail.com`
- `ranasalar964@gmail.com`

In emails ka matlab yeh nahi ke inka password code ya repository se mil sakta hai. Har email apne Firebase Authentication user account se linked hona chahiye. Apna account hi use karein; kisi aur ka password ya verification code na maangein.

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

App mein admin role email address ya Firebase ID token ke `admin: true` custom claim se pehchana jata hai. Production setup mein custom claim ko trusted Admin SDK environment se set karein, aur user profile mein `isRemoved: false` rakhein.

1. `Authentication` > `Users` mein admin account khol kar uska Firebase UID copy karein. UID email address ya username nahi hota.
2. Trusted server, Cloud Function, ya apne secured admin workstation par Firebase Admin SDK ka istemal karein. Service-account key ko Android project, GitHub, ya `.env` mein mat rakhein.
3. Existing claims ko preserve karte hue sirf `admin: true` add karein. Node.js example:

```js
const account = await getAuth().getUser(adminUid);
await getAuth().setCustomUserClaims(adminUid, {
  ...account.customClaims,
  admin: true,
});
```

`adminUid` ko sirf Firebase Console se verify kiye hue account UID se set karein. Yeh code Android client mein nahi chalana.

4. Firestore `users/{uid}` profile verify karein. Kam az kam `uid`, `email`, `username`, `fullName`, `authProvider`, `createdAt`, `lastLoginAt`, `activitySummary`, aur `isRemoved: false` fields consistent hon.
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
6. Agar email reset-code dialog aaye, apne inbox par aaya current code enter karein. Code is guide mein store nahi karna.
7. Login ke baad Terms screen aaye to parh kar `Agree` select karein.
8. Admin account ko Admin Dashboard par jana chahiye. Agar normal user screen ya plan selection dikhe, sign out karke email spelling, Firebase UID/profile, account removal status, custom claim, aur Firestore rules check karein.

## 7. Password bhoolne par

1. Login screen mein `Forgot password?` select karein.
2. Usi admin Firebase email ko enter karein jiska password reset karna hai.
3. App/email flow jo reset instructions ya code de, usay privately use karein.
4. Naya password Firebase Auth aur app ke configured reset flow ke mutabiq set karein.
5. Agar email/code na aaye, Firebase Authentication user, sender/email service configuration, spam folder, aur app logs check karein. Password ya OTP kisi helper ko bhejne ki zaroorat nahi.

## 8. Zaroori security warning: legacy admin login

Current Android source mein purana local admin fallback aur embedded test credentials ka logic abhi maujood hai. Yeh secure production authentication nahi hai, aur is guide mein us credential ko repeat nahi kiya jayega. Sirf email list ko dekh kar app ko secure na samjhein.

Public GitHub, public APK, ya real users ke liye release se pehle maintainer ko:

1. Legacy hard-coded admin username/password/PIN fallback ko source aur tests se remove karna chahiye.
2. Admin ko sirf Firebase Authentication ke successful sign-in, verified identity, trusted admin claim, aur active Firestore profile se authorize karna chahiye.
3. Firestore rules mein removed users ko block karna aur admin writes ko admin custom claim tak restrict karna chahiye. Admin email ke naam par client-side bypass ko authorization na banayein.
4. Email/password accounts ke liye Firebase mein supported MFA enable karein; email OTP ko SMS MFA ka substitute na samjhein.
5. Agar legacy credential kabhi APK, GitHub commit, ya kisi message mein share hua ho to usay compromised samajh kar remove/rotate karein aur purane sessions/tokens revoke karne ka trusted server-side plan banayein.

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
