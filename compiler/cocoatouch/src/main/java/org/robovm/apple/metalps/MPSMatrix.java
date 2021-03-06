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
package org.robovm.apple.metalps;

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
import org.robovm.apple.coregraphics.*;
import org.robovm.apple.metal.*;
/*</imports>*/

/*<javadoc>*/
/**
 * @since Available in iOS 10.0 and later.
 */
/*</javadoc>*/
/*<annotations>*/@Library("MetalPerformanceShaders") @NativeClass/*</annotations>*/
/*<visibility>*/public/*</visibility>*/ class /*<name>*/MPSMatrix/*</name>*/ 
    extends /*<extends>*/NSObject/*</extends>*/ 
    /*<implements>*//*</implements>*/ {

    /*<ptr>*/public static class MPSMatrixPtr extends Ptr<MPSMatrix, MPSMatrixPtr> {}/*</ptr>*/
    /*<bind>*/static { ObjCRuntime.bind(MPSMatrix.class); }/*</bind>*/
    /*<constants>*//*</constants>*/
    /*<constructors>*/
    protected MPSMatrix() {}
    protected MPSMatrix(Handle h, long handle) { super(h, handle); }
    protected MPSMatrix(SkipInit skipInit) { super(skipInit); }
    @Method(selector = "initWithBuffer:descriptor:")
    public MPSMatrix(MTLBuffer buffer, MPSMatrixDescriptor descriptor) { super((SkipInit) null); initObject(init(buffer, descriptor)); }
    /**
     * @since Available in iOS 13.0 and later.
     */
    @Method(selector = "initWithBuffer:offset:descriptor:")
    public MPSMatrix(MTLBuffer buffer, @MachineSizedUInt long offset, MPSMatrixDescriptor descriptor) { super((SkipInit) null); initObject(init(buffer, offset, descriptor)); }
    @Method(selector = "initWithDevice:descriptor:")
    public MPSMatrix(MTLDevice device, MPSMatrixDescriptor descriptor) { super((SkipInit) null); initObject(init(device, descriptor)); }
    /*</constructors>*/
    /*<properties>*/
    @Property(selector = "device")
    public native MTLDevice getDevice();
    @Property(selector = "rows")
    public native @MachineSizedUInt long getRows();
    @Property(selector = "columns")
    public native @MachineSizedUInt long getColumns();
    /**
     * @since Available in iOS 11.0 and later.
     */
    @Property(selector = "matrices")
    public native @MachineSizedUInt long getMatrices();
    @Property(selector = "dataType")
    public native MPSDataType getDataType();
    @Property(selector = "rowBytes")
    public native @MachineSizedUInt long getRowBytes();
    /**
     * @since Available in iOS 11.0 and later.
     */
    @Property(selector = "matrixBytes")
    public native @MachineSizedUInt long getMatrixBytes();
    /**
     * @since Available in iOS 13.0 and later.
     */
    @Property(selector = "offset")
    public native @MachineSizedUInt long getOffset();
    @Property(selector = "data")
    public native MTLBuffer getData();
    /*</properties>*/
    /*<members>*//*</members>*/
    /*<methods>*/
    @Method(selector = "initWithBuffer:descriptor:")
    protected native @Pointer long init(MTLBuffer buffer, MPSMatrixDescriptor descriptor);
    /**
     * @since Available in iOS 13.0 and later.
     */
    @Method(selector = "initWithBuffer:offset:descriptor:")
    protected native @Pointer long init(MTLBuffer buffer, @MachineSizedUInt long offset, MPSMatrixDescriptor descriptor);
    @Method(selector = "initWithDevice:descriptor:")
    protected native @Pointer long init(MTLDevice device, MPSMatrixDescriptor descriptor);
    /**
     * @since Available in iOS 11.3 and later.
     */
    @Method(selector = "synchronizeOnCommandBuffer:")
    public native void synchronizeOnCommandBuffer(MTLCommandBuffer commandBuffer);
    /**
     * @since Available in iOS 11.3 and later.
     */
    @Method(selector = "resourceSize")
    public native @MachineSizedUInt long resourceSize();
    /*</methods>*/
}
