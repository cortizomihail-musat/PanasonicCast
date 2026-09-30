# Panasonic Cast v0.1 — diagnostic
Telefon țintă: HONOR X5c Plus. TV: Panasonic TX-40FSW404, nume observat 40FS400_Series.

Această versiune NU oglindește ecranul și NU transmite sunet. Este primul pas pentru verificarea conexiunii înaintea implementării transmisiei.

## Build fără Android Studio
Încarcă conținutul acestui director într-un repository GitHub, inclusiv .github/workflows/android.yml. Actions → Build Android APK → descarcă artifactul PanasonicCast-v0.1-APK și extrage app-debug.apk. Instalează APK-ul pe telefon.
Alternativ, cu JDK 17, SDK Android 35 și Gradle 8.9: `gradle assembleDebug`.

## Test
1. Telefonul și televizorul pe aceeași rețea locală; TV pornit.
2. Deschide Cast Android. Raportează ce ecran apare și dacă TV este listat.
3. Caută TV în Wi-Fi. Așteaptă 8 secunde.
4. Trimite diagnosticul sau copiază textul în conversație.

Un răspuns SSDP indică un receptor UPnP MediaRenderer, nu garantează că este televizorul Panasonic. Linkul descrierii este doar afișat; nu este accesat automat. Lipsa răspunsurilor nu dovedește lipsa DLNA. Routerul poate izola clienții sau bloca multicast.

Aplicația nu colectează parole și nu trimite date în cloud. Diagnosticarea trimite două cereri multicast locale și primește răspunsuri. Partajarea jurnalului este manuală; jurnalul conține adrese IP locale.

## Validare
Sursele și configurația au fost inspectate; APK-ul nu a fost compilat în mediul de creare (SDK și Gradle absente). Testele pe HONOR/TV sunt încă necesare.
