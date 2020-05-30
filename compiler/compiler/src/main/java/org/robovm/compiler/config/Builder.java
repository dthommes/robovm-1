package org.robovm.compiler.config;

import org.robovm.compiler.config.tools.Tools;
import org.robovm.compiler.log.Logger;
import org.robovm.compiler.plugin.CompilerPlugin;
import org.robovm.compiler.plugin.LaunchPlugin;
import org.robovm.compiler.plugin.Plugin;
import org.robovm.compiler.plugin.PluginArgument;
import org.robovm.compiler.plugin.TargetPlugin;
import org.robovm.compiler.target.ios.ProvisioningProfile;
import org.robovm.compiler.target.ios.SigningIdentity;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.convert.Converter;
import org.simpleframework.xml.convert.Registry;
import org.simpleframework.xml.convert.RegistryStrategy;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.filter.PlatformFilter;
import org.simpleframework.xml.stream.Format;
import org.simpleframework.xml.stream.InputNode;
import org.simpleframework.xml.stream.OutputNode;
import org.simpleframework.xml.transform.RegistryMatcher;
import org.simpleframework.xml.transform.Transform;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.stream.Collectors;

@SuppressWarnings({"UnusedReturnValue", "unused"})
public class Builder {
    protected final Config config;

    Builder(Config config) {
        this.config = config;
    }

    public Builder() {
        this.config = new Config();
    }

    public Builder os(OS os) {
        config.os = os;
        return this;
    }

    public Builder arch(Arch arch) {
        return archs(arch);
    }

    public Builder archs(Arch ... archs) {
        return archs(Arrays.asList(archs));
    }

    public Builder archs(List<Arch> archs) {
        if (config.archs == null) {
            config.archs = new ArrayList<>();
        }
        config.archs.clear();
        config.archs.addAll(archs);
        return this;
    }

    public Builder clearClasspathEntries() {
        if (config.classpath != null) {
            config.classpath.clear();
        }
        return this;
    }

    public Builder addClasspathEntry(File f) {
        if (config.classpath == null) {
            config.classpath = new ArrayList<>();
        }
        config.classpath.add(f);
        return this;
    }

    public Builder clearBootClasspathEntries() {
        if (config.bootclasspath != null) {
            config.bootclasspath.clear();
        }
        return this;
    }

    public Builder addBootClasspathEntry(File f) {
        if (config.bootclasspath == null) {
            config.bootclasspath = new ArrayList<>();
        }
        config.bootclasspath.add(f);
        return this;
    }

    public Builder mainJar(File f) {
        config.mainJar = f;
        return this;
    }

    public Builder installDir(File installDir) {
        config.installDir = installDir;
        return this;
    }

    public Builder executableName(String executableName) {
        config.executableName = executableName;
        return this;
    }

    public Builder imageName(String imageName) {
        config.imageName = imageName;
        return this;
    }

    public Builder home(Home home) {
        config.home = home;
        return this;
    }

    public Builder cacheDir(File cacheDir) {
        config.cacheDir = cacheDir;
        return this;
    }

    public Builder clean(boolean b) {
        config.clean = b;
        return this;
    }

    public Builder ccBinPath(File ccBinPath) {
        config.ccBinPath = ccBinPath;
        return this;
    }

    public Builder debug(boolean b) {
        config.debug = b;
        return this;
    }

    public Builder useDebugLibs(boolean b) {
        config.useDebugLibs = b;
        return this;
    }

    public Builder dumpIntermediates(boolean b) {
        config.dumpIntermediates = b;
        return this;
    }

    public Builder manuallyPreparedForLaunch(boolean b) {
        config.manuallyPreparedForLaunch = b;
        return this;
    }

    public Builder skipRuntimeLib(boolean b) {
        config.skipRuntimeLib = b;
        return this;
    }

    public Builder skipLinking(boolean b) {
        config.skipLinking = b;
        return this;
    }

    public Builder skipInstall(boolean b) {
        config.skipInstall = b;
        return this;
    }

    public Builder threads(int threads) {
        config.threads = threads;
        return this;
    }

    public Builder mainClass(String mainClass) {
        config.mainClass = mainClass;
        return this;
    }

    public Builder tmpDir(File tmpDir) {
        config.tmpDir = tmpDir;
        return this;
    }

    public Builder logger(Logger logger) {
        config.logger = logger;
        return this;
    }

    public Builder treeShakerMode(TreeShakerMode treeShakerMode) {
        config.treeShakerMode = treeShakerMode;
        return this;
    }

    public Builder smartSkipRebuild(boolean smartSkipRebuild){
        config.smartSkipRebuild = smartSkipRebuild;
        return this;
    }

