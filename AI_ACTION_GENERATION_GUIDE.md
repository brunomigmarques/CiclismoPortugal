# AI Action Generation Guide

## Why Actions May Not Appear

The DS Assistant chat has **two ways** to show actionable buttons:

### 1. **JSON Action Blocks** (Primary Method)
The AI is instructed to include JSON at the end of responses:
```json
{
  "actions": [
    {
      "type": "navigate_to",
      "title": "Ir ao Mercado",
      "description": "Ver ciclistas disponíveis",
      "parameters": {"route": "fantasy/market"}
    }
  ]
}
```

### 2. **Pattern Detection** (Fallback)
If no JSON is found, the parser detects keywords:
- "comprar" / "vender" → Navigate to Market
- "capitão" → Set captain action
- "wildcard" → Use wildcard
- Screen names → Navigate to that screen

## Check Logs to Diagnose

Filter logcat for `AiResponseParser`:
```bash
adb logcat | grep AiResponseParser
```

### Expected Logs (Working):
```
AiResponseParser: Parsing response (250 chars)
AiResponseParser: Found JSON block, parsing...
AiResponseParser: Pattern detection found 2 actions
AiResponseParser: Action - NAVIGATE_TO: Ir ao Mercado
```

### Problem Indicators:
```
AiResponseParser: No JSON block found
AiResponseParser: Pattern detection found no actions
AiResponseParser: using fallback...
```

## How to Improve Action Generation

### Option 1: Verify Gemini API is Working

The app uses **Gemini Nano** (on-device AI). Check:
1. Device supports Gemini Nano
2. API key is valid
3. No quota/rate limit issues

Check logs for:
```bash
adb logcat | grep AiService
```

Look for errors like:
- "API quota exceeded"
- "Model not available"
- "Network error"

### Option 2: Test with Example Queries

Try these prompts that **should** generate actions:

**Good prompts (likely to generate actions):**
- "Quem devo comprar para a próxima corrida?" → Should suggest BUY_CYCLIST or NAVIGATE_TO market
- "Como posso melhorar a minha equipa?" → Should suggest NAVIGATE_TO team
- "Qual ciclista devo escolher como capitão?" → Should suggest SET_CAPTAIN
- "Mostra-me o calendário" → Should suggest NAVIGATE_TO calendar

**Poor prompts (less likely to generate actions):**
- "Olá" → Generic greeting, no action
- "Obrigado" → Acknowledgment, no action
- "Explica-me as regras" → Informational, may not generate action

### Option 3: Force Fallback Actions

If JSON parsing fails, the parser has fallback patterns. To trigger them:
1. Mention screen names: "Mercado", "Equipa", "Calendário"
2. Mention actions: "comprar ciclista", "definir capitão"
3. Use keywords: "transferência", "wildcard"

### Option 4: Check Action Display in UI

Actions appear as **buttons below the AI message**. Verify in `AiGlobalOverlay.kt`:

```kotlin
// Actions should appear here
if (message.actions.isNotEmpty()) {
    ActionButtons(
        actions = message.actions,
        onActionClick = { action ->
            chatViewModel.executeAction(action)
        }
    )
}
```

## Manual Test

1. Open DS Assistant (FAB button)
2. Type: "Mostra-me o mercado de ciclistas"
3. Expected: AI responds + button "Ir ao Mercado" appears
4. Click button → Should navigate to Market screen

## Common Issues

### Issue: No buttons appear
**Diagnosis:**
```bash
adb logcat | grep "AiResponseParser"
```

**Possible causes:**
1. AI not generating JSON → Check Gemini API logs
2. JSON malformed → Parser falls back to pattern detection
3. No patterns matched → Fallback actions used
4. UI not rendering buttons → Check `ActionButtons` composable

### Issue: Buttons appear but don't work
**Diagnosis:**
```bash
adb logcat | grep "AiActionExecutor"
```

**Possible causes:**
1. No user logged in → userId is empty
2. No team → teamId is null
3. Action parameters invalid → Check action.parameters map

### Issue: Wrong actions suggested
**Problem:** AI suggests irrelevant actions

**Solution:**
The AI prompt includes context about user's team, next race, etc. If context is wrong:
1. Check `buildUserContext()` in `AiAssistantViewModel`
2. Verify team data is loading correctly
3. Check race data is current

## Improve Action Generation Prompt

If actions are still not generating, you can strengthen the prompt in `AiPromptTemplates.kt`:

Find the system prompt (around line 348) and emphasize:
```
REGRA CRÍTICA: SEMPRE inclui um bloco ```json com pelo menos UMA ação!

Exemplos de ações que DEVES sugerir:
- Mencionar "mercado" → navigate_to com route "fantasy/market"
- Mencionar "equipa" → navigate_to com route "fantasy/team"
- Sugerir comprar ciclista → buy_cyclist com cyclistId
- Recomendar capitão → set_captain com cyclistId

NÃO respondas SEM incluir ações quando falares de funcionalidades!
```

## Verify Actions are Executing

When user clicks an action button:

1. **Check execution log:**
```bash
adb logcat | grep "AiActionExecutor: Executing action"
```

2. **Expected flow:**
```
AiActionExecutor: Executing action: NAVIGATE_TO - Ir ao Mercado
AiActionExecutor: Action context: userId=abc123, teamId=team456
AiActionResult: NavigateTo("fantasy/market")
```

3. **Check navigation:**
```bash
adb logcat | grep "navigationEvent"
```

Should see:
```
AiOverlayViewModel: navigationEvent: fantasy/market
```

## Summary

DS Assistant actions require:
✅ AI generates JSON with actions (or patterns match)
✅ Parser extracts actions correctly
✅ UI renders action buttons
✅ User has context (logged in, has team)
✅ Action executor has proper userId/teamId

Most common issue: **AI not generating JSON** → Check Gemini API status
