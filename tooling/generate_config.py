#!/usr/bin/env python3
"""Generates src/main/resources/config.yml (the shipped default balance)."""
import os, io

UNLOCKS = {
 1:("Homestead", ["basic_generators"], "Your island starts producing."),
 2:("Apprentice", ["role_selection"], "Choose your role."),
 3:("Prospector", ["omnitool_efficiency"], "OmniTool efficiency node."),
 4:("Cultivator", ["farming_autoreplant"], "Crops replant themselves."),
 5:("Forgeworks", ["generator_tier_2"], "Generator tier 2."),
 6:("Angler", ["fishing_luck"], "Fishing luck node + rare catches."),
 7:("Beastpen", ["spawner_tier_1"], "Progression spawners."),
 8:("Excavator", ["omnitool_fortune"], "Fortune node."),
 9:("Specialist", ["mastery_system"], "Island Mastery unlocked."),
 10:("Industrialist", ["generator_tier_3"], "Generator tier 3."),
 11:("Botanist", ["crops_tier_2"], "Tier 2 crops."),
 12:("Hunter", ["slayer_tier_2"], "Tier 2 slayer targets."),
 13:("Companion Keeper", ["companion_system"], "Companions unlocked."),
 14:("Tunneler", ["omnitool_area_mining"], "Area mining."),
 15:("Refinery", ["generator_tier_4"], "Generator tier 4."),
 16:("Warden", ["spawner_tier_2"], "Spawner tier 2."),
 17:("Deepminer", ["mining_tier_2"], "Deep ores."),
 18:("Deepsea", ["fishing_tier_2"], "Deep sea loot."),
 19:("Lumberlord", ["omnitool_lumber"], "Tree felling."),
 20:("Foundry", ["generator_tier_5"], "Generator tier 5."),
 21:("Bondsmith", ["companion_merging"], "Companion merging."),
 22:("Grandmaster", ["mastery_branch_2"], "Second mastery branch."),
 23:("Beastmaster", ["spawner_tier_3"], "Spawner tier 3."),
 24:("Artificer", ["omnitool_special_procs"], "Special procs."),
 25:("Mythwright", ["generator_tier_6"], "Generator tier 6."),
 26:("Voidminer", ["mining_tier_3"], "Void ores."),
 27:("Executioner", ["slayer_tier_3"], "Tier 3 slayer targets."),
 28:("Tokenseeker", ["omnitool_token_find"], "Token find node."),
 29:("Overlord", ["spawner_tier_4"], "Spawner tier 4."),
 30:("Ascendant", ["prestige_workshop"], "Seasonal prestige workshop."),
}

def island_xp(level):
    return int(round(1200 * (1.19 ** (level - 1)) / 50.0) * 50)

out = io.StringIO()
w = out.write
w("""# =====================================================================
# Apex SkyBlock Progression - default configuration
# Every balance value lives here; the plugin hard codes nothing.
# Identifiers (roles, unlocks, nodes) are permanent keys - renaming a
# display-name is always safe, renaming an id resets stored progress.
# =====================================================================

storage:
  # sqlite (default) or mysql / mariadb
  type: sqlite
  file: progression.db
  table-prefix: apexsb_
  save-interval-seconds: 300
  mysql:
    host: localhost
    port: 3306
    database: skyblock
    username: root
    password: ""
    parameters: "useSSL=false&characterEncoding=utf8"

general:
  # Ticks between generator/spawner ticks. 20 ticks = 1 second.
  generator-task-interval: 20
  spawner-task-interval: 40
  # Maximum blocks a single OmniTool area-mining proc may break.
  max-area-blocks: 27
  level-up-broadcast: true

economy:
  # What happens to money costs when Vault (or any economy plugin) is missing:
  #   waive - the money part of a cost is skipped, the feature keeps working
  #   block - money dependent actions are refused with a clear message
  missing-vault-behaviour: waive

currency:
  display-name: "Sky Token"
  display-name-plural: "Sky Tokens"
  symbol: "\\u2726"
  max-balance: 1000000000
  placeholder-name: skytokens

season:
  season-id: season-1
  display-name: "Season 1"
  xp-multiplier: 1.0
  token-multiplier: 1.0
  # What a season rollover wipes. Anything omitted is preserved forever.
  reset:
    - island_xp
    - mastery_xp
    - role_xp
    - omnitool_upgrades
    - generators
    - spawners
  # Statistics that survive every reset (lifetime totals).
  protected-stats:
    - lifetime_tokens_earned
  keep-companions: true
  keep-tokens: false

island:
  # Island XP granted per unit of a generic source. Skills add their own XP.
  xp-sources:
    block_mined: 1.0
    crop_harvested: 1.0
    fish_caught: 2.0
    mob_killed: 1.5
    log_chopped: 1.0
    generator_cycle: 0.5
    role_level: 250.0
    spawner_tier: 400.0
  levels:
""")
for lvl in range(1, 31):
    title, unlocks, desc = UNLOCKS[lvl]
    w(f"    {lvl}:\n")
    w(f"      title: \"{title}\"\n")
    w(f"      xp-required: {island_xp(lvl)}\n")
    w(f"      token-reward: {50 + lvl * 25}\n")
    w(f"      money-reward: {1000 * lvl}\n")
    w( "      unlocks:\n")
    for u in unlocks:
        w(f"        - {u}\n")
    w( "      description:\n")
    w(f"        - \"{desc}\"\n")
    w( "      commands: []\n")