    public Builder clearForceLinkClasses() {
        if (config.forceLinkClasses != null) {
            config.forceLinkClasses.clear();
        }
        return this;
    }

    public Builder addForceLinkClass(String pattern) {
        if (config.forceLinkClasses == null) {
            config.forceLinkClasses = new ArrayList<>();
        }
        config.forceLinkClasses.add(pattern);
        return this;
    }

    public Builder clearExportedSymbols() {
        if (config.exportedSymbols != null) {
            config.exportedSymbols.clear();
        }
        return this;
    }

    public Builder addExportedSymbol(String symbol) {
        if (config.exportedSymbols == null) {
            config.exportedSymbols = new ArrayList<>();
        }
        config.exportedSymbols.add(symbol);
        return this;
    }

    public Builder clearUnhideSymbols() {
        if (config.unhideSymbols != null) {
            config.unhideSymbols.clear();
        }
        return this;
    }

    public Builder addUnhideSymbol(String symbol) {
        if (config.unhideSymbols == null) {
            config.unhideSymbols = new ArrayList<>();
        }
        config.unhideSymbols.add(symbol);
        return this;
    }

    public Builder clearLibs() {
        if (config.libs != null) {
            config.libs.clear();
        }
        return this;
    }

    public Builder addLib(Lib lib) {
        if (config.libs == null) {
            config.libs = new ArrayList<>();
        }
        config.libs.add(lib);
        return this;
    }

    public Builder clearFrameworks() {
        if (config.frameworks != null) {
            config.frameworks.clear();
        }
        return this;
    }

    public Builder addFramework(String framework) {
        if (config.frameworks == null) {
            config.frameworks = new ArrayList<>();
        }
        config.frameworks.add(framework);
        return this;
    }

    public Builder clearWeakFrameworks() {
        if (config.weakFrameworks != null) {
            config.weakFrameworks.clear();
        }
        return this;
    }

    public Builder addWeakFramework(String framework) {
        if (config.weakFrameworks == null) {
            config.weakFrameworks = new ArrayList<>();
        }
        config.weakFrameworks.add(framework);
        return this;
    }

    public Builder clearFrameworkPaths() {
        if (config.frameworkPaths != null) {
            config.frameworkPaths.clear();
        }
        return this;
    }

    public Builder addFrameworkPath(File frameworkPath) {
        if (config.frameworkPaths == null) {
            config.frameworkPaths = new ArrayList<>();
        }
        config.frameworkPaths.add(new QualifiedFile(frameworkPath));
        return this;
    }

    public Builder clearExtensions() {
        if (config.appExtensions != null) {
            config.appExtensions.clear();
        }
        return this;
    }

    public Builder addExtension(String name, String profile) {
        if (config.appExtensions == null) {
            config.appExtensions = new ArrayList<>();
        }
        AppExtension extension = new AppExtension();
        extension.name = name;
        extension.profile = profile;
        config.appExtensions.add(extension);
        return this;
    }

    public Builder clearExtensionPaths() {
        if (config.appExtensionPaths != null) {
            config.appExtensionPaths.clear();
        }
        return this;
    }

    public Builder addExtenaionPath(File extensionPath) {
        if (config.appExtensionPaths == null) {
            config.appExtensionPaths = new ArrayList<>();
        }
        config.appExtensionPaths.add(new QualifiedFile(extensionPath));
        return this;
    }

    public Builder clearResources() {
        if (config.resources != null) {
            config.resources.clear();
        }
        return this;
    }

    public Builder addResource(Resource resource) {
        if (config.resources == null) {
            config.resources = new ArrayList<>();
        }
        config.resources.add(resource);
        return this;
    }

    public Builder stripArchivesBuilder(StripArchivesConfig.StripArchivesBuilder stripArchivesBuilder) {
       this.config.stripArchivesBuilder = stripArchivesBuilder;
       return this;
   }

    public Builder targetType(String targetType) {
        config.targetType = targetType;
        return this;
    }

    public Builder clearProperties() {
        config.properties.clear();
        return this;
    }

    public Builder addProperties(Properties properties) {
        config.properties.putAll(properties);
        return this;
    }

