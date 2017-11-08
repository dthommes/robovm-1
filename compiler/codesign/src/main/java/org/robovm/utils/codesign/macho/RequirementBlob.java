package org.robovm.utils.codesign.macho;

import org.robovm.utils.codesign.context.SignCtx;
import org.robovm.utils.codesign.context.VerifyCtx;
import org.robovm.utils.codesign.macho.Requirements.Expression;
import org.robovm.utils.codesign.utils.ByteBufferUtils;
import org.robovm.utils.codesign.utils.FileByteBuffer;

import java.nio.ByteBuffer;

public class RequirementBlob extends Blob {

    private final Expression exp;

    public RequirementBlob(VerifyCtx ctx, int magic, FileByteBuffer reader) {
        super(magic);

        // pick up data
        reader.position(8); // skip magic and length

        // from: libsecurity_codesigning/lib/requirement.h
        // Single requirement.
        // This is a contiguous binary blob, starting with this header
        // and followed by binary expr-code. All links within the blob
        // are offset-relative to the start of the header.
        // This is designed to be a binary stable format. Note that we restrict
        // outselves to 4GB maximum size (4 byte size/offset), and we expect real
        // Requirement blobs to be fairly small (a few kilobytes at most).
        //
        // The "kind" field allows for adding different kinds of Requirements altogether
        // in the future. We expect to stay within the framework of "opExpr" requirements,
        // but it never hurts to have a way out.
        int kind = reader.getInt();

        exp = Expression.read(reader);
        ctx.debug(exp) ;
    }

    public RequirementBlob(SignCtx ctx, Expression exp) {
        super(Magic.REQUIREMENT);
        this.exp = exp;

        ByteBufferUtils.Writer writer = new ByteBufferUtils.Writer();
        writer.putInt(magic);
        writer.putInt(0); // size, will write later
        writer.putInt(1); // Kind.exprForm = 1

        // dump expression
        exp.write(writer);

        // dump back and write size
        writer.position(4);
        writer.putInt(writer.limit());

        setBlobData(writer.rawByteBuffer());
    }
}

