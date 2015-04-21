
package org.asterion;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.gson.Gson;
import com.google.inject.*;
import com.google.inject.util.Modules;
import org.apache.commons.io.FileUtils;
import org.asterion.store.DataStore;
import org.json.JSONException;
import org.json.JSONWriter;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.io.*;
import java.lang.reflect.Constructor;
import java.nio.charset.Charset;
import java.util.*;

import static com.google.common.base.Strings.isNullOrEmpty;

public class Main
{
	public static final Logger logger = (Logger) LoggerFactory.getLogger(Main.class);

	public static final Charset UTF_8 = Charset.forName("UTF-8");
	public static final String SERVICE_PREFIX = "kairosdb.service";

	private final static Object s_shutdownObject = new Object();

	private static final Arguments arguments = new Arguments();

	private Injector m_injector;
	private List<AsterionService> m_services = new ArrayList<AsterionService>();

	private void loadPlugins(Properties props, final File propertiesFile) throws IOException
	{
		File propDir = propertiesFile.getParentFile();
		if (propDir == null)
			propDir = new File(".");

		String[] pluginProps = propDir.list(new FilenameFilter()
		{
			@Override
			public boolean accept(File dir, String name)
			{
				return (name.endsWith(".properties") && !name.equals(propertiesFile.getName()));
			}
		});

		ClassLoader cl = getClass().getClassLoader();

		for (String prop : pluginProps)
		{
			logger.info("Loading plugin properties: {}", prop);
			//Load the properties file from a jar if there is one first.
			//This way defaults can be set
			InputStream propStream = cl.getResourceAsStream(prop);

			if (propStream != null)
			{
				props.load(propStream);
				propStream.close();
			}

			//Load the file in
			FileInputStream fis = new FileInputStream(new File(propDir, prop));
			props.load(fis);
			fis.close();
		}
	}


	public Main(File propertiesFile) throws IOException
	{
		Properties props = new Properties();
		InputStream is = getClass().getClassLoader().getResourceAsStream("kairosdb.properties");
		props.load(is);
		is.close();

		if (propertiesFile != null)
		{
			FileInputStream fis = new FileInputStream(propertiesFile);
			props.load(fis);
			fis.close();

			loadPlugins(props, propertiesFile);
		}

		List<Module> moduleList = new ArrayList<Module>();
		moduleList.add(new CoreModule(props));

		for (String propName : props.stringPropertyNames())
		{
			if (propName.startsWith(SERVICE_PREFIX))
			{
				Class<?> aClass;
				try
				{
					if ("".equals(props.getProperty(propName)))
						continue;

					aClass = Class.forName(props.getProperty(propName));
					if (Module.class.isAssignableFrom(aClass))
					{
						Constructor<?> constructor = null;

						try
						{
							constructor = aClass.getConstructor(Properties.class);
						}
						catch (NoSuchMethodException ignore)
						{
						}

						/*
					    Check if they have a constructor that takes the properties
						if not construct using the default constructor
						 */
						Module mod;
						if (constructor != null)
							mod = (Module) constructor.newInstance(props);
						else
							mod = (Module) aClass.newInstance();

						if (mod instanceof CoreModule)
						{
							mod = Modules.override(moduleList).with(mod);
							moduleList.set(0, mod);
						}
						else
							moduleList.add(mod);
					}
				}
				catch (Exception e)
				{
					logger.error("Unable to load service " + propName, e);
				}
			}
		}

		m_injector = Guice.createInjector(moduleList);
	}


	public static void main(String[] args) throws Exception
	{
		//This sends jersey java util logging to logback
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();

		JCommander commander = new JCommander(arguments);
		try
		{
			commander.parse(args);
		}
		catch (Exception e)
		{
			System.out.println(e.getMessage());
			commander.usage();
			System.exit(0);
		}

		if (arguments.helpMessage || arguments.help)
		{
			commander.usage();
			System.exit(0);
		}

		if (!arguments.operationCommand.equals("run"))
		{
			//Turn off console logging
			Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
			root.getAppender("stdout").addFilter(new Filter<ILoggingEvent>()
			{
				@Override
				public FilterReply decide(ILoggingEvent iLoggingEvent)
				{
					return (FilterReply.DENY);
				}
			});
		}

		File propertiesFile = null;
		if (!isNullOrEmpty(arguments.propertiesFile))
			propertiesFile = new File(arguments.propertiesFile);

		final Main main = new Main(propertiesFile);


		if (arguments.operationCommand.equals("run") || arguments.operationCommand.equals("start"))
		{
			try
			{
				Runtime.getRuntime().addShutdownHook(new Thread(new Runnable()
				{
					public void run()
					{
						try
						{
							main.stopServices();

							synchronized (s_shutdownObject)
							{
								s_shutdownObject.notify();
							}
						}
						catch (Exception e)
						{
							logger.error("Shutdown exception:", e);
						}
					}
				}));

				main.startServices();

				logger.info("------------------------------------------");
				logger.info("     KairosDB service started");
				logger.info("------------------------------------------");

				waitForShutdown();
			}
			catch (Exception e)
			{
				logger.error("Failed starting up services", e);
				//main.stopServices();
				System.exit(0);
			}
			finally
			{
				logger.info("--------------------------------------");
				logger.info("     KairosDB service is now down!");
				logger.info("--------------------------------------");
			}

		}
	}

