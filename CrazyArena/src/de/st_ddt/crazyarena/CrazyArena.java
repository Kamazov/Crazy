package de.st_ddt.crazyarena;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;

import de.st_ddt.crazyarena.arenas.Arena;
import de.st_ddt.crazyarena.arenas.ArenaSet;
import de.st_ddt.crazyarena.exceptions.CrazyArenaException;
import de.st_ddt.crazyarena.participants.Participant;
import de.st_ddt.crazyarena.participants.ParticipantType;
import de.st_ddt.crazyplugin.CrazyPlugin;
import de.st_ddt.crazyplugin.exceptions.CrazyCommandAlreadyExistsException;
import de.st_ddt.crazyplugin.exceptions.CrazyCommandCircumstanceException;
import de.st_ddt.crazyplugin.exceptions.CrazyCommandCrazyErrorException;
import de.st_ddt.crazyplugin.exceptions.CrazyCommandErrorException;
import de.st_ddt.crazyplugin.exceptions.CrazyCommandException;
import de.st_ddt.crazyplugin.exceptions.CrazyCommandExecutorException;
import de.st_ddt.crazyplugin.exceptions.CrazyCommandNoSuchException;
import de.st_ddt.crazyplugin.exceptions.CrazyCommandParameterException;
import de.st_ddt.crazyplugin.exceptions.CrazyCommandPermissionException;
import de.st_ddt.crazyplugin.exceptions.CrazyCommandUsageException;
import de.st_ddt.crazyplugin.exceptions.CrazyException;
import de.st_ddt.crazyutil.PairList;

public class CrazyArena extends CrazyPlugin
{

	private static CrazyArena plugin;
	private CrazyArenaPlayerListener playerListener = null;
	private CrazyArenaBlockListener blocklistener = null;
	private ArenaSet arenas = null;
	private final PairList<Player, Arena> invitations = new PairList<Player, Arena>();
	private final HashMap<Player, Arena> selection = new HashMap<Player, Arena>();
	private static HashMap<String, Class<? extends Arena>> arenaTypes = new HashMap<String, Class<? extends Arena>>();

	public static CrazyArena getPlugin()
	{
		return plugin;
	}

	@Override
	protected String getShortPluginName()
	{
		return "ca";
	}

	public void onEnable()
	{
		plugin = this;
		getServer().getScheduler().scheduleAsyncDelayedTask(this, new ScheduledPermissionAllTask(), 20);
		this.arenas = new ArenaSet(getConfig());
		registerHooks();
		super.onEnable();
	}

	public void onDisable()
	{
		save();
		super.onDisable();
	}

	public void save()
	{
		ConfigurationSection config = getConfig();
		LinkedList<String> names = new LinkedList<String>();
		for (Arena arena : arenas)
		{
			arena.stop(Bukkit.getConsoleSender(), true);
			arena.disable();
			arena.save();
			arena.saveConfig();
			names.add(arena.getName());
		}
		config.set("arenas", names);
	}

	public void registerHooks()
	{
		PluginManager pm = this.getServer().getPluginManager();
		playerListener = new CrazyArenaPlayerListener();
		blocklistener = new CrazyArenaBlockListener();
		pm.registerEvents(playerListener, this);
		pm.registerEvents(blocklistener, this);
	}

	public ArenaSet getArenas()
	{
		return arenas;
	}

	@Override
	public boolean command(CommandSender sender, String commandLabel, String[] args) throws CrazyCommandException
	{
		if (commandLabel.equalsIgnoreCase("join"))
		{
			if (sender instanceof ConsoleCommandSender)
				throw new CrazyCommandExecutorException(false);
			commandJoin((Player) sender, args);
			return true;
		}
		if (commandLabel.equalsIgnoreCase("spectate"))
		{
			if (sender instanceof ConsoleCommandSender)
				throw new CrazyCommandExecutorException(false);
			commandSpectate((Player) sender, args);
			return true;
		}
		if (commandLabel.equalsIgnoreCase("ready"))
		{
			if (sender instanceof ConsoleCommandSender)
				throw new CrazyCommandExecutorException(false);
			if (args.length != 0)
				throw new CrazyCommandUsageException("/ready");
			commandReady((Player) sender);
			return true;
		}
		if (commandLabel.equalsIgnoreCase("team") || commandLabel.equalsIgnoreCase("teams"))
		{
			if (sender instanceof ConsoleCommandSender)
				throw new CrazyCommandExecutorException(false);
			commandTeam((Player) sender, args);
			return true;
		}
		if (commandLabel.equalsIgnoreCase("leave"))
		{
			if (sender instanceof ConsoleCommandSender)
				throw new CrazyCommandExecutorException(false);
			if (args.length != 0)
				throw new CrazyCommandUsageException("/ready");
			commandLeave((Player) sender);
			return true;
		}
		if (commandLabel.equalsIgnoreCase("invite"))
		{
			if (sender instanceof ConsoleCommandSender)
				throw new CrazyCommandExecutorException(false);
			commandInvite((Player) sender, args);
			return true;
		}
		if (commandLabel.equalsIgnoreCase("score"))
		{
			if (sender instanceof ConsoleCommandSender)
				throw new CrazyCommandExecutorException(false);
			commandScore((Player) sender, args);
			return true;
		}
		return false;
	}

