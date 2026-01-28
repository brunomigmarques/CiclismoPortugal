# Guia de Registo da App no Google AdMob Console

Este guia ajuda a registar a aplicação Ciclismo Portugal no Google AdMob para ativar a monetização com anúncios reais.

## 1. Pré-requisitos

- Conta Google (Gmail)
- App já publicada ou em fase de teste na Google Play Console
- Package name da app: `com.ciclismo.portugal`

## 2. Aceder ao AdMob Console

1. Aceda a: **https://admob.google.com/**
2. Faça login com a sua conta Google
3. Se é a primeira vez, aceite os Termos de Serviço do AdMob

## 3. Criar ou Selecionar Conta AdMob

### Se é a primeira vez:
1. Clique em **"Get Started"** ou **"Começar"**
2. Escolha o país/região (Portugal)
3. Escolha a moeda (EUR - Euro)
4. Aceite os termos de serviço
5. Configure as preferências de pagamento (pode fazer depois)

### Se já tem conta:
1. Selecione a conta existente
2. Vá para o Dashboard

## 4. Adicionar a sua App

### Passo 1: Adicionar App
1. No menu lateral, clique em **"Apps"**
2. Clique no botão **"+ ADD APP"** ou **"+ ADICIONAR APP"**

### Passo 2: Selecionar Plataforma
1. Selecione **"Android"**
2. Responda se a app já está na Google Play Store:
   - **Sim**: Se já publicou (mesmo em teste interno/fechado)
   - **Não**: Se ainda não publicou

### Passo 3: Configurar App
Se respondeu **Sim** à Google Play Store:
1. Pesquise pelo package name: `com.ciclismo.portugal`
2. Ou pesquise pelo nome: "Ciclismo Portugal"
3. Selecione a app da lista

Se respondeu **Não**:
1. Digite o nome da app: **"Ciclismo Portugal"**
2. Clique em **"ADD"** ou **"ADICIONAR"**

### Passo 4: Configurações Iniciais
1. **Enable user metrics**: Ative para ver estatísticas de utilizadores
2. **Enable GDPR compliance**: **ATIVE OBRIGATORIAMENTE** (para conformidade GDPR na Europa)
3. **Enable COPPA compliance**: Configure se a app for dirigida a crianças (não aplicável)

## 5. Criar Unidades de Anúncio

Agora que a app está registada, precisa criar as unidades de anúncio específicas.

### Criar Banner Ad Unit

1. Dentro da sua app, clique em **"Ad units"** ou **"Unidades de anúncio"**
2. Clique em **"+ ADD AD UNIT"** ou **"+ ADICIONAR UNIDADE DE ANÚNCIO"**
3. Selecione **"Banner"**
4. Configure:
   - **Ad unit name**: `Home_Banner` ou `News_Banner`
   - **Advanced settings**: deixe padrão
5. Clique em **"CREATE AD UNIT"** ou **"CRIAR UNIDADE DE ANÚNCIO"**
6. **GUARDE O AD UNIT ID** que aparece (formato: `ca-app-pub-XXXXXXXXXXXXXXXX/YYYYYYYYYY`)

### Criar Interstitial Ad Unit (Opcional)

1. Clique novamente em **"+ ADD AD UNIT"**
2. Selecione **"Interstitial"**
3. Configure:
   - **Ad unit name**: `App_Interstitial`
4. Clique em **"CREATE AD UNIT"**
5. **GUARDE O AD UNIT ID**

### Criar Rewarded Ad Unit (Opcional para funcionalidades premium)

1. Clique em **"+ ADD AD UNIT"**
2. Selecione **"Rewarded"**
3. Configure:
   - **Ad unit name**: `Calendar_Export_Reward`
4. Clique em **"CREATE AD UNIT"**
5. **GUARDE O AD UNIT ID**

## 6. Obter o App ID

1. No menu lateral, clique em **"Apps"**
2. Selecione a sua app "Ciclismo Portugal"
3. Clique em **"App settings"** ou **"Definições da app"**
4. Encontre o **App ID** (formato: `ca-app-pub-XXXXXXXXXXXXXXXX~ZZZZZZZZZZ`)
5. **COPIE ESTE ID**

## 7. Atualizar a Aplicação com IDs Reais

### Passo 1: Atualizar AndroidManifest.xml

Abra: `app/src/main/AndroidManifest.xml`

Localize a secção `<application>` e adicione/substitua:

```xml
<application>
    <!-- ... outros elementos ... -->

    <meta-data
        android:name="com.google.android.gms.ads.APPLICATION_ID"
        android:value="ca-app-pub-XXXXXXXXXXXXXXXX~ZZZZZZZZZZ"/>

    <!-- ... resto do manifest ... -->
</application>
```

Substitua `ca-app-pub-XXXXXXXXXXXXXXXX~ZZZZZZZZZZ` pelo seu **App ID** real.

### Passo 2: Atualizar AdManager.kt

Abra: `app/src/main/java/com/ciclismo/portugal/presentation/ads/AdManager.kt`

Substitua os IDs de teste pelos IDs reais:

