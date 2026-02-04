# Keystore para Release

## Criar Keystore

```bash
keytool -genkey -v -keystore release.keystore -keyalg RSA -keysize 2048 -validity 10000 -alias ciclismoportugal
```

Guarda as passwords num local seguro!

## Adicionar ao build.gradle.kts

Adiciona isto ao `app/build.gradle.kts` dentro do bloco `android {}`:

```kotlin
signingConfigs {
    create("release") {
        storeFile = file("../keystore/release.keystore")
        storePassword = "YOUR_STORE_PASSWORD"
        keyAlias = "ciclismoportugal"
        keyPassword = "YOUR_KEY_PASSWORD"
    }
}

buildTypes {
    release {
        signingConfig = signingConfigs.getByName("release")
        // ... resto da config
    }
}
```

**IMPORTANTE:** Para produção, usa variáveis de ambiente ou `local.properties` para as passwords:

```kotlin
// No build.gradle.kts
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

signingConfigs {
    create("release") {
        storeFile = file(keystoreProperties["storeFile"] as String)
        storePassword = keystoreProperties["storePassword"] as String
        keyAlias = keystoreProperties["keyAlias"] as String
        keyPassword = keystoreProperties["keyPassword"] as String
    }
}
```

E cria um ficheiro `keystore.properties` na raiz (adiciona ao .gitignore!):

```properties
storeFile=keystore/release.keystore
storePassword=YOUR_STORE_PASSWORD
keyAlias=ciclismoportugal
keyPassword=YOUR_KEY_PASSWORD
```

## Obter SHA-1/SHA-256 do Release

Depois de criar o keystore:

```bash
keytool -list -v -keystore release.keystore -alias ciclismoportugal
```

Adiciona estes fingerprints ao Firebase Console!