# ---------------- mastery ----------------
MASTERY = [
 ("mining", "Mining Mastery", "IRON_PICKAXE", "block_mined"),
 ("farming", "Farming Mastery", "IRON_HOE", "crop_harvested"),
 ("fishing", "Fishing Mastery", "FISHING_ROD", "fish_caught"),
 ("slayer", "Slayer Mastery", "IRON_SWORD", "mob_killed"),
 ("logging", "Logging Mastery", "IRON_AXE", "log_chopped"),
 ("generators", "Generator Mastery", "COBBLESTONE", "generator_cycle"),
]
w("""
mastery:
  categories:
""")
for cid, name, icon, source in MASTERY:
    w(f"""    {cid}:
      display-name: "{name}"
      icon: {icon}
      xp-curve:
        type: geometric
        max-level: 25
        base: 800
        growth: 1.22
      xp-sources:
        {source}: 1.0
      unlocks:
        efficient_hands:
          level: 3
          type: xp_bonus
          value: 0.05
          label: "Efficient Hands"
          description:
            - "&7+5% {cid} XP"
        lucky_touch:
          level: 7
          type: rare_drop
          value: 0.01
          label: "Lucky Touch"
          description:
            - "&7+1% rare drop chance"
        token_sense:
          level: 12
          type: token_find
          value: 0.01
          label: "Token Sense"
          description:
            - "&7+1% Sky Token find chance"
        specialist_ability:
          level: 18
          type: ability
          value: 1.0
          label: "{name} Ability"
          description:
            - "&7Unlocks the {cid} specialist ability"
        grandmaster:
          level: 25
          type: xp_bonus
          value: 0.15
          label: "Grandmaster"
          description:
            - "&7+15% {cid} XP"
""")

# ---------------- roles ----------------
ROLES = [
 ("miner", "Miner", "DIAMOND_PICKAXE", "omni_pickaxe", 0),
 ("farmer", "Farmer", "DIAMOND_HOE", "omni_hoe", 0),
 ("fisher", "Fisher", "FISHING_ROD", "omni_rod", 0),
 ("slayer", "Slayer", "DIAMOND_SWORD", "omni_blade", 0),
 ("logger", "Logger", "DIAMOND_AXE", "omni_axe", 0),
 ("universal", "Universal", "NETHER_STAR", "omni_tool", 12),
]
w("""
roles-config:
  default-role: miner
  roles:
""")
for rid, name, icon, tool, req in ROLES:
    w(f"""    {rid}:
      display-name: "{name}"
      icon: {icon}
      omnitool: {tool}
      required-island-level: {req}
      description:
        - "&7Specialise as a {name}."
      xp-curve:
        type: geometric
        max-level: 50
        base: 400
        growth: 1.14
      perks:
        5:
          xp_boost:
            type: xp_bonus
            value: 0.03
            label: "Focused"
        15:
          token_luck:
            type: token_find
            value: 0.005
            label: "Token Luck"
        30:
          xp_boost_2:
            type: xp_bonus
            value: 0.05
            label: "Veteran"
        50:
          mastery_boost:
            type: rare_drop
            value: 0.02
            label: "Legend"
""")

