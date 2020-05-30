/*
 * Copyright (C) 2012 RoboVM AB
 * Copyright (C) 2018 Daniel Thommes, NeverNull GmbH, <daniel.thommes@nevernull.io>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/gpl-2.0.html>.
 */
package org.robovm.compiler.config;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.robovm.compiler.DependencyGraph;
import org.robovm.compiler.ITable;
import org.robovm.compiler.MarshalerLookup;
import org.robovm.compiler.VTable;
import org.robovm.compiler.clazz.Clazz;
import org.robovm.compiler.clazz.Clazzes;
import org.robovm.compiler.clazz.Path;
import org.robovm.compiler.config.OS.Family;
import org.robovm.compiler.config.StripArchivesConfig.StripArchivesBuilder;
import org.robovm.compiler.config.tools.Tools;
import org.robovm.compiler.llvm.DataLayout;
import org.robovm.compiler.log.Logger;
import org.robovm.compiler.plugin.CompilerPlugin;
import org.robovm.compiler.plugin.LaunchPlugin;
import org.robovm.compiler.plugin.Plugin;
import org.robovm.compiler.plugin.TargetPlugin;
import org.robovm.compiler.plugin.annotation.AnnotationImplPlugin;
import org.robovm.compiler.plugin.debug.DebugInformationPlugin;
import org.robovm.compiler.plugin.debug.DebuggerLaunchPlugin;
import org.robovm.compiler.plugin.lambda.LambdaPlugin;
import org.robovm.compiler.plugin.objc.InterfaceBuilderClassesPlugin;
import org.robovm.compiler.plugin.objc.ObjCBlockPlugin;
import org.robovm.compiler.plugin.objc.ObjCMemberPlugin;
import org.robovm.compiler.plugin.objc.ObjCProtocolProxyPlugin;
import org.robovm.compiler.target.ConsoleTarget;
import org.robovm.compiler.target.Target;
import org.robovm.compiler.target.framework.FrameworkTarget;
import org.robovm.compiler.target.ios.IOSTarget;
import org.robovm.compiler.target.ios.ProvisioningProfile;
import org.robovm.compiler.target.ios.SigningIdentity;
import org.robovm.compiler.util.DigestUtil;
import org.robovm.compiler.util.InfoPList;
import org.robovm.compiler.util.io.RamDiskTools;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Holds compiler configuration.
 */
@Root
public class Config {
    

	/**
     * The max file name length of files stored in the cache. OS X has a limit
     * of 255 characters. Class names are very unlikely to be this long but some
     * JVM language compilers (e.g. the Scala compiler) are known to generate
     * very long class names for auto-generated classes. See #955.
     */
    private static final int MAX_FILE_NAME_LENGTH = 255;

    @Element(required = false)
    File installDir = null;
    @Element(required = false)
    String executableName = null;
    @Element(required = false)
    String imageName = null;
    @Element(required = false)
    Boolean skipRuntimeLib = null;
    @Element(required = false)
    File mainJar;
    @Element(required = false)
    String mainClass;
    @Element(required = false)
    Cacerts cacerts = null;
    @Element(required = false)
    OS os = null;
    @ElementList(required = false, inline = true)
    ArrayList<Arch> archs = null;
    @ElementList(required = false, entry = "root")
    ArrayList<String> roots;
    @ElementList(required = false, entry = "pattern")
    ArrayList<String> forceLinkClasses;
    @ElementList(required = false, entry = "entry")
    private ArrayList<ForceLinkMethodsConfig> forceLinkMethods;
    @ElementList(required = false, entry = "lib")
    ArrayList<Lib> libs;
    @ElementList(required = false, entry = "symbol")
    ArrayList<String> exportedSymbols;
    @ElementList(required = false, entry = "symbol")
    ArrayList<String> unhideSymbols;
    @ElementList(required = false, entry = "framework")
    ArrayList<String> frameworks;
    @ElementList(required = false, entry = "framework")
    ArrayList<String> weakFrameworks;
    @ElementList(required = false, entry = "path")
    ArrayList<QualifiedFile> frameworkPaths;
    @ElementList(required = false, entry = "extension")
    ArrayList<AppExtension> appExtensions;
    @ElementList(required = false, entry = "path")
    ArrayList<QualifiedFile> appExtensionPaths;
    @ElementList(required = false, entry = "resource")
    ArrayList<Resource> resources;
    @ElementList(required = false, entry = "classpathentry")
    ArrayList<File> bootclasspath;
    @ElementList(required = false, entry = "classpathentry")
    ArrayList<File> classpath;
    @ElementList(required = false, entry = "argument")
    ArrayList<String> pluginArguments;
    @Element(required = false, name = "target")
    String targetType;
    @Element(required = false, name = "stripArchives")
    private StripArchivesConfig stripArchivesConfig;
    @Element(required = false, name = "treeShaker")
    TreeShakerMode treeShakerMode;
    @Element(required = false, name = "smartSkipRebuild")
    Boolean smartSkipRebuild;

