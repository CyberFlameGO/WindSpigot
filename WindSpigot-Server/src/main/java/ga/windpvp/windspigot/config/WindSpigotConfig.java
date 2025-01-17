package ga.windpvp.windspigot.config;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.sugarcanemc.sugarcane.util.yaml.YamlCommenter;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;

import ga.windpvp.windspigot.WindSpigot;
import ga.windpvp.windspigot.async.pathsearch.AsyncNavigation;
import ga.windpvp.windspigot.entity.EntityTickLimiter;
import me.elier.nachospigot.config.NachoConfig;

public class WindSpigotConfig {

	private static final Logger LOGGER = LogManager.getLogger(WindSpigotConfig.class);
	private static File CONFIG_FILE;
	protected static final YamlCommenter c = new YamlCommenter();
	private static final String HEADER = "This is the main configuration file for WindSpigot.\n"
			+ "As you can see, there's tons to configure. Some options may impact gameplay, so use\n"
			+ "with caution, and make sure you know what each option does before configuring.\n" + "\n"
			+ "If you need help with the configuration or have any questions related to WindSpigot,\n"
			+ "join us in our Discord.\n" + "\n" + "Discord: https://discord.gg/kAbTsFkbmN\n";

	static YamlConfiguration config;
	static int version;

	public static void init(File configFile) {
		CONFIG_FILE = configFile;
		config = new YamlConfiguration();
		try {
			WindSpigot.LOGGER.info("Loading WindSpigot config from " + configFile.getName());
			config.load(CONFIG_FILE);
		} catch (IOException ignored) {
		} catch (InvalidConfigurationException ex) {
			LOGGER.log(Level.ERROR, "Could not load windspigot.yml, please correct your syntax errors", ex);
			throw Throwables.propagate(ex);
		}
		config.options().copyDefaults(true);

		int configVersion = 19; // Update this every new configuration update

    version = getInt("config-version", configVersion);
		set("config-version", configVersion);
		c.setHeader(HEADER);
		c.addComment("config-version", "Configuration version, do NOT modify this!");
		readConfig(WindSpigotConfig.class, null);
	}
	
