package org.robovm.utils.codesign.bundle;

/**
 * Represents a bundle. In the words of the Apple docs, it"s a convenient way to deliver
 * software. Really it"s a particular kind of directory structure, with one main executable,
 * well-known places for various data files and libraries,
 * and tracking hashes of all those files for signing purposes.
 * <p>
 * For isign, we have two main kinds of bundles: the App, and the Framework (a reusable
 * library packaged along with its data files.) An App may contain many Frameworks, but
 * a Framework has to be re-signed independently.
 * <p>
 * See the Apple Developer Documentation "About Bundles"
 */


import com.dd.plist.NSDictionary;
import com.dd.plist.PropertyListParser;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.bouncycastle.cms.CMSSignedData;
import org.robovm.utils.codesign.coderesources.CodeResources;
import org.robovm.utils.codesign.context.Ctx;
import org.robovm.utils.codesign.context.SignCtx;
import org.robovm.utils.codesign.context.VerifyCtx;
import org.robovm.utils.codesign.exceptions.CodeSignException;
import org.robovm.utils.codesign.macho.MachOException;
import org.robovm.utils.codesign.macho.SimpleMachOLoader;
import org.robovm.utils.codesign.macho.EmbeddedSignature;
import org.robovm.utils.codesign.utils.FileByteBuffer;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * A bundle is a standard directory structure, a signable, installable set of files.
 * Apps are Bundles, but so are some kinds of Frameworks (libraries)
 */
public abstract class Bundle {

    protected File path;
    protected File infoPlistPath;
    protected NSDictionary infoPlist;


    protected Bundle(File path) {
        this.path = path;
        infoPlistPath = new File(this.path, "Info.plist");
        if (!infoPlistPath.exists())
            throw new CodeSignException("no Info.plist found; probably not a bundle");

        try {
            this.infoPlist = (NSDictionary) PropertyListParser.parse(infoPlistPath);
        } catch (Throwable e) {
            throw new CodeSignException("failed to parse Info.plist", e);
        }
    }

    public void verify(VerifyCtx ctx) {
        // read executable if present
        ctx.debug("Verifying executable");
        VerifyCtx executableCtx = ctx.push();
        File executablePath = getExecutablePath(executableCtx);
        if (!executablePath.exists())
            throw new CodeSignException("Executable is missing in bundle!");

        // prepare path to code resources
        File codeResourcePath = CodeResources.codeResourcePath(path);
        if (!codeResourcePath.exists())
            throw new CodeSignException("Bundle is not signed, _CodeSignature/CodeResources is missing!");

        // get mach-o binary
        try (SimpleMachOLoader loader = new SimpleMachOLoader(executablePath)) {
            // lets work with slices
            List<SimpleMachOLoader.ArchSlice> slices = loader.getSliceList();

            // verify slice
            VerifyCtx sliceCtx = executableCtx.push();
            for (SimpleMachOLoader.ArchSlice slice : slices) {
                executableCtx.debug("Verifying arch slice " + SimpleMachOLoader.archToString(slice.cpuType()));
                verifyExecutableSlice(sliceCtx, slice, codeResourcePath, infoPlistPath);
            }
        } catch (MachOException e ) {
            throw new CodeSignException("Unable to load mach-o of executable", e);
        }

        // read code resources seal to verify against it
        ctx.debug("Verifying _CodeSignature/CodeResources seal");
        CodeResources seal;
        try {
            seal = new CodeResources(path);
        } catch (Throwable e) {
            throw new CodeSignException("failed to parse _CodeSignature/CodeResources", e);
        }

        // create a new seal. it is slow as it is possible that verification fails on first file but who cares
        CodeResources.Builder b = CodeResources.BuilderForVerification(path, executablePath, seal);
        CodeResources verifySeal = b.build();

        // verifying v1
        seal.verify(verifySeal);
    }

    private void verifyExecutableSlice(VerifyCtx sliceCtx, SimpleMachOLoader.ArchSlice slice, File codeResourcePath, File infoPlistPath) {
        FileByteBuffer signData = slice.codeSignBytes();
        if (signData == null)
            throw new CodeSignException("Code signature is missing");

        FileByteBuffer archBytes = slice.archBytes();
        sliceCtx.debug("Parsing EMBEDDED_SIGNATURE(LC_CODE_SIGNATURE)");
        EmbeddedSignature embeddedSignature = EmbeddedSignature.parse(sliceCtx.push(), signData);

        sliceCtx.debug("Verifying EMBEDDED_SIGNATURE");
        embeddedSignature.verifySignature(sliceCtx.push(), slice.archBytes(), codeResourcePath, infoPlistPath);
    }