    @Element(required = false)
    String iosSdkVersion;
    @Element(required = false, name = "iosInfoPList")
    File iosInfoPListFile = null;
    @Element(required = false, name = "infoPList")
    File infoPListFile = null;
    @Element(required = false)
    File iosEntitlementsPList;

    @Element(required = false)
    private WatchKitApp watchKitApp;

    @Element(required = false)
    Tools tools;

    @Element(required = false)
    Boolean enableBitcode;

    SigningIdentity iosSignIdentity;
    ProvisioningProfile iosProvisioningProfile;
    String iosDeviceType;
    private InfoPList infoPList;

    boolean iosSkipSigning = false;

    Properties properties = new Properties();

    Home home = null;
    File tmpDir;
    File cacheDir = new File(System.getProperty("user.home"), ".robovm/cache");
    File ccBinPath = null;

    boolean clean = false;
    boolean debug = false;
    boolean useDebugLibs = false;
    boolean skipLinking = false;
    boolean skipInstall = false;
    boolean dumpIntermediates = false;
    boolean manuallyPreparedForLaunch = false;
    int threads = Runtime.getRuntime().availableProcessors();
    Logger logger = Logger.NULL_LOGGER;

    /*
     * The fields below are all initialized in build() and must not be included
     * when constructing Config clone. We mark them as transient which will make
     * the builder() method skip them.
     */

    transient List<Plugin> plugins = new ArrayList<>();
    private transient Target target = null;
    private transient File osArchDepLibDir;
    private transient File osArchCacheDir;
    private transient Clazzes clazzes;
    private transient VTable.Cache vtableCache;
    private transient ITable.Cache itableCache;
    private transient List<Path> resourcesPaths = new ArrayList<>();
    private transient DataLayout dataLayout;
    private transient MarshalerLookup marshalerLookup;
    private transient Config configBeforeBuild;
    private transient DependencyGraph dependencyGraph;
    private transient Arch sliceArch;
    transient StripArchivesBuilder stripArchivesBuilder;

    protected Config() {
        // Add standard plugins
        this.plugins.addAll(0, Arrays.asList(
                new InterfaceBuilderClassesPlugin(),
                new ObjCProtocolProxyPlugin(),
                new ObjCMemberPlugin(),
                new ObjCBlockPlugin(),
                new AnnotationImplPlugin(),
                new LambdaPlugin(),         
                new DebugInformationPlugin(),
                new DebuggerLaunchPlugin()
                ));
        this.loadPluginsFromClassPath();
    }

    /**
     * Returns a new {@link Builder} which builds exactly this {@link Config}
     * when {@link Builder#build()} is called.
     */
    public Builder builder() throws IOException {
        return new Builder(clone(configBeforeBuild));
    }

    public Home getHome() {
        return home;
    }

    public File getInstallDir() {
        return installDir;
    }

    public String getExecutableName() {
        return executableName;
    }

    public String getImageName() {
        return imageName;
    }

    public File getExecutablePath() {
        return new File(installDir, getExecutableName());
    }

    public File getImagePath() {
        return getExecutablePath();
    }

    public File getCacheDir() {
        return osArchCacheDir;
    }

    public File getCcBinPath() {
        return ccBinPath;
    }

    public OS getOs() {
        return os;
    }

    public Arch getArch() {
        return sliceArch;
    }

    public List<Arch> getArchs() {
        return archs == null ? Collections.emptyList()
                : Collections.unmodifiableList(archs);
    }
    
    public String getTriple() {
        return sliceArch.getLlvmName() + "-unknown-" + os.getLlvmName();
    }

    public String getClangTriple() {
        return sliceArch.getClangName() + "-unknown-" + os.getLlvmName();
    }

    public DataLayout getDataLayout() {
        return dataLayout;
    }

    public boolean isClean() {
        return clean;
    }

