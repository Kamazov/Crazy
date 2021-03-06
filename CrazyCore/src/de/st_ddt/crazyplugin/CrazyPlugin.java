package de.st_ddt.crazyplugin;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import de.st_ddt.crazyplugin.exceptions.CrazyCommandException;
import de.st_ddt.crazyplugin.exceptions.CrazyCommandNoSuchException;
import de.st_ddt.crazyplugin.exceptions.CrazyCommandPermissionException;
import de.st_ddt.crazyplugin.exceptions.CrazyCommandUsageException;
import de.st_ddt.crazyplugin.exceptions.CrazyException;
import de.st_ddt.crazyutil.ChatHelper;
import de.st_ddt.crazyutil.Commandable;
import de.st_ddt.crazyutil.Named;
import de.st_ddt.crazyutil.PairList;
import de.st_ddt.crazyutil.locales.CrazyLocale;

public abstract class CrazyPlugin extends CrazyLightPlugin implements Named, Commandable
{

	protected CrazyLocale locale = null;
	public final static SimpleDateFormat DateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
	private static final PairList<Class<? extends CrazyPlugin>, CrazyPlugin> plugins = new PairList<Class<? extends CrazyPlugin>, CrazyPlugin>();

	public static ArrayList<CrazyPlugin> getCrazyPlugins()
	{
		return plugins.getData2List();
	}

	public final static CrazyPlugin getPlugin(final Class<? extends CrazyPlugin> plugin)
	{
		return plugins.findDataVia1(plugin);
	}

	public final static CrazyPlugin getPlugin(final String name)
	{
		for (CrazyPlugin plugin : plugins.getData2List())
			if (plugin.getName().equalsIgnoreCase(name))
				return plugin;
		return null;
	}

	@Override
	public final boolean onCommand(final CommandSender sender, final Command command, final String commandLabel, final String[] args)
	{
		try
		{
			if (command(sender, commandLabel, args))
				return true;
			if (getDescription().getName().equalsIgnoreCase(commandLabel) || (commandLabel.equalsIgnoreCase(getShortPluginName())))
			{
				try
				{
					if (args.length == 0)
					{
						commandInfo(sender, new String[0]);
						return true;
					}
					String[] newArgs = ChatHelper.shiftArray(args, 1);
					if (commandMain(sender, args[0], newArgs))
						return true;
					if (args[0].equalsIgnoreCase("info"))
					{
						commandInfo(sender, newArgs);
						return true;
					}
					if (args[0].equalsIgnoreCase("reload"))
					{
						commandReload(sender, newArgs);
						return true;
					}
					if (args[0].equalsIgnoreCase("save"))
					{
						commandSave(sender, newArgs);
						return true;
					}
					if (args[0].equalsIgnoreCase("help"))
					{
						commandHelp(sender, newArgs);
						return true;
					}
					throw new CrazyCommandNoSuchException("Function", args[0]);
				}
				catch (CrazyCommandException e)
				{
					e.shiftCommandIndex();
					throw e;
				}
			}
		}
		catch (CrazyCommandException e)
		{
			e.setCommand(commandLabel, args);
			e.print(sender, getChatHeader());
			return true;
		}
		catch (CrazyException e)
		{
			e.print(sender, getChatHeader());
			return true;
		}
		return super.onCommand(sender, command, commandLabel, args);
	}

	public boolean command(final CommandSender sender, final String commandLabel, final String[] args) throws CrazyException
	{
		return false;
	}

	public boolean commandMain(final CommandSender sender, final String commandLabel, final String[] args) throws CrazyException
	{
		return false;
	}

	public void commandInfo(final CommandSender sender, final String[] newArgs)
	{
		sender.sendMessage(getChatHeader() + "Version " + getDescription().getVersion());
		sender.sendMessage(getChatHeader() + "Authors " + getDescription().getAuthors().toString());
	}

	public final void commandReload(final CommandSender sender, final String[] args) throws CrazyCommandException
	{
		if (!sender.hasPermission(getDescription().getName().toLowerCase() + ".reload"))
			throw new CrazyCommandPermissionException();
		if (args.length != 0)
			throw new CrazyCommandUsageException("/" + getDescription().getName().toLowerCase() + " reload");
		reloadConfig();
		load();
		sendLocaleRootMessage("CRAZYPLUGIN.COMMAND.CONFIG.RELOADED", sender);
	}

	private final void commandSave(final CommandSender sender, final String[] args) throws CrazyCommandException
	{
		if (!sender.hasPermission(getDescription().getName().toLowerCase() + ".save"))
			throw new CrazyCommandPermissionException();
		if (args.length != 0)
			throw new CrazyCommandUsageException("/" + getDescription().getName().toLowerCase() + " save");
		save();
		sendLocaleRootMessage("CRAZYPLUGIN.COMMAND.CONFIG.SAVED", sender);
	}

