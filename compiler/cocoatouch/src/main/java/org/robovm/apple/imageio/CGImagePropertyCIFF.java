/*
 * Copyright (C) 2013-2015 RoboVM AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.robovm.apple.imageio;

/*<imports>*/
import java.io.*;
import java.nio.*;
import java.util.*;
import org.robovm.objc.*;
import org.robovm.objc.annotation.*;
import org.robovm.objc.block.*;
import org.robovm.rt.*;
import org.robovm.rt.annotation.*;
import org.robovm.rt.bro.*;
import org.robovm.rt.bro.annotation.*;
import org.robovm.rt.bro.ptr.*;
import org.robovm.apple.foundation.*;
import org.robovm.apple.corefoundation.*;
import org.robovm.apple.coregraphics.*;
/*</imports>*/

/*<javadoc>*/
/*</javadoc>*/
/*<annotations>*/@Library("ImageIO") @StronglyLinked/*</annotations>*/
@Marshaler(/*<name>*/CGImagePropertyCIFF/*</name>*/.Marshaler.class)
/*<visibility>*/public/*</visibility>*/ class /*<name>*/CGImagePropertyCIFF/*</name>*/ 
    extends /*<extends>*/GlobalValueEnumeration<CFString>/*</extends>*/
    /*<implements>*//*</implements>*/ {

    static { Bro.bind(/*<name>*/CGImagePropertyCIFF/*</name>*/.class); }

    /*<marshalers>*/
    public static class Marshaler {
        @MarshalsPointer
        public static CGImagePropertyCIFF toObject(Class<CGImagePropertyCIFF> cls, long handle, long flags) {
            CFString o = (CFString) CFType.Marshaler.toObject(CFString.class, handle, flags);
            if (o == null) {
                return null;
            }
            return CGImagePropertyCIFF.valueOf(o);
        }
        @MarshalsPointer
        public static long toNative(CGImagePropertyCIFF o, long flags) {
            if (o == null) {
                return 0L;
            }
            return CFType.Marshaler.toNative(o.value(), flags);
        }
    }
    public static class AsListMarshaler {
        @MarshalsPointer
        public static List<CGImagePropertyCIFF> toObject(Class<? extends CFType> cls, long handle, long flags) {
            CFArray o = (CFArray) CFType.Marshaler.toObject(CFArray.class, handle, flags);
            if (o == null) {
                return null;
            }
            List<CGImagePropertyCIFF> list = new ArrayList<>();
            for (int i = 0; i < o.size(); i++) {
                list.add(CGImagePropertyCIFF.valueOf(o.get(i, CFString.class)));
            }
            return list;
        }
        @MarshalsPointer
        public static long toNative(List<CGImagePropertyCIFF> l, long flags) {
            if (l == null) {
                return 0L;
            }
            CFArray array = CFMutableArray.create();
            for (CGImagePropertyCIFF o : l) {
                array.add(o.value());
            }
            return CFType.Marshaler.toNative(array, flags);
        }
    }
    /*</marshalers>*/

    /*<constants>*/
    public static final CGImagePropertyCIFF Description = new CGImagePropertyCIFF("Description");
    public static final CGImagePropertyCIFF Firmware = new CGImagePropertyCIFF("Firmware");
    public static final CGImagePropertyCIFF OwnerName = new CGImagePropertyCIFF("OwnerName");
    public static final CGImagePropertyCIFF ImageName = new CGImagePropertyCIFF("ImageName");
    public static final CGImagePropertyCIFF ImageFileName = new CGImagePropertyCIFF("ImageFileName");
    public static final CGImagePropertyCIFF ReleaseMethod = new CGImagePropertyCIFF("ReleaseMethod");
    public static final CGImagePropertyCIFF ReleaseTiming = new CGImagePropertyCIFF("ReleaseTiming");
    public static final CGImagePropertyCIFF RecordID = new CGImagePropertyCIFF("RecordID");
    public static final CGImagePropertyCIFF SelfTimingTime = new CGImagePropertyCIFF("SelfTimingTime");
    public static final CGImagePropertyCIFF CameraSerialNumber = new CGImagePropertyCIFF("CameraSerialNumber");
    public static final CGImagePropertyCIFF ImageSerialNumber = new CGImagePropertyCIFF("ImageSerialNumber");
    public static final CGImagePropertyCIFF ContinuousDrive = new CGImagePropertyCIFF("ContinuousDrive");
    public static final CGImagePropertyCIFF FocusMode = new CGImagePropertyCIFF("FocusMode");
    public static final CGImagePropertyCIFF MeteringMode = new CGImagePropertyCIFF("MeteringMode");
    public static final CGImagePropertyCIFF ShootingMode = new CGImagePropertyCIFF("ShootingMode");
    public static final CGImagePropertyCIFF LensModel = new CGImagePropertyCIFF("LensModel");
    public static final CGImagePropertyCIFF LensMaxMM = new CGImagePropertyCIFF("LensMaxMM");
    public static final CGImagePropertyCIFF LensMinMM = new CGImagePropertyCIFF("LensMinMM");
    public static final CGImagePropertyCIFF WhiteBalanceIndex = new CGImagePropertyCIFF("WhiteBalanceIndex");
    public static final CGImagePropertyCIFF FlashExposureComp = new CGImagePropertyCIFF("FlashExposureComp");
    public static final CGImagePropertyCIFF MeasuredEV = new CGImagePropertyCIFF("MeasuredEV");
    /*</constants>*/
    
    private static /*<name>*/CGImagePropertyCIFF/*</name>*/[] values = new /*<name>*/CGImagePropertyCIFF/*</name>*/[] {/*<value_list>*/Description, Firmware, OwnerName, ImageName, ImageFileName, ReleaseMethod, ReleaseTiming, RecordID, SelfTimingTime, CameraSerialNumber, ImageSerialNumber, ContinuousDrive, FocusMode, MeteringMode, ShootingMode, LensModel, LensMaxMM, LensMinMM, WhiteBalanceIndex, FlashExposureComp, MeasuredEV/*</value_list>*/};
    
    /*<name>*/CGImagePropertyCIFF/*</name>*/ (String getterName) {
        super(Values.class, getterName);
    }
    
    public static /*<name>*/CGImagePropertyCIFF/*</name>*/ valueOf(/*<type>*/CFString/*</type>*/ value) {
        for (/*<name>*/CGImagePropertyCIFF/*</name>*/ v : values) {
            if (v.value().equals(value)) {
                return v;
            }
        }
        throw new IllegalArgumentException("No constant with value " + value + " found in " 
            + /*<name>*/CGImagePropertyCIFF/*</name>*/.class.getName());
    }
    
    /*<methods>*//*</methods>*/
    
    /*<annotations>*/@Library("ImageIO") @StronglyLinked/*</annotations>*/
    public static class Values {
    	static { Bro.bind(Values.class); }

        /*<values>*/
        @GlobalValue(symbol="kCGImagePropertyCIFFDescription", optional=true)
        public static native CFString Description();
        @GlobalValue(symbol="kCGImagePropertyCIFFFirmware", optional=true)
        public static native CFString Firmware();
        @GlobalValue(symbol="kCGImagePropertyCIFFOwnerName", optional=true)
        public static native CFString OwnerName();
        @GlobalValue(symbol="kCGImagePropertyCIFFImageName", optional=true)
        public static native CFString ImageName();
        @GlobalValue(symbol="kCGImagePropertyCIFFImageFileName", optional=true)
        public static native CFString ImageFileName();
        @GlobalValue(symbol="kCGImagePropertyCIFFReleaseMethod", optional=true)
        public static native CFString ReleaseMethod();
        @GlobalValue(symbol="kCGImagePropertyCIFFReleaseTiming", optional=true)
        public static native CFString ReleaseTiming();
        @GlobalValue(symbol="kCGImagePropertyCIFFRecordID", optional=true)
        public static native CFString RecordID();
        @GlobalValue(symbol="kCGImagePropertyCIFFSelfTimingTime", optional=true)
        public static native CFString SelfTimingTime();
        @GlobalValue(symbol="kCGImagePropertyCIFFCameraSerialNumber", optional=true)
        public static native CFString CameraSerialNumber();
        @GlobalValue(symbol="kCGImagePropertyCIFFImageSerialNumber", optional=true)
        public static native CFString ImageSerialNumber();
        @GlobalValue(symbol="kCGImagePropertyCIFFContinuousDrive", optional=true)
        public static native CFString ContinuousDrive();
        @GlobalValue(symbol="kCGImagePropertyCIFFFocusMode", optional=true)
        public static native CFString FocusMode();
        @GlobalValue(symbol="kCGImagePropertyCIFFMeteringMode", optional=true)
        public static native CFString MeteringMode();
        @GlobalValue(symbol="kCGImagePropertyCIFFShootingMode", optional=true)
        public static native CFString ShootingMode();
        @GlobalValue(symbol="kCGImagePropertyCIFFLensModel", optional=true)
        public static native CFString LensModel();
        @GlobalValue(symbol="kCGImagePropertyCIFFLensMaxMM", optional=true)
        public static native CFString LensMaxMM();
        @GlobalValue(symbol="kCGImagePropertyCIFFLensMinMM", optional=true)
        public static native CFString LensMinMM();
        @GlobalValue(symbol="kCGImagePropertyCIFFWhiteBalanceIndex", optional=true)
        public static native CFString WhiteBalanceIndex();
        @GlobalValue(symbol="kCGImagePropertyCIFFFlashExposureComp", optional=true)
        public static native CFString FlashExposureComp();
        @GlobalValue(symbol="kCGImagePropertyCIFFMeasuredEV", optional=true)
        public static native CFString MeasuredEV();
        /*</values>*/
    }
}