# ---------------- omnitools ----------------
def node(nid, name, icon, effect, maxl, vpl, base_cost, cost_step, role_req, role_step, isl, unlock, desc):
    s = f"""        {nid}:
          display-name: "{name}"
          icon: {icon}
          effect: {effect}
          max-level: {maxl}
          base-value: 0.0
          value-per-level: {vpl}
          token-cost-base: {base_cost}
          token-cost-per-level: {cost_step}
          money-cost-base: 0.0
          money-cost-per-level: 0.0
          required-role-level: {role_req}
          role-level-per-node-level: {role_step}
          required-island-level: {isl}
"""
    if unlock:
        s += f"          required-unlock: {unlock}\n"
    s += "          description:\n"
    for line in desc:
        s += f"            - \"{line}\"\n"
    return s

SHARED = lambda: (
    node("efficiency", "Efficiency", "REDSTONE", "efficiency", 5, 1.0, 150, 120, 2, 2, 3, "omnitool_efficiency", ["&7+1 mining/break speed per level"]) +
    node("xp_bonus", "Insight", "EXPERIENCE_BOTTLE", "xp_bonus", 5, 0.03, 200, 175, 3, 2, 0, "", ["&7+3% XP per level"]) +
    node("token_find", "Token Find", "SUNFLOWER", "token_find", 5, 0.01, 400, 350, 10, 3, 28, "omnitool_token_find", ["&7+1% Sky Token find chance per level"])
)

w("""
omnitools-config:
  omnitools:
    omni_pickaxe:
      display-name: "&bMiner OmniTool"
      role: miner
      material: DIAMOND_PICKAXE
      nodes:
""")
w(SHARED())
w(node("fortune", "Fortune", "LAPIS_LAZULI", "fortune", 4, 0.15, 300, 250, 6, 3, 8, "omnitool_fortune", ["&7+15% extra drops per level"]))
w(node("area_mining", "Area Mining", "TNT", "area_mining", 3, 1.0, 900, 900, 15, 5, 14, "omnitool_area_mining", ["&7Mines a 3x3 (and larger) area"]))
w(node("rare_drop", "Vein Sense", "AMETHYST_SHARD", "rare_drop", 3, 0.01, 750, 650, 20, 4, 17, "mining_tier_2", ["&7+1% rare mining drop per level"]))

w("""    omni_hoe:
      display-name: "&aFarmer OmniTool"
      role: farmer
      material: DIAMOND_HOE
      nodes:
""")
w(SHARED())
w(node("auto_replant", "Auto Replant", "WHEAT_SEEDS", "auto_replant", 1, 1.0, 500, 0, 5, 0, 4, "farming_autoreplant", ["&7Harvested crops replant themselves"]))
w(node("crop_radius", "Harvest Radius", "HAY_BLOCK", "crop_radius", 3, 1.0, 800, 800, 12, 5, 11, "crops_tier_2", ["&7Harvests a larger radius per level"]))
w(node("crop_multiplier", "Bountiful", "GOLDEN_CARROT", "fortune", 4, 0.12, 350, 275, 8, 3, 0, "", ["&7+12% crop yield per level"]))

w("""    omni_axe:
      display-name: "&6Logger OmniTool"
      role: logger
      material: DIAMOND_AXE
      nodes:
""")
w(SHARED())
w(node("lumber", "Tree Feller", "OAK_LOG", "lumber", 3, 8.0, 850, 700, 14, 5, 19, "omnitool_lumber", ["&7Fells up to 8 extra logs per level"]))
w(node("timber_fortune", "Timber Fortune", "STRIPPED_OAK_LOG", "fortune", 4, 0.12, 320, 260, 7, 3, 0, "", ["&7+12% log yield per level"]))