	public void commandHelp(final CommandSender sender, final String[] args)
	{
		sendLocaleRootMessage("CRAZYPLUGIN.COMMAND.HELP.NOHELP", sender);
	}

	protected String getShortPluginName()
	{
		return null;
	}

	@Override
	public void onLoad()
	{
		plugins.setDataVia1(this.getClass(), this);
		getDataFolder().mkdir();
		new File(getDataFolder().getPath() + "/lang").mkdirs();
		super.onLoad();
	}

	@Override
	public void onEnable()
	{
		ConfigurationSection config = getConfig();
		boolean updated = config.getString("version", "").equals(getDescription().getVersion());
		config.set("version", getDescription().getVersion());
		for (String language : CrazyLocale.getLoadedLanguages())
			loadLanguage(language, updated);
		checkLocale();
		load();
		if (updated)
			save();
		super.onEnable();
	}

	@Override
	public void onDisable()
	{
		save();
		super.onDisable();
	}

	public void save()
	{
		saveConfig();
	}

	public void load()
	{
	}

	public void checkLocale()
	{
		locale = CrazyLocale.getPluginHead(this);
	}

	public final void sendLocaleMessage(final String localepath, final CommandSender target, final Object... args)
	{
		sendLocaleMessage(getLocale().getLanguageEntry(localepath), target, args);
	}

	public final void sendLocaleRootMessage(final String localepath, final CommandSender target, final Object... args)
	{
		sendLocaleMessage(CrazyLocale.getLocaleHead().getLanguageEntry(localepath), target, args);
	}

	public final void sendLocaleMessage(final CrazyLocale locale, final CommandSender target, final Object... args)
	{
		ChatHelper.sendMessage(target, getChatHeader(), locale, args);
	}

	public final void sendLocaleMessage(final String localepath, final CommandSender[] targets, final Object... args)
	{
		sendLocaleMessage(getLocale().getLanguageEntry(localepath), targets, args);
	}

	public final void sendLocaleRootMessage(final String localepath, final CommandSender[] targets, final Object... args)
	{
		sendLocaleMessage(CrazyLocale.getLocaleHead().getLanguageEntry(localepath), targets, args);
	}

	public final void sendLocaleMessage(final CrazyLocale locale, final CommandSender[] targets, final Object... args)
	{
		ChatHelper.sendMessage(targets, getChatHeader(), locale, args);
	}

	public final void sendLocaleMessage(final String localepath, final Collection<CommandSender> targets, final Object... args)
	{
		sendLocaleMessage(getLocale().getLanguageEntry(localepath), targets, args);
	}

	public final void sendLocaleRootMessage(final String localepath, final Collection<CommandSender> targets, final Object... args)
	{
		sendLocaleMessage(CrazyLocale.getLocaleHead().getLanguageEntry(localepath), targets, args);
	}

	public final void sendLocaleMessage(final CrazyLocale locale, final Collection<CommandSender> targets, final Object... args)
	{
		ChatHelper.sendMessage(targets, getChatHeader(), locale, args);
	}

	public final void broadcastLocaleMessage(final String localepath, final Object... args)
	{
		broadcastLocaleMessage(getLocale().getLanguageEntry(localepath), args);
	}

	public final void broadcastLocaleRootMessage(final String localepath, final Object... args)
	{
		broadcastLocaleMessage(CrazyLocale.getLocaleHead().getLanguageEntry(localepath), args);
	}

	public final void broadcastLocaleMessage(final CrazyLocale locale, final Object... args)
	{
		sendLocaleMessage(locale, getServer().getConsoleSender(), args);
		sendLocaleMessage(locale, getServer().getOnlinePlayers(), args);
	}

	public final void broadcastLocaleMessage(final boolean console, final String permission, final String localepath, final Object... args)
	{
		broadcastLocaleMessage(console, permission, getLocale().getLanguageEntry(localepath), args);
	}

	public final void broadcastLocaleRootMessage(final boolean console, final String permission, final String localepath, final Object... args)
	{
		broadcastLocaleMessage(console, permission, CrazyLocale.getLocaleHead().getLanguageEntry(localepath), args);
	}

	public final void broadcastLocaleMessage(final boolean console, final String permission, final CrazyLocale locale, final Object... args)
	{
		if (console)
			sendLocaleMessage(locale, Bukkit.getConsoleSender(), args);
		for (Player player : Bukkit.getOnlinePlayers())
			if (permission != null)
				if (player.hasPermission(permission))
					sendLocaleMessage(locale, player, args);
	}

	public final CrazyLocale getLocale()
	{
		return locale;
	}