    /**
     * performs signing of bundle
     *
     * @param ctx for signing
     */
    public void sign(SignCtx ctx) {
        SignCtx appCtx = ctx.forApp(getIdentifier());
        // small sanity
        File executablePath = getExecutablePath(appCtx);
        if (!executablePath.exists())
            throw new CodeSignException("Executable is missing in bundle!");

        // start signing all children that are there
        // TODO: frameworks
        // TODO: dylibs

        // remove old signatures if any
        // link (legacy)
        File codeResourcePath = CodeResources.codeResourceLinkPath(path);
        if (codeResourcePath.exists())
            if (!codeResourcePath.delete())
                throw new CodeSignException("Filed to remove old CodeResources signature");
        // and old signature
        File codeResourceDir = CodeResources.codeResourceDir(path);
        if (codeResourceDir.exists()) {
            try {
                FileUtils.deleteDirectory(codeResourceDir);
            } catch (IOException e) {
                throw new CodeSignException("Filed to remove old CodeResources signature due " + e.getMessage(), e);
            }
        }

        // get entitlements
        byte[] entitlements = ctx.getEntitlements();
        if (entitlements == null) {
            // TODO: read from plist
        }

        // check if there is LC_CODE_SIGNATURE present in binary
        // creating new code-directory data
        // contains size available for each signature4
        File reallocatedExecutablePath = null;
        try (SimpleMachOLoader loader = new SimpleMachOLoader(executablePath)) {
            List<SimpleMachOLoader.ArchSlice> slices = loader.getSliceList();;
            int[] codeSignatureSizes = new int[slices.size()];
            // check for LC_CODE_SIGNATURE section in each slice

            for (int idx = 0; idx < slices.size(); idx++) {
                codeSignatureSizes[idx] = slices.get(idx).codeSignSize();
                if (codeSignatureSizes[idx] <= 0) {
                    // there is no such section, make idx 0 negative as flag
                    codeSignatureSizes[0] = -1;
                    break;
                }
            }

            // check if there are LC_CODE_SIGNATURE present (e.g. was signed before)
            if (codeSignatureSizes[0] < 0) {
                // no code signature, need to allocate with codesign_allocate
                // 0. check if codesign_allocate is available
                if (appCtx.getCodesignAllocatePath() == null)
                    throw new CodeSignException("Binary was not signed before and codesign_allocate path is not specified, use CODESIGN_ALLOCATE env variable to specify one");

                // 1. evaluate size required
                for (int idx = 0; idx < slices.size(); idx++) {
                    SimpleMachOLoader.ArchSlice slice = slices.get(idx);
                    codeSignatureSizes[idx] = EmbeddedSignature.estimateSignatureSize(appCtx, slice.archBytes(), slice.sliceSize(), codeResourcePath, infoPlistPath, entitlements);
                }

                // 2. run codesign_allocate to resize things
                try {
                    reallocatedExecutablePath = File.createTempFile(executablePath.getName(), "codesign_allocate");
                    CommandLine cmdLine = new CommandLine(appCtx.getCodesignAllocatePath());
                    cmdLine.addArgument("-i");
                    cmdLine.addArgument(executablePath.getAbsolutePath(), true);
                    cmdLine.addArgument("-o");
                    cmdLine.addArgument(reallocatedExecutablePath.getAbsolutePath(), true);

                    // add arches
                    for (int idx = 0; idx < slices.size(); idx++) {
                        SimpleMachOLoader.ArchSlice slice = slices.get(idx);
                        cmdLine.addArgument("-a");
                        cmdLine.addArgument(SimpleMachOLoader.archToString(slice.cpuType()));
                        cmdLine.addArgument(Integer.toString(codeSignatureSizes[idx]));
                    }

                    ByteArrayOutputStream stdout = new ByteArrayOutputStream();
                    DefaultExecutor executor = new DefaultExecutor();
                    executor.setStreamHandler(new PumpStreamHandler(stdout));
                    try {
                        executor.execute(cmdLine);
                    } catch (ExecuteException e) {
                        throw new CodeSignException("codesign_allocate failed due " + e.getMessage() + " with out " + stdout.toString());
                    }
                } catch (IOException e) {
                    throw new CodeSignException("codesign_allocate failed due " + e.getMessage(), e);
                }
            }
        } catch (MachOException e) {
            throw new CodeSignException("Unable to load mach-o of executable", e);
        }

        // override file with re-allocated one
        // do this here when mach loader is closed
        if (reallocatedExecutablePath != null) {
            try {
                Files.move(reallocatedExecutablePath.toPath(), executablePath.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new CodeSignException("codesign_allocate failed when replacing app file due " + e.getMessage(), e);
            }
        }

        // create CodeResources signature
        CodeResources.Builder b = CodeResources.BuilderForSigning(path, executablePath);
        CodeResources seal = b.build();

        // and write it back
        if (!codeResourceDir.exists() && !codeResourceDir.mkdirs())
            throw new CodeSignException("Filed to create dirs to CodeResources signature = " + codeResourceDir);
        codeResourcePath = CodeResources.codeResourcePath(path);
        seal.writeTo(codeResourcePath);

        // signing

        // creating new code-directory data
        try (SimpleMachOLoader loader = new SimpleMachOLoader(executablePath, true)) {
            // lets work with slices
            List<SimpleMachOLoader.ArchSlice> slices = loader.getSliceList();

            // sign slice
            SignCtx sliceCtx = appCtx.push();
            for (SimpleMachOLoader.ArchSlice slice : slices) {
                appCtx.debug("Signing arch slice " + SimpleMachOLoader.archToString(slice.cpuType()));
                if (slice.codeSignSize() <= 0)
                    throw new CodeSignException("Arch " + SimpleMachOLoader.archToString(slice.cpuType()) +
                            " has no LC_CODE_SIGNATURE section (INTERNAL ERROR)");
                EmbeddedSignature signature = EmbeddedSignature.sign(sliceCtx, slice.archBytes(), slice.codeSignOffset(),
                        codeResourcePath, infoPlistPath, entitlements);
                // checking if size of signature fits section
                byte[] bytes = signature.rawBytes();
                if (bytes.length > slice.codeSignSize())
                    throw new CodeSignException("Unable to fit signature into existing LC_CODE_SIGNATURE slot (INTERNAL ERROR)");
                // write this sign data back
                loader.writeSectionToFile(slice.sliceFileOffset() + slice.codeSignOffset(), bytes, slice.codeSignSize());
            }
        } catch (MachOException e) {
            throw new CodeSignException("Unable to load mach-o of executable", e);
        } catch (IOException e) {
            throw new CodeSignException("Error while writing LC_CODE_SIGNATURE to executable due " + e.getMessage(), e);
        }
    }


    /**
     * Path to the main executable. For an app, this is app itthis. For
     * a Framework, this is the main framework
     */
    private File getExecutablePath(Ctx ctx) {
        String name;
        if (this.infoPlist.containsKey("CFBundleExecutable")) {
            name = (String) this.infoPlist.get(("CFBundleExecutable")).toJavaObject();
            ctx.debug("Executable name specified in info.plist[CFBundleExecutable] = " + name);
        } else {
            name = FilenameUtils.removeExtension(this.path.getName());
            ctx.debug("Executable name is not specified in info.plist[CFBundleExecutable], making a guess = " + name);
        }

        return new File(this.path, name);
    }


    private String getIdentifier() {
        return (String) this.infoPlist.get(("CFBundleIdentifier")).toJavaObject();
    }

    /**
     * Subclass responsible for signing mobile application itself
     */
    private static class AppBundle extends Bundle {
        private NSDictionary provisioningProfile;

        AppBundle(File path) {
            super(path);

            // read provisioning profile
            File provProfilePath = new File(this.path, "embedded.mobileprovision");
            if (!provProfilePath.exists())
                throw new CodeSignException("no embedded.mobileprovision found; probably not a bundle");

            try (InputStream in = new BufferedInputStream(new FileInputStream(provProfilePath))) {
                CMSSignedData data = new CMSSignedData(in);
                byte[] content = (byte[]) data.getSignedContent().getContent();
                provisioningProfile = (NSDictionary) PropertyListParser.parse(content);
            } catch (Exception e) {
                throw new CodeSignException("Error while reading provisioning profile due " + e);
            }
        }
    }

    public static Bundle bundleForPath(File path) {
        return new AppBundle(path);
    }
}

