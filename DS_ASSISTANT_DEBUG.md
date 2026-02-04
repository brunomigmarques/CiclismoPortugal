# DS Assistant Debug Guide

## Check if DS Assistant is working

### 1. Check Logcat for Debug Messages

Filter logcat by these tags:
```
AiCoordinator
AiTriggerEngine
AiOverlayVM
```

### 2. Expected Log Messages

When navigating to a screen, you should see:
```
AiCoordinator: Screen changed: fantasy/team
AiCoordinator: Team context: size=10, hasCaptain=false
AiCoordinator: Context refreshed: team=MyTeam, nextRace=Volta a Portugal
AiTriggerEngine: Active triggers: [NO_CAPTAIN]
AiOverlayVM: Showing suggestion: EXPANDABLE_CARD - Capitão em falta!
```

### 3. Check Premium/Trial Status

The DS Assistant requires premium access or an active trial. Check logs for:
```
AiCoordinator: No AI access, skipping triggers
```

If you see this, the trial may have expired or not started. To check:
- Trial starts on first AI usage
- Trial lasts 7 days
- After trial: requires premium

### 4. Manual Testing

To test specific triggers:

**No Captain Trigger:**
1. Go to Fantasy Hub → Create team
2. Add 1+ cyclists but DON'T set captain
3. Navigate to "Minha Equipa" screen
4. Should see: "Capitão em falta!" suggestion card

**Incomplete Team Trigger:**
1. Have a team with < 15 cyclists
2. Have an upcoming race in < 48 hours
3. Navigate to any screen
4. Should see: "Equipa incompleta" suggestion

**First Visit Tutorial:**
1. Clear app data to reset
2. Navigate to Market screen for first time
3. Should see: "Bem-vindo ao Mercado!" mini tip

**Transfer Penalty:**
1. Go to Market
2. Queue 3+ transfers (more than free transfers)
3. Should see: "Penalização de transferências" mini tip

### 5. Force Trigger Evaluation

If suggestions aren't appearing, try:
1. Navigate away and back to the screen
2. Wait 30 seconds for idle trigger
3. Check if premium/trial is active

### 6. Check if Overlay is Visible

The AiGlobalOverlay should show a FAB (Floating Action Button) in the bottom-right corner of most screens. If you don't see it:
- Check if you're on an excluded screen (onboarding, video player)
- Check NavGraph.kt hideAiOverlay logic

## Common Issues

### Issue: No suggestions appearing
**Causes:**
1. Premium/trial expired → Check premium status
2. No team created → Create a fantasy team first
3. Context not meeting trigger conditions → Check team size, captain, etc.
4. Triggers dismissed → Wait 24h or clear app data

### Issue: Actions not executing
**Causes:**
1. No user logged in → userId is empty
2. No team → teamId is null
3. Network error → Check Firebase/Firestore connection

### Issue: Crashes
**Causes:**
1. Dependencies not injected → Check AiModule.kt
2. Repository errors → Check database/Firestore queries

## Log Commands

```bash
# Filter by AI tags
adb logcat | grep -E "AiCoordinator|AiTriggerEngine|AiOverlayVM"

# Check premium status
adb logcat | grep PremiumManager

# Full AI logs
adb logcat *:S AiCoordinator:D AiTriggerEngine:D AiOverlayVM:D
```