	public Injector getInjector()
	{
		return (m_injector);
	}




	/**
	 * Simple technique to prevent the main thread from existing until we are done
	 */
	private static void waitForShutdown()
	{
		try
		{
			synchronized (s_shutdownObject)
			{
				s_shutdownObject.wait();
			}
		}
		catch (InterruptedException ignore)
		{
		}
	}


	public void startServices() throws AsterionException
	{
		Map<Key<?>, Binding<?>> bindings =
				m_injector.getAllBindings();

		for (Key<?> key : bindings.keySet())
		{
			Class bindingClass = key.getTypeLiteral().getRawType();
			if (AsterionService.class.isAssignableFrom(bindingClass))
			{
				AsterionService service = (AsterionService) m_injector.getInstance(bindingClass);
				logger.info("Starting service " + bindingClass);
				service.start();
				m_services.add(service);
			}
		}
	}


	public void stopServices() throws InterruptedException
	{
		logger.info("Shutting down");
		for (AsterionService service : m_services)
		{
			String serviceName = service.getClass().getName();
			logger.info("Stopping " + serviceName);
			try
			{
				service.stop();
				logger.info("Stopped  " + serviceName);
			}
			catch (Exception e)
			{
				logger.error("Error stopping " + serviceName, e);
			}
		}

		//Stop the datastore
		DataStore ds = m_injector.getInstance(DataStore.class);
		ds.close();
	}

	private class RecoveryFile
	{
		private final Set<String> metricsExported = new HashSet<String>();

		private File recoveryFile;
		private PrintWriter writer;

		public RecoveryFile() throws IOException
		{
			if (!isNullOrEmpty(arguments.exportRecoveryFile))
			{
				recoveryFile = new File(arguments.exportRecoveryFile);
				logger.info("Tracking exported metric names in " + recoveryFile.getAbsolutePath());

				if (recoveryFile.exists())
				{
					logger.info("Skipping metrics found in " + recoveryFile.getAbsolutePath());
					List<String> list = FileUtils.readLines(recoveryFile);
					metricsExported.addAll(list);
				}

				writer = new PrintWriter(new FileOutputStream(recoveryFile, true));
			}
		}

		public boolean contains(String metric)
		{
			return metricsExported.contains(metric);
		}

		public void writeMetric(String metric)
		{
			if (writer != null)
			{
				writer.println(metric);
				writer.flush();
			}
		}

		public void close()
		{
			if (writer != null)
				writer.close();
		}
	}


	@SuppressWarnings("UnusedDeclaration")
	private static class Arguments
	{
		@Parameter(names = "-p", description = "A custom properties file")
		private String propertiesFile;

		@Parameter(names = "-f", description = "File to save export to or read from depending on command.")
		private String exportFile;

		@Parameter(names = "-n", description = "Name of metrics to export. If not specified, then all metrics are exported.")
		private List<String> exportMetricNames;

		@Parameter(names = "-r", description = "Full path to a recovery file. The file tracks metrics that have been exported. " +
				"If export fails and is run again it uses this file to pickup where it left off.")
		private String exportRecoveryFile;

		@Parameter(names = "-a", description = "Appends to the export file. By default, the export file is overwritten.")
		private boolean appendToExportFile;

		@Parameter(names = "--help", description = "Help message.", help = true)
		private boolean helpMessage;

		@Parameter(names = "-h", description = "Help message.", help = true)
		private boolean help;

		/**
		 * start is identical to run except that logging data only goes to the log file
		 * and not to standard out as well
		 */
		@Parameter(names = "-c", description = "Command to run: export, import, run, start.")
		private String operationCommand;
	}
}