	private void commandJoin(Player player, String[] args) throws CrazyCommandException
	{
		if (player.hasPermission("crazyarena.join"))
			throw new CrazyCommandPermissionException();
		Arena arena = arenas.getArena(player);
		if (arena != null)
			throw new CrazyCommandCircumstanceException();
		switch (args.length)
		{
			case 0:
				arena = invitations.findDataVia1(player);
				if (arena == null)
					throw new CrazyCommandUsageException("/join <Arena/Player>");
				break;
			case 1:
				arena = arenas.getArena(args[0]);
				if (arena == null)
				{
					Player to = getServer().getPlayerExact(args[0]);
					if (to == null)
						to = getServer().getPlayer(args[0]);
					arena = arenas.getArena(to);
				}
				if (arena == null)
					throw new CrazyCommandNoSuchException("Arena/Player", args[0]);
				break;
			default:
				throw new CrazyCommandUsageException("/join <Arena/Player>");
		}
		if (!arena.isEnabled())
			throw new CrazyCommandCircumstanceException("when arena is enabled", "disabled");
		arena.join(player);
		return;
	}

	private boolean commandSpectate(Player player, String[] args) throws CrazyCommandException
	{
		if (player.hasPermission("crazyarena.spectate"))
			throw new CrazyCommandPermissionException();
		Arena arena = arenas.getArena(player);
		if (arena != null)
			throw new CrazyCommandCircumstanceException("when not in arena.", "(Currently in " + arena.getName() + ")");
		switch (args.length)
		{
			case 0:
				arena = invitations.findDataVia1(player);
				if (arena == null)
					throw new CrazyCommandUsageException("/spectate <Arena/Player>");
				break;
			case 1:
				arena = arenas.getArena(args[0]);
				if (arena == null)
				{
					Player to = getServer().getPlayerExact(args[0]);
					if (to == null)
						to = getServer().getPlayer(args[0]);
					arena = arenas.getArena(to);
				}
				if (arena == null)
					throw new CrazyCommandNoSuchException("Arena/Player", args[0]);
				break;
			default:
				throw new CrazyCommandUsageException("/spectate <Arena/Player>");
		}
		if (!arena.isEnabled())
			throw new CrazyCommandCircumstanceException("when arena is enabled", "disabled");
		arena.spectate(player);
		return true;
	}

	private boolean commandReady(Player player) throws CrazyCommandException
	{
		Arena arena = arenas.getArena(player);
		if (arena == null || !arena.isParticipant(player, ParticipantType.WAITING))
			throw new CrazyCommandCircumstanceException("when waiting inside an arena!");
		arena.ready(player);
		return true;
	}

	private boolean commandTeam(Player player, String[] args) throws CrazyCommandException
	{
		Arena arena = arenas.getArena(player);
		if (arena == null || !arena.isParticipant(player))
			throw new CrazyCommandCircumstanceException("when participating in an arena!");
		arena.team(player);
		return true;
	}

	private boolean commandLeave(Player player) throws CrazyCommandException
	{
		if (player.hasPermission("crazyarena.leave"))
			throw new CrazyCommandPermissionException();
		Arena arena = arenas.getArena(player);
		if (arena == null || !arena.isParticipant(player))
			throw new CrazyCommandCircumstanceException("when participating in an arena!");
		arena.leave(player);
		return true;
	}

