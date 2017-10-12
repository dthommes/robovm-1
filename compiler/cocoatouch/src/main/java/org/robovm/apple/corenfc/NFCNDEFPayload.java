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
package org.robovm.apple.corenfc;

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
import org.robovm.apple.dispatch.*;
/*</imports>*/

/*<javadoc>*/
/**
 * @since Available in iOS 11.0 and later.
 */
/*</javadoc>*/
/*<annotations>*/@Library("CoreNFC") @NativeClass/*</annotations>*/
/*<visibility>*/public/*</visibility>*/ class /*<name>*/NFCNDEFPayload/*</name>*/ 
    extends /*<extends>*/NSObject/*</extends>*/ 
    /*<implements>*/implements NSSecureCoding/*</implements>*/ {

    /*<ptr>*/public static class NFCNDEFPayloadPtr extends Ptr<NFCNDEFPayload, NFCNDEFPayloadPtr> {}/*</ptr>*/
    /*<bind>*/static { ObjCRuntime.bind(NFCNDEFPayload.class); }/*</bind>*/
    /*<constants>*//*</constants>*/
    /*<constructors>*/
    protected NFCNDEFPayload() {}
    protected NFCNDEFPayload(Handle h, long handle) { super(h, handle); }
    protected NFCNDEFPayload(SkipInit skipInit) { super(skipInit); }
    @Method(selector = "initWithCoder:")
    public NFCNDEFPayload(NSCoder decoder) { super((SkipInit) null); initObject(init(decoder)); }
    /*</constructors>*/
    /*<properties>*/
    @Property(selector = "typeNameFormat")
    public native NFCTypeNameFormat getTypeNameFormat();
    @Property(selector = "setTypeNameFormat:")
    public native void setTypeNameFormat(NFCTypeNameFormat v);
    @Property(selector = "type")
    public native NSData getType();
    @Property(selector = "setType:")
    public native void setType(NSData v);
    @Property(selector = "identifier")
    public native NSData getIdentifier();
    @Property(selector = "setIdentifier:")
    public native void setIdentifier(NSData v);
    @Property(selector = "payload")
    public native NSData getPayload();
    @Property(selector = "setPayload:")
    public native void setPayload(NSData v);
    @Property(selector = "supportsSecureCoding")
    public static native boolean supportsSecureCoding();
    /*</properties>*/
    /*<members>*//*</members>*/
    /*<methods>*/
    @Method(selector = "encodeWithCoder:")
    public native void encode(NSCoder coder);
    @Method(selector = "initWithCoder:")
    protected native @Pointer long init(NSCoder decoder);
    /*</methods>*/
}