w("""    omni_rod:
      display-name: "&3Fisher OmniTool"
      role: fisher
      material: FISHING_ROD
      nodes:
""")
w(SHARED())
w(node("fishing_luck", "Sea Luck", "NAUTILUS_SHELL", "fishing_luck", 4, 0.05, 300, 280, 6, 3, 6, "fishing_luck", ["&7+5% rare catch chance per level"]))
w(node("fishing_speed", "Quick Cast", "LILY_PAD", "fishing_speed", 4, 0.08, 280, 250, 5, 3, 0, "", ["&7-8% bite time per level"]))

w("""    omni_blade:
      display-name: "&cSlayer OmniTool"
      role: slayer
      material: DIAMOND_SWORD
      nodes:
""")
w(SHARED())
w(node("mob_damage", "Slayer Edge", "IRON_SWORD", "mob_damage", 5, 0.06, 320, 300, 4, 3, 0, "", ["&7+6% damage to hostile mobs per level"]))
w(node("special_proc", "Executioner", "WITHER_SKELETON_SKULL", "special_proc", 3, 0.02, 950, 900, 18, 6, 24, "omnitool_special_procs", ["&7+2% chance of an instant execute proc"]))

w("""    omni_tool:
      display-name: "&dUniversal OmniTool"
      role: universal
      material: NETHERITE_PICKAXE
      nodes:
""")
w(SHARED())
w(node("versatility", "Versatility", "NETHER_STAR", "xp_bonus", 3, 0.04, 1200, 1000, 20, 5, 12, "", ["&7+4% XP for every skill per level"]))

# ---------------- skills ----------------
def sources(entries):
    s = "    sources:\n"
    for key, role_xp, isl_xp, mast_xp, req, tchance, tamount in entries:
        s += f"""      {key}:
        role-xp: {role_xp}
        island-xp: {isl_xp}
        mastery-xp: {mast_xp}
        required-island-level: {req}
        token-chance: {tchance}
        token-amount: {tamount}
"""
    return s

w("""
skills:
  mining:
    role: miner
    mastery-category: mining
    rare-drop-chance: 0.01
""")
w(sources([("STONE",1,1,1,0,0.001,1),("DEEPSLATE",2,1,2,0,0.002,1),("COAL_ORE",3,2,3,0,0.004,1),
           ("IRON_ORE",5,3,4,0,0.006,1),("GOLD_ORE",8,4,6,5,0.008,2),("DIAMOND_ORE",18,10,14,10,0.02,3),
           ("EMERALD_ORE",22,12,16,17,0.03,4),("ANCIENT_DEBRIS",45,25,35,26,0.06,10)]))
w("""    rare-drops:
      enriched_coal:
        material: COAL_BLOCK
        amount: 1
        weight: 60
        required-island-level: 0
        tokens: 2
      crystal_shard:
        material: AMETHYST_SHARD
        amount: 2
        weight: 30
        required-island-level: 10
        tokens: 5
      void_core:
        material: NETHERITE_SCRAP
        amount: 1
        weight: 10
        required-island-level: 26
        tokens: 25
    milestones:
      first_thousand:
        amount: 1000
        tokens: 100
        island-xp: 500
        unlocks: []
      ten_thousand:
        amount: 10000
        tokens: 750
        island-xp: 4000
        unlocks: []
      hundred_thousand:
        amount: 100000
        tokens: 6000
        island-xp: 30000
        unlocks: []
  farming:
    role: farmer
    mastery-category: farming
    rare-drop-chance: 0.008
""")
w(sources([("WHEAT",2,1,2,0,0.002,1),("CARROTS",2,1,2,0,0.002,1),("POTATOES",2,1,2,0,0.002,1),
           ("BEETROOTS",3,1,2,0,0.002,1),("SUGAR_CANE",2,1,2,0,0.002,1),("MELON",3,2,3,11,0.004,1),
           ("PUMPKIN",3,2,3,11,0.004,1),("NETHER_WART",6,3,5,11,0.01,2)]))
