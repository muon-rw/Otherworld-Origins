## 1.0.0
- Updated many selection icons
- Reworked Warlock's Eldritch Blast upgrade 
- Githyanki can now be protected from water by wearing a full set of Ice and Fire Tide gear or Alex's Cave Diving gear
- Fixed College of Lore Bard's passive reducing neutral effects, instead of only negative effects
- Fixed Defensive Duelist not working
- Fixed Dragonborn breath abilities showing 2 cooldown bars
- Removed a number of required dependencies
- Cleaned up unused files

## 0.9.0
- Enchantment restrictions can now be disabled in the config
- Moved the enchantment text restriction rendering so it will be hidden if restrictions are disabled
- Cleaned up internal project structure
- Conditionally load mixins into other mods (most mods are still required to see all origins - working on it!)
- Mark Origins, Iron's, Apotheosis, Medieval, and JustLevelingFork as required in mods.toml (for user clarity)
- Mark Forge 47.3.12 or newer as required in mods.toml

## 0.8.1
- Minor changes to Reforging restriction logic

## 0.8.0
- Fixed reforge restrictions occasionally not working
- Also restricted Augmenting and Gem Cutting
- Added a new skill, Legendary Smith (Level 20 Building) to access Gem Cutting and Reforging
- Underdark subraces can learn Legendary Smith to access Ancient Reforging and Augmenting 
- Made Dark Vision end immediately when disabled

## 0.7.0
- Nerfed Spell Power passive from 0.5 per point to 0.1
- Spell Restrictions now apply only to inscribed spells, not scrolls
- Restricted *all* Apotheosis reforging to specific origins.
- Fixed a softlock where the server repeatedly tried to prompt clients to select an Origin, while the client thought none were available
- Fixed Warlock's Eldritch Blast ability not upgrading at level 15
- Fixed Conjuration subclass' Golem not being summonable
- Added a few default spell categorizations (regenerating the otherworldorigins-commmon config is recommended)

## 0.6.0
- Refactored Casting restrictions. Check the otherworldorigins-common config for more info

## 0.5.1
- Nerfed Spell Power passive ability
- Fixed missing descriptions for Spell Power / Mana Spring passives

## 0.5.0
- Fixed Respec not resetting passive levels or titles
- Added Mana bonus passive to Magic 
- Added Spell Power bonus passive to Intelligence
- Restricted spell casting implement usage to Sorcerers, Warlocks, and Wizards
- Changed "Summon Beast" spell from Polar Bears to Grizzly Bears. Capped at 1 bear.
- Fixed Dragon Breath spells showing up in JEI
- Fixed Summon Golem being a craftable scroll 

## 0.4.2
- Fix crash on dedicated servers

## 0.4.1
- Fix summon powers going on cooldown if attempted to cast with insufficient mana
- Fix missing icon and translation for Golem timer effect

## 0.4.0
- Greatly improved Origin fallback checks. You should now only be asked to reselect an origin layer if there is an error
- Rebalanced Summons/Summoning powers. All summons now have a 5 minute cooldown, 10 minute time limit, and golems have a cap of 1
- Locked Endless Quiver enchant behind Ranger class
- Locked Repairman enchant behind Artificer class
- Fixed the Fifth Feat (Level 20) selection only occurring after reconnecting
- Fixed Origin stat bonuses counting toward the maximum selectable stats, making level 20 impossible to obtain for most players
- Fixed Cantrip casts not being instant, added a short cooldown
- Fixed the Artificer:Armorer subclass being untranslated
- Fixed the Ranger:Hunter subclass having a missing translation

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