	private boolean commandInvite(Player player, String[] args) throws CrazyCommandException
	{
		if (player.hasPermission("crazyarena.invite"))
			throw new CrazyCommandPermissionException();
		Arena arena = arenas.getArena(player);
		if (arena == null || !arena.isParticipant(player))
			throw new CrazyCommandCircumstanceException("when participating in an arena!");
		if (args.length == 0)
			throw new CrazyCommandUsageException("/invite <Player1> [Player...]");
		if (args[0] == "*")
		{
			if (player.hasPermission("crazyarena.invite.all"))
			{
				int anz = 0;
				for (Player invited : getServer().getOnlinePlayers())
					if (arenas.getArena(invited) == null)
					{
						anz++;
						invitations.setDataVia1(invited, arena);
						sendLocaleMessage("COMMAND.INVITATION.MESSAGE", invited, player.getName(), arena.getName());
					}
				sendLocaleMessage("COMMAND.INVITATION.SUMMARY", player, anz, arena.getName());
				return true;
			}
			else
				throw new CrazyCommandPermissionException();
		}
		int anz = 0;
		for (String name : args)
		{
			Player invited = getServer().getPlayerExact(name);
			if (invited == null)
				invited = getServer().getPlayer(name);
			if (invited == null)
				continue;
			anz++;
			invitations.setDataVia1(invited, arena);
			sendLocaleMessage("COMMAND.INVITATION.MESSAGE", invited, player.getName(), arena.getName());
		}
		sendLocaleMessage("COMMAND.INVITATION.SUMMARY", player, anz, arena.getName());
		return true;
	}

	private boolean commandScore(Player sender, String[] args)
	{
		// EDIT score
		// <Arena> [Player/Number]
		// Arena => 1.2.3.
		// Arena Player/Rank => Detailed Information about Player/Rank (lastest match maybe total)
		// implemented in Arena getArena->sendScoreInformation(sender,Player/Rank)
		//
		return false;
	}

	@Override
	public boolean commandMain(CommandSender sender, String commandLabel, String[] args) throws CrazyException
	{
		if (command(sender, commandLabel, args))
			return true;
		if (commandLabel.equalsIgnoreCase("enable"))
		{
			commandEnable(sender, args, true);
			return true;
		}
		if (commandLabel.equalsIgnoreCase("disable"))
		{
			commandEnable(sender, args, false);
			return true;
		}
		if (commandLabel.equalsIgnoreCase("import"))
		{
			commandImport(sender, args);
			return true;
		}
		if (commandLabel.equalsIgnoreCase("create") || commandLabel.equalsIgnoreCase("new"))
		{
			commandCreate(sender, args);
			return true;
		}
		if (commandLabel.equalsIgnoreCase("delete") || commandLabel.equalsIgnoreCase("remove"))
		{
			commandDelete(sender, args);
			return true;
		}
		if (commandLabel.equalsIgnoreCase("sel") || commandLabel.equalsIgnoreCase("select"))
		{
			commandSelect(sender, args);
			return true;
		}
		if (commandLabel.equalsIgnoreCase("forceready"))
		{
			commandForceReady(sender, args);
			return true;
		}
		if (commandLabel.equalsIgnoreCase("forcestop"))
		{
			commandForceStop(sender, args);
			return true;
		}
		if (commandLabel.equalsIgnoreCase("kick"))
		{
			commandKick(sender, args);
			return true;
		}
		if (sender instanceof ConsoleCommandSender)
			return false;
		Player player = (Player) sender;
		Arena arena = selection.get(player);
		if (arena == null)
			throw new CrazyCommandCircumstanceException("when an arena is selected!");
		if (arena.command(player, commandLabel, args))
			return true;
		return false;
	}

	private boolean commandEnable(CommandSender sender, String[] args, boolean enabled) throws CrazyCommandException
	{
		if (sender.hasPermission("crazyarena.arena.switchmode"))
			throw new CrazyCommandPermissionException();
		if (args.length != 1)
			throw new CrazyCommandUsageException("/crazyarena " + (enabled ? "enabled" : "disabled") + " <Arena>");
		String name = args[0];
		Arena arena = arenas.getArena(name);
		try
		{
			arena.setEnabled(enabled);
		}
		catch (CrazyArenaException e)
		{
			throw new CrazyCommandCrazyErrorException(e);
		}
		if (enabled)
		{
			sendLocaleMessage("COMMAND.ARENA.MODE.ENABLED", sender, name);
			if (sender != getServer().getConsoleSender())
				sendLocaleMessage("COMMAND.ARENA.MODE.ENABLED", getServer().getConsoleSender(), name);
		}
		else
		{
			sendLocaleMessage("COMMAND.ARENA.MODE.DISABLED", sender, name);
			if (sender != getServer().getConsoleSender())
				sendLocaleMessage("COMMAND.ARENA.MODE.DISABLED", getServer().getConsoleSender(), name);
		}
		return true;
	}

