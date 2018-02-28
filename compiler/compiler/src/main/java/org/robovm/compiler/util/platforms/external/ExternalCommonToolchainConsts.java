package org.robovm.compiler.util.platforms.external;

/**
 * @author Demyan Kimitsa
 * contains links to external resources as well as version information
 */
class ExternalCommonToolchainConsts {
    // TODO: udpate the link
    final static String TOOLCHAIN_DOWNLOAD_URL = "https://dkimitsa.github.io/2017/12/12/robovm-now-linux-windows/?source=idea";

    // version of toolchain currently supported
    // toolchain is considered supported if major.minor.xxx version specified in toolchain
    // same as defined here. it means that contract (e.g. parameter set) is compatible
    // rev value could be any but higher than specified
    // version is encoded as  (((major * 1000 + minor) * 1000 + rev) * 1000) + build + buildType
    // Version history:
    // * 0.1.x -- initial version of toolchain made for POC
    // * 0.2.x -- version with modified protocol to xib2nib
    @SuppressWarnings("PointlessArithmeticExpression")
    final static long TOOLCHAIN_VERSION = ((0 * 1000 + 2) * 1000 + 1);
}