	public void loadLanguage(final String language)
	{
		loadLanguage(language, Bukkit.getConsoleSender(), false);
	}

	public void loadLanguage(final String language, boolean forceDownload)
	{
		loadLanguage(language, Bukkit.getConsoleSender(), forceDownload);
	}

	public void loadLanguageDelayed(final String language, final CommandSender sender)
	{
		getServer().getScheduler().scheduleAsyncDelayedTask(this, new LanguageLoadRunnable(this, language, sender));
	}

	public void loadLanguage(final String language, final CommandSender sender)
	{
		loadLanguage(language, sender, false);
	}

	public void loadLanguage(final String language, final CommandSender sender, boolean forceDownload)
	{
		// default files
		File file = new File(getDataFolder().getPath() + "/lang/" + language + ".lang");
		if (!file.exists() || forceDownload)
		{
			downloadLanguage(language);
			if (!file.exists())
			{
				unpackLanguage(language);
				if (!file.exists())
				{
					sendLocaleRootMessage("CRAZYPLUGIN.LANGUAGE.ERROR.AVAILABLE", sender, language);
					return;
				}
			}
		}
		try
		{
			InputStream stream = null;
			InputStreamReader reader = null;
			try
			{
				stream = new FileInputStream(file);
				reader = new InputStreamReader(stream, "UTF-8");
				CrazyLocale.readFile(language, reader);
				// sendLocaleRootMessage("CRAZYPLUGIN.LANGUAGE.LOADED", sender, language);
			}
			finally
			{
				if (reader != null)
					reader.close();
				if (stream != null)
					stream.close();
			}
		}
		catch (IOException e)
		{
			sendLocaleRootMessage("CRAZYPLUGIN.LANGUAGE.ERROR.READ", sender, language);
		}
		// Custom files:
		file = new File(getDataFolder().getPath() + "/lang/custom_" + language + ".lang");
		if (file.exists())
		{
			try
			{
				InputStream stream = null;
				InputStreamReader reader = null;
				try
				{
					stream = new FileInputStream(file);
					reader = new InputStreamReader(stream, "UTF-8");
					CrazyLocale.readFile(language, reader);
					// sendLocaleRootMessage("CRAZYPLUGIN.LANGUAGE.LOADED", sender, language + " (Custom)");
				}
				finally
				{
					if (reader != null)
						reader.close();
					if (stream != null)
						stream.close();
				}
			}
			catch (IOException e)
			{
				sendLocaleRootMessage("CRAZYPLUGIN.LANGUAGE.ERROR.READ", sender, language + " (Custom)");
			}
		}
	}

	public void unpackLanguage(final String language)
	{
		try
		{
			InputStream stream = null;
			InputStream in = null;
			OutputStream out = null;
			try
			{
				stream = getClass().getResourceAsStream("/resource/lang/" + language + ".lang");
				if (stream == null)
					return;
				in = new BufferedInputStream(stream);
				out = new BufferedOutputStream(new FileOutputStream(getDataFolder().getPath() + "/lang/" + language + ".lang"));
				byte data[] = new byte[1024];
				int count;
				while ((count = in.read(data, 0, 1024)) != -1)
					out.write(data, 0, count);
				out.flush();
			}
			finally
			{
				if (out != null)
					out.close();
				if (stream != null)
					stream.close();
				if (in != null)
					in.close();
			}
		}
		catch (IOException e)
		{
			sendLocaleRootMessage("CRAZYPLUGIN.LANGUAGE.ERROR.EXPORT", getServer().getConsoleSender(), language);
		}
	}

	public String getMainDownloadLocation()
	{
		return "https://raw.github.com/ST-DDT/Crazy/master/" + getDescription().getName() + "/src/resource";
	}

	public void downloadLanguage(final String language)
	{
		downloadLanguage(language, Bukkit.getConsoleSender());
	}

	public void downloadLanguage(final String language, CommandSender sender)
	{
		try
		{
			InputStream stream = null;
			BufferedInputStream in = null;
			FileOutputStream out = null;
			try
			{
				stream = new URL(getMainDownloadLocation() + "/lang/" + language + ".lang").openStream();
				if (stream == null)
					return;
				in = new BufferedInputStream(stream);
				out = new FileOutputStream(getDataFolder().getPath() + "/lang/" + language + ".lang");
				byte data[] = new byte[1024];
				int count;
				while ((count = in.read(data, 0, 1024)) != -1)
					out.write(data, 0, count);
				out.flush();
			}
			finally
			{
				if (in != null)
					in.close();
				if (stream != null)
					stream.close();
				if (out != null)
					out.close();
			}
		}
		catch (IOException e)
		{
			sendLocaleRootMessage("CRAZYPLUGIN.LANGUAGE.ERROR.DOWNLOAD", sender, language);
		}
	}
}