w("""    rare-drops:
      golden_seed:
        material: GOLDEN_CARROT
        amount: 1
        weight: 70
        required-island-level: 0
        tokens: 3
      mythic_sprout:
        material: ENCHANTED_GOLDEN_APPLE
        amount: 1
        weight: 5
        required-island-level: 11
        tokens: 40
    milestones:
      green_thumb:
        amount: 2500
        tokens: 200
        island-xp: 1200
        unlocks: []
      harvest_lord:
        amount: 50000
        tokens: 4000
        island-xp: 20000
        unlocks: []
  fishing:
    role: fisher
    mastery-category: fishing
    rare-drop-chance: 0.12
""")
w(sources([("COD",6,3,5,0,0.01,1),("SALMON",8,4,6,0,0.012,1),("TROPICAL_FISH",14,7,10,6,0.02,2),
           ("PUFFERFISH",16,8,12,6,0.025,2)]))
w("""    rare-drops:
      shimmer_scale:
        material: PRISMARINE_SHARD
        amount: 1
        weight: 60
        required-island-level: 0
        tokens: 4
      abyssal_pearl:
        material: HEART_OF_THE_SEA
        amount: 1
        weight: 8
        required-island-level: 18
        tokens: 60
      treasure_map:
        material: MAP
        amount: 1
        weight: 32
        required-island-level: 6
        tokens: 10
    milestones:
      angler:
        amount: 500
        tokens: 150
        island-xp: 900
        unlocks: []
      deepsea_master:
        amount: 10000
        tokens: 3500
        island-xp: 18000
        unlocks: []
  slayer:
    role: slayer
    mastery-category: slayer
    rare-drop-chance: 0.02
""")
w(sources([("ZOMBIE",5,3,4,0,0.004,1),("SKELETON",6,3,5,0,0.004,1),("SPIDER",5,3,4,0,0.004,1),
           ("CREEPER",8,4,6,0,0.006,1),("ENDERMAN",16,8,12,12,0.015,3),("BLAZE",18,9,13,12,0.018,3),
           ("WITHER_SKELETON",30,15,22,27,0.03,6)]))
w("""    rare-drops:
      soul_fragment:
        material: SOUL_SAND
        amount: 1
        weight: 70
        required-island-level: 0
        tokens: 5
      slayer_sigil:
        material: NETHER_STAR
        amount: 1
        weight: 4
        required-island-level: 27
        tokens: 80
    milestones:
      first_blood:
        amount: 250
        tokens: 100
        island-xp: 600
        unlocks: []
      reaper:
        amount: 25000
        tokens: 5000
        island-xp: 24000
        unlocks: []
  logging:
    role: logger
    mastery-category: logging
    rare-drop-chance: 0.01
""")
w(sources([("OAK_LOG",3,2,3,0,0.003,1),("BIRCH_LOG",3,2,3,0,0.003,1),("SPRUCE_LOG",3,2,3,0,0.003,1),
           ("JUNGLE_LOG",4,2,3,0,0.003,1),("ACACIA_LOG",4,2,3,0,0.003,1),("DARK_OAK_LOG",5,3,4,0,0.004,1),
           ("CHERRY_LOG",6,3,5,11,0.006,2),("MANGROVE_LOG",6,3,5,11,0.006,2)]))
w("""    rare-drops:
      heartwood:
        material: OAK_SAPLING
        amount: 2
        weight: 80
        required-island-level: 0
        tokens: 3
      ancient_bark:
        material: MANGROVE_ROOTS
        amount: 3
        weight: 20
        required-island-level: 19
        tokens: 15
    milestones:
      woodcutter:
        amount: 2000
        tokens: 180
        island-xp: 1000
        unlocks: []
      forest_lord:
        amount: 40000
        tokens: 4200
        island-xp: 21000
        unlocks: []
""")

