package org.robovm.utils.codesign.coderesources;

import com.dd.plist.NSDictionary;
import com.dd.plist.NSObject;
import com.dd.plist.PropertyListParser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.robovm.utils.codesign.CodeSign;
import org.robovm.utils.codesign.exceptions.CodeSignException;
import org.robovm.utils.codesign.utils.DiggestAlgorithm;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Pattern;

public class CodeResources {
    private final static String BUNDLEDISKREP_DIRECTORY = "_CodeSignature";
    private final static String CODERESOURCES_LINK = "CodeResources";
    private final static String RESOURCE_DIR_FILE = "CodeResources";
    private final static String TEMPLATE_FILENAME = "code_resources_template.xml";

    private RuleVo.List[] rules = new RuleVo.List[2];
    private FileVo[][] files = new FileVo[2][];


    /**
     * opens existing signature
     * @param bundlePath root folder of Bundle
     */
    public CodeResources(File bundlePath) {
        // open CodeResources
        NSDictionary cr;
        try {
            cr = (NSDictionary) PropertyListParser.parse(codeResourcePath(bundlePath));
        } catch (Throwable e) {
            throw new CodeSignException("Failed to read _CodeSignature/CodeResources at location " + bundlePath, e);
        }

        // parse rules
        rules[0] = new RuleVo.List((NSDictionary) cr.get("rules"));
        rules[1] = new RuleVo.List((NSDictionary) cr.get("rules2"));

        // save files1 and files2
        files[0] = parseFiles((NSDictionary) cr.get("files"));
        files[1] = parseFiles((NSDictionary)cr.get("files2"));
    }


    CodeResources(RuleVo.List rules, RuleVo.List rules2, FileVo[] files, FileVo[] files2) {
        // parse rules
        this.rules[0] = rules;
        this.rules[1] = rules2;

        // save files1 and files2
        this.files[0] = files;
        this.files[1] = files2;
    }

    private FileVo[] parseFiles(NSDictionary plistFiles) {
        FileVo[] result = new FileVo[plistFiles.size()];
        int idx = 0;
        for (Map.Entry<String, NSObject> e : plistFiles.entrySet()) {
            result[idx++] = new FileVo(e.getKey(), e.getValue());
        }

        return result;
    }

    /**
     * Obtain the 'template' plist which also contains things like
     * default rules about which files should count
     */
    static NSDictionary get_template() {
        InputStream in = CodeSign.class.getResourceAsStream(TEMPLATE_FILENAME);
        if (in == null) {
            throw new UnsatisfiedLinkError("CodeResources.get_template " + TEMPLATE_FILENAME + " not found");
        }
        try {
            return (NSDictionary) PropertyListParser.parse(in);
        } catch (Throwable e) {
            throw new UnsatisfiedLinkError("broken CodeResources.get_template  " + TEMPLATE_FILENAME + " not found");
        }
    }

    /**
     * Verifies if this object corresponds one that was build for actual data being read
     * @param actualSeal actual seal data
     */
    public void verify(CodeResources actualSeal) {
        // verify v1 seal
        verifyVersion(this.files[0], actualSeal.files[0], false);
        // verify v2 seal
        verifyVersion(this.files[1], actualSeal.files[1], true);
    }

    private void verifyVersion(FileVo[] files, FileVo[] actualFiles, boolean v2) {
        // create maps
        Map<String, FileVo> filesMap = new HashMap<>(files.length);
        for (FileVo f : files)
            filesMap.put(f.name, f);
//        Map<String, FileVo> actualMap = new HashMap<>(actualFiles.length);
//        for (FileVo f : actualFiles)
//            actualMap.put(f.name, f);

        Set<String> matchedFiles = new HashSet<>(actualFiles.length);
        for (FileVo actualFile : actualFiles) {
            FileVo referenceFile = filesMap.get(actualFile.name);
            if (referenceFile == null) {
                // this file was not present in codesign
                throw new CodeSignException("CodeResources: file not present in seal " + actualFile.name);
            }

            // compare them
            if (!actualFile.compare(referenceFile, v2)) {
                // missmatch between files
                throw new CodeSignException("CodeResources: file signature error, computed: " + actualFile + " expected: " + referenceFile);
            }


            matchedFiles.add(actualFile.name);
        }

        // now check if there are files mentioned in reference but not actually present in this seal
        Set<String> missingFiles = new HashSet<>(files.length);
        missingFiles.addAll(filesMap.keySet());
        missingFiles.removeAll(matchedFiles);
        if (!missingFiles.isEmpty()) {
            throw new CodeSignException("CodeResources: files are mentioned in reference but missing in actualSeal: " +
                    StringUtils.join(missingFiles, ","));
        }
    }

    /**
     * returns CodeResource file path from bundle path
     */
    public static File codeResourcePath(File bundlePath) {
        return new File(bundlePath, BUNDLEDISKREP_DIRECTORY + "/" + RESOURCE_DIR_FILE);
    }

    /**
     * returns directory seal directory path
     */
    public static File codeResourceDir(File bundlePath) {
        return new File(bundlePath, BUNDLEDISKREP_DIRECTORY);
    }

    /**
     * returns path to CodeResource link file (legacy)
     */
    public static File codeResourceLinkPath(File bundlePath) {
        return new File(bundlePath, CODERESOURCES_LINK);
    }