    public Builder addProperties(File file) throws IOException {
        Properties props = new Properties();
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            props.load(reader);
            addProperties(props);
        }
        return this;
    }

    public Builder addProperty(String name, String value) {
        config.properties.put(name, value);
        return this;
    }

    public Builder cacerts(Cacerts cacerts) {
        config.cacerts = cacerts;
        return this;
    }

    public Builder tools(Tools tools) {
        config.tools = tools;
        return this;
    }

    public Builder iosSdkVersion(String sdkVersion) {
        config.iosSdkVersion = sdkVersion;
        return this;
    }

    public Builder iosDeviceType(String deviceType) {
        config.iosDeviceType = deviceType;
        return this;
    }

    public Builder iosInfoPList(File infoPList) {
        config.iosInfoPListFile = infoPList;
        return this;
    }

    public Builder infoPList(File infoPList) {
        config.infoPListFile = infoPList;
        return this;
    }

    public Builder iosEntitlementsPList(File entitlementsPList) {
        config.iosEntitlementsPList = entitlementsPList;
        return this;
    }

    public Builder iosSignIdentity(SigningIdentity signIdentity) {
        config.iosSignIdentity = signIdentity;
        return this;
    }

    public Builder iosProvisioningProfile(ProvisioningProfile iosProvisioningProfile) {
        config.iosProvisioningProfile = iosProvisioningProfile;
        return this;
    }

    public Builder iosSkipSigning(boolean b) {
        config.iosSkipSigning = b;
        return this;
    }

    public Builder addCompilerPlugin(CompilerPlugin compilerPlugin) {
        config.plugins.add(compilerPlugin);
        return this;
    }

    public Builder addLaunchPlugin(LaunchPlugin plugin) {
        config.plugins.add(plugin);
        return this;
    }

    public Builder addTargetPlugin(TargetPlugin plugin) {
        config.plugins.add(plugin);
        return this;
    }

    public Builder enableBitcode(boolean enableBitcode) {
        config.enableBitcode = enableBitcode;
        return this;
    }

    public void addPluginArgument(String argName) {
        if (config.pluginArguments == null) {
            config.pluginArguments = new ArrayList<>();
        }
        config.pluginArguments.add(argName);
    }

    public Config build() throws IOException {
        for (CompilerPlugin plugin : config.getCompilerPlugins()) {
            plugin.beforeConfig(this, config);
        }

        return config.build();
    }

    /**
     * Reads properties from a project basedir. If {@code isTest} is
     * {@code true} this method will first attempt to load a
     * {@code robovm.test.properties} file in {@code basedir}.
     * <p>
     * If no test specific file is found or if {@code isTest} is
     * {@code false} this method attempts to load a
     * {@code robovm.properties} and a {@code robovm.local.properties} file
     * in {@code basedir} and merges them so that properties from the local
     * file (if it exists) override properties in the non-local file.
     * <p>
     * If {@code isTest} is {@code true} and no test specific properties
     * file was found this method will append {@code Test} to the
     * {@code app.id} and {@code app.name} properties (if they exist).
     * <p>
     * If none of the files can be found found this method does nothing.
     */
    public void readProjectProperties(File basedir, boolean isTest) throws IOException {
        File testPropsFile = new File(basedir, "robovm.test.properties");
        File localPropsFile = new File(basedir, "robovm.local.properties");
        File propsFile = new File(basedir, "robovm.properties");
        if (isTest && testPropsFile.exists()) {
            config.logger.info("Loading test RoboVM config properties file: "
                    + testPropsFile.getAbsolutePath());
            addProperties(testPropsFile);
        } else {
            Properties props = new Properties();
            if (propsFile.exists()) {
                config.logger.info("Loading default RoboVM config properties file: "
                        + propsFile.getAbsolutePath());
                try (Reader reader = new InputStreamReader(new FileInputStream(propsFile), StandardCharsets.UTF_8)) {
                    props.load(reader);
                }
            }
            if (localPropsFile.exists()) {
                config.logger.info("Loading local RoboVM config properties file: "
                        + localPropsFile.getAbsolutePath());
                try (Reader reader = new InputStreamReader(new FileInputStream(localPropsFile), StandardCharsets.UTF_8)) {
                    props.load(reader);
                }
            }
            if (isTest) {
                modifyPropertyForTest(props, "app.id");
                modifyPropertyForTest(props, "app.name");
                modifyPropertyForTest(props, "app.executable");
            }
            addProperties(props);
        }
    }

    private void modifyPropertyForTest(Properties props, String propName) {
        String propValue = props.getProperty(propName);
        if (propValue != null && !propValue.endsWith("Test")) {
            String newPropValue = propValue + "Test";
            config.logger.info("Changing %s property from '%s' to '%s'", propName, propValue, newPropValue);
            props.setProperty(propName, newPropValue);
        }
    }

    /**
     * Reads a config file from a project basedir. If {@code isTest} is
     * {@code true} this method will first attempt to load a
     * {@code robovm.test.xml} file in {@code basedir}.
     * <p>
     * If no test-specific file is found or if {@code isTest} is
     * {@code false} this method attempts to load a {@code robovm.xml} file
     * in {@code basedir}.
     * <p>
     * If none of the files can be found found this method does nothing.
     */
    public void readProjectConfig(File basedir, boolean isTest) throws IOException {
        File testConfigFile = new File(basedir, "robovm.test.xml");
        File configFile = new File(basedir, "robovm.xml");
        if (isTest && testConfigFile.exists()) {
            config.logger.info("Loading test RoboVM config file: "
                    + testConfigFile.getAbsolutePath());
            read(testConfigFile);
        } else if (configFile.exists()) {
            config.logger.info("Loading default RoboVM config file: "
                    + configFile.getAbsolutePath());
            read(configFile);
        }
    }

    public void read(File file) throws IOException {
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            read(reader, file.getAbsoluteFile().getParentFile());
        }
    }

    public void read(Reader reader, File wd) throws IOException {
        try {
            Serializer serializer = createSerializer(config, wd);
            serializer.read(config, reader);
        } catch (IOException | RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }
        // <roots> was renamed to <forceLinkClasses> but we still support
        // <roots>. We need to copy <roots> to <forceLinkClasses> and set
        // <roots> to null.
        if (config.roots != null && !config.roots.isEmpty()) {
            if (config.forceLinkClasses == null) {
                config.forceLinkClasses = new ArrayList<>();
            }
            config.forceLinkClasses.addAll(config.roots);
            config.roots = null;
        }
    }

    public void write(File file) throws IOException {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            write(writer, file.getAbsoluteFile().getParentFile());
        }
    }

    public void write(Writer writer, File wd) throws IOException {
        try {
            Serializer serializer = createSerializer(config, wd);
            serializer.write(config, writer);
        } catch (IOException | RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public static Serializer createSerializer(Config config, final File wd) throws Exception {
        RelativeFileConverter fileConverter = new RelativeFileConverter(wd);

        Serializer resourceSerializer = new Persister(
                new RegistryStrategy(new Registry().bind(File.class, fileConverter)),
                new PlatformFilter(config.properties), new Format(2));

        Registry registry = new Registry();
        RegistryStrategy registryStrategy = new RegistryStrategy(registry);
        RegistryMatcher matcher = new RegistryMatcher();
        Serializer serializer = new Persister(registryStrategy,
                new PlatformFilter(config.properties), matcher, new Format(2));

        registry.bind(File.class, fileConverter);
        registry.bind(Lib.class, new RelativeLibConverter(fileConverter));
        registry.bind(Resource.class, new ResourceConverter(fileConverter, resourceSerializer));
        registry.bind(StripArchivesConfig.class, new StripArchivesConfigConverter());

        // converters for attributes (comma separated arrays)
        // adding file converter to matcher, as it fails to pick it from registry when writing
        // tag text of custom object (such as QualifiedFile)
        matcher.bind(File.class, fileConverter);
        matcher.bind(OS[].class, new EnumArrayConverter<>(OS.class));
        matcher.bind(Arch[].class, new EnumArrayConverter<>(Arch.class));
        matcher.bind(PlatformVariant[].class, new EnumArrayConverter<>(PlatformVariant.class));

        return serializer;
    }

    /**
     * Fetches the {@link PluginArgument}s of all registered plugins for
     * parsing.
     */
    public Map<String, PluginArgument> fetchPluginArguments() {
        Map<String, PluginArgument> args = new TreeMap<>();
        for (Plugin plugin : config.plugins) {
            for (PluginArgument arg : plugin.getArguments().getArguments()) {
                args.put(plugin.getArguments().getPrefix() + ":" + arg.getName(), arg);
            }
        }
        return args;
    }

    public List<Plugin> getPlugins() {
        return config.getPlugins();
    }

    /**
     * transformer for xml attribute/entry that transforms comma separated values into
     * enum array
     */
    private static final class EnumArrayConverter<T extends Enum<T>> implements Transform<T[]> {
        private final Class<T> enumClass;

        private EnumArrayConverter(Class<T> enumClass) {
            this.enumClass = enumClass;
        }

        @Override
        public T[] read(String s) {
            s = s.trim();
            if (s.isEmpty())
                return null;

            String[] tokens = s.split(",");
            @SuppressWarnings("unchecked")
            T[] res = (T[]) Array.newInstance(enumClass, tokens.length);
            for (int idx = 0; idx < tokens.length; idx++)
                res[idx] = Enum.valueOf(enumClass, tokens[idx].trim());
            return res;
        }

        @Override
        public String write(T[] ts) {
            return Arrays.stream(ts).map(Enum::name).collect(Collectors.joining());
        }
    }

    static final class RelativeFileConverter implements Converter<File>, Transform<File> {
        private final String wdPrefix;

        public RelativeFileConverter(File wd) {
            if (wd.isFile()) {
                wd = wd.getParentFile();
            }
            String prefix = wd.getAbsolutePath();
            if (prefix.endsWith(File.separator)) {
                prefix = prefix.substring(0, prefix.length() - 1);
            }
            wdPrefix = prefix;
        }

        @Override
        public File read(String value) {
            if (value == null) {
                return null;
            }
            File file = new File(value);
            if (!file.isAbsolute()) {
                file = new File(wdPrefix, value);
            }
            return file;
        }

        @Override
        public File read(InputNode node) throws Exception {
            return read(node.getValue());
        }

        @Override
        public String write(File value) {
            String path = value.isAbsolute() ? value.getAbsolutePath() : value.getPath();
            if (value.isAbsolute() && path.startsWith(wdPrefix)) {
                if (path.length() == wdPrefix.length())
                    path = "";
                else
                    path = path.substring(wdPrefix.length() + 1);
            }
            return path;
        }

        @Override
        public void write(OutputNode node, File value) throws Exception {
            String path = write(value);
            if (path.isEmpty()) {
                if ("directory".equals(node.getName())) {
                    // Skip
                    node.remove();
                } else {
                    node.setValue("");
                }
            } else {
                node.setValue(path);
            }
        }
    }

    private static final class ResourceConverter implements Converter<Resource> {
        private final RelativeFileConverter fileConverter;
        private final Serializer serializer;

        public ResourceConverter(RelativeFileConverter fileConverter, Serializer serializer) {
            this.fileConverter = fileConverter;
            this.serializer = serializer;
        }

        @Override
        public Resource read(InputNode node) throws Exception {
            String value = node.getValue();
            if (value != null && value.trim().length() > 0) {
                return new Resource(fileConverter.read(value));
            }
            return serializer.read(Resource.class, node);
        }

        @Override
        public void write(OutputNode node, Resource resource) throws Exception {
            File path = resource.getPath();
            if (path != null) {
                fileConverter.write(node, path);
            } else {
                node.remove();
                serializer.write(resource, node.getParent());
            }
        }
    }

    private static final class StripArchivesConfigConverter implements Converter<StripArchivesConfig> {

        @Override
        public StripArchivesConfig read(InputNode node) throws Exception {
            StripArchivesConfig.StripArchivesBuilder cfgBuilder = new StripArchivesConfig.StripArchivesBuilder();
            InputNode childNode;
            while ((childNode = node.getNext()) != null) {
                if (childNode.isElement() && !childNode.isEmpty() && childNode.getName().equals("include") || childNode.getName().equals("exclude")) {
                    boolean isInclude = childNode.getName().equals("include");
                    cfgBuilder.add(isInclude, childNode.getValue());
                }
            }
            return cfgBuilder.build();
        }

        @Override
        public void write(OutputNode node, StripArchivesConfig config) throws Exception {
            if (config.getPatterns() != null && !config.getPatterns().isEmpty()) {
                for (StripArchivesConfig.Pattern pattern : config.getPatterns()) {
                    OutputNode child = node.getChild(pattern.isInclude() ? "include" : "exclude");
                    child.setValue(pattern.getPatternAsString());
                    child.commit();
                }


            }
            node.commit();
        }

    }

    private static final class RelativeLibConverter implements Converter<Lib> {
        private final RelativeFileConverter fileConverter;

        public RelativeLibConverter(RelativeFileConverter fileConverter) {
            this.fileConverter = fileConverter;
        }

        @Override
        public Lib read(InputNode node) throws Exception {
            String value = node.getValue();
            if (value == null) {
                return null;
            }
            InputNode forceNode = node.getAttribute("force");
            boolean force = forceNode == null || Boolean.parseBoolean(forceNode.getValue());
            if (value.endsWith(".a") || value.endsWith(".o")) {
                return new Lib(fileConverter.read(value).getAbsolutePath(), force);
            } else {
                return new Lib(value, force);
            }
        }

        @Override
        public void write(OutputNode node, Lib lib) throws Exception {
            String value = lib.getValue();
            boolean force = lib.isForce();
            if (value.endsWith(".a") || value.endsWith(".o")) {
                fileConverter.write(node, new File(value));
            } else {
                node.setValue(value);
            }
            if (!force) {
                node.setAttribute("force", "false");
            }
        }
    }
}
