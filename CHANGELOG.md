## 0.4.0
- Fixed Origin stat bonuses counting toward the maximum selectable stats
- Made origin fallback checks only run on the logical server (should finally *actually* prevent layers being reselected)
- Locked Endless Quiver enchant behind Ranger class
Todo:
- Fixed the Fifth Feat (Level 20) being impossible to obtain in most cases
- Locked casting implement usage behind Wizard/Warlock/Sorcerer classes
- Scrolls can be used by non-magic classes for reduced effectiveness
- Locked Repairman enchant behind Artificer class 
- Rebalanced summons/summoning powers. All summons now have a time limit and entity cap


## 0.3.5
- Add more fallback checks to attempt to work around Origins Forge race conditions
- Fixed XP pickup being blocked for all players

## 0.3.4
- Fix Tiefling, Half Elf, and Dragonborn mistakenly having boosts to Magic
- Fixed Magic stat bonus not being selectable
- Fixed Dual Wielder feat not being selectable
- Remove Relics dependency
- Add sound feedback to Lucky feat 
- Fix gold durability powers 
- Balance gold proficiency bonuses
- Add missing hobgoblin icon texture
- Remove unused sound files
- Bulletproofed fallback origin selection reprompting + added warning log if a layer proceeds uncaught

## 0.3.3
- Fixed sizes resetting on logout/death (sizes will still adjust once after updating)
- Finalize all enchantment restrictions
- Improve enchantment restriction tooltip
- Implement Lucky feat. (May make this an enchanting GUI button instead of an active power)

## 0.3.2
- Fix startup crash caused by remap issue 

## 0.3.1
- Automate CF Dependencies

## 0.3.0
- Added Respec button

## 0.2.0
- Implemented all selection icons
- Fixed Feat Menu not consistently opening on levelup
- Rebalanced Feats and Classes
- Fixed most cases of layers being reprompted unnecessarily
- Almost all enchantment restrictions should now work properly