    /**
     * creates builder to for CodeResource to verify against existing seal
     * @param bundlePath where resources are located
     * @param executablePath if present
     * @param seal existing seal to get rules from
     * @return builder to build CodeResources
     */
    public static Builder BuilderForVerification(File bundlePath, File executablePath, CodeResources seal) {
        return new Builder(bundlePath, executablePath, seal.rules[0], seal.rules[1]);
    }

    /**
     * creates builder to sign code resources in bundle, rules will be picked up from template
     * @param bundlePath where resources are located
     * @param executablePath if present
     * @return builder to build CodeResources
     */
    public static Builder BuilderForSigning(File bundlePath, File executablePath) {
        return new Builder(bundlePath, executablePath);
    }

    /**
     * Writes content of seal to destination file location
     * @param codeResourcePath to write seal to
     */
    public void writeTo(File codeResourcePath) {
        // construct final dictionary
        NSDictionary cr = new NSDictionary();
        cr.put("files", FileVo.toPlist(files[0], false));
        cr.put("files2", FileVo.toPlist(files[1], true));
        cr.put("rules", rules[0].plistRules());
        cr.put("rules2", rules[1].plistRules());
        try {
            PropertyListParser.saveAsXML(cr, codeResourcePath);
        } catch (IOException e) {
            throw new CodeSignException("Failed to write CodeResources @" + codeResourcePath + " due " + e.getMessage(), e);
        }
    }


    private static class SliceBuilder {
        private final boolean version2;
        private final RuleVo[] rules;
        private final File bundleDir;

        SliceBuilder(boolean version2, RuleVo.List rulesList, File bundleDir, File app_path) {
            RuleVo[] listItems = rulesList.getItems();

            // adjust rules with exclusion
            int extraRules = app_path != null ? 3 : 2;
            listItems = Arrays.copyOf(listItems, listItems.length + extraRules);

            // exclude entire contents of meta directory
            listItems[listItems.length - 1] = new RuleVo("^" + BUNDLEDISKREP_DIRECTORY + "/", RuleVo.Flags.EXCLUSION);
            listItems[listItems.length - 2] = new RuleVo("^" + CODERESOURCES_LINK + "$", RuleVo.Flags.EXCLUSION);
            if (app_path != null) {
                // add app_path exclusion
                String appRelativePath = bundleDir.toPath().relativize(app_path.toPath()).toString();
                listItems[listItems.length - 3] = new RuleVo("^" + Pattern.quote(appRelativePath) + "$", RuleVo.Flags.EXCLUSION);
            }

            this.version2 = version2;
            this.rules = listItems;
            this.bundleDir = bundleDir;
        }

        /**
         * Walk entire directory, compile mapping
         * path relative to source_dir -> digest and other data
         */
        private void scanDir(List<FileVo> fileEntries, File path) {
            List<File> dirList = new ArrayList<>();
            // rule_debug_fmt = "rule: {0}, path: {1}, relative_path: {2}"
            Iterator<File> it = FileUtils.iterateFiles(path, null, true);
            while (it.hasNext()) {
                File filePath = it.next();
                String relativePath = this.bundleDir.toPath().relativize(filePath.toPath()).toString();
                // convert to unix way in case of windows
                relativePath = relativePath.replace('\\', '/');
                RuleVo rule = RuleVo.findRule(rules, relativePath);

                if (rule.is_exclusion())
                    continue;

                // in version 2 it shall obey omitted
                if (rule.is_omitted() && this.version2)
                    continue;

                if (filePath.isDirectory()) {
                    // directory to be processed
                    if (rule.is_nested())
                        continue;
                    dirList.add(filePath);
                    continue;
                }

                // create file VO entry
                byte[] hash1 = DiggestAlgorithm.HashSHA1.hash(filePath);
                byte[] hash2 = version2 ? DiggestAlgorithm.HashSHA256.hash(filePath) : null;
                fileEntries.add(new FileVo(relativePath, hash1, hash2, rule.isOptional()));
            }

            // process sub-directories
            for (File d : dirList) {
                scanDir(fileEntries, d);
            }
        }

        public FileVo[] build() {
            List<FileVo> result = new ArrayList<>();
            scanDir(result, this.bundleDir);
            return result.toArray(new FileVo[result.size()]);
        }
    }

    public static class Builder {
        private final File bundleDir;
        private final File appPath;
        private final RuleVo.List rules;
        private final RuleVo.List rules2;

        private Builder(File bundleDir, File appPath) {
            this.bundleDir = bundleDir;
            this.appPath = appPath;
            // extract rules from template
            NSDictionary template = CodeResources.get_template();
            rules = new RuleVo.List((NSDictionary) template.get("rules"));
            rules2 = new RuleVo.List((NSDictionary) template.get("rules2"));
        }

        private Builder(File bundleDir, File appPath, RuleVo.List rules, RuleVo.List rules2) {
            this.bundleDir = bundleDir;
            this.appPath = appPath;
            this.rules = rules;
            this.rules2 = rules2;
        }

        public CodeResources build() {
            // build v1 slice
            FileVo[] files = (new SliceBuilder(false, rules, bundleDir, appPath)).build();
            // build v2 slice
            FileVo[] files2 = (new SliceBuilder(true, rules2, bundleDir, appPath)).build();

            return new CodeResources(rules, rules2, files, files2);
        }
    }
}