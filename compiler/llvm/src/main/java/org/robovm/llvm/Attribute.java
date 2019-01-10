package org.robovm.llvm;

import org.robovm.llvm.binding.ContextRef;
import org.robovm.llvm.binding.LLVM;
import org.robovm.llvm.binding.ValueRef;

/**
 * This class was manually ported from llvm/IR/Attributes.inc to implement new AttributeRef API
 * @author dkimitsa
 */
public interface Attribute {

    void addToFunction(ContextRef ctx, ValueRef vref);
    void removeFromFunction(ContextRef ctx, ValueRef vref);
    void setOnFunction(ContextRef ctx, ValueRef vref, boolean val);

    //
    // Internal implementation below
    //
    int ReturnIndex = 0;
    int FunctionIndex = -1;
    int FirstArgIndex = 1;

    enum AttrKind {
        // TODO: these were manually ported from "llvm/IR/Attributes.inc"
        // content of these can change with new version of LLVM which might cause index
        // shift and broken behaviour

        // IR-Level Attributes
        None,                  ///< No attributes have been set
        Alignment,
        AllocSize,
        AlwaysInline,
        ArgMemOnly,
        Builtin,
        ByVal,
        Cold,
        Convergent,
        Dereferenceable,
        DereferenceableOrNull,
        InAlloca,
        InReg,
        InaccessibleMemOnly,
        InaccessibleMemOrArgMemOnly,
        InlineHint,
        JumpTable,
        MinSize,
        Naked,
        Nest,
        NoAlias,
        NoBuiltin,
        NoCapture,
        NoCfCheck,
        NoDuplicate,
        NoImplicitFloat,
        NoInline,
        NoRecurse,
        NoRedZone,
        NoReturn,
        NoUnwind,
        NonLazyBind,
        NonNull,
        OptForFuzzing,
        OptimizeForSize,
        OptimizeNone,
        ReadNone,
        ReadOnly,
        Returned,
        ReturnsTwice,
        SExt,
        SafeStack,
        SanitizeAddress,
        SanitizeHWAddress,
        SanitizeMemory,
        SanitizeThread,
        ShadowCallStack,
        Speculatable,
        StackAlignment,
        StackProtect,
        StackProtectReq,
        StackProtectStrong,
        StrictFP,
        StructRet,
        SwiftError,
        SwiftSelf,
        UWTable,
        WriteOnly,
        ZExt,
        EndAttrKinds           ///< Sentinal value useful for loops
    }


