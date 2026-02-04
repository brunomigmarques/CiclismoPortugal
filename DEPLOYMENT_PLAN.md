# Ciclismo Portugal - Google Play Deployment Plan

## 📊 Current Status

### App Configuration
- **Package**: `com.ciclismo.portugal`
- **Version Code**: 3
- **Version Name**: 1.0.2
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 35 (Android 15)
- **Signing**: Release keystore configured ✅

### AdMob Configuration
- **App ID**: `ca-app-pub-4498446920337333~5916260672` ✅
- **Banner ID**: `ca-app-pub-4498446920337333/8835929871` ✅
- **Interstitial ID**: `ca-app-pub-4498446920337333/7055944347` ✅
- **Rewarded ID**: `ca-app-pub-4498446920337333/5996151723` ✅
- **GDPR Consent (UMP)**: Implemented ✅

---

## 🎯 AdMob Placement Analysis

### Current Banner Placements (7 screens)
| Screen | Location | Status |
|--------|----------|--------|
| HomeScreen | Bottom of daily tips | ✅ |
| NewsScreen | After featured section | ✅ |
| CalendarScreen | Bottom of list | ✅ |
| DetailsScreen | Bottom of content | ✅ |
| FantasyHubScreen | Bottom of content | ✅ |
| MarketScreen | Bottom of list | ✅ |
| MyTeamScreen | Bottom of content | ✅ |

### Missing Banner Placements (6 opportunities)
| Screen | Recommended Location | Priority |
|--------|---------------------|----------|
| RaceDetailsScreen | After stages section | HIGH |
| CyclistDetailScreen | After stats section | HIGH |
| LeaguesScreen | Between league cards | MEDIUM |
| SeasonHistoryScreen | After history section | MEDIUM |
| GameRulesScreen | Between rule sections | LOW |
| ProfileScreen | After preferences | LOW |

### Interstitial Ad Opportunities (NOT IMPLEMENTED)
| Trigger Point | User Journey | Priority |
|---------------|--------------|----------|
| Team wizard completion | After creating Fantasy team | HIGH |
| Race details exit | After viewing WorldTour race | MEDIUM |
| After 3rd event view | Browsing multiple events | MEDIUM |
| Video completion | After watching cycling video | LOW |

### Rewarded Ad Opportunities (NOT IMPLEMENTED)
| Feature | Reward | Priority |
|---------|--------|----------|
| Extra transfer | +1 free transfer in Fantasy | HIGH |
| Unlock premium stats | View advanced cyclist stats | MEDIUM |
| Remove ads for session | 30min ad-free experience | LOW |

---

## ✅ Google Play Readiness Checklist

### Required Items
- [ ] **Privacy Policy URL** - Host on website/GitHub Pages
- [ ] **App Icon** - 512x512 PNG (hi-res)
- [ ] **Feature Graphic** - 1024x500 PNG
- [ ] **Screenshots** - At least 2 phone screenshots
- [ ] **Short Description** - Max 80 characters
- [ ] **Full Description** - Max 4000 characters
- [ ] **Content Rating Questionnaire** - Complete in Play Console
- [ ] **Data Safety Form** - Declare data collection practices
- [ ] **Target Audience** - Set age rating (likely Everyone)

### Technical Requirements
- [x] Signed APK/AAB with release keystore
- [x] ProGuard/R8 enabled for release
- [x] Version code incremented
- [x] AD_ID permission declared
- [x] GDPR consent implemented (UMP SDK)
- [ ] Test release build on device
- [ ] Verify all features work in release mode

### Content Checklist
- [ ] No placeholder content
- [ ] All images have proper licenses
- [ ] Portuguese translations complete
- [ ] Error messages user-friendly
- [ ] Contact email visible in app

---

## 📝 Store Listing (Suggested)

### Short Description (80 chars)
```
Calendário de ciclismo em Portugal, notícias e Fantasy Cycling!
```