	private boolean commandImport(CommandSender sender, String[] args) throws CrazyCommandException
	{
		if (sender.hasPermission("crazyarena.arena.import"))
			throw new CrazyCommandPermissionException();
		if (args.length != 1)
			throw new CrazyCommandUsageException();
		String name = args[0];
		Arena arena = arenas.loadArena(name);
		if (arena == null)
			throw new CrazyCommandNoSuchException("ArenaFile", name);
		sendLocaleMessage("COMMAND.ARENA.LOADED", sender);
		if (sender != getServer().getConsoleSender())
			sendLocaleMessage("COMMAND.ARENA.LOADED", getServer().getConsoleSender());
		arena.sendInfo(sender);
		return true;
	}

	private boolean commandCreate(CommandSender sender, String[] args) throws CrazyCommandException
	{
		if (!sender.hasPermission("crazyarena.arena.create"))
			throw new CrazyCommandPermissionException();
		if (sender instanceof ConsoleCommandSender)
			throw new CrazyCommandExecutorException(false);
		if (args.length != 2)
			throw new CrazyCommandUsageException("/crazyarena create <Name> <ArenaClass/Type>");
		Player player = (Player) sender;
		String name = args[0];
		if (arenas.getArena(name) != null)
			throw new CrazyCommandAlreadyExistsException("Arena", name);
		String type = args[1];
		Class<?> clazz = arenaTypes.get(type);
		if (clazz == null)
			try
			{
				clazz = Class.forName(type);
			}
			catch (ClassNotFoundException e)
			{
				try
				{
					clazz = Class.forName("de.st_ddt.crazyarena.arenas." + type);
				}
				catch (ClassNotFoundException e2)
				{
					throw new CrazyCommandNoSuchException("ArenaClass/Type", type);
				}
			}
		if (!Arena.class.isAssignableFrom(clazz))
			throw new CrazyCommandParameterException(2, "ArenaClass/Type");
		Arena arena = null;
		try
		{
			arena = (Arena) clazz.getConstructor(String.class, World.class).newInstance(name, player.getWorld());
		}
		catch (Exception e)
		{
			throw new CrazyCommandErrorException(e);
		}
		if (arena == null)
			throw new CrazyCommandException();
		arenas.add(arena);
		sendLocaleMessage("COMMAND.ARENA.CREATED", player, arena.getName());
		selection.put(player, arena);
		sendLocaleMessage("COMMAND.ARENA.SELECTED", player, arena.getName());
		return true;
	}

	private boolean commandDelete(CommandSender sender, String[] args) throws CrazyCommandException
	{
		if (!sender.hasPermission("crazyarena.arena.delete"))
			throw new CrazyCommandPermissionException();
		if (args.length != 1)
			throw new CrazyCommandUsageException("/crazyarena kick <Player> [Player...]");
		String name = args[0];
		Arena arena = arenas.getArena(name);
		if (arena == null)
			throw new CrazyCommandNoSuchException("Arena", name);
		arenas.remove(arena);
		sendLocaleMessage("COMMAND.ARENA.DELETED", sender, name);
		sendLocaleMessage("COMMAND.ARENA.DELETED.RECOVER", sender, name);
		return true;
	}

	private boolean commandSelect(CommandSender sender, String[] args) throws CrazyCommandException
	{
		if (!sender.hasPermission("crazyarena.arena.modify"))
			throw new CrazyCommandPermissionException();
		if (sender instanceof ConsoleCommandSender)
			throw new CrazyCommandException();
		Player player = (Player) sender;
		switch (args.length)
		{
			case 0:
				Arena arena = selection.get(player);
				if (arena == null)
					sendLocaleMessage("COMMAND.ARENA.SELECTED.NONE", player);
				else
					sendLocaleMessage("COMMAND.ARENA.SELECTED", player, arena.getName());
				return true;
			case 1:
				arena = arenas.getArena(args[0]);
				if (arena == null)
					throw new CrazyCommandNoSuchException("Arena", args[0]);
				selection.put(player, arena);
				sendLocaleMessage("COMMAND.ARENA.SELECTED", player, arena.getName());
				return true;
			default:
				throw new CrazyCommandUsageException("/crazyarena select [Arena]");
		}
	}