    public boolean isDebug() {
        return debug;
    }

    public boolean isUseDebugLibs() {
        return useDebugLibs;
    }

    public boolean isDumpIntermediates() {
        return dumpIntermediates;
    }

    public boolean isManuallyPreparedForLaunch() {
        return manuallyPreparedForLaunch;
    }

    public boolean isSkipRuntimeLib() {
        return skipRuntimeLib != null && skipRuntimeLib;
    }

    public boolean isSkipLinking() {
        return skipLinking;
    }

    public boolean isSkipInstall() {
        return skipInstall;
    }

    public int getThreads() {
        return threads;
    }

    public File getMainJar() {
        return mainJar;
    }

    public String getMainClass() {
        return mainClass;
    }

    public Cacerts getCacerts() {
        return cacerts == null ? Cacerts.full : cacerts;
    }

    public List<Path> getResourcesPaths() {
        return resourcesPaths;
    }

    public void addResourcesPath(Path path) {
        resourcesPaths.add(path);
    }
    
    public StripArchivesConfig getStripArchivesConfig() {
       return stripArchivesConfig == null ? StripArchivesConfig.DEFAULT : stripArchivesConfig;
    }

    public DependencyGraph getDependencyGraph() {
        return dependencyGraph;
    }
    
    public File getTmpDir() {
        if (tmpDir == null) {
            try {
                tmpDir = File.createTempFile("robovm", ".tmp");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            tmpDir.delete();
            tmpDir.mkdirs();
        }
        return tmpDir;
    }

    public List<String> getForceLinkClasses() {
        return forceLinkClasses == null ? Collections.emptyList()
                : Collections.unmodifiableList(forceLinkClasses);
    }

    public List<ForceLinkMethodsConfig> getForceLinkMethods() {
        return forceLinkMethods == null ? Collections.emptyList()
                : Collections.unmodifiableList(forceLinkMethods);
    }

    public List<String> getExportedSymbols() {
        return exportedSymbols == null ? Collections.emptyList()
                : Collections.unmodifiableList(exportedSymbols);
    }

    public List<String> getUnhideSymbols() {
        return unhideSymbols == null ? Collections.emptyList()
                : Collections.unmodifiableList(unhideSymbols);
    }
    
    public List<Lib> getLibs() {
        return libs == null ? Collections.emptyList()
                : Collections.unmodifiableList(libs);
    }

    public List<String> getFrameworks() {
        return frameworks == null ? Collections.emptyList()
                : Collections.unmodifiableList(frameworks);
    }

    public List<String> getWeakFrameworks() {
        return weakFrameworks == null ? Collections.emptyList()
                : Collections.unmodifiableList(weakFrameworks);
    }

    public List<File> getFrameworkPaths() {
        return frameworkPaths == null ? Collections.emptyList()
                : frameworkPaths.stream()
                .filter(this::isQualified)
                .map(f -> f.entry)
                .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
    }

    public List<AppExtension> getAppExtensions() {
        return appExtensions == null ? Collections.emptyList()
                : Collections.unmodifiableList(appExtensions);
    }

    public List<File> getAppExtensionPaths() {
        return appExtensionPaths == null ? Collections.emptyList()
                : appExtensionPaths.stream()
                .filter(this::isQualified)
                .map(e -> e.entry)
                .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
    }

    public List<Resource> getResources() {
        return resources == null ? Collections.emptyList()
                : Collections.unmodifiableList(resources);
    }

    public File getOsArchDepLibDir() {
        return osArchDepLibDir;
    }

    public Clazzes getClazzes() {
        return clazzes;
    }

    public VTable.Cache getVTableCache() {
        return vtableCache;
    }

    public ITable.Cache getITableCache() {
        return itableCache;
    }

    public MarshalerLookup getMarshalerLookup() {
        return marshalerLookup;
    }

    public List<CompilerPlugin> getCompilerPlugins() {
        List<CompilerPlugin> compilerPlugins = new ArrayList<>();
        for (Plugin plugin : plugins) {
            if (plugin instanceof CompilerPlugin) {
                compilerPlugins.add((CompilerPlugin) plugin);
            }
        }
        return compilerPlugins;
    }

    public List<LaunchPlugin> getLaunchPlugins() {
        List<LaunchPlugin> launchPlugins = new ArrayList<>();
        for (Plugin plugin : plugins) {
            if (plugin instanceof LaunchPlugin) {
                launchPlugins.add((LaunchPlugin) plugin);
            }
        }
        return launchPlugins;
    }

    public List<TargetPlugin> getTargetPlugins() {
        List<TargetPlugin> targetPlugins = new ArrayList<>();
        for (Plugin plugin : plugins) {
            if (plugin instanceof TargetPlugin) {
                targetPlugins.add((TargetPlugin) plugin);
            }
        }
        return targetPlugins;
    }

    public List<Plugin> getPlugins() {
        return plugins;
    }

    public List<String> getPluginArguments() {
        return pluginArguments == null ? Collections.emptyList()
                : Collections.unmodifiableList(pluginArguments);
    }

    public List<File> getBootclasspath() {
        return bootclasspath == null ? Collections.emptyList()
                : Collections.unmodifiableList(bootclasspath);
    }

    public List<File> getClasspath() {
        return classpath == null ? Collections.emptyList()
                : Collections.unmodifiableList(classpath);
    }

    public Properties getProperties() {
        return properties;
    }

    public Logger getLogger() {
        return logger;
    }

    public Target getTarget() {
        return target;
    }

    public String getTargetType() {
        return targetType;
    }

    public TreeShakerMode getTreeShakerMode() {
        return treeShakerMode == null ? TreeShakerMode.none : treeShakerMode;
    }

    public boolean isSmartSkipRebuild(){
        return smartSkipRebuild != null && smartSkipRebuild;
    }

    public String getIosSdkVersion() {
        return iosSdkVersion;
    }

    public String getIosDeviceType() {
        return iosDeviceType;
    }

    public InfoPList getIosInfoPList() {
        return getInfoPList();
    }

    public InfoPList getInfoPList() {
        if (infoPList == null && iosInfoPListFile != null) {
            infoPList = new InfoPList(iosInfoPListFile);
        } else if (infoPList == null && infoPListFile != null) {
            infoPList = new InfoPList(infoPListFile);
        }
        return infoPList;
    }

    public File getIosEntitlementsPList() {
        return iosEntitlementsPList;
    }

    public SigningIdentity getIosSignIdentity() {
        return iosSignIdentity;
    }

    public ProvisioningProfile getIosProvisioningProfile() {
        return iosProvisioningProfile;
    }

    public boolean isIosSkipSigning() {
        return iosSkipSigning;
    }

    public boolean isEnableBitcode() {return enableBitcode != null && enableBitcode && shouldEmitBitcode(); }

    public boolean shouldEmitBitcode() {
        // emit bitcode to object file even if it is not enabled.
        // as currently only `__LLVM,__asm` is being added
        // but build should match criteria
        return !debug && os == OS.ios && (sliceArch == Arch.arm64 || sliceArch == Arch.thumbv7);
    }

    public Tools getTools() {
        return tools;
    }

    public WatchKitApp getWatchKitApp() {
        return watchKitApp;
    }

    private static File makeFileRelativeTo(File dir, File f) {
        if (f.getParentFile() == null) {
            return dir;
        }
        return new File(makeFileRelativeTo(dir, f.getParentFile()), f.getName());
    }

    public String getArchiveName(Path path) {
        if (path.getFile().isFile()) {
            return path.getFile().getName();
        } else {
            return "classes" + path.getIndex() + ".jar";
        }
    }

    static String getFileName(Clazz clazz, String ext) {
        return getFileName(clazz.getInternalName(), ext, MAX_FILE_NAME_LENGTH);
    }

    static String getFileName(String internalName, String ext, int maxFileNameLength) {
        String packagePath = internalName.substring(0, internalName.lastIndexOf('/') + 1);
        String className = internalName.substring(internalName.lastIndexOf('/') + 1);
        String suffix = ext.startsWith(".") ? ext : "." + ext;

        int length = className.length() + suffix.length();
        if (length > maxFileNameLength) {
            String sha1 = DigestUtil.sha1(className);
            className = className.substring(0, Math.max(0, maxFileNameLength - suffix.length() - sha1.length())) + sha1;
        }
        return packagePath.replace('/', File.separatorChar) + className + suffix;
    }

    public File getLlFile(Clazz clazz) {
        return new File(getCacheDir(clazz.getPath()), getFileName(clazz, "class.ll"));
    }

    public File getCFile(Clazz clazz) {
        return new File(getCacheDir(clazz.getPath()), getFileName(clazz, "class.c"));
    }

    public File getBcFile(Clazz clazz) {
        return new File(getCacheDir(clazz.getPath()), getFileName(clazz, "class.bc"));
    }

    public File getSFile(Clazz clazz) {
        return new File(getCacheDir(clazz.getPath()), getFileName(clazz, "class.s"));
    }

    public File getOFile(Clazz clazz) {
        return new File(getCacheDir(clazz.getPath()), getFileName(clazz, "class.o"));
    }

    public File getLinesOFile(Clazz clazz) {
        return new File(getCacheDir(clazz.getPath()), getFileName(clazz, "class.lines.o"));
    }

    public File getLinesLlFile(Clazz clazz) {
        return new File(getCacheDir(clazz.getPath()), getFileName(clazz, "class.lines.ll"));
    }

    public File getDebugInfoOFile(Clazz clazz) {
        return new File(getCacheDir(clazz.getPath()), getFileName(clazz, "class.debuginfo.o"));
    }

    public File getDebugInfoLlFile(Clazz clazz) {
        return new File(getCacheDir(clazz.getPath()), getFileName(clazz, "class.debuginfo.ll"));
    }

    public File getInfoFile(Clazz clazz) {
        return new File(getCacheDir(clazz.getPath()), getFileName(clazz, "class.info"));
    }

    public File getCacheDir(Path path) {
        File srcRoot = path.getFile().getAbsoluteFile().getParentFile();
        String name = path.getFile().getName();
        try {
            return new File(makeFileRelativeTo(osArchCacheDir, srcRoot.getCanonicalFile()), name);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the directory where generated classes are stored for the
     * specified {@link Path}. Generated classes are stored in the cache
     * directory in a dir at the same level as the cache dir for the
     * {@link Path} with <code>.generated</code> appended to the dir name.
     */
    public File getGeneratedClassDir(Path path) {
        File pathCacheDir = getCacheDir(path);
        return new File(pathCacheDir.getParentFile(), pathCacheDir.getName() + ".generated");
    }

    /**
     * tests if qualified item matches this config
     */
    protected boolean isQualified(Qualified qualified) {
        if (qualified.filterPlatforms() != null) {
            if (!Arrays.asList(qualified.filterPlatforms()).contains(os))
                return false;
        }
        if (qualified.filterArch() != null) {
            if (!Arrays.asList(qualified.filterArch()).contains(sliceArch))
                return false;
        }
        // TODO: there is no platform variant, just guess it from arch, applies to iOS temporaly
        if (os == OS.ios && qualified.filterPlatformVariants() != null) {
            PlatformVariant variant = sliceArch.isArm() ? PlatformVariant.device : PlatformVariant.simulator;
            if (!Arrays.asList(qualified.filterPlatformVariants()).contains(variant))
                return false;
        }

        return true;
    }

    private static Map<Object, Object> getManifestAttributes(File jarFile) throws IOException {
        try (JarFile jf = new JarFile(jarFile)) {
            return new HashMap<>(jf.getManifest().getMainAttributes());
        }
    }

    static String getImplementationVersion(File jarFile) throws IOException {
        return (String) getManifestAttributes(jarFile).get(Attributes.Name.IMPLEMENTATION_VERSION);
    }

    private static String getMainClass(File jarFile) throws IOException {
        return (String) getManifestAttributes(jarFile).get(Attributes.Name.MAIN_CLASS);
    }

    private File extractIfNeeded(Path path) throws IOException {
        if (path.getFile().isFile()) {
            File pathCacheDir = getCacheDir(path);
            File target = new File(pathCacheDir.getParentFile(), pathCacheDir.getName() + ".extracted");

            if (!target.exists() || path.getFile().lastModified() > target.lastModified()) {
                FileUtils.deleteDirectory(target);
                target.mkdirs();
                try (ZipFile zipFile = new ZipFile(path.getFile())) {
                    Enumeration<? extends ZipEntry> entries = zipFile.entries();
                    while (entries.hasMoreElements()) {
                        ZipEntry entry = entries.nextElement();
                        if (entry.getName().startsWith("META-INF/robovm/") && !entry.isDirectory()) {
                            File f = new File(target, entry.getName());
                            f.getParentFile().mkdirs();
                            try (InputStream in = zipFile.getInputStream(entry);
                                 OutputStream out = new FileOutputStream(f)) {

                                IOUtils.copy(in, out);
                                if (entry.getTime() != -1) {
                                    f.setLastModified(entry.getTime());
                                }
                            }
                        }
                    }
                }
                target.setLastModified(path.getFile().lastModified());
            }

            return target;
        } else {
            return path.getFile();
        }
    }

    private <T> ArrayList<T> mergeLists(ArrayList<T> from, ArrayList<T> to) {
        if (from == null) {
            return to;
        }
        to = to != null ? to : new ArrayList<>();
        for (T o : from) {
            if (!to.contains(o)) {
                to.add(o);
            }
        }
        return to;
    }

    private void mergeConfig(Config from, Config to) {
        to.exportedSymbols = mergeLists(from.exportedSymbols, to.exportedSymbols);
        to.unhideSymbols = mergeLists(from.unhideSymbols, to.unhideSymbols);
        to.forceLinkClasses = mergeLists(from.forceLinkClasses, to.forceLinkClasses);
        to.forceLinkMethods = mergeLists(from.forceLinkMethods, to.forceLinkMethods);
        to.frameworkPaths = mergeLists(from.frameworkPaths, to.frameworkPaths);
        to.frameworks = mergeLists(from.frameworks, to.frameworks);
        to.libs = mergeLists(from.libs, to.libs);
        to.resources = mergeLists(from.resources, to.resources);
        to.weakFrameworks = mergeLists(from.weakFrameworks, to.weakFrameworks);
    }

    private void mergeConfigsFromClasspath() throws IOException {
        List<String> dirs = Arrays.asList(
                "META-INF/robovm/" + os + "/" + sliceArch,
                "META-INF/robovm/" + os);

        // The algorithm below preserves the order of config data from the
        // classpath. Last the config from this object is added.

        // First merge all configs on the classpath to an empty Config
        Config config = new Config();
        for (Path path : clazzes.getPaths()) {
            for (String dir : dirs) {
                if (path.contains(dir + "/robovm.xml")) {
                    File configXml = new File(new File(extractIfNeeded(path), dir), "robovm.xml");
                    Builder builder = new Builder();
                    builder.read(configXml);
                    mergeConfig(builder.config, config);
                    break;
                }
            }
        }

        // Then merge with this Config
        mergeConfig(this, config);

        // Copy back to this Config
        this.exportedSymbols = config.exportedSymbols;
        this.unhideSymbols = config.unhideSymbols;
        this.forceLinkClasses = config.forceLinkClasses;
        this.forceLinkMethods = config.forceLinkMethods;
        this.frameworkPaths = config.frameworkPaths;
        this.frameworks = config.frameworks;
        this.libs = config.libs;
        this.resources = config.resources;
        this.weakFrameworks = config.weakFrameworks;
    }

    private static <T> List<T> toList(Iterator<T> it) {
        List<T> l = new ArrayList<>();
        while (it.hasNext()) {
            l.add(it.next());
        }
        return l;
    }

    private void loadPluginsFromClassPath() {
        ClassLoader classLoader = getClass().getClassLoader();
        ServiceLoader<CompilerPlugin> compilerPluginLoader = ServiceLoader.load(CompilerPlugin.class, classLoader);
        ServiceLoader<LaunchPlugin> launchPluginLoader = ServiceLoader.load(LaunchPlugin.class, classLoader);
        ServiceLoader<TargetPlugin> targetPluginLoader = ServiceLoader.load(TargetPlugin.class, classLoader);

        plugins.addAll(toList(compilerPluginLoader.iterator()));
        plugins.addAll(toList(launchPluginLoader.iterator()));
        plugins.addAll(toList(targetPluginLoader.iterator()));
    }

    private static Config clone(Config config) throws IOException {
        Config clone = new Config();
        for (Field f : Config.class.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers()) && !Modifier.isTransient(f.getModifiers())) {
                f.setAccessible(true);
                try {
                    Object o = f.get(config);
                    if (o instanceof Collection && o instanceof Cloneable) {
                        // Clone collections. Assume the class has a public
                        // clone() method.
                        Method m = o.getClass().getMethod("clone");
                        o = m.invoke(o);
                    }
                    f.set(clone, o);
                } catch (Throwable t) {
                    throw new Error(t);
                }
            }
        }
        return clone;
    }

    Config build() throws IOException {
        // Create a clone of this Config before we have done anything with it so
        // that builder() has a fresh Config it can use.
        this.configBeforeBuild = clone(this);

        if (home == null) {
            home = Home.find();
        }

        if (bootclasspath == null) {
            bootclasspath = new ArrayList<>();
        }
        if (classpath == null) {
            classpath = new ArrayList<>();
        }

        if (mainJar != null) {
            mainClass = getMainClass(mainJar);
            classpath.add(mainJar);
        }

        if (executableName == null && imageName != null) {
            executableName = imageName;
        }

        if (!skipLinking && executableName == null && mainClass == null) {
            throw new IllegalArgumentException("No target and no main class specified");
        }

        if (!skipLinking && classpath.isEmpty()) {
            throw new IllegalArgumentException("No classpath specified");
        }

        if (skipLinking) {
            skipInstall = true;
        }

        if (executableName == null) {
            executableName = mainClass;
        }

        if (imageName == null || !imageName.equals(executableName)) {
            imageName = executableName;
        }

        List<File> realBootclasspath = bootclasspath == null ? new ArrayList<>() : bootclasspath;
        if (!isSkipRuntimeLib()) {
            realBootclasspath = new ArrayList<>(bootclasspath);
            realBootclasspath.add(0, home.getRtPath());
        }

        this.vtableCache = new VTable.Cache();
        this.itableCache = new ITable.Cache();
        this.marshalerLookup = new MarshalerLookup(this);

        if (!skipInstall) {
            if (installDir == null) {
                installDir = new File(".", executableName);
            }
            installDir.mkdirs();
        }

        if (targetType != null) {
            if (ConsoleTarget.TYPE.equals(targetType)) {
                target = new ConsoleTarget();
            } else if (IOSTarget.TYPE.equals(targetType)) {
                target = new IOSTarget();
            } else if (FrameworkTarget.TYPE.equals(targetType)) {
                target = new FrameworkTarget();
            } else {
                for (TargetPlugin plugin : getTargetPlugins()) {
                    if (plugin.getTarget().getType().equals(targetType)) {
                        target = plugin.getTarget();
                        break;
                    }
                }
                if (target == null) {
                    throw new IllegalArgumentException("Unsupported target '" + targetType + "'");
                }
            }
        } else {
            // Auto
            if (os == OS.ios) {
                target = new IOSTarget();
            } else {
                target = new ConsoleTarget();
            }
        }

        if (!getArchs().isEmpty()) {
            sliceArch = getArchs().get(0);
        }

        target.init(this);

        os = target.getOs();
        sliceArch = target.getArch();
        dataLayout = new DataLayout(getTriple());

        osArchDepLibDir = new File(new File(home.getLibVmDir(), os.toString()),
                sliceArch.toString());

        if (treeShakerMode != null && treeShakerMode != TreeShakerMode.none 
                && os.getFamily() == Family.darwin && sliceArch == Arch.x86) {

            logger.warn("Tree shaking is not supported when building "
                    + "for OS X/iOS x86 32-bit due to a bug in Xcode's linker. No tree "
                    + "shaking will be performed. Run in 64-bit mode instead to "
                    + "use tree shaking.");
            treeShakerMode = TreeShakerMode.none;
        }
        dependencyGraph = new DependencyGraph(getTreeShakerMode());

        RamDiskTools ramDiskTools = new RamDiskTools();
        ramDiskTools.setupRamDisk(this, this.cacheDir, this.tmpDir);
        this.cacheDir = ramDiskTools.getCacheDir();
        this.tmpDir = ramDiskTools.getTmpDir();

        File osDir = new File(cacheDir, os.toString());
        File archDir = new File(osDir, sliceArch.toString());
        osArchCacheDir = new File(archDir, debug ? "debug" : "release");
        osArchCacheDir.mkdirs();

        this.clazzes = new Clazzes(this, realBootclasspath, classpath);
        
        if(this.stripArchivesConfig == null) {
            if(stripArchivesBuilder == null) {
                this.stripArchivesConfig = StripArchivesConfig.DEFAULT;
            }
            else {
                this.stripArchivesConfig = stripArchivesBuilder.build();
            }
        }

        mergeConfigsFromClasspath();

        return this;
    }

    /**
     * reads configuration from disk without any analysis
     */
    public static Config loadRawConfig(File contentRoot) throws IOException {
        // dkimitsa: config retrieved this way shall not be used for any compilation needs
        // just for IB and other UI related things
        Builder builder = new Builder();
        builder.readProjectProperties(contentRoot, false);
        builder.readProjectConfig(contentRoot, false);
        return builder.config;
    }
}
