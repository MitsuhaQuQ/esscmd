# EssentialsX command reference

Text version of `EssentialsX.pdf` (kept locally, not in git), a snapshot of
<https://essentialsx.net/commands> taken 2026-10-30 (163 commands). Use this file instead of the
PDF: it is the source of truth for command names, aliases and syntax. Which of these commands we
implement, and in what order, is decided in [AGENTS.md](../AGENTS.md#commands).

"+N more" means the source page hid the rest of the aliases.

| Module | Command | Description | Aliases | Usage |
|---|---|---|---|---|
| core | `/afk` | Marks you as away-from-keyboard. | away eafk eaway | `/afk [player/message...]` |
| core | `/antioch` | A little surprise for operators. | tnt grenade etnt +2 more | `/antioch [message]` |
| core | `/anvil` | Opens up an anvil. | eanvil | `/anvil` |
| core | `/back` | Teleports you to your location prior to tp/spawn/warp. | return eback ereturn | `/back [player]` |
| core | `/backup` | Runs the backup if configured. | ebackup | `/backup` |
| core | `/balance` | States the current balance of a player. | bal money ebal +2 more | `/balance [player]` |
| core | `/balancetop` | Gets the top balance values. | baltop ebaltop ebalancetop | `/balancetop [page]` |
| core | `/ban` | Bans a player. | eban | `/ban <player> [reason]` |
| core | `/banip` | Bans an IP address. | ebanip | `/banip <address> [reason]` |
| core | `/beezooka` | Throw an exploding bee at your opponent. | beecannon ebeezooka ebeecannon | `/beezooka` |
| core | `/book` | Allows reopening and editing of sealed books. | ebook | `/book [title\|author [name]]` |
| core | `/bottom` | Teleport to the lowest block at your current position. | ebottom | `/bottom` |
| core | `/break` | Breaks the block you are looking at. | ebreak | `/break` |
| core | `/broadcast` | Broadcasts a message to the entire server. | bc bcast shout +4 more | `/broadcast <msg>` |
| core | `/broadcastworld` | Broadcasts a message to a world. | bcw bcastw shoutworld +4 more | `/broadcastworld <world> <msg>` |
| core | `/bigtree` | Spawn a big tree where you are looking. | largetree ebigtree elargetree | `/bigtree <tree\|redwood\|jungle\|darkoak>` |
| core | `/burn` | Set a player on fire. | eburn | `/burn <player> <seconds>` |
| core | `/cartographytable` | Opens up a cartography table. | carttable ecarttable ecartographytable | `/cartographytable` |
| core | `/clearinventory` | Clear all items in your inventory. | ci clean clear +6 more | `/clearinventory [player\|*] [item[:\<data>]\|*\|**] [amount]` |
| core | `/clearinventoryconfirmtoggle` | Toggles whether you are prompted to confirm inventory clears. | clearconfirm clearconfirmon clearconfirmoff +6 more | `/clearinventoryconfirmtoggle` |
| core | `/condense` | Condenses items into a more compact blocks. | blocks compact toblocks +4 more | `/condense [item]` |
| core | `/compass` | Describes your current bearing. | direction ecompass edirection | `/compass` |
| core | `/createkit` | Create a kit in game! | ck kc createk +1 more | `/createkit <kitname> <delay>` |
| core | `/customtext` | Allows you to create custom text commands. |  | `/customtext Define in bukkit.yml` |
| core | `/delhome` | Removes a home. | rmhome remhome ermhome +2 more | `/delhome [player:]<name>` |
| core | `/deljail` | Removes a jail. | rmjail remjail ermjail +2 more | `/deljail <jailname>` |
| core | `/delkit` | Deletes the specified kit. | rmkit remkit deletekit +4 more | `/delkit <kit>` |
| core | `/delwarp` | Deletes the specified warp. | rmwarp remwarp ermwarp +2 more | `/delwarp <warp>` |
| core | `/depth` | States current depth, relative to sea level. | height edepth eheight | `/depth` |
| core | `/disposal` | Opens a portable disposal menu. | trash etrash edisposal | `/disposal` |
| core | `/eco` | Manages the server economy. | economy eeco eeconomy | `/eco <give\|take\|set\|reset> <player> <amount>` |
| core | `/enchant` | Enchants the item the user is holding. | enchantment eenchant eenchantment | `/enchant <enchantmentname> [level]` |
| core | `/enderchest` | Lets you see inside an enderchest. | ec echest endersee +4 more | `/enderchest [player]` |
| core | `/essentials` | Reloads essentials. | ess essversion eess +1 more | `/essentials` |
| core | `/exp` | Give, set, reset, or look at a players experience. | xp eexp | `/exp [reset\|show\|set\|give] [playername [amount]]` |
| core | `/ext` | Extinguish players. | extinguish eext eextinguish | `/ext [player]` |
| core | `/feed` | Satisfy the hunger. | eat eeat efeed | `/feed [player]` |
| core | `/fly` | Take off, and soar! | efly | `/fly [player] [on\|off]` |
| core | `/fireball` | Throw a fireball or other assorted projectiles. | fireskull fireentity efireball +2 more | `/fireball [fireball\|small\|large\|arrow\|skull\|egg\|snowball\|expbottle\|dragon\|splash [speed]` |
| core | `/firework` | Allows you to modify a stack of fireworks. | efirework | `/firework <<meta param>\|power [amount]\|clear\|fire [amount]>` |
| core | `/gamemode` | Change player gamemode. | gm sp gma +26 more | `/gamemode <survival\|creative\|adventure\|spectator> [player]` |
| core | `/gc` | Reports memory, uptime and tick info. | lag mem tps +10 more | `/gc` |
| core | `/getpos` | Get your current coordinates or those of a player. | coords getloc position +7 more | `/getpos [player]` |
| core | `/give` | Give a player an item. | egive | `/give <player> <item\|numeric> [amount [itemmeta...]]` |
| core | `/god` | Enables your godly powers. | tgm godmode egod +2 more | `/god [player] [on\|off]` |
| core | `/grindstone` | Opens up a grindstone. | egrindstone | `/grindstone` |
| core | `/hat` | Get some cool new headgear. | head ehat ehead | `/hat [remove]` |
| core | `/heal` | Heals you or the given player. | eheal | `/heal [player]` |
| core | `/help` | Views a list of available commands. | ehelp | `/help [search term] [page]` |
| core | `/helpop` | Message online admins. | ac amsg eac +2 more | `/helpop <message>` |
| core | `/home` | Teleport to your home. | homes ehome ehomes | `/home [player:][name]` |
| core | `/ice` | Cools a player off. | efreeze eice | `/ice [player]` |
| core | `/ignore` | Ignore or unignore other players. | rmignore unignore delignore +6 more | `/ignore <player>` |
| core | `/info` | Shows information set by the server owner. | ifo news about +6 more | `/info [chapter] [page]` |
| core | `/invsee` | See the inventory of other players. | einvsee | `/invsee <player>` |
| core | `/item` | Spawn an item. | i ei eitem | `/item <item\|numeric> [amount [itemmeta...]]` |
| core | `/itemdb` | Searches for an item. | dura itemno durability +4 more | `/itemdb <item>` |
| core | `/itemlore` | Edit the lore of an item. | lore ilore elore +2 more | `/itemlore <add/set/clear> [text/line] [text]` |
| core | `/itemname` | Names an item. | iname irename itemrename +4 more | `/itemname [name]` |
| core | `/jailedplayers` | List all jailed players. | ejp ejailed ejailedplayers | `/jailedplayers` |
| core | `/jails` | List all jails. | ejails | `/jails` |
| core | `/jump` | Ju mps to the nearest block in the line of sight. | j jumpto ej +2 more | `/jump` |
| core | `/kick` | Kicks a specified player with a reason. | ekick | `/kick <player> [reason]` |
| core | `/kickall` | Kicks all players off the server except the issuer. | ekickall | `/kickall [reason]` |
| core | `/kill` | Kills specified player. | ekill | `/kill <player>` |
| core | `/kit` | Obtains the specified kit or views all available kits. | kits ekit ekits | `/kit [kit] [player]` |
| core | `/kitreset` | Resets the cooldown on the specified kit. | kitr resetkit ekitr +2 more | `/kitreset <kit> [player]` |
| core | `/kittycannon` | Throw an exploding kitten at your opponent. | ekittycannon | `/kittycannon` |
| core | `/lightning` | The power of Thor. Strike at cursor or player. | thor shock smite +6 more | `/lightning [player] [power]` |
| core | `/list` | List all online players. | who plist online +6 more | `/list [group]` |
| core | `/loom` | Opens up a loom. | eloom | `/loom` |
| core | `/mail` | Manages inter-player, intra-server mail. | memo email ememo +1 more | `/mail [read\|clear\|clear [number]\|clear <player> [number]\|send [to] [message]\|sendtemp [to] [expire time] [message]\|sendall [message]]` |
| core | `/me` | Describes an action in the context of the player. | action describe eme +2 more | `/me <description>` |
| core | `/more` | Fills the item stack in hand to specified amount, or to maximum size if none is specified. | emore | `/more [amount]` |
| core | `/motd` | Views the Message Of The Day. | emotd | `/motd [chapter] [page]` |
| core | `/msg` | Sends a private message to the specified player. | m t w +7 more | `/msg <to> <message>` |
| core | `/msgtoggle` | Blocks receiving all private messages. | emsgtoggle | `/msgtoggle [player] [on\|off]` |
| core | `/mute` | Mutes or unm utes a player. | unmute silence emute +2 more | `/mute <player> [datediff] [reason]` |
| core | `/near` | Lists the players near by or around a player. | nearby enear enearby | `/near [playername] [radius]` |
| core | `/nick` | Change your nickname or that of another player. | nickname enick enickname | `/nick [player] <nickname\|off>` |
| core | `/nuke` | May death rain upon them. | enuke | `/nuke [player]` |
| core | `/tpoffline` | Teleport to a player's last known logout location | otp tpoff offlinetp +2 more | `/tpoffline <player>` |
| core | `/pay` | Pays another player from your balance. | epay | `/pay <player> <amount>` |
| core | `/paytoggle` | Toggles whether you are accepting payments. | payon payoff epayon +2 more | `/paytoggle [player]` |
| core | `/payconfirmtoggle` | Toggles whether you are prompted to confirm payments. | payconfirm payconfirmon payconfirmoff +4 more | `/payconfirmtoggle` |
| core | `/ping` | Pong! | echo pong eecho +2 more | `/ping` |
| core | `/playtime` | Shows a player's time played in game | eplaytime | `/playtime [player]` |
| core | `/potion` | Adds custom potion effects to a potion. | elixer eelixer epotion | `/potion <clear\|apply\|effect:<effect> power:<power> duration: <duration>>` |
| core | `/powertool` | Assigns a command to the item in hand. | pt ept epowertool | `/powertool [l:\|a:\|r:\|c:\|d:][command] [arguments] - {player} can be replaced by name of a clicked player.` |
| core | `/powertoollist` | Lists all current powertools. | ptlist eptlist epowertoollist | `/powertoollist` |
| core | `/powertooltoggle` | Enables or disables all current powertools. | ptt pttoggle eptt +2 more | `/powertooltoggle` |
| core | `/ptime` | Adjust player's client time. Add @ prefix to fix. | playertime eptime eplayertime | `/ptime [list\|reset\|day\|night\|dawn\|17:30\|4pm\|4000ticks] [player\|*]` |
| core | `/pweather` | Adjust a player's weather | playerweather epweather eplayerweather | `/pweather [list\|reset\|storm\|sun\|clear] [player\|*]` |
| core | `/r` | Quickly reply to the last player to message you. | reply er ereply | `/r <message>` |
| core | `/rtoggle` | Change whether the recipient of the reply is last recipient or last sender | replytoggle ertoggle ereplytoggle | `/rtoggle [player] [on\|off]` |
| core | `/realname` | Displays the username of a user based on nick. | erealname | `/realname <nickname>` |
| core | `/recipe` | Displays how to craft items. | method formula recipes +4 more | `/recipe <<item>\|hand> [number]` |
| core | `/remove` | Removes entities in your world. | butcher killall mobkill +4 more | `/remove <all\|tamed\|named\|drops\|arrows\|boats\|minecarts\|xp\|paintings\|itemframes\|e [mobType]> [radius\|world]` |
| core | `/renamehome` | Renames a home. | erenamehome | `/renamehome <[player:]name> <new name>` |
| core | `/repair` | Repairs the durability of one or all items. | fix efix erepair | `/repair [hand\|all]` |
| core | `/rest` | Rests you or the given player. | erest | `/rest [player]` |
| core | `/rules` | Views the server rules. | erules | `/rules [chapter] [page]` |
| core | `/seen` | Shows the last logout time of a player. | alts ealts eseen | `/seen <playername>` |
| core | `/sell` | Sells the item currently in your hand. | esell | `/sell <<itemname>\|<id>\|hand\|inventory\|blocks> [amount]` |
| core | `/sethome` | Set your home to your current location. | createhome esethome ecreatehome | `/sethome [[player:]name]` |
| core | `/setjail` | Creates a jail where you specified named [jailname]. | createjail esetjail ecreatejail | `/setjail <jailname>` |
| core | `/settpr` | Set the random teleport location and parameters. | settprandom esettpr esettprandom | `/settpr <world> [center\|minrange\|maxrange] [value]` |
| core | `/setwarp` | Creates a new warp. | createwarp esetwarp ecreatewarp | `/setwarp <warp>` |
| core | `/setworth` | Set the sell value of an item. | esetworth | `/setworth [itemname\|id] <price>` |
| core | `/showkit` | Show contents of a kit. | kitshow preview kitpreview | `/showkit <kitname>` |
| core | `/editsign` | Edits a sign in the world. | sign esign eeditsign | `/editsign <set/clear/copy/paste> [line number] [text]` |
| core | `/skull` | Set the owner of a player skull | head playerskull ehead +2 more | `/skull [owner] [player]` |
| core | `/smithingtable` | Opens up a smithing table. | smithtable esmithtable esmithingtable | `/smithingtable` |
| core | `/socialspy` | Toggles if you can see msg/mail commands in chat. | esocialspy | `/socialspy [player] [on\|off]` |
| core | `/spawner` | Change the mob type of a spawner. | changems mobspawner espawner +2 more | `/spawner <mob> [delay]` |
| core | `/spawnmob` | Spawns a mob. | mob spawnentity emob +2 more | `/spawnmob <mob>[:data][,<mount>[:data]] [amount] [player]` |
| core | `/speed` | Change your speed limits. | fspeed wspeed flyspeed +6 more | `/speed [type] <speed> [player]` |
| core | `/stonecutter` | Opens up a stonecutter. | estonecutter | `/stonecutter` |
| core | `/sudo` | Make another user perform a command. | esudo | `/sudo <player> <command [args]>` |
| core | `/suicide` | Causes you to perish. | esuicide | `/suicide` |
| core | `/tempban` | Temporary ban a user. | etempban | `/tempban <playername> <datediff> [reason]` |
| core | `/tempbanip` | Temporarily ban an IP Address. | etempbanip | `/tempbanip <playername> <datediff> [reason]` |
| core | `/thunder` | Enable/disable thunder. | ethunder | `/thunder <true/false> [duration]` |
| core | `/time` | Display/Change the world time. Defaults to current world. | day night eday +2 more | `/time [set\|add] [day\|night\|dawn\|17:30\|4pm\|4000ticks] [worldname\|all]` |
| core | `/togglejail` | Jails/Unjails a player, TPs them to the jail specified. | jail tjail unjail +4 more | `/togglejail <player> <jailname> [datediff]` |
| core | `/top` | Teleport to the highest block at your current position. | etop | `/top` |
| core | `/tp` | Teleport to a player. | tele tp2p teleport +4 more | `/tp <player> [otherplayer]` |
| core | `/tpa` | Request to teleport to the specified player. | call tpask etpa +2 more | `/tpa <player>` |
| core | `/tpaall` | Requests all players online to teleport to you. | etpaall | `/tpaall <player>` |
| core | `/tpaccept` | Accepts teleport requests. | tpyes etpyes etpaccept | `/tpaccept [otherplayer]` |
| core | `/tpahere` | Request that the specified player teleport to you. | etpahere | `/tpahere <player>` |
| core | `/tpall` | Teleport all online players to another player. | etpall | `/tpall [player]` |
| core | `/tpauto` | Automatically accept teleportation requests. | etpauto | `/tpauto [player]` |
| core | `/tpacancel` | Cancel all outstanding teleport requests. Specify [player] to cancel requests with them. | etpacancel | `/tpacancel [player]` |
| core | `/tpdeny` | Rejects teleport requests. | tpno etpno etpdeny | `/tpdeny` |
| core | `/tphere` | Teleport a player to you. | s etphere | `/tphere <player>` |
| core | `/tpo` | Teleport override for tptoggle. | etpo | `/tpo <player> [otherplayer]` |
| core | `/tpohere` | Teleport here override for tptoggle. | etpohere | `/tpohere <player>` |
| core | `/tppos` | Teleport to coordinates. | etppos | `/tppos <x> <y> <z> [yaw] [pitch] [world]` |
| core | `/tpr` | Teleport randomly. | tprandom etpr etprandom | `/tpr` |
| core | `/tptoggle` | Blocks all forms of teleportation. | etptoggle | `/tptoggle [player] [on\|off]` |
| core | `/tree` | Spawn a tree where you are looking. | etree | `/tree <tree\|birch\|redwood\|redmushroom\|brownmushroom\|jungle\|junglebush\|swamp\|…` (truncated in source) |
| core | `/unban` | Unbans the specified player. | pardon eunban epardon | `/unban <player>` |
| core | `/unbanip` | Unbans the specified IP address. | pardonip eunbanip epardonip | `/unbanip <address>` |
| core | `/unlimited` | Allows the unlimited placing of items. | ul unl eul +2 more | `/unlimited <list\|item\|clear> [player]` |
| core | `/vanish` | Hide yourself from other players. | v ev evanish | `/vanish [player] [on\|off]` |
| core | `/warp` | List all warps or warp to the specified location. | warps ewarp ewarps | `/warp <pagenumber\|warp> [player]` |
| core | `/warpinfo` | Finds location information for a specified warp. | ewarpinfo | `/warpinfo <warp>` |
| core | `/weather` | Sets the weather. | sky sun rain +6 more | `/weather <storm/sun> [duration]` |
| core | `/whois` | Determine basic information about the specified player. | ewhois | `/whois <nickname>` |
| core | `/workbench` | Opens up a workbench. | wb craft wbench +4 more | `/workbench` |
| core | `/world` | Switch between worlds. | eworld | `/world [world]` |
| core | `/worth` | Calculates the worth of items in hand or as specified. | price eprice eworth | `/worth <<itemname>\|<id>\|hand\|inventory\|blocks> [-][amount]` |
| chat | `/toggleshout` | Toggles whether you are talking in shout mode | etoggleshout | `/toggleshout [player] [on\|off]` |
| discord | `/discordbroadcast` | Broadcasts a message to the specified Discord channel. | dbc dbcast dbroadcast +4 more | `/discordbroadcast <channel> <msg>` |
| discord | `/discord` | Sends the Discord invite link to the player. | ediscord | `/discord` |
| discord | `/link` | Generates a code to link your Minecraft account to Discord. | discordlink elink ediscordlink | `/link` |
| discord | `/unlink` | Unlinks your Minecraft account from the currently linked Discord account. | discordunlink eunlink ediscordunlink | `/unlink` |
| spawn | `/setspawn` |  | esetspawn | `/setspawn` |
| spawn | `/spawn` |  | espawn | `/spawn` |
| xmpp | `/setxmpp` |  |  | `/setxmpp` |
| xmpp | `/xmpp` |  |  | `/xmpp` |
| xmpp | `/xmppspy` |  |  | `/xmppspy Downloads 8.37mLatest Release 2.22.0 Jenkinsb1832 Discord 4.4k online GitHub 2307 stars Website copyright © 2019-2026 EssentialsX Team, 2015-2026 EssentialsX wiki contributors except where otherwise noted. ✨ Beta Site Light Dark Auto` |