```kotlin
companion object {
    // PRODUÇÃO - Substitua pelos seus Ad Unit IDs reais
    const val BANNER_AD_UNIT_ID = "ca-app-pub-XXXXXXXXXXXXXXXX/YYYYYYYYYY"
    const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-XXXXXXXXXXXXXXXX/YYYYYYYYYY"
    const val REWARDED_AD_UNIT_ID = "ca-app-pub-XXXXXXXXXXXXXXXX/YYYYYYYYYY"
}
```

### Passo 3: Build e Deploy

1. Compile a nova versão:
   ```bash
   ./gradlew.bat assembleRelease
   ```

2. Teste em modo release primeiro
3. Publique na Play Store

## 8. Configurar Pagamentos (Importante!)

1. No AdMob Console, vá para **"Payments"** ou **"Pagamentos"**
2. Configure:
   - **Informações fiscais**: Preencha com os seus dados
   - **Método de pagamento**: Adicione conta bancária
   - **Limite de pagamento**: Mínimo €70 (padrão do AdMob)

**NOTA**: Só receberá pagamentos após configurar corretamente esta secção!

## 9. Consentimento GDPR (OBRIGATÓRIO para Europa)

A app já tem o SDK UMP (User Messaging Platform) incluído. Para ativar:

1. No AdMob Console, vá para **"Privacy & messaging"**
2. Clique em **"Create message"** ou **"Criar mensagem"**
3. Configure:
   - Selecione **"European Economic Area"** (EEA)
   - Escolha template de consentimento GDPR
   - Ative **"Consent"** para publicidade personalizada
4. Publique a mensagem de consentimento

Depois, adicione código de inicialização no CiclismoApp.kt:

```kotlin
// Adicionar imports
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

// No onCreate do CiclismoApp
override fun onCreate() {
    super.onCreate()

    // Configurar consentimento GDPR
    val params = ConsentRequestParameters.Builder()
        .setTagForUnderAgeOfConsent(false)
        .build()

    val consentInformation = UserMessagingPlatform.getConsentInformation(this)
    consentInformation.requestConsentInfoUpdate(
        this,
        params,
        { /* Consentimento pronto */ },
        { /* Erro ao obter consentimento */ }
    )

    setupWorkManager()
}
```

## 10. Verificar Integração

1. Publique a app na Play Store (mesmo em teste interno)
2. No AdMob Console, vá para **"Apps"** → sua app
3. Verifique se aparecem dados de impressões em 24-48 horas
4. Se não aparecer nada, verifique os logs do Logcat:
   ```
   adb logcat -s Ads
   ```

## 11. Políticas Importantes do AdMob

⚠️ **ATENÇÃO**: Violações podem resultar em suspensão da conta!

### NÃO PERMITIDO:
- ❌ Clicar nos próprios anúncios
- ❌ Pedir a amigos/família para clicar
- ❌ Implementar anúncios em apps que violam políticas
- ❌ Colocar mais de 1 banner visível ao mesmo tempo
- ❌ Anúncios em conteúdo impróprio

### RECOMENDADO:
- ✅ Testar sempre com IDs de teste primeiro
- ✅ Implementar anúncios de forma não intrusiva
- ✅ Respeitar a experiência do utilizador
- ✅ Seguir as diretrizes de UX do AdMob
- ✅ Configurar GDPR corretamente

## 12. Estimativa de Receitas

### Fórmula básica:
**Receita = Impressões × CTR × CPC**

- **Impressões**: Número de vezes que o anúncio é mostrado
- **CTR** (Click-Through Rate): Taxa de cliques (média: 1-3%)
- **CPC** (Cost Per Click): Custo por clique (média em PT: €0.10 - €0.50)

### Exemplo:
- 10.000 utilizadores ativos/mês
- 5 sessões por utilizador
- 2 banners por sessão
- = 100.000 impressões/mês
- CTR de 2% = 2.000 cliques
- CPC de €0.20 = **€400/mês** (estimativa)

## 13. Checklist Final

Antes de publicar em produção:

- [ ] App registada no AdMob Console
- [ ] App ID adicionado ao AndroidManifest.xml
- [ ] Banner Ad Units criadas
- [ ] IDs de teste substituídos por IDs reais no AdManager.kt
- [ ] Consentimento GDPR configurado
- [ ] Métodos de pagamento configurados
- [ ] App testada em modo release
- [ ] Logs verificados (sem erros de anúncios)
- [ ] App publicada na Play Store

## 14. Links Úteis

- **AdMob Console**: https://admob.google.com/
- **Políticas do AdMob**: https://support.google.com/admob/answer/6128543
- **Guia GDPR**: https://support.google.com/admob/answer/9997589
- **Central de Ajuda**: https://support.google.com/admob

## 15. Suporte

Se tiver problemas:

1. Verifique a [Central de Ajuda do AdMob](https://support.google.com/admob)
2. Consulte os logs de erro no Logcat
3. Verifique se a app está aprovada na Play Store
4. Aguarde 24-48h para os primeiros dados aparecerem

---

**Data de criação**: Janeiro 2026
**Versão da app**: 1.0.0
**Package**: com.ciclismo.portugal
