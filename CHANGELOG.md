# 1.4.0

- added enchanted book consumption modes, optionally defined individually for each RPG Enchanting Table, with a configurable default. The current modes are:
  - KEEP (same as before)
  - CONSUME (enchanted books are replaced with a configurable item when one of their enchantments is used)
  - PARTIAL_CONSUME (same as consume, but books with multiple enchants are not replaced, only the relevant enchantment is removed)
- added enchantment unlock modes, optionally defined individually for each RPG Enchanting Table, with a configurable default. The current modes are:
  - ADDITION (same as before)
  - BLOCK_REQUIRED_FOR_ADVANCEMENT (enchantments unlocked by advancements are only added, when they are also unlocked by blocks, book enchantments are added like normal)
- block reach radius can now be defined individually for each RPG Enchanting Table, with a configurable default.
- added item name overwrites for specific combinations of items and enchantments

# 1.3.0

- the vanilla Enchanting Table recipe now instead results in one RPG Enchanting Table
- added loot table for RPG Enchanting Table
- added RPG Enchanting Table to "minecraft:mineable:pickaxe" block tag

# 1.2.0

- added compatibility for 'Dungeons and Taverns' by NovaWostra
- added compatibility for 'Extra RPG Attributes' by Forg (Thanks @ Forg for the contribution)
- added an optional "Enchanted by player_name" line to item tooltips
- most modded chiseled bookshelves are now getting checked for enchanted books by the RPG Enchanting Table
- merged compatibility resource packs into one
- compatibility data packs are now enabled by default
- pre/suffix strings can now wrap around the item name (suffix strings are processed first)
- fixed missing translation for some enchantments (Thanks @ Not A Noob for the contribution)

# 1.1.0

- added optional compatibility with 'Enchantment Descriptions'
- added compatibility data/resource packs for the 'RPG Series' mods by Daedelus
- added compatibility data/resource packs for the 'More RPG Series' mods by Fichte
- several small fixes

# 1.0.0

First release.

Provided changes for vanilla enchantments and default config values are very much arbitrary. The gameplay balance is likely non-existent, feedback is much appreciated!

#