# ---------------- generators ----------------
GEN_TIERS = [
 (1, "Cobble Generator I", 100, {"COBBLESTONE":100}, 1, 0, "", 0, 0, 1, 1),
 (2, "Cobble Generator II", 80, {"COBBLESTONE":80,"COAL":20}, 1, 5, "generator_tier_2", 750, 5000, 2, 2),
 (3, "Reinforced Generator III", 60, {"COBBLESTONE":60,"IRON_INGOT":25,"COAL":15}, 1, 10, "generator_tier_3", 2500, 25000, 3, 3),
 (4, "Refined Generator IV", 45, {"IRON_INGOT":45,"GOLD_INGOT":35,"REDSTONE":20}, 2, 15, "generator_tier_4", 7500, 90000, 5, 5),
 (5, "Foundry Generator V", 30, {"GOLD_INGOT":45,"DIAMOND":25,"EMERALD":30}, 2, 20, "generator_tier_5", 20000, 250000, 8, 8),
 (6, "Mythic Generator VI", 20, {"DIAMOND":45,"EMERALD":30,"NETHERITE_SCRAP":25}, 3, 25, "generator_tier_6", 60000, 750000, 12, 12),
]
w("""
generators-config:
  generators:
    cobble_gen:
      display-name: "&7Cobble Generator"
      icon: COBBLESTONE
      max-per-island: 24
      tiers:
""")
for tier, name, interval, outputs, amount, isl, unlock, tcost, mcost, ixp, mxp in GEN_TIERS:
    w(f"""        {tier}:
          display-name: "{name}"
          interval-ticks: {interval}
          output-amount: {amount}
          required-island-level: {isl}
          required-unlock: {unlock}
          token-cost: {tcost}
          money-cost: {mcost}
          island-xp-per-cycle: {ixp}
          mastery-xp-per-cycle: {mxp}
          abilities:
            - auto_collect
          outputs:
""")
    for mat, weight in outputs.items():
        w(f"            {mat}: {weight}\n")

# ---------------- spawners ----------------
SPAWN_TIERS = [
 (1, "Zombie Spawner", "ZOMBIE", 200, 1, 10, 7, "spawner_tier_1", 0, 0, 0.0),
 (2, "Skeleton Spawner", "SKELETON", 180, 1, 12, 16, "spawner_tier_2", 5000, 50000, 0.1),
 (3, "Blaze Spawner", "BLAZE", 160, 1, 12, 23, "spawner_tier_3", 18000, 200000, 0.25),
 (4, "Wither Skeleton Spawner", "WITHER_SKELETON", 140, 1, 10, 29, "spawner_tier_4", 55000, 650000, 0.5),
]
w("""
spawners-config:
  spawners:
    progression_spawner:
      display-name: "&5Progression Spawner"
      icon: SPAWNER
      max-per-island: 8
      max-stack-size: 5
      tiers:
""")
for tier, name, entity, interval, amount, nearby, isl, unlock, tcost, mcost, bonus in SPAWN_TIERS:
    w(f"""        {tier}:
          display-name: "{name}"
          entity-type: {entity}
          interval-ticks: {interval}
          spawn-amount: {amount}
          max-nearby-entities: {nearby}
          required-island-level: {isl}
          required-unlock: {unlock}
          token-cost: {tcost}
          money-cost: {mcost}
          slayer-xp-bonus: {bonus}
          abilities:
            - island_only
""")

# ---------------- companions ----------------
w("""
companions-config:
  max-per-player: 9
  merge-xp-carry: 0.5
  rarities:
    common:
      display-name: "Common"
      colour: "&7"
      max-level: 50
      xp-base: 120
      xp-growth: 1.06
      ability-scale: 1.0
      merge-cost: 500
      next-rarity: rare
    rare:
      display-name: "Rare"
      colour: "&9"
      max-level: 70
      xp-base: 180
      xp-growth: 1.06
      ability-scale: 1.35
      merge-cost: 2500
      next-rarity: epic
    epic:
      display-name: "Epic"
      colour: "&5"
      max-level: 100
      xp-base: 260
      xp-growth: 1.06
      ability-scale: 1.8
      merge-cost: 10000
      next-rarity: mythic
    mythic:
      display-name: "Mythic"
      colour: "&6"
      max-level: 200
      xp-base: 400
      xp-growth: 1.05
      ability-scale: 2.5
      merge-cost: 0
      next-rarity: ""
  companions:
""")
COMPANIONS = [
 ("rock_golem","Rock Golem","STONE","mining_xp_bonus",0.02,0.002,"common","Boosts mining XP."),
 ("harvest_sprite","Harvest Sprite","WHEAT","farming_xp_bonus",0.02,0.002,"common","Boosts farming XP."),
 ("tide_otter","Tide Otter","SALMON","fishing_xp_bonus",0.03,0.002,"rare","Boosts fishing XP."),
 ("shadow_wolf","Shadow Wolf","BONE","slayer_xp_bonus",0.03,0.0025,"rare","Boosts slayer XP."),
 ("timber_owl","Timber Owl","OAK_SAPLING","logging_xp_bonus",0.03,0.002,"epic","Boosts logging XP."),
 ("token_drake","Token Drake","GOLD_NUGGET","token_find",0.01,0.0005,"mythic","Finds extra Sky Tokens."),
]
for cid, name, icon, ability, base, per, rarity, desc in COMPANIONS:
    w(f"""    {cid}:
      display-name: "{name}"
      icon: {icon}
      ability: {ability}
      ability-base: {base}
      ability-per-level: {per}
      rarity: {rarity}
      required-island-level: 13
      description:
        - "&7{desc}"
""")