	// Not private as the config is read by calling all private methods with 0 params
	static void makeReadable() {
		LOGGER.warn("Waiting for 10 seconds so this can be read...");
		
		try {
			TimeUnit.SECONDS.sleep(10);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	static void readConfig(Class<?> clazz, Object instance) {
		for (Method method : clazz.getDeclaredMethods()) {
			if (Modifier.isPrivate(method.getModifiers())) {
				if (method.getParameterTypes().length == 0 && method.getReturnType() == Void.TYPE) {
					try {
						method.setAccessible(true);
						method.invoke(instance);
					} catch (InvocationTargetException ex) {
						throw Throwables.propagate(ex.getCause());
					} catch (Exception ex) {
						LOGGER.log(Level.ERROR, "Error invoking " + method, ex);
					}
				}
			}
		}

		try {
			config.save(CONFIG_FILE);
			c.saveComments(CONFIG_FILE);
		} catch (Exception ex) {
			LOGGER.log(Level.ERROR, "Could not save " + CONFIG_FILE, ex);
			
			LOGGER.warn("Please regenerate your windspigot.yml file to prevent this issue! The server will run with the default config for now.");

			makeReadable();
		}
	}

	private static void set(String path, Object val) {
		config.set(path, val);
	}

	private static boolean getBoolean(String path, boolean def) {
		config.addDefault(path, def);
		return config.getBoolean(path, config.getBoolean(path));
	}

	private static double getDouble(String path, double def) {
		config.addDefault(path, def);
		return config.getDouble(path, config.getDouble(path));
	}

	private static float getFloat(String path, float def) {
		config.addDefault(path, def);
		return config.getFloat(path, config.getFloat(path));
	}

	private static int getInt(String path, int def) {
		config.addDefault(path, def);
		return config.getInt(path, config.getInt(path));
	}

	private static <T> List getList(String path, T def) {
		config.addDefault(path, def);
		return config.getList(path, config.getList(path));
	}

	private static String getString(String path, String def) {
		config.addDefault(path, def);
		return config.getString(path, config.getString(path));
	}
	
	// General header comments
	private static void comments() {
		c.addComment("settings.async", "Configuration for asynchronous things.");
		c.addComment("settings.pearl-passthrough", "Configuration for ender pearls passing through certain blocks. (Credits to FlamePaper)");
		c.addComment("settings.command", "Configuration for WindSpigot's commands");
		c.addComment("settings.max-tick-time", "Configuration for maximum entity tick time");
	}
	
	public static boolean disableTracking;
	public static int trackingThreads;

	private static void tracking() {
		disableTracking = !getBoolean("settings.async.entity-tracking.enable", true);
		c.addComment("settings.async.entity-tracking.enable", "Enables asynchronous entity tracking");
		trackingThreads = getInt("settings.async.entity-tracking.threads", 5);
		c.addComment("settings.async.entity-tracking.threads",
				"The amount of threads to use when asynchronous entity tracking is enabled.");
		
		c.addComment("settings.async.entity-tracking", "Configuration for the async entity tracker.");
	}

	public static boolean threadAffinity;

	private static void threadAffinity() {
		threadAffinity = getBoolean("settings.thread-affinity", false);
		c.addComment("settings.thread-affinity",
				"Only switch to true if your OS is properly configured!! (See https://github.com/OpenHFT/Java-Thread-Affinity#isolcpus) \nWhen properly configured on the OS this allocates an entire cpu core to the server, it improves performance but uses more cpu.");
	}

	public static boolean mobAiCmd;

	private static void mobAiCmd() {
		mobAiCmd = getBoolean("settings.command.mob-ai", true);
		c.addComment("settings.command.mob-ai",
				"Enables the command \"/mobai\" which toggles mob ai. Users require the permission windspigot.command.mobai");
	}

	public static boolean parallelWorld;

	private static void parallelWorld() {
		parallelWorld = getBoolean("settings.async.parallel-world", true);
		// Disable timings by making timings check a variable (Code from api can't
		// access server code, so we have to do this)
		// Please open a PR if you know of a better method to do this.
		if (parallelWorld) {
			TimingsCheck.setEnableTimings(false);
		} else {
			TimingsCheck.setEnableTimings(true);
		}
		c.addComment("settings.async.parallel-world",
				"Enables async world ticking, ticking is faster if there are more worlds. Timings and other profilers are not supported when using this.");
	}

	public static boolean limitedMobSpawns;

	private static void limitedMobSpawns() {
		limitedMobSpawns = getBoolean("settings.limited-mob-spawns", false);
		c.addComment("settings.limited-mob-spawns",
				"Disables mob spawning if TPS is lower than the specified threshold.");
	}

	public static double limitedMobSpawnsThreshold;

	private static void limitedMobSpawnsThreshold() {
		limitedMobSpawnsThreshold = getDouble("settings.limited-mob-spawns-threshold", 18);
		c.addComment("settings.limited-mob-spawns-threshold",
				"Threshold to disable mob spawning. This does not apply if limited mob spawns is not enabled. This option accepts decimals.");
	}
	
	// FlamePaper start - 0117-Pearl-through-blocks
	public static boolean pearlPassthroughFenceGate;

	private static void pearlPassthroughFenceGate() {
		pearlPassthroughFenceGate = getBoolean("settings.pearl-passthrough.fence-gate", false);
		c.addComment("settings.pearl-passthrough.fence-gate", "Allows pearls to pass through fences.");
	}

	public static boolean pearlPassthroughTripwire;

	private static void pearlPassthroughTripwire() {
		pearlPassthroughTripwire = getBoolean("settings.pearl-passthrough.tripwire", false);
		c.addComment("settings.pearl-passthrough.tripwire", "Allows pearls to pass through tripwires.");
	}

	public static boolean pearlPassthroughSlab;

	private static void pearlPassthroughSlab() {
		pearlPassthroughSlab = getBoolean("settings.pearl-passthrough.slab", false);
		c.addComment("settings.pearl-passthrough.slab", "Allows pearls to pass through slabs.");
	}

	public static boolean pearlPassthroughCobweb;

	private static void pearlPassthroughCobweb() {
		pearlPassthroughCobweb = getBoolean("settings.pearl-passthrough.cobweb", false);
		c.addComment("settings.pearl-passthrough.cobweb", "Allows pearls to pass through cobwebs.");
	}

	public static boolean pearlPassthroughBed;

	private static void pearlPassthroughBed() {
		pearlPassthroughBed = getBoolean("settings.pearl-passthrough.bed", false);
		c.addComment("settings.pearl-passthrough.bed", "Allows pearls to pass through beds.");
	}
	// FlamePaper end
	
	// From
	// https://github.com/Argarian-Network/NachoSpigot/tree/async-kb-hit
    public static int combatThreadTPS;

    private static void combatThread() {
        combatThreadTPS = getInt("settings.async.combat-thread-tps", 40);        
        c.addComment("settings.async.combat-thread-tps", "Combat thread TPS for async knockback.");
    }

    // public static boolean asyncHitDetection;
    public static boolean asyncKnockback;

    private static void asyncPackets() {
    	// We use Nacho's implementation of instant interactions for async hit detection
        NachoConfig.instantPlayInUseEntity = getBoolean("settings.async.hit-detection", true);
        asyncKnockback = getBoolean("settings.async.knockback", false);
        c.addComment("settings.async.hit-detection", "Enables instant hit detection. This overrides the \"instant-interaction\" setting in nacho.yml (Credits to NachoSpigot).");
        c.addComment("settings.async.knockback", "Enables asynchronous knockback. This increases overall cpu usage, but sends knockback packets faster. Disable this if you do not run a pvp server. \nThis may be incompatible with a few plugins that listen to knockback packets. Test before using in production.");
    }
    
	public static boolean pingCmd;
	public static String pingSelfCmdString;
	public static String pingOtherCmdString;

	private static void pingCmd() {
		pingCmd = getBoolean("settings.command.ping.enable", true);
		
		pingSelfCmdString = getString("settings.command.ping.self-ping-msg", "&bYour ping: &3%ping%");
		pingOtherCmdString = getString("settings.command.ping.other-ping-msg", "&3%player%'s &bping: &3%ping%");
		c.addComment("settings.command.ping.enable",
				"Enables the command \"/ping <player>\" which shows player ping. Users require the permission windspigot.command.ping");
		c.addComment("settings.command.ping.self-ping-msg", "The message displayed for the /ping command");
		c.addComment("settings.command.ping.other-ping-msg", "The message displayed for the /ping <player> command");
	}
	
	public static boolean asyncTnt;

	private static void asyncTnt() {
		asyncTnt = getBoolean("settings.async.tnt", true);
		c.addComment("settings.async.tnt", "Enables async tnt (Credits to NachoSpigot).");
	}
	
	public static boolean statistics;
	
	private static void statistics() {
		statistics = getBoolean("settings.statistics", true);
		c.addComment("settings.statistics",
				"Enables WindSpigot statistics. This allows developers to see how many WindSpigot servers are running. \nThis has no performance impact and is completely anonymous, but you can opt out of this if you want.");
	}
	
	public static int hitDelay;
	
	private static void hitDelay() {
		hitDelay = getInt("settings.hit-delay", 20);
		c.addComment("settings.hit-delay", "This sets the delay between player attacks, 20 is the default. Setting this to 0 allows for no hit delay.");
	}
	
	public static double potionSpeed;
	
	private static void potionSpeed() {
		potionSpeed = getDouble("settings.potion-speed-offset", 0);
		c.addComment("settings.potion-speed-offset", "This sets the speed offset of splash potions, 0 is the default speed. Setting this higher makes potions splash faster. \nThis config option accepts decimals.");
	}
	
	public static boolean showPlayerIps;
	
	private static void showPlayerIps() {
		showPlayerIps = getBoolean("settings.show-player-ips", true);
		c.addComment("settings.show-player-ips", "Disabling this will prevent display of player ips in the console.");
	}
	
	public static boolean modernKeepalive;
	
	private static void modernKeepalive() {
		modernKeepalive = getBoolean("settings.modern-keep-alive", false);
		c.addComment("settings.modern-keep-alive", "This enables keep alive handling from modern Minecraft. This may break some plugins.");
	}
	
	public static boolean asyncPathSearches;
	public static int distanceToAsync;
	
	public static int pathSearchThreads;
	
	private static void asyncPathSearches() {
		asyncPathSearches = getBoolean("settings.async.path-searches.enabled", true);
		
		if (asyncPathSearches) {
	
			distanceToAsync = getInt("settings.async.path-searches.distance-to-async", 3);
			AsyncNavigation.setMinimumDistanceForOffloading(distanceToAsync);
			
			pathSearchThreads = getInt("settings.async.path-searches.threads", 3);
			
			if (pathSearchThreads > 4) {
				LOGGER.warn("The \"threads\" setting in windspigot.yml is very high! Having this too high will result in no performance gain as there are unused threads!");
				makeReadable();
			}
			
		} 
		c.addComment("settings.async.path-searches.enabled", "Enables async path searching for entities. (Credits to Minetick)");
		c.addComment("settings.async.path-searches.distance-to-async", "The mininum distance an entity is targeting to handle it async. It is recommended to use the default value.");
		c.addComment("settings.async.path-searches.threads", "The threads used for path searches. It is recommended to use the default value.");
		
		c.addComment("settings.async.path-searches", "Configuration for async entity path searches");
	}
	
	public static boolean debugMode;
	
	private static void debugMode() {
		debugMode = getBoolean("settings.debug-mode", false);
		c.addComment("settings.debug-mode", "This outputs information to developers in the console. There is no need to enable this.");
	}

	public static int tileMaxTickTime;
	public static int entityMaxTickTime;

	private static void maxTickTimes() {
		entityMaxTickTime = getInt("settings.max-tick-time.entity", 25);
		tileMaxTickTime = 1000; // We do not re-implement the tile entity tick cap, so we disable it by setting it to 1000
		
		c.addComment("settings.max-tick-time.entity", "The maximum time that entities can take to tick before moving on. This may break some gameplay, so set to 1000 to disable. \nFor reference, there are 50 ms in a tick. This setting makes it so that entities can only take up half the tick.");
	}
	
	public static boolean stopMobSpawnsDuringOverload;
	
	@SuppressWarnings({ "unchecked", "deprecation" })
	private static void skippableEntities() {
		List<String> skippableEntities = getList("settings.max-tick-time.skippable-entities",
				Lists.newArrayList("BAT", "BLAZE", "CHICKEN", "COW", "CREEPER", "ENDERMAN", "HORSE", "IRON_GOLEM",
						"MAGMA_CUBE", "MUSHROOM_COW", "PIG", "PIG_ZOMBIE", "RABBIT", "SHEEP", "SKELETON", "SILVERFISH",
						"SLIME", "SNOWMAN", "SQUID", "WITCH", "ZOMBIE"));
		
		List<EntityType> finalEntities = Lists.newArrayList();
		
		for (String entityName : skippableEntities) {
			finalEntities.add(EntityType.fromName(entityName));
		}
		EntityTickLimiter.addSkippableEntities(finalEntities);
		
		stopMobSpawnsDuringOverload = getBoolean("settings.max-tick-time.limit-on-overload", false);
		
		c.addComment("settings.max-tick-time.skippable-entities", "The entity types that can be skipped when ticking. They will only be skipped if the server is lagging based on the set threshold. \nRemove entities from this list if their vanilla behavior is absolutely needed on your server.");
		c.addComment("settings.max-tick-time.limit-on-overload", "If the server should stop mob spawns when there are too many mobs to handle and some have to be skipped.");
	}
	
}
