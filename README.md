# YinYangLauncher — volledig project

Android-launcher met twee startschermen, negen aanpasbare mappen per scherm, persoonlijke fotoachtergronden per scherm, zoom/verschuiven, transparante yin-yang met blauw/oranje gloed en appkeuze per map.

## GitHub (op telefoon)
1. Maak een **lege** GitHub-repository `yin-yang-launcher` (README uit).
2. Upload **de inhoud van deze map**, met de mapstructuur intact. GitHub's webinterface pakt ZIP-bestanden niet automatisch uit. Een GitHub-app of een Git-client die mappen uploadt kan helpen.
3. Open **Actions** → **Bouw Android APK** → **Run workflow** (of push naar main).
4. Download na een geslaagde build onder **Artifacts** de `YinYangLauncher-APK` en pak de APK uit.
5. Installeer APK en kies onder Samsung Instellingen → Apps → Standaard-apps → Startscherm de launcher.

## Bediening
- Tik op yin-yang of veeg links/rechts om van scherm te wisselen.
- Tik op een map, kies Apps toevoegen/verwijderen of Mapnaam wijzigen.
- Onderaan: Foto (kies uit galerij), Pas foto aan (zoom en verplaats), Apps (volledige lijst), Startscherm (standaard instellen).
- Tijdens foto verplaatsen: sleep op een lege plek; tik op yin-yang om af te sluiten.

## Belangrijk
Deze broncode is samengesteld maar nog niet op een Android-toestel gebouwd of getest. GitHub Actions moet de APK nog daadwerkelijk compileren. Android kan waarschuwen bij installatie buiten Play Store. Geen verborgen tracking of internetrechten.