	private boolean commandForceReady(CommandSender sender, String[] args) throws CrazyCommandException
	{
		if (!sender.hasPermission("crazyarena.forceready"))
			throw new CrazyCommandPermissionException();
		if (args.length != 1)
			throw new CrazyCommandUsageException("/crazyarena forceready <Arena>");
		Arena arena = arenas.getArena(args[0]);
		if (arena == null)
			throw new CrazyCommandNoSuchException("Arena", args[0]);
		for (Participant player : arena.getParticipants(ParticipantType.WAITING))
			arena.ready(player.getPlayer());
		return true;
	}

	private boolean commandForceStop(CommandSender sender, String[] args) throws CrazyCommandException
	{
		if (!sender.hasPermission("crazyarena.forcestop"))
			throw new CrazyCommandPermissionException();
		if (args.length != 1)
			throw new CrazyCommandUsageException("/crazyarena forcestop <Arena>");
		Arena arena = arenas.getArena(args[0]);
		if (arena == null)
			throw new CrazyCommandNoSuchException("Arena", args[0]);
		arena.stop(sender, true);
		sendLocaleMessage("COMMAND.ARENA.STOP.FORCE", getServer().getOnlinePlayers(), arena.getName());
		sendLocaleMessage("COMMAND.ARENA.STOP.FORCE", getServer().getConsoleSender(), arena.getName());
		return true;
	}

	private boolean commandKick(CommandSender sender, String[] args) throws CrazyCommandException
	{
		if (!sender.hasPermission("crazyarena.kick"))
			throw new CrazyCommandPermissionException();
		if (args.length == 0)
			throw new CrazyCommandUsageException("/crazyarena kick <Player> [Player...]");
		for (String name : args)
		{
			Player player = getServer().getPlayerExact(name);
			if (player == null)
				player = getServer().getPlayer(name);
			if (player == null)
				throw new CrazyCommandNoSuchException("Player", name);
			Arena arena = arenas.getArena(player);
			if (arena != null)
				arena.leave(player, true);
			sendLocaleMessage("COMMAND.ARENA.KICK", getServer().getOnlinePlayers(), arena.getName(), player.getName());
			sendLocaleMessage("COMMAND.ARENA.KICK", getServer().getConsoleSender(), arena.getName(), player.getName());
		}
		return true;
	}

	public static Map<String, Class<? extends Arena>> getArenaTypes()
	{
		return arenaTypes;
	}

	public static void registerArenaType(String type, Class<? extends Arena> clazz)
	{
		arenaTypes.put(type, clazz);
	}

	public static void registerArenaTypes(Map<String, Class<? extends Arena>> types)
	{
		for (Map.Entry<String, Class<? extends Arena>> type : types.entrySet())
			registerArenaType(type.getKey(), type.getValue());
	}

	public static void unregisterArenaType(String... types)
	{
		for (String type : types)
		{
			Class<? extends Arena> clazz = arenaTypes.remove(type);
			if (clazz != null)
				if (!arenaTypes.values().contains(clazz))
					getPlugin().getArenas().removeArenaType(clazz);
		}
	}

	public static void unregisterArenaType(Class<? extends Arena>... clazzes)
	{
		for (Class<? extends Arena> clazz : clazzes)
		{
			Set<Entry<String, Class<? extends Arena>>> entries = arenaTypes.entrySet();
			for (Entry<String, Class<? extends Arena>> entry : entries)
				if (entry.getValue().equals(arenaTypes))
					arenaTypes.remove(entry);
			getPlugin().getArenas().removeArenaType(clazz);
		}
	}

	public static void unregisterArenaType(Collection<Class<? extends Arena>> clazzes)
	{
		for (Class<? extends Arena> clazz : clazzes)
		{
			Set<Entry<String, Class<? extends Arena>>> entries = arenaTypes.entrySet();
			for (Entry<String, Class<? extends Arena>> entry : entries)
				if (entry.getValue().equals(arenaTypes))
					arenaTypes.remove(entry);
			getPlugin().getArenas().removeArenaType(clazz);
		}
	}
}
