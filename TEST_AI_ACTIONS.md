# Test AI Actions - GUARANTEED Test Cases

## ✅ GUARANTEED Tests (New Simple Keyword System)

These use the new `addSimpleKeywordActions()` system that triggers on single keywords BEFORE any complex pattern matching.

### Test 1: Market (GUARANTEED) ✅

**Message:** `"mercado"`

**Expected Button:** "Ir ao Mercado"

**Why it WILL work:**
- Simple keyword "mercado" triggers addSimpleKeywordActions()
- Runs FIRST, before complex patterns
- No AI JSON parsing needed

**Log command:**
```bash
adb logcat | grep "addSimpleKeywordActions"
```

**Expected log:**
```
AiResponseParser: addSimpleKeywordActions: ✅ Added Market action (keyword: mercado/comprar/vender)
```

---

### Test 2: Buy Cyclist (GUARANTEED) ✅

**Message:** `"comprar"`

**Expected Button:** "Ir ao Mercado"

**Why it WILL work:**
- Keyword "comprar" triggers Market action

**Expected log:**
```
AiResponseParser: addSimpleKeywordActions: ✅ Added Market action (keyword: mercado/comprar/vender)
```

---

### Test 3: Team (GUARANTEED) ✅

**Message:** `"minha equipa"`

**Expected Button:** "Ver Equipa"

**Why it WILL work:**
- Exact phrase "minha equipa" matches

**Expected log:**
```
AiResponseParser: addSimpleKeywordActions: ✅ Added Team action (keyword: minha equipa)
```

---

### Test 4: Captain (GUARANTEED) ✅

**Message:** `"capitao"`

**Expected Button:** "Ver Equipa" or "Ajuda sobre Capitao"

**Why it WILL work:**
- Single keyword "capitao" triggers captain action

**Expected log:**
```
AiResponseParser: addSimpleKeywordActions: ✅ Added Captain action
```

---

### Test 5: Calendar (GUARANTEED) ✅

**Message:** `"calendario"`

**Expected Button:** "Ver Calendario"

**Why it WILL work:**
- Single keyword "calendario" triggers

**Expected log:**
```
AiResponseParser: addSimpleKeywordActions: ✅ Added Calendar action
```

---

### Test 6: Wildcard (GUARANTEED) ✅

**Message:** `"wildcard"`

**Expected Button:** "Ver Equipa" or "Ajuda sobre Wildcard"

**Expected log:**
```
AiResponseParser: addSimpleKeywordActions: ✅ Added Wildcard action
```

---

### Test 7: Fantasy Hub (GUARANTEED) ✅

**Message:** `"jogo fantasy"`

**Expected Button:** "Fantasy Hub"

**Expected log:**
```
AiResponseParser: addSimpleKeywordActions: ✅ Added Fantasy Hub action
```

---

### Test 8: Leagues (GUARANTEED) ✅

**Message:** `"liga"`

**Expected Button:** "Ver Ligas"

**Expected log:**
```
AiResponseParser: addSimpleKeywordActions: ✅ Added Leagues action
```

---

### Test 9: News (GUARANTEED) ✅

**Message:** `"noticia"`

**Expected Button:** "Ver Noticias"

**Expected log:**
```
AiResponseParser: addSimpleKeywordActions: ✅ Added News action
```

---

### Test 10: ULTIMATE FALLBACK ✅

**Message:** `"xyzabc123"` (random text)

**Expected Button:** "Explorar App"

**Why it WILL work:**
- Even with NO keywords, the ultimate fallback (line 44-58 in parseResponse) ALWAYS adds an action

**Expected log:**
```
AiResponseParser: ⚠️ CRITICAL: No actions after all parsing! Injecting guaranteed action...
AiResponseParser: ✅ Final result has 1 actions
AiResponseParser: Action - NAVIGATE_TO: Explorar App
```

---

## 🔍 How to Test

### Step 1: Clear App Data (Optional)
```bash
adb shell pm clear com.ciclismo.portugal
```

### Step 2: Open AI Chat
1. Launch app
2. Tap FAB (floating action button) in bottom-right
3. Chat window appears

### Step 3: Send Test Message
Type one of the GUARANTEED messages above (e.g., `"mercado"`)

### Step 4: Check Logs
```bash
adb logcat | grep -E "AiResponseParser|AiAssistantVM"
```

### Step 5: Verify Button Appears
- Look below AI response
- Should see "Acoes sugeridas:" label
- Should see action button (e.g., "Ir ao Mercado")

---

## 📊 Expected Log Flow

### Full Successful Flow:

```
AiAssistantVM: Sending message: mercado
AiService: Generating response with Gemini API
AiResponseParser: parseResponse: Parsing response (85 chars)
AiResponseParser: parseResponse: No JSON block found, using pattern detection...
AiResponseParser: parseTextResponse: Analyzing text for patterns...
AiResponseParser: parseTextResponse: After simple keyword detection: 1 actions
AiResponseParser: addSimpleKeywordActions: ✅ Added Market action (keyword: mercado/comprar/vender)
AiResponseParser: parseResponse: ✅ Final result has 1 actions
AiResponseParser: parseResponse: Action - NAVIGATE_TO: Ir ao Mercado
AiAssistantVM: ========== AI RESPONSE RECEIVED ==========
AiAssistantVM: Actions count: 1
AiAssistantVM: Action 0: NAVIGATE_TO - Ir ao Mercado
AiAssistantVM: ✅ Set 1 pending actions for user approval
```

---

## ❌ Problem Indicators

### Issue 1: No actions in logs

**Symptom:**
```
AiResponseParser: ✅ Final result has 0 actions
AiResponseParser: ❌❌❌ STILL NO ACTIONS! This should never happen!
```

**This should be IMPOSSIBLE now** with the new guaranteed system. If you see this, there's a critical bug.

---

### Issue 2: Actions parsed but not set in ViewModel

**Symptom:**
```
AiResponseParser: Action - NAVIGATE_TO: Ir ao Mercado
(but no log from AiAssistantVM about setting actions)
```

**Cause:** ViewModel not receiving response properly

**Fix:** Check if `aiService.chatWithActions()` is returning successfully

---

### Issue 3: Actions set but UI not showing

**Symptom:**
```
AiAssistantVM: ✅ Set 1 pending actions for user approval
(but no buttons visible in UI)
```

**Cause:** UI not rendering action buttons

**Check:**
1. `AiAssistantScreen.kt` line 519: `if (message.actions.isNotEmpty())`
2. Verify actions are being passed to ChatMessage
3. Check if ActionButton composable is rendering

---

## 🔧 Debug Commands

### Full logs:
```bash
adb logcat | grep -E "AiCoordinator|AiTriggerEngine|AiResponseParser|AiAssistantVM|AiOverlayVM"
```

### Action parsing only:
```bash
adb logcat | grep "AiResponseParser"
```

### Action execution:
```bash
adb logcat | grep "AiActionExecutor"
```

### UI rendering:
```bash
adb logcat | grep "Acoes sugeridas"
```

---

## 🎯 Success Criteria

For a test to PASS, you should see:

✅ **Step 1:** User sends message
```
AiAssistantVM: Sending message: mercado
```

✅ **Step 2:** Response parsed with actions
```
AiResponseParser: ✅ Final result has 1 actions
```

✅ **Step 3:** Actions set in ViewModel
```
AiAssistantVM: ✅ Set 1 pending actions for user approval
```

✅ **Step 4:** Button appears in chat
- Visual: Action button below AI response
- Text: "Ir ao Mercado" (or relevant action)

✅ **Step 5:** Button click works
```
AiActionExecutor: Executing action: NAVIGATE_TO
```

---

## 💡 Manual Action Injection (If All Else Fails)

If you want to test the UI independently of the AI, add this to `AiAssistantViewModel.kt` in the `sendMessage()` function:

```kotlin
// DEBUG: Force action injection
if (response.actions.isEmpty()) {
    Log.w(TAG, "⚠️ DEBUG: Injecting test action")
    val testAction = AiAction(
        id = UUID.randomUUID().toString(),
        type = AiActionType.NAVIGATE_TO,
        title = "Mercado (TEST)",
        description = "Test button",
        parameters = mapOf("route" to "fantasy/market"),
        priority = ActionPriority.NORMAL
    )

    val aiMessage = ChatMessage(
        id = UUID.randomUUID().toString(),
        content = response.message,
        isUser = false,
        timestamp = System.currentTimeMillis(),
        actions = listOf(testAction)  // Force test action
    )
    _messages.value = _messages.value + aiMessage
    _pendingActions.value = listOf(testAction)

    _uiState.value = AiAssistantUiState.Idle
    return@launch  // Exit early to avoid duplicate message
}
```

This will FORCE a button to appear every time, proving the UI works.

---

## 📝 Summary

With the new guaranteed keyword system:

1. **Simple keywords** (mercado, comprar, equipa, capitao) trigger actions IMMEDIATELY
2. **No AI JSON parsing needed** for basic queries
3. **Ultimate fallback** ALWAYS adds an action if all else fails
4. **Three-layer safety net:**
   - Layer 1: Simple keyword detection
   - Layer 2: Complex pattern matching
   - Layer 3: Topic-based fallback
   - Layer 4: ULTIMATE fallback (never empty)

**Bottom line:** Actions should ALWAYS appear for any query that contains relevant keywords!
