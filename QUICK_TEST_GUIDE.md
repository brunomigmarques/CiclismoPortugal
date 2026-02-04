# Quick DS Assistant Test Guide

## Test 1: Contextual Suggestions (Should Work)

### A. No Captain Trigger
1. Go to Fantasy Hub
2. Make sure you have a team with cyclists but NO captain set
3. Navigate to "Minha Equipa" screen
4. **Expected:** Card appears: "Capitão em falta!"

**Check logs:**
```bash
adb logcat | grep -E "AiCoordinator|AiTriggerEngine"
```

Look for:
```
AiCoordinator: Context: teamSize=10, hasCaptain=false
AiTriggerEngine: NO_CAPTAIN: should=true
AiTriggerEngine: ✅ Active triggers: [NO_CAPTAIN]
AiCoordinator: ✅ NEW SUGGESTION: Capitão em falta!
```

### B. First Visit Tutorial
1. Clear app data OR uninstall/reinstall
2. Go to Fantasy Hub → "Mercado"
3. **Expected:** Mini tip: "Bem-vindo ao Mercado!"

**Check logs:**
```
AiCoordinator: isFirstVisit: true
AiTriggerEngine: FIRST_VISIT_MARKET: should=true
```

---

## Test 2: AI Chat Actions (May Not Work - Gemini Issue)

### Step 1: Open AI Chat
1. Tap FAB button (bottom-right)
2. Chat window opens

### Step 2: Send Test Messages

**Test A: Navigation Request**
- Type: `"Mostra-me o mercado de ciclistas"`
- **Expected:** Response + button "Ir ao Mercado"
- **If no button:** Check logs below

**Test B: Action Request**
- Type: `"Quem devo comprar para a próxima corrida?"`
- **Expected:** Response + button "Ver Mercado" or "Comprar Ciclista"

**Test C: Captain Request**
- Type: `"Como escolho o capitão?"`
- **Expected:** Response + button "Ver Equipa" or "Definir Capitão"

### Step 3: Check Action Logs

```bash
adb logcat | grep AiResponseParser
```

**Good (Working):**
```
AiResponseParser: Found JSON block, parsing...
AiResponseParser: Pattern detection found 2 actions
AiResponseParser: Action - NAVIGATE_TO: Ir ao Mercado
```

**Problem (Not Working):**
```
AiResponseParser: No JSON block found
AiResponseParser: Pattern detection found no actions
AiResponseParser: using fallback...
```

---

## Test 3: Action Execution

If buttons appear in chat:

1. Click the action button (e.g., "Ir ao Mercado")
2. **Expected:** App navigates to Market screen

**Check execution logs:**
```bash
adb logcat | grep "AiActionExecutor"
```

Look for:
```
AiActionExecutor: Executing action: NAVIGATE_TO
AiActionExecutor: Action context: userId=abc123, teamId=team456
```

---

## Common Issues & Solutions

### Issue 1: No contextual suggestions appearing

**Diagnosis:**
```bash
adb logcat | grep "AiCoordinator: =========="
```

**Possible causes:**
- `hasAiAccess: false` → Trial expired or premium check failing
- `teamSize=0` → Team not loading
- All triggers show `should=false` → No trigger conditions met

**Solution:**
Check team status:
```bash
adb logcat | grep "Team context: size"
```

Should see: `Team context: size=10, hasCaptain=false`

---

### Issue 2: AI chat responds but no action buttons

**This is EXPECTED** - Gemini Nano may not generate proper JSON.

**Workaround:** The parser has fallback patterns. Use these keywords:
- "mercado" → Triggers navigation to Market
- "equipa" → Triggers navigation to Team
- "comprar" or "vender" → Triggers Market navigation
- "capitão" → Triggers Team navigation
- "wildcard" → Triggers wildcard action

**Example that should work:**
Instead of: `"Qual ciclista é melhor?"`
Try: `"Vai ao mercado e mostra ciclistas"` (includes "mercado" keyword)

---

### Issue 3: Buttons appear but don't work

**Check:**
```bash
adb logcat | grep "executeAction"
```

**Common errors:**
- `userId is empty` → User not logged in
- `teamId is null` → Team not created
- `RequiresAuth` → Need to login first

---

## Full Log Command

To see everything at once:
```bash
adb logcat | grep -E "AiCoordinator|AiTriggerEngine|AiResponseParser|AiActionExecutor|AiOverlayVM"
```

---

## Expected Results Summary

✅ **Should Work:**
- Contextual suggestions (cards, mini tips)
- First-visit tutorials
- No captain warning
- Transfer penalty warnings

❓ **May Not Work (Gemini Dependent):**
- AI chat action buttons
- Requires Gemini to generate JSON
- Fallback patterns may help

✅ **Should Work IF Buttons Appear:**
- Action execution (navigation, buy/sell, etc.)
- User/team context passed correctly
