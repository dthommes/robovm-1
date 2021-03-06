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
package org.robovm.apple.coremedia;

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
import org.robovm.apple.dispatch.*;
import org.robovm.apple.coreaudio.*;
import org.robovm.apple.coregraphics.*;
import org.robovm.apple.corevideo.*;
import org.robovm.apple.audiotoolbox.*;
/*</imports>*/

/*<javadoc>*/
/*</javadoc>*/
/*<annotations>*/@Library("CoreMedia")/*</annotations>*/
/*<visibility>*/public/*</visibility>*/ class /*<name>*/CMTimeCodeFormatDescription/*</name>*/ 
    extends /*<extends>*/CMFormatDescription/*</extends>*/ 
    /*<implements>*//*</implements>*/ {

    /*<ptr>*/public static class CMTimeCodeFormatDescriptionPtr extends Ptr<CMTimeCodeFormatDescription, CMTimeCodeFormatDescriptionPtr> {}/*</ptr>*/
    /*<bind>*/static { Bro.bind(CMTimeCodeFormatDescription.class); }/*</bind>*/
    /*<constants>*//*</constants>*/
    /*<constructors>*//*</constructors>*/
    /*<properties>*//*</properties>*/
    /*<members>*//*</members>*/
    /**
     * @throws OSStatusException 
     * @since Available in iOS 4.0 and later.
     */
    public static CMTimeCodeFormatDescription create(CMTimeCodeFormatType timeCodeFormatType, @ByVal CMTime frameDuration, int frameQuanta, CMTimeCodeFlags tcFlags, CMTimeCodeFormatDescriptionExtension extensions) throws OSStatusException {
        CMTimeCodeFormatDescription.CMTimeCodeFormatDescriptionPtr ptr = new CMTimeCodeFormatDescription.CMTimeCodeFormatDescriptionPtr();
        OSStatus status = create0(null, timeCodeFormatType, frameDuration, frameQuanta, tcFlags, extensions, ptr);
        OSStatusException.throwIfNecessary(status);
        return ptr.get();
    }
    /**
     * @throws OSStatusException 
     * @since Available in iOS 8.0 and later.
     */
    public static CMTimeCodeFormatDescription createFromBigEndianTimeCodeDescriptionData(BytePtr timeCodeDescriptionData, @MachineSizedUInt long timeCodeDescriptionSize, String timeCodeDescriptionFlavor) throws OSStatusException {
        CMTimeCodeFormatDescription.CMTimeCodeFormatDescriptionPtr ptr = new CMTimeCodeFormatDescription.CMTimeCodeFormatDescriptionPtr();
        OSStatus status = createFromBigEndianTimeCodeDescriptionData0(null, timeCodeDescriptionData, timeCodeDescriptionSize, timeCodeDescriptionFlavor, ptr);
        OSStatusException.throwIfNecessary(status);
        return ptr.get();
    }
    /**
     * @throws OSStatusException 
     * @since Available in iOS 8.0 and later.
     */
    public static CMTimeCodeFormatDescription createFromBigEndianTimeCodeDescriptionBlockBuffer(CMBlockBuffer timeCodeDescriptionBlockBuffer, String timeCodeDescriptionFlavor) throws OSStatusException {
        CMTimeCodeFormatDescription.CMTimeCodeFormatDescriptionPtr ptr = new CMTimeCodeFormatDescription.CMTimeCodeFormatDescriptionPtr();
        OSStatus status = createFromBigEndianTimeCodeDescriptionBlockBuffer0(null, timeCodeDescriptionBlockBuffer, timeCodeDescriptionFlavor, ptr);
        OSStatusException.throwIfNecessary(status);
        return ptr.get();
    }
    /**
     * @throws OSStatusException 
     * @since Available in iOS 8.0 and later.
     */
    public CMBlockBuffer copyAsBigEndianTimeCodeDescriptionBlockBuffer(CMTimeCodeFormatDescription timeCodeFormatDescription, String timeCodeDescriptionFlavor) throws OSStatusException {
        CMBlockBuffer.CMBlockBufferPtr ptr = new CMBlockBuffer.CMBlockBufferPtr();
        OSStatus status = copyAsBigEndianTimeCodeDescriptionBlockBuffer0(null, timeCodeFormatDescription, timeCodeDescriptionFlavor, ptr);
        OSStatusException.throwIfNecessary(status);
        return ptr.get();
    }
    /*<methods>*/
    @Bridge(symbol="CMTimeCodeFormatDescriptionCreate", optional=true)
    private static native OSStatus create0(CFAllocator allocator, CMTimeCodeFormatType timeCodeFormatType, @ByVal CMTime frameDuration, int frameQuanta, CMTimeCodeFlags flags, CMTimeCodeFormatDescriptionExtension extensions, CMTimeCodeFormatDescription.CMTimeCodeFormatDescriptionPtr formatDescriptionOut);
    @Bridge(symbol="CMTimeCodeFormatDescriptionGetFrameDuration", optional=true)
    public native @ByVal CMTime getFrameDuration();
    @Bridge(symbol="CMTimeCodeFormatDescriptionGetFrameQuanta", optional=true)
    public native int getFrameQuanta();
    @Bridge(symbol="CMTimeCodeFormatDescriptionGetTimeCodeFlags", optional=true)
    public native CMTimeCodeFlags getTimeCodeFlags();
    @Bridge(symbol="CMTimeCodeFormatDescriptionCreateFromBigEndianTimeCodeDescriptionData", optional=true)
    private static native OSStatus createFromBigEndianTimeCodeDescriptionData0(CFAllocator allocator, BytePtr timeCodeDescriptionData, @MachineSizedUInt long size, String flavor, CMTimeCodeFormatDescription.CMTimeCodeFormatDescriptionPtr formatDescriptionOut);
    @Bridge(symbol="CMTimeCodeFormatDescriptionCreateFromBigEndianTimeCodeDescriptionBlockBuffer", optional=true)
    private static native OSStatus createFromBigEndianTimeCodeDescriptionBlockBuffer0(CFAllocator allocator, CMBlockBuffer timeCodeDescriptionBlockBuffer, String flavor, CMTimeCodeFormatDescription.CMTimeCodeFormatDescriptionPtr formatDescriptionOut);
    @Bridge(symbol="CMTimeCodeFormatDescriptionCopyAsBigEndianTimeCodeDescriptionBlockBuffer", optional=true)
    private static native OSStatus copyAsBigEndianTimeCodeDescriptionBlockBuffer0(CFAllocator allocator, CMTimeCodeFormatDescription timeCodeFormatDescription, String flavor, CMBlockBuffer.CMBlockBufferPtr blockBufferOut);
    /*</methods>*/
}