# ---------------- messages / gui ----------------
w("""
gui:
  filler-material: GRAY_STAINED_GLASS_PANE
  progression-title: "&8Progression Overview"
  island-title: "&8Island Levels"
  mastery-title: "&8Island Mastery"
  roles-title: "&8Roles"
  omnitool-title: "&8OmniTool Upgrades"
  generators-title: "&8Generators"
  spawners-title: "&8Spawners"
  companions-title: "&8Companions"

messages:
  prefix: "&b&lSKY &8\\u00bb &r"
  no-permission: "&cYou do not have permission to do that."
  player-only: "&cThis command can only be used in game."
  unknown-player: "&cUnknown player."
  unknown-subcommand: "&cUnknown subcommand. Try /progression help."
  reloaded: "&aConfiguration reloaded in %ms%ms."
  reload-failed: "&cReload failed: %error%"
  invalid-amount: "&cEnter a valid positive amount."
  island-level-up: "&aYour island reached level &e%level% &7(%title%)&a!"
  island-unlock: "&d\\u2726 Unlocked: &f%unlock%"
  role-level-up: "&aYour &e%role% &arole reached level &e%level%&a!"
  mastery-level-up: "&aMastery &e%category% &areached level &e%level%&a!"
  role-selected: "&aYou are now a &e%role%&a."
  role-already-active: "&cThat role is already active."
  role-island-level: "&cYour island must be level %level% to pick that role."
  role-unknown: "&cUnknown role."
  tokens-received: "&a+%amount% %currency%"
  tokens-spent: "&e-%amount% %currency%"
  tokens-balance: "&7Balance: &e%amount% %currency%"
  tokens-insufficient: "&cYou need %amount% more %currency%."
  upgrade-success: "&a%node% upgraded to level &e%level%&a."
  upgrade-max-level: "&cThat upgrade is already maxed."
  upgrade-role-level: "&cRequires role level %level%."
  upgrade-island-level: "&cRequires island level %level%."
  upgrade-locked: "&cThat upgrade is still locked."
  economy-unavailable: "&cThis feature needs Vault, which is not installed."
  generator-placed: "&aGenerator placed (tier %tier%)."
  generator-limit: "&cYou reached the generator limit for this island."
  generator-upgraded: "&aGenerator upgraded to tier %tier%."
  generator-max-tier: "&cThat generator is at its maximum tier."
  spawner-placed: "&aSpawner placed (tier %tier%)."
  spawner-stacked: "&aSpawner stacked (x%amount%)."
  spawner-limit: "&cYou reached the spawner limit for this island."
  spawner-upgraded: "&aSpawner upgraded to tier %tier%."
  companion-granted: "&aYou received the &e%companion% &acompanion!"
  companion-merged: "&aCompanions merged into &e%rarity% %companion%&a."
  companion-merge-mismatch: "&cBoth companions must be the same type and rarity."
  companion-limit: "&cYou cannot hold any more companions."
  companion-active: "&aActive companion: &e%companion%"
  season-reset: "&eSeason %season% started - progression has been reset."
  gui-locked: "&cLocked."
  gui-click-disabled: "&cYou cannot take items from this menu."
""")

path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "config.yml")
with open(path, "w") as fh:
    fh.write(out.getvalue())
print("wrote", os.path.normpath(path), len(out.getvalue()), "bytes")