    enum EnumAttr implements Attribute {
        // EnumAttr classes
        AlignmentAttr(AttrKind.Alignment),
        AllocSizeAttr(AttrKind.AllocSize),
        AlwaysInlineAttr(AttrKind.AlwaysInline),
        ArgMemOnlyAttr(AttrKind.ArgMemOnly),
        BuiltinAttr(AttrKind.Builtin),
        ByValAttr(AttrKind.ByVal),
        ColdAttr(AttrKind.Cold),
        ConvergentAttr(AttrKind.Convergent),
        DereferenceableAttr(AttrKind.Dereferenceable),
        DereferenceableOrNullAttr(AttrKind.DereferenceableOrNull),
        InAllocaAttr(AttrKind.InAlloca),
        InRegAttr(AttrKind.InReg),
        InaccessibleMemOnlyAttr(AttrKind.InaccessibleMemOnly),
        InaccessibleMemOrArgMemOnlyAttr(AttrKind.InaccessibleMemOrArgMemOnly),
        InlineHintAttr(AttrKind.InlineHint),
        JumpTableAttr(AttrKind.JumpTable),
        MinSizeAttr(AttrKind.MinSize),
        NakedAttr(AttrKind.Naked),
        NestAttr(AttrKind.Nest),
        NoAliasAttr(AttrKind.NoAlias),
        NoBuiltinAttr(AttrKind.NoBuiltin),
        NoCaptureAttr(AttrKind.NoCapture),
        NoCfCheckAttr(AttrKind.NoCfCheck),
        NoDuplicateAttr(AttrKind.NoDuplicate),
        NoImplicitFloatAttr(AttrKind.NoImplicitFloat),
        NoInlineAttr(AttrKind.NoInline),
        NoRecurseAttr(AttrKind.NoRecurse),
        NoRedZoneAttr(AttrKind.NoRedZone),
        NoReturnAttr(AttrKind.NoReturn),
        NoUnwindAttr(AttrKind.NoUnwind),
        NonLazyBindAttr(AttrKind.NonLazyBind),
        NonNullAttr(AttrKind.NonNull),
        OptForFuzzingAttr(AttrKind.OptForFuzzing),
        OptimizeForSizeAttr(AttrKind.OptimizeForSize),
        OptimizeNoneAttr(AttrKind.OptimizeNone),
        ReadNoneAttr(AttrKind.ReadNone),
        ReadOnlyAttr(AttrKind.ReadOnly),
        ReturnedAttr(AttrKind.Returned),
        ReturnsTwiceAttr(AttrKind.ReturnsTwice),
        SExtAttr(AttrKind.SExt),
        SafeStackAttr(AttrKind.SafeStack),
        SanitizeAddressAttr(AttrKind.SanitizeAddress),
        SanitizeHWAddressAttr(AttrKind.SanitizeHWAddress),
        SanitizeMemoryAttr(AttrKind.SanitizeMemory),
        SanitizeThreadAttr(AttrKind.SanitizeThread),
        ShadowCallStackAttr(AttrKind.ShadowCallStack),
        SpeculatableAttr(AttrKind.Speculatable),
        StackAlignmentAttr(AttrKind.StackAlignment),
        StackProtectAttr(AttrKind.StackProtect),
        StackProtectReqAttr(AttrKind.StackProtectReq),
        StackProtectStrongAttr(AttrKind.StackProtectStrong),
        StrictFPAttr(AttrKind.StrictFP),
        StructRetAttr(AttrKind.StructRet),
        SwiftErrorAttr(AttrKind.SwiftError),
        SwiftSelfAttr(AttrKind.SwiftSelf),
        UWTableAttr(AttrKind.UWTable),
        WriteOnlyAttr(AttrKind.WriteOnly),
        ZExtAttr(AttrKind.ZExt);

        final AttrKind kind;

        EnumAttr(AttrKind kind) {
            this.kind = kind;
        }

        @Override
        public void addToFunction(ContextRef ctx, ValueRef vref) {
            LLVM.AddAttributeAtIndex(vref, FunctionIndex, LLVM.CreateEnumAttribute(ctx, kind.ordinal(), 0));
        }

        @Override
        public void removeFromFunction(ContextRef ctx, ValueRef vref) {

        }

        @Override
        public void setOnFunction(ContextRef ctx, ValueRef vref, boolean val) {
            if (val)
                addToFunction(ctx, vref);
            else
                removeFromFunction(ctx, vref);
        }
    }

    enum StrBoolAttr implements Attribute {
        // StrBoolAttr classes
        LessPreciseFPMADAttr("less-precise-fpmad"),
        NoInfsFPMathAttr("no-infs-fp-math"),
        NoJumpTablesAttr("no-jump-tables"),
        NoNansFPMathAttr("no-nans-fp-math"),
        ProfileSampleAccurateAttr("profile-sample-accurate"),
        UnsafeFPMathAttr("unsafe-fp-math");

        final String kind;

        StrBoolAttr(String kind) {
            this.kind = kind;
        }

        @Override
        public void addToFunction(ContextRef ctx, ValueRef vref) {
            // nobody shell call it directly
            setOnFunction(ctx, vref, true);
        }

        @Override
        public void removeFromFunction(ContextRef ctx, ValueRef vref) {
            LLVM.RemoveStringAttributeAtIndex(vref, FunctionIndex, kind);
        }

        @Override
        public void setOnFunction(ContextRef ctx, ValueRef vref, boolean val) {
            LLVM.RemoveStringAttributeAtIndex(vref, FunctionIndex, kind);
            LLVM.AddAttributeAtIndex(vref, FunctionIndex, LLVM.CreateStringAttribute(ctx, kind, val ? "true" : "false"));
        }
    }
}

