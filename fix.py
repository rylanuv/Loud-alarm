import re

path = 'e:/Apps/Loud Alarm - Solve2Wake/app/src/main/java/com/loud/alarm/ui/editor/AlarmEditorScreen.kt'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

replacements = [
    (r'ChallengeType\.NONE to \(".*?" to "None"\)', 'ChallengeType.NONE to ("💤" to "None")'),
    (r'ChallengeType\.MATH to \(".*?" to "Maths"\)', 'ChallengeType.MATH to ("🧮" to "Maths")'),
    (r'ChallengeType\.QR_CODE to \(".*?" to "QR Code"\)', 'ChallengeType.QR_CODE to ("🔳" to "QR Code")'),
    (r'ChallengeType\.REWRITE to \(".*?" to "Rewrite"\)', 'ChallengeType.REWRITE to ("✍️" to "Rewrite")'),
    (r'ChallengeType\.STEP to \(".*?" to "Steps"\)', 'ChallengeType.STEP to ("👣" to "Steps")'),
    (r'ChallengeType\.MAZE to \(".*?" to "Maze"\)', 'ChallengeType.MAZE to ("🕹️" to "Maze")'),
    (r'ChallengeType\.MEMORY to \(".*?" to "Memory"\)', 'ChallengeType.MEMORY to ("🧠" to "Memory")'),
    (r'ChallengeType\.SHAKE to \(".*?" to "Shake"\)', 'ChallengeType.SHAKE to ("📳" to "Shake")'),
    (r'ChallengeType\.TYPING to \(".*?" to "Typing"\)', 'ChallengeType.TYPING to ("⌨️" to "Typing")'),
    (r'ChallengeType\.PUZZLE to \(".*?" to "Puzzle"\)', 'ChallengeType.PUZZLE to ("🧩" to "Puzzle")'),
    (r'\"â–¸\"', '"▸"'),
    (r'Text\(".*?", fontSize = 22\.sp\)', 'Text("🔊", fontSize = 22.sp)'),
    (r'//\s*â”€.*?\n', '// ──────────────────────────────────────────────────\n'),
    (r'//\s*.*?\n', '// ──────────────────────────────────────────────────\n'),
    (r'\"200% volume â€” extra loud\"', '"200% volume — extra loud"'),
    (r'Text\(\n\s*".*?",\n\s*fontSize = 48\.sp\n\s*\)', 'Text(\n                    "📱",\n                    fontSize = 48.sp\n                )'),
    (r'"If you\'re notorious[\s\S]*?ring again!"', '"If you\'re notorious for turning off alarms and going back to sleep, this feature is for you!\\n\\nAfter you dismiss the alarm, a notification will appear. Tap it to confirm you\'re awake — if you don\'t tap within 5 minutes, the alarm will ring again!"'),
    (r'".*?" to "Alarm Rings"', '"⏰" to "Alarm Rings"'),
    (r'".*?" to "You dismiss it"', '"✅" to "You dismiss it"'),
    (r'".*?" to "Delay minutes pass\.\.\."', '"🛌" to "Delay minutes pass..."'),
    (r'".*?" to "Notification: Tap to confirm!"', '"📱" to "Notification: Tap to confirm!"'),
    (r'".*?" to "Tap within 5 min .*? Confirmed!"', '"👆" to "Tap within 5 min → Confirmed!"'),
    (r'".*?" to "Didn\'t tap\? Alarm rings again!"', '"🔔" to "Didn\'t tap? Alarm rings again!"')
]

original_content = content
for pattern, repl in replacements:
    content = re.sub(pattern, repl, content)

if content != original_content:
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
    print('Fixed formatting.')
else:
    print('No changes needed.')