### Full Description
```
🚴 CICLISMO PORTUGAL - A app essencial para ciclistas portugueses!

📅 CALENDÁRIO DE PROVAS
• Todas as provas de ciclismo em Portugal
• Estrada, BTT e Gravel
• Adiciona lembretes ao teu calendário
• Filtra por região e tipo

📰 NOTÍCIAS
• Agregador das principais fontes portuguesas
• A Bola, Record, O Jogo, Jornal de Notícias
• Vídeos de ciclismo do YouTube
• Atualizações diárias

🏆 FANTASY CYCLING (JOGO DAS APOSTAS)
• Cria a tua equipa de 15 ciclistas
• Orçamento de 100M para gerir
• Compete com amigos em ligas
• Pontuação baseada em corridas reais WorldTour

🌍 CORRIDAS WORLDTOUR
• Calendário completo UCI WorldTour
• Informação de etapas para Grand Tours
• Tour de France, Giro, Vuelta e mais

⚙️ FUNCIONALIDADES
• Notificações personalizáveis
• Tema escuro automático
• Login com Google
• 100% gratuito

Desenvolvido por ciclistas, para ciclistas! 🇵🇹
```

### Keywords
```
ciclismo, portugal, btt, gravel, estrada, provas, calendário,
fantasy, corridas, worldtour, tour de france, notícias, bike
```

---

## 🚀 Deployment Steps

### Phase 1: Add Missing Ad Placements (1-2 hours)
1. Add BannerAdView to RaceDetailsScreen
2. Add BannerAdView to CyclistDetailScreen
3. Add BannerAdView to LeaguesScreen
4. Implement interstitial ad on team creation completion

### Phase 2: Pre-Launch Testing (1 hour)
1. Build release APK: `./gradlew assembleRelease`
2. Install and test all features
3. Verify ads load correctly (use AdMob test device)
4. Test GDPR consent flow
5. Test Google Sign-In flow

### Phase 3: Prepare Store Assets (2-3 hours)
1. Create 512x512 app icon
2. Create 1024x500 feature graphic
3. Take 5-8 screenshots of main features
4. Write privacy policy
5. Host privacy policy online

### Phase 4: Google Play Console Setup (1 hour)
1. Create app in Google Play Console
2. Fill store listing
3. Complete content rating questionnaire
4. Fill data safety form
5. Upload AAB (App Bundle)

### Phase 5: Launch
1. Submit for review (Internal Testing first)
2. Fix any review issues
3. Promote to Production track
4. Monitor crash reports and reviews

---

## 📋 Data Safety Declaration

### Data Collected
| Data Type | Collected | Shared | Purpose |
|-----------|-----------|--------|---------|
| Email address | Yes | No | Account authentication |
| Profile photo | Yes (Google) | No | User profile display |
| Device identifiers | Yes | Yes (AdMob) | Advertising |
| App interactions | Yes | Yes (Firebase) | Analytics |
| Crash logs | Yes | Yes (Firebase) | Crash reporting |

### Data Practices
- Data encrypted in transit ✅
- Users can request data deletion ✅
- Data used for advertising ✅

---

## 💰 Revenue Optimization Tips

### Banner Ad Best Practices
- Place below scroll fold (don't obstruct content)
- Use BANNER size (320x50) for phones
- Load ads when screen appears (already implemented)

### Interstitial Best Practices
- Show after natural break points
- Don't show more than 1 per 2 minutes
- Always preload next ad after showing

### Expected Revenue (estimates)
- Banner CPM: €0.50-2.00
- Interstitial CPM: €2.00-8.00
- Rewarded CPM: €5.00-15.00

With current implementation (7 banner placements):
- 1000 DAU × 3 ad views × €1.00 CPM = ~€3/day = ~€90/month

With optimized implementation (10 banners + interstitials):
- Potential: €150-300/month with 1000 DAU

---

## ⚠️ Pre-Launch Warnings

1. **API Keys Exposed**: YouTube API key is in build.gradle - consider moving to server
2. **Cleartext Traffic**: `usesCleartextTraffic="true"` - review if needed for release
3. **Test Thoroughly**: Fantasy features must work perfectly to avoid bad reviews
4. **Privacy Policy**: REQUIRED for apps with ads and login - must be hosted online

---

## 📞 Support Info

- **Developer Email**: app.cyclingai@gmail.com
- **GitHub**: https://github.com/brunomigmarques/CiclismoPortugal
