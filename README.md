# Ciclismo Portugal - App Android

Aplicação Android nativa para pesquisar provas de ciclismo em Portugal, com calendário pessoal e sistema de notificações.

## Funcionalidades Implementadas

✅ **Arquitetura Completa**
- Clean Architecture com MVVM
- Dependency Injection com Hilt
- Room Database para persistência offline
- Kotlin Coroutines + Flow

✅ **Scraping de Dados**
- Sistema modular de web scraping
- Suporte para múltiplas fontes (FPC, Ciclismo em Portugal)
- Dados de exemplo incluídos

✅ **Sincronização Automática**
- WorkManager para sync periódica (24h)
- Pull-to-refresh na interface
- Limpeza automática de provas antigas

✅ **Sistema de Notificações**
- Notificações de novas provas
- Lembretes de provas no calendário
- Avisos de prazos de inscrição

✅ **Interface Moderna**
- Jetpack Compose com Material 3
- Dark theme automático
- Navegação bottom bar
- Busca e filtros

✅ **Monetização**
- Integração com Google AdMob
- Banner ads (IDs de teste incluídos)
- Estrutura para ads nativos e intersticiais

## Requisitos

- Android Studio Hedgehog (2023.1.1) ou superior
- JDK 17
- Android SDK 26+
- Gradle 8.2

## Como Compilar e Executar

### 1. Abrir o Projeto

```bash
cd C:\Users\Bruno\AndroidStudioProjects\CiclismoPortugal
```

Abra o projeto no Android Studio:
- File → Open → Selecione a pasta `CiclismoPortugal`
- Aguarde o Gradle sync completar

### 2. Configurar Emulador ou Dispositivo

**Opção A: Emulador**
- Tools → Device Manager → Create Device
- Selecione um dispositivo (ex: Pixel 6)
- Selecione Android 13 (API 33) ou superior
- Finish

**Opção B: Dispositivo Físico**
- Ative "Opções de programador" no dispositivo
- Ative "Depuração USB"
- Conecte o dispositivo via USB

### 3. Executar a Aplicação

No Android Studio:
- Clique no botão "Run" (▶️) ou pressione `Shift + F10`
- Selecione o dispositivo/emulador
- Aguarde a compilação e instalação

Ou via linha de comando:

```bash
# Windows
gradlew.bat assembleDebug
gradlew.bat installDebug

# Linux/Mac
./gradlew assembleDebug
./gradlew installDebug
```

## Estrutura do Projeto

```
app/src/main/
├── java/com/ciclismo/portugal/
│   ├── data/
│   │   ├── local/          # Room Database
│   │   │   ├── dao/        # Data Access Objects
│   │   │   └── entity/     # Entidades
│   │   ├── remote/         # Web Scraping
│   │   │   └── scrapers/   # Scrapers por fonte
│   │   └── repository/     # Repositories
│   ├── domain/
│   │   ├── model/          # Modelos de domínio
│   │   └── usecase/        # Use cases
│   ├── presentation/
│   │   ├── home/           # Tela principal
│   │   ├── details/        # Detalhes da prova
│   │   ├── calendar/       # Calendário pessoal
│   │   ├── ads/            # AdMob
│   │   ├── navigation/     # Navegação
│   │   └── theme/          # Tema Compose
│   ├── workers/            # WorkManager
│   ├── notifications/      # Notificações
│   ├── di/                 # Hilt modules
│   ├── CiclismoApp.kt     # Application class
│   └── MainActivity.kt     # Activity principal
└── res/                    # Recursos (strings, colors, etc)
```

## Próximos Passos (Melhorias Futuras)

### 1. Implementar Scraping Real

Os scrapers atuais retornam dados de exemplo. Para implementar scraping real:

```kotlin
// Em FPCScraper.kt, substituir a função scrapeProvas()
override suspend fun scrapeProvas(): Result<List<ProvaEntity>> = withContext(Dispatchers.IO) {
    try {
        val document: Document = Jsoup.connect(baseUrl + "calendario")
            .timeout(10000)
            .get()

        val provas = mutableListOf<ProvaEntity>()

        document.select(".prova-item").forEach { element ->
            val nome = element.select(".nome").text()
            val data = parseDate(element.select(".data").text())
            // ... extrair mais campos

            provas.add(ProvaEntity(...))
        }

        Result.success(provas)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

### 2. Configurar AdMob Real

1. Criar conta em https://admob.google.com
2. Criar app e ad units
3. Substituir IDs de teste em:
   - `AndroidManifest.xml` (APPLICATION_ID)
   - `presentation/ads/AdManager.kt` (AD_UNIT_IDs)

### 3. Adicionar Filtros Avançados

Criar tela de filtros completa em `presentation/filters/`:
- Filtro por tipo de prova
- Filtro por região
- Intervalo de datas
- Distância

### 4. Implementar Rewarded Ads

Para exportação de calendário e funcionalidades premium:

```kotlin
// Em presentation/ads/RewardedAdManager.kt
class RewardedAdManager {
    fun showRewardedAd(
        activity: Activity,
        onRewardEarned: () -> Unit
    ) {
        // Implementar lógica de rewarded ad
    }
}
```

### 5. Adicionar Testes

```bash
# Testes unitários
./gradlew test

# Testes instrumentados
./gradlew connectedAndroidTest
```

### 6. Preparar para Publicação

1. **Gerar keystore:**
```bash
keytool -genkey -v -keystore ciclismo-portugal.keystore -alias ciclismo -keyalg RSA -keysize 2048 -validity 10000
```

2. **Configurar signing em `app/build.gradle.kts`:**
```kotlin
signingConfigs {
    create("release") {
        storeFile = file("ciclismo-portugal.keystore")
        storePassword = "sua_senha"
        keyAlias = "ciclismo"
        keyPassword = "sua_senha"
    }
}
```

3. **Gerar APK de release:**
```bash
./gradlew assembleRelease
```

4. **Publicar na Google Play Store:**
   - Criar conta de desenvolvedor
   - Preparar assets (ícone, screenshots, descrição)
   - Upload do APK/AAB
   - Configurar listagem da loja

## Permissões Necessárias

A app solicita as seguintes permissões:

- `INTERNET` - Para sincronização de dados
- `ACCESS_NETWORK_STATE` - Para verificar conectividade
- `POST_NOTIFICATIONS` - Para enviar notificações (Android 13+)
- `AD_ID` - Para anúncios personalizados

## Troubleshooting

### Erro de compilação do Gradle

```bash
# Limpar build
./gradlew clean

# Invalidar cache do Android Studio
File → Invalidate Caches → Invalidate and Restart
```

### Erro de sincronização do Room

```kotlin
// Forçar recreação do banco de dados
Room.databaseBuilder(...)
    .fallbackToDestructiveMigration()
    .build()
```

### Anúncios não aparecem

- Verificar conexão internet
- Aguardar alguns minutos (ads podem demorar)
- Verificar logs: `adb logcat | grep Ads`
- Confirmar que está usando IDs de teste

## Suporte

Para problemas ou sugestões:
- GitHub Issues (quando disponível)
- Email do desenvolvedor

## Licença

Copyright © 2026 - Aplicação desenvolvida para ciclistas em Portugal

---

**Nota:** Esta é uma aplicação de demonstração. Os scrapers incluem dados de exemplo e devem ser adaptados aos sites reais. Consulte os sites oficiais das organizações para confirmar informações sobre provas.
